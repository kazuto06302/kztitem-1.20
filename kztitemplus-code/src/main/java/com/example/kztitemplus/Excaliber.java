package com.example.kztitemplus;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Excaliber implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 5000; // 5秒

    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        // クールダウンチェック
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < COOLDOWN_MS) return;
        cooldowns.put(uuid, now);

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon.getType() != Material.DIAMOND_SWORD) return;

        if (!weapon.hasItemMeta() || !weapon.getItemMeta().hasLore()) return;
        List<String> lore = weapon.getItemMeta().getLore();
        if (lore == null || lore.stream().noneMatch(line -> ChatColor.stripColor(line).toLowerCase().contains("excalibur")))
            return;

        // 爆発音を鳴らす
        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        // 防具貫通ダメージ（4HP = 2ハート）
        double damage = 4.0;
        double newHealth = Math.max(target.getHealth() - damage, 0.1);
        target.setHealth(newHealth);

    }
}
