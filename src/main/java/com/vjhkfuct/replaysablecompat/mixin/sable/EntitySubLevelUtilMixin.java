package com.vjhkfuct.replaysablecompat.mixin.sable;

import com.vjhkfuct.replaysablecompat.compat.ReplayRecordingBridge;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntitySubLevelUtil.class)
public class EntitySubLevelUtilMixin {

    @Inject(method = "shouldKick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void replaysablecompat$ignoreReplayFreeCameraKickLogic(final Entity entity, final CallbackInfoReturnable<Boolean> cir) {
        if (!ReplayRecordingBridge.shouldSuppressCameraSubLevelAttachment(entity)) {
            return;
        }

        cir.setReturnValue(false);
    }
}
