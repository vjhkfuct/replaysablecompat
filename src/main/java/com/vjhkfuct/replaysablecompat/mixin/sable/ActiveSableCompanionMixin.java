package com.vjhkfuct.replaysablecompat.mixin.sable;

import com.vjhkfuct.replaysablecompat.compat.ReplayRecordingBridge;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ActiveSableCompanion.class)
public class ActiveSableCompanionMixin {

    @Inject(
            method = "getContaining(Lnet/minecraft/world/entity/Entity;)Ldev/ryanhcode/sable/sublevel/SubLevel;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void replaysablecompat$suppressReplayCameraContainingSubLevel(final Entity entity, final CallbackInfoReturnable<SubLevel> cir) {
        if (!ReplayRecordingBridge.shouldSuppressCameraSubLevelAttachment(entity)) {
            return;
        }

        cir.setReturnValue(null);
    }

    @Inject(method = "getTrackingSubLevel", at = @At("HEAD"), cancellable = true, remap = false)
    private void replaysablecompat$suppressReplayCameraTrackingSubLevel(final Entity entity, final CallbackInfoReturnable<SubLevel> cir) {
        if (ReplayRecordingBridge.shouldSuppressCameraSubLevelAttachment(entity)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "getVehicleSubLevel", at = @At("HEAD"), cancellable = true, remap = false)
    private void replaysablecompat$suppressReplayCameraVehicleSubLevel(final Entity entity, final CallbackInfoReturnable<SubLevel> cir) {
        if (ReplayRecordingBridge.shouldSuppressCameraSubLevelAttachment(entity)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "getTrackingOrVehicleSubLevel", at = @At("HEAD"), cancellable = true, remap = false)
    private void replaysablecompat$suppressReplayCameraTracking(final Entity entity, final CallbackInfoReturnable<SubLevel> cir) {
        if (ReplayRecordingBridge.shouldSuppressCameraSubLevelAttachment(entity)) {
            cir.setReturnValue(null);
        }
    }
}
