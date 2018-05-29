package com.iflytek.cyber.resolver.audioplayer.service.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ProgressReport implements Parcelable {
    public long progressReportDelayInMilliseconds;
    public long progressReportIntervalInMilliseconds;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.progressReportDelayInMilliseconds);
        dest.writeLong(this.progressReportIntervalInMilliseconds);
    }

    public ProgressReport() {
    }

    protected ProgressReport(Parcel in) {
        this.progressReportDelayInMilliseconds = in.readLong();
        this.progressReportIntervalInMilliseconds = in.readLong();
    }

    public static Creator<ProgressReport> CREATOR = new Creator<ProgressReport>() {
        @Override
        public ProgressReport createFromParcel(Parcel source) {
            return new ProgressReport(source);
        }

        @Override
        public ProgressReport[] newArray(int size) {
            return new ProgressReport[size];
        }
    };
}