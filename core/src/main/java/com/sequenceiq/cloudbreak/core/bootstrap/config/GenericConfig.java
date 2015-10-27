package com.sequenceiq.cloudbreak.core.bootstrap.config;

import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;

public class GenericConfig {

    private final ContainerConfig containerConfig;

    private GenericConfig(Builder builder) {
        this.containerConfig = new ContainerConfig(builder.name, builder.version);
    }

    private ContainerConfig getContainerConfig() {
        return containerConfig;
    }

    public static class Builder {


        private final String name;

        private final String version;

        public Builder(String imageNameAndVersion) {
            String[] image = imageNameAndVersion.split(":");
            this.name = image[0];
            this.version = image[1];
        }

        public ContainerConfig build() {
            return new GenericConfig(this).getContainerConfig();
        }


    }
}
