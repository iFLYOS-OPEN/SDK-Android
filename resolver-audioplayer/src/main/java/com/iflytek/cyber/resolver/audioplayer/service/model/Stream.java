package com.iflytek.cyber.resolver.audioplayer.service.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Stream implements Parcelable {
    public String url;
    public String streamFormat;
    public long offsetInMilliseconds;
    public String expiryTime;
    public ProgressReport progressReport;
    public String token;
    public String expectedPreviousToken;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.url);
        dest.writeString(this.streamFormat);
        dest.writeLong(this.offsetInMilliseconds);
        dest.writeString(this.expiryTime);
        dest.writeParcelable(this.progressReport, flags);
        dest.writeString(this.token);
        dest.writeString(this.expectedPreviousToken);
    }

    public Stream() {
    }

    protected Stream(Parcel in) {
        this.url = in.readString();
        this.streamFormat = in.readString();
        this.offsetInMilliseconds = in.readLong();
        this.expiryTime = in.readString();
        this.progressReport = in.readParcelable(ProgressReport.class.getClassLoader());
        this.token = in.readString();
        this.expectedPreviousToken = in.readString();
    }

    public static Creator<Stream> CREATOR = new Creator<Stream>() {
        @Override
        public Stream createFromParcel(Parcel source) {
            return new Stream(source);
        }

        @Override
        public Stream[] newArray(int size) {
            return new Stream[size];
        }
    };
}