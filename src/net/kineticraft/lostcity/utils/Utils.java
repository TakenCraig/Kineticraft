package net.kineticraft.lostcity.utils;

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kineticraft.lostcity.Core;
import net.kineticraft.lostcity.EnumRank;
import net.kineticraft.lostcity.commands.DiscordSender;
import net.kineticraft.lostcity.data.JsonData;
import net.kineticraft.lostcity.data.lists.JsonList;
import net.kineticraft.lostcity.data.Jsonable;
import net.kineticraft.lostcity.data.wrappers.KCPlayer;
import net.kineticraft.lostcity.discord.DiscordAPI;
import net.kineticraft.lostcity.item.ItemType;
import net.kineticraft.lostcity.item.ItemWrapper;
import net.kineticraft.lostcity.mechanics.metadata.Metadata;
import net.kineticraft.lostcity.mechanics.metadata.MetadataManager;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utils - Contains basic static utilties.
 * Created by Kneesnap on 5/29/2017.
 */
public class Utils {

    /**
     * Gets an enum value from the given class. Returns null if not found.
     * @param value
     * @param clazz
     */
    public static <T extends Enum<T>> T getEnum(String value, Class<T> clazz) {
        return getEnum(value, clazz, null);
    }

    /**
     * Gets an enum value, falling back on a default value.
     * @param value
     * @param defaultValue
     */
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T getEnum(String value, T defaultValue) {
        return getEnum(value, (Class<T>) defaultValue.getClass(), defaultValue);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T getEnum(String value, Class<T> clazz, T defaultValue) {
        try {
            return (T) clazz.getMethod("valueOf", String.class).invoke(null, value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Capitalize every letter after a space.
     * @param sentence
     * @return
     */
    public static String capitalize(String sentence) {
        String[] split = sentence.replaceAll("_", " ").split(" ");
        List<String> out = new ArrayList<>();
        for (String s : split)
            out.add(s.length() > 0 ? s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase() : "");
        return String.join(" ", out);
    }

    /**
     * Joins a string together by the given string.
     * @deprecated Use String#join
     *
     * @param join
     * @param values
     * @param display
     * @return
     */
    public static <T> String join(String join, T[] values, Function<T, String> display) {
        return join(join, Arrays.asList(values), display);
    }

    /**
     * Joins a string together by the delimeter.
     * @deprecrated Use String#join
     *
     * @param join
     * @param values
     * @param displayer
     * @param <T>
     * @return joined
     */
    public static <T> String join(String join, Iterable<T> values, Function<T, String> displayer) {
        String res = "";
        for (T val : values)
            res += (res.length() > 0 ? join : "") + displayer.apply(val);
        return res;
    }

    /**
     * Is the given location 'safe'?
     * @param loc
     * @return safe
     */
    private static boolean isSafe(Location loc) {
        return !isSolid(loc.getBlock()) && !isSolid(loc.clone().add(0, 1, 0).getBlock());
    }

    /**
     * Is this block solid? (Meaning players cannot walk through it)
     * @param bk
     * @return solid
     */
    public static boolean isSolid(Block bk) {
        return isSolid(bk.getType());
    }

    /**
     * Returns whether or not the given material has a collision box.
     * @param mat
     * @return solid
     */
    public static boolean isSolid(Material mat) {
        return mat.isSolid() && mat.isOccluding();
    }

    /**
     * Find the first available 'safe' teleport location.
     *
     *
     * @param origin
     * @return safe
     */
    public static Location findSafe(Location origin) {
        int maxHeight = origin.getWorld().getMaxHeight();
        Location safe = origin.clone();
        safe.setY(maxHeight); // Default location

        Location temp = origin.clone();
        temp.setY(0);
        while(temp.getBlockY() < maxHeight - 1) { // Don't go to the very max height because we check the block above tempY.
            temp.setY(temp.getBlockY() + 1);
            if (isSafe(temp) && origin.distanceSquared(safe) > origin.distanceSquared(temp))
                safe = temp.clone(); // This location is closer than the saved one, and it's safe.
        }
        return safe;
    }

    /**
     * Teleport the player to the specified location with cooldowns applied.
     * @param player
     * @param locationDescription
     * @param location
     */
    public static void teleport(Player player, String locationDescription, Location location) {
        if (MetadataManager.hasMetadata(player, Metadata.TELEPORTING)) {
            player.sendMessage(ChatColor.RED + "Please wait until your current teleport finishes.");
            return;
        }
        KCPlayer p = KCPlayer.getWrapper(player);

        double lastDamage = player.getLastDamage();
        final Location startLocation = player.getLocation().clone();
        final BukkitTask[] tpTask = new BukkitTask[1]; // Have to use an array here to store a value, as otherwise it can't be final.
        // Must be final to work in the bukkit scheduler.
        int[] tpTime = new int[] {p.getRank().isStaff() ? 0 : p.getTemporaryRank().getTpTime()};

        MetadataManager.setMetadata(player, Metadata.TELEPORTING, true);
        player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1F, 1F);
        player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * (tpTime[0] + 4), 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * (tpTime[0] + 4), 2));
        player.sendMessage(ChatColor.BOLD + "Teleport: " + ChatColor.WHITE + ChatColor.UNDERLINE + locationDescription);

        tpTask[0] = Bukkit.getScheduler().runTaskTimer(Core.getInstance(), () -> {
                boolean complete = tpTime[0] <= -1 || !player.isOnline();
                if (complete || player.getLocation().distanceSquared(startLocation) >= 3.5 || player.getLastDamage() < lastDamage) {
                    if (!complete) {
                        player.removePotionEffect(PotionEffectType.CONFUSION);
                        player.removePotionEffect(PotionEffectType.BLINDNESS);
                        player.sendMessage(ChatColor.RED + "Teleport cancelled.");
                        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1F, 2F);
                    }
                    tpTask[0].cancel();
                    MetadataManager.removeMetadata(player, Metadata.TELEPORTING);
                    return;
                }

                if (tpTime[0] > 0) {
                    player.sendMessage(ChatColor.WHITE + "Teleporting... " + ChatColor.UNDERLINE + tpTime[0] + "s");
                } else {
                    player.setNoDamageTicks(100);
                    final Location safe = findSafe(location.clone());
                    if (player.getWorld() != safe.getWorld()) // If teleporting cross-dimensionally we'll need to teleport them again.
                        Bukkit.getScheduler().runTask(Core.getInstance(), () -> player.teleport(safe));
                    player.teleport(safe);
                    player.playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, 1F, 1.333F);
                }

                tpTime[0]--;
        }, 0L, 20L);
    }

    /**
     * Turn milliseconds into a user friendly string.
     * @param time
     * @return formatted
     */
    public static String formatTime(long time) {
        if (time == -1)
            return "never";

        time /= 1000;
        String formatted = "";
        for (int i = 0; i < TimeInterval.values().length; i++) {
            TimeInterval iv = TimeInterval.values()[TimeInterval.values().length - i - 1];
            if (time >= iv.getInterval()) {
                int temp = (int) (time - (time % iv.getInterval()));
                int add = temp / iv.getInterval();
                formatted += " " + add + iv.getSuffix() + (add > 1 && iv != TimeInterval.SECOND ? "s" : "");
                time -= temp;
            }
        }
        return formatted.length() > 0 ? formatted.substring(1) : "now";
    }

    /**
     * Formats milliseconds into a user friendly display.
     * Different from formatTime because this does not use abbreviations.
     *
     * @param time
     * @return formatted
     */
    public static String formatTimeFull(long time) {
        if (time == -1)
            return "Never";

        time /= 1000;
        String formatted = "";

        for (int i = 0; i < TimeInterval.values().length; i++) {
            TimeInterval iv = TimeInterval.values()[TimeInterval.values().length - i - 1];
            if (time >= iv.getInterval()) {
                int temp = (int) (time - (time % iv.getInterval()));
                int add = temp / iv.getInterval();
                formatted += " " + add + " " + capitalize(iv.name()) + (add > 1 ? "s" : "");
                time -= temp;
            }
        }

        return formatted.length() > 0 ? formatted.substring(1) : "Now";
    }

    @AllArgsConstructor @Getter
    private enum TimeInterval {
        SECOND("s", 1),
        MINUTE("min", 60 * SECOND.getInterval()),
        HOUR("hr", 60 * MINUTE.getInterval()),
        DAY("day", 24 * HOUR.getInterval()),
        MONTH("month", 30 * DAY.getInterval()),
        YEAR("yr", 365 * DAY.getInterval());

        private String suffix;
        private int interval;
    }

    /**
     * Convert a location into a friendly string.
     * @param location
     * @return
     */
    public static String toString(Location location) {
       return "[" + location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ() + "]";
    }

    /**
     * Give an item to a player. If their inventory is full, it drops it at their feet.
     * @param player
     * @param itemStack
     */
    public static void giveItem(Player player, ItemStack itemStack) {
        if (player.getInventory().firstEmpty() > -1) {
            player.getInventory().addItem(itemStack);
        } else {
            player.getWorld().dropItem(player.getLocation(), itemStack);
            player.sendMessage(ChatColor.RED + "Your inventory was full, so you dropped the item.");
        }
    }

    /**
     * Construct an object from JSON.
     * @param type
     * @param object
     * @return Constructed object
     */
    public static <T extends Jsonable> T fromJson(Class<T> type, JsonObject object) {
        return ReflectionUtil.construct(type, new JsonData(object));
    }

    /**
     * Gets a players username by their uuid. Offline safe.
     * Returns null if this player has never joined.
     * @param uuid
     * @return name
     */
    public static String getPlayerName(UUID uuid) {
        return KCPlayer.isWrapper(uuid) ? KCPlayer.getWrapper(uuid).getUsername() : null;
    }

    /**
     * Return the display name of an ItemStack.
     * @param itemStack
     * @return Display Name
     */
    public static String getItemName(ItemStack itemStack) {
        return itemStack.getItemMeta().hasDisplayName() ? itemStack.getItemMeta().getDisplayName()
                : capitalize(itemStack.getType().name().replaceAll("_", " "));
    }

    /**
     * Get a random number between the given range.
     * @param min
     * @param max
     * @return int
     */
    public static int randInt(int min, int max) {
        return max + min > 0 ? nextInt(max - min) + min : 0;
    }

    /**
     * Get a random element from an array.
     * @param arr
     * @param <T>
     * @return element
     */
    public static <T> T randElement(T[] arr) {
        return randElement(Arrays.asList(arr));
    }

    /**
     * Get a random element from a json list
     * @param list
     * @param <T>
     * @return rand
     */
    public static <T extends Jsonable> T randElement(JsonList<T> list) {
        return randElement(list.getValues());
    }

    /**
     * Get a random element from a list.
     * @param iterable
     * @param <T>
     * @return element
     */
    public static <T> T randElement(Iterable<T> iterable) {
        List<T> list = new ArrayList<>();
        iterable.forEach(list::add);
        return list.isEmpty() ? null : list.get(nextInt(list.size()));
    }

    /**
     * Get a random true/false value.
     * @return randomBool
     */
    public static boolean nextBool() {
        return randChance(2);
    }

    /**
     * Gets a random number between 0 and max - 1
     * @param max
     * @return rand
     */
    public static int nextInt(int max) {
        return ThreadLocalRandom.current().nextInt(max);
    }

    /**
     * Check if a random chance succeeds.
     * @param chance
     * @return success
     */
    public static boolean randChance(int chance) {
        return nextInt(chance) == 0;
    }

    /**
     * Replaces an existing itemstack in a player's inventory with a new one.
     * @param player
     * @param original
     * @param newItem
     */
    public static void replaceItem(Player player, ItemStack original, ItemStack newItem) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (original.equals(player.getInventory().getItem(i))) {
                player.getInventory().setItem(i, newItem);
                player.updateInventory();
                return;
            }
        }

        Core.warn("Failed to replace " + Utils.getItemName(original) + " in " + player.getName() + "'s inventory.");
    }

    /**
     * Shave the first element of any array.
     * @param array
     * @param <T>
     * @return shavedArray
     */
    public static <T> T[] shift(T[] array) {
        return shift(array, 1);
    }

    /**
     * Shave a given number of elements from an array.
     * @param array
     * @param shave
     * @param <T>
     * @return shavedArray
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] shift(T[] array, int shave) {
        List<T> l = new ArrayList<>(Arrays.asList(array));
        for (int i = 0; i < shave; i++)
            l.remove(0);
        return l.toArray((T[]) Array.newInstance(array[0].getClass(), array.length - shave));
    }

    /**
     * Get the colored name of this CommandSender
     * @param sender
     * @return name
     */
    public static String getSenderName(CommandSender sender) {
        return hasWrapper(sender) ? KCPlayer.getWrapper(sender).getColoredName() : ChatColor.YELLOW + sender.getName();
    }

    /**
     * Use an item and decrement its amount.
     * @param item
     * @return item
     */
    public static ItemStack useItem(ItemStack item) {
        item.setAmount(item.getAmount() - 1);
        if (item.getAmount() <= 0)
            item.setType(Material.AIR);
        return item;
    }

    /**
     * Formats a boolean toggle to be displayed in chat.
     * @param name
     * @param value
     * @return string
     */
    public static String formatToggle(String name, boolean value) {
        return ChatColor.GRAY + name + ": " + (value ? ChatColor.GREEN : ChatColor.RED) + value;
    }

    /**
     * Can this CommandSender type have a wrapper?
     * @param sender
     * @return canHave
     */
    public static boolean hasWrapper(CommandSender sender) {
        return sender instanceof Player || sender instanceof DiscordSender;
    }

    /**
     * Gets the rank of a CommandSender
     * @param sender
     * @return rank
     */
    public static EnumRank getRank(CommandSender sender) {
        return hasWrapper(sender) ? KCPlayer.getWrapper(sender).getRank()
                : (sender instanceof ConsoleCommandSender ? EnumRank.DEV : EnumRank.MU);
    }

    /**
     * Does the first array contain any elements from the second array?
     * @param a
     * @param b
     * @param <T>
     * @return contains
     */
    public static <T> boolean containsAny(T[] a, T[] b) {
        return containsAny(Arrays.asList(a), Arrays.asList(b));
    }

    /**
     * Does the first list contain any elements from the second array?
     * @param a
     * @param b
     * @param <T>
     * @return
     */
    public static <T> boolean containsAny(List<T> a, List<T> b) {
        return b.stream().anyMatch(a::contains);
    }

    /**
     * Remove a potion with infinite duration.
     * @param player
     * @param type
     */
    public static void removeInfinitePotion(Player player, PotionEffectType type) {
        PotionEffect pe = player.getPotionEffect(type);
        if (pe != null && pe.getDuration() >= 30 * 60 * 20)
            player.removePotionEffect(type);
    }

    /**
     * Give a player an infinite potion effect, but only if they do not already have a potion of this type.
     * @param player
     * @param type
     */
    public static void giveInfinitePotion(Player player, PotionEffectType type) {
        if (!player.hasPotionEffect(type))
            player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, 2));
    }

    /**
     * Gives or removes an infinite potion based on the 'has' parameter.
     * Respects player brewed potions, if they exist.
     *
     * @param player
     * @param type
     * @param has
     */
    public static void setPotion(Player player, PotionEffectType type, boolean has) {
        if (has) {
            giveInfinitePotion(player, type);
        } else {
            removeInfinitePotion(player, type);
        }
    }

    /**
     * Gives or removes an infinite potion based on if they do or do not already have this potion.
     * WARNING: Does not respect player-brewed potions.
     *
     * @param player
     * @param type
     * @return hasPotion (After this is called)
     */
    public static boolean togglePotion(Player player, PotionEffectType type) {
        boolean newState = !player.hasPotionEffect(type);
        setPotion(player, type, newState);
        return newState;
    }

    /**
     * Broadcast a message everywhere except to a specified player.
     * @param message
     * @param except
     */
    public static void broadcastExcept(String message, Player except) {
        getAllPlayersExcept(except).forEach(p -> p.sendMessage(message));
        Bukkit.getConsoleSender().sendMessage(message);
        DiscordAPI.sendGame(message);
    }

    /**
     * Returns a list of all players except the given player.
     * @param player
     * @return allExcept
     */
    public static List<Player> getAllPlayersExcept(Player player) {
        return Bukkit.getOnlinePlayers().stream().filter(p -> p != player).collect(Collectors.toList());
    }

    /**
     * Gets the supplied input from a resulting NFE.
     * @param nfe
     * @return input
     */
    public static String getInput(NumberFormatException nfe) {
        return nfe.getLocalizedMessage().split(": ")[1].replaceAll("\"", "");
    }

    /**
     * Find the amount of times a
     * @param search
     * @param find
     * @return
     */
    public static int getCount(String search, String find) {
        int found = 0;
        for (int i = 0; i < search.length() - find.length() + 1; i++)
            if (search.substring(i, i + find.length()).equals(find))
                found++;
        return found;
    }

    /**
     * Gives the player an item if they don't already have an item of that type.
     * Replaces it otherwise.
     *
     * @param player
     * @param item
     */
    public static void replaceItem(Player player, ItemWrapper item) {
        ItemType check = item.getType();
        for (int i = 0; i < player.getInventory().getSize(); i++ ) {
            if (check != ItemWrapper.getType(player.getInventory().getItem(i)))
                continue;
            player.getInventory().setItem(i, item.generateItem());
            return;
        }

        giveItem(player, item.generateItem());
    }

    /**
     * Get players nearby an entity. Async-Safe.
     *
     * @param entity
     * @param radius
     * @return players
     */
    public static List<Player> getNearbyPlayers(Entity entity, int radius) {
        return getNearbyPlayers(entity.getLocation(), radius);
    }

    /**
     * Get players nearby a location. Async-Safe.
     *
     * @param loc
     * @param radius
     * @return players
     */
    public static List<Player> getNearbyPlayers(Location loc, int radius) {
        return getNearbyEntities(loc, radius).stream().filter(e -> e instanceof Player).map(e -> (Player) e)
                .collect(Collectors.toList());
    }

    /**
     * Get entities nearby a location. Async-Safe
     * TODO: Make this actually async-safe.
     *
     * @param loc
     * @param radius
     * @return entities
     */
    public static Collection<Entity> getNearbyEntities(Location loc, int radius) {
        return loc.getWorld().getNearbyEntities(loc, radius, radius, radius);
    }

    /**
     * Check if a given location is within the given coordinates.
     * Does not require the Y parameter.
     *
     * @param location
     * @param x1
     * @param z1
     * @param x2
     * @param z2
     * @return inArea
     */
    public static boolean inArea(Location location, int x1, int z1, int x2, int z2) {
        return inArea(location, x1, 0, z1, x2, 256, z2);
    }

    /**
     * Is the given location between the given box coordinates?
     *
     * @param l
     * @param x1
     * @param y1
     * @param z1
     * @param x2
     * @param y2
     * @param z2
     * @return inArea
     */
    public static boolean inArea(Location l, int x1, int y1, int z1, int x2, int y2, int z2) {
        return Math.min(x2, Math.max(x1, l.getX())) == l.getX()
                && Math.min(y2, Math.max(y1, l.getY())) == l.getY()
                && Math.min(z2, Math.max(z1, l.getZ())) == l.getZ();
    }

    /**
     * Is the provided location within spawn?
     * @param location
     * @return spawn
     */
    public static boolean inSpawn(Location location) {
        return inArea(location, -200, -200, 200, 306);
    }

    /**
     * Is this input an integer?
     * @param input
     * @return integer
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean isInteger(String input) {
        try {
            Integer.parseInt(input);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Is the given KCPlayer not null and visible to the sender?
     * If the player is not visible, it will tell the player so.
     *
     * @param sender
     * @param player
     * @return
     */
    public static boolean isVisible(CommandSender sender, KCPlayer player) {
        return isVisible(sender, player != null ? player.getPlayer() : null);
    }

    /**
     * Is the given username online and visible to the supplied sender?
     * If the player is not visible, it tells the player so.
     *
     * @param sender
     * @param username
     * @return visible
     */
    public static boolean isVisible(CommandSender sender, String username) {
        return isVisible(sender, Bukkit.getPlayer(username));
    }

    /**
     * Checks if the given CommandSender is online and visible to the supplied sender.
     * If not visible, it will tell the player so.
     *
     * @param sender
     * @param receiver
     * @return
     */
    public static boolean isVisible(CommandSender sender, CommandSender receiver) {
        boolean shown = receiver != null && (!(receiver instanceof Player) || !KCPlayer.getWrapper(receiver).isVanished(sender));
        if (!shown)
            sender.sendMessage(ChatColor.RED + "Player not found.");
        return shown;
    }

    /**
     * Read all lines from a plugin resource.
     * @param resource
     * @return lines
     */
    public static List<String> readLines(String resource) {
        return new BufferedReader(new InputStreamReader(Core.loadResource(resource), StandardCharsets.UTF_8))
                .lines().collect(Collectors.toList());
    }

    /**
     * Get the size of the data in a plugin resource.
     * @param resource
     * @return size
     */
    public static int readSize(String resource) {
        return readLines(resource).stream().mapToInt(String::length).sum();
    }

    /**
     * Remove duplicate entries from a list.
     * @param list
     */
    public static <T> void removeDuplicates(List<T> list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.lastIndexOf(list.get(i)) != i) {
                list.remove(i);
                i--;
            }
        }
    }

    /**
     * Append an element to create a new array.
     * @param arr
     * @param <T>
     * @return newArray
     */
    public static <T> T[] append(T[] arr, T value) {
        List<T> add = new ArrayList<>(Arrays.asList(arr));
        add.add(value);
        return add.toArray(arr);
    }
}