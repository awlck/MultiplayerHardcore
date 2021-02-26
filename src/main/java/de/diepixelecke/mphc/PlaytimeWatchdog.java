package de.diepixelecke.mphc;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.CopyOnWriteArrayList;

public class PlaytimeWatchdog implements Runnable {
    private final MultiplayerHardcore plugin;

    PlaytimeWatchdog(MultiplayerHardcore main) {
        plugin = main;
    }

    @Override
    public void run() {
        if (!plugin.playtime.isPlaytime()) {
            CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>(Bukkit.getOnlinePlayers());
            final String nextPlaytime = plugin.playtime.getNextPlaytime();
            for (Player p : players) {
                p.kickPlayer(String.format("Playtime has ended. The next window begins %s.", nextPlaytime));
            }
        }
    }
}
