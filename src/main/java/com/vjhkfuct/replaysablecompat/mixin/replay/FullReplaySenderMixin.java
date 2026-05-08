package com.vjhkfuct.replaysablecompat.mixin.replay;

import com.vjhkfuct.replaysablecompat.compat.ReplayRecordingBridge;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.replaymod.replay.FullReplaySender", remap = false)
public abstract class FullReplaySenderMixin {

    @Shadow
    protected boolean allowMovement;

    @Inject(
            method = "processPacket",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void replaysablecompat$suppressReplayFreeCameraPlayerCorrection(final Packet<?> packet, final CallbackInfoReturnable<Object> cir) {
        if (!(packet instanceof ClientboundPlayerPositionPacket)) {
            return;
        }

        if (!ReplayRecordingBridge.isReplayFreeCameraView() || this.allowMovement) {
            return;
        }

        cir.setReturnValue(null);
    }
}
