package com.sequenceiq.cloudbreak.client.internal;

import jakarta.ws.rs.client.WebTarget;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.co2.ClusterCO2V4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.cost.ClusterCostV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.DatabaseConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.diagnostics.DiagnosticsV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.encryption.EncryptionV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.EventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.FileSystemV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.operation.OperationV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.progress.ProgressV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DatalakeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DiskUpdateEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.UserProfileV4Endpoint;
import com.sequenceiq.cloudbreak.client.ApiClientRequestFilter;
import com.sequenceiq.cloudbreak.client.ThreadLocalUserCrnWebTargetBuilder;
import com.sequenceiq.cloudbreak.client.WebTargetEndpointFactory;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXDatabaseServerV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXInternalV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXUpgradeV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1RotationEndpoint;
import com.sequenceiq.flow.api.FlowEndpoint;

@Configuration
public class CloudbreakApiClientConfig {

    private final ApiClientRequestFilter apiClientRequestFilter;

    public CloudbreakApiClientConfig(ApiClientRequestFilter apiClientRequestFilter) {
        this.apiClientRequestFilter = apiClientRequestFilter;
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
                .build();
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    DistroXV1Endpoint distroXV1Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, DistroXV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    DistroXInternalV1Endpoint distroXInternalV1Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, DistroXInternalV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    DistroXDatabaseServerV1Endpoint distroXDatabaseServerV1Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, DistroXDatabaseServerV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    DistroXV1RotationEndpoint distroXV1RotationEndpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, DistroXV1RotationEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    ClusterCostV4Endpoint clusterCostV1Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, ClusterCostV4Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    ClusterCO2V4Endpoint clusterCO2V4Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, ClusterCO2V4Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    DistroXUpgradeV1Endpoint distroXUpgradeV1Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, DistroXUpgradeV1Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    UserProfileV4Endpoint userProfileV4Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, UserProfileV4Endpoint.class);
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
    EventV4Endpoint eventV4Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, EventV4Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    DatabaseConfigV4Endpoint databaseConfigV4Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, DatabaseConfigV4Endpoint.class);
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
    DiskUpdateEndpoint diskUpdateEndpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, DiskUpdateEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    FlowEndpoint flowEndpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, FlowEndpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    DiagnosticsV4Endpoint diagnosticsV4Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, DiagnosticsV4Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    ProgressV4Endpoint progressV4Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, ProgressV4Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    OperationV4Endpoint operationV4Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, OperationV4Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    ImageCatalogV4Endpoint imageCatalogV4Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, ImageCatalogV4Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    RecipeV4Endpoint recipeV4Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, RecipeV4Endpoint.class);
    }

    @Bean
    @ConditionalOnBean(name = "cloudbreakApiClientWebTarget")
    EncryptionV4Endpoint encryptionV4Endpoint(WebTarget cloudbreakApiClientWebTarget) {
        return new WebTargetEndpointFactory().createEndpoint(cloudbreakApiClientWebTarget, EncryptionV4Endpoint.class);
    }
}
