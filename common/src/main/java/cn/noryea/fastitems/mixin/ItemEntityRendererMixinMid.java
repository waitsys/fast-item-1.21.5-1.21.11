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
import net.minecraft.world.phys.AABB;
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
public abstract class ItemEntityRendererMixinMid extends EntityRenderer<ItemEntity, ItemEntityRenderState> {

    @Shadow @Final private RandomSource random;

    protected ItemEntityRendererMixinMid(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V", at = @At("HEAD"), cancellable = true)
    public void submit(ItemEntityRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci) {
        if (!FastItemsConfig.enable) return;
        if (state.item.isEmpty()) return;

        AABB aabb = state.item.getModelBoundingBox();
        boolean gui3d = aabb.getYsize() > 0.0625f;

        if (gui3d && !FastItemsConfig.affect3DModels) {
            return;
        }

        poseStack.pushPose();
        
        float minY = -((float)aabb.minY) + 0.0625f;
        float bob = Mth.sin((float)(state.ageInTicks / 10.0f + state.bobOffset)) * 0.1f + 0.1f;
        poseStack.translate(0.0f, bob + minY, 0.0f);
        poseStack.scale(0.75f, 0.75f, 0.75f);

        Quaternionf cameraRotation = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        poseStack.mulPose(cameraRotation);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        fastitems$renderMultipleFromCount(poseStack, bufferSource, state.lightCoords, state, gui3d, aabb);

        poseStack.popPose();
        try {
            for (java.lang.reflect.Method method : EntityRenderer.class.getDeclaredMethods()) {
                if ((method.getName().equals("submit") || method.getName().equals("method_3996")) 
                        && method.getParameterCount() == 3) {
                    method.setAccessible(true);
                    method.invoke(this, state, poseStack, bufferSource);
                    break;
                }
            }
        } catch (Exception ignored) {}
        ci.cancel();
    }

    @Unique
    private void fastitems$renderMultipleFromCount(PoseStack poseStack, MultiBufferSource multiBufferSource, int light, ItemClusterRenderState state, boolean gui3d, AABB aabb) {
        int count = state.count;
        int renderAmount = 1;
        if (count > 48) renderAmount = 5;
        else if (count > 32) renderAmount = 4;
        else if (count > 16) renderAmount = 3;
        else if (count > 1) renderAmount = 2;
        if (renderAmount == 0) return;

        this.random.setSeed(state.seed);
        float f11;
        float f13;

        if (!gui3d) {
            float f7 = -0.0F * (float)(renderAmount - 1) * 0.5F;
            f11 = -0.0F * (float)(renderAmount - 1) * 0.5F;
            f13 = -0.09375F * (float)(renderAmount - 1) * 0.5F;
            poseStack.translate(f7, f11, f13);
        }

        for (int k = 0; k < renderAmount; ++k) {
            poseStack.pushPose();
            if (k > 0) {
                if (gui3d) {
                    f11 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    f13 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    float f10 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F;
                    poseStack.translate(f11, f13, f10);
                } else {
                    f11 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    f13 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    poseStack.translate(f11, f13, 0.0);
                }
            }

            if (!FastItemsConfig.renderSidesOfItems && !gui3d) {
                poseStack.scale(1.0F, 1.0F, 0.01F);
            }

            try {
                boolean invoked = false;
                for (java.lang.reflect.Method method : state.item.getClass().getMethods()) {
                    if ((method.getName().equals("submit") || method.getName().equals("render")) 
                            && method.getParameterCount() == 5) {
                        method.setAccessible(true);
                        method.invoke(state.item, poseStack, multiBufferSource, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
                        invoked = true;
                        break;
                    }
                }
                if (!invoked) {
                    for (java.lang.reflect.Method method : state.item.getClass().getMethods()) {
                        if ((method.getName().equals("submit") || method.getName().equals("render")) 
                                && method.getParameterCount() == 4) {
                            method.setAccessible(true);
                            method.invoke(state.item, poseStack, multiBufferSource, light, OverlayTexture.NO_OVERLAY);
                            invoked = true;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[FastItems] Failed to render item reflectively: " + e.toString());
            }

            poseStack.popPose();
            
            if (!gui3d) {
                float offset = FastItemsConfig.renderSidesOfItems ? 0.02f : 0.001f;
                poseStack.translate(0.0, 0.0, offset);
            }
        }
    }
}
