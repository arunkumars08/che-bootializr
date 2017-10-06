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
import io.openshift.booster.catalog.spi.BoosterCatalogListener;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.Stack;

public class SpringBootBoosterListener implements BoosterCatalogListener, Publisher<Booster> {

    //Local Cache
    private final Stack<Booster> boosters = new Stack<>();

    private Subscriber<? super Booster> subscriber;

    @Override
    public void boosterAdded(Booster booster) {
        if (subscriber != null) {
            //FIXME there should be a nice reactive way to handle this
            while (!boosters.empty()) {
                subscriber.onNext(boosters.pop());
            }
            subscriber
                .onNext(booster);
        } else {
            boosters.push(booster);
        }
    }

    @Override
    public void subscribe(Subscriber<? super Booster> subscriber) {
        this.subscriber = subscriber;
    }
}
