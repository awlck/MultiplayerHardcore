package de.diepixelecke.mphc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;

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

            LocalTime beginning = LocalTime.parse(hoursForDay.substring(0,5));
            LocalTime ending = LocalTime.parse(hoursForDay.substring(6));
            rules.put(d, new TimeRule(beginning, ending));
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

    private interface PlaytimeRule {

    }

    private static class ClosedRule implements PlaytimeRule {

    }

    private static class OpenRule implements PlaytimeRule {

    }

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
