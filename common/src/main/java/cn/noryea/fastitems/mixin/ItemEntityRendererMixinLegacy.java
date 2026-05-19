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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
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
            "render(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            "method_3996(Lnet/minecraft/class_10039;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;I)V"
        },
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    public void render(ItemEntityRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if (!FastItemsConfig.enable) return;
        if (state.item == null || state.item.isEmpty()) return;

        poseStack.pushPose();

        float bob = (float) Math.sin((double)(state.ageInTicks / 10.0f + state.bobOffset)) * 0.1f + 0.1f;
        poseStack.translate(0.0f, bob + 0.0625f, 0.0f);
        
        float customScale = ((ItemEntityRenderStateAccessor) state).fastitems$getCustomScale();
        float finalScale = FastItemsConfig.itemScale * customScale;
        poseStack.scale(finalScale, finalScale, finalScale);

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
                boolean invoked = false;
                for (java.lang.reflect.Method method : state.item.getClass().getMethods()) {
                    Class<?>[] params = method.getParameterTypes();
                    if (params.length == 4 || params.length == 5) {
                        if (params[0].getName().contains("PoseStack") || params[0].getName().contains("class_4587")) {
                            if (params[2] == int.class && params[3] == int.class) {
                                if (params.length == 5 && params[4] != int.class) {
                                    continue;
                                }
                                method.setAccessible(true);
                                if (params.length == 4) {
                                    method.invoke(state.item, poseStack, bufferSource, light, OverlayTexture.NO_OVERLAY);
                                } else {
                                    int outlineColor = 0;
                                    try {
                                        java.lang.reflect.Field field = state.getClass().getField("outlineColor");
                                        outlineColor = (int) field.get(state);
                                    } catch (Exception ignored) {
                                        try {
                                            java.lang.reflect.Field field = state.getClass().getDeclaredField("outlineColor");
                                            field.setAccessible(true);
                                            outlineColor = (int) field.get(state);
                                        } catch (Exception ignored2) {}
                                    }
                                    method.invoke(state.item, poseStack, bufferSource, light, OverlayTexture.NO_OVERLAY, outlineColor);
                                }
                                invoked = true;
                                break;
                            }
                        }
                    }
                }
                if (!invoked) {
                    System.err.println("[FastItems] Failed to find rendering method on " + state.item.getClass().getName());
                    for (java.lang.reflect.Method method : state.item.getClass().getMethods()) {
                        System.err.println("[FastItems]   Method: " + method.getName() + " params: " + method.getParameterCount());
                    }
                }
            } catch (Exception e) {
                System.err.println("[FastItems] Render exception: " + e.toString());
                e.printStackTrace();
            }
            poseStack.popPose();
            poseStack.translate(0.0, 0.0, 0.001);
        }
    }
}
