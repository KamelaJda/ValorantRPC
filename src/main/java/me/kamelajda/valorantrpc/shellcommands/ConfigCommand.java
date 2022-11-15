package me.kamelajda.valorantrpc.shellcommands;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.kamelajda.valorantrpc.components.ConfigComponent;
import me.kamelajda.valorantrpc.config.ConfigEntity;
import me.kamelajda.valorantrpc.services.LanguageService;
import org.springframework.shell.component.context.ComponentContext;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.standard.AbstractShellComponent;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ShellComponent
@RequiredArgsConstructor
public class ConfigCommand extends AbstractShellComponent {

    private final ConfigComponent component;
    private final LanguageService languageService;

    @ShellMethod(key = "config edit", value = "Edit config", group = "Configs")
    public void editConfig() throws IOException {
        ComponentFlow.ComponentFlowResult flowResult = component.generateFlow(languageService).run();

        ComponentContext<?> context = flowResult.getContext();
        for (Map.Entry<Object, Object> entry : context.stream().collect(Collectors.toList())) {
            component.updateValue(entry.getKey().toString(), entry.getValue().toString(), false);
        }

        component.updateConfig();
        log.info(languageService.get("components.commands.config.success"));
    }

    @ShellMethod(key = "config show", value = "Show config", group = "Configs")
    public void showConfig() {
        for (ConfigEntity<?> entity : component.getEntityList()) {
            log.info("{} ({}) = {}", languageService.get(entity.getTranslateKey()), entity.getId(), entity.getValue());
        }
    }

}
