package dev.marggx.surventool.packets;

import com.hypixel.hytale.builtin.buildertools.BuilderToolsPacketHandler;
import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolEntityAction;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.handlers.IPacketHandler;
import com.hypixel.hytale.server.core.io.handlers.IWorldPacketHandler;
import com.hypixel.hytale.server.core.io.handlers.SubPacketHandler;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class PacketHandler implements SubPacketHandler {

    private static final Message MESSAGE_BUILDER_TOOLS_USAGE_DENIED = Message.translation("server.builderTools.usageDenied");
    private final IPacketHandler packetHandler;
    private final BuilderToolsPacketHandler builderToolsPacketHandler;

    public PacketHandler(@Nonnull IPacketHandler packetHandler) {
        this.packetHandler = packetHandler;
        this.builderToolsPacketHandler = new BuilderToolsPacketHandler(packetHandler);
    }

    @Override
    public void registerHandlers() {
        if (BuilderToolsPlugin.get().isDisabled()) {
            return;
        }

        IWorldPacketHandler.registerHandler(this.packetHandler, 401, this::handleBuilderToolEntityAction);
    }

    public void handleBuilderToolEntityAction(
            @Nonnull BuilderToolEntityAction packet,
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull World world,
            @Nonnull Store<EntityStore> store
    ) {
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null || playerComponent.getGameMode() == GameMode.Creative) {
            builderToolsPacketHandler.handleBuilderToolEntityAction(packet, playerRef, ref, world, store);
            return;
        }

        switch (packet.action) {
            case Remove:
                playerRef.sendMessage(Message.translation("surventool.remove.denied"));
                return;
            case Clone:
                playerRef.sendMessage(Message.translation("surventool.clone.denied"));
                return;
        }

        builderToolsPacketHandler.handleBuilderToolEntityAction(packet, playerRef, ref, world, store);
    }
}
