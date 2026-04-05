package tamakake.slotPlugin;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.ItemFrame;

import java.util.List;
import java.util.Random;

public class TicketSlotListener {

    private final SlotPlugin plugin;

    public TicketSlotListener(SlotPlugin plugin) {
        this.plugin = plugin;
    }

    // 🎟️ チケットスロット開始
    public void startTicketSlot(Player player, List<ItemFrame> frames) {

        Random random = new Random();

        // 🎯 50%で当たり
        boolean win = random.nextDouble() < 0.5;

        // 🎯 当たり用アイテム（CMD:19）
        ItemStack winItem = createItem(19);

        // ハズレ用
        ItemStack item21 = createItem(21);
        ItemStack item22 = createItem(22);

        new BukkitRunnable() {

            int count = 0;

            @Override
            public void run() {

                // 🎰 回転演出
                for (ItemFrame frame : frames) {
                    frame.setItem(randomItem(random, winItem, item21, item22));
                }

                count++;

                // 🎬 左停止
                if (count == 10) {
                    frames.get(0).setItem(win ? winItem : randomItem(random, winItem, item21, item22));
                }

                // 🎬 右停止
                if (count == 15) {
                    frames.get(2).setItem(win ? winItem : randomItem(random, winItem, item21, item22));
                }

                // 🎬 真ん中停止（結果確定）
                if (count == 20) {
                    cancel();

                    if (win) {
                        // 🎉 全部揃える
                        for (ItemFrame frame : frames) {
                            frame.setItem(winItem);
                        }

                        player.sendMessage("§a大当たり！100万円ゲット！");
                        SlotPlugin.econ.depositPlayer(player, 1000000);

                    } else {
                        // ❌ ハズレ（バラバラ）
                        frames.get(1).setItem(randomItem(random, item21, item22));

                        player.sendMessage("§c外れました。");
                    }
                }

            }
        }.runTaskTimer(plugin, 0, 2);
    }

    // 🎲 ランダム（3種類）
    private ItemStack randomItem(Random random, ItemStack... items) {
        return items[random.nextInt(items.length)];
    }

    // 🎨 iron_nugget + CustomModelData
    private ItemStack createItem(int cmd) {

        ItemStack item = new ItemStack(Material.IRON_NUGGET);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setCustomModelData(cmd);
            item.setItemMeta(meta);
        }

        return item;
    }
}