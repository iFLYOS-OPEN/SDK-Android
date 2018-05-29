package com.iflytek.cyber.resolver.xftemplateruntime.model;

import android.os.Parcel;
import android.os.Parcelable;

public class XfTemplatePayload implements Parcelable {
    public String token;
    public String title;
    public Image skillIcon;
    public String mediaType;
    public Image imageMedia;
    public Video videoMedia;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.token);
        dest.writeString(this.title);
        dest.writeParcelable(this.skillIcon, flags);
        dest.writeString(this.mediaType);
        dest.writeParcelable(this.imageMedia, flags);
        dest.writeParcelable(this.videoMedia, flags);
    }

    public XfTemplatePayload() {
    }

    protected XfTemplatePayload(Parcel in) {
        this.token = in.readString();
        this.title = in.readString();
        this.skillIcon = in.readParcelable(Image.class.getClassLoader());
        this.mediaType = in.readString();
        this.imageMedia = in.readParcelable(Image.class.getClassLoader());
        this.videoMedia = in.readParcelable(Video.class.getClassLoader());
    }

    public static final Parcelable.Creator<XfTemplatePayload> CREATOR = new Parcelable.Creator<XfTemplatePayload>() {
        @Override
        public XfTemplatePayload createFromParcel(Parcel source) {
            return new XfTemplatePayload(source);
        }

        @Override
        public XfTemplatePayload[] newArray(int size) {
            return new XfTemplatePayload[size];
        }
    };
}
