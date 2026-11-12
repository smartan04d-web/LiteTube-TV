package com.litetube.tv;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.SharedPreferences;
import android.webkit.WebResourceResponse;
import java.io.ByteArrayInputStream;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String HOME = "https://m.youtube.com/";
    private FrameLayout root;
    private LinearLayout toolbar;
    private WebView web;
    private View fullView;
    private WebChromeClient.CustomViewCallback fullCallback;
    private int oldOrientation;
    private EditText search;
    private Button filterButton;
    private boolean filterEnabled;
    private boolean autoSkipEnabled;
    private Button skipButton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean active;
    private final Runnable skipCheck = new Runnable() {
        @Override public void run() {
            if (!active || web == null) return;
            if (autoSkipEnabled) web.evaluateJavascript(SKIP_SCRIPT, null);
            handler.postDelayed(this, 1500);
        }
    };
    // Only click an enabled and visible Skip Ad button during an actual ad.
    // The script runs in the top document only; cross-origin frames are inaccessible.
    private static final String SKIP_SCRIPT =
        "(function(){try{" +
        "var ad=document.querySelector('.ad-showing,.ytp-ad-player-overlay,.ytp-ad-preview-container');" +
        "if(!ad)return;" +
        "var selectors=['.ytp-ad-skip-button','.ytp-ad-skip-button-modern'," +
        "'.ytp-skip-ad-button','.ytp-ad-skip-button-container button'," +
        "'button[aria-label=\\\"Skip Ad\\\"]','button[aria-label=\\\"Skip ads\\\"]'];" +
        "var buttons=document.querySelectorAll(selectors.join(','));" +
        "for(var i=0;i<buttons.length;i++){var b=buttons[i];" +
        "var r=b.getBoundingClientRect(),st=getComputedStyle(b);" +
        "var disabled=b.disabled||b.getAttribute('aria-disabled')==='true';" +
        "if(!disabled&&r.width>0&&r.height>0&&st.visibility!=='hidden'&&st.display!=='none'){" +
        "b.click();break;}}}catch(e){}})();";
    private static final String[] FILTERED_HOSTS = {
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "adnxs.com", "taboola.com", "outbrain.com"
    };
    private boolean isFilteredHost(String host) {
        if (!filterEnabled || host == null) return false;
        String h = host.toLowerCase(Locale.ROOT);
        for (String suffix : FILTERED_HOSTS)
            if (h.equals(suffix) || h.endsWith("." + suffix)) return true;
        return false;
    }
    private void refreshFilterLabel() {
        filterButton.setText(filterEnabled ? "Ads filter ON" : "Ads filter OFF");
    }
    private void refreshSkipLabel() {
        skipButton.setText(autoSkipEnabled ? "Auto skip ON" : "Auto skip OFF");
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(10), dp(6), dp(10), dp(6));
        toolbar.setBackgroundColor(Color.rgb(24,24,24));

        Button home = button("Home");
        home.setOnClickListener(v -> web.loadUrl(HOME));
        Button back = button("Back");
        back.setOnClickListener(v -> goBack());
        Button refresh = button("Reload");
        refresh.setOnClickListener(v -> web.reload());
        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(Color.LTGRAY);
        search.setHint("Search YouTube");
        search.setTextSize(17);
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener((v, actionId, event) -> {
            runSearch();
            return true;
        });
        Button find = button("Search");
        find.setOnClickListener(v -> runSearch());
        Button browser = button("Browse");
        browser.setOnClickListener(v -> { web.requestFocus(); toast("Use remote arrows and OK; some WebView pages may require a mouse."); });
        toolbar.addView(home);
        toolbar.addView(back);
        toolbar.addView(refresh);
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(0, dp(52), 1f);
        searchLp.setMargins(dp(8), 0, dp(8), 0);
        toolbar.addView(search, searchLp);
        toolbar.addView(find);
        toolbar.addView(browser);
        SharedPreferences prefs = getSharedPreferences("litetube_tv", MODE_PRIVATE);
        filterEnabled = prefs.getBoolean("filter", false);
        autoSkipEnabled = prefs.getBoolean("auto_skip", true);
        filterButton = button("");
        refreshFilterLabel();
        filterButton.setOnClickListener(v -> {
            filterEnabled = !filterEnabled;
            prefs.edit().putBoolean("filter", filterEnabled).apply();
            refreshFilterLabel();
            web.reload();
        });
        skipButton = button("");
        refreshSkipLabel();
        skipButton.setOnClickListener(v -> {
            autoSkipEnabled = !autoSkipEnabled;
            prefs.edit().putBoolean("auto_skip", autoSkipEnabled).apply();
            refreshSkipLabel();
            if (autoSkipEnabled && active && web != null) web.evaluateJavascript(SKIP_SCRIPT, null);
        });
        toolbar.addView(filterButton);
        toolbar.addView(skipButton);
        column.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(66)));

        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (isFilteredHost(request.getUrl().getHost())) {
                    return new WebResourceResponse("text/plain", "UTF-8",
                        new ByteArrayInputStream(new byte[0]));
                }
                return null;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri uri = request.getUrl();
                String scheme = uri.getScheme(), host = uri.getHost();
                if ("https".equalsIgnoreCase(scheme) && isYoutube(host)) return false;
                if ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                    catch (ActivityNotFoundException ex) { toast("No browser installed"); }
                }
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullView != null) { callback.onCustomViewHidden(); return; }
                fullView = view;
                fullCallback = callback;
                oldOrientation = getRequestedOrientation();
                toolbar.setVisibility(View.GONE);
                web.setVisibility(View.GONE);
                root.addView(view, new FrameLayout.LayoutParams(-1, -1));
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
            @Override public void onHideCustomView() { exitFull(); }
        });
        column.addView(web, new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(column, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        if (state != null) web.restoreState(state); else web.loadUrl(HOME);
        home.requestFocus();
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setFocusable(true);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(7), 0, dp(7), 0);
        return b;
    }
    private void runSearch() {
        String q = search.getText().toString().trim();
        if (q.isEmpty()) return;
        web.loadUrl("https://m.youtube.com/results?search_query=" + Uri.encode(q));
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(search.getWindowToken(), 0);
        web.requestFocus();
    }
    private static boolean isYoutube(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(Locale.ROOT);
        return h.equals("youtube.com") || h.endsWith(".youtube.com") ||
            h.equals("youtu.be") || h.equals("youtube-nocookie.com") ||
            h.endsWith(".youtube-nocookie.com");
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
    private void exitFull() {
        if (fullView == null) return;
        root.removeView(fullView);
        fullView = null;
        toolbar.setVisibility(View.VISIBLE);
        web.setVisibility(View.VISIBLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(oldOrientation);
        WebChromeClient.CustomViewCallback cb = fullCallback;
        fullCallback = null;
        if (cb != null) cb.onCustomViewHidden();
        web.requestFocus();
    }
    private void goBack() {
        if (fullView != null) exitFull();
        else if (web.canGoBack()) web.goBack();
        else finish();
    }
    @Override public void onBackPressed() { goBack(); }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            goBack(); return true;
        }
        return super.dispatchKeyEvent(event);
    }
    @Override protected void onPause() {
        active = false;
        handler.removeCallbacks(skipCheck);
        if (web != null) web.onPause();
        super.onPause();
    }
    @Override protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
        active = true;
        handler.removeCallbacks(skipCheck);
        handler.postDelayed(skipCheck, 1500);
    }
    @Override protected void onSaveInstanceState(Bundle b) { if (web != null) web.saveState(b); super.onSaveInstanceState(b); }
    @Override protected void onDestroy() {
        active = false;
        handler.removeCallbacks(skipCheck);
        if (fullView != null) exitFull();
        if (web != null) {
            ViewGroup parent = (ViewGroup) web.getParent();
            if (parent != null) parent.removeView(web);
            web.destroy(); web = null;
        }
        super.onDestroy();
    }
}
