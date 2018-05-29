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

package com.iflytek.cyber.iot.show.core;

import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Network;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.JsonObject;
import com.iflytek.cyber.iot.show.core.widget.MarqueeView;
import com.iflytek.cyber.platform.AuthManager;
import com.iflytek.cyber.platform.DefaultTokenStorage;
import com.iflytek.cyber.platform.Recorder;
import com.iflytek.cyber.platform.TokenManager;
import com.iflytek.cyber.platform.resolver.ResolverManager;
import com.iflytek.cyber.resolver.audioplayer.AudioPlayerResolver;
import com.iflytek.cyber.resolver.audioplayer.service.model.AudioItem;
import com.iflytek.cyber.resolver.playbackcontroller.PlaybackControllerResolver;
import com.iflytek.cyber.resolver.speechrecognizer.SpeechRecognizerResolver;
import com.iflytek.cyber.resolver.templateruntime.TemplateRuntimeResolver;
import com.iflytek.cyber.resolver.templateruntime.fragment.player.PlayerInfoFragment;
import com.iflytek.cyber.resolver.templateruntime.fragment.player.PlayerInfoPayload;
import com.iflytek.cyber.resolver.xftemplateruntime.PlayerFragment;
import com.iflytek.cyber.resolver.xftemplateruntime.XfTemplateRuntimeResolver;

import java.lang.ref.WeakReference;

import jp.wasabeef.blurry.Blurry;
import okio.Source;

public class LauncherActivity extends BaseActivity implements TemplateRuntimeResolver.TemplateRuntimeCallback, XfTemplateRuntimeResolver.XfTemplateRuntimeCallback {
    private AuthManager authManager;
    private TokenManager tokenManager;
    private CoreService coreService;

    private Fragment current;

    private ViewPager mViewPager;
    private MainPageAdapter mPageAdapter;
    private ImageView blurView;
    private LinearLayout pagerIndicatorContainer;
    private TextView tvCounter;
    //private RecognizeBar recognizeBar;
    private LinearLayout iatContainer;
    private View volumeBar;
    private TextView tvIat;
    private MarqueeView marqueeView;
    private TextView networkMessage;
    private LinearLayout marqueeContent;

    private CounterHandler counterHandler;

    private TemplateRuntimeResolver templateRuntimeResolver;
    private XfTemplateRuntimeResolver xfTemplateRuntimeResolver;

    private int templateRuntimeHeight;
    private int bottomBarHeight;

    private ValueAnimator animator;

