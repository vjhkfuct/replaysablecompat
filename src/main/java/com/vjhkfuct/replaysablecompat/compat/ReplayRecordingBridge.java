package com.vjhkfuct.replaysablecompat.compat;

import com.vjhkfuct.replaysablecompat.ReplaySableCompat;
import dev.ryanhcode.sable.mixinterface.udp.ConnectionExtension;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundFreezePlayerPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.RejectedExecutionException;

public final class ReplayRecordingBridge {
    private static final String RECORDING_CLASS_NAME = "com.replaymod.recording.ReplayModRecording";
    private static final String REPLAY_CLASS_NAME = "com.replaymod.replay.ReplayModReplay";
    private static final String MCVER_CLASS_NAME = "com.replaymod.core.versions.MCVer";
    private static final String REPLAY_PACKET_CLASS_NAME = "com.replaymod.replaystudio.protocol.Packet";
    private static final String REPLAY_PACKET_TYPE_CLASS_NAME = "com.replaymod.replaystudio.protocol.PacketType";
    private static final String REPLAY_PACKET_TYPE_REGISTRY_CLASS_NAME = "com.replaymod.replaystudio.protocol.PacketTypeRegistry";
    private static final String REPLAY_STATE_CLASS_NAME = "com.replaymod.replaystudio.lib.viaversion.api.protocol.packet.State";
    private static final String REPLAY_BUFFER_CLASS_NAME = "com.github.steveice10.netty.buffer.ByteBuf";
    private static final String REPLAY_UNPOOLED_CLASS_NAME = "com.github.steveice10.netty.buffer.Unpooled";

    private static boolean recordingReflectionInitialized;
    private static boolean recordingReflectionUnavailable;
    private static Field recordingInstanceField;
    private static Method getConnectionEventHandlerMethod;
    private static Method getPacketListenerMethod;
    private static boolean replayPacketReflectionInitialized;
    private static boolean replayPacketReflectionUnavailable;
    private static Constructor<?> replayPacketConstructor;
    private static Method getReplayPacketTypeRegistryMethod;
    private static Method replayWrappedBufferMethod;
    private static Object replayPlayState;
    private static Object replayPluginMessagePacketType;

    private static boolean replayReflectionInitialized;
    private static boolean replayReflectionUnavailable;
    private static Field replayInstanceField;
    private static Method getReplayHandlerMethod;
    private static Method isCameraViewMethod;

    private static boolean recordingLookupLogged;
    private static boolean replayLookupLogged;

    private ReplayRecordingBridge() {
    }

    public static void mirrorClientboundPayload(final CustomPacketPayload payload) {
        if (!shouldRecordPayload(payload)) {
            return;
        }

        final Object packetListener = getPacketListener();
        if (packetListener == null) {
            return;
        }

        final Object packet = createReplayPayloadPacket(payload);
        if (packet == null) {
            return;
        }

        final Method saveMethod = getSaveMethod(packetListener.getClass(), packet);
        if (saveMethod == null) {
            logRecordingLookupFailure("ReplayMod PacketListener has no compatible save(Packet) overload");
            return;
        }

        try {
            saveMethod.invoke(packetListener, packet);
        } catch (final InvocationTargetException e) {
            if (e.getCause() instanceof RejectedExecutionException) {
                return;
            }

            logRecordingLookupFailure("Failed to mirror Sable payload into ReplayMod recording", e);
        } catch (final ReflectiveOperationException e) {
            logRecordingLookupFailure("Failed to mirror Sable payload into ReplayMod recording", e);
        }
    }

    public static Runnable wrapReplayMirrorIntoClientTask(final Runnable runnable, @Nullable final CustomPacketPayload payload) {
        if (payload == null) {
            return runnable;
        }

        return () -> {
            mirrorClientboundPayload(payload);
            runnable.run();
        };
    }

    private static Object createReplayPayloadPacket(final CustomPacketPayload payload) {
        if (!initializeReplayPacketReflection()) {
            return null;
        }

        try {
            final byte[] encodedPayload = encodeReplayPayload(payload);
            final Object packetRegistry = getReplayPacketTypeRegistryMethod.invoke(null, replayPlayState);
            final Object packetBuffer = replayWrappedBufferMethod.invoke(null, encodedPayload);
            return replayPacketConstructor.newInstance(packetRegistry, replayPluginMessagePacketType, packetBuffer);
        } catch (final ReflectiveOperationException e) {
            logRecordingLookupFailure("Failed to encode Sable payload as a replay custom payload packet", e);
            return null;
        }
    }

    private static boolean shouldRecordPayload(final CustomPacketPayload payload) {
        // This packet only drives the local live player's client-side anchor/tracking state.
        // Replaying it later re-applies that local camera lock to the replay viewpoint.
        return !(payload instanceof ClientboundFreezePlayerPacket);
    }

