package com.example.kztRecipe2;

import com.mojang.authlib.GameProfile;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;
import com.mojang.authlib.properties.Property;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

public class ItemManager {

    public static Map<String, ItemStack> customItems = new HashMap<>();
    public static Map<String, Map<PotionEffectType, Integer>> itemEffects = new HashMap<>();

    public static Map<String, Boolean> itemAnvilUsage = new HashMap<>();
    public static Map<String, Boolean> itemEnchantUsage = new HashMap<>();
    public static Map<String, Boolean> itemSmithingUsage = new HashMap<>();
    public static Map<String, Boolean> itemGrindstoneUsage = new HashMap<>();
    public static Map<String, Map<Material, Integer>> itemExtraCosts = new HashMap<>();


    public static void loadItems() {
        customItems.clear();
        itemEffects.clear();
        itemAnvilUsage.clear();
        itemEnchantUsage.clear();
        itemSmithingUsage.clear();
        itemGrindstoneUsage.clear();

        File itemsFolder = new File(CustomItems.getInstance().getDataFolder(), "items");
        if (!itemsFolder.exists()) itemsFolder.mkdirs();

        File[] files = itemsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String key = file.getName().replace(".yml", "");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            try {
                Material material = Material.valueOf(config.getString("material"));
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta == null) continue;

                meta.setDisplayName(config.getString("name"));
                meta.setCustomModelData(config.getInt("model"));
                meta.setLore(config.getStringList("lore"));
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

                // Enchantments
                ConfigurationSection enchSec = config.getConfigurationSection("enchantments");
                if (enchSec != null) {
                    for (String enchKey : enchSec.getKeys(false)) {
                        Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(enchKey.toLowerCase()));
                        if (enchant != null) {
                            meta.addEnchant(enchant, enchSec.getInt(enchKey), true);
                        }
                    }
                }

                // loadItems() 内に以下を追加
                if (config.contains("extra-cost")) {
                    ConfigurationSection costSec = config.getConfigurationSection("extra-cost");
                    Map<Material, Integer> costs = new HashMap<>();
                    for (String mat : costSec.getKeys(false)) {
                        Material cost = Material.matchMaterial(mat.toUpperCase());
                        if (cost != null) {
                            costs.put(cost, costSec.getInt(mat));
                        }
                    }
                    itemExtraCosts.put(key, costs);
                }


                // Attributes
                ConfigurationSection attrSec = config.getConfigurationSection("attributes");
                if (attrSec != null) {
                    for (String attr : attrSec.getKeys(false)) {
                        try {
                            Attribute attribute = Attribute.valueOf(attr);
                            double value = attrSec.getDouble(attr);
                            meta.addAttributeModifier(attribute, new AttributeModifier(UUID.randomUUID(), attr, value, AttributeModifier.Operation.ADD_NUMBER));
                        } catch (Exception ignored) {}
                    }
                }

                if (material == Material.PLAYER_HEAD && config.contains("skull-url")) {
                    String url = config.getString("skull-url");
                    if (meta instanceof SkullMeta skullMeta && url != null && url.startsWith("http")) {
                        try {
                            // Base64 にエンコード
                            String base64 = Base64.getEncoder().encodeToString((
                                    "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}"
                            ).getBytes());

                            // GameProfile に埋め込む
                            GameProfile profile = new GameProfile(UUID.randomUUID(), "CustomHead");
                            profile.getProperties().put("textures", new Property("textures", base64));

                            // プロファイルフィールドに設定
                            Field field = skullMeta.getClass().getDeclaredField("profile");
                            field.setAccessible(true);
                            field.set(skullMeta, profile);

                            meta = skullMeta;

                        } catch (Exception e) {
                            // "Profile name must not be null" のような既知の問題は無視
                            String msg = e.getMessage();
                            if (msg == null || !msg.contains("Profile name must not be null")) {
                                e.printStackTrace(); // 他の例外だけ表示
                            }
                        }
                    }
                }




                NamespacedKey idKey = new NamespacedKey(CustomItems.getInstance(), "itemId");
                meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, key);

                item.setItemMeta(meta);
                customItems.put(key, item);

                itemAnvilUsage.put(key, config.getBoolean("allow-anvil", false));
                itemEnchantUsage.put(key, config.getBoolean("allow-enchant", false));
                itemSmithingUsage.put(key, config.getBoolean("allow-smithing", false));
                itemGrindstoneUsage.put(key, config.getBoolean("allow-grindstone", false));

                ConfigurationSection effectSec = config.getConfigurationSection("effects");
                if (effectSec != null) {
                    Map<PotionEffectType, Integer> map = new HashMap<>();
                    for (String eff : effectSec.getKeys(false)) {
                        for (PotionEffectType type : PotionEffectType.values()) {
                            if (type != null && type.getKey().getKey().equalsIgnoreCase(eff)) {
                                map.put(type, effectSec.getInt(eff));
                                break;
                            }
                        }
                    }
                    itemEffects.put(key, map);
                }

                if (config.contains("recipe")) {
                    loadRecipe(key, item.clone(), config.getConfigurationSection("recipe"));
                }

            } catch (Exception e) {
                Bukkit.getLogger().warning("[kztitem] " + key + ".yml の読み込みに失敗: " + e.getMessage());
            }
        }
    }

    private static void loadRecipe(String key, ItemStack result, ConfigurationSection section) {
        if (section == null) return;

        List<String> shapeList = section.getStringList("shape");
        if (shapeList.size() != 3) return;

        NamespacedKey namespacedKey = new NamespacedKey(CustomItems.getInstance(), key);
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe r = it.next();
            if (r instanceof Keyed && ((Keyed) r).getKey().equals(namespacedKey)) {
                it.remove();
                break;
            }
        }

        ShapedRecipe recipe = new ShapedRecipe(namespacedKey, result);
        recipe.shape(shapeList.toArray(new String[0]));

        ConfigurationSection ingredients = section.getConfigurationSection("ingredients");
        if (ingredients == null) return;

        for (String symbol : ingredients.getKeys(false)) {
            String value = ingredients.getString(symbol);
            if (value == null) continue;

            if (value.toLowerCase().startsWith("potion:")) {
                String potionTypeName = value.split(":", 2)[1].toUpperCase();
                try {
                    PotionType potionType = PotionType.valueOf(potionTypeName);
                    ItemStack potion = new ItemStack(Material.POTION);
                    PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
                    potionMeta.setBasePotionData(new PotionData(potionType));
                    potion.setItemMeta(potionMeta);
                    recipe.setIngredient(symbol.charAt(0), new RecipeChoice.ExactChoice(potion));
                } catch (IllegalArgumentException ex) {
                    Bukkit.getLogger().warning("[kztitem] 無効なポーションタイプ: " + potionTypeName);
                }
                continue;
            }

            Material mat = Material.matchMaterial(value.toUpperCase());
            if (mat != null) {
                recipe.setIngredient(symbol.charAt(0), mat);
            }
        }

        Bukkit.addRecipe(recipe);
    }

    public static ItemStack getItem(String id) {
        return customItems.get(id);
    }
}
