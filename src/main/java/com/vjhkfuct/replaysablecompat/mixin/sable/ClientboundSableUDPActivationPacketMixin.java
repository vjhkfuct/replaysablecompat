package com.vjhkfuct.replaysablecompat.mixin.sable;

import com.vjhkfuct.replaysablecompat.compat.ReplayRecordingBridge;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundSableUDPActivationPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientboundSableUDPActivationPacket.class)
public class ClientboundSableUDPActivationPacketMixin {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private void replaysablecompat$cancelReplayUdpActivation(final CallbackInfo ci) {
        if (ReplayRecordingBridge.shouldSuppressUdpActivation()) {
            ci.cancel();
        }
    }
}