    public static boolean shouldSuppressUdpActivation() {
        if (isReplayPlaybackActive()) {
            return true;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            return true;
        }

        final Connection connection = minecraft.getConnection().getConnection();
        final ConnectionExtension connectionExtension = (ConnectionExtension) connection;
        if (connectionExtension.sable$getUDPChannel() == null) {
            return true;
        }

        final SocketAddress remoteAddress = connection.getRemoteAddress();
        return !(remoteAddress instanceof InetSocketAddress);
    }

    public static boolean isReplayPlaybackActive() {
        if (!initializeReplayReflection()) {
            return false;
        }

        try {
            final Object replayInstance = replayInstanceField.get(null);
            if (replayInstance == null) {
                return false;
            }

            return getReplayHandlerMethod.invoke(replayInstance) != null;
        } catch (final ReflectiveOperationException e) {
            logReplayLookupFailure("Failed to query ReplayMod replay state", e);
            return false;
        }
    }

    public static boolean shouldSuppressCameraSubLevelAttachment(@Nullable final Entity entity) {
        if (entity == null || !isReplayFreeCameraView()) {
            return false;
        }

        return entity == Minecraft.getInstance().getCameraEntity();
    }

    public static boolean isReplayFreeCameraView() {
        if (!initializeReplayReflection()) {
            return false;
        }

        try {
            final Object replayInstance = replayInstanceField.get(null);
            if (replayInstance == null) {
                return false;
            }

            final Object replayHandler = getReplayHandlerMethod.invoke(replayInstance);
            if (replayHandler == null) {
                return false;
            }

            return Boolean.TRUE.equals(isCameraViewMethod.invoke(replayHandler));
        } catch (final ReflectiveOperationException e) {
            logReplayLookupFailure("Failed to query ReplayMod camera view state", e);
            return false;
        }
    }

    private static Object getPacketListener() {
        if (!initializeRecordingReflection()) {
            return null;
        }

        try {
            final Object recordingInstance = recordingInstanceField.get(null);
            if (recordingInstance == null) {
                return null;
            }

            final Object connectionEventHandler = getConnectionEventHandlerMethod.invoke(recordingInstance);
            if (connectionEventHandler == null) {
                return null;
            }

            return getPacketListenerMethod.invoke(connectionEventHandler);
        } catch (final ReflectiveOperationException e) {
            logRecordingLookupFailure("Failed to query ReplayMod recording state", e);
            return null;
        }
    }

