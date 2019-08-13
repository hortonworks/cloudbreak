package com.sequenceiq.cloudbreak.client;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.AuditEventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.AutoscaleV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.BlueprintUtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.BlueprintV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.FileSystemV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.info.CloudbreakInfoV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.KubernetesV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.ManagementPackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.providerservices.CloudProviderServicesV4Endopint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.DatalakeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.user.UserV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.UserProfileV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.UtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.WorkspaceAwareUtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.WorkspaceV4Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;

public interface CloudbreakClient {

    AuditEventV4Endpoint auditV4Endpoint();

    AutoscaleV4Endpoint autoscaleEndpoint();

    BlueprintV4Endpoint blueprintV4Endpoint();

    BlueprintUtilV4Endpoint blueprintUtilV4Endpoint();

    ImageCatalogV4Endpoint imageCatalogV4Endpoint();

    CloudbreakInfoV4Endpoint cloudbreakInfoV4Endpoint();

    ManagementPackV4Endpoint managementPackV4Endpoint();

    KubernetesV4Endpoint kubernetesV4Endpoint();

    WorkspaceV4Endpoint workspaceV4Endpoint();

    DatabaseV4Endpoint databaseV4Endpoint();

    RecipeV4Endpoint recipeV4Endpoint();

    UserProfileV4Endpoint userProfileV4Endpoint();

    UserV4Endpoint userV4Endpoint();

    FileSystemV4Endpoint filesystemV4Endpoint();

    UtilV4Endpoint utilV4Endpoint();

    WorkspaceAwareUtilV4Endpoint workspaceAwareUtilV4Endpoint();

    ClusterTemplateV4Endpoint clusterTemplateV4EndPoint();

    StackV4Endpoint stackV4Endpoint();

    DistroXV1Endpoint distroXV1Endpoint();

    DatalakeV4Endpoint datalakeV4Endpoint();

    CloudProviderServicesV4Endopint cloudProviderServicesEndpoint();
}
