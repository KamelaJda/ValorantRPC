package me.kamelajda.valorantrpc.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.kamelajda.valorantrpc.utils.RPCInfo;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

@Slf4j
@Service
@Order(value = Ordered.HIGHEST_PRECEDENCE + 1)
public class VersionService {

    private final Gson gson = new Gson();
    private final OkHttpClient okHttpClient;
    private final LanguageService languageService;
    private Date build;

    public VersionService(OkHttpClient okHttpClient, LanguageService languageService) {
        this.okHttpClient = okHttpClient;
        this.languageService = languageService;

        Properties prop = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF")) {
            prop.load(is);
        } catch (IOException e) {
            log.error("Error!", e);
            return;
        }

        String version = prop.getProperty("Implementation-Version", "?.?.?");
        build = new Date(Long.parseLong(prop.getProperty("Build-Time", "0")));

        log.info(languageService.get("service.version.used_version", version, build));
        checkUpdate(getGithubLastReleases(), true);
    }

    @SneakyThrows(value = ParseException.class)
    public boolean checkUpdate(JsonObject jsonObject, boolean sendLog) {
        Date publishedAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(jsonObject.get("published_at").getAsString());

        if (publishedAt.getTime() > build.getTime()) {
            if (sendLog) {
                log.info("=======================================");
                log.info("");
                log.info(languageService.get("service.version.new_update", jsonObject.get("tag_name").getAsString()));
                log.info("");
                log.info("=======================================");
            }
            return true;
        }

        return false;
    }

    public JsonObject getGithubLastReleases() {
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
        builder.url("https://api.github.com/repos/" + RPCInfo.GITHUB_TAG + "/releases/latest");
        builder.addHeader("Content-Type", "application/json");

        try (Response call = okHttpClient.newCall(builder.build()).execute()) {
            return gson.fromJson(call.body().string(), JsonObject.class);
        } catch (Exception e) {
            log.error("Error!", e);
        }

        return null;
    }

}
