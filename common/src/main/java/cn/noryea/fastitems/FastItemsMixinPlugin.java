package cn.noryea.fastitems;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class FastItemsMixinPlugin implements IMixinConfigPlugin {
    private static int renderType = 0; // 0 = legacy, 1 = mid, 2 = modern

    @Override
    public void onLoad(String mixinPackage) {
        boolean hasSubmitCollector = false;
        try {
            try {
                Class.forName("net.minecraft.client.renderer.SubmitNodeCollector", false, getClass().getClassLoader());
            } catch (ClassNotFoundException e) {
                Class.forName("net.minecraft.class_11659", false, getClass().getClassLoader());
            }
            hasSubmitCollector = true;
        } catch (ClassNotFoundException ignored) {}

        if (!hasSubmitCollector) {
            renderType = 0; // 1.21.5 (render method)
        } else {
            boolean hasCreaking = false;
            try {
                try {
                    Class.forName("net.minecraft.world.entity.monster.creaking.Creaking", false, getClass().getClassLoader());
                } catch (ClassNotFoundException e) {
                    Class.forName("net.minecraft.class_10279", false, getClass().getClassLoader());
                }
                hasCreaking = true;
            } catch (ClassNotFoundException ignored) {}

            if (hasCreaking) {
                renderType = 2; // 1.21.8+ (modern 4-param submit)
            } else {
                renderType = 1; // 1.21.6 - 1.21.7 (mid 3-param submit)
            }
        }
    }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        List<String> mixins = new ArrayList<>();
        if (renderType == 2) {
            mixins.add("ItemEntityRendererMixin");
        } else if (renderType == 1) {
            mixins.add("ItemEntityRendererMixinMid");
        } else {
            mixins.add("ItemEntityRendererMixinLegacy");
        }
        return mixins;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
