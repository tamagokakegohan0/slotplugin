package tamakake.slotPlugin;

import java.util.Comparator;
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
    private final List<Player> spinningPlayers = new ArrayList<>();

    public SlotListener(SlotPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {

        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = e.getItem();
        Block block = e.getClickedBlock();
        if (block == null) return;

        if (block.getType() != Material.LEVER) return;

        Player player = e.getPlayer();

        if (spinningPlayers.contains(player)) {
            player.sendMessage("§cすでに回っています！");
            return;
        }

        // 手ぶら
        if (item == null || item.getType() == Material.AIR) {
            e.setCancelled(true);
            List<ItemFrame> frames = getFrames(block);
            if (frames.size() != 3) {
                player.sendMessage("§c額縁を3つ設置してください！");
                return;
            }
            startSlot(player, frames);
            return;
        }

        // 🎟️ チケット使用（IRON_NUGGET CMD 76）
        if (isTicket(item)) {
            e.setCancelled(true);
            item.setAmount(item.getAmount() - 1);

            List<ItemFrame> frames = getFrames(block);
            if (frames.size() != 3) {
                player.sendMessage("§c額縁を3つ設置してください！");
                return;
            }

            startTicketSlot(player, frames);
            return;
        }

        // スロット棒
        if (item.getType() == Material.STICK && item.hasItemMeta()
                && item.getItemMeta().hasCustomModelData()
                && item.getItemMeta().getCustomModelData() == 9999) {

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
    public void startSlot(Player player, List<ItemFrame> frames) {

        int cost = 1000;

        if (SlotPlugin.econ.getBalance(player) < cost) {
            player.sendMessage("§cお金が足りません！");
            return;
        }

        SlotPlugin.econ.withdrawPlayer(player, cost);
        player.sendMessage("§e" + cost + "円支払いました");

        List<ItemStack> items = getSlotItems();

        spinningPlayers.add(player);

        new BukkitRunnable() {

            int count = 0;
            Random random = new Random();

            @Override
            public void run() {

                for (ItemFrame frame : frames) {
                    frame.setItem(items.get(random.nextInt(items.size())));
                }

                count++;

                if (count > 15) {
                    cancel();
                    spinningPlayers.remove(player);

                    ItemStack a = frames.get(0).getItem();
                    ItemStack b = frames.get(1).getItem();
                    ItemStack c = frames.get(2).getItem();

                    if (isSame(a, b) && isSame(b, c)) {

                        if (a.getType() == Material.NETHERITE_INGOT) {
                            player.sendMessage("§a大当たり！チケットゲット！");
                            player.getInventory().addItem(getTicket());
                        } else if (a.getType() == Material.DIAMOND) {
                            SlotPlugin.econ.depositPlayer(player, 10000);
                            player.sendMessage("§a1万円ゲット！");
                        } else if (a.getType() == Material.EMERALD) {
                            SlotPlugin.econ.depositPlayer(player, 100000);
                            player.sendMessage("§a10万円ゲット！");
                        } else {
                            player.sendMessage("§a当たりました！");
                        }

                    } else {
                        player.sendMessage("§c外れました。");
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    // 🎟️ チケットスロット
    public void startTicketSlot(Player player, List<ItemFrame> frames) {

        spinningPlayers.add(player);

        new BukkitRunnable() {

            int count = 0;
            Random random = new Random();

            @Override
            public void run() {

                for (int i = 0; i < frames.size(); i++) {
                    frames.get(i).setItem(getTicketSlotItems().get(random.nextInt(3)));
                }

                count++;

                if (count > 20) {
                    cancel();
                    spinningPlayers.remove(player);

                    ItemStack a = frames.get(0).getItem();
                    ItemStack b = frames.get(1).getItem();
                    ItemStack c = frames.get(2).getItem();

                    if (a.getItemMeta().getCustomModelData() == 19 &&
                            b.getItemMeta().getCustomModelData() == 19 &&
                            c.getItemMeta().getCustomModelData() == 19) {

                        SlotPlugin.econ.depositPlayer(player, 1000000);
                        player.sendMessage("§a100万円ゲット！");
                    } else {
                        player.sendMessage("§c外れました。");
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    // 🎟️ チケット作成（←ここが重要）
    public ItemStack getTicket() {
        ItemStack item = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§eスロットチケット");
            meta.setCustomModelData(76); // ←ここ変更
            item.setItemMeta(meta);
        }

        return item;
    }

    // 🎟️ チケット判定
    public boolean isTicket(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != Material.IRON_NUGGET) return false;
        if (!item.hasItemMeta()) return false;
        if (!item.getItemMeta().hasCustomModelData()) return false;
        return item.getItemMeta().getCustomModelData() == 76;
    }

    public boolean isSame(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        return a.isSimilar(b);
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

    private List<ItemStack> getSlotItems() {
        List<ItemStack> items = new ArrayList<>();
        items.add(createItem(Material.COPPER_INGOT, 1));
        items.add(createItem(Material.IRON_INGOT, 1));
        items.add(createItem(Material.GOLD_INGOT, 1));
        items.add(createItem(Material.EMERALD, 1));
        items.add(createItem(Material.DIAMOND, 1));
        items.add(createItem(Material.NETHERITE_INGOT, 1));
        return items;
    }

    private List<ItemStack> getTicketSlotItems() {
        List<ItemStack> items = new ArrayList<>();
        items.add(createItem(Material.IRON_NUGGET, 19));
        items.add(createItem(Material.IRON_NUGGET, 21));
        items.add(createItem(Material.IRON_NUGGET, 22));
        return items;
    }

    private ItemStack createItem(Material material, int cmd) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(cmd);
            item.setItemMeta(meta);
        }
        return item;
    }
}