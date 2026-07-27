package org.rexi.velocityUtils.utils;

import java.time.Instant;

public class BanData {

    private final String name;
    private final String ip;
    private final Boolean ipban;
    private final String bannedBy;
    private final String reason;
    private final Instant bannedAt;

    public BanData(String name, String ip, boolean ipban, String bannedBy, Instant bannedAt, String reason) {
        this.name = name;
        this.ip = ip;
        this.ipban = ipban;
        this.bannedBy = bannedBy;
        this.bannedAt = bannedAt;
        this.reason = reason;
    }

    public String getName() { return name; }
    public String getIp() { return ip; }
    public Boolean getIpBan() { return ipban; }
    public String getBannedBy() { return bannedBy; }
    public String getReason() { return reason; }
    public Instant getBannedAt() { return bannedAt; }
}

