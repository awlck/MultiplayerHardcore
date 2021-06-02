package de.diepixelecke.mphc;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.text.MessageFormat;
import java.util.Objects;

public class EventListener implements Listener {
    private final MultiplayerHardcore plugin;

    EventListener(MultiplayerHardcore main) {
        plugin = main;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (plugin.isBroken || plugin.isBusy) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        double newHealth = player.getHealth() - event.getFinalDamage();
        if (newHealth <= 0 &&
                player.getInventory().getItemInOffHand().getType() != Material.TOTEM_OF_UNDYING &&
                player.getInventory().getItemInMainHand().getType() != Material.TOTEM_OF_UNDYING) {
            event.setCancelled(true);
            String msg = plugin.getDeathMessage(event.getCause(), DamageSourceFinder.describeSource(event), player.getDisplayName());
            Bukkit.broadcastMessage(msg);
            plugin.messenger.sendMessageIfConfigured(msg);
            String nextWorld;
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent) event;
                nextWorld = plugin.logDeathAndGetNextAttempt(event.getCause(), player, evt.getDamager() instanceof Player ? (Player) evt.getDamager() : null);
            } else nextWorld = plugin.logDeathAndGetNextAttempt(event.getCause(), player, null);

            plugin.createWorldAndMoveEveryone(nextWorld, msg);
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (plugin.isBroken) event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                "The server was unable to generate the world for the next attempt. Please contact a server administrator.");
        if (plugin.isBusy) event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                "Generating world for the next attempt, stand by...");
        if (!plugin.playtime.isPlaytime()) event.disallow(PlayerLoginEvent.Result.KICK_OTHER,
                String.format("The next playtime window is %s.", plugin.playtime.getNextPlaytime()));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!plugin.hasPlayerParticipatedInAttempt(player)) {
            plugin.restartPlayerInWorld(player, plugin.getCurrentAttempt(), plugin.getConfig().getString("messages.ending"));
            plugin.addPlayerToCurrentAttempt(player);
        }
    }

    @EventHandler
    public void onServerListPingEvent(ServerListPingEvent event) {
        final FileConfiguration config = plugin.getConfig();
        if (!config.getBoolean("motd.enabled")) return;
        String line1 = config.getString("motd.line1");
        if (plugin.isBusy) {
            event.setMotd(MessageFormat.format("{0}\n{1}", line1, config.getString("motd.line2busy")));
        } else if (plugin.isBroken) {
            event.setMotd(MessageFormat.format("{0}\n{1}", line1, config.getString("motd.line2broken")));
        } else if (!plugin.playtime.isPlaytime()) {
            event.setMotd(MessageFormat.format("{0}\n{1}", line1, String.format(Objects.requireNonNull(config.getString("motd.line2closed")), plugin.playtime.getNextPlaytime())));
        } else {
            event.setMotd(MessageFormat.format("{0}\n{1}", line1, String.format(Objects.requireNonNull(config.getString("motd.line2default")), config.getInt("attempt.current"))));
        }
    }
}
