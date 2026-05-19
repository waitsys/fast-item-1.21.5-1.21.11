package cn.noryea.fastitems.mixin;

import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntityRenderState.class)
public class ItemEntityRenderStateMixin implements ItemEntityRenderStateAccessor {
    @Unique
    private float fastitems$customScale = 1.0f;

    @Override
    public float fastitems$getCustomScale() {
        return this.fastitems$customScale;
    }

    @Override
    public void fastitems$setCustomScale(float scale) {
        this.fastitems$customScale = scale;
    }
}
