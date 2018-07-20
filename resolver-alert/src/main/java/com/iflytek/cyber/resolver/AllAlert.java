package com.iflytek.cyber.resolver;

import com.iflytek.cyber.resolver.player.PlayOrder;
import com.litesuits.orm.db.annotation.Column;
import com.litesuits.orm.db.annotation.PrimaryKey;
import com.litesuits.orm.db.annotation.Table;
import com.litesuits.orm.db.enums.AssignType;

import java.util.ArrayList;

@Table("all_alert")
public class AllAlert extends Alert {

    public String type;

    @PrimaryKey(AssignType.BY_MYSELF)
    public String token;

    @Column("scheduled_time")
    public String scheduledTime;

    @Column("play_orders")
    public ArrayList<PlayOrder> playOrders;

    @Column("has_loopCount")
    public boolean hasLoopCount;

    public long loopCount;

    public long loopPauseInMilliSeconds;

    public AllAlert() {

    }

    public AllAlert(String type, String token, String scheduledTime, ArrayList<PlayOrder> playOrders,
                    boolean hasLoopCount, long loopCount, long loopPauseInMilliSeconds) {
        this.type = type;
        this.token = token;
        this.scheduledTime = scheduledTime;
        this.playOrders = playOrders;
        this.hasLoopCount = hasLoopCount;
        this.loopCount = loopCount;
        this.loopPauseInMilliSeconds = loopPauseInMilliSeconds;
    }
}
