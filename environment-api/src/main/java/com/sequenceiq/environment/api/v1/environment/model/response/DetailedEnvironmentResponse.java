package com.sequenceiq.environment.api.v1.environment.model.response;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.doc.environment.EnvironmentModelDescription;
import com.sequenceiq.environment.api.proxy.model.response.ProxyV1Response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "DetailedEnvironmentV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DetailedEnvironmentResponse extends EnvironmentBaseResponse {

    @ApiModelProperty(EnvironmentModelDescription.PROXY_CONFIGS_RESPONSE)
    private Set<ProxyV1Response> proxies = new HashSet<>();

    public void setProxies(Set<ProxyV1Response> proxies) {
        this.proxies = proxies;
    }

    public Set<ProxyV1Response> getProxies() {
        return proxies;
    }

    public static final class DetailedEnvironmentResponseBuilder {
        private Long id;

        private String name;

        private String description;

        private CompactRegionResponse regions;

        private String cloudPlatform;

        private Set<ProxyV1Response> proxies = new HashSet<>();

        private String credentialName;

        private LocationResponse location;

        private EnvironmentNetworkResponse network;

        private EnvironmentStatus environmentStatus;

        private DetailedEnvironmentResponseBuilder() {
        }

        public static DetailedEnvironmentResponseBuilder aDetailedEnvironmentResponse() {
            return new DetailedEnvironmentResponseBuilder();
        }

        public DetailedEnvironmentResponseBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public DetailedEnvironmentResponseBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public DetailedEnvironmentResponseBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public DetailedEnvironmentResponseBuilder withRegions(CompactRegionResponse regions) {
            this.regions = regions;
            return this;
        }

        public DetailedEnvironmentResponseBuilder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public DetailedEnvironmentResponseBuilder withProxies(Set<ProxyV1Response> proxies) {
            this.proxies = proxies;
            return this;
        }

        public DetailedEnvironmentResponseBuilder withCredentialName(String credentialName) {
            this.credentialName = credentialName;
            return this;
        }

        public DetailedEnvironmentResponseBuilder withLocation(LocationResponse location) {
            this.location = location;
            return this;
        }

        public DetailedEnvironmentResponseBuilder withNetwork(EnvironmentNetworkResponse network) {
            this.network = network;
            return this;
        }

        public DetailedEnvironmentResponseBuilder withEnvironmentStatus(EnvironmentStatus environmentStatus) {
            this.environmentStatus = environmentStatus;
            return this;
        }

        public DetailedEnvironmentResponse build() {
            DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
            detailedEnvironmentResponse.setId(id);
            detailedEnvironmentResponse.setName(name);
            detailedEnvironmentResponse.setDescription(description);
            detailedEnvironmentResponse.setRegions(regions);
            detailedEnvironmentResponse.setCloudPlatform(cloudPlatform);
            detailedEnvironmentResponse.setProxies(proxies);
            detailedEnvironmentResponse.setCredentialName(credentialName);
            detailedEnvironmentResponse.setLocation(location);
            detailedEnvironmentResponse.setNetwork(network);
            detailedEnvironmentResponse.setEnvironmentStatus(environmentStatus);
            return detailedEnvironmentResponse;
        }
    }
}
