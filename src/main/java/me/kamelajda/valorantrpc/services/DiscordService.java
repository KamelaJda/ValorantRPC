package me.kamelajda.valorantrpc.services;

import de.jcm.discordgamesdk.Core;
import de.jcm.discordgamesdk.CreateParams;
import de.jcm.discordgamesdk.DiscordEventAdapter;
import de.jcm.discordgamesdk.LobbyManager;
import de.jcm.discordgamesdk.activity.Activity;
import de.jcm.discordgamesdk.lobby.LobbyTransaction;
import de.jcm.discordgamesdk.lobby.LobbyType;
import de.jcm.discordgamesdk.user.DiscordUser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.kamelajda.valorantrpc.components.ConfigComponent;
import me.kamelajda.valorantrpc.config.ConfigBoolean;
import me.kamelajda.valorantrpc.dto.SocketData;
import me.kamelajda.valorantrpc.presences.StartingPresence;
import me.kamelajda.valorantrpc.presences.ingame.InGamePresence;
import me.kamelajda.valorantrpc.presences.ingame.PreGamePresence;
import me.kamelajda.valorantrpc.presences.menu.AwayPresence;
import me.kamelajda.valorantrpc.presences.menu.InMenuPresence;
import me.kamelajda.valorantrpc.presences.menu.QueuePresence;
import me.kamelajda.valorantrpc.utils.PresenceImages;
import me.kamelajda.valorantrpc.utils.PresenceState;
import me.kamelajda.valorantrpc.utils.RPCInfo;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Service
@Slf4j
@Order(value = Ordered.HIGHEST_PRECEDENCE + 4)
public class DiscordService {

    private final Core core;
    @Getter
    private final List<PresenceState> states;
    private final ConfigComponent configComponent;
    private Activity currentActivity = null;
    private final LockFileService lockFileService;

    @Getter
    private DiscordUser currentUser = null;

    public DiscordService(LockFileService lockFileService, LanguageService languageService, ConfigComponent configComponent) {
        this.states = new ArrayList<>();
        this.configComponent = configComponent;
        this.lockFileService = lockFileService;
        states.add(new StartingPresence(this, languageService));
        states.add(new AwayPresence(this, languageService));
        states.add(new QueuePresence(this, languageService));
        states.add(new InMenuPresence(this, languageService, configComponent));
        states.add(new InGamePresence(this, lockFileService, languageService, configComponent));
        states.add(new PreGamePresence(this, lockFileService, languageService));

        try {
            Runtime runtime = Runtime.getRuntime();
            Process exec = runtime.exec("tasklist");

            InputStream inputStream = exec.getInputStream();

            Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8).useDelimiter("\\A");
            String text = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            if (!text.contains("Discord.exe") && !text.contains("DiscordCanary.exe")) {
                log.error("You need to turn on Discord first!");
                System.exit(1);
            }
        } catch (IOException e) {
            log.error("Error", e);
        }

        Core.initFromClasspath();

        CreateParams createParams = new CreateParams();

        createParams.registerEventHandler(new DiscordListener());

        createParams.setClientID(RPCInfo.APPLICATION_ID);
        createParams.setFlags(CreateParams.Flags.DEFAULT);

        this.core = new Core(createParams);

        runCallback();

        while (getCurrentUser() == null) {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        tryApply(null);


        LobbyManager lobbyManager = core.lobbyManager();

        LobbyTransaction transaction = lobbyManager.getLobbyCreateTransaction();

        transaction.setCapacity(6);
        transaction.setType(LobbyType.PUBLIC);
        transaction.setMetadata("a", "123");
        transaction.setLocked(false);
    }

    public void tryApply(SocketData socketData) {
        Activity activity = new Activity();
        activity.assets().setLargeImage(PresenceImages.VALORANT_LOGO.getKey());

        if (socketData != null) {
            if (configComponent.getEntityById("showparty", ConfigBoolean.class).getValue()) {
                activity.party().size().setMaxSize(socketData.getStateDetails().getMaxPartySize());
                activity.party().size().setCurrentSize(socketData.getStateDetails().getPartySize());
            }

            if (socketData.getStateDetails().isPartyOwner()) {
                if (configComponent.getEntityById("joinbutton", ConfigBoolean.class).getValue() && !socketData.getStateDetails().getPartyAccessibility().equals("CLOSED")) {
                    activity.party().setID(socketData.getPid().split("@")[0]);
                    activity.secrets().setJoinSecret(socketData.getStateDetails().getPartyId());
                }
            }
        }

        for (PresenceState state : states) {
            if (state.tryApply(socketData, activity)) {
                return;
            }
        }

        activity.close();
    }

    @Async
    public void runCallback() {
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                try {
                    core.runCallbacks();
                } catch (Exception e) {
                    log.error("Error!", e);
                }

            }
        }).start();
    }

    public void updateRP(Activity activity) {
        if (currentActivity != null) currentActivity.close();

        if (activity == null) {
            core.activityManager().clearActivity();
            currentActivity = null;
            return;
        }

        currentActivity = activity;
        core.activityManager().updateActivity(activity);
    }

    public class DiscordListener extends DiscordEventAdapter {

        @Override
        public void onActivityJoin(String secret) {
            try {
                lockFileService.joinParty(secret);
            } catch (IOException e) {
                log.info("Error!");
            }
        }

        @Override
        public void onCurrentUserUpdate() {
            currentUser = core.userManager().getCurrentUser();
            log.info("Discord Current user update! {}", getCurrentUser().getUsername() + "#" + getCurrentUser().getDiscriminator());
        }
    }
}
