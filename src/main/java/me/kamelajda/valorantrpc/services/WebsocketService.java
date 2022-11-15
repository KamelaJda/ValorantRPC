package me.kamelajda.valorantrpc.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import me.kamelajda.valorantrpc.ValorantRPCApplication;
import me.kamelajda.valorantrpc.dto.LockFileData;
import me.kamelajda.valorantrpc.dto.SocketData;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@Order(value = Ordered.HIGHEST_PRECEDENCE + 3)
public class WebsocketService {

    private final DiscordService discordService;
    private final LockFileService lockFileService;

    public WebsocketService(LockFileService lockFileService, DiscordService discordService) {
        this.discordService = discordService;
        this.lockFileService = lockFileService;

        try {
            tryConnect().whenCompleteAsync((value, throwable) -> {
                if (throwable != null) {
                    log.error("Error in tryConnect() return value!", throwable);
                    return;
                }
                if (!value) log.info("tryConnect() return value={}", false);
            });
        } catch (Exception e) {
            log.error("Error!", e);
        }
    }

    private CompletableFuture<Boolean> tryConnect() throws IOException, InterruptedException {
        while (!lockFileService.isLockFileExist()) {
            Thread.sleep(1_000);
        }
        LockFileData fileData = lockFileService.retrieveLockFileData();

        try {
            JsonObject helpData = null;

            while (helpData == null) {
                try {
                    helpData = lockFileService.retrieveHelpData();
                } catch (ConnectException ignored) {
                    try {
                        Thread.sleep(1_000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } catch (Exception e) {
                    log.error("Error!", e);
                    return CompletableFuture.completedFuture(false);
                }
            }

            WebSocketClient webSocketClient = new WebSocketClient(new URI(String.format("wss://127.0.0.1:%s", fileData.getPort())), fileData.getBase64Password(), helpData);
            try {
                final SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, ValorantRPCApplication.getTrustManager(), new java.security.SecureRandom());
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                SSLEngine engine = sslContext.createSSLEngine();
                List<String> ciphers = new ArrayList<>(Arrays.asList(engine.getEnabledCipherSuites()));
                ciphers.remove("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
                List<String> protocols = new ArrayList<>(Arrays.asList(engine.getEnabledProtocols()));
                protocols.remove("SSLv3");

                webSocketClient.setSocketFactory(sslSocketFactory);
            } catch (Exception e) {
                e.printStackTrace();
            }
            webSocketClient.connect();
            return CompletableFuture.completedFuture(true);
        } catch (URISyntaxException e) {
            log.error("Error!", e);
        }

        return CompletableFuture.completedFuture(false);
    }

    private class WebSocketClient extends org.java_websocket.client.WebSocketClient {
        private final Gson gson = new Gson();
        private final JsonObject helpData;
        public WebSocketClient(URI serverUri, String auth, JsonObject helpData) {
            super(serverUri, Map.of("Authorization", "Basic " + auth));
            this.helpData = helpData;
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            log.debug("WebSocket connected! msg={} code={} content={}", handshakedata.getHttpStatusMessage(), handshakedata.getHttpStatus(), handshakedata.getContent());
            JsonObject events = helpData.get("events").getAsJsonObject();

            for (String key : events.keySet()) {
                if (key.isBlank() || key.isEmpty()) continue;
                JsonArray array = new JsonArray();
                array.add(5);
                array.add(key);
                send(array.toString());
            }
        }

        @Override
        public void onMessage(String message) {
            if (message.isEmpty() || message.isBlank()) return;

            JsonArray ja = gson.fromJson(message, JsonArray.class);
            JsonObject data = ja.get(2).getAsJsonObject();

            String privateData = getPrivateData(data);
            if (privateData == null || !data.isJsonObject()) return;

            data.getAsJsonObject("data").getAsJsonArray("presences").get(0).getAsJsonObject().add("private", gson.fromJson(privateData, JsonObject.class));

            SocketData socketData = gson.fromJson(data.getAsJsonObject("data").getAsJsonArray("presences").get(0).getAsJsonObject(), SocketData.class);

            try {
                if (!socketData.getPid().equals(lockFileService.getSession().getPid())) {
                    return;
                }
            } catch (Exception e) {
                log.error("Error!", e);
                return;
            }

            discordService.tryApply(socketData);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            discordService.updateRP(null);
            log.info("WebSocket closed! reason={}, remote={}, code={}", reason, remote, code);

            try {
                tryConnect().whenCompleteAsync((value, throwable) -> {
                    if (throwable != null) {
                        log.error("Error in tryConnect() return value!", throwable);
                        return;
                    }
                    if (value) discordService.tryApply(null);
                    else log.info("tryConnect() return value={}", false);
                });
            } catch (Exception e) {
                log.error("Error!", e);
            }
        }

        @Override
        public void onError(Exception ex) {
            log.error("WebSocket error!", ex);
        }

        @Nullable
        private String getPrivateData(JsonObject data) {
            if (!data.get("data").isJsonObject()) return null;

            JsonObject data1 = data.getAsJsonObject("data");
            if (data1.has("presences")) {
                JsonArray presences = data1.getAsJsonArray("presences");
                if (presences.size() > 0 && presences.get(0).isJsonObject() && presences.get(0).getAsJsonObject().has("private")) {
                    JsonElement aPrivate = presences.get(0).getAsJsonObject().get("private");
                    if (aPrivate.isJsonNull() || aPrivate.isJsonObject()) return null;

                    try {
                        return new String(Base64.getDecoder().decode(aPrivate.getAsString()), StandardCharsets.UTF_8);
                    } catch (Exception ignored) {
                        return null;
                    }

                }
            }

            return null;
        }

    }

}
