package net.kineticraft.lostcity.commands.player;

import net.kineticraft.lostcity.commands.PlayerCommand;
import net.kineticraft.lostcity.data.QueryTools;
import net.kineticraft.lostcity.data.KCPlayer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Show top voters.
 *
 * Created by Kneesnap on 6/10/2017.
 */
public class CommandVotes extends PlayerCommand {

    private static final int ENTRIES = 10;

    public CommandVotes() {
        super("", "Shows how many times you have voted.", "votes");
    }

    @Override
    protected void onCommand(CommandSender sender, String[] args) {
        if (QueryTools.isBusy(sender))
            return;

        QueryTools.queryData(stream -> {
            List<KCPlayer> list = stream.sorted(Comparator.comparing(KCPlayer::getMonthlyVotes).reversed())
                    .collect(Collectors.toList());
            KCPlayer p = KCPlayer.getWrapper((Player) sender);
            String bar = ChatColor.GRAY.toString() + ChatColor.STRIKETHROUGH + "----------";
            int show = Math.min(ENTRIES, list.size());

            sender.sendMessage(bar + ChatColor.AQUA + "Top " + show + " Monthly Voters" + bar);
            for (int i = 0; i < show; i++) {
                KCPlayer player = list.get(i);
                sender.sendMessage(ChatColor.YELLOW.toString() + (i + 1) + ") "
                        + (p.getUsername().equals(player.getUsername()) ? ChatColor.GREEN : ChatColor.AQUA)
                        + player.getUsername() + ChatColor.GRAY.toString() + ": " + player.getMonthlyVotes());
            }

            sender.sendMessage(ChatColor.GRAY + "Monthly Votes: " + ChatColor.AQUA + p.getMonthlyVotes());
            sender.sendMessage(ChatColor.GRAY + "Total Votes: " + ChatColor.AQUA + p.getTotalVotes());
        });
    }
}
