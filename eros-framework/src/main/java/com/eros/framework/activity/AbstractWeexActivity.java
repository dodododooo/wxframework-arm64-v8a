package com.eros.framework.activity;

import com.eros.framework.BMWXApplication;
import com.eros.framework.BMWXEnvironment;
import com.eros.framework.BuildConfig;
import com.eros.framework.adapter.router.RouterTracker;
import com.eros.framework.constant.Constant;
import com.eros.framework.constant.WXConstant;
import com.eros.framework.constant.WXEventCenter;
import com.eros.framework.manager.ManagerFactory;
import com.eros.framework.manager.impl.PermissionManager;
import com.eros.framework.manager.impl.dispatcher.DispatchEventManager;
import com.eros.framework.model.AxiosResultBean;
import com.eros.framework.model.UploadResultBean;
import com.eros.framework.model.WeexEventBean;
import com.eros.framework.utils.DebugableUtil;
import com.eros.framework.utils.WXAnalyzerDelegate;
import com.eros.widget.utils.ColorUtils;
import com.eros.widget.view.DebugErrorDialog;
import com.eros.widget.view.loading.LoadingDialog;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.eros.framework.R;
import com.eros.framework.manager.impl.GlobalEventManager;
import com.eros.framework.manager.impl.ImageManager;
import com.eros.framework.manager.impl.PersistentManager;
import com.eros.framework.manager.impl.status.StatusBarManager;
import com.eros.framework.model.CameraResultBean;
import com.eros.framework.model.RouterModel;
import com.eros.framework.model.UploadImageBean;
import com.eros.framework.utils.InsertEnvUtil;
import com.eros.framework.utils.WXCommonUtil;
import com.eros.widget.view.BMFloatingLayer;
import com.eros.widget.view.BMLoding;
import com.eros.widget.view.BaseToolBar;
import com.lzy.imagepicker.ImagePicker;
import com.lzy.imagepicker.bean.ImageItem;
import com.lzy.imagepicker.util.BitmapUtil;
import com.taobao.weex.IWXRenderListener;
import com.taobao.weex.RenderContainer;
import com.taobao.weex.WXEnvironment;
import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.common.WXRenderStrategy;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by Carry on 2017/8/16.
 */

