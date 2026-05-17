package cn.noryea.fastitems;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class FastItemsMixinPlugin implements IMixinConfigPlugin {
    private static boolean useModernRenderer = false;

    @Override
    public void onLoad(String mixinPackage) {
        try {
            Class<?> rendererClass;
            try {
                rendererClass = Class.forName("net.minecraft.client.renderer.entity.ItemEntityRenderer", false, getClass().getClassLoader());
            } catch (ClassNotFoundException e) {
                rendererClass = Class.forName("net.minecraft.class_916", false, getClass().getClassLoader());
            }

            boolean hasSubmit = false;
            for (java.lang.reflect.Method method : rendererClass.getDeclaredMethods()) {
                for (Class<?> param : method.getParameterTypes()) {
                    if (param.getName().equals("net.minecraft.client.renderer.SubmitNodeCollector") || 
                        param.getName().equals("net.minecraft.class_11659")) {
                        hasSubmit = true;
                        break;
                    }
                }
            }
            useModernRenderer = hasSubmit;
        } catch (Exception e) {
            useModernRenderer = false;
        }
    }

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("ItemEntityRendererMixin")) {
            return useModernRenderer;
        }
        if (mixinClassName.endsWith("ItemEntityRendererMixinLegacy")) {
            return !useModernRenderer;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        List<String> mixins = new ArrayList<>();
        mixins.add("mixin.ItemEntityRendererMixin");
        mixins.add("mixin.ItemEntityRendererMixinLegacy");
        return mixins;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
