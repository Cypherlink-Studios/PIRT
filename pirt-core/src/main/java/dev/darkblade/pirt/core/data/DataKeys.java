package dev.darkblade.pirt.core.data;

/**
 * Standard data keys recognized by PIRT.
 */
public final class DataKeys {

    private DataKeys() {}

    // Identity
    public static final DataKey<String> PLAYER_NAME = DataKey.of("player.name", String.class);
    public static final DataKey<String> PLAYER_UUID = DataKey.of("player.uuid", String.class);

    // Vitals & Stats
    public static final DataKey<Double> HEALTH = DataKey.of("player.health", Double.class);
    public static final DataKey<Double> MAX_HEALTH = DataKey.of("player.max_health", Double.class);
    public static final DataKey<Integer> FOOD_LEVEL = DataKey.of("player.food", Integer.class);
    public static final DataKey<Integer> LEVEL = DataKey.of("player.level", Integer.class);
    public static final DataKey<Float> EXP = DataKey.of("player.exp", Float.class);
    public static final DataKey<String> GAMEMODE = DataKey.of("player.gamemode", String.class);
    public static final DataKey<Integer> PING = DataKey.of("player.ping", Integer.class);

    // Location
    public static final DataKey<String> WORLD = DataKey.of("player.world", String.class);
    public static final DataKey<Double> X = DataKey.of("player.x", Double.class);
    public static final DataKey<Double> Y = DataKey.of("player.y", Double.class);
    public static final DataKey<Double> Z = DataKey.of("player.z", Double.class);
}
