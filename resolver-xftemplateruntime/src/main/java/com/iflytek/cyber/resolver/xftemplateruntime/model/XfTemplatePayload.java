/*
 * Copyright (C) 2018 iFLYTEK CO.,LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
