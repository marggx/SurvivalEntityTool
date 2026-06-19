package dev.marggx.surventool;

import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.marggx.surventool.commands.CustomSpawnPageCommand;
import dev.marggx.surventool.ui.supplier.SpawnEntityPageSupplier;
import dev.marggx.surventool.utils.Logger;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;

public class SurvivalEntityToolPlugin extends JavaPlugin {

    private static SurvivalEntityToolPlugin INSTANCE;

    public SurvivalEntityToolPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    public static SurvivalEntityToolPlugin get() { return INSTANCE; }

    @Override
    protected void setup() {

        INSTANCE = this;
        Logger.get().info("Plugin " + this.getName() + " with version " + this.getManifest().getVersion().toString() + " is starting");
        OpenCustomUIInteraction.registerCustomPageSupplier(this, SpawnEntityPageSupplier.class, "SurventoolSpawnEntity", new SpawnEntityPageSupplier());
        overrideSpawnPageCommand();

    }

    private void overrideSpawnPageCommand() {
        try {
            CommandManager cm = CommandManager.get();
            AbstractCommand npcCmd = cm.getCommandRegistration().get("npc");
            if (npcCmd == null) {
                Logger.get().warning("Could not find NPC command to override");
                return;
            }

            AbstractCommand spawnCmd = npcCmd.getSubCommands().get("spawn");
            if (spawnCmd == null) {
                Logger.get().warning("Could not find NPC spawn command to override");
                return;
            }

            Field variantsField = AbstractCommand.class.getDeclaredField("variantCommands");
            variantsField.setAccessible(true);
            Int2ObjectMap<AbstractCommand> variants = (Int2ObjectMap<AbstractCommand>) variantsField.get(spawnCmd);

            CustomSpawnPageCommand custom = new CustomSpawnPageCommand();
            custom.setOwner(cm);
            custom.completeRegistration();
            variants.put(0, custom);

            Logger.get().info("Successfully overridden SpawnPageCommand with custom implementation");
        } catch (Exception e) {
            Logger.get().warning("Failed to override SpawnPageCommand: " + e.getMessage());
        }
    }

    @Override
    protected void start() {
    }
}