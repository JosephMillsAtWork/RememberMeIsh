package org.m_jimmer.remembermeish;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import org.slf4j.Logger;



import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class RememberMeIshStorage {
    private static final String CONFIG_FILE = "remembermeish.json";

    private final Logger logger;
    private final Path configFile;
    private final Gson gson;

    private final Set<String> enabled = new HashSet<>();
    private final Set<String> disabled = new HashSet<>();
    private final Map<UUID, String> rememberedServers = new HashMap<>();

    public RememberMeIshStorage(Path configDir, Logger logger) {


        this.logger = logger;

        this.configFile = configDir.resolve(CONFIG_FILE);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadConfig();
    }

    public boolean isEnabled(String serverName) {
        return enabled.contains(serverName);
    }

    public void savePlayerServer(UUID uuid, String server) {
        rememberedServers.put(uuid, server);
        saveConfig();
    }

    public void clearPlayerServer(UUID uuid) {
        if (rememberedServers.remove(uuid) != null) {
            saveConfig();
        }
    }

    public Optional<String> getPlayerServer(UUID uuid) {
        return Optional.ofNullable(rememberedServers.get(uuid));
    }

    private void loadConfig() {
        if (!Files.exists(configFile)) {
            logger.warn("[RememberMe'ish] config Not found, creating default.");
            enabled.add("creative");
            enabled.add("survival");
            disabled.add("lobby");
            saveConfig();
            return;
        }

        try (Reader reader = Files.newBufferedReader(configFile)) {
            StorageConfig config = gson.fromJson(reader, StorageConfig.class);
            enabled.clear();
            disabled.clear();
            rememberedServers.clear();
            if (config.enabled != null) enabled.addAll(config.enabled);
            if (config.disabled != null) disabled.addAll(config.disabled);
            if (config.remembered_servers != null) rememberedServers.putAll(config.remembered_servers);
        } catch (IOException | JsonSyntaxException e) {
            logger.error("[RememberMe'ish] Failed to load remembermeish.json", e);
        }
    }

    private void saveConfig() {
        StorageConfig config = new StorageConfig();
        config.enabled = new ArrayList<>(enabled);
        config.disabled = new ArrayList<>(disabled);
        config.remembered_servers = new HashMap<>(rememberedServers);

        try {
            Files.createDirectories(configFile.getParent());
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                gson.toJson(config, writer);
            }
        } catch (IOException e) {
            logger.error("[RememberMe'ish] Save Failed for config ", e);
        }
    }

    private static class StorageConfig {
        public List<String> enabled;
        public List<String> disabled;
        public Map<UUID, String> remembered_servers;
    }
}
