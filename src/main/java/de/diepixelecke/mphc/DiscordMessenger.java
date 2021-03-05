package de.diepixelecke.mphc;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

class DiscordMessenger {
    private final MultiplayerHardcore plugin;

    DiscordMessenger(MultiplayerHardcore main) {
        plugin = main;
    }

    void sendMessageIfConfigured(String message) {
        if (!plugin.getConfig().getBoolean("discord.enabled")) return;
        String webhookUrl = plugin.getConfig().getString("discord.webhookUrl");
        if (webhookUrl == null || webhookUrl.equalsIgnoreCase("")) return;
        DiscordMessage msg = new DiscordMessage(message);
        byte[] toSend = (new Gson()).toJson(msg).getBytes(StandardCharsets.UTF_8);
        int length = toSend.length;

        URL url;
        try {
            url = new URL(webhookUrl);
        } catch (MalformedURLException e) {
            plugin.getLogger().log(Level.WARNING, "Discord Webhook URL appears to be invalid.");
            plugin.getLogger().log(Level.WARNING, e.getMessage());
            return;
        }
        URLConnection conn;
        try {
            conn = url.openConnection();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Unable to open connection to Discord webhook.");
            plugin.getLogger().log(Level.WARNING, e.getMessage());
            return;
        }
        HttpURLConnection http = (HttpURLConnection) conn;
        try {
            http.setRequestMethod("POST");
        } catch (ProtocolException e) {
            plugin.getLogger().log(Level.WARNING, "Unable to set connection type for Discord webhook.");
            plugin.getLogger().log(Level.WARNING, e.getMessage());
            return;
        }
        http.setDoOutput(true);
        http.setFixedLengthStreamingMode(length);
        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        try {
            http.connect();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Unable to connect to Discord webhook.");
            plugin.getLogger().log(Level.WARNING, e.getMessage());
            return;
        }
        try (OutputStream os = http.getOutputStream()) {
            os.write(toSend);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Unable to write to Discord webhook.");
            plugin.getLogger().log(Level.WARNING, e.getMessage());
            return;
        }
        http.disconnect();
    }

    static class DiscordMessage {
        String content;

        DiscordMessage(String message) {
            content = message;
        }
    }
}
