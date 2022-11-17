package me.kamelajda.valorantrpc.shellcommands;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.kamelajda.valorantrpc.services.LanguageService;
import me.kamelajda.valorantrpc.services.VersionService;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@ShellComponent
@RequiredArgsConstructor
public class UpdateCommand {

    private final VersionService versionService;
    private final OkHttpClient okHttpClient;
    private final LanguageService languageService;

    @ShellMethod(key = "update", value = "Download latest version", group = "Update")
    public void update() {
        log.info(languageService.get("components.commands.update.checking"));

        JsonObject jsonObject = versionService.getGithubLastReleases();
        boolean bol = versionService.checkUpdate(jsonObject, false);

        if (!bol) {
            log.info(languageService.get("components.commands.update.last_version"));
            return;
        }

        log.info(languageService.get("components.commands.update.downloading"));

        String assetUrl = jsonObject.getAsJsonArray("assets")
            .get(0)
            .getAsJsonObject()
            .get("browser_download_url")
            .getAsString();

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
        builder.addHeader("Content-Type", "application/x-zip-compressed");
        builder.url(assetUrl);

        byte[] buffer = new byte[2048];

        try (Response response = okHttpClient.newCall(builder.build()).execute();
             InputStream stream = response.body().byteStream();
             ZipInputStream zip = new ZipInputStream(stream, StandardCharsets.UTF_8)
        ) {

            Path outDir = Files.createDirectory(Path.of(jsonObject.get("tag_name").getAsString()));

            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path filePath = outDir.resolve(entry.getName());

                try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
                     BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length)
                ) {

                    int len;
                    while ((len = zip.read(buffer)) > 0) {
                        bos.write(buffer, 0, len);
                    }
                }
            }

            log.info(languageService.get("components.commands.update.success", outDir));
        } catch (Exception e) {
            log.error("Error!", e);
        }

    }

}
