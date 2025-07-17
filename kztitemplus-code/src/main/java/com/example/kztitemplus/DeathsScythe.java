package com.example.kztitemplus;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class DeathsScythe implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 500; // 0.5秒

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
        if (weapon.getType() != Material.IRON_HOE) return;

        if (!weapon.hasItemMeta() || !weapon.getItemMeta().hasLore()) return;
        List<String> lore = weapon.getItemMeta().getLore();
        if (lore == null || lore.stream().noneMatch(line -> ChatColor.stripColor(line).toLowerCase().contains("scythe")))
            return;

        double targetHealth = target.getHealth();
        double damage = targetHealth * 0.2;
        damage = Math.min(damage, targetHealth - 0.1);
        if (damage <= 0) return;

        // 防具無視ダメージ（直接HPを減らす）
        double afterHealth = Math.max(target.getHealth() - damage, 0.1);
        target.setHealth(afterHealth);

        // 回復処理
        double heal = damage * 0.25;
        player.setHealth(Math.min(player.getHealth() + heal,
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));

        // メッセージ表示
        player.sendMessage(ChatColor.GREEN + "与えたダメージ: " + String.format("%.1f", damage) +ChatColor.WHITE + "/" + ChatColor.RED + String.format("%.1f", target.getHealth())+ChatColor.AQUA + "　回復したHP: " + String.format("%.1f", heal));

        // ▼▼ 耐久値処理ここ ▼▼
        ItemMeta meta = weapon.getItemMeta();
        if (meta != null && weapon.getType().getMaxDurability() > 0) {
            short current = weapon.getDurability();
            short updated = (short) (current + 23);
            weapon.setDurability(updated);

        }
    }
}
