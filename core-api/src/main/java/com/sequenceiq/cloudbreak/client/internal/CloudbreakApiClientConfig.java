package com.sequenceiq.cloudbreak.client.internal;

import javax.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.FileSystemV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DatalakeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.client.ApiClientRequestFilter;
import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.flow.api.FlowEndpoint;

import io.opentracing.contrib.jaxrs2.client.ClientTracingFeature;

@Configuration
public class CloudbreakApiClientConfig {

    private final ApiClientRequestFilter apiClientRequestFilter;

    private final ClientTracingFeature clientTracingFeature;

    public CloudbreakApiClientConfig(ApiClientRequestFilter apiClientRequestFilter, ClientTracingFeature clientTracingFeature) {
        this.apiClientRequestFilter = apiClientRequestFilter;
        this.clientTracingFeature = clientTracingFeature;
    }

    @Bean
    @ConditionalOnBean(CloudbreakApiClientParams.class)
    public WebTarget cloudbreakApiClientWebTarget(CloudbreakApiClientParams cloudbreakApiClientParams) {
        return new ThreadLocalUserCrnWebTargetBuilder(cloudbreakApiClientParams.getServiceUrl())
                .withCertificateValidation(cloudbreakApiClientParams.isCertificateValidation())
                .withIgnorePreValidation(cloudbreakApiClientParams.isIgnorePreValidation())
                .withDebug(cloudbreakApiClientParams.isRestDebug())
                .withClientRequestFilter(apiClientRequestFilter)
                .withApiRoot(CoreApi.API_ROOT_CONTEXT)
                .withTracer(clientTracingFeature)
                .build();
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    DistroXV1Endpoint distroXV1Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, DistroXV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    ClusterTemplateV4Endpoint clusterTemplateV4Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, ClusterTemplateV4Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    StackV4Endpoint stackV4Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, StackV4Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    CloudProviderServicesV4Endopint cloudProviderServicesV4Endopint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, CloudProviderServicesV4Endopint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    FileSystemV4Endpoint fileSystemV4Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, FileSystemV4Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    DatalakeV4Endpoint datalakeV4Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, DatalakeV4Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    FlowEndpoint flowEndpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, FlowEndpoint.class);
    }
}
