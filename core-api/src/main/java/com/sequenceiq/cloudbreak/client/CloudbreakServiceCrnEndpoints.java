package com.sequenceiq.cloudbreak.client;

import jakarta.ws.rs.client.WebTarget;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.restartinstances.RestartInstancesV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DatalakeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DiskUpdateEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.support.SupportV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.UserProfileV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.UtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.WorkspaceAwareUtilV4Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXDatabaseServerV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXInternalV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXKraftMigrationV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXUpgradeV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1EventEndpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1RotationEndpoint;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.FlowPublicEndpoint;

public class CloudbreakServiceCrnEndpoints extends AbstractUserCrnServiceEndpoint implements CloudbreakClient {

    protected CloudbreakServiceCrnEndpoints(WebTarget webTarget, String crn) {
        super(webTarget, crn);
    }

    @Override
    public AuditEventV4Endpoint auditV4Endpoint() {
        return getEndpoint(AuditEventV4Endpoint.class);
    }

    @Override
    public AutoscaleV4Endpoint autoscaleEndpoint() {
        return getEndpoint(AutoscaleV4Endpoint.class);
    }

    @Override
    public RestartInstancesV4Endpoint restartInstancesV4Endpoint() {
        return getEndpoint(RestartInstancesV4Endpoint.class);
    }

    @Override
    public BlueprintV4Endpoint blueprintV4Endpoint() {
        return getEndpoint(BlueprintV4Endpoint.class);
    }

    public BlueprintUtilV4Endpoint blueprintUtilV4Endpoint() {
        return getEndpoint(BlueprintUtilV4Endpoint.class);
    }

    @Override
    public ImageCatalogV4Endpoint imageCatalogV4Endpoint() {
        return getEndpoint(ImageCatalogV4Endpoint.class);
    }

    @Override
    public CloudbreakInfoV4Endpoint cloudbreakInfoV4Endpoint() {
        return getEndpoint(CloudbreakInfoV4Endpoint.class);
    }

    @Override
    public DatabaseV4Endpoint databaseV4Endpoint() {
        return getEndpoint(DatabaseV4Endpoint.class);
    }

    @Override
    public RecipeV4Endpoint recipeV4Endpoint() {
        return getEndpoint(RecipeV4Endpoint.class);
    }

    @Override
    public CustomConfigurationsV4Endpoint customConfigurationsV4Endpoint() {
        return getEndpoint(CustomConfigurationsV4Endpoint.class);
    }

    @Override
    public UserProfileV4Endpoint userProfileV4Endpoint() {
        return getEndpoint(UserProfileV4Endpoint.class);
    }

    @Override
    public FileSystemV4Endpoint filesystemV4Endpoint() {
        return getEndpoint(FileSystemV4Endpoint.class);
    }

    @Override
    public UtilV4Endpoint utilV4Endpoint() {
        return getEndpoint(UtilV4Endpoint.class);
    }

    @Override
    public WorkspaceAwareUtilV4Endpoint workspaceAwareUtilV4Endpoint() {
        return getEndpoint(WorkspaceAwareUtilV4Endpoint.class);
    }

    @Override
    public ClusterTemplateV4Endpoint clusterTemplateV4EndPoint() {
        return getEndpoint(ClusterTemplateV4Endpoint.class);
    }

    @Override
    public StackV4Endpoint stackV4Endpoint() {
        return getEndpoint(StackV4Endpoint.class);
    }

    @Override
    public DistroXV1Endpoint distroXV1Endpoint() {
        return getEndpoint(DistroXV1Endpoint.class);
    }

    @Override
    public DistroXV1RotationEndpoint distroXV1RotationEndpoint() {
        return getEndpoint(DistroXV1RotationEndpoint.class);
    }

    @Override
    public DistroXUpgradeV1Endpoint distroXUpgradeV1Endpoint() {
        return getEndpoint(DistroXUpgradeV1Endpoint.class);
    }

    @Override
    public DistroXV1EventEndpoint distroXV1EventEndpoint() {
        return getEndpoint(DistroXV1EventEndpoint.class);
    }

    @Override
    public DistroXKraftMigrationV1Endpoint distroXKraftMigrationV1Endpoint() {
        return getEndpoint(DistroXKraftMigrationV1Endpoint.class);
    }

    public DistroXInternalV1Endpoint distroXInternalV1Endpoint() {
        return getEndpoint(DistroXInternalV1Endpoint.class);
    }

    @Override
    public DatalakeV4Endpoint datalakeV4Endpoint() {
        return getEndpoint(DatalakeV4Endpoint.class);
    }

    @Override
    public DiskUpdateEndpoint diskUpdateEndpoint() {
        return getEndpoint(DiskUpdateEndpoint.class);
    }

    @Override
    public CloudProviderServicesV4Endopint cloudProviderServicesEndpoint() {
        return getEndpoint(CloudProviderServicesV4Endopint.class);
    }

    @Override
    public AuthorizationUtilEndpoint authorizationUtilEndpoint() {
        return getEndpoint(AuthorizationUtilEndpoint.class);
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

    @Override
    public SupportV1Endpoint supportV1Endpoint() {
        return getEndpoint(SupportV1Endpoint.class);
    }
}
