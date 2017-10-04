package com.redhat.developers.vo;

import io.openshift.booster.catalog.Booster;
import io.openshift.booster.catalog.Mission;
import lombok.Data;

@Data
public class ProjectMissionVO {

    private Mission mission;
    private Booster booster;
    private String boosterDescription;
}
