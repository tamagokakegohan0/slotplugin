package tamakake.slotPlugin;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

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
import org.bukkit.scheduler.BukkitRunnable;

public class SlotListener implements Listener {

    private final SlotPlugin plugin;
    private final Set<Player> playing = new HashSet<>();

    public SlotListener(SlotPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {

        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = e.getClickedBlock();
        if (block == null) return;

        Player player = e.getPlayer();
        ItemStack item = e.getItem();

        // 🎟️ チケット使用（最優先）
        if (isTicket(item)) {
            e.setCancelled(true);
            item.setAmount(item.getAmount() - 1);

            List<ItemFrame> frames = getFrames(block);
            if (frames.size() != 3) {
                player.sendMessage("§c額縁を3つ設置してください！");
                return;
            }

            TicketSlotListener ticketSlot = new TicketSlotListener(plugin);
            ticketSlot.startTicketSlot(player, frames);
            return; // 通常スロットは処理しない
        }

        // ❗ 連打防止
        if (playing.contains(player)) {
            player.sendMessage("§cすでに回っています！");
            return;
        }

        // 🎰 通常スロット（手ぶらでもOK）
        boolean isNormalSlot = false;
        if (item == null) {
            isNormalSlot = true; // 手ぶらで回す場合
        } else if (item.getType() == Material.STICK && item.hasItemMeta() && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 9999) {
            isNormalSlot = true;
        }

        if (isNormalSlot) {
            e.setCancelled(true);

            List<ItemFrame> frames = getFrames(block);
            if (frames.size() != 3) {
                player.sendMessage("§c額縁を3つ設置してください！");
                return;
            }

            startSlot(player, frames);
        }
    }

    // 🎰 通常スロット
    private void startSlot(Player player, List<ItemFrame> frames) {

        int cost = 1000;

        if (SlotPlugin.econ.getBalance(player) < cost) {
            player.sendMessage("§cお金が足りません！");
            return;
        }

        playing.add(player);
        SlotPlugin.econ.withdrawPlayer(player, cost);
        player.sendMessage("§e" + cost + "円支払いました");

        List<ItemStack> items = getSlotItems();
        new BukkitRunnable() {

            int count = 0;
            Random random = new Random();

            @Override
            public void run() {
                // 15回ランダムに回転演出
                for (ItemFrame frame : frames) {
                    frame.setItem(items.get(random.nextInt(items.size())));
                }

                count++;
                if (count > 15) {
                    cancel();

                    // 最終的に揃ったか判定
                    ItemStack a = frames.get(0).getItem();
                    ItemStack b = frames.get(1).getItem();
                    ItemStack c = frames.get(2).getItem();

                    if (isSame(a, b) && isSame(b, c)) {
                        Material type = a.getType();
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
                            player.sendMessage("§c外れました。");
                        }
                    } else {
                        player.sendMessage("§c外れました。");
                    }

                    playing.remove(player);
                }
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    // 🎰 通常スロット用アイテム
    private List<ItemStack> getSlotItems() {
        List<ItemStack> items = new ArrayList<>();
        items.add(createItem(Material.COPPER_INGOT,1));
        items.add(createItem(Material.IRON_INGOT,1));
        items.add(createItem(Material.GOLD_INGOT,1));
        items.add(createItem(Material.EMERALD,1));
        items.add(createItem(Material.DIAMOND,1));
        items.add(createItem(Material.NETHERITE_INGOT,1)); // チケット用
        return items;
    }

    // ItemStack作成（CustomModelData指定）
    private ItemStack createItem(Material material, int cmd) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(cmd);
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

    public boolean isTicket(ItemStack item) {
        if (item == null) return false;
        if (!item.hasItemMeta()) return false;
        if (!item.getItemMeta().hasCustomModelData()) return false;
        return item.getItemMeta().getCustomModelData() == 7777;
    }

    // ItemStack同士が同じか判定
    public boolean isSame(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        return a.isSimilar(b);
    }

    // ブロック周囲のItemFrame取得
    public List<ItemFrame> getFrames(Block block) {
        List<ItemFrame> frames = new ArrayList<>();
        for (Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), 3, 3, 3)) {
            if (entity instanceof ItemFrame) frames.add((ItemFrame) entity);
        }
        frames.sort(Comparator.comparingDouble(f -> f.getLocation().getX()));
        return frames;
    }
}