package com.sequenceiq.cloudbreak.converter.stack.cluster;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.converter.AbstractJsonConverterTest;
import com.sequenceiq.cloudbreak.converter.IdBrokerConverterUtil;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.CloudStorageConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ClusterV4RequestToClusterConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.GatewayV4RequestToGatewayConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ExtendWith(MockitoExtension.class)
public class ClusterRequestToClusterConverterTest extends AbstractJsonConverterTest<ClusterV4Request> {

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @InjectMocks
    private ClusterV4RequestToClusterConverter underTest;

    @Mock
    private CloudStorageValidationUtil cloudStorageValidationUtil;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private Workspace workspace;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private CloudStorageConverter cloudStorageConverter;

    @Mock
    private GatewayV4RequestToGatewayConverter gatewayV4RequestToGatewayConverter;

    @Spy
    @SuppressFBWarnings(value = "UrF", justification = "This gets injected")
    private IdBrokerConverterUtil idBrokerConverterUtil = new IdBrokerConverterUtil();

    @BeforeEach
    public void setUp() {
        when(workspaceService.getForCurrentUser()).thenReturn(workspace);
    }

    @Test
    public void testConvert() {
        // GIVEN
        ClusterV4Request request = getRequest("cluster.json");

        Blueprint blueprint = new Blueprint();
        blueprint.setStackType(StackType.HDP.name());
        given(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(eq("my-blueprint"), any())).willReturn(blueprint);
        given(gatewayV4RequestToGatewayConverter.convert(request.getGateway())).willReturn(new Gateway());
        // WHEN
        Cluster result = underTest.convert(request);
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason",
                "clusterManagerIp", "fileSystem", "additionalFileSystem", "rdsConfigs", "attributes", "uptime", "proxyConfigCrn",
                "configStrategy", "extendedBlueprintText", "environmentCrn", "variant", "description", "databaseServerCrn", "fqdn", "customConfigurations",
                "dbSslEnabled", "dbSslRootCertBundle", "certExpirationDetails"));
    }

    @Test
    public void testConvertWithCloudStorageDetails() {
        // GIVEN
        ClusterV4Request request = getRequest("cluster-with-cloud-storage.json");

        given(gatewayV4RequestToGatewayConverter.convert(request.getGateway())).willReturn(new Gateway());
        given(cloudStorageConverter.requestToFileSystem(request.getCloudStorage())).willReturn(new FileSystem());
        given(cloudStorageValidationUtil.isCloudStorageConfigured(request.getCloudStorage())).willReturn(true);
        Blueprint blueprint = new Blueprint();
        blueprint.setStackType(StackType.HDP.name());
        given(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(eq("my-blueprint"), any())).willReturn(blueprint);
        // WHEN
        Cluster result = ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.convert(request));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason",
                "clusterManagerIp", "additionalFileSystem", "rdsConfigs", "attributes", "uptime", "proxyConfigCrn",
                "extendedBlueprintText", "environmentCrn", "variant", "description", "databaseServerCrn", "fqdn", "configStrategy", "customConfigurations",
                "dbSslEnabled", "dbSslRootCertBundle", "certExpirationDetails"));
    }

    @Test
    public void testNoGateway() {
        // GIVEN
        Blueprint blueprint = new Blueprint();
        blueprint.setStackType(StackType.HDP.name());
        given(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(eq("my-blueprint"), any())).willReturn(blueprint);
        // WHEN
        ClusterV4Request clusterRequest = getRequest("cluster-no-gateway.json");
        Cluster result = underTest.convert(clusterRequest);
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason",
                "clusterManagerIp", "fileSystem", "additionalFileSystem", "rdsConfigs", "attributes", "uptime", "proxyConfigCrn",
                "configStrategy", "extendedBlueprintText", "gateway", "environmentCrn", "variant", "description", "databaseServerCrn", "fqdn",
                "customConfigurations", "dbSslEnabled", "dbSslRootCertBundle", "certExpirationDetails"));
        assertNull(result.getGateway());
    }

    @Override
    public Class<ClusterV4Request> getRequestClass() {
        return ClusterV4Request.class;
    }
}
