package cn.noryea.fastitems.config;

import eu.midnightdust.lib.config.MidnightConfig;
import java.util.HashMap;
import java.util.Map;

public class FastItemsConfig extends MidnightConfig {
    @Entry public static boolean enable = true;

    @Entry(min = 0.1f, max = 3.0f) public static float itemScale = 0.75f;

    @Entry public static String customScaleItems = "minecraft:golden_apple=1.5, minecraft:totem_of_undying=1.2";

    @Entry public static boolean castShadows = true;

    @Entry public static boolean renderSidesOfItems = false;

    @Entry public static boolean affect3DModels = true;

    private static final Map<String, Float> customScalesMap = new HashMap<>();
    private static String lastParsedConfig = "";

    public static float getCustomScale(String itemId) {
        if (!customScaleItems.equals(lastParsedConfig)) {
            customScalesMap.clear();
            lastParsedConfig = customScaleItems;
            if (customScaleItems != null && !customScaleItems.trim().isEmpty()) {
                for (String part : customScaleItems.split(",")) {
                    String[] kv = part.split("=");
                    if (kv.length == 2) {
                        try {
                            String key = kv[0].trim().toLowerCase();
                            float val = Float.parseFloat(kv[1].trim());
                            customScalesMap.put(key, val);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        return customScalesMap.getOrDefault(itemId, 1.0f);
    }
}
