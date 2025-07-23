package org.rexi.velocityUtils;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class Config {

    @Setting("alert.prefix")
    private String alertPrefix = "&7[&b&lSERVER&7]";

    public Config() {}

    // Getter and setter for alertPrefix
    public String getAlertPrefix() {
        return alertPrefix;
    }

    public void setAlertPrefix(String alertPrefix) {
        this.alertPrefix = alertPrefix;
    }
}