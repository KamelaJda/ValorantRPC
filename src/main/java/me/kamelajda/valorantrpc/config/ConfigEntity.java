package me.kamelajda.valorantrpc.config;

import com.google.gson.JsonElement;
import lombok.Getter;
import me.kamelajda.valorantrpc.services.LanguageService;
import org.springframework.shell.component.flow.ComponentFlow;

@Getter
public abstract class ConfigEntity<T> {

    private final T defaultValue;
    private final String id;

    protected ConfigEntity(T defaultValue, String id) {
        this.defaultValue = defaultValue;
        this.id = id;
        setValue(defaultValue);
    }

    public abstract void setValueFromObject(String stringObject);

    public abstract T getValue();

    public abstract void setValue(T value);

    public abstract JsonElement toJson();

    public abstract ComponentFlow.Builder appendToFlow(ComponentFlow.Builder flow, LanguageService languageService);

    public String getTranslateKey() {
        return "components.config." + getId();
    }

}
