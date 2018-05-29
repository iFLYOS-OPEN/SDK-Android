/*
 * Copyright (C) 2018 iFLYTEK CO.,LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iflytek.cyber.iot.show.core.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.iflytek.cyber.iot.show.core.R;

public class RecognizeWave extends View {
    private Paint paint;
    private float[] barHeightArray = new float[5];
    private float barHeightMax;
    private float barHeightMin;
    private float barHeightMaxExpanded;
    private float barHeightMinExpanded;
    private float barMargin;
    private float barWidth;
    private float barMarginExpanded;
    private float barWidthExpanded;
    private float barMarginNormal;
    private float barWidthNormal;
    private int color = Color.WHITE;

    private boolean isWaving = false;

    public RecognizeWave(Context context) {
        super(context);
        init();
    }

    public RecognizeWave(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RecognizeWave(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(color);

        Resources resources = getResources();
        barHeightMin = resources.getDimensionPixelSize(R.dimen.recognize_bar_min_height);
        barHeightMax = resources.getDimensionPixelSize(R.dimen.recognize_bar_max_height);
        barHeightMinExpanded = resources.getDimensionPixelSize(R.dimen.recognize_bar_min_height_expanded);
        barHeightMaxExpanded = resources.getDimensionPixelSize(R.dimen.recognize_bar_max_height_expanded);
        for (int i = 0; i < barHeightArray.length; i++) {
            barHeightArray[i] = i % 2 == 0 ? barHeightMin : barHeightMax;
        }

        barMarginNormal = resources.getDimensionPixelSize(R.dimen.recognize_bar_margin);
        barWidthNormal = resources.getDimensionPixelSize(R.dimen.recognize_bar_width);
        barMarginExpanded = resources.getDimensionPixelSize(R.dimen.recognize_bar_margin_expanded);
        barWidthExpanded = resources.getDimensionPixelSize(R.dimen.recognize_bar_width_expanded);

        barMargin = barMarginNormal;
        barWidth = barWidthNormal;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float startX = getMeasuredWidth() / 2 - (barWidth * barHeightArray.length + barMargin * (barHeightArray.length - 1)) / 2;
        float startY = getMeasuredHeight() / 2;

        for (int i = 0; i < barHeightArray.length; i++) {
            canvas.drawRect(startX + i * barWidth + i * barMargin,
                    startY - barHeightArray[i] / 2,
                    startX + (i + 1) * barWidth + i * barMargin,
                    startY + barHeightArray[i] / 2,
                    paint);
        }
    }

    /**
     * start waving while animation end
     */
    void animateToWaving() {
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.addUpdateListener(updateListener);
        animator.setDuration(200);
        animator.setInterpolator(new LinearOutSlowInInterpolator());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                startWaving();
            }
        });
        animator.start();
    }

    void startWaving() {
        isWaving = true;
    }

    void stopWaving() {
        isWaving = false;
        for (int i = 0; i < barHeightArray.length; i++) {
            barHeightArray[i] = i % 2 == 0 ? barHeightMin : barHeightMax;
        }
        barMargin = barMarginNormal;
        barWidth = barWidthNormal;
        invalidate();
    }

    public void animateFromWaving() {
        ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
        animator.addUpdateListener(updateListener);
        animator.setDuration(200);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.start();
        stopWaving();
    }

    private ValueAnimator.AnimatorUpdateListener updateListener = animation -> {
        float value = (float) animation.getAnimatedValue();
        barWidth = (barWidthExpanded - barWidthNormal) * value + barWidthNormal;
        barMargin = (barMarginExpanded - barMarginNormal) * value + barMarginNormal;
        postInvalidate();
    };

    public void updateVolume(int level) {
        if (!isWaving)
            return;
        float offset = (barHeightMaxExpanded - barHeightMinExpanded) * level / 100;
        for (int i = 0; i < barHeightArray.length; i++) {
            barHeightArray[i] = (i % 2 == 0 ? barHeightMinExpanded
                    : barHeightMax) + offset + (Math.random() > 0.5 ? 1 : -1); // Magic number
        }
        invalidate();
    }
}
