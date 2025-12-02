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
            while (running) {
                try {
                    check();
                    Thread.sleep(1300);
                } catch (Exception ignored) {}
            }
        }).start();
    }

    public void stop() { running = false; }

    private void check() throws Exception {
        URL url = new URL("https://api.telegram.org/bot" + token + "/getUpdates?offset=" + (lastUpdate + 1));
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
            if (!upd.has("message")) continue;
            JSONObject msg = upd.getJSONObject("message");
            long cid = msg.getJSONObject("chat").getLong("id");
            if (cid != chatId) continue;
            if (!msg.has("text")) continue;
            handle(msg.getString("text"));
        }
    }

    private void handle(String t) {
        if (t.equals("/players")) send("Online: " + Bukkit.getOnlinePlayers().size());
        else if (t.startsWith("/say ")) {
            Bukkit.broadcastMessage("§b[Telegram] §f" + t.substring(5));
            send("Mensaje enviado.");
        }
        else if (t.startsWith("/cmd ")) {
            String cmd = t.substring(5);
            Bukkit.getScheduler().runTask(plugin, () -> {
                boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                send(ok ? "Comando ejecutado." : "Error ejecutando comando.");
            });
        }
        else if (t.equals("/stop")) {
            send("Servidor apagándose...");
            Bukkit.shutdown();
        }
        else send("Comando no reconocido.");
    }

    private void send(String msg) {
        try {
            msg = msg.replace(" ", "%20");
            new URL("https://api.telegram.org/bot" + token + "/sendMessage?chat_id=" + chatId + "&text=" + msg).openStream().close();
        } catch (Exception e) {
    e.printStackTrace();
}

    }
}
