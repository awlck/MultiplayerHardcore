package de.diepixelecke.mphc;

import org.bukkit.Bukkit;

import java.io.ByteArrayInputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class PlaytimeControl {
    private final MultiplayerHardcore plugin;
    private HashMap<DayOfWeek, PlaytimeRule> rules;

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
                rules.put(d, new PlaytimeRule(d, RuleType.OPEN));
                continue;
            } else if (hoursForDay.equalsIgnoreCase("closed")) {
                rules.put(d, new PlaytimeRule(d, RuleType.CLOSED));
                continue;
            }

            LocalTime beginning = LocalTime.parse(hoursForDay.substring(0,5));
            LocalTime ending = LocalTime.parse(hoursForDay.substring(6));
            rules.put(d, new PlaytimeRule(d, beginning, ending));
        }
    }

    boolean isPlaytime() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        PlaytimeRule rule = rules.get(today.getDayOfWeek());
        PlaytimeRule yesterday = rules.get(today.minusDays(1).getDayOfWeek());
        if (rule.getType() == RuleType.OPEN) return true;
        if (rule.getType() == RuleType.CLOSED) return false;
        return (rule.endsNextDay && now.isAfter(rule.begins)) ||
                (yesterday.endsNextDay && now.isBefore(yesterday.ends)) ||
                (now.isAfter(rule.begins) && now.isBefore(rule.ends));
    }

    String getNextPlaytime() {
        if (isPlaytime()) return "now";
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        PlaytimeRule rule = rules.get(today.getDayOfWeek());
        if (rule.type != RuleType.CLOSED && now.isBefore(rule.begins)) {
            return "today at " + rule.begins.toString();
        }

        for (long i = 1; i < 7; i++) {
            LocalDate theDay = today.plusDays(i);
            rule = rules.get(theDay.getDayOfWeek());
            if (rule.type == RuleType.OPEN) return "on " + theDay.getDayOfWeek().toString();
            if (rule.type == RuleType.CLOSED) continue;
            return "on " + theDay.getDayOfWeek().toString() + " at " + rule.begins.toString();
        }
        return "not for the foreseeable future";
    }

    private static class PlaytimeRule {
        private final RuleType type;
        private final DayOfWeek day;
        private final LocalTime begins;
        private final LocalTime ends;
        private final boolean endsNextDay;

        PlaytimeRule(DayOfWeek d, RuleType type) {
            this.type = type;
            day = d;
            begins = null;
            ends = null;
            endsNextDay = false;
        }

        PlaytimeRule(DayOfWeek day, LocalTime begins, LocalTime ends) {
            this.type = RuleType.TIME_CONTROL;
            this.day = day;
            this.begins = begins;
            this.ends = ends;
            endsNextDay = ends.compareTo(begins) < 0;
        }

        RuleType getType() {
            return type;
        }
        DayOfWeek getDay() {
            return day;
        }
        LocalTime getBegins() {
            return begins;
        }
        LocalTime getEnds() {
            return ends;
        }
        boolean getEndsNextDay() {
            return endsNextDay;
        }

    }

    enum RuleType {
        TIME_CONTROL,
        OPEN,
        CLOSED
    }
}
