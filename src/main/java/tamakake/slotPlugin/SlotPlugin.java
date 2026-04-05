package tamakake.slotPlugin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class SlotPlugin extends JavaPlugin {

    public static Economy econ = null;

    @Override
    public void onEnable() {
        getLogger().info("SlotPlugin 起動！");

        // Vaultチェック
        if (!setupEconomy()) {
            getLogger().severe("Vaultが見つかりません！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // リスナー登録
        getServer().getPluginManager().registerEvents(new SlotListener(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("SlotPlugin 停止！");
    }

    // Vaultセットアップ
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            return false;
        }

        econ = rsp.getProvider();
        return econ != null;
    }

    // コマンド処理
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("giveslot")) {

            if (!(sender instanceof Player)) return true;

            Player player = (Player) sender;

            // 権限チェック
            if (!player.hasPermission("tamakake.slot.op")) {
                player.sendMessage("§c権限がありません！");
                return true;
            }

            // スロット設置ツール作成
            ItemStack item = new ItemStack(Material.STICK);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName("§6スロット設置ツール");
                meta.setCustomModelData(9999); // ←超重要
                item.setItemMeta(meta);
            }

            player.getInventory().addItem(item);
            player.sendMessage("§aスロットアイテムを取得！");

            return true;
        }

        return false;
    }
}