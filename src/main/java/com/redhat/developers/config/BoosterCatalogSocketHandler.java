package com.redhat.developers.config;

import io.openshift.booster.catalog.Booster;
import io.openshift.booster.catalog.BoosterCatalogService;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Component
@Slf4j
public class BoosterCatalogSocketHandler extends TextWebSocketHandler {

    private final BoosterCatalogService boosterCatalogService;
    private Flowable<Booster> boosters;

    public BoosterCatalogSocketHandler(BoosterCatalogService boosterCatalogService) {
        this.boosterCatalogService = boosterCatalogService;
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Connection Established ... triggering index");
        boosters = boosterCatalogService.rxIndex();
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {

        log.info("Handling message ...");

        boosters
            .filter(booster -> isSpringBoot(booster))
            .subscribe(booster -> {
                JSONObject projectBoosterJson = new JSONObject()
                    .put("id", booster.getId())
                    .put("name", booster.getName())
                    .put("description", booster.getDescription());
                try {
                    String payload = projectBoosterJson.toString();
                    log.debug("PAYLOAD:{}", payload);
                    session.sendMessage(new TextMessage(payload));
                } catch (IOException e) {
                    log.error("Error sending messgae", e);
                }
            }, throwable -> log.warn(throwable.getMessage()));
    }

    /**
     * @param booster
     * @return
     */
    private boolean isSpringBoot(Booster booster) {

        return (booster.getRuntime().getId().equals("spring-boot"))
            && (booster.getVersion().getId().equals("community"))
            && (booster.getId().contains("spring")
            || booster.getLabels().contains("spring")
            || booster.getDescription().contains("spring")
            || booster.getName().contains("spring"));
    }
}
