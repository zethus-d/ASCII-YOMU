package com.niket.asciiyomu;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import java.io.InputStream;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
  private volatile boolean volKeys = false;
  private Uri pending;
  private InputStream in;

  private void handle(Intent i) {
    if (i == null) return;
    Uri u = null;
    if (Intent.ACTION_VIEW.equals(i.getAction())) u = i.getData();
    else if (Intent.ACTION_SEND.equals(i.getAction())) u = (Uri) i.getParcelableExtra(Intent.EXTRA_STREAM);
    if (u == null) return;
    pending = u;
    getBridge().getWebView().post(() ->
      getBridge().getWebView().evaluateJavascript("window.yomuOpen&&yomuOpen()", null));
  }

  @Override
  protected void onNewIntent(Intent i) {
    super.onNewIntent(i);
    setIntent(i);
    handle(i);
  }

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
      @JavascriptInterface public String begin() {
        try {
          if (pending == null) return "";
          String name = "book.cbz"; long size = -1;
          Cursor c = getContentResolver().query(pending, null, null, null, null);
          if (c != null) {
            if (c.moveToFirst()) {
              int n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME), s = c.getColumnIndex(OpenableColumns.SIZE);
              if (n >= 0 && !c.isNull(n)) name = c.getString(n);
              if (s >= 0 && !c.isNull(s)) size = c.getLong(s);
            }
            c.close();
          } else if (pending.getLastPathSegment() != null) name = pending.getLastPathSegment();
          in = getContentResolver().openInputStream(pending);
          return size + "|" + name;
        } catch (Exception e) { return ""; }
      }
      @JavascriptInterface public String chunk(int len) {
        try {
          if (in == null) return "";
          byte[] b = new byte[len];
          int n = in.read(b);
          if (n <= 0) return "";
          return Base64.encodeToString(b, 0, n, Base64.NO_WRAP);
        } catch (Exception e) { return ""; }
      }
      @JavascriptInterface public void end() {
        try { if (in != null) in.close(); } catch (Exception e) {}
        in = null; pending = null;
      }
    }, "YomuNative");
    handle(getIntent());
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
