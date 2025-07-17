package com.example.kztRecipe2;

import com.example.kztRecipe2.gui.ItemGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                if (!player.hasPermission("kztitem.gui")) {
                    player.sendMessage("§c[kztitem] このコマンドを実行する権限がありません。");
                    return true;
                }
                ItemGUI.openMainGUI(player);
                return true;
            }
            sender.sendMessage("§c[kztitem] プレイヤー専用コマンドです。");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("kztitem.reload")) {
                sender.sendMessage("§c[kztitem] このコマンドを実行する権限がありません。");
                return true;
            }

            ItemManager.loadItems();
            sender.sendMessage("§6[kztitem] アイテムを再読み込みしました。");
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("kztitem.give")) {
                sender.sendMessage("§c[kztitem] このコマンドを実行する権限がありません。");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage("§6[kztitem] 使い方: /kztitem give <player> <itemId>");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§c[kztitem] プレイヤーが見つかりません: " + args[1]);
                return true;
            }

            ItemStack item = ItemManager.getItem(args[2]);
            if (item == null) {
                sender.sendMessage("§c[kztitem] アイテムが見つかりません: " + args[2]);
                return true;
            }

            target.getInventory().addItem(item.clone());
            sender.sendMessage("§6[kztitem] " + target.getName() + " に「" + args[2] + "」を配布しました。");
            return true;
        }

        sender.sendMessage("§6[kztitem] 使い方: /kztitem [reload|give <player> <itemId>]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "give").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return ItemManager.customItems.keySet().stream()
                    .filter(id -> id.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
