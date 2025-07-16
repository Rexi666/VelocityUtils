package org.rexi.velocityUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class StaffSession {
    private final Instant loginTime;
    private final Map<String, Duration> timePerServer = new HashMap<>();
    private String currentServer;
    private Instant lastSwitch;

    public StaffSession(Instant loginTime, String currentServer) {
        this.loginTime = loginTime;
        this.currentServer = currentServer;
        this.lastSwitch = loginTime;
        timePerServer.put(currentServer, Duration.ZERO);
    }

    public void switchServer(String newServer) {
        Duration timeSpent = Duration.between(lastSwitch, Instant.now());
        timePerServer.merge(currentServer, timeSpent, Duration::plus);
        currentServer = newServer;
        lastSwitch = Instant.now();
        timePerServer.putIfAbsent(newServer, Duration.ZERO);
    }

    public void finalizeSession() {
        Duration timeSpent = Duration.between(lastSwitch, Instant.now());
        timePerServer.merge(currentServer, timeSpent, Duration::plus);
    }

    public Duration getTotalTime() {
        return Duration.between(loginTime, Instant.now());
    }

    public Map<String, Duration> getTimePerServer() {
        return timePerServer;
    }
}
