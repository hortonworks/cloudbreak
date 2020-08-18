package com.sequenceiq.environment.api.v1.environment.model;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "EnvironmentNetworkOpenstackV1Params")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class EnvironmentNetworkOpenstackParams {

    @Size(max = 255)
    @ApiModelProperty(value = EnvironmentModelDescription.OPENSTACK_NETWORK_ID, required = true)
    private String networkId;

    @ApiModelProperty(value = EnvironmentModelDescription.OPENSTACK_ROUTER_ID, required = true)
    private String routerId;

    @ApiModelProperty(value = EnvironmentModelDescription.OPENSTACK_PUBLIC_NET_ID, required = true)
    private String publicNetId;

    @ApiModelProperty(value = EnvironmentModelDescription.OPENSTACK_NETWORKING_OPTION_ID, required = true)
    private String networkingOption;

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

    public String getNetworkingOption() {
        return networkingOption;
    }

    public void setNetworkingOption(String networkingOption) {
        this.networkingOption = networkingOption;
    }

    public static final class EnvironmentNetworkOpenstackParamsBuilder {
        private String networkId;

        private String routerId;

        private String publicNetId;

        private String networkingOption;

        private EnvironmentNetworkOpenstackParamsBuilder() {
        }

        public static EnvironmentNetworkOpenstackParamsBuilder anEnvironmentNetworkOpenstackParamsBuilder() {
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

        public EnvironmentNetworkOpenstackParamsBuilder withNetworkingOption(String networkingOption) {
            this.networkingOption = networkingOption;
            return this;
        }

        public EnvironmentNetworkOpenstackParams build() {
            EnvironmentNetworkOpenstackParams environmentNetworkAwsParams = new EnvironmentNetworkOpenstackParams();
            environmentNetworkAwsParams.setNetworkId(networkId);
            environmentNetworkAwsParams.setRouterId(routerId);
            environmentNetworkAwsParams.setPublicNetId(publicNetId);
            environmentNetworkAwsParams.setNetworkingOption(networkingOption);
            return environmentNetworkAwsParams;
        }
    }
}
