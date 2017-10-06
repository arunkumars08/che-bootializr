/**
 * Copyright (c) 2017 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
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
                    .put("description", booster.getDescription())
                    .put("adoc", "function(){ return function(text, render) { return docToHTML(render(text));}}");
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
