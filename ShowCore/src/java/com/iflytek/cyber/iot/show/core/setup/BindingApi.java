package com.iflytek.cyber.iot.show.core.setup;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface BindingApi {

    @FormUrlEncoded
    @POST("/device/binding_code")
    Call<Binding> requestBind(@Field("model_id") String modelId, @Field("device_id") String deviceId);

}
