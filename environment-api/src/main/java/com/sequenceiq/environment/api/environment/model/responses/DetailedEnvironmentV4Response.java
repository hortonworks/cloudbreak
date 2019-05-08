package com.sequenceiq.environment.api.environment.model.responses;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses.KubernetesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.environment.api.environment.doc.EnvironmentResponseModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetailedEnvironmentV4Response extends EnvironmentV4BaseResponse {
    @ApiModelProperty(EnvironmentResponseModelDescription.PROXY_CONFIGS)
    private Set<ProxyV4Response> proxies = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.LDAP_CONFIGS)
    private Set<LdapV4Response> ldaps = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.RDS_CONFIGS)
    private Set<String> databases = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.KUBERNETES_CONFIGS)
    private Set<String> kubernetes = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.KERBEROS_CONFIGS)
    private Set<String> kerberoses = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.DATALAKE_RESOURCES)
    private Set<DatalakeResourcesV4Response> datalakeResourcesResponses;

    @ApiModelProperty(EnvironmentResponseModelDescription.WORKLOAD_CLUSTERS)
    private Set<String> workloadClusters = new HashSet<>();

    @ApiModelProperty(EnvironmentResponseModelDescription.DATALAKE_CLUSTERS)
    private Set<String> datalakeClusters = new HashSet<>();

}
