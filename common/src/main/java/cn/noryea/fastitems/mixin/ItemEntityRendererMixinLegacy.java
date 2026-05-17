package cn.noryea.fastitems.mixin;

import cn.noryea.fastitems.config.FastItemsConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class ItemEntityRendererMixinLegacy extends EntityRenderer<ItemEntity, ItemEntityRenderState> {

    @Shadow @Final private RandomSource random;

    protected ItemEntityRendererMixinLegacy(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(
        method = {
            "render(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            "method_3996(Lnet/minecraft/class_10039;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V"
        },
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    public void render(ItemEntityRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if (!FastItemsConfig.enable) return;
        if (state.item.isEmpty()) return;

        poseStack.pushPose();

        float bob = Mth.sin((float)(state.ageInTicks / 10.0f + state.bobOffset)) * 0.1f + 0.1f;
        poseStack.translate(0.0f, bob + 0.0625f, 0.0f);
        poseStack.scale(0.75f, 0.75f, 0.75f);

        Quaternionf cameraRotation = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        poseStack.mulPose(cameraRotation);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        fastitems$renderLegacy(poseStack, bufferSource, packedLight, state);

        poseStack.popPose();
        ci.cancel();
    }

    @Unique
    private void fastitems$renderLegacy(PoseStack poseStack, MultiBufferSource bufferSource, int light, ItemClusterRenderState state) {
        int count = state.count;
        int renderAmount = 1;
        if (count > 48) renderAmount = 5;
        else if (count > 32) renderAmount = 4;
        else if (count > 16) renderAmount = 3;
        else if (count > 1) renderAmount = 2;

        this.random.setSeed(state.seed);

        for (int k = 0; k < renderAmount; ++k) {
            poseStack.pushPose();
            if (k > 0) {
                float xOff = (this.random.nextFloat() * 2.0F - 1.0F) * 0.075F;
                float yOff = (this.random.nextFloat() * 2.0F - 1.0F) * 0.075F;
                poseStack.translate(xOff, yOff, 0.0);
            }
            if (!FastItemsConfig.renderSidesOfItems) {
                poseStack.scale(1.0F, 1.0F, 0.01F);
            }
            try {
                java.lang.reflect.Method renderMethod = state.item.getClass().getMethod("render",
                    PoseStack.class, MultiBufferSource.class, int.class, int.class);
                renderMethod.invoke(state.item, poseStack, bufferSource, light, OverlayTexture.NO_OVERLAY);
            } catch (Exception ignored) {}
            poseStack.popPose();
            poseStack.translate(0.0, 0.0, 0.001);
        }
    }
}
