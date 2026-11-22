package __PACKAGE__;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final Uri HOME_URI = Uri.parse("__URL__");
    private static final String HOME = HOME_URI.toString();
    private static final int BACKGROUND = Color.rgb(244, 240, 230);
    private static final int TEXT = Color.rgb(21, 38, 58);

    private WebView web;
    private LinearLayout errorPanel;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(BACKGROUND);
        setContentView(frame);
        configureSystemBars(frame);

        web = new WebView(this);
        web.setBackgroundColor(BACKGROUND);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri target = request.getUrl();
                if (isAppOrigin(target)) return false;
                if ("https".equalsIgnoreCase(target.getScheme())) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, target));
                    } catch (ActivityNotFoundException exception) {
                        Toast.makeText(MainActivity.this, "No browser is available to open this link.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showConnectionError();
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request,
                    WebResourceResponse response) {
                if (request.isForMainFrame()) showConnectionError();
            }
        });

        frame.addView(web, new FrameLayout.LayoutParams(-1, -1));
        errorPanel = makeErrorPanel();
        frame.addView(errorPanel, new FrameLayout.LayoutParams(-1, -1));
        if (state == null || web.restoreState(state) == null) openDashboard();
    }

    private static boolean isAppOrigin(Uri target) {
        return "https".equalsIgnoreCase(target.getScheme())
                && HOME_URI.getHost().equalsIgnoreCase(target.getHost())
                && httpsPort(HOME_URI) == httpsPort(target);
    }

    private static int httpsPort(Uri uri) {
        return uri.getPort() == -1 ? 443 : uri.getPort();
    }

    private void configureSystemBars(FrameLayout frame) {
        if (Build.VERSION.SDK_INT >= 35) {
            frame.setOnApplyWindowInsetsListener((view, windowInsets) -> {
                Insets bars = windowInsets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout());
                int keyboard = windowInsets.getInsets(WindowInsets.Type.ime()).bottom;
                view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard));
                return windowInsets;
            });
            // The view's controller is available once Android attaches the window.
            frame.post(() -> {
                WindowInsetsController controller = frame.getWindowInsetsController();
                if (controller != null) {
                    int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                    controller.setSystemBarsAppearance(light, light);
                }
            });
        } else {
            getWindow().setStatusBarColor(BACKGROUND);
            getWindow().setNavigationBarColor(BACKGROUND);
            frame.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    private LinearLayout makeErrorPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        panel.setPadding(padding, padding, padding, padding);
        panel.setBackgroundColor(BACKGROUND);
        panel.setVisibility(View.GONE);

        TextView message = new TextView(this);
        message.setGravity(Gravity.CENTER);
        message.setTextColor(TEXT);
        message.setTextSize(16);
        message.setText("The app could not load. Check your connection and try again.");
        panel.addView(message);
        Button retry = new Button(this);
        retry.setText("Try again");
        retry.setOnClickListener(view -> openDashboard());
        panel.addView(retry);
        return panel;
    }

    private void openDashboard() {
        errorPanel.setVisibility(View.GONE);
        web.setVisibility(View.VISIBLE);
        web.loadUrl(HOME);
    }

    private void showConnectionError() {
        web.setVisibility(View.GONE);
        errorPanel.setVisibility(View.VISIBLE);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) {
            errorPanel.setVisibility(View.GONE);
            web.setVisibility(View.VISIBLE);
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        web.saveState(state);
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onDestroy() {
        ((FrameLayout) web.getParent()).removeView(web);
        web.destroy();
        super.onDestroy();
    }
}
