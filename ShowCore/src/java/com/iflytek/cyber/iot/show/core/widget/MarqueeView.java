package com.iflytek.cyber.iot.show.core.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.FloatEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.iflytek.cyber.iot.show.core.R;


public class MarqueeView extends FrameLayout {

    private String[] marqueeData;
    private int internal = 5000;
    private int textSize = 16;
    private int textColor = Color.BLACK;
    private int textGravity = Gravity.START;
    private TextView child1;
    private TextView child2;
    private boolean isRunningAnim = false;
    private int currentPosition = 0;
    private FloatEvaluator floatEval = new FloatEvaluator();

    public MarqueeView(Context context) {
        this(context, null);
    }

    public MarqueeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MarqueeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MarqueeView_MarqueeStyle, defStyleAttr, 0);
        internal = typedArray.getInteger(R.styleable.MarqueeView_MarqueeStyle_marqueeInterval, internal);
        textSize = typedArray.getDimensionPixelSize(R.styleable.MarqueeView_MarqueeStyle_marqueeTextSize, textSize);
        textColor = typedArray.getColor(R.styleable.MarqueeView_MarqueeStyle_marqueeTextColor, textColor);
        int gravity = typedArray.getInt(R.styleable.MarqueeView_MarqueeStyle_marqueeTextGravity, 0);
        switch (gravity) {
            case 0:
                textGravity = Gravity.START;
                break;
            case 1:
                textGravity = Gravity.CENTER;
                break;
            case 2:
                textGravity = Gravity.END;
                break;
        }

        typedArray.recycle();

        initChild();
    }

    public void setInterval(int internal) {
        this.internal = internal;
    }

    public void setMarqueeData(String[] marqueeData) {
        this.marqueeData = marqueeData;

        start();
    }

    private void initChild() {
        if (getChildCount() == 2) {
            return;
        }
        child1 = createTextView();
        child2 = createTextView();

        addView(child1);
        addView(child2);

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeGlobalOnLayoutListener(this);
                ViewCompat.setTranslationY(child2, child2.getHeight());
                child1.setText(marqueeData[currentPosition]);
                child2.setText(marqueeData[currentPosition + 1]);
                currentPosition += 1;
            }
        });
    }

    private TextView createTextView() {
        TextView textView = new TextView(getContext());
        textView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        textView.setSingleLine();
        textView.setTextColor(textColor);
        textView.setGravity(Gravity.CENTER_VERTICAL | textGravity);
        return textView;
    }

    public void start() {
        if (marqueeData == null || marqueeData.length == 0) return;
        if (isRunningAnim) return;

        postDelayed(translationTask, internal);
    }

    public void stop() {
        removeCallbacks(translationTask);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stop();
    }

    Runnable translationTask = new Runnable() {
        @Override
        public void run() {
            //doAnim
            final float startY1 = child1.getTranslationY();
            final float startY2 = child2.getTranslationY();
            final float endY1 = startY1 == 0 ? -getHeight() : 0;
            final float endY2 = startY2 == 0 ? -getHeight() : 0;

            ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
            animator.addUpdateListener(animation -> {
                float fraction = animation.getAnimatedFraction();
                child1.setTranslationY(floatEval.evaluate(fraction, startY1, endY1));
                child2.setTranslationY(floatEval.evaluate(fraction, startY2, endY2));
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    isRunningAnim = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    currentPosition += 1;
                    if (currentPosition > (marqueeData.length - 1)) {
                        currentPosition = 0;
                    }

                    TextView moveView = (child1.getTranslationY() == -getHeight() ? child1 : child2);
                    moveView.setTranslationY(getHeight() * 2);
                    moveView.setText(marqueeData[currentPosition]);

                    //again
                    postDelayed(translationTask, internal);

                    isRunningAnim = false;
                }
            });
            animator.setDuration(getHeight() * 6)
                    .setInterpolator(new LinearOutSlowInInterpolator());
            animator.start();
        }
    };

}
