package me.kamelajda.valorantrpc.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import lombok.Getter;
import lombok.Setter;
import me.kamelajda.valorantrpc.services.LanguageService;
import org.springframework.shell.component.flow.ComponentFlow;

import java.util.Map;

public class ConfigSingleChoose extends ConfigEntity<String> {

    @Getter @Setter
    private String value;

    private final Map<String, String> options;

    public ConfigSingleChoose(String defaultValue, String id, Map<String, String> options) {
        super(defaultValue, id);
        this.options = options;
    }

    @Override
    public void setValueFromObject(String stringObject) {
        setValue(stringObject);
    }

    @Override
    public JsonElement toJson() {
        if (value == null) return JsonNull.INSTANCE;
        return new JsonPrimitive(value);
    }

    @Override
    public ComponentFlow.Builder appendToFlow(ComponentFlow.Builder flow, LanguageService languageService) {
        return flow.withSingleItemSelector(getId())
            .name(languageService.get(getTranslateKey()))
            .selectItems(options)
            .and();
    }

}
