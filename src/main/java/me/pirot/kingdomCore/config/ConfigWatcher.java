package me.pirot.kingdomCore.config;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Monitors the shops/ directory for file changes and triggers reload in ConfigManager.
 * This allows the WebStore to live-update shop configurations.
 */
public class ConfigWatcher {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final ExecutorService executor;
    private WatchService watchService;

    public ConfigWatcher(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Start watching the shops/ directory.
     */
    public void start() {
        Path shopPath = Paths.get(plugin.getDataFolder().getAbsolutePath(), "shops");
        
        // Ensure directory exists
        if (!Files.exists(shopPath)) {
            try {
                Files.createDirectories(shopPath);
            } catch (IOException e) {
                plugin.getLogger().severe("[KingdomCore] Could not create shops directory for watcher: " + e.getMessage());
                return;
            }
        }

        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            shopPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            
            plugin.getLogger().info("[KingdomCore] Started file watcher for shops/ directory.");

            executor.submit(() -> {
                try {
                    WatchKey key;
                    while ((key = watchService.take()) != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                Path fileName = (Path) event.context();
                                String nameStr = fileName.toString();
                                
                                if (nameStr.endsWith(".yml")) {
                                    String shopKey = nameStr.replace(".yml", "");
                                    
                                    // Delay slightly to ensure file is fully written by the webstore
                                    Thread.sleep(200);
                                    
                                    // Reload on main thread
                                    Bukkit.getScheduler().runTask(plugin, () -> {
                                        configManager.reloadShop(shopKey);
                                    });
                                }
                            }
                        }
                        key.reset();
                    }
                } catch (InterruptedException e) {
                    // Shutting down
                } catch (Exception e) {
                    plugin.getLogger().warning("[KingdomCore] Error in config watcher loop: " + e.getMessage());
                }
            });

        } catch (IOException e) {
            plugin.getLogger().severe("[KingdomCore] Failed to start file watcher: " + e.getMessage());
        }
    }

    /**
     * Stop the watcher.
     */
    public void stop() {
        try {
            if (watchService != null) {
                watchService.close();
            }
            executor.shutdownNow();
        } catch (IOException e) {
            // Ignore
        }
    }
}
