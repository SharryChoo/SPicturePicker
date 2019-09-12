package com.sharry.lib.picturepicker;

import android.os.Handler;
import android.os.Looper;

import java.text.MessageFormat;
import java.util.ArrayList;

/**
 * The presenter associated with PictureWatcher.
 *
 * @author Sharry <a href="SharryChooCHN@Gmail.com">Contact me.</a>
 * @version 1.0
 * @since 2019/3/15 21:56
 */
class WatcherPresenter implements WatcherContract.IPresenter {

    /**
     * Final fields.
     */
    private final WatcherContract.IView mView;
    private final WatcherConfig mConfig;
    private final ArrayList<MediaMeta> mDisplayMetas;
    private final ArrayList<MediaMeta> mPickedMetas;
    private final SharedElementModel mSharedElementModel;

    private int mCurPosition;
    private MediaMeta mCurDisplay;
    private boolean mIsEnsurePressed = false;

    WatcherPresenter(WatcherContract.IView view, WatcherConfig config, SharedElementModel sharedElementModel) {
        this.mView = view;
        this.mConfig = config;
        this.mSharedElementModel = sharedElementModel;
        // 获取需要展示图片的 URI 集合
        this.mDisplayMetas = config.getPictureUris();
        // 获取已经选中的图片
        this.mPickedMetas = config.getUserPickedSet();
        // 获取当前需要展示的 Position 和 URI
        this.mCurPosition = config.getPosition();
        this.mCurDisplay = mDisplayMetas.get(mCurPosition);
        // 配置视图
        setupViews();
    }

    @Override
    public void handlePagerChanged(int position) {
        // 更新数据
        mCurPosition = position;
        mCurDisplay = mDisplayMetas.get(position);
        // 展示 Toolbar 左边的指示文本
        mView.displayToolbarLeftText(buildToolbarLeftText());
        // 展示图片
        mView.displayAt(mCurPosition);
        if (mConfig.isPickerSupport()) {
            mView.setToolbarIndicatorChecked(mPickedMetas.indexOf(mCurDisplay) != -1);
            mView.displayToolbarIndicatorText(buildToolbarCheckedIndicatorText());
            mView.displayPreviewEnsureText(buildEnsureText());
        }
    }

    @Override
    public void handleToolbarCheckedIndicatorClick(boolean isChecked) {
        if (isChecked) {
            // 移除选中数据与状态
            int removedIndex = mPickedMetas.indexOf(mCurDisplay);
            if (removedIndex < 0) {
                return;
            }
            mPickedMetas.remove(removedIndex);
            // 通知 RecyclerView 数据变更
            mView.notifyBottomPicturesRemoved(mCurDisplay, removedIndex);
        } else {
            // 判断是否达到选择上限
            if (mPickedMetas.size() < mConfig.getThreshold()) {
                mPickedMetas.add(mCurDisplay);
                int addedIndex = mPickedMetas.indexOf(mCurDisplay);
                // 通知 RecyclerView 数据变更
                mView.notifyBottomPictureAdded(mCurDisplay, addedIndex);
                mView.previewPicturesSmoothScrollToPosition(addedIndex);
            } else {
                mView.showMsg(
                        mView.getString(R.string.picture_picker_watcher_tips_over_threshold_prefix) +
                                mConfig.getThreshold() +
                                mView.getString(R.string.picture_picker_watcher_tips_over_threshold_suffix)
                );
            }
        }
        mView.setToolbarIndicatorChecked(mPickedMetas.indexOf(mCurDisplay) != -1);
        mView.displayToolbarIndicatorText(buildToolbarCheckedIndicatorText());
        mView.displayPreviewEnsureText(buildEnsureText());
        // 控制底部导航栏的展示
        if (mPickedMetas.isEmpty()) {
            mView.dismissBottomPreview();
        } else {
            mView.showBottomPreview();
        }
    }

    @Override
    public void handlePickedItemClicked(MediaMeta meta) {
        int index = mDisplayMetas.indexOf(meta);
        if (index >= 0) {
            handlePagerChanged(index);
        }
    }

    @Override
    public void handleEnsureClick() {
        if (mPickedMetas.isEmpty()) {
            mView.showMsg(mView.getString(R.string.picture_picker_watcher_tips_ensure_failed));
            return;
        }
        mIsEnsurePressed = true;
        mView.finish();
    }

    @Override
    public void handleBackPressed() {
        if (mSharedElementModel != null && mCurPosition == mSharedElementModel.sharedPosition) {
            mView.showSharedElementExitAndFinish(mSharedElementModel);
            mView.dismissBottomPreview();
        } else {
            mView.finish();
        }
    }

    @Override
    public void handleSetResultBeforeFinish() {
        mView.setResultBeforeFinish(mPickedMetas, mIsEnsurePressed);
    }

    private void setupViews() {
        // 1. 设置 Toolbar 数据
        mView.displayToolbarLeftText(buildToolbarLeftText());
        mView.setToolbarCheckedIndicatorVisibility(mConfig.isPickerSupport());

        // 2. 设置 Pictures 数据
        mView.setDisplayAdapter(mDisplayMetas);
        mView.displayAt(mCurPosition);

        // 3. 设置底部菜单和按钮选中的状态
        if (mConfig.isPickerSupport()) {
            mView.setToolbarCheckedIndicatorColors(
                    mConfig.getIndicatorBorderCheckedColor(),
                    mConfig.getIndicatorBorderUncheckedColor(),
                    mConfig.getIndicatorSolidColor(),
                    mConfig.getIndicatorTextColor()
            );
            mView.setToolbarIndicatorChecked(mPickedMetas.indexOf(mCurDisplay) != -1);
            mView.displayToolbarIndicatorText(buildToolbarCheckedIndicatorText());
            // 底部菜单
            mView.setPreviewAdapter(mPickedMetas);
            mView.displayPreviewEnsureText(buildEnsureText());
            // 底部菜单延时弹出
            if (!mPickedMetas.isEmpty()) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mView.showBottomPreview();
                    }
                }, mSharedElementModel != null ? 500 : 0);
            }
        }
        // 4. 执行共享元素入场动画
        if (mSharedElementModel != null) {
            mView.showSharedElementEnter(mDisplayMetas.get(mCurPosition), mSharedElementModel);
        }
    }

    /**
     * 构建 Toolbar 左边的文本
     */
    private CharSequence buildToolbarLeftText() {
        return MessageFormat.format("{0}/{1}", mCurPosition + 1, mDisplayMetas.size());
    }

    /**
     * 构建 Toolbar checked Indicator 的文本
     */
    private CharSequence buildToolbarCheckedIndicatorText() {
        return String.valueOf(mPickedMetas.indexOf(mCurDisplay) + 1);
    }

    /**
     * 构建确认按钮文本
     */
    private CharSequence buildEnsureText() {
        return MessageFormat.format("{0}({1}/{2})",
                mView.getString(R.string.picture_picker_watcher_ensure), mPickedMetas.size(), mConfig.getThreshold());
    }

}
