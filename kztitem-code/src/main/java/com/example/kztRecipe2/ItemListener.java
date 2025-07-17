package com.example.kztRecipe2;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Map;

public class ItemListener implements Listener {

    public void startEffectTask() {
        Bukkit.getScheduler().runTaskTimer(CustomItems.getInstance(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                ItemStack item = p.getInventory().getItemInMainHand();
                String id = getItemId(item);
                if (id != null && ItemManager.itemEffects.containsKey(id)) {
                    Map<PotionEffectType, Integer> effects = ItemManager.itemEffects.get(id);
                    for (Map.Entry<PotionEffectType, Integer> entry : effects.entrySet()) {
                        p.addPotionEffect(new PotionEffect(entry.getKey(), 40, entry.getValue() - 1, true, false));
                    }
                }
            }
        }, 0L, 20L);
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        Player player = e.getPlayer();
        ItemStack newItem = player.getInventory().getItem(e.getNewSlot());
        String newId = getItemId(newItem);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (PotionEffectType type : PotionEffectType.values()) {
                    if (ItemManager.itemEffects.values().stream().anyMatch(map -> map.containsKey(type))) {
                        PotionEffect current = player.getPotionEffect(type);
                        if (current != null && current.getDuration() <= 100) {
                            if (newId == null || !ItemManager.itemEffects.getOrDefault(newId, Map.of()).containsKey(type)) {
                                player.removePotionEffect(type);
                            }
                        }
                    }
                }

                if (newId != null && ItemManager.itemEffects.containsKey(newId)) {
                    Map<PotionEffectType, Integer> effects = ItemManager.itemEffects.get(newId);
                    for (Map.Entry<PotionEffectType, Integer> entry : effects.entrySet()) {
                        player.addPotionEffect(new PotionEffect(entry.getKey(), 40, entry.getValue() - 1, true, false));
                    }
                }
            }
        }.runTaskLater(CustomItems.getInstance(), 2L);
    }

    @EventHandler
    public void onAnvilUse(InventoryClickEvent e) {
        if (e.getView().getType() != InventoryType.ANVIL) return;

        ItemStack movedItem = e.getCurrentItem();

        // 数字キーでホットバーから移動した場合はそちらを取得
        if (e.getClick() == ClickType.NUMBER_KEY) {
            int hotbarSlot = e.getHotbarButton();
            movedItem = e.getWhoClicked().getInventory().getItem(hotbarSlot);
        }

        String id = getItemId(movedItem);
        if (id != null && !ItemManager.itemAnvilUsage.getOrDefault(id, false)) {
            e.setCancelled(true);
            e.getWhoClicked().sendMessage("§cこのカスタムアイテムは金床で使用できません。");
        }
    }


    @EventHandler
    public void onEnchant(PrepareItemEnchantEvent e) {
        String id = getItemId(e.getItem());
        if (id != null && !ItemManager.itemEnchantUsage.getOrDefault(id, false)) {
            e.setCancelled(true);
            e.getEnchanter().sendMessage("§cこのカスタムアイテムはエンチャントできません。");
        }
    }

    @EventHandler
    public void onSmithing(PrepareSmithingEvent e) {
        SmithingInventory inv = e.getInventory();
        String idBase = getItemId(inv.getInputEquipment());
        String idAdd = getItemId(inv.getInputMineral());

        if ((idBase != null && !ItemManager.itemSmithingUsage.getOrDefault(idBase, false)) ||
                (idAdd != null && !ItemManager.itemSmithingUsage.getOrDefault(idAdd, false))) {
            e.setResult(null);
        }
    }

    @EventHandler
    public void onGrindstone(PrepareGrindstoneEvent e) {
        GrindstoneInventory inv = e.getInventory();
        String id1 = getItemId(inv.getUpperItem());
        String id2 = getItemId(inv.getLowerItem());

        if ((id1 != null && !ItemManager.itemGrindstoneUsage.getOrDefault(id1, false)) ||
                (id2 != null && !ItemManager.itemGrindstoneUsage.getOrDefault(id2, false))) {
            e.setResult(null);
        }
    }

    private String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        NamespacedKey idKey = new NamespacedKey(CustomItems.getInstance(), "itemId");
        if (meta.getPersistentDataContainer().has(idKey, org.bukkit.persistence.PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(idKey, org.bukkit.persistence.PersistentDataType.STRING);
        }

        return null;
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        ItemStack result = e.getRecipe().getResult();
        String id = getItemId(result);
        if (id == null) return;

        Map<Material, Integer> cost = ItemManager.itemExtraCosts.get(id);
        if (cost == null || cost.isEmpty()) return;

        Inventory inv = player.getInventory();
        for (Map.Entry<Material, Integer> entry : cost.entrySet()) {
            int has = Arrays.stream(inv.getContents())
                    .filter(i -> i != null && i.getType() == entry.getKey())
                    .mapToInt(ItemStack::getAmount)
                    .sum();
            if (has < entry.getValue()) {
                player.sendMessage("§c必要な追加素材が足りません: " + entry.getKey() + " x" + entry.getValue());
                e.setCancelled(true);
                return;
            }
        }

        // コスト消費
        for (Map.Entry<Material, Integer> entry : cost.entrySet()) {
            int needed = entry.getValue();
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack != null && stack.getType() == entry.getKey()) {
                    int amt = stack.getAmount();
                    if (amt > needed) {
                        stack.setAmount(amt - needed);
                        break;
                    } else {
                        needed -= amt;
                        inv.clear(i);
                        if (needed <= 0) break;
                    }
                }
            }
        }
    }


}