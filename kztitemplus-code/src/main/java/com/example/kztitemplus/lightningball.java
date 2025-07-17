package com.example.kztitemplus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class lightningball implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 5000; // 5秒
    private static final String META_TAG = "lightningball";

    public lightningball(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerThrow(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() != Material.SNOWBALL) return;
        if (!hasLightningLore(item)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < COOLDOWN_MS) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "LightningBall はクールダウン中です！");
        } else {
            cooldowns.put(uuid, now);
        }
    }

    @EventHandler
    public void onSnowballLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Snowball)) return;

        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!hasLightningLore(item)) return;

        // 雪玉にメタデータを設定（lightningballタグ）
        projectile.setMetadata(META_TAG, new FixedMetadataValue(plugin, true));

        // 雷を追尾的に落とす処理
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (projectile.isDead() || projectile.isOnGround() || ticks > 40) {
                    cancel();
                    return;
                }

                World world = projectile.getWorld();
                world.strikeLightning(projectile.getLocation());
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0L, 3L); // 5tickごとにチェック（0.25秒ごと）
    }

    private boolean hasLightningLore(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return false;
        List<String> lore = item.getItemMeta().getLore();
        return lore.stream().anyMatch(line ->
                ChatColor.stripColor(line).toLowerCase().contains("lightningball"));
    }
}
