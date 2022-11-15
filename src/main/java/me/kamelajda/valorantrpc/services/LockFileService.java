package me.kamelajda.valorantrpc.services;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.kamelajda.valorantrpc.dto.ClientSession;
import me.kamelajda.valorantrpc.dto.ContentData;
import me.kamelajda.valorantrpc.dto.LockFileData;
import me.kamelajda.valorantrpc.utils.Agent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.logging.log4j.util.Base64Util;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@Order(value = Ordered.HIGHEST_PRECEDENCE + 2)
public class LockFileService {

    private static final String CLIENT_PLATFORM = "ew0KCSJwbGF0Zm9ybVR5cGUiOiAiUEMiLA0KCSJwbGF0Zm9ybU9TIjogIldpbmRvd3MiLA0KCSJwbGF0Zm9ybU9TVmVyc2lvbiI6ICIxMC4wLjE5MDQyLjEuMjU2LjY0Yml0IiwNCgkicGxhdGZvcm1DaGlwc2V0IjogIlVua25vd24iDQp9";
    private static final String GLZ_URL = "https://glz-{region}-1.{shard}.a.pvp.net";

    private static final String FOLDER_LOCATION =
        File.separatorChar + "Riot Games" + File.separatorChar + "Riot Client" + File.separatorChar + "Config" + File.separatorChar;

    private final Gson gson = new Gson();
    private final OkHttpClient okHttpClient;
    private final File lockFile;

    private final String currentApiVersion;

    @Getter
    private final ContentData contentData = new ContentData();

    public LockFileService(Environment env, OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
        this.lockFile = new File(env.getProperty("LOCALAPPDATA") + FOLDER_LOCATION + "lockfile");
        this.currentApiVersion = retrieveCurrentApiVersion();

        configureAgents();

        checkLockFile();
    }

    @Scheduled(fixedDelay = 1_000L)
    public void checkLockFile() {
        boolean exist = isLockFileExist();

        if (!exist) log.info("Waiting for Valorant Lock File...");

        while (!isLockFileExist()) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (!exist) log.info("Valorant Lock File found!");
    }

    private void configureAgents() {
        Map<UUID, Agent> agents = new HashMap<>();

        for (JsonElement datum : retrieveContentData("agents")) {
            JsonObject object = datum.getAsJsonObject();

            UUID uuid = UUID.fromString(object.get("uuid").getAsString());
            Agent agent = Agent.findForKey(object.get("displayName").getAsString());

            agents.put(uuid, agent);
        }

        this.contentData.setAgents(agents);
    }

    public boolean isLockFileExist() {
        return lockFile.exists();
    }

    public LockFileData retrieveLockFileData() throws IOException {
        if (!isLockFileExist()) throw new IllegalStateException("Lock File doesn't exist!");

        String[] fileData = Files.readString(Path.of(lockFile.getAbsolutePath())).split(":");

        return new LockFileData(fileData[0], fileData[1], Integer.parseInt(fileData[2]), Base64Util.encode("riot:" + fileData[3]), fileData[3], fileData[4]);
    }

    @Nullable
    public ClientSession getSession() throws IOException {
        LockFileData fileData = retrieveLockFileData();

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
        builder.url(String.format("https://127.0.0.1:%s/chat/v1/session", fileData.getPort()));
        builder.addHeader("Authorization", "Basic " + fileData.getBase64Password());

        Request request = builder.build();

        try (Response call = okHttpClient.newCall(request).execute()) {
            return gson.fromJson(call.body().string(), ClientSession.class);
        } catch (Exception e) {
            log.error("Error", e);
        }

        return null;
    }

    @Nullable
    public JsonObject retrieveHelpData() throws IOException {
        LockFileData fileData = retrieveLockFileData();

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
            .url(String.format("https://127.0.0.1:%s/help", fileData.getPort()))
            .addHeader("Authorization", "Basic " + fileData.getBase64Password());

        try (Response call = okHttpClient.newCall(builder.build()).execute()) {
            return gson.fromJson(call.body().string(), JsonObject.class);
        } catch (Exception e) {
            log.info("Error", e);
        }

        return null;
    }

