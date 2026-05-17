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
        String versionStr = getMinecraftVersion();
        int patch = getMinecraftPatchVersion(versionStr);

        if (patch <= 5) {
            renderType = 0; // 1.21.5 (render method)
        } else if (patch == 6 || patch == 7) {
            renderType = 1; // 1.21.6 - 1.21.7 (3-param submit)
        } else {
            renderType = 2; // 1.21.8+ (4-param submit)
        }
    }

    private static String getMinecraftVersion() {
        // Try Fabric Loader API
        try {
            Class<?> fabricLoaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = fabricLoaderClass.getMethod("getInstance").invoke(null);
            Object modContainerOpt = fabricLoaderClass.getMethod("getModContainer", String.class).invoke(loader, "minecraft");
            if (modContainerOpt != null) {
                Object actualContainer = modContainerOpt.getClass().getMethod("get").invoke(modContainerOpt);
                Object metadata = actualContainer.getClass().getMethod("getMetadata").invoke(actualContainer);
                Object version = metadata.getClass().getMethod("getVersion").invoke(metadata);
                return (String) version.getClass().getMethod("getFriendlyString").invoke(version);
            }
        } catch (Exception ignored) {}

        // Try NeoForge FML ModList API
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            Object modContainerOpt = modListClass.getMethod("getModContainerById", String.class).invoke(modList, "minecraft");
            if (modContainerOpt != null) {
                Object actualContainer = modContainerOpt.getClass().getMethod("get").invoke(modContainerOpt);
                Object modInfo = actualContainer.getClass().getMethod("getModInfo").invoke(actualContainer);
                Object version = modInfo.getClass().getMethod("getVersion").invoke(modInfo);
                return version.toString();
            }
        } catch (Exception ignored) {}

        return "";
    }

    private static int getMinecraftPatchVersion(String versionStr) {
        if (versionStr == null || versionStr.isEmpty()) return 11; // default to modern
        try {
            String[] parts = versionStr.split("\\.");
            if (parts.length >= 3) {
                String patchPart = parts[2];
                StringBuilder sb = new StringBuilder();
                for (char c : patchPart.toCharArray()) {
                    if (Character.isDigit(c)) {
                        sb.append(c);
                    } else {
                        break;
                    }
                }
                return Integer.parseInt(sb.toString());
            }
        } catch (Exception ignored) {}
        return 11;
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
