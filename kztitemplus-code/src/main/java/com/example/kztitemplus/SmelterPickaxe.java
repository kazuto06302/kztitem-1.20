package com.example.kztitemplus;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SmelterPickaxe implements Listener {

    private final Map<Material, ItemStack> smeltMap = new HashMap<>();

    public SmelterPickaxe() {
        // 鉱石 → 焼かれたドロップ
        smeltMap.put(Material.IRON_ORE, new ItemStack(Material.IRON_INGOT));
        smeltMap.put(Material.DEEPSLATE_IRON_ORE, new ItemStack(Material.IRON_INGOT));

        smeltMap.put(Material.GOLD_ORE, new ItemStack(Material.GOLD_INGOT));
        smeltMap.put(Material.DEEPSLATE_GOLD_ORE, new ItemStack(Material.GOLD_INGOT));

        smeltMap.put(Material.COPPER_ORE, new ItemStack(Material.COPPER_INGOT));
        smeltMap.put(Material.DEEPSLATE_COPPER_ORE, new ItemStack(Material.COPPER_INGOT));

        // 他にも追加可（例：古代の残骸 → ネザライトスクラップ など）
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (tool == null || tool.getType() != Material.IRON_PICKAXE) return;
        if (!tool.hasItemMeta() || !tool.getItemMeta().hasLore()) return;

        List<String> lore = tool.getItemMeta().getLore();
        if (lore == null || lore.stream().noneMatch(line -> ChatColor.stripColor(line).toLowerCase().contains("smelter's pickaxe")))
            return;

        Block block = event.getBlock();
        Material blockType = block.getType();

        if (!smeltMap.containsKey(blockType)) return;

        // 処理を上書き：ドロップをキャンセルし、焼かれたドロップを手動で落とす
        event.setDropItems(false);
        block.getWorld().dropItemNaturally(block.getLocation(), smeltMap.get(blockType));

        // エフェクト：炎のパーティクル
        block.getWorld().spawnParticle(Particle.FLAME, block.getLocation().add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.01);

        // サウンド（任意）：精錬音を再生
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_BLASTFURNACE_FIRE_CRACKLE, 1.0f, 1.0f);

        // ▼▼ 耐久値を5削る ▼▼
        short currentDurability = tool.getDurability();
        short newDurability = (short) (currentDurability + 4);
        tool.setDurability(newDurability);
    }
}