public class AbstractWeexActivity extends AppCompatActivity implements IWXRenderListener, Handler
        .Callback,
        RouterTracker.RouterTrackerListener {
    protected RouterModel mRouterParam;
    private WXSDKInstance mWXInstance;
    protected ViewGroup mContainer;
    private String mPageUrl;
    private static final String TAG = "AbstractWeexActivity";
    private String mPageName;
    private String mRouterType;
    protected BMLoding mLoding;
    private BaseToolBar mNavigationBar;
    private BMFloatingLayer mDebugger;
    protected Activity mAct;
    public String[] mDebugOptions = new String[]{"????????????", "??????", "?????????"};
    private RelativeLayout rl_error;
    private ViewGroup mRootView;
    private ViewGroup decorView;//activity??????View
    private ViewGroup rootView;// mSharedView ??? ???View
    private LoadingDialog loadingDialog;
    private DebugErrorDialog errorDialog;
    private final int EVENT_SINGLE_CILKE = 1;
    private final int EVENT_DOUBLE_CILKE = 2;
    private Handler mHandler = new Handler(this);
    private long mLastTime, mCurTime; // ????????????????????????
    private ImagePicker imagePicker;

    protected WXAnalyzerDelegate mWxAnalyzerDelegate;

    private boolean isStatusBarHidden = false;

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case EVENT_SINGLE_CILKE:
                debugLayerClick();
                break;
            case EVENT_DOUBLE_CILKE:
                refresh();
                break;
        }
        return false;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAct = this;
        mRouterType = GlobalEventManager.TYPE_OPEN;
        Intent data = getIntent();
        initRouterParams(data);
        initUrl(data);
        synRouterStack();
        initDebug();
        imagePicker = ImagePicker.getInstance();

        mWxAnalyzerDelegate = new WXAnalyzerDelegate(this);
        mWxAnalyzerDelegate.onCreate();

    }


    private void initDebug() {
        if (!DebugableUtil.isDebug()) return;
        mDebugger = new BMFloatingLayer(mAct);
        mDebugger.setListener(new BMFloatingLayer.FloatingLayerListener() {
            @Override
            public void onClick() {
                mLastTime = mCurTime;
                mCurTime = System.currentTimeMillis();
                if (mCurTime - mLastTime < 300) {//????????????
                    mCurTime = 0;
                    mLastTime = 0;
                    mHandler.removeMessages(1);
                    mHandler.sendEmptyMessage(2);
                } else {//????????????
                    mHandler.sendEmptyMessageDelayed(1, 310);
                }

            }

            @Override
            public void onShow() {

            }

            @Override
            public void onClose() {

            }
        });
        mDebugger.show(mAct);
    }

    private void debugLayerClick() {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7
                .app.AlertDialog.Builder(mAct);
        builder.setItems(mDebugOptions, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    Intent intent = new Intent(mAct, DebugActivity.class);
                    startActivity(intent);
                } else if (which == 1) {
                    refresh();
                } else if (which == 2) {
                    DispatchEventManager dispatchEventManager = ManagerFactory
                            .getManagerService(DispatchEventManager.class);
                    WeexEventBean eventBean = new WeexEventBean();
                    eventBean.setContext(mAct);
                    eventBean.setKey(WXEventCenter.EVENT_CAMERA);
                    dispatchEventManager.getBus().post(eventBean);
//                    connectionDebugService(BMWXEnvironment.mPlatformConfig.getUrl()
//                            .getDebugServer());

                }
            }
        });
        builder.create().show();
    }

    private void connectionDebugService(String url) {
        WXEnvironment.sDebugServerConnectable = BuildConfig.DEBUG;
        WXEnvironment.sRemoteDebugProxyUrl = url;
        WXSDKEngine.reload();
        Toast.makeText(this, "devtool", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        mRootView = (ViewGroup) View.inflate(this, R.layout.layout_root, null);
        RelativeLayout rl_root = (RelativeLayout) mRootView.findViewById(R.id.rl_root);
        rl_error = (RelativeLayout) mRootView.findViewById(R.id.rl_error);
        mNavigationBar = (BaseToolBar) mRootView.findViewById(R.id.base_navBar);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams
                .MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        View child = View.inflate(this, layoutResID, null);
        rl_root.addView(child, params);
        setContentView(mRootView);
        StatusBarManager.setHeaderBg(mRouterParam, this);
        StatusBarManager.setStatusBarFontStyle(this, mRouterParam);
        setNavigationBar();
    }

    public View getRootView() {
        return mRootView;
    }


    public void setNavigationBar() {
        if (mNavigationBar == null) return;
        //setVisibility
        if (null == mRouterParam) {
            mNavigationBar.setNavigationVisible(false);
            return;
        }
        if (!mRouterParam.navShow) {
            mNavigationBar.setNavigationVisible(false);
            return;
        }
        mNavigationBar.setNavigationVisible(true);
        //set color
        String navBarColor = BMWXEnvironment.mPlatformConfig.getPage().getNavBarColor();
        if (!TextUtils.isEmpty(navBarColor)) {
            mNavigationBar.setBackgroundColor(Color.parseColor(navBarColor));
        }
        //set title
        String title = mRouterParam.navTitle;
        mNavigationBar.setTitle(title);

        //left icon
        mNavigationBar.setOnLeftListenner(new BaseToolBar.OnLeftIconClick() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //back
        mNavigationBar.setLeftIconVisible(mRouterParam.canBack);

        //nav item color
        mNavigationBar.setNavigationItemColor(BMWXEnvironment.mPlatformConfig.getPage()
                .getNavItemColor(), mNavigationBar);

    }


    public BaseToolBar getNavigationBar() {
        return mNavigationBar;
    }


    protected void initRouterParams(Intent data) {

        Serializable serializableExtra = data.getSerializableExtra(Constant.ROUTERPARAMS);
        if (serializableExtra instanceof RouterModel) {
            setRouterParam((RouterModel) serializableExtra);
        }


    }


    public void showError() {
        rl_error.setVisibility(View.VISIBLE);
    }


    public void hideError() {
        rl_error.setVisibility(View.GONE);
    }

    protected void initUrl(Intent data) {
        Uri pageUri = data.getData();
        if (pageUri == null) return;
        setPageUrl(pageUri.toString());
    }

    public void setRouterParam(RouterModel param) {
        this.mRouterParam = param;
    }

    public RouterModel getRouterParam() {
        return mRouterParam;
    }

    protected void synRouterStack() {
        if (mRouterParam != null) {
            if (!Constant.ACTIVITIES_ANIMATION.ANIMATION_PRESENT.equals(mRouterParam.type)) {
                onAttach(this);
            } else {
                onAttach(this, getClass().getName());
            }
        } else {
            onAttach(this);
        }
        //??????????????????
        ManagerFactory.getManagerService(DispatchEventManager.class).getBus().post(new Intent
                (WXConstant.ACTION_ACTIVITY_ATTACH));
    }

    public void refresh() {
        if (mWXInstance == null) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DispatchEventManager dispatchEventManager = ManagerFactory.getManagerService
                        (DispatchEventManager.class);
                Intent intent = new Intent(WXConstant.ACTION_WEEX_REFRESH);
                intent.putExtra("instanceId", mWXInstance.getInstanceId());
                dispatchEventManager.getBus().post(intent);
                createWXInstance();
                renderPage();
            }
        });

    }

    //??????SVProgressHUD loadingView
    private void createLoadingView() {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        decorView = (ViewGroup) (this).getWindow().getDecorView().findViewById(android.R.id
                .content);
        rootView = (ViewGroup) layoutInflater.inflate(com.eros.R.layout
                .layout_svprogresshud, null, false);
        rootView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    public ViewGroup getLoadingDecorView() {
        if (decorView == null) {
            createLoadingView();
        }
        return decorView;
    }

    public ViewGroup getLoadingRootView() {
        if (rootView == null) {
            createLoadingView();
        }
        return rootView;
    }

    public void showLoadingDialog(String msg) {
        if (loadingDialog == null) {
            loadingDialog = new LoadingDialog();
            loadingDialog.createLoadingDialog(this, msg);
        }
        loadingDialog.setTipText(msg);
        loadingDialog.show();

    }


    public void closeDialog() {
        if (loadingDialog != null) {
            loadingDialog.dismiss();
        }
    }

    public void setPageUrl(String url) {
        this.mPageUrl = url;
    }


    public String getPageUrl() {
        return mPageUrl;
    }


    protected void renderPage() {
        if (TextUtils.isEmpty(mPageUrl)) {
            return;
        }
        createWXInstance();
        preRender();
        renderPageByURL();
        postRender();
    }

    private void renderPageByURL() {
        WXCommonUtil.throwIfNull(mContainer, new RuntimeException("Can't render page, container " +
                "is" +
                " null"));
        Map<String, Object> options = new HashMap<>();
        options.put(WXSDKInstance.BUNDLE_URL, mPageUrl);
        InsertEnvUtil.customerRender(options);
        mWXInstance.renderByUrl(
                getPageName(),
                mPageUrl,
                options,
                null,
                WXRenderStrategy.APPEND_ASYNC);
    }


    public String getPageName() {
        return mPageName == null ? TAG : mPageName;
    }


    public void setPageName(String pageName) {
        this.mPageName = pageName;
    }


    protected void preRender() {

    }

    protected void postRender() {

    }

    public WXSDKInstance getWXSDkInstance() {
        return mWXInstance;
    }

    protected void createWXInstance() {
        if (mWXInstance != null) {
            destroyWXInstance();
        }
        RenderContainer renderContainer = new RenderContainer(this);
        if (mRouterParam != null && !TextUtils.isEmpty(mRouterParam.backgroundColor)) {
            renderContainer.setBackgroundColor(ColorUtils.getColor(mRouterParam.backgroundColor));
        }
        mContainer.addView(renderContainer);
        mWXInstance = new WXSDKInstance(this);
        mWXInstance.registerRenderListener(this);
        mWXInstance.setRenderContainer(renderContainer);

    }

    protected void destroyWXInstance() {
        if (mWXInstance != null) {
            Intent intent = new Intent(WXEventCenter.EVENT_INSTANCE_DESTORY);
            intent.putExtra("data", mWXInstance.getInstanceId());
            ManagerFactory.getManagerService(DispatchEventManager.class).getBus().post(intent);
            mWXInstance.registerRenderListener(null);
            mWXInstance.registerOnWXScrollListener(null);
            mWXInstance.destroy();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mRouterType = GlobalEventManager.TYPE_BACK;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mWXInstance != null) {
            mWXInstance.onActivityResume();
        }

        if (mWXInstance != null) {
            GlobalEventManager.onViewDidAppear(mWXInstance, mRouterType);
        }
        if (mWxAnalyzerDelegate != null) {
            mWxAnalyzerDelegate.onResume();
        }

//        MobclickAgent.onResume(this);

        ManagerFactory.getManagerService(DispatchEventManager.class).getBus().post
                (new Intent(Constant.Action
                        .ACTION_AUTHLOGIN_CANCEL));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mWXInstance != null) {
            mWXInstance.onActivityStart();
        }

        if (mWXInstance != null) {
            GlobalEventManager.onViewWillAppear(mWXInstance, mRouterType);
        }

        if (mWxAnalyzerDelegate != null) {
            mWxAnalyzerDelegate.onStart();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mWXInstance != null) {
            mWXInstance.onActivityPause();
        }

        if (mWXInstance != null) {
            GlobalEventManager.onViewWillDisappear(mWXInstance, mRouterType);
        }

        if (mWxAnalyzerDelegate != null) {
            mWxAnalyzerDelegate.onPause();
        }
//        MobclickAgent.onPause(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mWXInstance != null) {
            mWXInstance.onActivityStop();
        }

        if (mWXInstance != null) {
            GlobalEventManager.onViewDidDisappear(mWXInstance, mRouterType);
        }

        if (mWxAnalyzerDelegate != null) {
            mWxAnalyzerDelegate.onStop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RouterTracker.autoRemoveActivity(this);
        if (mWXInstance != null) {
            mWXInstance.onActivityDestroy();
        }
        if (mDebugger != null) {
            mDebugger.close();
        }

        if (mWxAnalyzerDelegate != null) {
            mWxAnalyzerDelegate.onDestroy();
        }

//        BMWXApplication.getWXApplication().getWatcher().watch(this);
    }

    @Override
    public void onViewCreated(WXSDKInstance instance, View view) {
        View wrappedView = null;
        if (mWxAnalyzerDelegate != null) {
            wrappedView = mWxAnalyzerDelegate.onWeexViewCreated(mWXInstance, view);
        }

        if (wrappedView != null) {
            view = wrappedView;
        }
        if (view != null && view.getParent() == null) {
            mContainer.addView(view);
        }
        if (view instanceof RenderContainer) {
            RenderContainer container = (RenderContainer) view;
            int childCount = container.getChildCount();
            if (childCount > 0) {
                container.getChildAt(0).setBackgroundColor(ContextCompat.getColor(this, R.color
                        .c_eff3f4));
            }
        }
        mContainer.requestLayout();
        GlobalEventManager.onViewWillAppear(mWXInstance, mRouterType);
    }

    @Override
    public void onRenderSuccess(WXSDKInstance instance, int width, int height) {
        //do some report
        GlobalEventManager.onViewDidAppear(mWXInstance, mRouterType);

        if (mWxAnalyzerDelegate != null) {
            mWxAnalyzerDelegate.onWeexRenderSuccess(instance);
        }
    }

    @Override
    public void onRefreshSuccess(WXSDKInstance instance, int width, int height) {

    }

    @Override
    public void onException(WXSDKInstance instance, String errCode, String msg) {
        if (!DebugableUtil.isDebug()) return;
        if (errorDialog == null) {
            errorDialog = new DebugErrorDialog();
            errorDialog.createErrorDialog(this);
        }
        String errorMsg = "errCode -> " + errCode + " msg -> " + msg;
        errorDialog.setTextMsg(errorMsg);
        errorDialog.show();

        if (mWxAnalyzerDelegate != null) {
            mWxAnalyzerDelegate.onException(instance, errCode, msg);
        }

    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return (mWxAnalyzerDelegate != null && mWxAnalyzerDelegate.onKeyUp(keyCode, event)) ||
                super.onKeyUp(keyCode, event);
    }

    @Override
    public void onAttach(Activity activity) {
        RouterTracker.push(activity);
    }

    @Override
    public void onAttach(Activity activity, String activityName) {
        RouterTracker.newInstancePush(activity, activityName);
    }

    @Override
    public void onDetach(Activity activity, boolean force) {
        if (activity == this) {
            if (force)
                finish();
            if (mRouterParam != null) {
                String type = mRouterParam.type;
                if (Constant.ACTIVITIES_ANIMATION.ANIMATION_PUSH.equals(type)) {
                    overridePendingTransition(R.anim.view_stay, R.anim.right_out);
                } else if (Constant.ACTIVITIES_ANIMATION.ANIMATION_PRESENT.equals(type)) {
                    overridePendingTransition(R.anim.view_stay, R.anim.bottom_out);
                } else if (Constant.ACTIVITIES_ANIMATION.ANIMATION_TRANSLATION.equals(type)) {
                    overridePendingTransition(R.anim.view_stay, R.anim.left_out);
                }
            }
        }
    }

    @Override
    public void onDetach(Activity activity, String activityName) {

    }

    @Override
    public void onBackPressed() {
        if (mRouterParam != null && mRouterParam.isRunBackCallback) {
            if (mRouterParam.backCallback != null) {
                mRouterParam.backCallback.invoke(null);
                return;
            }
        }

        if (mRouterParam != null && !mRouterParam.canBack) {
            return;
        }

        RouterTracker.popActivity();

    }


    public BMLoding getLoading() {
        return mLoding;
    }


    public void setLoading(BMLoding bmLoading) {
        this.mLoding = bmLoading;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                handleDecodeInternally(result.getContents());
            }
        }
        /**
         * ?????????????????????
         */
        if (resultCode == RESULT_OK && requestCode == Constant.REQUEST_CODE.REQUEST_CODE_CONTRACT) {
            readContractResult(data);
        }
        /**
         * ??????????????????
         */
        if (resultCode == RESULT_OK && requestCode == ImagePicker.REQUEST_CODE_TAKE) {
            cameraResult();
            return;
        }
        /**
         * ???????????? ??????
         */
        switch (resultCode) {
            case ImagePicker.RESULT_CODE_ITEMS:
                if (data != null && requestCode == Constant.ImageConstants.IMAGE_PICKER) {
                    ArrayList<ImageItem> items = (ArrayList<ImageItem>) data
                            .getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
                    UpMultipleImageData(items);
                }
                break;
        }
        /**
         * ???????????? ??????
         */
        switch (resultCode) {
            case ImagePicker.RESULT_CODE_ITEMS:
                if (data != null && requestCode == Constant.ImageConstants
                        .IMAGE_NOT_UPLOADER_PICKER) {
                    ArrayList<ImageItem> items = (ArrayList<ImageItem>) data
                            .getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
                    pickReturn(items);
                }
                break;
        }
