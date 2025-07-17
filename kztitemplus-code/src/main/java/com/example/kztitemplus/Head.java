package com.example.kztitemplus;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class Head implements Listener {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 1000; // 1秒

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < COOLDOWN_MS) {
            return; // クールダウン中は無視
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.PLAYER_HEAD) return;

        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return;
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null || lore.stream().noneMatch(line -> ChatColor.stripColor(line).toLowerCase().endsWith("'s head")))
            return;

        cooldowns.put(uuid, now); // クールダウン登録

        // アイテムを1つ消費
        item.setAmount(item.getAmount() - 1);

        // 効果付与
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 2)); // 5秒, Lv3
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 21, 1)); // 21秒, Lv2

        // 音を再生（食べ終わり）
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1.0f, 1.0f);

        // メッセージ表示
        player.sendMessage(ChatColor.GREEN+"You ate a "+ChatColor.GOLD+"Head"+ChatColor.GREEN+" and gained 4 seconds of Regeneration III");
        player.sendMessage(ChatColor.GREEN+"You gained 21 seconds of Speed II");
    }

    // ヘッドを設置できないようにする
    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || item.getType() != Material.PLAYER_HEAD) return;

        if (!item.hasItemMeta() || !item.getItemMeta().hasLore()) return;
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null || lore.stream().noneMatch(line -> ChatColor.stripColor(line).toLowerCase().endsWith("'s head")))
            return;

        event.setCancelled(true); // 設置をブロック
    }
}
