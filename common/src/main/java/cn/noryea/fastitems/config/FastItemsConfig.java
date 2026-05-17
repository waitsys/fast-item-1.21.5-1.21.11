package cn.noryea.fastitems.config;

import eu.midnightdust.lib.config.MidnightConfig;

public class FastItemsConfig extends MidnightConfig {
    @Entry public static boolean enable = true;

    @Entry(min = 0.1f, max = 3.0f) public static float itemScale = 0.75f;

    @Entry public static boolean castShadows = true;

    @Entry public static boolean renderSidesOfItems = false;

    @Entry public static boolean affect3DModels = true;
}
