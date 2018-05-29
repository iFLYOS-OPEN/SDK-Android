package com.iflytek.cyber.resolver.xftemplateruntime.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Video implements Parcelable {
    public String contentDescription;
    public List<Source> sources;

    public static class Source {
        public String url;
        public String resolution;
        public String size;
        public long widthPixels;
        public long heightPixels;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.contentDescription);
        dest.writeList(this.sources);
    }

    public Video() {
    }

    protected Video(Parcel in) {
        this.contentDescription = in.readString();
        this.sources = new ArrayList<Source>();
        in.readList(this.sources, Source.class.getClassLoader());
    }

    public static final Parcelable.Creator<Video> CREATOR = new Parcelable.Creator<Video>() {
        @Override
        public Video createFromParcel(Parcel source) {
            return new Video(source);
        }

        @Override
        public Video[] newArray(int size) {
            return new Video[size];
        }
    };
}
