package com.iflytek.cyber.iot.show.core.setup;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.iflytek.cyber.iot.show.core.R;
import com.iflytek.cyber.iot.show.core.SetupWizardActivity;

import java.util.concurrent.TimeUnit;

public class FinishFragment extends Fragment {

    private SetupWizardActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (SetupWizardActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_finish, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        view.postDelayed(() -> activity.initMainFragment(), TimeUnit.SECONDS.toMillis(3));
    }

}
