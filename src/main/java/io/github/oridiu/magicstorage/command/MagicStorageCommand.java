package io.github.oridiu.magicstorage.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import io.github.oridiu.magicstorage.MagicStorageConfig;
import io.github.oridiu.magicstorage.MagicStoragePlugin;

import javax.annotation.Nonnull;

/**
 * {@code /magicstorage} (or {@code /ms}): shows and changes the mod's settings
 * in game. Like every plugin command it needs the plugin's command
 * permission, which operators have.
 */
public class MagicStorageCommand extends AbstractCommandCollection {

    public MagicStorageCommand() {
        super("magicstorage", "server.magicstorage.commands.desc");
        addAliases("ms");
        addSubCommand(new SettingsCommand());
        addSubCommand(new ShowCommand());
        addSubCommand(new SetCommand());
        addSubCommand(new ReloadCommand());
    }

    static void sendSettings(@Nonnull CommandContext context) {
        MagicStorageConfig config = MagicStoragePlugin.getInstance().getSettings();
        context.sendMessage(Message.translation("server.magicstorage.settings.header").color("#b48cff"));
        for (Setting setting : Setting.values()) {
            context.sendMessage(Message.translation("server.magicstorage.settings.line")
                    .param("name", setting.key)
                    .param("short", setting.shortName)
                    .param("value", setting.get(config)));
        }
    }

    /** Opens the settings window. */
    static class SettingsCommand extends AbstractPlayerCommand {

        SettingsCommand() {
            super("settings", "server.magicstorage.commands.settings.desc");
            addAliases("ui");
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef, @Nonnull World world
        ) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                player.getPageManager().openCustomPage(ref, store, new SettingsPage(playerRef));
            }
        }
    }

    static class ShowCommand extends CommandBase {

        ShowCommand() {
            super("show", "server.magicstorage.commands.show.desc");
            addAliases("list");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            sendSettings(context);
        }
    }

    static class SetCommand extends CommandBase {

        private final RequiredArg<String> settingArg =
                withRequiredArg("setting", "server.magicstorage.commands.set.setting.desc", ArgTypes.STRING);
        private final RequiredArg<String> valueArg =
                withRequiredArg("value", "server.magicstorage.commands.set.value.desc", ArgTypes.STRING);

        SetCommand() {
            super("set", "server.magicstorage.commands.set.desc");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            String name = settingArg.get(context);
            String input = valueArg.get(context);
            Setting setting = Setting.byName(name);
            if (setting == null) {
                context.sendMessage(Message.translation("server.magicstorage.settings.unknown")
                        .param("name", name).param("names", Setting.names()).color("#ff5555"));
                return;
            }
            Integer value = setting.parse(input);
            if (value == null) {
                context.sendMessage(Message.translation("server.magicstorage.settings.invalid")
                        .param("name", setting.key).param("value", input).color("#ff5555"));
                return;
            }
            MagicStoragePlugin plugin = MagicStoragePlugin.getInstance();
            plugin.updateSettings(config -> setting.set(config, value));
            context.sendMessage(Message.translation("server.magicstorage.settings.changed")
                    .param("name", setting.key).param("value", setting.get(plugin.getSettings())).color("#b48cff"));
        }
    }

    static class ReloadCommand extends CommandBase {

        ReloadCommand() {
            super("reload", "server.magicstorage.commands.reload.desc");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            MagicStoragePlugin.getInstance().reloadSettings().whenComplete((config, error) -> {
                if (error != null) {
                    context.sendMessage(Message.translation("server.magicstorage.settings.reloadFailed").color("#ff5555"));
                    return;
                }
                context.sendMessage(Message.translation("server.magicstorage.settings.reloaded").color("#b48cff"));
                sendSettings(context);
            });
        }
    }
}
