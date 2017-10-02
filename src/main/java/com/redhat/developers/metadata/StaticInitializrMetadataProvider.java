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

import io.spring.initializr.metadata.DefaultMetadataElement;
import io.spring.initializr.metadata.InitializrMetadata;
import io.spring.initializr.metadata.InitializrMetadataProvider;

import java.util.ArrayList;
import java.util.List;


public class StaticInitializrMetadataProvider implements InitializrMetadataProvider {

    private final InitializrMetadata metadata;

    public StaticInitializrMetadataProvider(InitializrMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public InitializrMetadata get() {
        metadata.updateSpringBootVersions(staticVersions());
        return metadata;
    }

    /**
     * @return
     */
    protected List<DefaultMetadataElement> staticVersions() {

        List<DefaultMetadataElement> bootVersions = new ArrayList<>();

        DefaultMetadataElement vMetadata = new DefaultMetadataElement();

        vMetadata.setId("1.5.7.RELEASE");
        vMetadata.setDefault(true);
        vMetadata.setName("1.5.7");
        bootVersions.add(vMetadata);

        vMetadata = new DefaultMetadataElement();
        vMetadata.setId("1.4.7.RELEASE");
        vMetadata.setDefault(false);
        vMetadata.setName("1.4.7");
        bootVersions.add(vMetadata);

        return bootVersions;
    }
}
