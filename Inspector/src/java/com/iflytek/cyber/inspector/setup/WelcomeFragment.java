package com.iflytek.cyber.inspector.setup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.iflytek.cyber.inspector.LauncherActivity;
import com.iflytek.cyber.inspector.R;

public class WelcomeFragment extends Fragment {

    private LauncherActivity activity;

    private SharedPreferences pref;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (LauncherActivity) context;
        pref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    @SuppressLint("ApplySharedPref")
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        final EditText clientId = view.findViewById(R.id.client_id);
        clientId.setText(pref.getString("client_id", "d97dfbaf-bb2f-4936-821d-30e27244260d"));

        final EditText modelId = view.findViewById(R.id.model_id);
        modelId.setText(pref.getString("model_id", "20120a03-178b-404e-95e5-ab3ade60fbf0"));

        view.findViewById(R.id.next).setOnClickListener(v -> {
            pref.edit()
                    .putString("client_id", clientId.getText().toString())
                    .putString("model_id", modelId.getText().toString())
                    .commit();

            activity.updateClientId();
            activity.navigateTo(new PairFragment());
        });
    }

}
