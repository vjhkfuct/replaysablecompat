package com.vjhkfuct.replaysablecompat.mixin.sable;

import com.vjhkfuct.replaysablecompat.compat.ReplayRecordingBridge;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.network.udp.AddressedSableUDPPacket;
import dev.ryanhcode.sable.network.udp.handler.SableUDPChannelHandlerClient;
import io.netty.channel.ChannelHandlerContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SableUDPChannelHandlerClient.class)
public class SableUDPChannelHandlerClientMixin {

    @Inject(
            method = "channelRead0",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/network/client/SableClientNetworkEventLoop;tell(Ljava/lang/Runnable;)V",
                    remap = false
            ),
            remap = false
    )
    private void replaysablecompat$mirrorClientUdpPayload(final ChannelHandlerContext ctx, final AddressedSableUDPPacket msg, final CallbackInfo ci) {
        if (msg.packet() instanceof final SableTCPPacket tcpPacket) {
            ReplayRecordingBridge.mirrorClientboundPayload(tcpPacket);
        }
    }
}
