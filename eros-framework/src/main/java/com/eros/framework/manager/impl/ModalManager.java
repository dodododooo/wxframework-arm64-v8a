package com.eros.framework.manager.impl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.eros.framework.R;
import com.eros.framework.activity.AbstractWeexActivity;
import com.eros.framework.manager.Manager;
import com.eros.widget.view.BMAlert;
import com.eros.widget.view.BMGridDialog;
import com.eros.widget.view.BMLoding;

import java.util.List;


/**
 * Created by Carry on 2017/8/7.
 */

public class ModalManager extends Manager {
    public static class BmAlert {
        private static AlertDialog mBmAlert = null;

        public static void showAlert(Context context, String title, String message, String okBtn,
                                     DialogInterface.OnClickListener okListenner, String cancelBtn,
                                     DialogInterface.OnClickListener cancelListenner, String
                                             titleAlign, String contentAlign, boolean cancel) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(title).setMessage(message).setPositiveButton(okBtn, okListenner)
                    .setCancelable(cancel);
            if (!TextUtils.isEmpty(cancelBtn)) {
                builder.setNegativeButton(cancelBtn, cancelListenner);
            }
            mBmAlert = builder.create();
            if (mBmAlert != null && !mBmAlert.isShowing() && !((Activity) context).isFinishing()) {
                mBmAlert.show();
            }
        }

        public static void showAlert(Context context, String title, String message, String okBtn,
                                     DialogInterface.OnClickListener okListenner, String cancelBtn,
                                     DialogInterface.OnClickListener cancelListenner, String
                                             titleAlign, String contentAlign) {
            showAlert(context, title, message, okBtn, okListenner, cancelBtn, cancelListenner,
                    titleAlign, contentAlign, true);
        }
    }

    public static class BmLoading {

        public static void showLoading(Context context, final String message, boolean
                canWatchOutsideTouch) {
            if (context instanceof AbstractWeexActivity) {
                final AbstractWeexActivity activity = (AbstractWeexActivity) context;
                if (activity.isFinishing()) return;
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    activity.showLoadingDialog(message);
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.showLoadingDialog(message);
                        }
                    });
                }
            }
        }

        public static void dismissLoading(Context context) {
            if (context instanceof AbstractWeexActivity) {
                final AbstractWeexActivity activity = (AbstractWeexActivity) context;
                if (activity.isFinishing()) return;
                activity.closeDialog();
                if (Looper.myLooper() == Looper.getMainLooper()) {
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            activity.closeDialog();
                        }
                    });
                }
            }

        }
    }

    public static class BmToast {

        private static void makeToast(final Context context, final String message, final int
                duration) {

            if (TextUtils.isEmpty(message) || context == null) {
                return;
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Toast mToast = Toast.makeText(context, message, duration);
                mToast.setDuration(duration);
                mToast.setText(message);
                mToast.show();
            } else {
                Log.i("BMModalManager", "toast can not show in child thread");
                if (context instanceof Activity) {
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            makeToast(context, message, duration);
                        }
                    });
                }
            }
        }

        public static void toast(Context context, String message, int duration) {
            makeToast(context, message, duration);
        }

    }

    public static class BmShareDialog {

        private static BMGridDialog mDialog;

        public static void show(Activity activity, List<BMGridDialog.GridItem> list, BMGridDialog
                .OnItemClickListener onItemClickListener) {
            if (list == null) return;
            BMGridDialog.Builder builder = new BMGridDialog.Builder(activity, R.style
                    .ActionSheetDialogStyle);
            mDialog = builder.setGravity(Gravity.BOTTOM).setNegativeButton("??????",
                    null).setAdapter(new
                    BMGridDialog.Adapter(activity, list, 4)).setOnItemClickListenner
                    (onItemClickListener).build();
            mDialog.show();
        }

        public static void dismiss() {
            if (mDialog != null) {
                mDialog.hide();
            }
        }
    }
}
