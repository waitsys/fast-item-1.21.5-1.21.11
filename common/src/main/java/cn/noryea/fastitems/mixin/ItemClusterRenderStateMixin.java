package cn.noryea.fastitems.mixin;

import cn.noryea.fastitems.config.FastItemsConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemClusterRenderState.class)
@Environment(EnvType.CLIENT)
public class ItemClusterRenderStateMixin {

    @Inject(method = "getRenderedAmount", at = @At("HEAD"), cancellable = true)
    private static void getRenderedAmount(int count, CallbackInfoReturnable<Integer> cir) {
        if (!FastItemsConfig.enable) {
            return;
        }
        // Return exactly the item count to bypass vanilla max limit of 5.
        cir.setReturnValue(count);
    }
}
