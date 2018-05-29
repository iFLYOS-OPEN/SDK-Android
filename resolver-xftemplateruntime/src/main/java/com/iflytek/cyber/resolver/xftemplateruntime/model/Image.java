package com.iflytek.cyber.resolver.xftemplateruntime.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Image implements Parcelable {
    public String contentDescription;
    public List<Source> sources;

    public static class Source {
        public String url;
        public String darkBackgroundUrl;
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

    public Image() {
    }

    protected Image(Parcel in) {
        this.contentDescription = in.readString();
        this.sources = new ArrayList<Source>();
        in.readList(this.sources, Source.class.getClassLoader());
    }

    public static final Parcelable.Creator<Image> CREATOR = new Parcelable.Creator<Image>() {
        @Override
        public Image createFromParcel(Parcel source) {
            return new Image(source);
        }

        @Override
        public Image[] newArray(int size) {
            return new Image[size];
        }
    };
}
