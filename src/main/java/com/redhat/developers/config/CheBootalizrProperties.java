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

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "chebootializr")
@Data
@NoArgsConstructor(force = true)
@Component
public class CheBootalizrProperties {
    @Data
    @NoArgsConstructor(force = true)
    public static class Che {
        String serviceUrl;
        String githubUserFile;
        String githubUserTokenFile;
        String factoryPath;
    }

    @Data
    @NoArgsConstructor(force = true)
    public static class BoosterCatalog {
        String catalogRef = "master";
        String catalogRepository = "https://github.com/openshiftio/booster-catalog.git";
    }

    private Che che;
    private BoosterCatalog boosterCatalog;
}
