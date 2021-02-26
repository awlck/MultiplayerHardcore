package de.diepixelecke.mphc;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

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
        if (newHealth <= 0) {
            event.setCancelled(true);
            String msg = plugin.getDeathMessage(event.getCause(), DamageSourceFinder.describeSource(event), player.getDisplayName());
            Bukkit.broadcastMessage(msg);
            String nextWorld = plugin.logDeathAndGetNextAttempt(event.getCause(), player);
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
                String.format("The next playtime window is at %s.", plugin.playtime.getNextPlaytime()));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!plugin.hasPlayerParticipatedInAttempt(player)) {
            plugin.restartPlayerInWorld(player, plugin.getCurrentAttempt(), plugin.getConfig().getString("messages.ending"));
            plugin.addPlayerToCurrentAttempt(player);
        }
    }
}
