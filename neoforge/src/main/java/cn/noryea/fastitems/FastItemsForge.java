package cn.noryea.fastitems;

import cn.noryea.fastitems.config.FastItemsConfig;
import net.neoforged.fml.common.Mod;

@Mod(value = "fastitems")
public class FastItemsForge {
    public FastItemsForge() {
        FastItemsConfig.init("fastitems", FastItemsConfig.class);
    }
}
