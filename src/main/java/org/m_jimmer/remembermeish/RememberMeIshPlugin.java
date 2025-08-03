package org.m_jimmer.remembermeish;

import com.google.inject.Inject;

import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

@Plugin(
        id = "remembermeish",
        name = "RememberMe'ish",
        version = "1.0.0",
        description = "Send the player to the last known server and spot if enabled on the proxy server" ,
        url = "https://github.com/JosephMillsAtWork/RememberMeIsh/tree/main",
        authors = {"m_jimmer"}
)

public class RememberMeIshPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path configDir;
    private RememberMeIshStorage storage;

    @Inject
    public RememberMeIshPlugin(
            ProxyServer server,
            Logger logger,
            @DataDirectory Path dataDirectory
    ) {
        this.server = server;
        this.logger = logger;
        this.configDir = dataDirectory;
        this.storage = new RememberMeIshStorage(dataDirectory, logger);
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        storage = new RememberMeIshStorage(configDir, logger);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        player.getCurrentServer().ifPresent(serverConn -> {
            String serverName = serverConn.getServerInfo().getName();
            if (storage.isEnabled(serverName)) {
                storage.savePlayerServer(player.getUniqueId(), serverName);
            } else {
                storage.clearPlayerServer(player.getUniqueId());
            }
        });
    }

    @Subscribe
    public void onPreConnect(ServerPreConnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Optional<String> remembered = storage.getPlayerServer(uuid);
        remembered.flatMap(server::getServer).ifPresent(server -> {
            event.setResult(ServerPreConnectEvent.ServerResult.allowed(server));
            logger.info("[RememberMe'ish] Redirecting {} to remembered server {}",
                    event.getPlayer().getUsername(),
                    server.getServerInfo().getName());
        });
    }
}
