package com.sequenceiq.environment.api.v1.environment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentNetworkYarnV1Params")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentNetworkYarnParams {
    @Schema(description = EnvironmentModelDescription.YARN_QUEUE, requiredMode = Schema.RequiredMode.REQUIRED)
    private String queue;

    @Schema(description = EnvironmentModelDescription.YARN_LIFETIME)
    private Integer lifetime;

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public Integer getLifetime() {
        return lifetime;
    }

    public void setLifetime(Integer lifetime) {
        this.lifetime = lifetime;
    }

    @Override
    public String toString() {
        return "EnvironmentNetworkYarnParams{" +
                "queue='" + queue + '\'' +
                ", lifetime=" + lifetime +
                '}';
    }

    public static final class EnvironmentNetworkYarnParamsBuilder {

        private String queue;

        private Integer lifetime;

        private EnvironmentNetworkYarnParamsBuilder() {
        }

        public static EnvironmentNetworkYarnParamsBuilder anEnvironmentNetworkYarnParams() {
            return new EnvironmentNetworkYarnParamsBuilder();
        }

        public EnvironmentNetworkYarnParamsBuilder withQueue(String queue) {
            this.queue = queue;
            return this;
        }

        public EnvironmentNetworkYarnParamsBuilder withLifetime(Integer lifetime) {
            this.lifetime = lifetime;
            return this;
        }

        public EnvironmentNetworkYarnParams build() {
            EnvironmentNetworkYarnParams environmentNetworkYarnParams = new EnvironmentNetworkYarnParams();
            environmentNetworkYarnParams.setQueue(queue);
            environmentNetworkYarnParams.setLifetime(lifetime);
            return environmentNetworkYarnParams;
        }
    }
}
