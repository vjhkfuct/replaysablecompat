package com.vjhkfuct.replaysablecompat.mixin.sable;

import com.vjhkfuct.replaysablecompat.compat.ReplayRecordingBridge;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.network.udp.AddressedSableUDPPacket;
import dev.ryanhcode.sable.network.client.SableClientNetworkEventLoop;
import dev.ryanhcode.sable.network.udp.handler.SableUDPChannelHandlerClient;
import io.netty.channel.ChannelHandlerContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SableUDPChannelHandlerClient.class)
public class SableUDPChannelHandlerClientMixin {

    @Redirect(
            method = "channelRead0",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/network/client/SableClientNetworkEventLoop;tell(Ljava/lang/Runnable;)V",
                    remap = false
            ),
            remap = false
    )
    private void replaysablecompat$wrapClientUdpPayloadTask(
            final SableClientNetworkEventLoop eventLoop,
            final Runnable runnable,
            final ChannelHandlerContext ctx,
            final AddressedSableUDPPacket msg
    ) {
        final Runnable wrappedRunnable = msg.packet() instanceof final SableTCPPacket tcpPacket
                ? ReplayRecordingBridge.wrapReplayMirrorIntoClientTask(runnable, tcpPacket)
                : runnable;
        eventLoop.tell(wrappedRunnable);
    }
}
