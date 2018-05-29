package com.iflytek.cyber.iot.show.core.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.iflytek.cyber.iot.show.core.AboutFragment;
import com.iflytek.cyber.iot.show.core.R;
import com.iflytek.cyber.iot.show.core.WifiInfoManager;
import com.iflytek.cyber.iot.show.core.setup.WifiActivity;

public class SettingsFragment extends DialogFragment implements View.OnClickListener {
    private SeekBar seekBarBrightness;
    private SeekBar seekBarVolume;
    private ImageView ivVolume;
    private ImageView ivBrightness;
    private ImageView ivNetwork;
    private TextView wifiName;
    private LinearLayout wifiContent;

    private static final Uri BRIGHTNESS_MODE_URI =
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE);
    private static final Uri BRIGHTNESS_URI =
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS);
    private static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";

    private Context context;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_settings, container, false);
        seekBarBrightness = view.findViewById(R.id.brightness_bar);
        seekBarBrightness.setMax(255);
        seekBarBrightness.setOnSeekBarChangeListener(brightnessBarChangeListener);
        seekBarVolume = view.findViewById(R.id.volume_bar);
        seekBarVolume.setOnSeekBarChangeListener(volumeBarChangeListener);
        ivVolume = view.findViewById(R.id.iv_volume);
        ivBrightness = view.findViewById(R.id.iv_brightness);
        ivVolume.setOnClickListener(this);
        ivBrightness.setOnClickListener(this);
        ivNetwork = view.findViewById(R.id.network);
        wifiName = view.findViewById(R.id.wifi_name);
        wifiContent = view.findViewById(R.id.wifi_content);
        wifiContent.setOnClickListener(v -> {
            startActivity(new Intent(context, WifiActivity.class));
        });
        view.findViewById(R.id.about).setOnClickListener(v -> {
            dismiss();
            final FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null)
                new AboutFragment().show(fragmentManager, "About");
        });
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        WifiInfoManager.getManager().registerWifiRssiCallback(context, this::updateNetworkRssi);
    }

    private void updateWifiName() {
        final WifiInfo info = WifiInfoManager.getManager().getWifiInfo(context);
        if (info != null) {
            String ssid = info.getSSID();
            String name = ssid.substring(1, ssid.length() - 1);
            wifiName.setText(name);
        }
    }

    private void updateNetworkRssi() {
        updateWifiName();
        int level = WifiInfoManager.getManager().getWifiSignalLevel(context);
        if (level == 1) {
            ivNetwork.setImageResource(R.drawable.ic_signal_wifi_1_bar_black_24dp);
        } else if (level == 2) {
            ivNetwork.setImageResource(R.drawable.ic_signal_wifi_2_bar_black_24dp);
        } else if (level == 3) {
            ivNetwork.setImageResource(R.drawable.ic_signal_wifi_3_bar_black_24dp);
        } else if (level == 4) {
            ivNetwork.setImageResource(R.drawable.ic_signal_wifi_4_bar_white_24dp);
        } else {
            ivNetwork.setImageResource(R.drawable.ic_baseline_wifi_error_24px);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        updateWifiName();

        getDialog().getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        Context context = getContext();
        if (context == null)
            return;

        try {
            ContentResolver contentResolver = context.getContentResolver();
            int currentBrightness = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
            seekBarBrightness.setProgress(currentBrightness);

            int maxBrightness = seekBarBrightness.getMax();
            if (currentBrightness > maxBrightness * 0.677) {
                ivBrightness.setImageResource(R.drawable.ic_brightness_high_black_24dp);
            } else if (currentBrightness > maxBrightness * 0.333) {
                ivBrightness.setImageResource(R.drawable.ic_brightness_medium_black_24dp);
            } else {
                ivBrightness.setImageResource(R.drawable.ic_brightness_low_black_24dp);
            }
            int mode = 0;
            try {
                mode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                seekBarBrightness.setEnabled(false);
                ivBrightness.setImageResource(R.drawable.ic_brightness_auto_black_24dp);
            } else {
                seekBarBrightness.setEnabled(true);
            }

            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                seekBarVolume.setMax(max);

                int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                seekBarVolume.setProgress(current);

                if (current == 0) {
                    ivVolume.setImageResource(R.drawable.ic_volume_off_black_24dp);
                } else if (current > max * 0.75) {
                    ivVolume.setImageResource(R.drawable.ic_volume_up_black_24dp);
                } else if (current > max * 0.333) {
                    ivVolume.setImageResource(R.drawable.ic_volume_down_black_24dp);
                } else {
                    ivVolume.setImageResource(R.drawable.ic_volume_mute_black_24dp);
                }
                boolean mute = false;
                if (Build.VERSION.SDK_INT < 23) {
                    if (ivVolume.getTag() != null) {
                        mute = (boolean) ivVolume.getTag();
                    }
                } else {
                    mute = audioManager.isStreamMute(AudioManager.STREAM_MUSIC);
                }
                if (mute) {
                    ivVolume.setImageResource(R.drawable.ic_volume_off_black_24dp);
                }
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null) {
            int height = getResources().getDimensionPixelSize(R.dimen.settings_height);
            if (dialog.getWindow() != null) {
                Window window = dialog.getWindow();
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, height);
                window.setGravity(Gravity.TOP);
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setWindowAnimations(R.style.SettingsAnimation);
            }
        }

        Activity activity = getActivity();
        if (activity == null)
            return;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(VOLUME_CHANGED_ACTION);
        activity.registerReceiver(volumeChangeReceiver, intentFilter);

        try {
            if (brightnessObserver != null) {
                final ContentResolver cr = activity.getContentResolver();
                cr.registerContentObserver(BRIGHTNESS_MODE_URI, false, brightnessObserver);
                cr.registerContentObserver(BRIGHTNESS_URI, false, brightnessObserver);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Activity activity = getActivity();
        if (activity == null)
            return;

        activity.unregisterReceiver(volumeChangeReceiver);

        if (brightnessObserver != null)
            getActivity().getContentResolver().unregisterContentObserver(brightnessObserver);
    }

    private SeekBar.OnSeekBarChangeListener volumeBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int max = seekBar.getMax();
            if (progress == 0) {
                ivVolume.setImageResource(R.drawable.ic_volume_off_black_24dp);
            } else if (progress > max * 0.75) {
                ivVolume.setImageResource(R.drawable.ic_volume_up_black_24dp);
            } else if (progress > max * 0.333) {
                ivVolume.setImageResource(R.drawable.ic_volume_down_black_24dp);
            } else {
                ivVolume.setImageResource(R.drawable.ic_volume_mute_black_24dp);
            }
            if (!fromUser)
                return;
            AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private SeekBar.OnSeekBarChangeListener brightnessBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int max = seekBar.getMax();
            if (progress > max * 0.677) {
                ivBrightness.setImageResource(R.drawable.ic_brightness_high_black_24dp);
            } else if (progress > max * 0.333) {
                ivBrightness.setImageResource(R.drawable.ic_brightness_medium_black_24dp);
            } else {
                ivBrightness.setImageResource(R.drawable.ic_brightness_low_black_24dp);
            }
            if (!fromUser)
                return;
            Activity activity = getActivity();
            if (activity == null)
                return;
            ContentResolver contentResolver = activity.getContentResolver();
            try {
                if (Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
                        == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                }
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            Settings.System.putInt(contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS, progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    private BroadcastReceiver volumeChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || TextUtils.isEmpty(intent.getAction()))
                return;
            switch (intent.getAction()) {
                case VOLUME_CHANGED_ACTION:
                    AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
                    if (audioManager != null) {
                        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        seekBarVolume.setMax(max);

                        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                        seekBarVolume.setProgress(current);
                    }
                    break;
            }
        }
    };

    private ContentObserver brightnessObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Context context = getContext();
            if (context == null) return;
            ContentResolver contentResolver = context.getContentResolver();
            if (BRIGHTNESS_MODE_URI.equals(uri)) {
                int mode = 0;
                try {
                    mode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE);
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }
                if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    ivBrightness.setImageResource(R.drawable.ic_brightness_auto_black_24dp);
                } else {
                    int currentBrightness = 0;
                    int max = seekBarBrightness.getMax();
                    try {
                        currentBrightness = Settings.System.getInt(contentResolver,
                                Settings.System.SCREEN_BRIGHTNESS);
                    } catch (Settings.SettingNotFoundException e) {
                        e.printStackTrace();
                    }
                    if (currentBrightness > max * 0.677) {
                        ivBrightness.setImageResource(R.drawable.ic_brightness_high_black_24dp);
                    } else if (currentBrightness > max * 0.333) {
                        ivBrightness.setImageResource(R.drawable.ic_brightness_medium_black_24dp);
                    } else {
                        ivBrightness.setImageResource(R.drawable.ic_brightness_low_black_24dp);
                    }
                }
            } else if (BRIGHTNESS_URI.equals(uri)) {
                int currentBrightness = 0;
                try {
                    currentBrightness = Settings.System.getInt(contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS);
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }
                seekBarBrightness.setProgress(currentBrightness);
            }
        }
    };

    @Override
    public void onClick(View v) {
        Context context = getContext();
        if (context == null)
            return;
        switch (v.getId()) {
            case R.id.iv_volume:
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audioManager == null)
                    return;
                boolean mute = false;
                if (Build.VERSION.SDK_INT < 23) {
                    if (ivVolume.getTag() != null) {
                        mute = !(boolean) ivVolume.getTag();
                    }
                } else {
                    mute = !audioManager.isStreamMute(AudioManager.STREAM_MUSIC);
                }
                if (Build.VERSION.SDK_INT < 23) {
                    audioManager.setStreamMute(AudioManager.STREAM_MUSIC, mute);
                } else {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            mute ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE, 0);
                }
                ivVolume.setTag(mute);
                if (mute) {
                    ivVolume.setImageResource(R.drawable.ic_volume_off_black_24dp);
                    seekBarVolume.setProgress(0);
                } else {
                    int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    if (current == 0) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 1, 0);
                        current = 1;
                    }
                    seekBarVolume.setProgress(current);
                    int max = seekBarVolume.getMax();
                    if (current > max * 0.667) {
                        ivVolume.setImageResource(R.drawable.ic_volume_up_black_24dp);
                    } else if (current > max * 0.333) {
                        ivVolume.setImageResource(R.drawable.ic_volume_down_black_24dp);
                    } else {
                        ivVolume.setImageResource(R.drawable.ic_volume_mute_black_24dp);
                    }
                }
                break;
            case R.id.iv_brightness:
                ContentResolver contentResolver = context.getContentResolver();
                int mode = 0;
                try {
                    mode = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE);
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }
                if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    int currentBrightness = seekBarBrightness.getProgress();

                    int maxBrightness = seekBarBrightness.getMax();
                    if (currentBrightness > maxBrightness * 0.677) {
                        ivBrightness.setImageResource(R.drawable.ic_brightness_high_black_24dp);
                    } else if (currentBrightness > maxBrightness * 0.333) {
                        ivBrightness.setImageResource(R.drawable.ic_brightness_medium_black_24dp);
                    } else {
                        ivBrightness.setImageResource(R.drawable.ic_brightness_low_black_24dp);
                    }
                    seekBarBrightness.setEnabled(true);
                } else {
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
                    ivBrightness.setImageResource(R.drawable.ic_brightness_auto_black_24dp);
                    seekBarBrightness.setEnabled(false);
                }
                break;
        }
    }
}
