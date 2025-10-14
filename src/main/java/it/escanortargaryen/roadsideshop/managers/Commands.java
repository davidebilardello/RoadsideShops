package it.escanortargaryen.roadsideshop.managers;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.AsyncPlayerProfileArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import it.escanortargaryen.roadsideshop.InternalUtil;
import it.escanortargaryen.roadsideshop.RoadsideShops;
import it.escanortargaryen.roadsideshop.classes.Newspaper;
import it.escanortargaryen.roadsideshop.classes.Shop;
import it.escanortargaryen.roadsideshop.classes.ViewMode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Command recorder.
 */
public class Commands {

    private static final String SHOPCOMMAND_PERMISSION = "roadsideshops.shopcommand";
    private static final String ADMIN_EDITSHOPS_PERMISSION = "roadsideshops.admin.editshops";
    private static final String NEWSPAPER_PERMISSION = "roadsideshops.newspapercommand";

    private Plugin plugin;

    public void enableCommands() {
        enableNewsPaperCommand();
        enableRoadSideCommand();
    }

    public void onLoad(Plugin plugin) {

        this.plugin = plugin;
        // Choose the right CommandAPI's class config
        CommandAPIBukkitConfig<?> config;
        try {
            if (LibbyManager.isMojangMapped()) {
                Class<?> lifecycleEventOwnerClass = Class.forName("io.papermc.paper.plugin.lifecycle.event.LifecycleEventOwner");
                Class<?> paperConfigClass = Class.forName("dev.jorel.commandapi.CommandAPIPaperConfig");
                config = (CommandAPIBukkitConfig<?>) paperConfigClass.getDeclaredConstructor(lifecycleEventOwnerClass).newInstance(plugin);
            } else {
                Class<?> spigotConfigClass = Class.forName("dev.jorel.commandapi.CommandAPISpigotConfig");
                config = (CommandAPIBukkitConfig<?>) spigotConfigClass.getDeclaredConstructor(JavaPlugin.class).newInstance(plugin);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot create CommandAPI config", e);
        }

        CommandAPI.onLoad(config
                .verboseOutput(false)
                .silentLogs(true)
                .setNamespace(plugin.getName().toLowerCase(Locale.ENGLISH)) // Plugin names contain only latin characters present in english
        );

        enableCommands();
    }

    public void onEnable() {
        CommandAPI.onEnable();
    }

    public void checkPermission(Player player, String permission) throws WrapperCommandSyntaxException {
        if (!player.hasPermission(permission))
            throw CommandAPI.failWithString("Permission denied.");
    }

    public String getUniqueIdMethodName() {
        return LibbyManager.isMojangMapped() ? "getId" : "getUniqueId";
    }

    public void manageAsyncPlayerProfileArgument(CompletableFuture<List<?>> profiles, Player sender, Consumer<OfflinePlayer> consumer) {
        profiles.thenAccept(profileList -> {
            Object pl = profileList.getFirst();
            new BukkitRunnable() {

                @Override
                public void run() {
                    try {
                        OfflinePlayer op = Bukkit.getOfflinePlayer((UUID) pl.getClass().getDeclaredMethod(getUniqueIdMethodName()).invoke(pl));
                        consumer.accept(op);
                    } catch (ReflectiveOperationException e) {
                        sender.sendMessage(ChatColor.RED + "Error while trying to execute the command.");
                        throw new RuntimeException(e);
                    }

                }
            }.runTask(plugin);

        }).exceptionally(throwable -> {
            sender.sendMessage(InternalUtil.CONFIGMANAGER.getNoShop());
            return null;
        });

    }


    private void enableRoadSideCommand() {

        new CommandAPICommand(ConfigManager.SHOPCOMMAND).withRequirement((e) -> e.hasPermission(SHOPCOMMAND_PERMISSION)).executesPlayer((p, objects) -> {
            checkPermission(p, SHOPCOMMAND_PERMISSION);
            openPersonalShop(p);
        }).register();

        new CommandAPICommand(ConfigManager.SHOPCOMMAND).withRequirement((e) -> e.hasPermission(SHOPCOMMAND_PERMISSION)).withArguments(new AsyncPlayerProfileArgument("shopOwner")).executesPlayer((p, objects) -> {
            checkPermission(p, SHOPCOMMAND_PERMISSION);
            manageAsyncPlayerProfileArgument((CompletableFuture<List<?>>) objects.get(0), p, shopOwner -> {
                if (shopOwner != null) {
                    openPlayerShop(p, shopOwner);
                } else {
                    p.sendMessage(InternalUtil.CONFIGMANAGER.getNoShop());
                }
            });

        }).register();

        new CommandAPICommand("roadsideshopsadmin").withRequirement((e) -> e.hasPermission(ADMIN_EDITSHOPS_PERMISSION)).withArguments(new AsyncPlayerProfileArgument("shopOwner")).executesPlayer((p, objects) -> {
            checkPermission(p, ADMIN_EDITSHOPS_PERMISSION);

            manageAsyncPlayerProfileArgument((CompletableFuture<List<?>>) objects.get(0), p, shopOwner -> {
                if (shopOwner != null) {
                    openPlayerShopAsSeller(p, shopOwner);
                } else {
                    p.sendMessage(InternalUtil.CONFIGMANAGER.getNoShop());
                }
            });

        }).register();
    }

    private void enableNewsPaperCommand() {

        new CommandAPICommand(ConfigManager.NEWSPAPERCOMMAND).withRequirement((e) -> e.hasPermission(NEWSPAPER_PERMISSION)).executesPlayer((player, objects) -> {
            checkPermission(player, NEWSPAPER_PERMISSION);
            CompletableFuture.runAsync(() -> {
                ArrayList<Shop> shops = RoadsideShops.getAllShops();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        new Newspaper(shops, player);
                    }
                }.runTask(RoadsideShops.INSTANCE);
            });
        }).register();

    }

    public static void openPersonalShop(Player p) {
        CompletableFuture.runAsync(() -> {
            if (!RoadsideShops.hasShop(p.getUniqueId())) {
                RoadsideShops.getShop(p);
            }
            Shop s = RoadsideShops.getShop(p);
            new BukkitRunnable() {

                @Override
                public void run() {
                    s.openInventory(p, ViewMode.SELLER);
                }
            }.runTask(RoadsideShops.INSTANCE);
        });
    }

    public static void openPlayerShop(Player p, OfflinePlayer shopOwner) {
        CompletableFuture.runAsync(() -> {
            if (!RoadsideShops.hasShop(shopOwner.getUniqueId())) {
                new BukkitRunnable() {

                    @Override
                    public void run() {
                        p.sendMessage(InternalUtil.CONFIGMANAGER.getNoShop());
                    }
                }.runTask(RoadsideShops.INSTANCE);
            } else {
                if (p.getUniqueId().equals(shopOwner.getUniqueId())) {

                    Shop s = RoadsideShops.getShop(shopOwner.getUniqueId());
                    new BukkitRunnable() {

                        @Override
                        public void run() {
                            s.openInventory(p, ViewMode.SELLER);
                        }
                    }.runTask(RoadsideShops.INSTANCE);
                } else {
                    Shop s = RoadsideShops.getShop(shopOwner.getUniqueId());
                    new BukkitRunnable() {

                        @Override
                        public void run() {
                            s.openInventory(p, ViewMode.BUYER);

                        }
                    }.runTask(RoadsideShops.INSTANCE);
                }

            }

        });
    }

    public static void openPlayerShopAsSeller(Player p, OfflinePlayer shopOwner) {
        CompletableFuture.runAsync(() -> {
            if (!RoadsideShops.hasShop(shopOwner.getUniqueId())) {
                new BukkitRunnable() {

                    @Override
                    public void run() {
                        p.sendMessage(InternalUtil.CONFIGMANAGER.getNoShop());
                    }
                }.runTask(RoadsideShops.INSTANCE);

            } else {

                Shop s = RoadsideShops.getShop(shopOwner.getUniqueId());
                new BukkitRunnable() {

                    @Override
                    public void run() {
                        s.openInventory(p, ViewMode.SELLER);
                    }
                }.runTask(RoadsideShops.INSTANCE);

            }

        });
    }

}
