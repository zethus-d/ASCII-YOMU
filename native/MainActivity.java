package com.niket.asciiyomu;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
  private volatile boolean volKeys = false;

  @Override
  public void onCreate(Bundle b) {
    super.onCreate(b);
    getBridge().getWebView().addJavascriptInterface(new Object() {
      @JavascriptInterface public void fs(final boolean on) {
        runOnUiThread(() -> {
          WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
          if (on) {
            c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            c.hide(WindowInsetsCompat.Type.systemBars());
          } else c.show(WindowInsetsCompat.Type.systemBars());
        });
      }
      @JavascriptInterface public void awake(final boolean on) {
        runOnUiThread(() -> {
          if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        });
      }
      @JavascriptInterface public void vol(boolean on) { volKeys = on; }
    }, "YomuNative");
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent e) {
    int k = e.getKeyCode();
    if (volKeys && (k == KeyEvent.KEYCODE_VOLUME_UP || k == KeyEvent.KEYCODE_VOLUME_DOWN)) {
      if (e.getAction() == KeyEvent.ACTION_DOWN)
        getBridge().getWebView().evaluateJavascript(
          "window.yomuVol&&yomuVol('" + (k == KeyEvent.KEYCODE_VOLUME_UP ? "up" : "down") + "')", null);
      return true;
    }
    return super.dispatchKeyEvent(e);
  }
}
