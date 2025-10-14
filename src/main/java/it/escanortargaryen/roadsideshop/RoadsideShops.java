package it.escanortargaryen.roadsideshop;

import it.escanortargaryen.roadsideshop.classes.LockedSlot;
import it.escanortargaryen.roadsideshop.classes.LockedSlotCheck;
import it.escanortargaryen.roadsideshop.classes.Shop;
import it.escanortargaryen.roadsideshop.db.DatabaseManager;
import it.escanortargaryen.roadsideshop.managers.Commands;
import it.escanortargaryen.roadsideshop.managers.LibbyManager;
import it.escanortargaryen.roadsideshop.managers.ShopsManager;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static it.escanortargaryen.roadsideshop.InternalUtil.CONFIGMANAGER;

public class RoadsideShops extends JavaPlugin implements Listener {

    public static RoadsideShops INSTANCE;

    private static Economy econ = null;
    private static DatabaseManager databaseManager = null;

    private boolean test = false;

    public static Economy getEconomy() {
        return econ;
    }

    private Commands commandAPI;
    private LibbyManager libbyManager;

    /**
     * All custom locked slots.
     */
    private final ArrayList<LockedSlot> lockedSlots = new ArrayList<>();

    @Override
    public void onLoad() {
        INSTANCE = this;
        saveResource("config.yml", false);
        new InternalUtil();
        libbyManager = new LibbyManager(this);
        commandAPI = new Commands();

        if (!test)
            commandAPI.onLoad(this);

    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return true;
    }

    public static ArrayList<Shop> getAllShops() {
        return databaseManager.getAllShops();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        CompletableFuture.runAsync(() -> {
            databaseManager.addPlayer(p);

            Shop shop = getShop(p, true);
            new BukkitRunnable() {

                @Override
                public void run() {

                    if (shop != null) {

                        if (shop.getOffMessages().size() > 0) {
                            p.sendMessage(CONFIGMANAGER.getWhileOffline());
                            for (String s : shop.getOffMessages()) {

                                p.sendMessage(s);
                            }

                            shop.clearMessages();

                        }

                    }

                }
            }.runTask(RoadsideShops.INSTANCE);

        });

    }

    @Override
    public void onDisable() {
        super.onDisable();
        try {
            databaseManager.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getServer().getLogger().info("Disabled due to no Vault economy found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Metrics metrics = null;
        if (!test) {
            commandAPI.onEnable();

            //setup bstat
            int pluginId = 17468;
            metrics = new Metrics(this, pluginId);

        }

        Bukkit.getPluginManager().registerEvents(this, this);


        new ShopsManager(metrics);

        try {
            databaseManager = new DatabaseManager(new File(getDataFolder() + "/database.db"), metrics);
        } catch (Exception e) {
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(RoadsideShops.INSTANCE);
            return;
        }

        String s = "§7----§c§nRoadside§r §6§nShops§r§7----§r\n§fby §eEscanorTargaryen§r\n§2Enabled version: " + this.getDescription().getVersion() + "§r\n§7-----------------------§r";
        Bukkit.getConsoleSender().sendMessage(s);

    }

    /**
     * @see DatabaseManager#hasShop(UUID)
     */
    public static boolean hasShop(@NotNull UUID player) {

        return databaseManager.hasShop(player);

    }

    /**
     * @see DatabaseManager#updateShop(Shop)
     */
    public static void saveShop(@NotNull Shop shop) {
        databaseManager.updateShop(shop);

    }

    /**
     * Register a new custom locked slot.
     *
     * @param itemStack       The icon of the locked slot.
     * @param lockedSlotCheck The condition of when the slot is blocked.
     */
    public static void registerCustomLockedSlot(@NotNull ItemStack itemStack, @NotNull LockedSlotCheck lockedSlotCheck) {

        Objects.requireNonNull(itemStack);
        Objects.requireNonNull(lockedSlotCheck);

        INSTANCE.lockedSlots.add(new LockedSlot(itemStack, lockedSlotCheck));

    }

    public ArrayList<LockedSlot> getCustomLockedSlots() {
        return new ArrayList<>(lockedSlots);
    }

    /**
     * @see DatabaseManager#getShop(UUID, boolean)
     */
    public static Shop getShop(@NotNull Player player) {
        Objects.requireNonNull(player);
        return getShop(player.getUniqueId(), true);
    }

    /**
     * @see DatabaseManager#getShop(UUID, boolean)
     */
    public static Shop getShop(@NotNull Player player, boolean saveInCache) {
        Objects.requireNonNull(player);
        return getShop(player.getUniqueId(), saveInCache);
    }

    /**
     * @see DatabaseManager#getShop(UUID, boolean)
     */
    public static Shop getShop(@NotNull UUID player, boolean saveInCache) {
        Objects.requireNonNull(player);
        return databaseManager.getShop(player, saveInCache);
    }

    /**
     * @see DatabaseManager#getShop(UUID, boolean)
     */
    public static Shop getShop(@NotNull UUID player) {
        Objects.requireNonNull(player);
        return databaseManager.getShop(player, true);
    }

    public RoadsideShops() {

    }

    protected RoadsideShops(JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
        test = true;
    }


}
