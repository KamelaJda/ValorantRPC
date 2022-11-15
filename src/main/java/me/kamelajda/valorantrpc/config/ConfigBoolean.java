package me.kamelajda.valorantrpc.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import lombok.Getter;
import lombok.Setter;
import me.kamelajda.valorantrpc.services.LanguageService;
import org.springframework.shell.component.flow.ComponentFlow;

public class ConfigBoolean extends ConfigEntity<Boolean> {

    @Getter @Setter
    private Boolean value = true;

    public ConfigBoolean(Boolean defaultValue, String id) {
        super(defaultValue, id);
    }

    @Override
    public void setValueFromObject(String stringObject) {
        try {
            setValue(Boolean.valueOf(stringObject));
        } catch (Exception e) {
            setValue(getDefaultValue());
        }
    }

    @Override
    public JsonElement toJson() {
        if (value == null) return JsonNull.INSTANCE;
        return new JsonPrimitive(value);
    }

    @Override
    public ComponentFlow.Builder appendToFlow(ComponentFlow.Builder flow, LanguageService languageService) {
        return flow.withConfirmationInput(getId())
            .name(languageService.get(getTranslateKey()))
            .and();
    }

}
