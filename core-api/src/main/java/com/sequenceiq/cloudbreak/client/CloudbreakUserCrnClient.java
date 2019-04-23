package com.sequenceiq.cloudbreak.client;

import static com.sequenceiq.cloudbreak.auth.altus.CrnTokenExtractor.CRN_HEADER;

import java.util.Collections;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.api.endpoint.v4.audits.AuditEventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.AutoscaleV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.BlueprintV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.ConnectorV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.CredentialV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.DatabaseV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.EnvironmentV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.EventV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.FileSystemV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.ImageCatalogV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.KerberosConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.KubernetesV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.LdapConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.mpacks.ManagementPackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.ProxyV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.user.UserV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.userprofile.UserProfileV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.UtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.WorkspaceAwareUtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.WorkspaceV4Endpoint;

public class CloudbreakUserCrnClient {

    protected static final Form EMPTY_FORM = new Form();

    private final Client client;

    private final Logger logger = LoggerFactory.getLogger(CloudbreakUserCrnClient.class);

    private WebTarget webTarget;

    protected CloudbreakUserCrnClient(String cloudbreakAddress, ConfigKey configKey) {
        client = RestClientUtil.get(configKey);
        webTarget = client.target(cloudbreakAddress).path(CoreApi.API_ROOT_CONTEXT);
        logger.info("CloudbreakUserCrnClient has been created with token. cloudbreak: {}, configKey: {}", cloudbreakAddress, configKey);
    }

    protected Client getClient() {
        return client;
    }

    public UserCrnEndpoint withCrn(String crn) {
        return new UserCrnEndpoint(webTarget, crn);
    }

    public static class UserCrnEndpoint {

        private WebTarget webTarget;

        private String crn;

        public UserCrnEndpoint(WebTarget webTarget, String crn) {
            this.webTarget = webTarget;
            this.crn = crn;
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

        public EnvironmentV4Endpoint environmentV4Endpoint() {
            return getEndpoint(EnvironmentV4Endpoint.class);
        }

        public EventV4Endpoint eventV3Endpoint() {
            return getEndpoint(EventV4Endpoint.class);
        }

        public CredentialV4Endpoint credentialV4Endpoint() {
            return getEndpoint(CredentialV4Endpoint.class);
        }

        public ImageCatalogV4Endpoint imageCatalogV4Endpoint() {
            return getEndpoint(ImageCatalogV4Endpoint.class);
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

        public ConnectorV4Endpoint connectorV4Endpoint() {
            return getEndpoint(ConnectorV4Endpoint.class);
        }

        public ProxyV4Endpoint proxyConfigV4Endpoint() {
            return getEndpoint(ProxyV4Endpoint.class);
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

        protected <E> E getEndpoint(Class<E> clazz) {
            MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(CRN_HEADER, crn);
            return newEndpoint(clazz, headers);
        }

        private <C> C newEndpoint(Class<C> resourceInterface, MultivaluedMap<String, Object> headers) {
            return WebResourceFactory.newResource(resourceInterface, webTarget, false, headers, Collections.emptyList(), EMPTY_FORM);
        }
    }

}
