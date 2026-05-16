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
public abstract class ItemEntityRendererMixin extends EntityRenderer<ItemEntity, ItemEntityRenderState> {

    @Shadow @Final private RandomSource random;

    protected ItemEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    public void render(ItemEntityRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if (!FastItemsConfig.enable) {
            return;
        }

        if (state.item.isEmpty()) {
            return;
        }

        poseStack.pushPose();

        // Simple bobbing animation
        float bob = Mth.sin((float)(state.ageInTicks / 10.0f + state.bobOffset)) * 0.1f + 0.1f;
        poseStack.translate(0.0f, bob + 0.0625f, 0.0f);
        poseStack.scale(0.75f, 0.75f, 0.75f);

        // Face to player (always look at the camera)
        Quaternionf cameraRotation = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        poseStack.mulPose(cameraRotation);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        fastitems$renderMultipleFromCount(poseStack, bufferSource, packedLight, state);

        poseStack.popPose();

        ci.cancel();
    }

    @Unique
    private void fastitems$renderMultipleFromCount(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, ItemClusterRenderState state) {
        int count = state.count;
        int renderAmount = 1;
        if (count > 48) {
            renderAmount = 5;
        } else if (count > 32) {
            renderAmount = 4;
        } else if (count > 16) {
            renderAmount = 3;
        } else if (count > 1) {
            renderAmount = 2;
        }
        if (renderAmount == 0) return;

        this.random.setSeed(state.seed);

        for (int k = 0; k < renderAmount; ++k) {
            poseStack.pushPose();
            if (k > 0) {
                float xOff = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                float yOff = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                poseStack.translate(xOff, yOff, 0.0);
            }

            if (!FastItemsConfig.renderSidesOfItems) {
                poseStack.scale(1.0F, 1.0F, 0.01F);
            }

            state.item.render(poseStack, multiBufferSource, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();

            poseStack.translate(0.0, 0.0, 0.001);
        }
    }
}