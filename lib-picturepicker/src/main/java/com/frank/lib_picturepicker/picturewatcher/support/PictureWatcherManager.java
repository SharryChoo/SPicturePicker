package com.frank.lib_picturepicker.picturewatcher.support;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.frank.lib_picturepicker.picturepicker.support.PicturePickerManager;
import com.frank.lib_picturepicker.picturewatcher.PictureWatcherActivity;

import java.util.ArrayList;

import static com.frank.lib_picturepicker.picturewatcher.PictureWatcherActivity.EXTRA_SHARED_ELEMENT;

/**
 * Created by Frank on 2018/6/19.
 * Email: frankchoochina@gmail.com
 * Version: 1.0
 * Description:
 */
public class PictureWatcherManager {

    public static final String TAG = PicturePickerManager.class.getSimpleName();

    public static PictureWatcherManager with(@NonNull Context context) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            return new PictureWatcherManager(activity);
        } else {
            throw new IllegalArgumentException("PictureWatcherManager.with -> Context can not cast to Activity");
        }
    }

    private Activity mActivity;
    private PictureWatcherConfig mConfig;
    private PictureWatcherFragment mPictureWatcherFragment;
    private View mTransitionView;

    private PictureWatcherManager(Activity activity) {
        this.mActivity = activity;
        this.mConfig = new PictureWatcherConfig();
        this.mPictureWatcherFragment = getCallbackFragment(mActivity);
    }

    /**
     * 选择的最大阈值
     */
    public PictureWatcherManager setThreshold(int threshold) {
        mConfig.threshold = threshold;
        return this;
    }

    /**
     * 需要展示的 URI
     */
    public PictureWatcherManager setPictureUri(@NonNull String uri) {
        ArrayList<String> pictureUris = new ArrayList<>();
        pictureUris.add(uri);
        setPictureUris(pictureUris, 0);
        return this;
    }

    /**
     * 需要展示的 URI 集合
     *
     * @param pictureUris 数据集合
     * @param position    展示的位置
     */
    public PictureWatcherManager setPictureUris(@NonNull ArrayList<String> pictureUris, int position) {
        mConfig.pictureUris = pictureUris;
        mConfig.position = position;
        return this;
    }

    /**
     * 设置用户已经选中的图片, 相册会根据 Path 比较, 在相册中打钩
     *
     * @param pickedPictures 已选中的图片
     */
    public PictureWatcherManager setUserPickedSet(@NonNull ArrayList<String> pickedPictures) {
        mConfig.pickedPictures = pickedPictures;
        return this;
    }

    /**
     * 设置共享元素
     */
    public PictureWatcherManager setSharedElement(View transitionView) {
        mTransitionView = transitionView;
        return this;
    }

    /**
     * 设置选择索引的边框颜色
     *
     * @param textColorId 边框的颜色 ID
     */
    public PictureWatcherManager setIndicatorTextColorRes(@ColorRes int textColorId) {
        return setIndicatorTextColor(ContextCompat.getColor(mActivity, textColorId));
    }

    /**
     * 设置选择索引的边框颜色
     *
     * @param textColor 边框的颜色
     */
    public PictureWatcherManager setIndicatorTextColor(@ColorInt int textColor) {
        mConfig.indicatorTextColor = textColor;
        return this;
    }

    /**
     * 设置选择索引的边框颜色
     *
     * @param solidColorId 边框的颜色 ID
     */
    public PictureWatcherManager setIndicatorSolidColorRes(@ColorRes int solidColorId) {
        return setIndicatorSolidColor(ContextCompat.getColor(mActivity, solidColorId));
    }

    /**
     * 设置选择索引的边框颜色
     *
     * @param solidColor 边框的颜色
     */
    public PictureWatcherManager setIndicatorSolidColor(@ColorInt int solidColor) {
        mConfig.indicatorSolidColor = solidColor;
        return this;
    }

    /**
     * 设置选择索引的边框颜色
     *
     * @param checkedColorId   选中的边框颜色
     * @param uncheckedColorId 未选中的边框颜色
     */
    public PictureWatcherManager setIndicatorBorderColorRes(@ColorRes int checkedColorId, @ColorRes int uncheckedColorId) {
        return setIndicatorBorderColor(ContextCompat.getColor(mActivity, checkedColorId),
                ContextCompat.getColor(mActivity, uncheckedColorId));
    }

    /**
     * 设置选择索引的边框颜色
     *
     * @param checkedColor   选中的边框颜色的 Res Id
     * @param uncheckedColor 未选中的边框颜色的Res Id
     */
    public PictureWatcherManager setIndicatorBorderColor(@ColorInt int checkedColor, @ColorInt int uncheckedColor) {
        mConfig.indicatorBorderCheckedColor = checkedColor;
        mConfig.indicatorBorderUncheckedColor = uncheckedColor;
        return this;
    }

    /**
     * 调用图片查看器的方法(共享元素)
     */
    public void start(@NonNull final PictureWatcherCallback callback) {
        mPictureWatcherFragment.verifyPermission(new PictureWatcherFragment.PermissionsCallback() {
            @Override
            public void onResult(boolean granted) {
                if (granted) startActual(callback);
            }
        });
    }

    /**
     * 真正的执行 Activity 的启动
     */
    private void startActual(final PictureWatcherCallback callback) {
        mPictureWatcherFragment.setPickerCallback(callback);
        Intent intent = new Intent(mActivity, PictureWatcherActivity.class);
        intent.putExtra(PictureWatcherActivity.EXTRA_CONFIG, mConfig);
        // 5.0 以上的系统使用 Transition 跳转
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mTransitionView != null) {
                // 共享元素
                intent.putExtra(EXTRA_SHARED_ELEMENT, true);
                mPictureWatcherFragment.startActivityForResult(
                        intent, PictureWatcherFragment.REQUEST_CODE_PICKED,
                        ActivityOptions.makeSceneTransitionAnimation(mActivity, mTransitionView,
                                mConfig.pictureUris.get(mConfig.position)).toBundle()
                );
            } else {
                mPictureWatcherFragment.startActivityForResult(
                        intent, PictureWatcherFragment.REQUEST_CODE_PICKED,
                        ActivityOptions.makeSceneTransitionAnimation(mActivity).toBundle()
                );
            }
        } else {
            mPictureWatcherFragment.startActivityForResult(intent, PictureWatcherFragment.REQUEST_CODE_PICKED);
        }
    }

    /**
     * 获取用于回调的 Fragment
     */
    private PictureWatcherFragment getCallbackFragment(Activity activity) {
        PictureWatcherFragment pictureWatcherFragment = findCallbackFragment(activity);
        if (pictureWatcherFragment == null) {
            pictureWatcherFragment = PictureWatcherFragment.newInstance();
            FragmentManager fragmentManager = activity.getFragmentManager();
            fragmentManager.beginTransaction().add(pictureWatcherFragment, TAG).commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        }
        return pictureWatcherFragment;
    }

    /**
     * 在 Activity 中通过 TAG 去寻找我们添加的 Fragment
     */
    private PictureWatcherFragment findCallbackFragment(Activity activity) {
        return (PictureWatcherFragment) activity.getFragmentManager().findFragmentByTag(TAG);
    }

}
