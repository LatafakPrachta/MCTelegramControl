package com.victor.telegramcontrol;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private TelegramBot bot;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String token = getConfig().getString("telegram-token");
        long chatId = getConfig().getLong("chat-id");
        bot = new TelegramBot(token, chatId, this);
        bot.start();
        getLogger().info("Telegram bot conectado.");
    }

    @Override
    public void onDisable() {
        if (bot != null) bot.stop();
    }
}
