package com.vjhkfuct.replaysablecompat.mixin.vanilla;

import com.vjhkfuct.replaysablecompat.compat.ReplayRecordingBridge;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    @Nullable
    public abstract Entity getVehicle();

    @Shadow
    public abstract void removeVehicle();

    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", at = @At("HEAD"), cancellable = true)
    private void replaysablecompat$preventReplayFreeCameraMount(final Entity vehicle, final boolean force, final CallbackInfoReturnable<Boolean> cir) {
        final Entity self = (Entity) (Object) this;
        if (!ReplayRecordingBridge.shouldSuppressCameraSubLevelAttachment(self)) {
            return;
        }

        cir.setReturnValue(false);
    }

    @Inject(method = "positionRider(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void replaysablecompat$preventReplayFreeCameraPassengerPosition(final Entity passenger, final CallbackInfo ci) {
        if (!ReplayRecordingBridge.shouldSuppressCameraSubLevelAttachment(passenger)) {
            return;
        }

        ci.cancel();
    }

    @Inject(method = "rideTick", at = @At("HEAD"), cancellable = true)
    private void replaysablecompat$detachReplayFreeCameraVehicle(final CallbackInfo ci) {
        final Entity self = (Entity) (Object) this;
        final Entity vehicle = this.getVehicle();
        if (vehicle == null || !ReplayRecordingBridge.shouldSuppressCameraSubLevelAttachment(self)) {
            return;
        }

        this.removeVehicle();
        ci.cancel();
    }
}