//        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void readContractResult(Intent data) {
        String username, usernumber = "";
        ContentResolver reContentResolverol = getContentResolver();
        //URI,??????ContentProvider??????????????????????????????URI,??????????????????????????????
        Uri contactData = data.getData();
        //??????????????????URI?????????,??????URI????????????,??????????????????,?????????????????????URI?????????ContentProvider???????????????Cursor??????.
        Cursor cursor = managedQuery(contactData, null, null, null, null);
        cursor.moveToFirst();
        //??????DATA???????????????
        username = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        //??????????????????ID
        String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
        // ??????DATA??????????????????????????????????????????ID,????????????????????????????????????
        Cursor phone = reContentResolverol.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
                null,
                null);
        while (phone.moveToNext()) {
            usernumber = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds
                    .Phone.NUMBER));
        }
        String json = joinContractJson(username, usernumber);
        AxiosResultBean resultBean = new AxiosResultBean();
        resultBean.status = 0;
        resultBean.data = json;

        DispatchEventManager dispatchEventManager = ManagerFactory.getManagerService
                (DispatchEventManager.class);
        dispatchEventManager.getBus().post(resultBean);
    }

    private String joinContractJson(String name, String poneNumber) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", name);
            jsonObject.put("phone", poneNumber);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    /**
     * ???????????????????????????
     */
    private void pickReturn(ArrayList<ImageItem> items) {
        UploadResultBean bean = new UploadResultBean();
        List<String> data = new ArrayList<>();
        for (ImageItem path : items) {
            data.add(path.path);
        }
        bean.data = data;
        ManagerFactory.getManagerService(DispatchEventManager.class).getBus().post(bean);
    }

    /**
     * ????????????
     */
    private void UpMultipleImageData(ArrayList<ImageItem> items) {
        ImageManager imageManager = ManagerFactory.getManagerService(ImageManager
                .class);
        UploadImageBean bean = ManagerFactory.getManagerService
                (PersistentManager.class).getCacheData
                (Constant.ImageConstants.UPLOAD_IMAGE_BEAN, UploadImageBean.class);
        imageManager.UpMultipleImageData(this, items, bean);
    }

    /**
     * ??????????????????????????????
     */
    private void cameraResult() {
        ImagePicker.galleryAddPic(this, this.imagePicker.getTakeImageFile());
        String path = this.imagePicker.getTakeImageFile().getAbsolutePath();
        int degree = BitmapUtil.getBitmapDegree(path);
        if (degree != 0) {
            Bitmap bitmap = BitmapUtil.rotateBitmapByDegree(path, degree);
            if (bitmap != null) {
                File file = new File(path);

                try {
                    FileOutputStream bos = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                    bos.flush();
                    bos.close();
                } catch (IOException var9) {
                    var9.printStackTrace();
                }
            }
        }

        UploadResultBean uploadResultBean = new UploadResultBean();
        uploadResultBean.resCode = 0;
        uploadResultBean.msg = "????????????";
        List<String> dataList = new ArrayList<>();
        dataList.add(path);
        uploadResultBean.setData(dataList);
        ManagerFactory.getManagerService(DispatchEventManager.class).getBus().post
                (uploadResultBean);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        PermissionManager permissionManager = ManagerFactory.getManagerService(PermissionManager
                .class);
        permissionManager.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    private void handleDecodeInternally(String code) {
        if (!TextUtils.isEmpty(code)) {
            Log.d("handleDecodeInternally", " String code -> " + code);
            Uri uri = Uri.parse(code);
            try {
                if (uri.getQueryParameterNames().contains("bundle")) {
                    WXEnvironment.sDynamicMode = uri.getBooleanQueryParameter("debug", false);
                    WXEnvironment.sDynamicUrl = uri.getQueryParameter("bundle");
                    String tip = WXEnvironment.sDynamicMode ? "Has switched to Dynamic Mode" :
                            "Has " +
                                    "switched to Normal Mode";
                    Toast.makeText(this, tip, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                } else if (uri.getQueryParameterNames().contains("_wx_devtool")) {
                    WXEnvironment.sRemoteDebugProxyUrl = uri.getQueryParameter("_wx_devtool");
                    WXEnvironment.sDebugServerConnectable = true;
                    WXSDKEngine.reload();
                    Toast.makeText(this, "devtool", Toast.LENGTH_SHORT).show();
//                connectionDebugService(uri.getQueryParameter("_wx_devtool"));
                    return;
                } else if (code.contains("_wx_debug")) {
                    uri = Uri.parse(code);
                    String debug_url = uri.getQueryParameter("_wx_debug");
//                WXSDKEngine.switchDebugModel(true, debug_url);
                    finish();
                } else {
                    postBusScanCode(code);
                }
            } catch (Exception e) {
                Log.e(TAG, " handleDecodeInternally Exception -> " + e.getMessage());
                postBusScanCode(code);
            }
        }
    }

    private void postBusScanCode(String code) {
        CameraResultBean bean = new CameraResultBean();
        if (!TextUtils.isEmpty(code)) {
            bean.text = code;
        } else {
            bean.text = "";
        }

        ManagerFactory.getManagerService(DispatchEventManager.class).getBus
                ().post(bean);
    }


    protected boolean isHomePage() {
        String homePage = BMWXEnvironment.mPlatformConfig.getPage().getHomePage(this);
        homePage = BMWXEnvironment.mPlatformConfig.getUrl().getJsServer() +
                "/dist/js" + homePage;
        return homePage.equals(this.mPageUrl);
    }

    /**
     * navigation ??????????????????????????????????????? fragment
     */
    public boolean navigationListenter(WeexEventBean weexEventBean) {
        return false;
    }

    public void statusBarHidden(boolean isFullScreen){
        if (isFullScreen) {
            setFullScreen(this);
        } else {
            quitFullScreen(this);
        }
    }

    private void setFullScreen(Activity activity) {
        if (!isStatusBarHidden) {
            activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            isStatusBarHidden = true;
        }
    }


    private void quitFullScreen(Activity activity) {
        if (isStatusBarHidden) {
            final WindowManager.LayoutParams attrs = activity.getWindow().getAttributes();
            attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
            activity.getWindow().setAttributes(attrs);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            isStatusBarHidden = false;
        }
    }

}
