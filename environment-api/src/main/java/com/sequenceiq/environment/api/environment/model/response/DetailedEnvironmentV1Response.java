package com.sequenceiq.environment.api.environment.model.response;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.environment.api.environment.doc.EnvironmentModelDescription;
import com.sequenceiq.environment.api.ldap.model.response.LdapV1Response;
import com.sequenceiq.environment.api.proxy.model.response.ProxyV1Response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class DetailedEnvironmentV1Response extends EnvironmentV1BaseResponse {

    @ApiModelProperty(EnvironmentModelDescription.PROXY_CONFIGS_RESPONSE)
    private final Set<ProxyV1Response> proxies = new HashSet<>();

    @ApiModelProperty(EnvironmentModelDescription.LDAP_CONFIGS_RESPONSE)
    private final Set<LdapV1Response> ldaps = new HashSet<>();

    @ApiModelProperty(EnvironmentModelDescription.RDS_CONFIGS_RESPONSE)
    private final Set<String> databases = new HashSet<>();

    @ApiModelProperty(EnvironmentModelDescription.KUBERNETES_CONFIGS_RESPONSE)
    private final Set<String> kubernetes = new HashSet<>();

    @ApiModelProperty(EnvironmentModelDescription.KERBEROS_CONFIGS_RESPONSE)
    private final Set<String> kerberoses = new HashSet<>();

    @ApiModelProperty(EnvironmentModelDescription.DATALAKE_RESOURCES)
    private Set<DatalakeResourcesV1Response> datalakeResourcesResponses;

    @ApiModelProperty(EnvironmentModelDescription.WORKLOAD_CLUSTERS)
    private final Set<String> workloadClusters = new HashSet<>();

    @ApiModelProperty(EnvironmentModelDescription.DATALAKE_CLUSTERS)
    private final Set<String> datalakeClusters = new HashSet<>();

    public Set<ProxyV1Response> getProxies() {
        return proxies;
    }

    public Set<LdapV1Response> getLdaps() {
        return ldaps;
    }

    public Set<String> getDatabases() {
        return databases;
    }

    public Set<String> getKubernetes() {
        return kubernetes;
    }

    public Set<String> getKerberoses() {
        return kerberoses;
    }

    public Set<DatalakeResourcesV1Response> getDatalakeResourcesResponses() {
        return datalakeResourcesResponses;
    }

    public void setDatalakeResourcesResponses(Set<DatalakeResourcesV1Response> datalakeResourcesResponses) {
        this.datalakeResourcesResponses = datalakeResourcesResponses;
    }

    public Set<String> getWorkloadClusters() {
        return workloadClusters;
    }

    public Set<String> getDatalakeClusters() {
        return datalakeClusters;
    }

    public static final class DetailedEnvironmentV1ResponseBuilder {
        private Long id;

        private String name;

        private String description;

        private CompactRegionV1Response regions;

        private String cloudPlatform;

        private Set<ProxyV1Response> proxies = new HashSet<>();

        private String credentialName;

        private LocationV1Response location;

        private Set<LdapV1Response> ldaps = new HashSet<>();

        private Set<String> datalakeResourcesNames;

        private Set<String> databases = new HashSet<>();

        private Set<String> datalakeClusterNames;

        private Set<String> kubernetes = new HashSet<>();

        private Set<String> workloadClusterNames;

        private Set<String> kerberoses = new HashSet<>();

        private EnvironmentNetworkV1Response network;

        private Set<DatalakeResourcesV1Response> datalakeResourcesResponses;

        private EnvironmentStatus environmentStatus;

        private Set<String> workloadClusters = new HashSet<>();

        private Set<String> datalakeClusters = new HashSet<>();

        private DetailedEnvironmentV1ResponseBuilder() {
        }

        public static DetailedEnvironmentV1ResponseBuilder aDetailedEnvironmentV1Response() {
            return new DetailedEnvironmentV1ResponseBuilder();
        }

        public DetailedEnvironmentV1ResponseBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withRegions(CompactRegionV1Response regions) {
            this.regions = regions;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withProxies(Set<ProxyV1Response> proxies) {
            this.proxies = proxies;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withCredentialName(String credentialName) {
            this.credentialName = credentialName;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withLocation(LocationV1Response location) {
            this.location = location;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withLdaps(Set<LdapV1Response> ldaps) {
            this.ldaps = ldaps;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withDatalakeResourcesNames(Set<String> datalakeResourcesNames) {
            this.datalakeResourcesNames = datalakeResourcesNames;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withDatabases(Set<String> databases) {
            this.databases = databases;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withDatalakeClusterNames(Set<String> datalakeClusterNames) {
            this.datalakeClusterNames = datalakeClusterNames;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withKubernetes(Set<String> kubernetes) {
            this.kubernetes = kubernetes;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withWorkloadClusterNames(Set<String> workloadClusterNames) {
            this.workloadClusterNames = workloadClusterNames;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withKerberoses(Set<String> kerberoses) {
            this.kerberoses = kerberoses;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withNetwork(EnvironmentNetworkV1Response network) {
            this.network = network;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withDatalakeResourcesResponses(Set<DatalakeResourcesV1Response> datalakeResourcesResponses) {
            this.datalakeResourcesResponses = datalakeResourcesResponses;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withEnvironmentStatus(EnvironmentStatus environmentStatus) {
            this.environmentStatus = environmentStatus;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withWorkloadClusters(Set<String> workloadClusters) {
            this.workloadClusters = workloadClusters;
            return this;
        }

        public DetailedEnvironmentV1ResponseBuilder withDatalakeClusters(Set<String> datalakeClusters) {
            this.datalakeClusters = datalakeClusters;
            return this;
        }

        public DetailedEnvironmentV1Response build() {
            DetailedEnvironmentV1Response detailedEnvironmentV1Response = new DetailedEnvironmentV1Response();
            detailedEnvironmentV1Response.setId(id);
            detailedEnvironmentV1Response.setName(name);
            detailedEnvironmentV1Response.setDescription(description);
            detailedEnvironmentV1Response.setRegions(regions);
            detailedEnvironmentV1Response.setCloudPlatform(cloudPlatform);
            detailedEnvironmentV1Response.setCredentialName(credentialName);
            detailedEnvironmentV1Response.setLocation(location);
            detailedEnvironmentV1Response.setDatalakeResourcesNames(datalakeResourcesNames);
            detailedEnvironmentV1Response.setDatalakeClusterNames(datalakeClusterNames);
            detailedEnvironmentV1Response.setWorkloadClusterNames(workloadClusterNames);
            detailedEnvironmentV1Response.setNetwork(network);
            detailedEnvironmentV1Response.setDatalakeResourcesResponses(datalakeResourcesResponses);
            detailedEnvironmentV1Response.setEnvironmentStatus(environmentStatus);
            detailedEnvironmentV1Response.kubernetes.addAll(this.kubernetes);
            detailedEnvironmentV1Response.kerberoses.addAll(this.kerberoses);
            detailedEnvironmentV1Response.workloadClusters.addAll(this.workloadClusters);
            detailedEnvironmentV1Response.databases.addAll(this.databases);
            detailedEnvironmentV1Response.ldaps.addAll(this.ldaps);
            detailedEnvironmentV1Response.datalakeClusters.addAll(this.datalakeClusters);
            detailedEnvironmentV1Response.proxies.addAll(this.proxies);
            return detailedEnvironmentV1Response;
        }
    }
}
