package de.diepixelecke.mphc;

import com.onarandombox.MultiverseCore.MultiverseCore;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MultiplayerHardcore extends JavaPlugin {
    private BukkitTask pruner = null;
    private BukkitTask watchdog;
    DiscordMessenger messenger;
    MultiverseCore mv;
    PlaytimeControl playtime;
    boolean isBusy = false;
    boolean isBroken = false;

    @Override
    public void onEnable() {
        mv = (MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");
        saveDefaultConfig();
        if (!mv.getMVWorldManager().isMVWorld(getCurrentAttempt())) {
            getLogger().log(Level.INFO, "Creating worlds...");
            createWorlds(getCurrentAttempt());
        }
        playtime = PlaytimeControl.fromConfig(this);
        EventListener listener = new EventListener(this);
        getServer().getPluginManager().registerEvents(listener, this);
        messenger = new DiscordMessenger(this);
        watchdog = getServer().getScheduler().runTaskTimer(this, new PlaytimeWatchdog(this), 10*60 * 20, 10*60 * 20);
        int attemptsToKeep;
        if ((attemptsToKeep = getConfig().getInt("attempt.attemptsToKeep")) >= 0) {
            pruner = getServer().getScheduler().runTaskTimer(this, new AttemptPruner(this, getConfig().getString("attempt.worldPrefix"), attemptsToKeep), 30 * 20, 30*60 * 20);
        }
    }

    @Override
    public void onDisable() {
        watchdog.cancel();
        if (pruner != null) {
            pruner.cancel();
        }
    }

    String getDeathMessage(EntityDamageEvent.DamageCause cause, String source, String playerName) {
        List<String> msgs;
        Random random = new Random();
        if (getConfig().contains("messages." + cause.name()))
            msgs = getConfig().getStringList("messages." + cause.name());
        else
            msgs = getConfig().getStringList("messages.generic");
        String theMessage = msgs.get(random.nextInt(msgs.size()));
        return String.format(theMessage + "\n%3$s", playerName, source, getConfig().getString("messages.ending"));
    }

    String getCurrentAttempt() {
        FileConfiguration conf = getConfig();
        return conf.getString("attempt.worldPrefix") + conf.getInt("attempt.current");
    }

    String logDeathAndGetNextAttempt(EntityDamageEvent.DamageCause cause, Player dying, Player killer) {
        FileConfiguration conf = getConfig();
        String statRef;
        if (killer != null)
            statRef = "stats." + dying.getName() + ".byPlayer." + killer.getUniqueId() + "." + cause.name();
        else
            statRef = "stats." + dying.getUniqueId() + "." + cause.name();

        conf.set("attempt.current", conf.getInt("attempt.current")+1);
        conf.set(statRef, conf.getInt(statRef, 0)+1);
        conf.set("attempt.participatedInCurrent", new ArrayList<String>());
        saveConfig();
        return getCurrentAttempt();
    }

    void createWorldAndMoveEveryone(String newWorldName, String message) {
        isBusy = true;
        getServer().getScheduler().runTaskLater(this, () -> {
            getLogger().log(Level.INFO, "Creating worlds for next attempt: " + newWorldName);
            boolean success = createWorlds(newWorldName);
            if (!success) {
                isBroken = true;
                for (Player p: getServer().getOnlinePlayers()) {
                    p.kickPlayer(message + "\nUnable to generate world.");
                }
                return;
            }
            CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>(getServer().getOnlinePlayers());
            for (Player p: players) {
                restartPlayerInWorld(p, newWorldName, message);
            }
            getServer().getScheduler().runTaskLater(this, () -> {
                getLogger().log(Level.INFO, "Saving new attempt...");
                getConfig().set("attempt.participatedInCurrent", getServer().getOnlinePlayers().stream().map(e -> e.getUniqueId().toString()).collect(Collectors.toList()));
                saveConfig();
                isBusy = false;
            }, 5*20);
        }, 1);
    }

    private boolean createWorlds(String newWorldName) {
        boolean success = mv.getMVWorldManager().addWorld(newWorldName, World.Environment.NORMAL, null, WorldType.NORMAL, true, null);
        boolean success_nether = mv.getMVWorldManager().addWorld(newWorldName + "_nether", World.Environment.NETHER, null, WorldType.NORMAL, true, null);
        boolean success_end = mv.getMVWorldManager().addWorld(newWorldName + "_the_end", World.Environment.THE_END, null, WorldType.NORMAL, true, null);
        if (success && success_nether && success_end) {
            getServer().dispatchCommand(getServer().getConsoleSender(), String.format("mv modify set difficulty hard %s", newWorldName));
            getServer().dispatchCommand(getServer().getConsoleSender(), String.format("mv modify set difficulty hard %s_nether", newWorldName));
            getServer().dispatchCommand(getServer().getConsoleSender(), String.format("mv modify set difficulty hard %s_the_end", newWorldName));
            getServer().dispatchCommand(getServer().getConsoleSender(), String.format("mvnp link nether %1$s %1$s_nether", newWorldName));
            getServer().dispatchCommand(getServer().getConsoleSender(), String.format("mvnp link end %1$s %1$s_the_end", newWorldName));
            return true;
        } else return false;
    }

    boolean hasPlayerParticipatedInAttempt(Player player) {
        return getConfig().getStringList("attempt.participatedInCurrent").contains(player.getUniqueId().toString());
    }

    void addPlayerToCurrentAttempt(Player player) {
        List<String> current = getConfig().getStringList("attempt.participatedInCurrent");
        current.add(player.getUniqueId().toString());
        getConfig().set("attempt.participatedInCurrent", current);
        saveConfig();
    }

    void restartPlayerInWorld(Player player, String newWorld, String message) {
        getLogger().log(Level.INFO, "Preparing to transfer player: " + player.getName());
        player.closeInventory();
        if (player.isSleeping())
            player.wakeup(false);
        player.getInventory().clear();
        player.setExp(0.0f);
        player.setLevel(0);
        getServer().dispatchCommand(getServer().getConsoleSender(), "effect clear " + player.getName());
        player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getBaseValue());
        player.setFoodLevel(20);
        player.setRemainingAir(player.getMaximumAir());
        player.setArrowsInBody(0);
        getServer().dispatchCommand(getServer().getConsoleSender(), "advancement revoke " + player.getName() + " everything");
        getServer().getScheduler().runTaskLater(this, () -> player.teleport(Objects.requireNonNull(getServer().getWorld(newWorld)).getSpawnLocation()), 1);
    }
}
