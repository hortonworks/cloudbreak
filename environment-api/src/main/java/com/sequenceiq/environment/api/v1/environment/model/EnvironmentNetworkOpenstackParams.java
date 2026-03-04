package com.sequenceiq.environment.api.v1.environment.model;

import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "EnvironmentNetworkOpenstackV1Params")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentNetworkOpenstackParams {

    @Size(max = 255)
    @Schema(description = EnvironmentModelDescription.OPENSTACK_NETWORK_ID, requiredMode = Schema.RequiredMode.REQUIRED)
    private String networkId;

    @Size(max = 255)
    @Schema(description = EnvironmentModelDescription.OPENSTACK_ROUTER_ID, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String routerId;

    @Size(max = 255)
    @Schema(description = EnvironmentModelDescription.OPENSTACK_PUBLIC_NETWORK_ID, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String publicNetId;

    public String getNetworkId() {
        return networkId;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public String getRouterId() {
        return routerId;
    }

    public void setRouterId(String routerId) {
        this.routerId = routerId;
    }

    public String getPublicNetId() {
        return publicNetId;
    }

    public void setPublicNetId(String publicNetId) {
        this.publicNetId = publicNetId;
    }

    @Override
    public String toString() {
        return "EnvironmentNetworkOpenstackParams{" +
                "networkId='" + networkId + '\'' +
                ", routerId='" + routerId + '\'' +
                ", publicNetId='" + publicNetId + '\'' +
                '}';
    }

    public static final class EnvironmentNetworkOpenstackParamsBuilder {

        private String networkId;

        private String routerId;

        private String publicNetId;

        private EnvironmentNetworkOpenstackParamsBuilder() {
        }

        public static EnvironmentNetworkOpenstackParamsBuilder anEnvironmentNetworkOpenstackParams() {
            return new EnvironmentNetworkOpenstackParamsBuilder();
        }

        public EnvironmentNetworkOpenstackParamsBuilder withNetworkId(String networkId) {
            this.networkId = networkId;
            return this;
        }

        public EnvironmentNetworkOpenstackParamsBuilder withRouterId(String routerId) {
            this.routerId = routerId;
            return this;
        }

        public EnvironmentNetworkOpenstackParamsBuilder withPublicNetId(String publicNetId) {
            this.publicNetId = publicNetId;
            return this;
        }

        public EnvironmentNetworkOpenstackParams build() {
            EnvironmentNetworkOpenstackParams environmentNetworkOpenstackParams = new EnvironmentNetworkOpenstackParams();
            environmentNetworkOpenstackParams.setNetworkId(networkId);
            environmentNetworkOpenstackParams.setRouterId(routerId);
            environmentNetworkOpenstackParams.setPublicNetId(publicNetId);
            return environmentNetworkOpenstackParams;
        }
    }
}
