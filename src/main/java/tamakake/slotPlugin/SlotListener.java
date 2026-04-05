package tamakake.slotPlugin;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.Random;

public class SlotListener implements Listener {

    private final SlotPlugin plugin;

    // 🎯 回転中プレイヤー管理
    private final Set<Player> playing = new HashSet<>();

    public SlotListener(SlotPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {

        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = e.getClickedBlock();
        if (block == null) return;

        if (block.getType() != Material.LEVER) return;

        Player player = e.getPlayer();

        e.setCancelled(true);

        // ❗ 連打防止
        if (playing.contains(player)) {
            player.sendMessage("§cすでに回っています！");
            return;
        }

        List<ItemFrame> frames = getFrames(block);

        if (frames.size() != 3) {
            player.sendMessage("§c額縁を3つ設置してください！");
            return;
        }

        startSlot(player, frames);
    }

    // 🎰 スロット開始
    public void startSlot(Player player, List<ItemFrame> frames) {

        int cost = 1000;

        if (SlotPlugin.econ.getBalance(player) < cost) {
            player.sendMessage("§cお金が足りません！");
            return;
        }

        // 🎯 回転中に追加
        playing.add(player);

        SlotPlugin.econ.withdrawPlayer(player, cost);
        player.sendMessage("§e" + cost + "円支払いました");

        List<ItemStack> items = getSlotItems();

        new BukkitRunnable() {

            int count = 0;
            Random random = new Random();

            boolean win = random.nextDouble() < 0.05;

            ItemStack winItem = null;

            {
                if (win) {
                    double r = random.nextDouble();

                    if (r < 0.05) {
                        winItem = createItem(Material.NETHERITE_INGOT);
                    } else if (r < 0.15) {
                        winItem = createItem(Material.EMERALD);
                    } else if (r < 0.45) {
                        winItem = createItem(Material.DIAMOND);
                    } else {
                        winItem = items.get(random.nextInt(items.size()));
                    }
                }
            }

            @Override
            public void run() {

                for (ItemFrame frame : frames) {
                    ItemStack randomItem = items.get(random.nextInt(items.size()));
                    frame.setItem(randomItem);
                }

                count++;

                if (count > 15) {
                    cancel();

                    if (win && winItem != null) {

                        for (ItemFrame frame : frames) {
                            frame.setItem(winItem);
                        }

                        Material type = winItem.getType();

                        if (type == Material.EMERALD) {
                            player.sendMessage("§aエメラルド揃い！10万円ゲット！");
                            SlotPlugin.econ.depositPlayer(player, 100000);

                        } else if (type == Material.DIAMOND) {
                            player.sendMessage("§bダイヤ揃い！1万円ゲット！");
                            SlotPlugin.econ.depositPlayer(player, 10000);

                        } else if (type == Material.NETHERITE_INGOT) {
                            player.sendMessage("§5ネザライト揃い！チケットゲット！");
                            player.getInventory().addItem(getTicket());

                        } else {
                            player.sendMessage("§e揃ったけどハズレ！");
                        }

                    } else {
                        player.sendMessage("§c外れました。");
                    }

                    // 🎯 回転終了 → 解放
                    playing.remove(player);
                }
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    // 🎰 アイテム
    public List<ItemStack> getSlotItems() {

        List<ItemStack> items = new ArrayList<>();

        items.add(createItem(Material.COPPER_INGOT));
        items.add(createItem(Material.IRON_INGOT));
        items.add(createItem(Material.GOLD_INGOT));
        items.add(createItem(Material.EMERALD));
        items.add(createItem(Material.DIAMOND));
        items.add(createItem(Material.NETHERITE_INGOT));

        return items;
    }

    public ItemStack createItem(Material material) {

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setCustomModelData(1);
            item.setItemMeta(meta);
        }

        return item;
    }

    public ItemStack getTicket() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§eスロットチケット");
            meta.setCustomModelData(7777);
            item.setItemMeta(meta);
        }

        return item;
    }

    public List<ItemFrame> getFrames(Block block) {

        List<ItemFrame> frames = new ArrayList<>();

        for (Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), 3, 3, 3)) {
            if (entity instanceof ItemFrame) {
                frames.add((ItemFrame) entity);
            }
        }

        frames.sort(Comparator.comparingDouble(f -> f.getLocation().getX()));

        return frames;
    }
}