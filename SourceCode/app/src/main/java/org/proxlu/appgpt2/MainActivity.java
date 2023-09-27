package org.proxlu.appgpt2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.TextView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends Activity {

    private WebView chatWebView = null;
    private TextView errorTextView = null; // Adicionei a declaração da variável errorTextView
    private CookieManager chatCookieManager = null;
    private final Context context = this;
    private final String TAG = "AppGPT2";
    private final String urlToLoad = "https://talkai.info/pt/chat";

    private static final ArrayList<String> allowedDomains = new ArrayList<>();

    @Override
    protected void onPause() {
        if (chatCookieManager != null) chatCookieManager.flush();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setTheme(android.R.style.Theme_DeviceDefault_DayNight);
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Pagina de erro
        errorTextView = findViewById(R.id.errorTextView);

        // Create the WebView
        chatWebView = findViewById(R.id.chatWebView);

        // Executa JavaScript para ocultar elementos com determinadas classes CSS
        chatWebView.evaluateJavascript("document.querySelectorAll('.ads').forEach(ad => ad.style.display = 'none');", null);

        // Set cookie options
        chatCookieManager = CookieManager.getInstance();
        chatCookieManager.setAcceptCookie(true);
        chatCookieManager.setAcceptThirdPartyCookies(chatWebView, false);

        // Restrict what gets loaded
        initURLs();

        chatWebView.setWebViewClient(new WebViewClient() {
            // Para suprimir avisos sobre uso de método obsoleto
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                // Esconder WebView e mostrar a mensagem de erro
                chatWebView.setVisibility(View.GONE);
                errorTextView.setVisibility(View.VISIBLE);

                // Verificar se o erro é causado pelo name resolution
                if (errorCode == -2) {
                    // Mostrar uma mensagem de erro personalizada
                    String message = "Erro ao carregar a página: " + "O nome de domínio não pode ser resolvido.";
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
            // Keep these in sync!
            @Override
            public WebResourceResponse shouldInterceptRequest(final WebView view, WebResourceRequest request) {
                if (request.getUrl().toString().equals("about:blank")) {
                    return null;
                }
                if (!request.getUrl().toString().startsWith("https://")) {
                    Log.d(TAG, "[shouldInterceptRequest][NON-HTTPS] Blocked access to " + request.getUrl().toString());
                    return new WebResourceResponse("text/javascript", "UTF-8", null); // Deny URLs that aren't HTTPS
                }
                boolean allowed = false;
                for (String url : allowedDomains) {
                    if (Objects.requireNonNull(request.getUrl().getHost()).endsWith(url)) {
                        allowed = true;
                    }
                }
                if (!allowed) {
                    Log.d(TAG, "[shouldInterceptRequest][NOT ON ALLOWLIST] Blocked access to " + request.getUrl().getHost());
                    if (request.getUrl().getHost().equals("login.microsoftonline.com") || request.getUrl().getHost().equals("accounts.google.com") || request.getUrl().getHost().equals("appleid.apple.com")) {
                        Toast.makeText(context, context.getString(R.string.error_microsoft_google), Toast.LENGTH_LONG).show();
                        resetChat();
                    }
                    return new WebResourceResponse("text/javascript", "UTF-8", null); // Deny URLs not on ALLOWLIST
                }
                return null;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request.getUrl().toString().equals("about:blank")) {
                    return false;
                }
                if (!request.getUrl().toString().startsWith("https://")) {
                    Log.d(TAG, "[shouldOverrideUrlLoading][NON-HTTPS] Blocked access to " + request.getUrl().toString());
                    return true; // Deny URLs that aren't HTTPS
                }
                boolean allowed = false;
                for (String url : allowedDomains) {
                    if (Objects.requireNonNull(request.getUrl().getHost()).endsWith(url)) {
                        allowed = true;
                    }
                }
                if (!allowed) {
                    Log.d(TAG, "[shouldOverrideUrlLoading][NOT ON ALLOWLIST] Blocked access to " + request.getUrl().getHost());
                    if (request.getUrl().getHost().equals("login.microsoftonline.com") || request.getUrl().getHost().equals("accounts.google.com") || request.getUrl().getHost().equals("appleid.apple.com")) {
                        Toast.makeText(context, context.getString(R.string.error_microsoft_google), Toast.LENGTH_LONG).show();
                        resetChat();
                    }
                    return true; // Deny URLs not on ALLOWLIST
                }
                return false;
            }

        });

        errorTextView = findViewById(R.id.errorTextView);
        errorTextView.setVisibility(View.GONE);
        // Set more options
        // Removi a segunda declaração desta variável
        WebSettings chatWebSettings = chatWebView.getSettings();
        // Enable some WebView features
        chatWebSettings.setJavaScriptEnabled(true);
        chatWebSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        chatWebSettings.setDomStorageEnabled(true);
        // Disable some WebView features
        chatWebSettings.setAllowContentAccess(false);
        chatWebSettings.setAllowFileAccess(false);
        chatWebSettings.setBuiltInZoomControls(false);
        chatWebSettings.setDatabaseEnabled(false);
        chatWebSettings.setDisplayZoomControls(false);
        chatWebSettings.setSaveFormData(false);
        chatWebSettings.setGeolocationEnabled(false);

        //Set copy image functionality
        chatWebView.setOnLongClickListener(v -> {
            WebView.HitTestResult result = chatWebView.getHitTestResult();
            if (result.getType() == WebView.HitTestResult.IMAGE_TYPE) {
                String imgUrl = result.getExtra();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Image URL", imgUrl);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Imagem copiada", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        // Load ChatGPT
        chatWebView.loadUrl(urlToLoad);
        if (GithubStar.shouldShowStarDialog(this))
            GithubStar.starDialog(this);

        // Editar o texto do placeholder do textarea
        chatWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // Injetar JavaScript na página
                String javascript = "javascript:(function() { document.querySelector('textarea[placeholder=\"Describe the image\"]').placeholder = 'Descreva a imagem'; })()";
                chatWebView.loadUrl(javascript);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Credit (CC BY-SA 3.0): https://stackoverflow.com/a/6077173
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (chatWebView.canGoBack() && !chatWebView.getUrl().equals("about:blank")) {
                    chatWebView.goBack();
                } else {
                    finish();
                }
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void resetChat() {

        chatWebView.clearFormData();
        chatWebView.clearHistory();
        chatWebView.clearMatches();
        chatWebView.clearSslPreferences();
        chatCookieManager.removeSessionCookie();
        chatCookieManager.removeAllCookie();
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        WebStorage.getInstance().deleteAllData();
        chatWebView.loadUrl(urlToLoad);
    }

    private static void initURLs() {
        // Allowed Domains
        allowedDomains.add("cdn.auth0.com");
        allowedDomains.add("openai.com");
        allowedDomains.add("talkai.info");
        allowedDomains.add("oaidalleapiprodscus.blob.core.windows.net");
        allowedDomains.add("cdn-icons-png.flaticon.com");

    }
}
