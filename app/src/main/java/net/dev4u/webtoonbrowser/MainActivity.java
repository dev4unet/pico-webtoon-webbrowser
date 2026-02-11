package net.dev4u.webtoonbrowser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
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
    private static final String DEFAULT_HOME_URL = "https://blog.naver.com/dev4unet";
    private static final String PREFS_NAME = "webtoon_browser_prefs";
    private static final String BOOKMARKS_KEY = "bookmarks";
    private static final String HOME_URL_KEY = "home_url";
    private static final int FILE_CHOOSER_REQUEST = 1001;

    // UI elements
    private FrameLayout webviewContainer;
    private EditText urlBar;
    private ProgressBar progressBar;
    private ImageButton btnBack, btnForward, btnRefresh, btnHome;
    private ImageButton btnBookmark, btnBookmarksList, btnNewTab;
    private LinearLayout tabContainer;

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

        initViews();
        setupNavigation();

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

        btnBookmarksList.setOnClickListener(v -> showBookmarksList());

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
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setSupportMultipleWindows(true);

        String defaultUA = settings.getUserAgentString();
        settings.setUserAgentString(defaultUA + " PicoWebtoonBrowser/1.0");

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

        String[] titles = new String[bookmarks.size()];
        for (int i = 0; i < bookmarks.size(); i++) {
            BookmarkEntry b = bookmarks.get(i);
            titles[i] = b.title + "\n" + b.url;
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
        for (TabInfo tab : tabs) {
            tab.webView.stopLoading();
            tab.webView.destroy();
        }
        tabs.clear();
        super.onDestroy();
    }
}