    private static boolean initializeRecordingReflection() {
        if (recordingReflectionInitialized) {
            return !recordingReflectionUnavailable;
        }

        recordingReflectionInitialized = true;

        try {
            final Class<?> recordingClass = Class.forName(RECORDING_CLASS_NAME);
            recordingInstanceField = recordingClass.getField("instance");
            getConnectionEventHandlerMethod = recordingClass.getMethod("getConnectionEventHandler");
            final Class<?> connectionEventHandlerClass = getConnectionEventHandlerMethod.getReturnType();
            getPacketListenerMethod = connectionEventHandlerClass.getMethod("getPacketListener");
            return true;
        } catch (final ClassNotFoundException e) {
            recordingReflectionUnavailable = true;
            return false;
        } catch (final ReflectiveOperationException e) {
            recordingReflectionUnavailable = true;
            logRecordingLookupFailure("Failed to initialize ReplayMod recording reflection", e);
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean initializeReplayPacketReflection() {
        if (replayPacketReflectionInitialized) {
            return !replayPacketReflectionUnavailable;
        }

        replayPacketReflectionInitialized = true;

        try {
            final Class<?> packetClass = Class.forName(REPLAY_PACKET_CLASS_NAME);
            final Class<?> packetTypeClass = Class.forName(REPLAY_PACKET_TYPE_CLASS_NAME);
            final Class<?> packetTypeRegistryClass = Class.forName(REPLAY_PACKET_TYPE_REGISTRY_CLASS_NAME);
            final Class<?> stateClass = Class.forName(REPLAY_STATE_CLASS_NAME);
            final Class<?> replayByteBufClass = Class.forName(REPLAY_BUFFER_CLASS_NAME);
            final Class<?> replayUnpooledClass = Class.forName(REPLAY_UNPOOLED_CLASS_NAME);
            final Class<?> mcVerClass = Class.forName(MCVER_CLASS_NAME);

            replayPacketConstructor = packetClass.getConstructor(packetTypeRegistryClass, packetTypeClass, replayByteBufClass);
            getReplayPacketTypeRegistryMethod = mcVerClass.getMethod("getPacketTypeRegistry", stateClass);
            replayWrappedBufferMethod = replayUnpooledClass.getMethod("wrappedBuffer", byte[].class);
            replayPlayState = Enum.valueOf((Class<? extends Enum>) stateClass.asSubclass(Enum.class), "PLAY");
            replayPluginMessagePacketType = Enum.valueOf((Class<? extends Enum>) packetTypeClass.asSubclass(Enum.class), "PluginMessage");
            return true;
        } catch (final ClassNotFoundException e) {
            replayPacketReflectionUnavailable = true;
            return false;
        } catch (final ReflectiveOperationException e) {
            replayPacketReflectionUnavailable = true;
            logRecordingLookupFailure("Failed to initialize ReplayMod replay packet reflection", e);
            return false;
        }
    }

    private static boolean initializeReplayReflection() {
        if (replayReflectionInitialized) {
            return !replayReflectionUnavailable;
        }

        replayReflectionInitialized = true;

        try {
            final Class<?> replayClass = Class.forName(REPLAY_CLASS_NAME);
            replayInstanceField = replayClass.getField("instance");
            getReplayHandlerMethod = replayClass.getMethod("getReplayHandler");
            isCameraViewMethod = getReplayHandlerMethod.getReturnType().getMethod("isCameraView");
            return true;
        } catch (final ClassNotFoundException e) {
            replayReflectionUnavailable = true;
            return false;
        } catch (final ReflectiveOperationException e) {
            replayReflectionUnavailable = true;
            logReplayLookupFailure("Failed to initialize ReplayMod replay reflection", e);
            return false;
        }
    }

    private static Method getSaveMethod(final Class<?> packetListenerClass, final Object packet) {
        for (final Method method : packetListenerClass.getMethods()) {
            if (!method.getName().equals("save") || method.getParameterCount() != 1) {
                continue;
            }

            if (!method.getParameterTypes()[0].isInstance(packet)) {
                continue;
            }

            return method;
        }

        return null;
    }

    private static byte[] encodeReplayPayload(final CustomPacketPayload payload) throws ReflectiveOperationException {
        final RegistryAccess registryAccess = Minecraft.getInstance().getConnection() != null
                ? Minecraft.getInstance().getConnection().registryAccess()
                : null;
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        try {
            buffer.writeUtf(payload.type().id().toString());
            encodeReplayPayloadBody(payload, buffer);

            final byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }

    private static void encodeReplayPayloadBody(final CustomPacketPayload payload, final RegistryFriendlyByteBuf buffer) throws ReflectiveOperationException {
        final Method payloadWriter = findPayloadWriter(payload.getClass(), buffer.getClass());
        if (payloadWriter != null) {
            payloadWriter.invoke(payload, buffer);
            return;
        }

        final Field codecField = payload.getClass().getField("CODEC");
        final Object codec = codecField.get(null);
        final Method codecEncoder = findCodecEncoder(codec.getClass(), buffer.getClass(), payload.getClass());
        if (codecEncoder == null) {
            throw new ReflectiveOperationException("No compatible codec encoder found for payload " + payload.getClass().getName());
        }

        codecEncoder.invoke(codec, buffer, payload);
    }

    private static @Nullable Method findPayloadWriter(final Class<?> payloadClass, final Class<?> bufferClass) {
        for (final Method method : payloadClass.getDeclaredMethods()) {
            if (method.getParameterCount() != 1) {
                continue;
            }

            if (!method.getName().equals("write") && !method.getName().equals("encode")) {
                continue;
            }

            if (!method.getParameterTypes()[0].isAssignableFrom(bufferClass)) {
                continue;
            }

            method.setAccessible(true);
            return method;
        }

        return null;
    }

    private static @Nullable Method findCodecEncoder(final Class<?> codecClass, final Class<?> bufferClass, final Class<?> payloadClass) {
        for (final Method method : codecClass.getMethods()) {
            if (!method.getName().equals("encode") || method.getParameterCount() != 2) {
                continue;
            }

            if (!method.getParameterTypes()[0].isAssignableFrom(bufferClass)) {
                continue;
            }

            final Class<?> payloadParameterType = method.getParameterTypes()[1];
            if (!payloadParameterType.isAssignableFrom(payloadClass) && payloadParameterType != Object.class) {
                continue;
            }

            return method;
        }

        return null;
    }

    private static void logRecordingLookupFailure(final String message) {
        if (!recordingLookupLogged) {
            recordingLookupLogged = true;
            ReplaySableCompat.LOGGER.warn(message);
        }
    }

    private static void logRecordingLookupFailure(final String message, final ReflectiveOperationException exception) {
        if (!recordingLookupLogged) {
            recordingLookupLogged = true;
            ReplaySableCompat.LOGGER.warn(message, exception);
        }
    }

    private static void logReplayLookupFailure(final String message, final ReflectiveOperationException exception) {
        if (!replayLookupLogged) {
            replayLookupLogged = true;
            ReplaySableCompat.LOGGER.warn(message, exception);
        }
    }
}
