package com.vjhkfuct.replaysablecompat.compat;

import com.vjhkfuct.replaysablecompat.ReplaySableCompat;
import dev.ryanhcode.sable.mixinterface.udp.ConnectionExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public final class ReplayRecordingBridge {
    private static final String RECORDING_CLASS_NAME = "com.replaymod.recording.ReplayModRecording";
    private static final String REPLAY_CLASS_NAME = "com.replaymod.replay.ReplayModReplay";

    private static boolean recordingReflectionInitialized;
    private static boolean recordingReflectionUnavailable;
    private static Field recordingInstanceField;
    private static Method getConnectionEventHandlerMethod;
    private static Method getPacketListenerMethod;
    private static Class<?> cachedPacketListenerClass;
    private static Method cachedSaveMethod;

    private static boolean replayReflectionInitialized;
    private static boolean replayReflectionUnavailable;
    private static Field replayInstanceField;
    private static Method getReplayHandlerMethod;

    private static boolean recordingLookupLogged;
    private static boolean replayLookupLogged;

    private ReplayRecordingBridge() {
    }

    public static void mirrorClientboundPayload(final CustomPacketPayload payload) {
        final Object packetListener = getPacketListener();
        if (packetListener == null) {
            return;
        }

        final ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(payload);
        final Method saveMethod = getSaveMethod(packetListener.getClass(), packet);
        if (saveMethod == null) {
            logRecordingLookupFailure("ReplayMod PacketListener has no compatible save(Packet) overload");
            return;
        }

        try {
            saveMethod.invoke(packetListener, packet);
        } catch (final ReflectiveOperationException e) {
            logRecordingLookupFailure("Failed to mirror Sable payload into ReplayMod recording", e);
        }
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

    private static boolean initializeReplayReflection() {
        if (replayReflectionInitialized) {
            return !replayReflectionUnavailable;
        }

        replayReflectionInitialized = true;

        try {
            final Class<?> replayClass = Class.forName(REPLAY_CLASS_NAME);
            replayInstanceField = replayClass.getField("instance");
            getReplayHandlerMethod = replayClass.getMethod("getReplayHandler");
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
        if (cachedSaveMethod != null && cachedPacketListenerClass == packetListenerClass) {
            return cachedSaveMethod;
        }

        for (final Method method : packetListenerClass.getMethods()) {
            if (!method.getName().equals("save") || method.getParameterCount() != 1) {
                continue;
            }

            if (!method.getParameterTypes()[0].isInstance(packet)) {
                continue;
            }

            cachedPacketListenerClass = packetListenerClass;
            cachedSaveMethod = method;
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
