package me.kamelajda.valorantrpc.services;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.kamelajda.valorantrpc.components.ConfigComponent;
import me.kamelajda.valorantrpc.config.ConfigSingleChoose;
import me.kamelajda.valorantrpc.utils.Language;
import me.kamelajda.valorantrpc.utils.RPCInfo;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class LanguageService {

    public static final String LANGUAGE_KEY = "language";

    private final ConfigComponent component;

    @Getter
    private Language selectedLanguage = null;

    private ResourceBundle resourceBundle;

    public LanguageService(ConfigComponent component) {
        this.component = component;
        String configValue = component.getEntityById(LANGUAGE_KEY, ConfigSingleChoose.class).getValue();

        Language language;

        if (configValue == null || !Language.exist(configValue)) {
            log.info("Your language is not specified! Trying to configure it...");
            Locale locale = Locale.getDefault();

            if (locale == null) {
                log.warn("Your system language cannot be determined. I set the language to " + RPCInfo.DEFAULT_LANGUAGE.getDisplayName());
                locale = Locale.US;
            }

            language = Language.fromLocale(locale.getLanguage() + "_" + locale.getCountry());

            if (language == null) {
                log.warn("Your language ({}) is not translated yet! You can help with the translation: {}", locale.getLanguage() + "_" + locale.getCountry(), RPCInfo.CROWDIN_URL);
                language = RPCInfo.DEFAULT_LANGUAGE;
            }
        } else language = Language.valueOf(configValue);

        try {
            updateLanguage(language);
            component.updateValue(LANGUAGE_KEY, language.name(), true);
            log.info(get("service.language.success.load", language.getDisplayName()));
        } catch (Exception e) {
            log.error("Error!", e);
        }
    }

    public void updateLanguage(Language language) {
        if (language == getSelectedLanguage()) return;

        selectedLanguage = language;
        loadBundle();
    }

    private void loadBundle() {
        this.resourceBundle = ResourceBundle.getBundle("messages", getSelectedLanguage().getLocale());
    }

    private String get(String key) {
        return resourceBundle.getString(key);
    }

    public String get(String key, Object... toReplace) {
        String configValue = component.getEntityById(LANGUAGE_KEY, ConfigSingleChoose.class).getValue();
        if (Language.exist(configValue) && !configValue.equals(getSelectedLanguage().name())) {
            updateLanguage(Language.valueOf(configValue));
        }

        List<String> parsedArray = new ArrayList<>();
        for (Object k : toReplace) parsedArray.add(k.toString());
        try {
            return String.format(get(key), parsedArray.toArray());
        } catch (MissingFormatArgumentException e) {
            log.error("Bad format for key={} toReplace={} language={}", key, parsedArray, getSelectedLanguage());
            log.error("", e);
            return get(key);
        } catch (NullPointerException e) {
            log.error("Missing key translation! key={}, language={}", key, getSelectedLanguage());
            return key;
        }
    }

}
