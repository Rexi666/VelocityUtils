package org.rexi.velocityUtils.api;

public final class VelocityUtilsProvider {
    private static VelocityUtilsAPI api;

    private VelocityUtilsProvider() {}

    public static VelocityUtilsAPI get() {
        return api;
    }

    public static void register(VelocityUtilsAPI instance) {
        if (api != null) throw new IllegalStateException("API was already registered!");
        api = instance;
        System.out.println("VelocityUtils API registered");
    }
}
