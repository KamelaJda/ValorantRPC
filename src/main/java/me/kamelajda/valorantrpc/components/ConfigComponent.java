package me.kamelajda.valorantrpc.components;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.kamelajda.valorantrpc.config.ConfigBoolean;
import me.kamelajda.valorantrpc.config.ConfigEntity;
import me.kamelajda.valorantrpc.config.ConfigSingleChoose;
import me.kamelajda.valorantrpc.services.LanguageService;
import me.kamelajda.valorantrpc.utils.Language;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ConfigComponent {

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    @Getter
    private final List<ConfigEntity<?>> entityList;
    private final File configFile;

    @Autowired
    private ComponentFlow.Builder componentFlowBuilder;

    public ConfigComponent() throws IOException {
        this.entityList = new ArrayList<>();
        this.configFile = new File("config.json");

        Map<String, String> languages = new HashMap<>();
        for (Language value : Language.values()) {
            if (value == Language.UNKNOWN) continue;
            languages.put(value.getDisplayName(), value.name());
        }

        ConfigSingleChoose language = new ConfigSingleChoose(Language.UNKNOWN.name(), LanguageService.LANGUAGE_KEY, languages);

        entityList.add(new ConfigBoolean(true, "showrank"));
        entityList.add(new ConfigBoolean(true, "showlevel"));
        entityList.add(new ConfigBoolean(true, "showagent"));
        entityList.add(new ConfigBoolean(true, "showmap"));
        entityList.add(new ConfigBoolean(true, "showparty"));
        entityList.add(new ConfigBoolean(true, "joinbutton"));
        entityList.add(language);

        if (!configFile.exists()) {
            try {
                if (!configFile.createNewFile()) throw new IllegalStateException("File not created");
                updateConfig();
                log.info("Config file created! You can use ,,config edit'' command to edit config.");
            } catch (IOException e) {
                log.error("Cannot create config file!", e);
            }
        } else {
            JsonObject json = gson.fromJson(new FileReader(configFile), JsonObject.class);
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                updateValue(entry.getKey(), entry.getValue().getAsString(), false);
            }
        }
    }

    public ComponentFlow generateFlow(LanguageService languageService) {
        ComponentFlow.Builder builder = componentFlowBuilder.clone().reset();

        for (ConfigEntity<?> entity : entityList) {
            builder = entity.appendToFlow(builder, languageService);
        }

        return builder.build();
    }

    public void updateValue(String id, String newValue, boolean updateConfigFile) throws IOException {
        ConfigEntity<?> entity = getEntityList().stream()
            .filter(f -> f.getId().equals(id)).findAny()
            .orElseThrow(() -> new IllegalStateException("Cannot find config entity with id " + id));

        entity.setValueFromObject(newValue);
        if (updateConfigFile) updateConfig();
    }

    public void updateConfig() throws IOException {
        if (!configFile.exists()) throw new FileNotFoundException("config.json");

        new FileWriter(configFile, false).close();
        Files.writeString(configFile.toPath(), toJson());
    }

    public String toJson() {
        JsonObject jo = new JsonObject();

        for (ConfigEntity<?> entity : entityList) {
            jo.add(entity.getId(), entity.toJson());
        }

        return gson.toJson(jo);
    }

    public <T extends ConfigEntity<?>> T getEntityById(String id, Class<T> cls) {
        return cls.cast(getEntityById(id));
    }

    private ConfigEntity<?> getEntityById(String id) {
        return getEntityList().stream()
            .filter(f -> f.getId().equals(id)).findAny()
            .orElseThrow(() -> new IllegalStateException("Cannot find config entity with id " + id));
    }

}
