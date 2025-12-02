package com.victor.telegramcontrol;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TelegramBot {

    private final String token;
    private final long chatId;
    private final JavaPlugin plugin;
    private boolean running = true;
    private int lastUpdate = 0;

    public TelegramBot(String token, long chatId, JavaPlugin plugin) {
        this.token = token;
        this.chatId = chatId;
        this.plugin = plugin;
    }

    public void start() {
        new Thread(() -> {
            plugin.getLogger().info("Hilo del bot iniciado.");
            while (running) {
                try {
                    check();
                    Thread.sleep(1200);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error en check(): " + e.getMessage());
                }
            }
        }).start();
    }

    public void stop() {
        running = false;
    }

    private void check() throws Exception {

        URL url = new URL("https://api.telegram.org/bot" + token +
                "/getUpdates?timeout=5&offset=" + (lastUpdate + 1));

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder text = new StringBuilder();
        String line;

        while ((line = in.readLine()) != null) text.append(line);
        in.close();

        JSONObject json = new JSONObject(text.toString());
        JSONArray result = json.getJSONArray("result");

        for (int i = 0; i < result.length(); i++) {

            JSONObject upd = result.getJSONObject(i);
            lastUpdate = upd.getInt("update_id");

            // detectar donde está el mensaje
            JSONObject msg = null;

            if (upd.has("message")) msg = upd.getJSONObject("message");
            else if (upd.has("edited_message")) msg = upd.getJSONObject("edited_message");
            else if (upd.has("channel_post")) msg = upd.getJSONObject("channel_post");
            else continue;

            long cid = msg.getJSONObject("chat").getLong("id");
            if (cid != chatId) continue;

            if (!msg.has("text")) continue;

            String textMsg = msg.getString("text");
            plugin.getLogger().info("Recibido desde Telegram: " + textMsg);

            handle(textMsg);
        }
    }

    private void handle(String text) {

        if (text.equals("/players")) {
            send("Online: " + Bukkit.getOnlinePlayers().size());
        }

        else if (text.startsWith("/say ")) {
            Bukkit.broadcastMessage("§b[Telegram] §f" + text.substring(5));
            send("Mensaje enviado.");
        }

        else if (text.startsWith("/cmd ")) {
            String cmd = text.substring(5);
            Bukkit.getScheduler().runTask(plugin, () -> {
                boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                send(ok ? "Comando ejecutado." : "Error ejecutando comando.");
            });
        }

        else if (text.equals("/stop")) {
            send("Apagando servidor...");
            Bukkit.shutdown();
        }

        else {
            send("Comando no reconocido.");
        }
    }

    private void send(String msg) {
        try {
            msg = msg.replace(" ", "%20");
            new URL("https://api.telegram.org/bot" + token +
                    "/sendMessage?chat_id=" + chatId + "&text=" + msg)
                    .openStream().close();
        } catch (Exception e) {
            plugin.getLogger().warning("Error enviando mensaje TG: " + e.getMessage());
        }
    }
}
