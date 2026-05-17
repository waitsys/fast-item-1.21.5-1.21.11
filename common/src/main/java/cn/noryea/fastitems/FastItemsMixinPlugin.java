package cn.noryea.fastitems;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class FastItemsMixinPlugin implements IMixinConfigPlugin {
    private static int renderType = 0; // 0 = legacy (1.21.5 - 1.21.7), 2 = modern (1.21.8+)

    @Override
    public void onLoad(String mixinPackage) {
        String versionStr = getMinecraftVersion();
        int patch = getMinecraftPatchVersion(versionStr);
        System.out.println("[FastItems] Detected Minecraft version string: '" + versionStr + "' (parsed patch: " + patch + ")");

        if (patch <= 7) {
            renderType = 0; // 1.21.5 - 1.21.7 (Legacy render method)
        } else {
            renderType = 2; // 1.21.8+ (Modern submit method)
            // Perform diagnostic ASM scan using classloader resource stream
            try {
                java.io.InputStream stream = FastItemsMixinPlugin.class.getClassLoader().getResourceAsStream("net/minecraft/class_916.class");
                if (stream != null) {
                    ClassReader reader = new ClassReader(stream);
                    ClassNode node = new ClassNode();
                    reader.accept(node, 0);
                    System.out.println("[FastItems] ASM scan of net/minecraft/class_916 methods for modern version:");
                    for (MethodNode method : node.methods) {
                        System.out.println("[FastItems] ASM Method: " + method.name + " " + method.desc);
                    }
                } else {
                    System.err.println("[FastItems] Could not find stream for net/minecraft/class_916.class");
                }
            } catch (Exception e) {
                System.err.println("[FastItems] ASM scan failed for modern version: " + e.toString());
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
