package com.example.kztitemplus;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class Exodus implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 2000; // 2秒

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Player)) return;

        // クールダウンチェック
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < COOLDOWN_MS) return;
        cooldowns.put(uuid, now);

        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null || helmet.getType() == Material.AIR) return;
        if (!helmet.hasItemMeta() || !helmet.getItemMeta().hasLore()) return;

        List<String> lore = helmet.getItemMeta().getLore();
        if (lore == null || lore.stream().noneMatch(line -> ChatColor.stripColor(line).toLowerCase().contains("exodus")))
            return;

        // 回復（2HP = 1ハート）
        double healAmount = 2.0;
        double newHealth = Math.min(player.getHealth() + healAmount,
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        player.setHealth(newHealth);

        // 再生エフェクト（視覚効果）
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20, 1, false, true));

    }
}
