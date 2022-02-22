package com.sequenceiq.cloudbreak.client;

import javax.ws.rs.client.WebTarget;

import com.sequenceiq.authorization.info.AuthorizationUtilEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.CustomConfigurationsV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.AuditEventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.AutoscaleV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.BlueprintUtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.BlueprintV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.EventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.FileSystemV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.info.CloudbreakInfoV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.operation.OperationV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.progress.ProgressV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DatalakeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.UserProfileV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.UtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.WorkspaceAwareUtilV4Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXDatabaseServerV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXUpgradeV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.FlowPublicEndpoint;

public class CloudbreakApiKeyEndpoints extends AbstractKeyBasedServiceEndpoint implements CloudbreakClient {

    CloudbreakApiKeyEndpoints(WebTarget webTarget, String accessKey, String secretKey) {
        super(webTarget, accessKey, secretKey);
    }

    public AuditEventV4Endpoint auditV4Endpoint() {
        return getEndpoint(AuditEventV4Endpoint.class);
    }

    public AutoscaleV4Endpoint autoscaleEndpoint() {
        return getEndpoint(AutoscaleV4Endpoint.class);
    }

    public BlueprintV4Endpoint blueprintV4Endpoint() {
        return getEndpoint(BlueprintV4Endpoint.class);
    }

    public BlueprintUtilV4Endpoint blueprintUtilV4Endpoint() {
        return getEndpoint(BlueprintUtilV4Endpoint.class);
    }

    public ImageCatalogV4Endpoint imageCatalogV4Endpoint() {
        return getEndpoint(ImageCatalogV4Endpoint.class);
    }

    public CloudbreakInfoV4Endpoint cloudbreakInfoV4Endpoint() {
        return getEndpoint(CloudbreakInfoV4Endpoint.class);
    }

    public DatabaseV4Endpoint databaseV4Endpoint() {
        return getEndpoint(DatabaseV4Endpoint.class);
    }

    public RecipeV4Endpoint recipeV4Endpoint() {
        return getEndpoint(RecipeV4Endpoint.class);
    }

    public CustomConfigurationsV4Endpoint customConfigurationsV4Endpoint() {
        return getEndpoint(CustomConfigurationsV4Endpoint.class);
    }

    public UserProfileV4Endpoint userProfileV4Endpoint() {
        return getEndpoint(UserProfileV4Endpoint.class);
    }

    public FileSystemV4Endpoint filesystemV4Endpoint() {
        return getEndpoint(FileSystemV4Endpoint.class);
    }

    public UtilV4Endpoint utilV4Endpoint() {
        return getEndpoint(UtilV4Endpoint.class);
    }

    public WorkspaceAwareUtilV4Endpoint workspaceAwareUtilV4Endpoint() {
        return getEndpoint(WorkspaceAwareUtilV4Endpoint.class);
    }

    public ClusterTemplateV4Endpoint clusterTemplateV4EndPoint() {
        return getEndpoint(ClusterTemplateV4Endpoint.class);
    }

    public StackV4Endpoint stackV4Endpoint() {
        return getEndpoint(StackV4Endpoint.class);
    }

    public DistroXV1Endpoint distroXV1Endpoint() {
        return getEndpoint(DistroXV1Endpoint.class);
    }

    public DistroXUpgradeV1Endpoint distroXUpgradeV1Endpoint() {
        return getEndpoint(DistroXUpgradeV1Endpoint.class);
    }

    public DatalakeV4Endpoint datalakeV4Endpoint() {
        return getEndpoint(DatalakeV4Endpoint.class);
    }

    public AuthorizationUtilEndpoint authorizationUtilEndpoint() {
        return getEndpoint(AuthorizationUtilEndpoint.class);
    }

    @Override
    public CloudProviderServicesV4Endopint cloudProviderServicesEndpoint() {
        return getEndpoint(CloudProviderServicesV4Endopint.class);
    }

    @Override
    public FlowEndpoint flowEndpoint() {
        return getEndpoint(FlowEndpoint.class);
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        return getEndpoint(FlowPublicEndpoint.class);
    }

    @Override
    public ProgressV4Endpoint progressV4Endpoint() {
        return getEndpoint(ProgressV4Endpoint.class);
    }

    @Override
    public OperationV4Endpoint operationV4Endpoint() {
        return getEndpoint(OperationV4Endpoint.class);
    }

    @Override
    public DistroXDatabaseServerV1Endpoint distroXDatabaseServerV1Endpoint() {
        return getEndpoint(DistroXDatabaseServerV1Endpoint.class);
    }

    @Override
    public EventV4Endpoint eventV4Endpoint() {
        return getEndpoint(EventV4Endpoint.class);
    }
}
