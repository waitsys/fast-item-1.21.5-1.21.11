package cn.noryea.fastitems;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class FastItemsMixinPlugin implements IMixinConfigPlugin {
    private static int renderType = 0; // 0 = legacy (1.21.5 - 1.21.10), 2 = modern (1.21.11+)

    @Override
    public void onLoad(String mixinPackage) {
        String versionStr = getMinecraftVersion();
        System.out.println("[FastItems] Loading Mod for Minecraft version string: '" + versionStr + "'");

        // Dynamically detect rendering pipeline using Class.forName check on SubmitNodeCollector
        try {
            Class.forName("net.minecraft.class_11659"); // Yarn name for SubmitNodeCollector (1.21.11+)
            renderType = 2; // modern (1.21.11+)
            System.out.println("[FastItems] Detected SubmitNodeCollector (class_11659) -> using Modern rendering pipeline.");
        } catch (ClassNotFoundException e1) {
            try {
                Class.forName("net.minecraft.client.renderer.SubmitNodeCollector"); // Mojang/NeoForge name
                renderType = 2; // modern (1.21.11+)
                System.out.println("[FastItems] Detected SubmitNodeCollector -> using Modern rendering pipeline.");
            } catch (ClassNotFoundException e2) {
                renderType = 0; // legacy (1.21.5 - 1.21.10)
                System.out.println("[FastItems] SubmitNodeCollector not found -> using Legacy rendering pipeline.");
            }
        }
        System.out.println("[FastItems] Selected renderType: " + renderType);
    }

    private static String getMinecraftVersion() {
        try {
            Class<?> fabricLoaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Class<?> modContainerClass = Class.forName("net.fabricmc.loader.api.ModContainer");
            Class<?> modMetadataClass = Class.forName("net.fabricmc.loader.api.metadata.ModMetadata");
            Class<?> versionClass = Class.forName("net.fabricmc.loader.api.Version");

            Object loader = fabricLoaderClass.getMethod("getInstance").invoke(null);
            java.util.Optional<?> modContainerOpt = (java.util.Optional<?>) fabricLoaderClass.getMethod("getModContainer", String.class).invoke(loader, "minecraft");
            
            if (modContainerOpt.isPresent()) {
                Object actualContainer = modContainerOpt.get();
                Object metadata = modContainerClass.getMethod("getMetadata").invoke(actualContainer);
                Object version = modMetadataClass.getMethod("getVersion").invoke(metadata);
                return (String) versionClass.getMethod("getFriendlyString").invoke(version);
            }
        } catch (Exception e) {
            System.err.println("[FastItems] Fabric version detection failed:");
            e.printStackTrace();
        }

        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Class<?> modContainerClass = Class.forName("net.neoforged.fml.ModContainer");
            Class<?> iModInfoClass = Class.forName("net.neoforged.neoforgespi.language.IModInfo");
            Class<?> mavenVersionClass = Class.forName("org.apache.maven.artifact.versioning.ArtifactVersion");

            Object modList = modListClass.getMethod("get").invoke(null);
            java.util.Optional<?> modContainerOpt = (java.util.Optional<?>) modListClass.getMethod("getModContainerById", String.class).invoke(modList, "minecraft");
            
            if (modContainerOpt.isPresent()) {
                Object actualContainer = modContainerOpt.get();
                Object modInfo = modContainerClass.getMethod("getModInfo").invoke(actualContainer);
                Object version = iModInfoClass.getMethod("getVersion").invoke(modInfo);
                return version.toString();
            }
        } catch (Exception e) {
            System.err.println("[FastItems] NeoForge version detection failed:");
            e.printStackTrace();
        }

        return "";
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
        } else {
            mixins.add("ItemEntityRendererMixinLegacy");
        }
        return mixins;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (targetClassName.equals("net.minecraft.class_916") || targetClassName.equals("net.minecraft.client.renderer.entity.ItemEntityRenderer")) {
            System.out.println("[FastItems] preApply hook triggered for class: " + targetClassName);
            for (MethodNode method : targetClass.methods) {
                System.out.println("[FastItems] ASM Method found: " + method.name + " " + method.desc);
            }
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
