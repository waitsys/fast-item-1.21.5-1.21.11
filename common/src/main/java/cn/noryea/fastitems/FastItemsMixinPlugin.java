package cn.noryea.fastitems;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
        useModernRenderer = checkModernRenderer();
    }

    private static boolean checkModernRenderer() {
        String[] paths = {
            "net/minecraft/client/renderer/entity/ItemEntityRenderer.class",
            "net/minecraft/class_916.class"
        };
        for (String path : paths) {
            try (InputStream is = FastItemsMixinPlugin.class.getClassLoader().getResourceAsStream(path)) {
                if (is != null) {
                    byte[] bytes = readAllBytes(is);
                    String content = new String(bytes, StandardCharsets.ISO_8859_1);
                    // Check if ItemEntityRenderer references CameraRenderState (class_12075),
                    // which was only introduced in 1.21.8's submit rendering pipeline.
                    if (content.contains("class_12075") || content.contains("CameraRenderState")) {
                        return true;
                    }
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
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
