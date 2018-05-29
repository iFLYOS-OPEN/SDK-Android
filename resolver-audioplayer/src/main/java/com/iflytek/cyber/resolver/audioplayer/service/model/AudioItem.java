package com.iflytek.cyber.resolver.audioplayer.service.model;

import android.os.Parcel;
import android.os.Parcelable;

public class AudioItem implements Parcelable {
    public String audioItemId;
    public Stream stream;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.audioItemId);
        dest.writeParcelable(this.stream, flags);
    }

    public AudioItem() {
    }

    protected AudioItem(Parcel in) {
        this.audioItemId = in.readString();
        this.stream = in.readParcelable(Stream.class.getClassLoader());
    }

    public static final Parcelable.Creator<AudioItem> CREATOR = new Parcelable.Creator<AudioItem>() {
        @Override
        public AudioItem createFromParcel(Parcel source) {
            return new AudioItem(source);
        }

        @Override
        public AudioItem[] newArray(int size) {
            return new AudioItem[size];
        }
    };
}
