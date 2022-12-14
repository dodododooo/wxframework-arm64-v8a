package com.eros.framework.manager.impl.status;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.eros.framework.BMWXEnvironment;
import com.eros.framework.activity.AbstractWeexActivity;
import com.eros.framework.model.RouterModel;
import com.eros.widget.utils.BaseCommonUtil;

import qiu.niorgai.StatusBarCompat;

/**
 * Created by Carry on 2017/9/14.
 */

public class StatusBarManager {

    public static boolean isSupportTranslucent(Activity activity) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window win = activity.getWindow();
            WindowManager.LayoutParams winParams = win.getAttributes();
            final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            winParams.flags |= bits;
            win.setAttributes(winParams);
            return true;
        } else {
            return false;
        }
    }


    public static void setStatusBarFontStyle(Activity activity, RouterModel style) {
        if (style == null) return;
        if (style.statusBarStyle == null || "Default".equals(style.statusBarStyle)) {
            Helper.statusBarLightMode(activity);
        }
    }

    public static void setHeaderBg(RouterModel router, AbstractWeexActivity activity) {
        if (router == null) return;
        String defaultColor = BMWXEnvironment.mPlatformConfig.getPage().getNavBarColor();
        if (TextUtils.isEmpty(defaultColor)) {
            defaultColor = "#3385ff";
        }
        if (router == null) return;

        if (router.navShow) {
            //show nav
            if (isSupportTranslucent(activity)) {
                //support
                setStatusBarColor(activity, BaseCommonUtil.getHexColor(defaultColor), 0, activity
                        .getRootView());
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
                    View root = activity.getRootView();
                    setOffset(activity, root, true);
                }
            } else {
                //unSupport

            }
        } else {
            //hide nav
            if (isSupportTranslucent(activity)) {
                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)
                        activity.getRootView().getLayoutParams();
                if (layoutParams == null) {
                    layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams
                            .MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                }
                layoutParams.topMargin = 0;
                translucentStatusBar(activity);
            } else {
                //???????????? ???????????????????????????
                View root = activity.getRootView();
                setOffset(activity, root, false);
            }
        }
    }

    private static void translucentStatusBar(AbstractWeexActivity activity) {
        StatusBarCompat.translucentStatusBar(activity);
    }


    public static void setOffset(Context context, View v, boolean puls) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v
                .getLayoutParams();
        if (layoutParams == null) return;
        int statusBarHeight = getStatusBarHeight(context);
        if (puls) {
            layoutParams.setMargins(0, statusBarHeight, 0, 0);
        } else {
            layoutParams.setMargins(0, -statusBarHeight, 0, 0);
        }

    }

    private static int getStatusBarHeight(Context context) {
        // ?????????????????????
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen",
                "android");
        return context.getResources().getDimensionPixelSize(resourceId);
    }

    public static void setStatusBarColor(Activity activity, int color, int statusBarAlpha, View
            rootView) {
        StatusBarCompat.setStatusBarColor(activity, color);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            activity.getWindow().addFlags(WindowManager.LayoutParams
//                    .FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//            activity.getWindow().setStatusBarColor(calculateStatusColor(color, statusBarAlpha));
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//            ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
//            int count = decorView.getChildCount();
//            if (count > 0 && decorView.getChildAt(count - 1) instanceof StatusBarView) {
//                decorView.getChildAt(count - 1).setBackgroundColor(calculateStatusColor(color,
//                        statusBarAlpha));
//            } else {
//                StatusBarView statusView = createStatusBarView(activity, color, statusBarAlpha);
//                decorView.addView(statusView);
//            }
//        }
//        setRootView(rootView);
    }

    private static int calculateStatusColor(int color, int alpha) {
        float a = 1 - alpha / 255f;
        int red = color >> 16 & 0xff;
        int green = color >> 8 & 0xff;
        int blue = color & 0xff;
        red = (int) (red * a + 0.5);
        green = (int) (green * a + 0.5);
        blue = (int) (blue * a + 0.5);
        return 0xff << 24 | red << 16 | green << 8 | blue;
    }


    public static class StatusBarView extends View {
        public StatusBarView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public StatusBarView(Context context) {
            super(context);
        }
    }

    private static StatusBarView createStatusBarView(Activity activity, int color, int alpha) {
        // ??????????????????????????????????????????
        StatusBarView statusBarView = new StatusBarView(activity);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        getStatusBarHeight(activity));
        statusBarView.setLayoutParams(params);
        statusBarView.setBackgroundColor(calculateStatusColor(color, alpha));
        return statusBarView;
    }

    private static void setRootView(View rootView) {
        if (rootView instanceof ViewGroup) {
            ViewGroup viewGroup = ((ViewGroup) rootView);
            viewGroup.setFitsSystemWindows(true);
            viewGroup.setClipToPadding(true);
        }

    }

}
