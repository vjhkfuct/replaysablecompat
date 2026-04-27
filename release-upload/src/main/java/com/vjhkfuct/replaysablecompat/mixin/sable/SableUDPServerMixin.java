package com.vjhkfuct.replaysablecompat.mixin.sable;

import com.vjhkfuct.replaysablecompat.compat.ReplayRecordingBridge;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.network.udp.SableUDPPacket;
import dev.ryanhcode.sable.network.udp.SableUDPServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SableUDPServer.class)
public class SableUDPServerMixin {

    @Inject(
            method = "sendUDPPacketLocal",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/network/client/SableClientNetworkEventLoop;tell(Ljava/lang/Runnable;)V",
                    remap = false
            ),
            remap = false
    )
    private void replaysablecompat$mirrorLocalUdpPayload(final SableUDPPacket packet, final CallbackInfo ci) {
        if (packet instanceof final SableTCPPacket tcpPacket) {
            ReplayRecordingBridge.mirrorClientboundPayload(tcpPacket);
        }
    }
}
