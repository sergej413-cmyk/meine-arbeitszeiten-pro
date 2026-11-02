package de.privat.arbeitszeiten;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int EXPORT = 41, IMPORT = 42;
    private WebView web;
    private ValueCallback<Uri[]> uploadCallback;
    private String exportData;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        setContentView(web);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},12);
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface public String timerState(){return TimerService.state(MainActivity.this).toString();}
            @JavascriptInterface public void timerAction(String action){
                if(!action.equals("start")&&!action.equals("pause")&&!action.equals("resume")&&!action.equals("stop"))return;
                runOnUiThread(()->TimerService.command(MainActivity.this,action));
            }
            @JavascriptInterface public void confirmSaved(String id){
                android.content.SharedPreferences p=TimerService.prefs(MainActivity.this);
                try {org.json.JSONObject j=new org.json.JSONObject(p.getString("pending",""));if(id.equals(j.optString("id")))p.edit().remove("pending").apply();}catch(Exception ignored){}
            }

            @JavascriptInterface public void saveFile(String name, String content, String mime) {
                runOnUiThread(() -> {
                    exportData = content;
                    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType(mime.split(";")[0]);
                    i.putExtra(Intent.EXTRA_TITLE, name);
                    startActivityForResult(i, EXPORT);
                });
            }
        }, "AndroidBridge");
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (uploadCallback != null) uploadCallback.onReceiveValue(null);
                uploadCallback = callback;
                try { startActivityForResult(params.createIntent(), IMPORT); }
                catch (Exception ex) { uploadCallback = null; callback.onReceiveValue(null); return false; }
                return true;
            }
        });
        web.loadUrl("file:///android_asset/index.html");
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == IMPORT && uploadCallback != null) {
            uploadCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result, data));
            uploadCallback = null;
        }
        if (request == EXPORT) {
            if (result == RESULT_OK && data != null && data.getData() != null && exportData != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                    if (out != null) out.write(exportData.getBytes(StandardCharsets.UTF_8));
                } catch (Exception ex) { android.widget.Toast.makeText(this,"Export fehlgeschlagen",android.widget.Toast.LENGTH_LONG).show(); }
            }
            exportData = null;
        }
    }
    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
