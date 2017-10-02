package com.redhat.developers.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheSpringBootProjectVO {

    private String artifactId;
    private String groupId;
    private String name;
    private String packaging;
    private String workspaceUrl;
}
