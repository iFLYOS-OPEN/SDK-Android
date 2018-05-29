package com.iflytek.cyber.iot.show.core.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.iflytek.cyber.iot.show.core.R;

public class RecognizeBar extends FrameLayout {
    private ImageView recognizeButton;
    private RecognizeWave recognizeWave;

    private OnWakeUpButtonClickListener onWakeUpButtonClickListener;

    private boolean isWakeUpState = false;

    public RecognizeBar(@NonNull Context context) {
        super(context);
        init();
    }

    public RecognizeBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RecognizeBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setBackgroundColor(Color.argb(1, 0, 0, 0));

        recognizeButton = new ImageView(getContext());
        recognizeButton.setImageResource(R.drawable.ic_logo_border);
        int recognizeButtonSize = getResources().getDimensionPixelSize(R.dimen.bottom_bar_logo_size);
        int recognizeButtonPadding = getResources().getDimensionPixelSize(R.dimen.bottom_bar_logo_padding);
        recognizeButton.setPadding(recognizeButtonPadding, recognizeButtonPadding, recognizeButtonPadding, recognizeButtonPadding);
        LayoutParams layoutParams = new LayoutParams(recognizeButtonSize, recognizeButtonSize);
        layoutParams.gravity = Gravity.CENTER;
        int[] attrs = new int[]{R.attr.selectableItemBackgroundBorderless};
        TypedArray ta = getContext().getTheme().obtainStyledAttributes(attrs);
        Drawable selectableItemBackgroundBorderless = ta.getDrawable(0);
        recognizeButton.setBackground(selectableItemBackgroundBorderless);
        recognizeButton.setOnClickListener(v -> {
            if (onWakeUpButtonClickListener != null) {
                boolean result = onWakeUpButtonClickListener.onWakeUpButtonClick(this);
                if (!result)
                    return;
                if (!isWakeUpState)
                    animateToWakeUp();
                else
                    animateFromWakeUp();
                isWakeUpState = !isWakeUpState;
            }
        });
        addView(recognizeButton, layoutParams);

        recognizeWave = new RecognizeWave(getContext());
        LayoutParams waveParams = new LayoutParams(LayoutParams.MATCH_PARENT, recognizeButtonSize);
        waveParams.gravity = Gravity.CENTER;
        addView(recognizeWave, waveParams);
    }

    private void animateToWakeUp() {
        if (recognizeButton.getAlpha() != 1)
            return;
        recognizeButton.setEnabled(false);
        recognizeButton.animate().alpha(0).setDuration(200)
                .withEndAction(() -> recognizeButton.setEnabled(true)).start();

        recognizeWave.animateToWaving();
    }

    private void animateFromWakeUp() {
        if (recognizeButton.getAlpha() != 0)
            return;
        recognizeButton.setEnabled(false);
        recognizeButton.animate().alpha(1).setDuration(200)
                .withEndAction(() -> recognizeButton.setEnabled(true)).start();

        recognizeWave.animateFromWaving();
    }

    public OnWakeUpButtonClickListener getOnWakeUpButtonClickListener() {
        return onWakeUpButtonClickListener;
    }

    public void setOnWakeUpButtonClickListener(OnWakeUpButtonClickListener onWakeUpButtonClickListener) {
        this.onWakeUpButtonClickListener = onWakeUpButtonClickListener;
    }

    public void stopWaving() {
        animateFromWakeUp();
    }

    public void startWaving() {
        animateToWakeUp();
    }

    public void updateVolume(int level) {
        recognizeWave.updateVolume(level);
    }

    public interface OnWakeUpButtonClickListener {
        /**
         * if do nothing, should return false
         *
         * @param view recognizeBar
         * @return result
         */
        boolean onWakeUpButtonClick(View view);
    }
}
