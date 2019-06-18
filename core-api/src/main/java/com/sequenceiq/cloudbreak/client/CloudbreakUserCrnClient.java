package com.sequenceiq.cloudbreak.client;

import javax.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.AuditEventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.AutoscaleV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.BlueprintUtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.BlueprintV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.EnvironmentV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.EventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.FileSystemV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.info.CloudbreakInfoV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.KerberosConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.KubernetesV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.LdapConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.ManagementPackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.user.UserV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.UserProfileV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.UtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.WorkspaceAwareUtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.WorkspaceV4Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;

public class CloudbreakUserCrnClient extends AbstractUserCrnServiceClient<CloudbreakUserCrnClient.CloudbreakEndpoint> {
    public CloudbreakUserCrnClient(String serviceAddress, ConfigKey configKey, String apiRoot) {
        super(serviceAddress, configKey, apiRoot);
    }

    @Override
    public CloudbreakEndpoint withCrn(String crn) {
        return new CloudbreakEndpoint(getWebTarget(), crn);
    }

    public static class CloudbreakEndpoint extends AbstractUserCrnServiceEndpoint {
        public CloudbreakEndpoint(WebTarget webTarget, String crn) {
            super(webTarget, crn);
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

        public EnvironmentV4Endpoint environmentV4Endpoint() {
            return getEndpoint(EnvironmentV4Endpoint.class);
        }

        public EventV4Endpoint eventV3Endpoint() {
            return getEndpoint(EventV4Endpoint.class);
        }

        public ImageCatalogV4Endpoint imageCatalogV4Endpoint() {
            return getEndpoint(ImageCatalogV4Endpoint.class);
        }

        public CloudbreakInfoV4Endpoint cloudbreakInfoV4Endpoint() {
            return getEndpoint(CloudbreakInfoV4Endpoint.class);
        }

        public LdapConfigV4Endpoint ldapConfigV4Endpoint() {
            return getEndpoint(LdapConfigV4Endpoint.class);
        }

        public ManagementPackV4Endpoint managementPackV4Endpoint() {
            return getEndpoint(ManagementPackV4Endpoint.class);
        }

        public KubernetesV4Endpoint kubernetesV4Endpoint() {
            return getEndpoint(KubernetesV4Endpoint.class);
        }

        public WorkspaceV4Endpoint workspaceV4Endpoint() {
            return getEndpoint(WorkspaceV4Endpoint.class);
        }

        public DatabaseV4Endpoint databaseV4Endpoint() {
            return getEndpoint(DatabaseV4Endpoint.class);
        }

        public RecipeV4Endpoint recipeV4Endpoint() {
            return getEndpoint(RecipeV4Endpoint.class);
        }

        public UserProfileV4Endpoint userProfileV4Endpoint() {
            return getEndpoint(UserProfileV4Endpoint.class);
        }

        public UserV4Endpoint userV4Endpoint() {
            return getEndpoint(UserV4Endpoint.class);
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

        public KerberosConfigV4Endpoint kerberosConfigV4Endpoint() {
            return getEndpoint(KerberosConfigV4Endpoint.class);
        }

        public StackV4Endpoint stackV4Endpoint() {
            return getEndpoint(StackV4Endpoint.class);
        }

        public DistroXV1Endpoint distroXV1Endpoint() {
            return getEndpoint(DistroXV1Endpoint.class);
        }
    }
}
