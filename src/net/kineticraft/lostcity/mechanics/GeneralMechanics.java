package net.kineticraft.lostcity.mechanics;

import net.kineticraft.lostcity.Core;
import net.kineticraft.lostcity.config.Configs;
import net.kineticraft.lostcity.data.wrappers.KCPlayer;
import net.kineticraft.lostcity.utils.TextUtils;
import net.kineticraft.lostcity.utils.Utils;
import net.minecraft.server.v1_12_R1.EntityEnderDragon;
import org.bukkit.*;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

/**
 * GeneralMechanics - Small general mechanics.
 *
 * Created by Kneesnap on 5/29/2017.
 */
public class GeneralMechanics extends Mechanic {

    @Override
    public void onEnable() {

        // Register announcer.
        Bukkit.getScheduler().runTaskTimer(Core.getInstance(), () -> {
            String s = Utils.randElement(Configs.getRawConfig(Configs.ConfigType.ANNOUNCER).getLines());
            if (s != null)
                Bukkit.broadcast(TextUtils.fromMojangson(s));
        }, 0L, 5 * 20 * 60L);

        // Increment time played.
        Bukkit.getScheduler().runTaskTimerAsynchronously(Core.getInstance(), () ->
            Bukkit.getOnlinePlayers().stream().map(KCPlayer::getWrapper)
                    .forEach(p -> p.setSecondsPlayed(p.getSecondsPlayed() + 1)), 0L, 20L);

        // Display donor particles.
        Bukkit.getScheduler().runTaskTimerAsynchronously(Core.getInstance(), () -> {
            for (Player p : Core.getOnlinePlayers()) {
                KCPlayer w = KCPlayer.getWrapper(p);
                if (w.getEffect() != null)
                    p.getWorld().spawnParticle(w.getEffect(), p.getLocation(), 10);
            }
        }, 0L, 20L);
    }

    @Override
    public void onJoin(Player player) {
        String gray = ChatColor.GRAY.toString() + ChatColor.ITALIC;
        player.sendMessage(TextUtils.centerChat(ChatColor.BOLD + "Kineticraft v4"));
        player.sendMessage(TextUtils.centerChat(ChatColor.AQUA.toString() + ChatColor.BOLD + "The Lost City"));
        player.sendMessage(TextUtils.centerChat(gray + "http://kineticraft.net/"));
        player.sendMessage("");
        player.sendMessage(gray + "Type " + ChatColor.YELLOW.toString() + ChatColor.ITALIC
                + "/info" + gray + " for help getting started.");
        player.sendMessage("");


        // 50 tick difference.
        Bukkit.getScheduler().runTaskLater(Core.getInstance(), () ->
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, .6929134F), 45L);

        Bukkit.getScheduler().runTaskLater(Core.getInstance(), () ->
                player.playSound(player.getLocation(), Sound.ENTITY_HORSE_ARMOR, .85F, 1.480315F), 95L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent evt) {
        if (evt.getPlayer().hasPlayedBefore())
            return;
        Bukkit.getScheduler().runTask(Core.getInstance(), () -> evt.getPlayer().teleport(Core.getMainWorld().getSpawnLocation()));
        Utils.getAllPlayersExcept(evt.getPlayer()).forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 1, 1.1F));
        Bukkit.broadcastMessage(ChatColor.GRAY + "Welcome " + ChatColor.GREEN + evt.getPlayer().getName()
                + ChatColor.GRAY + " to " + ChatColor.BOLD + "Kineticraft" + ChatColor.GRAY + "!");
    }

    @EventHandler(ignoreCancelled = true)
    public void onEggPunch(PlayerInteractEvent evt) {
        Block block = evt.getClickedBlock();
        if ((evt.getAction() != Action.LEFT_CLICK_BLOCK && evt.getAction() != Action.RIGHT_CLICK_BLOCK)
                || block == null || block.getType() != Material.DRAGON_EGG)
            return;

        evt.getPlayer().sendMessage(ChatColor.DARK_PURPLE + "You have picked up the egg.");
        block.setType(Material.AIR);
        Utils.giveItem(evt.getPlayer(), new ItemStack(Material.DRAGON_EGG));
        evt.setCancelled(true);
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent evt) {
        if (!(evt.getEntity() instanceof EntityEnderDragon))
            return; // If it's not a dragon, ignore it.
        evt.setDroppedExp(4000);
        Block bk = evt.getEntity().getWorld().getBlockAt(0, 63, 0);

        Bukkit.getScheduler().runTaskLater(Core.getInstance(), () -> {
            bk.setType(Material.DRAGON_EGG);
            bk.getWorld().getNearbyEntities(bk.getLocation(), 50, 50, 50)
                    .forEach(e -> e.sendMessage(ChatColor.GRAY + "As the dragon dies, an egg forms below."));
        }, 15 * 20);
    }

    @EventHandler // Water-vision.
    public void onWaterTraverse(PlayerMoveEvent evt) {
        Utils.setPotion(evt.getPlayer(), PotionEffectType.WATER_BREATHING,
                evt.getPlayer().getVehicle() == null && evt.getTo().getBlock().isLiquid());
    }
}