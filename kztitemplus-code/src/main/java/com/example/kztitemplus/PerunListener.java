package com.example.kztitemplus;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class PerunListener implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 8000; // 8秒

    @EventHandler
    public void onEntityHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon.getType() != Material.DIAMOND_AXE) return;

        if (!weapon.hasItemMeta() || !weapon.getItemMeta().hasLore()) return;

        List<String> lore = weapon.getItemMeta().getLore();
        if (lore == null || lore.stream().noneMatch(line -> ChatColor.stripColor(line).toLowerCase().contains("perun")))
            return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (cooldowns.containsKey(uuid) && (now - cooldowns.get(uuid)) < COOLDOWN_TIME) return;

        cooldowns.put(uuid, now);

        // ⚡ 本物の雷（ダメージあり）を落とす
        player.getWorld().strikeLightning(target.getLocation());

        // 🎯 防具貫通の追加ダメージ
        target.damage(3.0, player);
    }

    // 🔥 雷による着火を防ぐ
    @EventHandler
    public void onLightningIgnite(BlockIgniteEvent event) {
        if (event.getCause() == BlockIgniteEvent.IgniteCause.LIGHTNING) {
            event.setCancelled(true); // 火を付けない
        }
    }
}
