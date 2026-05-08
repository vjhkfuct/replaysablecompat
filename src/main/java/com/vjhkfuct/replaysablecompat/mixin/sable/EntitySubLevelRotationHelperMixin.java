package com.vjhkfuct.replaysablecompat.mixin.sable;

import com.vjhkfuct.replaysablecompat.compat.ReplayRecordingBridge;
import dev.ryanhcode.sable.mixinhelpers.camera.camera_rotation.EntitySubLevelRotationHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import org.joml.Quaterniond;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

@Mixin(EntitySubLevelRotationHelper.class)
public class EntitySubLevelRotationHelperMixin {

    @Inject(method = "getSubLevelInheritedOrientation", at = @At("HEAD"), cancellable = true, remap = false)
    private static void replaysablecompat$suppressReplayCameraRotation(final Entity cameraEntity,
                                                                       final Function<SubLevel, ?> poseProvider,
                                                                       final EntitySubLevelRotationHelper.Type type,
                                                                       final CallbackInfoReturnable<Quaterniond> cir) {
        if (type == EntitySubLevelRotationHelper.Type.CAMERA
                && ReplayRecordingBridge.shouldSuppressCameraSubLevelAttachment(cameraEntity)) {
            cir.setReturnValue(null);
        }
    }
}
