package com.iflytek.cyber.resolver.templateruntime.fragment.body1;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.iflytek.cyber.resolver.templateruntime.R;

public class Body1Fragment extends Fragment {
    private Body1Payload body1Payload;
    private TextView tvMainTitle;
    private TextView tvSubTitle;
    private TextView tvTextField;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_body1, container, false);
        tvMainTitle = view.findViewById(R.id.main_title);
        tvSubTitle = view.findViewById(R.id.sub_title);
        tvTextField = view.findViewById(R.id.text_field);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (body1Payload.title != null) {
            tvMainTitle.setText(body1Payload.title.mainTitle);
            tvSubTitle.setText(body1Payload.title.subTitle);
        }
        tvTextField.setText(body1Payload.textField);
    }

    public static Body1Fragment generate(JsonObject payload) {
        Body1Fragment fragment = new Body1Fragment();
        fragment.body1Payload = new Gson().fromJson(payload, Body1Payload.class);
        return fragment;
    }
}
