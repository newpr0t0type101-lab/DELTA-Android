package app.delta.learnc;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public final class MainActivity extends Activity {
    private static final String HOST = "appassets.androidplatform.net";
    private static final int EXPORT_REQUEST = 404;
    private WebView browser;
    private String pendingExport;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(249,248,253));
        getWindow().setNavigationBarColor(Color.rgb(249,248,253));
        getWindow().getDecorView().setSystemUiVisibility(
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        browser = new WebView(this);
        browser.setBackgroundColor(Color.rgb(249,248,253));
        WebSettings settings=browser.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setDefaultTextEncodingName("UTF-8");
        browser.setWebChromeClient(new WebChromeClient());
        browser.setWebViewClient(new LocalClient());
        browser.addJavascriptInterface(new NativeBridge(), "DELTANative");
        setContentView(browser);
        browser.loadUrl("https://" + HOST + "/index.html");
    }
    private static boolean isLocal(Uri u) {
        return u != null && "https".equalsIgnoreCase(u.getScheme())
            && HOST.equalsIgnoreCase(u.getHost()) && u.getPort() == -1;
    }
    private final class LocalClient extends WebViewClient {
        @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest request) {
            Uri u=request.getUrl();
            if (!isLocal(u)) return super.shouldInterceptRequest(v,request);
            String path=u.getEncodedPath();
            if(path==null || path.equals("/"))path="/index.html";
            String decoded=Uri.decode(path);
            if(!decoded.startsWith("/") || decoded.contains("..") || decoded.contains("\\"))
                return missing();
            String filename=decoded.substring(1);
            String mime=filename.endsWith(".html")?"text/html":
                filename.endsWith(".js")?"application/javascript":
                filename.endsWith(".css")?"text/css":
                filename.endsWith(".png")?"image/png":
                filename.endsWith(".webmanifest")?"application/manifest+json":"text/plain";
            try{
                InputStream stream=getAssets().open(filename);
                return new WebResourceResponse(mime,mime.startsWith("image/")?null:"UTF-8",stream);
            }catch(IOException e){return missing();}
        }
        private WebResourceResponse missing(){
            return new WebResourceResponse("text/plain","UTF-8",404,"Not Found",
                Collections.emptyMap(),
                new ByteArrayInputStream("Not Found".getBytes(StandardCharsets.UTF_8)));
        }
        @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){
            if(isLocal(request.getUrl()))return false;
            if(request.isForMainFrame())external(request.getUrl().toString());
            return true;
        }
    }
    private void external(String address){
        try{
            Uri url=Uri.parse(address);
            if(!"https".equalsIgnoreCase(url.getScheme()) && !"http".equalsIgnoreCase(url.getScheme()))return;
            startActivity(new Intent(Intent.ACTION_VIEW,url));
        }catch(ActivityNotFoundException|IllegalArgumentException e){
            Toast.makeText(this,"Нет приложения для открытия ссылки",Toast.LENGTH_SHORT).show();
        }
    }
    private final class NativeBridge {
        @JavascriptInterface public void openExternal(String url) {
            runOnUiThread(()->external(url));
        }
        @JavascriptInterface public void exportProgress(String json) {
            if(json==null || json.length()>2_000_000)return;
            runOnUiThread(()->{
                pendingExport=json;
                Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("application/json");
                i.putExtra(Intent.EXTRA_TITLE,"DELTA-progress.json");
                try{startActivityForResult(i,EXPORT_REQUEST);}
                catch(ActivityNotFoundException e){Toast.makeText(MainActivity.this,"Нет выбора файла",Toast.LENGTH_SHORT).show();}
            });
        }
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=EXPORT_REQUEST)return;
        if(resultCode==RESULT_OK && data!=null && data.getData()!=null && pendingExport!=null){
            try(OutputStream out=getContentResolver().openOutputStream(data.getData())){
                if(out!=null){
                    out.write(pendingExport.getBytes(StandardCharsets.UTF_8));
                    Toast.makeText(this,"Прогресс DELTA сохранён",Toast.LENGTH_SHORT).show();
                }
            }catch(IOException e){Toast.makeText(this,"Не удалось сохранить прогресс",Toast.LENGTH_SHORT).show();}
        }
        pendingExport=null;
    }
    @Override public void onBackPressed(){
        if(browser==null){finish();return;}
        browser.evaluateJavascript(
            "Boolean(window.DELTA && window.DELTA.onAndroidBack && window.DELTA.onAndroidBack())",
            result->{if(!"true".equals(result))finish();});
    }
    @Override protected void onDestroy(){
        if(browser!=null){browser.destroy();browser=null;}
        super.onDestroy();
    }
}
