package com.example.kztRecipe2;

import com.example.kztRecipe2.gui.ItemGUI;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomItems extends JavaPlugin {

    private static CustomItems instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        //saveResource("items/anduril.yml", false);
        ItemManager.loadItems();
        getServer().getPluginManager().registerEvents(new ItemListener(), this);
        getServer().getPluginManager().registerEvents(new ItemGUI(), this);
        if (getCommand("kztitem") != null) {
            getCommand("kztitem").setExecutor(new CommandHandler());
        } else {
            getLogger().warning("コマンド 'kztitem' が plugin.yml に見つかりませんでした！");
        }
        new ItemListener().startEffectTask();
    }

    public static CustomItems getInstance() {
        return instance;
    }
}
