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
package com.redhat.developers.metadata;

import io.spring.initializr.metadata.*;
import io.spring.initializr.util.Version;

import java.util.LinkedHashMap;
import java.util.Map;

//FIXME - lots more refining requried
public class VersionedDependencyMetadataProvider implements DependencyMetadataProvider {

    @Override
    public DependencyMetadata get(InitializrMetadata metadata, Version bootVersion) {

        //Dependencies: FIXME - get default Spring Boot dependencies and make them available
        Map<String, Dependency> dependencyMap = new LinkedHashMap<>();
        for (Dependency dependency : metadata.getDependencies().getAll()) {
            if (dependency.match(bootVersion)) {
                dependencyMap.put(dependency.getId(), dependency.resolve(bootVersion));
            }
        }


        //Dependency Repositories
        Map<String, Repository> repositoriesMap = new LinkedHashMap<>();
        for (Dependency dependency : dependencyMap.values()) {
            if (dependency.getRepository() != null) {
                repositoriesMap.put(dependency.getRepository(),
                    metadata.getConfiguration().getEnv().getRepositories().get(dependency.getRepository()));
            }
        }

        //Dependency Repositories
        Map<String, BillOfMaterials> bomMap = new LinkedHashMap<>();
        for (Dependency dependency : dependencyMap.values()) {
            if (dependency.getBom() != null) {
                bomMap.put(dependency.getBom(),
                    metadata.getConfiguration().getEnv().getBoms().get(dependency.getBom()).resolve(bootVersion));
            }
        }

        //BOM Repositories
        for (BillOfMaterials bom : bomMap.values()) {
            for (String id : bom.getRepositories()) {
                repositoriesMap.put(id, metadata.getConfiguration()
                    .getEnv().getRepositories().get(id));
            }
        }

        return new DependencyMetadata(bootVersion, dependencyMap, repositoriesMap, bomMap);
    }
}