    @Nullable
    public String retrieveCurrentApiVersion() {
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder().url("https://valorant-api.com/v1/version");

        try (Response call = okHttpClient.newCall(builder.build()).execute()) {
            StringBuilder version = new StringBuilder();
            JsonObject jsonObject = gson.fromJson(call.body().string(), JsonObject.class).getAsJsonObject("data");

            version.append(jsonObject.get("branch").getAsString());
            version.append("-shipping-");
            version.append(jsonObject.get("buildVersion").getAsString());
            version.append("-");
            version.append(jsonObject.get("version").getAsString().split("\\.")[3]);

            return version.toString();
        } catch (Exception e) {
            log.info("Error", e);
        }

        return null;
    }

    @Nullable
    private String getRiotEntitlementToken() throws IOException {
        String url = "https://entitlements.auth.riotgames.com/api/token/v1";

        JsonObject entitlements = retrieveEntitlementsAccessToken();

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
            .url(url)
            .method("POST", RequestBody.create(new byte[]{}))
            .addHeader("Authorization", "Bearer " + entitlements.get("accessToken").getAsString())
            .addHeader("X-Riot-Entitlements-JWT",  entitlements.get("token").getAsString())
            .addHeader("X-Riot-ClientPlatform", CLIENT_PLATFORM)
            .addHeader("X-Riot-ClientVersion", currentApiVersion)
            .addHeader("Content-Type", "application/json")
            ;

        try (Response call = okHttpClient.newCall(builder.build()).execute()) {
            return gson.fromJson(call.body().string(), JsonObject.class).get("entitlements_token").getAsString();
        } catch (Exception e) {
            log.info("Error", e);
        }

        return null;
    }

    @Nullable
    public JsonObject retrieveEntitlementsAccessToken() throws IOException {
        LockFileData fileData = retrieveLockFileData();

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
            .url(String.format("https://127.0.0.1:%s/entitlements/v1/token", fileData.getPort()))
            .addHeader("Authorization", "Basic " + fileData.getBase64Password());

        try (Response call = okHttpClient.newCall(builder.build()).execute()) {
            return gson.fromJson(call.body().string(), JsonObject.class);
        } catch (Exception e) {
            log.info("Error", e);
        }

        return null;
    }

    @Nullable
    public JsonObject retrieveCoreGame() throws IOException {
        String session = getSession().getRegion();
        String region = session.substring(0, session.length() - 1);
        String url = GLZ_URL.replace("{region}", region).replace("{shard}", region);

        JsonObject entitlements = retrieveEntitlementsAccessToken();

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
            .url(url + "/core-game/v1/players/" + getSession().getPuuid())
            .addHeader("Authorization", "Bearer " + entitlements.get("accessToken").getAsString())
            .addHeader("X-Riot-Entitlements-JWT",  entitlements.get("token").getAsString())
            .addHeader("X-Riot-ClientPlatform", CLIENT_PLATFORM)
            .addHeader("X-Riot-ClientVersion", currentApiVersion)
            .addHeader("Content-Type", "application/json");

        try (Response call = okHttpClient.newCall(builder.build()).execute()) {
            if (call.code() != 200) return null;
            return gson.fromJson(call.body().string(), JsonObject.class);
        } catch (Exception e) {
            log.info("Error", e);
        }

        return null;
    }

    @Nullable
    public JsonObject retrieveMatch(String matchId) throws IOException {
        String session = getSession().getRegion();
        String region = session.substring(0, session.length() - 1);
        String url = GLZ_URL.replace("{region}", region).replace("{shard}", region);

        JsonObject entitlements = retrieveEntitlementsAccessToken();

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
            .url(url + "/core-game/v1/matches/" + matchId)
            .addHeader("Authorization", "Bearer " + entitlements.get("accessToken").getAsString())
            .addHeader("X-Riot-Entitlements-JWT",  entitlements.get("token").getAsString())
            .addHeader("X-Riot-ClientPlatform", CLIENT_PLATFORM)
            .addHeader("X-Riot-ClientVersion", currentApiVersion)
            .addHeader("Content-Type", "application/json");

        try (Response call = okHttpClient.newCall(builder.build()).execute()) {
            return gson.fromJson(call.body().string(), JsonObject.class);
        } catch (Exception e) {
            log.info("Error", e);
        }

        return null;
    }

