package com.eros.framework.extend.hook.ui.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.http.SslError;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.eros.framework.activity.GlobalWebViewActivity;
import com.taobao.weex.ui.component.WXWeb;
import com.taobao.weex.ui.view.IWebView;
import com.taobao.weex.utils.WXLogUtils;
import com.taobao.weex.utils.WXViewUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by liuyuanxiao on 2018/7/3.
 */

public class BMWXWebView implements IWebView {

  private Context mContext;
  private WebView mWebView;
  private ProgressBar mProgressBar;
  private boolean mShowLoading = true;

  private OnErrorListener mOnErrorListener;
  private OnPageListener mOnPageListener;


  public BMWXWebView(Context context) {
    mContext = context;
  }

  @Override
  public View getView() {
    FrameLayout root = new FrameLayout(mContext);
    root.setBackgroundColor(Color.TRANSPARENT);

    mWebView = new WebView(mContext);//mContext.getApplicationContext();
    FrameLayout.LayoutParams wvLayoutParams =
      new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT);
    wvLayoutParams.gravity = Gravity.CENTER;
    mWebView.setLayoutParams(wvLayoutParams);
    mWebView.setBackgroundColor(0);
    root.addView(mWebView);
    initWebView(mWebView);

    mProgressBar = new ProgressBar(mContext);
    showProgressBar(false);
    FrameLayout.LayoutParams pLayoutParams =
      new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT);
    mProgressBar.setLayoutParams(pLayoutParams);
    pLayoutParams.gravity = Gravity.CENTER;
    root.addView(mProgressBar);
    return root;
  }

  @Override
  public void destroy() {
    if (getWebView() != null) {
      getWebView().removeAllViews();
      getWebView().destroy();
      mWebView = null;
    }
  }

  @Override
  public void loadUrl(String url) {
    if (getWebView() == null)
      return;
    getWebView().loadUrl(url);
  }

  @Override
  public void loadDataWithBaseURL(String source) {

  }

  @Override
  public void reload() {
    if (getWebView() == null)
      return;
    getWebView().reload();
  }

  @Override
  public void goBack() {
    if (getWebView() == null)
      return;
    getWebView().goBack();
  }

  @Override
  public void goForward() {
    if (getWebView() == null)
      return;
    getWebView().goForward();
  }

  @Override
  public void postMessage(Object msg) {

  }

    /*@Override
    public void setVisibility(int visibility) {
        if (mRootView != null) {
            mRootView.setVisibility(visibility);
        }
    }*/

  @Override
  public void setShowLoading(boolean shown) {
    mShowLoading = shown;
  }

  @Override
  public void setOnErrorListener(OnErrorListener listener) {
    mOnErrorListener = listener;
  }

  @Override
  public void setOnPageListener(OnPageListener listener) {
    mOnPageListener = listener;
  }

  @Override
  public void setOnMessageListener(OnMessageListener listener) {

  }

  private void showProgressBar(boolean shown) {
    if (mShowLoading) {
      mProgressBar.setVisibility(shown ? View.VISIBLE : View.GONE);
    }
  }

  private void showWebView(boolean shown) {
    mWebView.setVisibility(shown ? View.VISIBLE : View.INVISIBLE);
  }

  public @Nullable
  WebView getWebView() {
    //TODO: remove this, duplicate with getView semantically.
    return mWebView;
  }

  private void initWebView(WebView wv) {
    WebSettings settings = wv.getSettings();
    settings.setAppCacheEnabled(true);
    settings.setUseWideViewPort(true);
    settings.setDomStorageEnabled(true);
    settings.setSupportZoom(false);
    settings.setBuiltInZoomControls(false);
    addWebJavascriptInterface(wv);
    wv.setWebViewClient(new WebViewClient() {

      @Override
      public boolean shouldOverrideUrlLoading(WebView view, String url) {
        view.loadUrl(url);
        WXLogUtils.v("tag", "onPageOverride " + url);
        return true;
      }

      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        WXLogUtils.v("tag", "onPageStarted " + url);
        if (mOnPageListener != null) {
          mOnPageListener.onPageStart(url);
        }
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        WXLogUtils.v("tag", "onPageFinished " + url);
        if (mOnPageListener != null) {
          mOnPageListener.onPageFinish(url, view.canGoBack(), view.canGoForward());
        }
      }

      @Override
      public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
        if (mOnErrorListener != null) {
          //mOnErrorListener.onError("error", "page error code:" + error.getErrorCode() + ", desc:" + error.getDescription() + ", url:" + request.getUrl());
          mOnErrorListener.onError("error", "page error");
        }
      }

      @Override
      public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        super.onReceivedHttpError(view, request, errorResponse);
        if (mOnErrorListener != null) {
          mOnErrorListener.onError("error", "http error");
        }
      }

      @Override
      public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        super.onReceivedSslError(view, handler, error);
        if (mOnErrorListener != null) {
          mOnErrorListener.onError("error", "ssl error");
        }
      }

    });
    wv.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        showWebView(newProgress == 100);
        showProgressBar(newProgress != 100);
        WXLogUtils.v("tag", "onPageProgressChanged " + newProgress);
      }

      @Override
      public void onReceivedTitle(WebView view, String title) {
        super.onReceivedTitle(view, title);
        if (mOnPageListener != null) {
          mOnPageListener.onReceivedTitle(view.getTitle());
        }
      }

      @Override
      public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
        mContentHeight = Integer.parseInt(message);
//                mContentHeight = mContentHeight / (WXViewUtils.defaultPixelScaleFactor(getView().getContext()) * 2);
        Map<String, String> params = new HashMap<>();
        params.put("contentHeight", mContentHeight + "");
        if (mWXWeb != null) {
          mWXWeb.fireEvent("bmPageFinish", params);
        }
        result.confirm();
        return true;
      }

    });
  }

  @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
  private void addWebJavascriptInterface(WebView mWeb) {
    WebSettings settings = mWeb.getSettings();
    settings.setJavaScriptEnabled(true);
    mWeb.addJavascriptInterface(new GlobalWebViewActivity.JSMethod(mContext), "bmnative");
  }

  private double mContentHeight;
  private WXWeb mWXWeb;


  private void evaluateScriptValue() {
    getWebView().loadUrl("javascript:alert(document.body.scrollHeight)");
  }

}
