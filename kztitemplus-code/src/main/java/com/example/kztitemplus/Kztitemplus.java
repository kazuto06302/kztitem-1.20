package com.example.kztitemplus;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Kztitemplus extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new PerunListener(), this);
        getServer().getPluginManager().registerEvents(new DeathsScythe(), this);
        getServer().getPluginManager().registerEvents(new Excaliber(), this);
        getServer().getPluginManager().registerEvents(new Exodus(), this);
        getServer().getPluginManager().registerEvents(new lightningball(this), this);
        getServer().getPluginManager().registerEvents(new modularbow(this), this);
        getServer().getPluginManager().registerEvents(new GoldenHead(), this);
        getServer().getPluginManager().registerEvents(new Head(), this);
        getServer().getPluginManager().registerEvents(new SmelterPickaxe(), this);
        getLogger().info("kztitem-plus has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("kztitem-plus has been disabled.");
    }
}
