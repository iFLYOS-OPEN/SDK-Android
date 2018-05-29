package com.iflytek.cyber.resolver.templateruntime.fragment.player;

import com.iflytek.cyber.resolver.templateruntime.model.Image;

import java.util.List;

public class PlayerInfoPayload {
    public String audioItemId;
    public Content content;
    public List<Control> controls;

    public static class Content {
        public String title;
        public String titleSubtext1;
        public String titleSubtext2;
        public String header;
        public String headerSubtext1;
        public String mediaLengthInMilliseconds;
        public Image art;
        public Provider provider;
    }

    public static class Provider {
        public String name;
        public Image logo;
    }

    public class Control {
        public String type;
        public String name;
        public boolean enabled;
        public boolean selected;
    }
}
