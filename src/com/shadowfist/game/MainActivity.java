package com.shadowfist.game;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

// Raw ValueCallback on purpose: d8 8.2 crashes on the generic form compiled by JDK 21.
@SuppressWarnings("rawtypes")
public class MainActivity extends Activity implements ValueCallback {
    private WebView web;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0b0b0f"));
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setTextZoom(100);
        web.setWebViewClient(new AssetClient(getAssets()));
        setContentView(web);
        hideSystemUi();
        web.loadUrl("https://" + AssetClient.HOST + "/index.html");
    }

    /**
     * Serves the bundled assets from a fake https origin so localStorage (the save game)
     * works; file:// pages don't get persistent storage. Nothing leaves the device.
     */
    static class AssetClient extends WebViewClient {
        static final String HOST = "appassets.androidplatform.net";
        private final AssetManager assets;

        AssetClient(AssetManager assets) { this.assets = assets; }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
            Uri u = req.getUrl();
            if (!HOST.equals(u.getHost()) || u.getPath() == null) return null;
            String path = u.getPath().substring(1);
            try {
                InputStream in = assets.open(path);
                return new WebResourceResponse(mime(path), mime(path).startsWith("text") ? "utf-8" : null, in);
            } catch (IOException e) {
                return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found",
                        new HashMap<String, String>(), new ByteArrayInputStream(new byte[0]));
            }
        }

        private static String mime(String path) {
            if (path.endsWith(".html")) return "text/html";
            if (path.endsWith(".js")) return "text/javascript";
            if (path.endsWith(".png")) return "image/png";
            return "application/octet-stream";
        }
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    @Override
    protected void onPause() {
        web.evaluateJavascript("window.pauseGame && pauseGame()", null);
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.handleBack ? handleBack() : false", this);
    }

    // result of handleBack(): close the app only when the page didn't handle it
    @Override
    public void onReceiveValue(Object handled) {
        if (!"true".equals(handled)) super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        web.destroy();
        super.onDestroy();
    }
}
