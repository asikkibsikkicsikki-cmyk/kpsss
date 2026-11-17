package com.kpssankistudio;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;
import android.widget.Toast;

import java.io.OutputStream;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 4107;
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private WebViewAssetLoader assetLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF4F46E5);
        getWindow().setNavigationBarColor(0xFF0F172A);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setDefaultTextEncodingName("UTF-8");

        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        assetLoader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
            .build();
        webView.setWebViewClient(new WebViewClientCompat() {
            @Override public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, android.webkit.WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/pdf", "text/plain", "text/markdown", "application/json",
                    "application/octet-stream"
                });
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                try {
                    startActivityForResult(Intent.createChooser(intent, "KPSS dosyası seç"), FILE_CHOOSER_REQUEST);
                } catch (ActivityNotFoundException ex) {
                    filePathCallback = null;
                    callback.onReceiveValue(null);
                    Toast.makeText(MainActivity.this, "Cihazda dosya seçici bulunamadı.", Toast.LENGTH_LONG).show();
                }
                return true;
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) WebView.setWebContentsDebuggingEnabled(false);
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST) return;
        if (filePathCallback == null) return;
        Uri[] results = null;
        if (resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                results = new Uri[count];
                for (int i = 0; i < count; i++) results[i] = data.getClipData().getItemAt(i).getUri();
            } else if (data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
        }
        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
    }

    private class AndroidBridge {
        @JavascriptInterface
        public void saveTextFile(String filename, String base64Content) {
            try {
                byte[] bytes = Base64.decode(base64Content, Base64.DEFAULT);
                String safeName = filename == null ? "kpss_anki_yedek.json" : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, safeName);
                    values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/KPSS Anki Studio");
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri == null) throw new IOException("Dosya oluşturulamadı");
                    try (OutputStream out = getContentResolver().openOutputStream(uri)) { out.write(bytes); }
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Yedek İndirilenler/KPSS Anki Studio klasörüne kaydedildi.", Toast.LENGTH_LONG).show());
                } else {
                    java.io.File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    if (dir == null) throw new IOException("Depolama alanı bulunamadı");
                    try (java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(dir, safeName))) { out.write(bytes); }
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Yedek uygulamanın dosyaları klasörüne kaydedildi.", Toast.LENGTH_LONG).show());
                }
            } catch (Exception ex) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Yedek kaydedilemedi: " + ex.getMessage(), Toast.LENGTH_LONG).show());
            }
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onPause() { if (webView != null) webView.onPause(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); if (webView != null) webView.onResume(); }
    @Override protected void onDestroy() {
        if (filePathCallback != null) filePathCallback.onReceiveValue(null);
        if (webView != null) { webView.stopLoading(); webView.destroy(); }
        super.onDestroy();
    }
}
