package net.kineticraft.lostcity.commands.discord;

import net.kineticraft.lostcity.commands.Command;
import net.kineticraft.lostcity.commands.CommandType;
import net.kineticraft.lostcity.commands.DiscordSender;
import org.bukkit.command.CommandSender;

/**
 * A command that can be executed in discord.
 *
 * Created by Kneesnap on 6/28/2017.
 */
public abstract class DiscordCommand extends Command {

    public DiscordCommand(String usage, String help, String... alias) {
        super(CommandType.DISCORD, usage, help, alias);
    }

    @Override
    protected void onCommand(CommandSender sender, String[] args) {
        onCommand((DiscordSender) sender, args);
    }

    @Override
    public boolean canUse(CommandSender sender, boolean showMessage) {
        return sender instanceof DiscordSender;
    }

    protected abstract void onCommand(DiscordSender sender,  String[] args);
}
