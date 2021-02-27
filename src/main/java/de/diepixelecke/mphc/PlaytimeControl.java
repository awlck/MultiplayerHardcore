package de.diepixelecke.mphc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.logging.Level;

public class PlaytimeControl {
    private final MultiplayerHardcore plugin;
    private final HashMap<DayOfWeek, PlaytimeRule> rules = new HashMap<>();

    PlaytimeControl(MultiplayerHardcore main) {
        plugin = main;
    }

    static PlaytimeControl fromConfig(MultiplayerHardcore main) {
        PlaytimeControl result = new PlaytimeControl(main);
        result.loadConfig();
        return result;
    }

    void loadConfig() {
        for (DayOfWeek d: DayOfWeek.values()) {
            String hoursForDay = plugin.getConfig().getString("timeOfPlay." + d.name());
            if (hoursForDay == null || hoursForDay.equalsIgnoreCase("open")) {
                rules.put(d, new OpenRule());
                continue;
            } else if (hoursForDay.equalsIgnoreCase("closed")) {
                rules.put(d, new ClosedRule());
                continue;
            }

            try {
                LocalTime beginning = LocalTime.parse(hoursForDay.substring(0,5));
                LocalTime ending;
                String endingTime = hoursForDay.substring(6);
                if (endingTime.equalsIgnoreCase("24:00")) ending = LocalTime.of(23, 59, 59);
                else ending = LocalTime.parse(endingTime);
                rules.put(d, new TimeRule(beginning, ending));
            } catch (DateTimeParseException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not parse time " + e.getParsedString() + " (in playtime specification for " + d.name() + ")");
                plugin.getLogger().log(Level.WARNING, "Using `open' specification for " + d.name());
                rules.put(d, new OpenRule());
            }
        }
    }

    boolean isPlaytime() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        PlaytimeRule rule = rules.get(today.getDayOfWeek());
        if (rule instanceof OpenRule) return true;
        if (rule instanceof ClosedRule) return false;
        TimeRule tRule = (TimeRule) rule;
        if ((tRule.endsNextDay && now.isAfter(tRule.begins)) || (now.isAfter(tRule.begins) && now.isBefore(tRule.ends))) return true;
        PlaytimeRule yesterday = rules.get(today.minusDays(1).getDayOfWeek());
        if (yesterday instanceof TimeRule) {
            TimeRule tYesterday = (TimeRule) yesterday;
            return tYesterday.endsNextDay && now.isBefore(tYesterday.ends);
        }
        return false;
    }

    String getNextPlaytime() {
        if (isPlaytime()) return "now";
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        PlaytimeRule rule = rules.get(today.getDayOfWeek());
        TimeRule t;
        if (rule instanceof TimeRule && now.isBefore((t = (TimeRule) rule).begins)) {
            return "today at " + t.begins.toString();
        }

        for (long i = 1; i < 7; i++) {
            LocalDate theDay = today.plusDays(i);
            rule = rules.get(theDay.getDayOfWeek());
            if (rule instanceof OpenRule) return "on " + theDay.getDayOfWeek().toString();
            if (rule instanceof ClosedRule) continue;
            return "on " + theDay.getDayOfWeek().toString() + " at " + ((TimeRule) rule).begins.toString();
        }
        return "not for the foreseeable future";
    }

    private interface PlaytimeRule { }

    private static class ClosedRule implements PlaytimeRule { }

    private static class OpenRule implements PlaytimeRule { }

    private static class TimeRule implements PlaytimeRule {
        private final LocalTime begins;
        private final LocalTime ends;
        private final boolean endsNextDay;

        TimeRule(LocalTime begins, LocalTime ends) {
            this.begins = begins;
            this.ends = ends;
            endsNextDay = ends.compareTo(begins) < 0;
        }
    }
}
