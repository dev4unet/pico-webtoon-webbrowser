package net.dev4u.webtoonbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PicoWebtoonBrowser";
    private static final String DEFAULT_HOME_URL = "https://s.dev4u.net/code";
    private static final String PREFS_NAME = "webtoon_browser_prefs";
    private static final String BOOKMARKS_KEY = "bookmarks";
    private static final String HOME_URL_KEY = "home_url";
    private static final String TABS_KEY = "saved_tabs";
    private static final String RESTORE_TABS_KEY = "restore_tabs";
    private static final String SCALING_MODE_KEY = "scaling_mode";
    private static final String HISTORY_KEY = "history";
    private static final String MAX_HISTORY_KEY = "max_history";
    private static final String SHOW_TABS_KEY = "show_tabs";
    private static final String BOOKMARK_DISPLAY_KEY = "bookmark_display";
    private static final String HISTORY_DISPLAY_KEY = "history_display";
    private static final int DEFAULT_MAX_HISTORY = 100;
    private static final String DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int EXPORT_BOOKMARKS_REQUEST = 1002;
    private static final int IMPORT_BOOKMARKS_REQUEST = 1003;
    private static final int EXPORT_SETTINGS_REQUEST = 1004;
    private static final int IMPORT_SETTINGS_REQUEST = 1005;

    // UI elements
    private FrameLayout webviewContainer;
    private EditText urlBar;
    private ProgressBar progressBar;
    private ImageButton btnBack, btnForward, btnRefresh, btnHome;
    private ImageButton btnBookmark, btnBookmarksList, btnNewTab;
    private LinearLayout tabContainer;
    private LinearLayout tabBarLayout;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    // Tab management
    private final List<TabInfo> tabs = new ArrayList<>();
    private int activeTabIndex = -1;

    // File upload
    private ValueCallback<Uri[]> fileUploadCallback;

    // Bookmarks
    private SharedPreferences prefs;

    static class TabInfo {
        WebView webView;
        String title;
        String url;

        TabInfo(WebView webView) {
            this.webView = webView;
            this.title = "New Tab";
            this.url = "";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Migrate old 3-mode system to 2-mode system (v0.3.0)
        int savedMode = prefs.getInt(SCALING_MODE_KEY, 0);
        if (savedMode == 1) {
            // Old "normal portrait" mode removed → fall back to webtoon mode
            prefs.edit().putInt(SCALING_MODE_KEY, 0).apply();
        } else if (savedMode == 2) {
            // Old "normal landscape" → new mode 1 "normal desktop"
            prefs.edit().putInt(SCALING_MODE_KEY, 1).apply();
        }

        initViews();
        setupNavigation();

        // Restore tabs or create first tab
        boolean restored = false;
        if (prefs.getBoolean(RESTORE_TABS_KEY, false)) {
            restored = restoreTabs();
        }

        if (!restored) {
            // Create first tab
            String intentUrl = null;
            Intent intent = getIntent();
            if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
                Uri data = intent.getData();
                if (data != null) {
                    intentUrl = data.toString();
                }
            }
            addNewTab(intentUrl != null ? intentUrl : getHomeUrl());
        }
    }

    private void initViews() {
        webviewContainer = findViewById(R.id.webview_container);
        urlBar = findViewById(R.id.url_bar);
        progressBar = findViewById(R.id.progress_bar);
        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnHome = findViewById(R.id.btn_home);
        btnBookmark = findViewById(R.id.btn_bookmark);
        btnBookmarksList = findViewById(R.id.btn_bookmarks_list);
        btnNewTab = findViewById(R.id.btn_new_tab);
        tabContainer = findViewById(R.id.tab_container);
        tabBarLayout = findViewById(R.id.tab_bar_layout);
        swipeRefresh = findViewById(R.id.swipe_refresh);

        // Setup swipe refresh
        swipeRefresh.setOnRefreshListener(() -> {
            WebView wv = getActiveWebView();
            if (wv != null) {
                wv.reload();
            }
            swipeRefresh.setRefreshing(false);
        });
        // PC모드에서는 SwipeRefreshLayout 비활성화 (스크롤 간섭 방지)
        swipeRefresh.setEnabled(prefs.getInt(SCALING_MODE_KEY, 0) == 0);

        // Apply tab bar visibility
        boolean showTabs = prefs.getBoolean(SHOW_TABS_KEY, true);
        tabBarLayout.setVisibility(showTabs ? View.VISIBLE : View.GONE);
    }

    private void setupNavigation() {
        btnBack.setOnClickListener(v -> {
            WebView wv = getActiveWebView();
            if (wv != null && wv.canGoBack()) wv.goBack();
        });

        btnForward.setOnClickListener(v -> {
            WebView wv = getActiveWebView();
            if (wv != null && wv.canGoForward()) wv.goForward();
        });

        btnRefresh.setOnClickListener(v -> {
            WebView wv = getActiveWebView();
            if (wv != null) wv.reload();
        });

        btnHome.setOnClickListener(v -> {
            WebView wv = getActiveWebView();
            if (wv != null) wv.loadUrl(getHomeUrl());
        });

        btnHome.setOnLongClickListener(v -> {
            showHomePageSettings();
            return true;
        });

        btnNewTab.setOnClickListener(v -> addNewTab(getHomeUrl()));

        btnBookmark.setOnClickListener(v -> toggleBookmark());

        btnBookmarksList.setOnClickListener(v -> showMainMenu());

        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String input = urlBar.getText().toString().trim();
                if (!input.isEmpty()) {
                    WebView wv = getActiveWebView();
                    if (wv != null) wv.loadUrl(normalizeUrl(input));
                }
                return true;
            }
            return false;
        });
    }

    // ===================== Tab Management =====================

    private void addNewTab(String url) {
        WebView webView = createWebView();
        TabInfo tab = new TabInfo(webView);
        tabs.add(tab);

        webviewContainer.addView(webView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));

        switchToTab(tabs.size() - 1);
        webView.loadUrl(url);
        refreshTabBar();
    }

    private void closeTab(int index) {
        if (tabs.size() <= 1) {
            WebView wv = tabs.get(0).webView;
            wv.loadUrl(getHomeUrl());
            return;
        }

        TabInfo tab = tabs.get(index);
        webviewContainer.removeView(tab.webView);
        tab.webView.stopLoading();
        tab.webView.destroy();
        tabs.remove(index);

        if (activeTabIndex >= tabs.size()) {
            activeTabIndex = tabs.size() - 1;
        } else if (activeTabIndex > index) {
            activeTabIndex--;
        } else if (activeTabIndex == index) {
            activeTabIndex = Math.min(index, tabs.size() - 1);
        }

        switchToTab(activeTabIndex);
        refreshTabBar();
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        for (int i = 0; i < tabs.size(); i++) {
            tabs.get(i).webView.setVisibility(i == index ? View.VISIBLE : View.GONE);
        }

        activeTabIndex = index;
        TabInfo tab = tabs.get(index);
        urlBar.setText(tab.url);
        updateNavigationButtons();
        updateBookmarkButton();
        refreshTabBar();
    }

    private void refreshTabBar() {
        tabContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        float density = getResources().getDisplayMetrics().density;
        int maxTabWidth = (int) (160 * density);

        for (int i = 0; i < tabs.size(); i++) {
            final int tabIndex = i;
            TabInfo tab = tabs.get(i);

            View tabView = inflater.inflate(R.layout.tab_item, tabContainer, false);
            TextView titleView = tabView.findViewById(R.id.tab_title);
            ImageButton closeBtn = tabView.findViewById(R.id.tab_close);

            String displayTitle = tab.title;
            if (displayTitle == null || displayTitle.isEmpty()) {
                displayTitle = "New Tab";
            }
            titleView.setText(displayTitle);

            // 탭 최대 너비 제한 (닫기 버튼이 항상 보이도록)
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tabView.getLayoutParams();
            lp.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            tabView.setLayoutParams(lp);
            tabView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            if (tabView.getMeasuredWidth() > maxTabWidth) {
                lp.width = maxTabWidth;
                tabView.setLayoutParams(lp);
            }

            if (i == activeTabIndex) {
                tabView.setBackgroundResource(R.drawable.tab_background_active);
                titleView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            } else {
                tabView.setBackgroundResource(R.drawable.tab_background_inactive);
                titleView.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }

            tabView.setOnClickListener(v -> switchToTab(tabIndex));
            closeBtn.setOnClickListener(v -> closeTab(tabIndex));

            tabContainer.addView(tabView);
        }
    }

    private WebView getActiveWebView() {
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            return tabs.get(activeTabIndex).webView;
        }
        return null;
    }

    // ===================== WebView Setup =====================

    private WebView createWebView() {
        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setSupportMultipleWindows(true);

        // Apply browser mode (UA + viewport settings)
        int scalingMode = prefs.getInt(SCALING_MODE_KEY, 0);
        applyScalingMode(webView, scalingMode);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } catch (Exception e) {
                        // No handler
                    }
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (view == getActiveWebView()) {
                    urlBar.setText(url);
                    progressBar.setVisibility(View.VISIBLE);
                }
                updateTabInfo(view, null, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (view == getActiveWebView()) {
                    progressBar.setVisibility(View.GONE);
                    updateNavigationButtons();
                    updateBookmarkButton();
                }

                // Save to history
                String title = view.getTitle();
                saveHistory(title, url);

                // Inject viewport for browser mode
                injectViewportForMode(view);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (view == getActiveWebView()) {
                    progressBar.setProgress(newProgress);
                    if (newProgress == 100) {
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                updateTabInfo(view, title, null);
                if (view == getActiveWebView()) {
                    refreshTabBar();
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView,
                    ValueCallback<Uri[]> filePathCallback,
                    FileChooserParams fileChooserParams) {
                if (fileUploadCallback != null) {
                    fileUploadCallback.onReceiveValue(null);
                }
                fileUploadCallback = filePathCallback;
                try {
                    Intent intent = fileChooserParams.createIntent();
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    fileUploadCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                    boolean isUserGesture, android.os.Message resultMsg) {
                WebView newWebView = createWebView();
                TabInfo tab = new TabInfo(newWebView);
                tabs.add(tab);
                webviewContainer.addView(newWebView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
                switchToTab(tabs.size() - 1);
                refreshTabBar();

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) ->
            handleDownload(url, userAgent, contentDisposition, mimeType));

        // Listen for layout changes to re-inject viewport on window resize
        webView.addOnLayoutChangeListener((v, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> {
            int newWidth = right - left;
            int oldWidth = oldRight - oldLeft;
            if (newWidth != oldWidth && newWidth > 0) {
                injectViewportForMode((WebView) v);
            }
        });

        return webView;
    }

    private void updateTabInfo(WebView view, String title, String url) {
        for (TabInfo tab : tabs) {
            if (tab.webView == view) {
                if (title != null) tab.title = title;
                if (url != null) tab.url = url;
                break;
            }
        }
    }

    // ===================== Bookmarks =====================

    private void toggleBookmark() {
        WebView wv = getActiveWebView();
        if (wv == null) return;

        String url = wv.getUrl();
        String title = wv.getTitle();
        if (url == null || url.isEmpty()) return;

        List<BookmarkEntry> bookmarks = loadBookmarks();

        int existingIndex = -1;
        for (int i = 0; i < bookmarks.size(); i++) {
            if (bookmarks.get(i).url.equals(url)) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex >= 0) {
            bookmarks.remove(existingIndex);
            Toast.makeText(this, R.string.bookmark_removed, Toast.LENGTH_SHORT).show();
        } else {
            bookmarks.add(0, new BookmarkEntry(title != null ? title : url, url));
            Toast.makeText(this, R.string.bookmark_added, Toast.LENGTH_SHORT).show();
        }

        saveBookmarks(bookmarks);
        updateBookmarkButton();
    }

    private void updateBookmarkButton() {
        WebView wv = getActiveWebView();
        if (wv == null) return;

        String url = wv.getUrl();
        boolean isBookmarked = false;

        if (url != null) {
            List<BookmarkEntry> bookmarks = loadBookmarks();
            for (BookmarkEntry b : bookmarks) {
                if (b.url.equals(url)) {
                    isBookmarked = true;
                    break;
                }
            }
        }

        btnBookmark.setColorFilter(isBookmarked
            ? ContextCompat.getColor(this, R.color.bookmark_star)
            : ContextCompat.getColor(this, R.color.icon_tint));
    }

    private void showBookmarksList() {
        List<BookmarkEntry> bookmarks = loadBookmarks();

        if (bookmarks.isEmpty()) {
            Toast.makeText(this, R.string.no_bookmarks, Toast.LENGTH_SHORT).show();
            return;
        }

        int displayMode = prefs.getInt(BOOKMARK_DISPLAY_KEY, 0); // 0=타이틀만, 1=URL만, 2=타이틀+URL
        String[] titles = new String[bookmarks.size()];
        for (int i = 0; i < bookmarks.size(); i++) {
            BookmarkEntry b = bookmarks.get(i);
            if (displayMode == 0) { // 타이틀만
                titles[i] = b.title;
            } else if (displayMode == 1) { // URL만
                titles[i] = b.url;
            } else { // 타이틀 + URL
                titles[i] = b.title + "\n" + b.url;
            }
        }

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
            .setTitle(R.string.bookmarks)
            .setItems(titles, (dialog, which) -> {
                WebView wv = getActiveWebView();
                if (wv != null) {
                    wv.loadUrl(bookmarks.get(which).url);
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private List<BookmarkEntry> loadBookmarks() {
        List<BookmarkEntry> bookmarks = new ArrayList<>();
        String json = prefs.getString(BOOKMARKS_KEY, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                bookmarks.add(new BookmarkEntry(
                    obj.getString("title"),
                    obj.getString("url")));
            }
        } catch (Exception e) {
            // Ignore parse errors
        }
        return bookmarks;
    }

    private void saveBookmarks(List<BookmarkEntry> bookmarks) {
        try {
            JSONArray arr = new JSONArray();
            for (BookmarkEntry b : bookmarks) {
                JSONObject obj = new JSONObject();
                obj.put("title", b.title);
                obj.put("url", b.url);
                arr.put(obj);
            }
            prefs.edit().putString(BOOKMARKS_KEY, arr.toString()).apply();
        } catch (Exception e) {
            // Ignore
        }
    }

    static class BookmarkEntry {
        String title;
        String url;

        BookmarkEntry(String title, String url) {
            this.title = title;
            this.url = url;
        }
    }

    // ===================== Home Page Settings =====================

    private String getHomeUrl() {
        return prefs.getString(HOME_URL_KEY, DEFAULT_HOME_URL);
    }

    private void showHomePageSettings() {
        String currentHome = getHomeUrl();
        WebView wv = getActiveWebView();
        String currentPageUrl = (wv != null && wv.getUrl() != null) ? wv.getUrl() : "";

        String[] options = {
            "현재 페이지를 홈으로 설정",
            "URL 직접 입력",
            "기본값으로 초기화"
        };

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
            .setTitle("홈페이지 설정")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // 현재 페이지를 홈으로 설정
                        if (!currentPageUrl.isEmpty()) {
                            prefs.edit().putString(HOME_URL_KEY, currentPageUrl).apply();
                            Toast.makeText(this, "홈페이지가 현재 페이지로 설정됨", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 1: // URL 직접 입력
                        showHomeUrlInputDialog(currentHome);
                        break;
                    case 2: // 기본값으로 초기화
                        prefs.edit().remove(HOME_URL_KEY).apply();
                        Toast.makeText(this, "홈페이지가 기본값으로 초기화됨", Toast.LENGTH_SHORT).show();
                        break;
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void showHomeUrlInputDialog(String currentHome) {
        EditText input = new EditText(this);
        input.setText(currentHome);
        input.setSelectAllOnFocus(true);
        input.setSingleLine(true);
        input.setHint("https://");

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(input);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
            .setTitle("홈페이지 URL 입력")
            .setView(container)
            .setPositiveButton("설정", (dialog, which) -> {
                String url = input.getText().toString().trim();
                if (!url.isEmpty()) {
                    url = normalizeUrl(url);
                    prefs.edit().putString(HOME_URL_KEY, url).apply();
                    Toast.makeText(this, "홈페이지가 설정됨", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    // ===================== Utility =====================

    private String normalizeUrl(String input) {
        if (input.startsWith("http://") || input.startsWith("https://")) {
            return input;
        }
        if (input.contains(".") && !input.contains(" ")) {
            return "https://" + input;
        }
        return "https://www.google.com/search?q=" + Uri.encode(input);
    }

    private void updateNavigationButtons() {
        WebView wv = getActiveWebView();
        if (wv == null) return;
        btnBack.setAlpha(wv.canGoBack() ? 1.0f : 0.3f);
        btnForward.setAlpha(wv.canGoForward() ? 1.0f : 0.3f);
    }

    private void handleDownload(String url, String userAgent,
            String contentDisposition, String mimeType) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);

            request.setMimeType(mimeType);
            request.addRequestHeader("User-Agent", userAgent);

            String cookies = CookieManager.getInstance().getCookie(url);
            if (cookies != null) {
                request.addRequestHeader("Cookie", cookies);
            }

            request.setTitle(filename);
            request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, filename);

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(this, "다운로드 시작: " + filename, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "다운로드 실패", Toast.LENGTH_SHORT).show();
        }
    }

    // ===================== Lifecycle =====================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (fileUploadCallback != null) {
                Uri[] results = null;
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
                fileUploadCallback.onReceiveValue(results);
                fileUploadCallback = null;
            }
        } else if (requestCode == EXPORT_BOOKMARKS_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                try {
                    String jsonData = prefs.getString("export_data", "[]");
                    android.os.ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(data.getData(), "w");
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(pfd.getFileDescriptor());
                    fos.write(jsonData.getBytes());
                    fos.close();
                    pfd.close();
                    prefs.edit().remove("export_data").apply();
                    Toast.makeText(this, R.string.bookmarks_exported, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == IMPORT_BOOKMARKS_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                try {
                    android.os.ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(data.getData(), "r");
                    java.io.FileInputStream fis = new java.io.FileInputStream(pfd.getFileDescriptor());
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    fis.close();
                    pfd.close();

                    JSONArray arr = new JSONArray(sb.toString());
                    List<BookmarkEntry> bookmarks = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        bookmarks.add(new BookmarkEntry(obj.getString("title"), obj.getString("url")));
                    }
                    saveBookmarks(bookmarks);
                    Toast.makeText(this, R.string.bookmarks_imported, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, R.string.import_failed, Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == EXPORT_SETTINGS_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                try {
                    String jsonData = prefs.getString("export_settings_data", "{}");
                    android.os.ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(data.getData(), "w");
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(pfd.getFileDescriptor());
                    fos.write(jsonData.getBytes());
                    fos.close();
                    pfd.close();
                    prefs.edit().remove("export_settings_data").apply();
                    Toast.makeText(this, R.string.settings_exported, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == IMPORT_SETTINGS_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                try {
                    android.os.ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(data.getData(), "r");
                    java.io.FileInputStream fis = new java.io.FileInputStream(pfd.getFileDescriptor());
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    fis.close();
                    pfd.close();

                    JSONObject root = new JSONObject(sb.toString());

                    // Import settings
                    if (root.has("settings")) {
                        JSONObject settings = root.getJSONObject("settings");
                        SharedPreferences.Editor editor = prefs.edit();
                        if (settings.has("restore_tabs")) editor.putBoolean(RESTORE_TABS_KEY, settings.getBoolean("restore_tabs"));
                        if (settings.has("scaling_mode")) {
                            int importedMode = settings.getInt("scaling_mode");
                            // Migrate old 3-mode system: old mode 1 (portrait) → 0, old mode 2 (landscape) → 1
                            if (importedMode == 1) importedMode = 0;
                            else if (importedMode == 2) importedMode = 1;
                            else if (importedMode > 2) importedMode = 0;
                            editor.putInt(SCALING_MODE_KEY, importedMode);
                        }
                        if (settings.has("max_history")) editor.putInt(MAX_HISTORY_KEY, settings.getInt("max_history"));
                        if (settings.has("show_tabs")) editor.putBoolean(SHOW_TABS_KEY, settings.getBoolean("show_tabs"));
                        if (settings.has("bookmark_display")) editor.putInt(BOOKMARK_DISPLAY_KEY, settings.getInt("bookmark_display"));
                        if (settings.has("history_display")) editor.putInt(HISTORY_DISPLAY_KEY, settings.getInt("history_display"));
                        if (settings.has("home_url")) editor.putString(HOME_URL_KEY, settings.getString("home_url"));
                        editor.apply();

                        // Apply settings
                        tabBarLayout.setVisibility(prefs.getBoolean(SHOW_TABS_KEY, true) ? View.VISIBLE : View.GONE);
                        applyScalingModeToAllTabs();
                    }

                    // Import bookmarks
                    if (root.has("bookmarks")) {
                        JSONArray bookmarksArr = root.getJSONArray("bookmarks");
                        List<BookmarkEntry> bookmarks = new ArrayList<>();
                        for (int i = 0; i < bookmarksArr.length(); i++) {
                            JSONObject obj = bookmarksArr.getJSONObject(i);
                            bookmarks.add(new BookmarkEntry(obj.getString("title"), obj.getString("url")));
                        }
                        saveBookmarks(bookmarks);
                    }

                    // Import history
                    if (root.has("history")) {
                        JSONArray historyArr = root.getJSONArray("history");
                        List<HistoryEntry> history = new ArrayList<>();
                        for (int i = 0; i < historyArr.length(); i++) {
                            JSONObject obj = historyArr.getJSONObject(i);
                            history.add(new HistoryEntry(
                                obj.getString("title"),
                                obj.getString("url"),
                                obj.optLong("timestamp", 0)));
                        }
                        // Save history
                        prefs.edit().putString(HISTORY_KEY, historyArr.toString()).apply();
                    }

                    Toast.makeText(this, R.string.settings_imported, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, R.string.import_failed, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "dispatchKeyEvent: action=" + event.getAction()
                + ", keyCode=" + event.getKeyCode()
                + ", keyName=" + KeyEvent.keyCodeToString(event.getKeyCode())
                + ", scanCode=" + event.getScanCode()
                + ", source=0x" + Integer.toHexString(event.getSource()));
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown: keyCode=" + keyCode + ", keyName=" + KeyEvent.keyCodeToString(keyCode));
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        WebView wv = getActiveWebView();
        if (wv != null && wv.canGoBack()) {
            wv.goBack();
        } else {
            // 더 이상 뒤로갈 페이지가 없으면 앱을 종료하지 않고 현재 화면 유지
            Log.d(TAG, "onBackPressed: No history, staying on current page");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebView wv = getActiveWebView();
        if (wv != null) wv.onResume();
    }

    @Override
    protected void onPause() {
        WebView wv = getActiveWebView();
        if (wv != null) wv.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // Save tabs if restore_tabs is enabled
        if (prefs.getBoolean(RESTORE_TABS_KEY, false)) {
            saveTabs();
        }

        for (TabInfo tab : tabs) {
            tab.webView.stopLoading();
            tab.webView.destroy();
        }
        tabs.clear();
        super.onDestroy();
    }

    // ===================== Main Menu =====================

    private void showMainMenu() {
        boolean showTabs = prefs.getBoolean(SHOW_TABS_KEY, true);
        String tabToggleLabel = getString(R.string.show_tabs_bar) + ": " + (showTabs ? "ON" : "OFF");
        boolean restoreTabs = prefs.getBoolean(RESTORE_TABS_KEY, false);
        String restoreLabel = getString(R.string.restore_tabs_on_startup) + ": " + (restoreTabs ? "ON" : "OFF");

        int currentMode = prefs.getInt(SCALING_MODE_KEY, 0);
        String[] modeNamesShort = {
            getString(R.string.webtoon_mode_short),
            getString(R.string.pc_desktop_mode_short)
        };
        String browserModeLabel = getString(R.string.browser_mode) + ": " + modeNamesShort[Math.min(currentMode, 1)];

        String[] options = {
            getString(R.string.bookmarks),
            getString(R.string.history),
            browserModeLabel,
            restoreLabel,
            tabToggleLabel,
            getString(R.string.settings)
        };

        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
            .setTitle(R.string.menu)
            .setItems(options, (d, which) -> {
                switch (which) {
                    case 0: // 즐겨찾기
                        showBookmarksList();
                        break;
                    case 1: // 방문 기록
                        showHistory();
                        break;
                    case 2: // 브라우저 모드
                        showScalingModeDialog();
                        break;
                    case 3: // 탭 복원
                        toggleRestoreTabs();
                        break;
                    case 4: // 탭 바 표시/숨김
                        toggleShowTabs();
                        break;
                    case 5: // 설정
                        showSettings();
                        break;
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    // ===================== Settings =====================

    private void showSettings() {
        String[] options = {
            getString(R.string.max_history_count),
            getString(R.string.bookmark_display_mode),
            getString(R.string.history_display_mode),
            getString(R.string.export_bookmarks),
            getString(R.string.import_bookmarks),
            getString(R.string.export_settings),
            getString(R.string.import_settings),
            getString(R.string.clear_history)
        };

        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
            .setTitle(R.string.settings)
            .setItems(options, (d, which) -> {
                switch (which) {
                    case 0: // 최대 기록 개수
                        showMaxHistoryDialog();
                        break;
                    case 1: // 즐겨찾기 표시 방식
                        showBookmarkDisplayDialog();
                        break;
                    case 2: // 방문 기록 표시 방식
                        showHistoryDisplayDialog();
                        break;
                    case 3: // 즐겨찾기 내보내기
                        exportBookmarks();
                        break;
                    case 4: // 즐겨찾기 불러오기
                        importBookmarks();
                        break;
                    case 5: // 환경 설정 내보내기
                        exportSettings();
                        break;
                    case 6: // 환경 설정 불러오기
                        importSettings();
                        break;
                    case 7: // 기록 삭제
                        clearHistory();
                        break;
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void toggleRestoreTabs() {
        boolean current = prefs.getBoolean(RESTORE_TABS_KEY, false);
        prefs.edit().putBoolean(RESTORE_TABS_KEY, !current).apply();
        Toast.makeText(this,
            getString(R.string.restore_tabs_on_startup) + ": " + (!current ? "ON" : "OFF"),
            Toast.LENGTH_SHORT).show();
    }

    private void showScalingModeDialog() {
        String[] modes = {
            getString(R.string.webtoon_mode),
            getString(R.string.pc_desktop_mode)
        };

        int current = prefs.getInt(SCALING_MODE_KEY, 0);

        AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
            .setTitle(R.string.browser_mode)
            .setSingleChoiceItems(modes, Math.min(current, modes.length - 1), (d, which) -> {
                prefs.edit().putInt(SCALING_MODE_KEY, which).apply();
                applyScalingModeToAllTabs();
                Toast.makeText(this, modes[which], Toast.LENGTH_SHORT).show();
                d.dismiss();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private void showMaxHistoryDialog() {
        int current = prefs.getInt(MAX_HISTORY_KEY, DEFAULT_MAX_HISTORY);

        EditText input = new EditText(this);
        input.setText(String.valueOf(current));
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setSelectAllOnFocus(true);

        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(this);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(input);

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
            .setTitle(R.string.max_history_count)
            .setView(container)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                try {
                    int count = Integer.parseInt(input.getText().toString().trim());
                    if (count > 0) {
                        prefs.edit().putInt(MAX_HISTORY_KEY, count).apply();
                        Toast.makeText(this, "최대 이력: " + count, Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void applyScalingModeToAllTabs() {
        int mode = prefs.getInt(SCALING_MODE_KEY, 0);
        for (TabInfo tab : tabs) {
            applyScalingMode(tab.webView, mode);
            tab.webView.reload();
        }
        // PC모드에서는 SwipeRefreshLayout 비활성화 (스크롤 간섭 방지)
        swipeRefresh.setEnabled(mode == 0);
    }

    private void applyScalingMode(WebView webView, int mode) {
        WebSettings settings = webView.getSettings();
        String mobileUA = settings.getUserAgentString().replaceAll("\\s*PicoWebtoonBrowser/\\S+", "");
        if (mobileUA.contains("Windows NT")) {
            mobileUA = WebSettings.getDefaultUserAgent(this);
        }

        switch (mode) {
            case 1: // PC 모드 (가로/데스크톱) - 데스크톱 UA + 가로 화면
                settings.setUseWideViewPort(false);
                settings.setLoadWithOverviewMode(false);
                settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
                settings.setTextZoom(100);
                settings.setUserAgentString(DESKTOP_UA + " PicoWebtoonBrowser/1.0");
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            default: // 웹툰 모드 (세로/모바일) - 원래 형태
                settings.setUseWideViewPort(false);
                settings.setLoadWithOverviewMode(false);
                settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
                settings.setTextZoom(100);
                settings.setUserAgentString(mobileUA + " PicoWebtoonBrowser/1.0");
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
        }
    }

    private void injectViewportForMode(WebView webView) {
        int scalingMode = prefs.getInt(SCALING_MODE_KEY, 0);

        StringBuilder js = new StringBuilder();
        js.append("(function() {");

        if (scalingMode == 1) {
            // PC 모드: width=device-width로 페이지의 고정 viewport를 오버라이드
            // useWideViewPort(false)이므로 viewport가 실제 뷰 크기를 따르며,
            // 창 크기 변경 시 콘텐츠가 자연스럽게 리플로우됨
            js.append("var meta = document.querySelector('meta[name=viewport]');");
            js.append("if (!meta) { meta = document.createElement('meta'); meta.name = 'viewport'; document.head.appendChild(meta); }");
            js.append("meta.setAttribute('content', 'width=device-width, initial-scale=1.0, user-scalable=yes');");
        }

        js.append("document.body.style.transform = '';");
        js.append("document.body.style.transformOrigin = '';");
        js.append("document.body.style.width = '';");

        js.append("window.dispatchEvent(new Event('resize'));");
        js.append("})();");

        webView.evaluateJavascript(js.toString(), null);
    }

    private void toggleShowTabs() {
        boolean current = prefs.getBoolean(SHOW_TABS_KEY, true);
        prefs.edit().putBoolean(SHOW_TABS_KEY, !current).apply();
        tabBarLayout.setVisibility(!current ? View.VISIBLE : View.GONE);
        Toast.makeText(this,
            getString(R.string.show_tabs_bar) + ": " + (!current ? "ON" : "OFF"),
            Toast.LENGTH_SHORT).show();
    }

    private void showBookmarkDisplayDialog() {
        String[] modes = {
            getString(R.string.display_title_only),
            getString(R.string.display_url_only),
            getString(R.string.display_title_and_url)
        };

        int current = prefs.getInt(BOOKMARK_DISPLAY_KEY, 0); // 기본: 타이틀만

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
            .setTitle(R.string.bookmark_display_mode)
            .setSingleChoiceItems(modes, current, (dialog, which) -> {
                prefs.edit().putInt(BOOKMARK_DISPLAY_KEY, which).apply();
                Toast.makeText(this, modes[which], Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void showHistoryDisplayDialog() {
        String[] modes = {
            getString(R.string.display_title_only),
            getString(R.string.display_url_only),
            getString(R.string.display_title_and_url)
        };

        int current = prefs.getInt(HISTORY_DISPLAY_KEY, 0); // 기본: 타이틀만

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
            .setTitle(R.string.history_display_mode)
            .setSingleChoiceItems(modes, current, (dialog, which) -> {
                prefs.edit().putInt(HISTORY_DISPLAY_KEY, which).apply();
                Toast.makeText(this, modes[which], Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    // ===================== History =====================

    private void showHistory() {
        List<HistoryEntry> history = loadHistory();

        if (history.isEmpty()) {
            Toast.makeText(this, R.string.no_history, Toast.LENGTH_SHORT).show();
            return;
        }

        int displayMode = prefs.getInt(HISTORY_DISPLAY_KEY, 0); // 0=타이틀만, 1=URL만, 2=타이틀+URL
        String[] titles = new String[history.size()];
        for (int i = 0; i < history.size(); i++) {
            HistoryEntry h = history.get(i);
            if (displayMode == 0) { // 타이틀만
                titles[i] = h.title;
            } else if (displayMode == 1) { // URL만
                titles[i] = h.url;
            } else { // 타이틀 + URL
                titles[i] = h.title + "\n" + h.url;
            }
        }

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
            .setTitle(R.string.history)
            .setItems(titles, (dialog, which) -> {
                WebView wv = getActiveWebView();
                if (wv != null) {
                    wv.loadUrl(history.get(which).url);
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void saveHistory(String title, String url) {
        if (url == null || url.isEmpty() || url.equals("about:blank")) return;

        List<HistoryEntry> history = loadHistory();

        // Remove duplicates
        history.removeIf(h -> h.url.equals(url));

        // Add to front
        history.add(0, new HistoryEntry(title != null ? title : url, url, System.currentTimeMillis()));

        // Limit size
        int maxHistory = prefs.getInt(MAX_HISTORY_KEY, DEFAULT_MAX_HISTORY);
        while (history.size() > maxHistory) {
            history.remove(history.size() - 1);
        }

        // Save
        try {
            JSONArray arr = new JSONArray();
            for (HistoryEntry h : history) {
                JSONObject obj = new JSONObject();
                obj.put("title", h.title);
                obj.put("url", h.url);
                obj.put("timestamp", h.timestamp);
                arr.put(obj);
            }
            prefs.edit().putString(HISTORY_KEY, arr.toString()).apply();
        } catch (Exception e) {
            // Ignore
        }
    }

    private List<HistoryEntry> loadHistory() {
        List<HistoryEntry> history = new ArrayList<>();
        String json = prefs.getString(HISTORY_KEY, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                history.add(new HistoryEntry(
                    obj.getString("title"),
                    obj.getString("url"),
                    obj.optLong("timestamp", 0)));
            }
        } catch (Exception e) {
            // Ignore
        }
        return history;
    }

    private void clearHistory() {
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
            .setTitle(R.string.clear_history)
            .setMessage("모든 방문 기록을 삭제하시겠습니까?")
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                prefs.edit().remove(HISTORY_KEY).apply();
                Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    static class HistoryEntry {
        String title;
        String url;
        long timestamp;

        HistoryEntry(String title, String url, long timestamp) {
            this.title = title;
            this.url = url;
            this.timestamp = timestamp;
        }
    }

    // ===================== Tab Persistence =====================

    private void saveTabs() {
        try {
            JSONArray arr = new JSONArray();
            for (TabInfo tab : tabs) {
                JSONObject obj = new JSONObject();
                obj.put("title", tab.title);
                obj.put("url", tab.url);
                arr.put(obj);
            }
            prefs.edit().putString(TABS_KEY, arr.toString()).apply();
        } catch (Exception e) {
            // Ignore
        }
    }

    private boolean restoreTabs() {
        String json = prefs.getString(TABS_KEY, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            if (arr.length() == 0) return false;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String url = obj.getString("url");
                if (url != null && !url.isEmpty()) {
                    addNewTab(url);
                }
            }
            return tabs.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // ===================== Bookmarks Import/Export =====================

    private void exportBookmarks() {
        List<BookmarkEntry> bookmarks = loadBookmarks();
        if (bookmarks.isEmpty()) {
            Toast.makeText(this, R.string.no_bookmarks, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONArray arr = new JSONArray();
            for (BookmarkEntry b : bookmarks) {
                JSONObject obj = new JSONObject();
                obj.put("title", b.title);
                obj.put("url", b.url);
                arr.put(obj);
            }

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "pico-webtoon-bookmarks.json");

            // Store JSON data for later use
            prefs.edit().putString("export_data", arr.toString()).apply();

            startActivityForResult(intent, EXPORT_BOOKMARKS_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void importBookmarks() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, IMPORT_BOOKMARKS_REQUEST);
    }

    // ===================== Settings Import/Export =====================

    private void exportSettings() {
        View dialogView = LayoutInflater.from(this).inflate(android.R.layout.simple_list_item_multiple_choice, null);

        String[] options = {
            getString(R.string.include_bookmarks),
            getString(R.string.include_history)
        };

        boolean[] checkedItems = {true, true}; // 기본: 모두 체크

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog)
            .setTitle(R.string.export_settings)
            .setMultiChoiceItems(options, checkedItems, (dialog, which, isChecked) -> {
                checkedItems[which] = isChecked;
            })
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                performExportSettings(checkedItems[0], checkedItems[1]);
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private void performExportSettings(boolean includeBookmarks, boolean includeHistory) {
        try {
            JSONObject root = new JSONObject();

            // Settings
            JSONObject settings = new JSONObject();
            settings.put("restore_tabs", prefs.getBoolean(RESTORE_TABS_KEY, false));
            settings.put("scaling_mode", prefs.getInt(SCALING_MODE_KEY, 0));
            settings.put("max_history", prefs.getInt(MAX_HISTORY_KEY, DEFAULT_MAX_HISTORY));
            settings.put("show_tabs", prefs.getBoolean(SHOW_TABS_KEY, true));
            settings.put("bookmark_display", prefs.getInt(BOOKMARK_DISPLAY_KEY, 0));
            settings.put("history_display", prefs.getInt(HISTORY_DISPLAY_KEY, 0));
            settings.put("home_url", prefs.getString(HOME_URL_KEY, DEFAULT_HOME_URL));
            root.put("settings", settings);

            // Bookmarks
            if (includeBookmarks) {
                JSONArray bookmarksArr = new JSONArray();
                List<BookmarkEntry> bookmarks = loadBookmarks();
                for (BookmarkEntry b : bookmarks) {
                    JSONObject obj = new JSONObject();
                    obj.put("title", b.title);
                    obj.put("url", b.url);
                    bookmarksArr.put(obj);
                }
                root.put("bookmarks", bookmarksArr);
            }

            // History
            if (includeHistory) {
                JSONArray historyArr = new JSONArray();
                List<HistoryEntry> history = loadHistory();
                for (HistoryEntry h : history) {
                    JSONObject obj = new JSONObject();
                    obj.put("title", h.title);
                    obj.put("url", h.url);
                    obj.put("timestamp", h.timestamp);
                    historyArr.put(obj);
                }
                root.put("history", historyArr);
            }

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "pico-webtoon-settings.json");

            // Store JSON data for later use
            prefs.edit().putString("export_settings_data", root.toString()).apply();

            startActivityForResult(intent, EXPORT_SETTINGS_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void importSettings() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, IMPORT_SETTINGS_REQUEST);
    }
}
