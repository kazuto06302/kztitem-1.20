package com.example.kztRecipe2.gui;

import com.example.kztRecipe2.CustomItems;
import com.example.kztRecipe2.ItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;

public class ItemGUI implements Listener {

    private static final int[] gridSlots = {10, 11, 12, 19, 20, 21, 28, 29, 30};

    public static void openMainGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§aカスタムアイテム一覧");

        int index = 0;
        for (Map.Entry<String, ItemStack> entry : ItemManager.customItems.entrySet()) {
            gui.setItem(index++, entry.getValue());
        }

        player.openInventory(gui);
    }

    public static void openRecipe(Player player, String itemKey) {
        File file = new File(CustomItems.getInstance().getDataFolder(), "items/" + itemKey + ".yml");
        if (!file.exists()) {
            player.sendMessage("§cレシピファイルが見つかりません: " + itemKey);
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection recipe = config.getConfigurationSection("recipe");
        if (recipe == null) {
            player.sendMessage("§cこのアイテムにはレシピが定義されていません。");
            return;
        }

        Inventory gui = Bukkit.createInventory(null, 45, "§eレシピ: " + itemKey);

        // 背景装飾（ただし材料スロットは除く）
        Set<Integer> gridSet = new HashSet<>();
        for (int i : gridSlots) gridSet.add(i);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < gui.getSize(); i++) {
            if (!gridSet.contains(i)) {
                gui.setItem(i, filler);
            }
        }

        // 材料配置
        int index = 0;
        List<String> shape = recipe.getStringList("shape");
        ConfigurationSection ing = recipe.getConfigurationSection("ingredients");

        for (String row : shape) {
            for (char c : row.toCharArray()) {
                if (index >= gridSlots.length) break;
                String matName = ing.getString(String.valueOf(c));
                if (matName != null) {
                    Material mat = Material.matchMaterial(matName.toUpperCase());
                    if (mat != null) {
                        gui.setItem(gridSlots[index], new ItemStack(mat));
                    } else if (matName.toLowerCase().startsWith("potion:")) {
                        try {
                            String potionKey = matName.split(":", 2)[1].toUpperCase();
                            org.bukkit.potion.PotionType type = org.bukkit.potion.PotionType.valueOf(potionKey);
                            ItemStack potion = new ItemStack(Material.POTION);
                            org.bukkit.inventory.meta.PotionMeta meta = (org.bukkit.inventory.meta.PotionMeta) potion.getItemMeta();
                            meta.setBasePotionData(new org.bukkit.potion.PotionData(type));
                            potion.setItemMeta(meta);
                            gui.setItem(gridSlots[index], potion);
                        } catch (Exception ignored) {}
                    }
                }
                index++;
            }
        }

        // 完成品（slot 24）
        gui.setItem(24, ItemManager.getItem(itemKey).clone());

        // 戻るボタン（slot 44）
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§c戻る");
        back.setItemMeta(backMeta);
        gui.setItem(44, back);

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();

        if (title.equals("§aカスタムアイテム一覧") || title.startsWith("§eレシピ:")) {
            e.setCancelled(true); // クリック無効化

            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;

            if (title.equals("§aカスタムアイテム一覧")) {
                for (Map.Entry<String, ItemStack> entry : ItemManager.customItems.entrySet()) {
                    if (clicked.isSimilar(entry.getValue())) {
                        openRecipe((Player) e.getWhoClicked(), entry.getKey());
                        return;
                    }
                }
            } else if (clicked.getType() == Material.BARRIER &&
                    clicked.getItemMeta().getDisplayName().contains("戻る")) {
                openMainGUI((Player) e.getWhoClicked());
            }
        }
    }
}
