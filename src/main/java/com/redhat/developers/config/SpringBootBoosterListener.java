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
