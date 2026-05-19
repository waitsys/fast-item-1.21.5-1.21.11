package cn.noryea.fastitems.mixin;

import cn.noryea.fastitems.config.FastItemsConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.AABB;
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

    @Override
    public float getShadowRadius(ItemEntityRenderState state) {
        return FastItemsConfig.castShadows ? super.getShadowRadius(state) : 0.0f;
    }

    protected ItemEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(
        method = {
            "updateRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V",
            "method_62470(Lnet/minecraft/class_1542;Lnet/minecraft/class_10039;F)V"
        },
        at = @At("TAIL"),
        require = 0,
        remap = false
    )
    public void updateRenderState(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        ItemStack stack = entity.getItem();
        if (stack != null && !stack.isEmpty()) {
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            float scale = FastItemsConfig.getCustomScale(itemId);
            ((ItemEntityRenderStateAccessor) state).fastitems$setCustomScale(scale);
        } else {
            ((ItemEntityRenderStateAccessor) state).fastitems$setCustomScale(1.0f);
        }
    }

    @Inject(
        method = {
            "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            "method_3996(Lnet/minecraft/class_10039;Lnet/minecraft/class_4587;Lnet/minecraft/class_11659;Lnet/minecraft/class_12075;)V"
        },
        at = @At("HEAD"),
        cancellable = true,
        require = 0,
        remap = false
    )
    public void submit(ItemEntityRenderState state, PoseStack poseStack, SubmitNodeCollector bufferSource, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (!FastItemsConfig.enable) {
            return;
        }

        if (state.item.isEmpty()) {
            return;
        }

        AABB aabb = state.item.getModelBoundingBox();
        boolean gui3d = aabb.getYsize() > 0.0625f;

        if (gui3d && !FastItemsConfig.affect3DModels) {
            return; // Fallback to vanilla for 3D models
        }

        poseStack.pushPose();
        
        float minY = -((float)aabb.minY) + 0.0625f;
        float bob = (float) Math.sin((double)(state.ageInTicks / 10.0f + state.bobOffset)) * 0.1f + 0.1f;
        poseStack.translate(0.0f, bob + minY, 0.0f);
        
        float customScale = ((ItemEntityRenderStateAccessor) state).fastitems$getCustomScale();
        float finalScale = FastItemsConfig.itemScale * customScale;
        poseStack.scale(finalScale, finalScale, finalScale); // Custom configurable and item-specific size

        // face to player (always look at the camera)
        poseStack.mulPose(cameraRenderState.orientation);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        fastitems$renderMultipleFromCount(poseStack, bufferSource, state.lightCoords, state, gui3d, aabb);

        poseStack.popPose();
        super.submit(state, poseStack, bufferSource, cameraRenderState);

        ci.cancel();
    }

    @Unique
    private void fastitems$renderMultipleFromCount(PoseStack poseStack, SubmitNodeCollector multiBufferSource, int light, ItemClusterRenderState state, boolean gui3d, AABB aabb) {
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
                    poseStack.translate(this.fastitems$shouldSpreadItems() ? f11 : 0.0F, this.fastitems$shouldSpreadItems() ? f13 : 0.0F, this.fastitems$shouldSpreadItems() ? f10 : 0.0F);
                } else {
                    f11 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    f13 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.15F * 0.5F;
                    poseStack.translate(this.fastitems$shouldSpreadItems() ? f11 : 0.0, this.fastitems$shouldSpreadItems() ? f13 : 0.0, 0.0);
                }
            }

            if (!FastItemsConfig.renderSidesOfItems && !gui3d) {
                poseStack.scale(1.0F, 1.0F, 0.01F);
            }

            state.item.submit(poseStack, multiBufferSource, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
            
            if (!gui3d) {
                // translate by z-scale for thickness stacking - use much smaller offset
                float offset = FastItemsConfig.renderSidesOfItems ? 0.02f : 0.001f;
                poseStack.translate(0.0, 0.0, offset);
            }
        }
    }

    @Unique
    private boolean fastitems$shouldSpreadItems() {
        return true;
    }
}