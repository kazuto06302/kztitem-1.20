package com.example.kztitemplus;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class modularbow implements Listener {

    private final JavaPlugin plugin;

    public modularbow(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLeftClick(PlayerInteractEvent event) {
        if (!event.getAction().toString().contains("LEFT_CLICK")) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.BOW) return;

        if (!item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        boolean isModularBow = lore.stream()
                .anyMatch(line -> ChatColor.stripColor(line).toLowerCase().contains("modular bow"));

        if (!isModularBow) return;

        int currentMode = 1;
        if (!lore.isEmpty()) {
            String firstLine = ChatColor.stripColor(lore.get(0)).toLowerCase();
            if (firstLine.contains("mode2")) currentMode = 2;
            else if (firstLine.contains("mode3")) currentMode = 3;
        }

        int nextMode = (currentMode % 3) + 1;
        String newLoreLine = ChatColor.GOLD + "mode" + nextMode;

        if (lore.isEmpty()) lore.add(newLoreLine);
        else lore.set(0, newLoreLine);

        meta.setLore(lore);
        item.setItemMeta(meta);
        player.sendMessage(ChatColor.GREEN + "Modular Bowをmode " + nextMode + " に切り替えました");
    }

    @EventHandler
    public void onArrowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (bow == null || !bow.hasItemMeta() || !bow.getItemMeta().hasLore()) return;

        List<String> lore = bow.getItemMeta().getLore();
        if (lore == null || lore.isEmpty()) return;

        String mode = ChatColor.stripColor(lore.get(0)).toLowerCase();
        Arrow arrow = (Arrow) event.getProjectile();

        // モード1（パンチ） → ノックバック付与
        if (mode.contains("mode1")) {
            arrow.setKnockbackStrength(2);
        }

        // モード2・3 → フルチャージチェック & メタデータ付与
        if (mode.contains("mode2") || mode.contains("mode3")) {
            if (event.getForce() >= 1.0) {
                arrow.setMetadata("fullcharge", new FixedMetadataValue(plugin, mode));
            }
        }
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;

        if (!arrow.hasMetadata("fullcharge")) return;
        String mode = arrow.getMetadata("fullcharge").get(0).asString();

        Entity hitEntity = event.getHitEntity();
        if (mode.equals("mode2")) {
            if (hitEntity instanceof LivingEntity target) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 0)); // 5秒
            }
        }

        if (mode.equals("mode3")) {
            if (hitEntity instanceof Player target) {
                target.getWorld().strikeLightning(target.getLocation());
            }
        }
    }
}