    private boolean showIat = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        mViewPager = findViewById(R.id.view_pager);
        mPageAdapter = new MainPageAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mPageAdapter);
        blurView = findViewById(R.id.blur_music_album);
        pagerIndicatorContainer = findViewById(R.id.pager_indicator_container);
        tvCounter = findViewById(R.id.close_after_seconds);
        //recognizeBar = findViewById(R.id.recognize_bar);
        iatContainer = findViewById(R.id.iat_container);
        volumeBar = findViewById(R.id.volume_center_bar);
        tvIat = findViewById(R.id.iat);
        marqueeContent = findViewById(R.id.marquee_content);
        marqueeView = findViewById(R.id.marquee_view);
        networkMessage = findViewById(R.id.network_message);

        startMarquee();

        ImageView ivIndex = findViewById(R.id.iv_index);
        ivIndex.post(() -> {
            View parent = (View) ivIndex.getParent();
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) ivIndex.getLayoutParams();
            layoutParams.leftMargin = (int) (parent.getWidth() * 0.35);
            ivIndex.setLayoutParams(layoutParams);
        });

        counterHandler = new CounterHandler(this);

        findViewById(R.id.close).setOnClickListener(v -> dismissTemplate());
        findViewById(R.id.template_runtime_bottom_bar).post(() -> {
            View container = findViewById(R.id.template_runtime_container);
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            templateRuntimeHeight = dm.heightPixels;
            container.setTranslationY(templateRuntimeHeight);
            container.setVisibility(View.VISIBLE);
            bottomBarHeight = findViewById(R.id.template_runtime_bottom_bar).getHeight();
        });

        authManager = new AuthManager(BuildConfig.CLIENT_ID);
        tokenManager = new TokenManager(new DefaultTokenStorage(this), authManager);

        init();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(CoreService.ACTION_SERVICE_PREPARED);
        intentFilter.addAction(CoreService.ACTION_WAKE_UP_STATE_CHANGED);
        intentFilter.addAction(CoreService.ACTION_AUTH_STATE_CHANGED);
        registerReceiver(mReceiver, intentFilter);

        findViewById(R.id.recognize).setOnClickListener(view -> {
            Intent intent = new Intent(this, CoreService.class);
            intent.setAction(CoreService.ACTION_TOGGLE_WAKE_UP_STATE);
            startService(intent);
            dismissIat();
        });
        findViewById(R.id.iat_container).setOnClickListener(v -> {
            Intent intent = new Intent(this, CoreService.class);
            intent.setAction(CoreService.ACTION_TOGGLE_WAKE_UP_STATE);
            startService(intent);
            dismissIat();
        });

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position == 0)
                    blurView.setAlpha(positionOffset * positionOffset * positionOffset);
                if (mViewPager.getCurrentItem() == 1)
                    if (position == 1) {
                        mPageAdapter.playerInfoFragment.requestFocus();
                    } else {
                        mPageAdapter.playerInfoFragment.abandonFocus();
                    }
            }

            @Override
            public void onPageSelected(int position) {
                if (pagerIndicatorContainer.getChildCount() > position) {
                    for (int i = pagerIndicatorContainer.getChildCount() - 1; i >= 0; i--) {
                        pagerIndicatorContainer.getChildAt(i).setSelected(false);
                    }
                    pagerIndicatorContainer.getChildAt(position).setSelected(true);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        generatePagerIndicator();

        final WifiInfoManager infoManager = WifiInfoManager.getManager();
        if (!infoManager.isNetworkAvailable(this)) {
            networkLost();
        } else {
            networkAvailable();
        }

        WifiInfoManager.getManager().registerNetworkCallback(this,
                new WifiInfoManager.NetworkStateListener() {
                    @Override
                    public void onAvailable(Network network) {
                        networkAvailable();
                    }

                    @Override
                    public void onLost(Network network) {
                        networkLost();
                    }
                });
    }

    private void networkLost() {
        marqueeContent.setVisibility(View.GONE);
        networkMessage.setVisibility(View.VISIBLE);
    }

    private void networkAvailable() {
        marqueeContent.setVisibility(View.VISIBLE);
        networkMessage.setVisibility(View.GONE);
    }

    private void startMarquee() {
        String[] examples = getResources().getStringArray(R.array.examples);
        marqueeView.setMarqueeData(examples);
    }

    private void showTemplate(Fragment fragment) {
        showTemplate(fragment, true);
    }

    private void showTemplate(Fragment fragment, boolean showBottomBar) {
        current = fragment;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.template_runtime_fragment, fragment)
                .commitAllowingStateLoss();
        if (showBottomBar) {
            findViewById(R.id.template_runtime_bottom_bar).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.template_runtime_bottom_bar).setVisibility(View.GONE);
        }
        findViewById(R.id.template_runtime_container).animate()
                .setInterpolator(new FastOutLinearInInterpolator())
                .translationY(0).setDuration(300).start();
        if (showBottomBar)
            counterHandler.sendEmptyMessage(CounterHandler.START_COUNT);
    }

    @Override
    public void onBackPressed() {
        if (current != null)
            dismissTemplate();
        else
            super.onBackPressed();
    }

    private void dismissTemplate() {
        findViewById(R.id.template_runtime_container).animate()
                .translationY(templateRuntimeHeight).setDuration(250)
                .withEndAction(() -> {
                    if (current != null) {
                        counterHandler.stop();
                        getSupportFragmentManager().beginTransaction()
                                .remove(current).commitAllowingStateLoss();
                    }
                })
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WifiInfoManager.getManager().unregisterNetworkCallback(this);
        unregisterReceiver(mReceiver);

        unbindService(connection);
    }

    @Override
    protected void onResume() {
        super.onResume();

        ResolverManager resolverManager = ResolverManager.get();
        if (resolverManager != null) {
            templateRuntimeResolver = resolverManager.peek("TemplateRuntime", TemplateRuntimeResolver.class);
            xfTemplateRuntimeResolver = resolverManager.peek("XFTemplateRuntime", XfTemplateRuntimeResolver.class);
            AudioPlayerResolver audioResolver = resolverManager.peek("AudioPlayer", AudioPlayerResolver.class);
            if (audioResolver != null) {
                mPageAdapter.playerInfoFragment.setup(audioResolver);
            }
            PlaybackControllerResolver playbackResolver = resolverManager.peek("PlaybackController", PlaybackControllerResolver.class);
            if (playbackResolver != null) {
                mPageAdapter.playerInfoFragment.setup(playbackResolver);
            }
        }
        if (templateRuntimeResolver != null) {
            templateRuntimeResolver.setTemplateRuntimeCallback(this);
        }
        if (xfTemplateRuntimeResolver != null) {
            xfTemplateRuntimeResolver.setXfTemplateRuntimeCallback(LauncherActivity.this);
        }
    }

    public void init() {
        if (!tokenManager.hasToken()) {
            PackageManager pm = getPackageManager();
            pm.setComponentEnabledSetting(new ComponentName(this, SetupWizardActivity.class),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            startActivity(Intent.makeMainActivity(new ComponentName(this, SetupWizardActivity.class)));
            finish();
            pm.setComponentEnabledSetting(new ComponentName(this, LauncherActivity.class),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }

        // 让后台服务根据 token 情况决定是否活动
        startService(new Intent(this, CoreService.class));

        bindService(new Intent(this, CoreService.class), connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onNewTemplateFragment(Fragment fragment) {
        showTemplate(fragment);
    }

    @Override
    public void onPlayerInfoUpdate(PlayerInfoPayload payload) {
        // ignore
    }

    private void generatePagerIndicator() {
        pagerIndicatorContainer.removeAllViews();

        int pageIndicatorSize = getResources().getDimensionPixelSize(R.dimen.page_indicator_size);
        int pageIndicatorMargin = getResources().getDimensionPixelSize(R.dimen.page_indicator_margin);

        for (int i = 0; i < mPageAdapter.getCount(); i++) {
            View view = new View(this);
            view.setBackgroundResource(R.drawable.view_pager_indicator);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(pageIndicatorSize, pageIndicatorSize);
            params.leftMargin = pageIndicatorMargin / 2;
            params.rightMargin = pageIndicatorMargin / 2;
            params.bottomMargin = pageIndicatorMargin / 2;
            if (i == 0)
                view.setSelected(true);
            pagerIndicatorContainer.addView(view, params);
        }
    }

    @Override
    public void onNewXfFragment(Fragment fragment) {
        if (fragment instanceof PlayerFragment) {
            ((PlayerFragment) fragment).setPlayerCallback(this::dismissTemplate);
        }
        showTemplate(fragment, false);
    }

    class MainPageAdapter extends FragmentPagerAdapter implements PlayerInfoFragment.DataBindingCallback {
        private MainFragment mainFragment;
        private PlayerInfoFragment playerInfoFragment;

        MainPageAdapter(FragmentManager fm) {
            super(fm);
            mainFragment = new MainFragment();
            playerInfoFragment = new PlayerInfoFragment();
            playerInfoFragment.setOnAlbumCallback(bitmap ->
                    Blurry.with(getBaseContext()).radius(75)
                            .color(Color.argb(128, 0, 0, 0))
                            .from(bitmap).into(blurView));
            playerInfoFragment.setDataBindingCallback(MainPageAdapter.this);
        }

        @Override
        public Fragment getItem(int position) {
            return position == 0 ? mainFragment : playerInfoFragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public void onDataBind(AudioItem audioItem, PlayerInfoPayload payload) {
            if (audioItem != null) {
                mViewPager.setCurrentItem(1, true);
            }
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null)
                return;
            switch (intent.getAction()) {
                case CoreService.ACTION_SERVICE_PREPARED:
                    ResolverManager resolverManager = ResolverManager.get();
                    if (resolverManager != null) {
                        templateRuntimeResolver = resolverManager.peek("TemplateRuntime", TemplateRuntimeResolver.class);
                        xfTemplateRuntimeResolver = resolverManager.peek("XFTemplateRuntime", XfTemplateRuntimeResolver.class);
                        AudioPlayerResolver audioResolver = resolverManager.peek("AudioPlayer", AudioPlayerResolver.class);
                        if (audioResolver != null) {
                            mPageAdapter.playerInfoFragment.setup(audioResolver);
                        }
                        PlaybackControllerResolver playbackResolver = resolverManager.peek("PlaybackController", PlaybackControllerResolver.class);
                        if (playbackResolver != null) {
                            mPageAdapter.playerInfoFragment.setup(playbackResolver);
                        }
                        SpeechRecognizerResolver recognizeResolver = resolverManager.peek("XFSpeechRecognizer", SpeechRecognizerResolver.class);
                        if (recognizeResolver != null) {
                            recognizeResolver.setIatCallback(text -> {
                                tvIat.setText(text);
                                showIat = false;
                                tvIat.postDelayed(() -> {
                                    if (!showIat)
                                        dismissIat();
                                }, 1000);
                            });
                        }
                    }
                    if (templateRuntimeResolver != null) {
                        templateRuntimeResolver.setTemplateRuntimeCallback(LauncherActivity.this);
                    }
                    if (xfTemplateRuntimeResolver != null) {
                        xfTemplateRuntimeResolver.setXfTemplateRuntimeCallback(LauncherActivity.this);
                    }
                    break;
                case CoreService.ACTION_WAKE_UP_STATE_CHANGED:
                    boolean state = intent.getBooleanExtra("state", false);
                    if (state) {
                        //recognizeBar.startWaving();
                        showIat = true;
                        showIat();
                        dismissTemplate();
                    } else {
                        //recognizeBar.stopWaving();
                        showIat = false;
                        tvIat.postDelayed(() -> {
                            if (!showIat)
                                dismissIat();
                        }, 1000);
                    }
                    break;
                case CoreService.ACTION_AUTH_STATE_CHANGED:
                    if (!tokenManager.hasToken()) {
                        PackageManager pm = getPackageManager();
                        pm.setComponentEnabledSetting(new ComponentName(context, SetupWizardActivity.class),
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                        startActivity(Intent.makeMainActivity(new ComponentName(context, SetupWizardActivity.class)));
                        finish();
                        pm.setComponentEnabledSetting(new ComponentName(context, LauncherActivity.class),
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    }
                    break;
            }
        }
    };

    private void showIat() {
        if (iatContainer.getAlpha() != 0)
            return;
        iatContainer.setVisibility(View.VISIBLE);
        iatContainer.animate().alpha(1).setDuration(300).start();
        tvIat.setText("我正在听...");
        findViewById(R.id.marquee_content).setVisibility(View.GONE);
    }

    private void dismissIat() {
        if (iatContainer.getAlpha() != 1)
            return;
        iatContainer.animate().alpha(0).setDuration(250)
                .withEndAction(() -> {
                    iatContainer.setVisibility(View.GONE);
                    findViewById(R.id.marquee_content).setVisibility(View.VISIBLE);
                }).start();
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            CoreService.CoreServiceBinder binder = (CoreService.CoreServiceBinder) service;
            coreService = binder.getService();
            if (coreService != null)
                coreService.setAudioListener(audioListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (coreService != null)
                coreService.setAudioListener(null);
        }
    };

    private Recorder.AudioListener audioListener = new Recorder.AudioListener() {
        @Override
        public void onWakeup(Source audio, JsonObject initiator) {

        }

        @Override
        public void onVolumeChanged(int level) {
            float percent = 1f * level / 100;
            int targetWidth = (int) (iatContainer.getWidth() * percent / 2);

            if (animator != null)
                animator.cancel();
            animator = ValueAnimator.ofInt(volumeBar.getWidth(), targetWidth);
            animator.addUpdateListener(animation -> {
                int width = (int) animation.getAnimatedValue();
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) volumeBar.getLayoutParams();
                params.width = width;
                volumeBar.setLayoutParams(params);
            });
            animator.setDuration(100);
            animator.start();
        }
    };

    private static class CounterHandler extends Handler {
        static final int START_COUNT = 0x1;
        static final int CONTINUE_COUNT = 0x2;
        static final int MAX_COUNT_SECONDS = 15;
        static final int SHOW_COUNT_SECONDS = 10;

        private int count = 0;

        private boolean stopped = false;

        private WeakReference<LauncherActivity> reference;

        CounterHandler(LauncherActivity activity) {
            reference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (stopped && count != 0) {
                stopped = false;
                count = 0;
                return;
            }
            if (reference != null) {
                switch (msg.what) {
                    case START_COUNT:
                        stopped = false;
                        count = 0;
                        sendEmptyMessageDelayed(CONTINUE_COUNT, 1000);
                        break;
                    case CONTINUE_COUNT:
                        LauncherActivity activity = reference.get();
                        if (activity != null) {
                            if (count >= MAX_COUNT_SECONDS) {
                                count = 0;
                                activity.tvCounter.setText("");
                                activity.dismissTemplate();
                            } else {
                                if (count >= SHOW_COUNT_SECONDS)
                                    activity.tvCounter.setText(activity.getString(R.string.close_after_seconds, MAX_COUNT_SECONDS - count));
                                else
                                    activity.tvCounter.setText("");
                                count++;
                                sendEmptyMessageDelayed(CONTINUE_COUNT, 1000);
                            }
                        }
                        break;
                }
            }
        }

        void stop() {
            stopped = true;
        }
    }
}
