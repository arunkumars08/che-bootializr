package com.redhat.developers.config;

import io.openshift.booster.catalog.Booster;
import io.openshift.booster.catalog.BoosterCatalogService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.publisher.Flux;

import java.io.IOException;

@Component
@Slf4j
public class BoosterCatalogSocketHandler extends TextWebSocketHandler {

    private final SpringBootBoosterListener springBootBoosterListener;
    private final BoosterCatalogService boosterCatalogService;

    private Flux<Booster> boosters;

    public BoosterCatalogSocketHandler(BoosterCatalogService boosterCatalogService,
                                       SpringBootBoosterListener springBootBoosterListener) {
        this.boosterCatalogService = boosterCatalogService;
        this.springBootBoosterListener = springBootBoosterListener;
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Connection Established ... triggering index");
        boosters = Flux.from(springBootBoosterListener);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {

        log.info("Handling message ...");

        if (boosterCatalogService.index().isDone()) {
            boosters = Flux.fromIterable(boosterCatalogService.getBoosters());
        }

        boosters
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
}
