package com.redhat.developers;

import io.openshift.booster.catalog.Booster;
import io.openshift.booster.catalog.BoosterCatalogService;
import io.openshift.booster.catalog.Mission;
import io.openshift.booster.catalog.Runtime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnableAutoConfiguration
@ActiveProfiles("ut")
public class BoosterCatalogServiceTest {

    @Autowired
    private BoosterCatalogService boosterCatalogService;

    @Test
    public void should_load_only_spring_boot() throws Exception {
        boosterCatalogService.index().get();
        Set<Mission> missions = boosterCatalogService.getMissions();
        assertThat(missions).isNotEmpty();
        assertThat(missions.size()).isEqualTo(6);

        missions.forEach(mission -> {
            Optional<Booster> booster = boosterCatalogService.getBooster(mission, new Runtime("spring-boot"));
            assertThat(booster.isPresent()).isTrue();
            System.out.println(">>" + booster.get().getName());
            System.out.println("Github Repo:"+booster.get().getGithubRepo());
            System.out.println("Description Path:"+booster.get().getBoosterDescriptionPath());
        });

    }
}
