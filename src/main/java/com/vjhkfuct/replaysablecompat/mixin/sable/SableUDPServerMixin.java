package com.vjhkfuct.replaysablecompat.mixin.sable;

import com.vjhkfuct.replaysablecompat.compat.ReplayRecordingBridge;
import dev.ryanhcode.sable.network.client.SableClientNetworkEventLoop;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.network.udp.SableUDPPacket;
import dev.ryanhcode.sable.network.udp.SableUDPServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SableUDPServer.class)
public class SableUDPServerMixin {

    @Redirect(
            method = "sendUDPPacketLocal",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/network/client/SableClientNetworkEventLoop;tell(Ljava/lang/Runnable;)V",
                    remap = false
            ),
            remap = false
    )
    private void replaysablecompat$wrapLocalUdpPayloadTask(
            final SableClientNetworkEventLoop eventLoop,
            final Runnable runnable,
            final SableUDPPacket packet
    ) {
        final Runnable wrappedRunnable = packet instanceof final SableTCPPacket tcpPacket
                ? ReplayRecordingBridge.wrapReplayMirrorIntoClientTask(runnable, tcpPacket)
                : runnable;
        eventLoop.tell(wrappedRunnable);
    }
}
