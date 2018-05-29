package com.iflytek.cyber.resolver.templateruntime.fragment.player;

import java.util.HashMap;
import java.util.Map;

public class PlayerInfoMap {
    private static PlayerInfoMap map;

    private Map<String, PlayerInfoPayload> playerInfoMap;

    private PlayerInfoMap() {
        playerInfoMap = new HashMap<>();
    }

    public static PlayerInfoMap get() {
        if (map == null)
            map = new PlayerInfoMap();
        return map;
    }

    public void put(String key, PlayerInfoPayload payload) {
        playerInfoMap.put(key, payload);
    }

    public PlayerInfoPayload get(String key) {
        return playerInfoMap.get(key);
    }
}
