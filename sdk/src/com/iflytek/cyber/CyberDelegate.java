package com.iflytek.cyber;

import com.google.gson.JsonObject;

import okio.BufferedSource;
import okio.Source;

public abstract class CyberDelegate {

    /**
     * 更新当前 namespace 下的 context
     *
     * @param name    context 名称
     * @param payload context 内容
     */
    public abstract void updateContext(String name, JsonObject payload);

    /**
     * 移除当前 namespace 下对应的 context
     *
     * @param name 需要移除的 context 的名称
     */
    public abstract void removeContext(String name);

    /**
     * 向 IVS 提交一个当前 namespace 下的事件（不带 context）
     *
     * @param name    事件名称
     * @param payload 事件内容
     */
    public void postEvent(String name, JsonObject payload) {
        postEvent(name, payload, false);
    }

    /**
     * 向 IVS 提交一个当前 namespace 下的事件
     *
     * @param name        事件名称
     * @param payload     事件内容
     * @param withContext 是否附带 context
     */
    public abstract void postEvent(String name, JsonObject payload, boolean withContext);

    /**
     * 向 IVS 提交一个当前 namespace 下的事件，附带音频流
     *
     * @param name    事件名称
     * @param payload 事件内容
     * @param audio   音频流
     */
    public abstract void postEvent(String name, JsonObject payload, Source audio);

    /**
     * 在下行流中查找附件
     *
     * @param cid      附件 ID
     * @param callback 回调
     */
    public abstract void queryAttachment(String cid, AttachmentCallback callback);

    /**
     * 更换 IVS 服务地址
     *
     * @param endpoint 服务地址
     */
    public abstract void setEndpoint(String endpoint);

    @Deprecated
    public abstract void registerSpeechController(SpeechController controller);

    public abstract void stopCapture();

    public abstract void expectSpeech(JsonObject initiator);

    /**
     * {@link #queryAttachment(String, AttachmentCallback)} 的回调
     */
    public interface AttachmentCallback {

        /**
         * 附件找到并可用
         *
         * @param cid    附件 ID
         * @param source 附件的流
         */
        void onAttachment(String cid, BufferedSource source);

        /**
         * 找不到这个附件
         *
         * @param cid 附件 ID
         */
        void onNotFound(String cid);

    }

}