    @Nullable
    public JsonObject retrievePregamePlayer() throws IOException {
        String session = getSession().getRegion();
        String region = session.substring(0, session.length() - 1);
        String url = GLZ_URL.replace("{region}", region).replace("{shard}", region);

        JsonObject entitlements = retrieveEntitlementsAccessToken();

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
            .url(url + "/pregame/v1/players/" + getSession().getPuuid())
            .addHeader("Authorization", "Bearer " + entitlements.get("accessToken").getAsString())
            .addHeader("X-Riot-Entitlements-JWT",  entitlements.get("token").getAsString())
            .addHeader("X-Riot-ClientPlatform", CLIENT_PLATFORM)
            .addHeader("X-Riot-ClientVersion", currentApiVersion)
            .addHeader("Content-Type", "application/json");

        try (Response call = okHttpClient.newCall(builder.build()).execute()) {
            if (call.code() != 200) return null;
            return gson.fromJson(call.body().string(), JsonObject.class);
        } catch (Exception e) {
            log.info("Error", e);
        }

        return null;
    }

    @Nullable
    public JsonObject retrievePregameMatch(String matchId) throws IOException {
        String session = getSession().getRegion();
        String region = session.substring(0, session.length() - 1);
        String url = GLZ_URL.replace("{region}", region).replace("{shard}", region);

        JsonObject entitlements = retrieveEntitlementsAccessToken();

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
            .url(url + "/pregame/v1/matches/" + matchId)
            .addHeader("Authorization", "Bearer " + entitlements.get("accessToken").getAsString())
            .addHeader("X-Riot-Entitlements-JWT",  entitlements.get("token").getAsString())
            .addHeader("X-Riot-ClientPlatform", CLIENT_PLATFORM)
            .addHeader("X-Riot-ClientVersion", currentApiVersion)
            .addHeader("Content-Type", "application/json");

        try (Response call = okHttpClient.newCall(builder.build()).execute()) {
            if (call.code() != 200) return null;
            return gson.fromJson(call.body().string(), JsonObject.class);
        } catch (Exception e) {
            log.info("Error", e);
        }

        return null;
    }

    @Nullable
    public JsonArray retrieveContentData(String endpoint) {
        okhttp3.Request.Builder builder = new Request.Builder()
            .url(String.format("https://valorant-api.com/v1/%s?language=en-US", endpoint))
            .addHeader("Content-Type", "application/json");

        try (Response call = okHttpClient.newCall(builder.build()).execute()) {
            return gson.fromJson(call.body().string(), JsonObject.class).get("data").getAsJsonArray();
        } catch (Exception e) {
            log.info("Error", e);
        }

        return null;
    }

    @Nullable
    public JsonObject joinParty(String partyId) throws IOException {
        ClientSession session = getSession();

        String region = session.getRegion().substring(0, session.getRegion().length() - 1);
        String url = GLZ_URL.replace("{region}", region).replace("{shard}", region);

        JsonObject entitlements = retrieveEntitlementsAccessToken();

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
            .url(url + String.format("/parties/v1/players/%s/joinparty/%s", session.getPuuid(), partyId))
            .post(RequestBody.create(new byte[]{}))
            .addHeader("Authorization", "Bearer " + entitlements.get("accessToken").getAsString())
            .addHeader("X-Riot-Entitlements-JWT",  entitlements.get("token").getAsString())
            .addHeader("X-Riot-ClientPlatform", CLIENT_PLATFORM)
            .addHeader("X-Riot-ClientVersion", currentApiVersion)
            .addHeader("Content-Type", "application/json");

        try (Response call = okHttpClient.newCall(builder.build()).execute()) {
            String body = call.body().string();
            if (call.code() != 200) {
                log.warn("joinParty code={}, body={}", call.code(), body);
                return null;
            }
            return gson.fromJson(body, JsonObject.class);
        } catch (Exception e) {
            log.info("Error", e);
        }

        return null;
    }

}
