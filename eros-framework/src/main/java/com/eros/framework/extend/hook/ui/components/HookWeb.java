package com.eros.framework.extend.hook.ui.components;


import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;

import com.eros.framework.manager.impl.FileManager;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.annotation.Component;
import com.taobao.weex.adapter.URIAdapter;
import com.taobao.weex.common.Constants;
import com.taobao.weex.ui.action.BasicComponentData;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXComponentProp;
import com.taobao.weex.ui.component.WXVContainer;
import com.taobao.weex.ui.view.IWebView;
import com.taobao.weex.utils.WXUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by liuyuanxiao on 2018/7/3.
 */
@Component(lazyload = false)
public class HookWeb extends WXComponent {
    private final String LOCAL_SCHEME = "bmlocal";
    public static final String GO_BACK = "goBack";
    public static final String GO_FORWARD = "goForward";
    public static final String RELOAD = "reload";
    protected IWebView mWebView;

    @Deprecated
    public HookWeb(WXSDKInstance instance, WXVContainer parent, String instanceId, boolean isLazy, BasicComponentData basicComponentData) {
        this(instance, parent, isLazy, basicComponentData);
    }

    public HookWeb(WXSDKInstance instance, WXVContainer parent, boolean isLazy, BasicComponentData basicComponentData) {
        super(instance, parent, isLazy, basicComponentData);
        createWebView();
    }

    protected void createWebView() {
        mWebView = new BMWXWebView(getContext());
    }

    @Override
    protected View initComponentHostView(@NonNull Context context) {
        mWebView.setOnErrorListener(new IWebView.OnErrorListener() {
            @Override
            public void onError(String type, Object message) {
                fireEvent(type, message);
            }
        });
        mWebView.setOnPageListener(new IWebView.OnPageListener() {
            @Override
            public void onReceivedTitle(String title) {
                if (getEvents().contains(Constants.Event.RECEIVEDTITLE)) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("title", title);
                    fireEvent(Constants.Event.RECEIVEDTITLE, params);
                }
            }

            @Override
            public void onPageStart(String url) {
                if (getEvents().contains(Constants.Event.PAGESTART)) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("url", url);
                    fireEvent(Constants.Event.PAGESTART, params);
                }
            }

            @Override
            public void onPageFinish(String url, boolean canGoBack, boolean canGoForward) {
                if (getEvents().contains(Constants.Event.PAGEFINISH)) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("url", url);
                    params.put("canGoBack", canGoBack);
                    params.put("canGoForward", canGoForward);
                    fireEvent(Constants.Event.PAGEFINISH, params);
                }
            }
        });
        return mWebView.getView();
    }

    @Override
    public void destroy() {
        super.destroy();
        getWebView().destroy();
    }

    @Override
    protected boolean setProperty(String key, Object param) {
        switch (key) {
            case Constants.Name.SHOW_LOADING:
                Boolean result = WXUtils.getBoolean(param, null);
                if (result != null)
                    setShowLoading(result);
                return true;
            case Constants.Name.SRC:
                String src = WXUtils.getString(param, null);
                if (src != null)
                    setUrl(src);
                return true;
        }
        return super.setProperty(key, param);
    }

    @WXComponentProp(name = Constants.Name.SHOW_LOADING)
    public void setShowLoading(boolean showLoading) {
        getWebView().setShowLoading(showLoading);
    }

    @WXComponentProp(name = Constants.Name.SRC)
    public void setUrl(String url) {
        if (TextUtils.isEmpty(url) || getHostView() == null) {
            return;
        }
        if (!TextUtils.isEmpty(url)) {
            Uri ul = getInstance().rewriteUri(Uri.parse(url), URIAdapter.WEB);
            if (LOCAL_SCHEME.equalsIgnoreCase(ul.getScheme())) {
                String mUrl = "file://" + localPath(ul);
                loadUrl(mUrl);
            } else {
                loadUrl(ul.toString());
            }
        }
    }


    private String localPath(Uri uri) {
        String path = uri.getHost() + File.separator +
                uri.getPath() + "?" + uri.getQuery();
        return FileManager.getPathBundleDir(getContext(), "bundle/" + path)
                .getAbsolutePath();
    }

    public void setAction(String action) {
        if (!TextUtils.isEmpty(action)) {
            if (action.equals(GO_BACK)) {
                goBack();
            } else if (action.equals(GO_FORWARD)) {
                goForward();
            } else if (action.equals(RELOAD)) {
                reload();
            }
        }
    }

    private void fireEvent(String type, Object message) {
        if (getEvents().contains(Constants.Event.ERROR)) {
            Map<String, Object> params = new HashMap<>();
            params.put("type", type);
            params.put("errorMsg", message);
            fireEvent(Constants.Event.ERROR, params);
        }
    }

    private void loadUrl(String url) {
        getWebView().loadUrl(url);
    }

    private void reload() {
        getWebView().reload();
    }

    private void goForward() {
        getWebView().goForward();
    }

    private void goBack() {
        getWebView().goBack();
    }

    private IWebView getWebView() {
        return mWebView;
    }

}
