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
