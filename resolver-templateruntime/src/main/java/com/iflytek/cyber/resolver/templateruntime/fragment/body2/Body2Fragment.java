package com.iflytek.cyber.resolver.templateruntime.fragment.body2;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.iflytek.cyber.resolver.templateruntime.GlideApp;
import com.iflytek.cyber.resolver.templateruntime.R;

public class Body2Fragment extends Fragment {
    private Body2Payload payload;
    private TextView tvMainTitle;
    private TextView tvSubTitle;
    private TextView tvTextField;
    private ImageView imageView;

    public static Body2Fragment generate(JsonObject payload) {
        Body2Fragment fragment = new Body2Fragment();
        fragment.payload = new Gson().fromJson(payload, Body2Payload.class);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_body2, container, false);
        tvMainTitle = view.findViewById(R.id.main_title);
        tvSubTitle = view.findViewById(R.id.sub_title);
        tvTextField = view.findViewById(R.id.text_field);
        imageView = view.findViewById(R.id.image);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (payload.title != null) {
            tvMainTitle.setText(payload.title.mainTitle);
            tvSubTitle.setText(payload.title.subTitle);
        }
        tvTextField.setText(payload.textField);
        if (payload.image != null && payload.image.sources != null && payload.image.sources.size() > 0) {
            GlideApp.with(getContext())
                    .load(payload.image.sources.get(0).url)
                    .into(imageView);
        }
    }
}
