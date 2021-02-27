package de.diepixelecke.mphc;

import java.util.logging.Level;

public class AttemptPruner implements Runnable {
    private final MultiplayerHardcore plugin;
    private final String attemptName;
    private final int attemptsToKeep;

    public AttemptPruner(MultiplayerHardcore plugin, String attemptName, int attemptsToKeep) {
        this.plugin = plugin;
        this.attemptName = attemptName;
        this.attemptsToKeep = attemptsToKeep;
    }

    @Override
    public void run() {
        String current;
        for (int i = plugin.getConfig().getInt("attempt.current") - 1 - attemptsToKeep; i > 0; i--) {
            current = attemptName + i;
            if (plugin.mv.getMVWorldManager().isMVWorld(current)) {
                plugin.getLogger().log(Level.INFO, "Pruning old attempt " + current);
                plugin.mv.getMVWorldManager().deleteWorld(current + "_the_end", true, true);
                plugin.mv.getMVWorldManager().deleteWorld(current + "_nether", true, true);
                plugin.mv.getMVWorldManager().deleteWorld(current, true, true);
            } else break;
        }
    }
}
