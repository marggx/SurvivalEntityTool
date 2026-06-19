package dev.marggx.surventool.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.pages.EntitySpawnPage;
import dev.marggx.surventool.ui.SpawnEntityPage;

public class CustomSpawnPageCommand extends AbstractPlayerCommand {
    public CustomSpawnPageCommand() {
        super("server.commands.npc.spawn.page.desc");
    }

    @Override
    protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world) {
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());

        if (playerComponent.getGameMode() != GameMode.Creative) {
            playerComponent.getPageManager().openCustomPage(ref, store, new SpawnEntityPage(playerRefComponent));
        } else {
            playerComponent.getPageManager().openCustomPage(ref, store, new EntitySpawnPage(playerRefComponent));
        }
    }
}
