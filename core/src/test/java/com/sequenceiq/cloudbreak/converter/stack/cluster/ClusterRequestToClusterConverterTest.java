package com.sequenceiq.cloudbreak.converter.stack.cluster;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.converter.AbstractJsonConverterTest;
import com.sequenceiq.cloudbreak.converter.IdBrokerConverterUtil;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.CloudStorageConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ClusterV4RequestToClusterConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@RunWith(MockitoJUnitRunner.class)
public class ClusterRequestToClusterConverterTest extends AbstractJsonConverterTest<ClusterV4Request> {

    @InjectMocks
    private ClusterV4RequestToClusterConverter underTest;

    @Mock
    private ConversionService conversionService;

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

    @Spy
    @SuppressFBWarnings(value = "UrF", justification = "This gets injected")
    private IdBrokerConverterUtil idBrokerConverterUtil =  new IdBrokerConverterUtil();

    @Before
    public void setUp() {
        when(workspaceService.getForCurrentUser()).thenReturn(workspace);

        if (StringUtils.isEmpty(ThreadBasedUserCrnProvider.getUserCrn())) {
            ThreadBasedUserCrnProvider.setUserCrn("crn:cdp:iam:us-west-1:1234:user:1");
        }
        when(entitlementService.dataLakeEfsEnabled(anyString())).thenReturn(true);
    }

    @Test
    public void testConvert() {
        // GIVEN
        ClusterV4Request request = getRequest("cluster.json");

        Blueprint blueprint = new Blueprint();
        blueprint.setStackType(StackType.HDP.name());
        given(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(eq("my-blueprint"), any())).willReturn(blueprint);
        given(conversionService.convert(request.getGateway(), Gateway.class)).willReturn(new Gateway());
        // WHEN
        Cluster result = underTest.convert(request);
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason", "clusterManagerIp",
                "fileSystem", "additionalFileSystem", "rdsConfigs", "attributes", "uptime", "ambariSecurityMasterKey", "proxyConfigCrn", "configStrategy",
                "extendedBlueprintText", "environmentCrn", "variant", "description", "databaseServerCrn", "fqdn", "customConfigurations"));
    }

    @Test
    public void testConvertWithCloudStorageDetails() {
        // GIVEN
        ClusterV4Request request = getRequest("cluster-with-cloud-storage.json");

        given(conversionService.convert(request.getGateway(), Gateway.class)).willReturn(new Gateway());
        given(cloudStorageConverter.requestToFileSystem(request.getCloudStorage())).willReturn(new FileSystem());
        given(cloudStorageValidationUtil.isCloudStorageConfigured(request.getCloudStorage())).willReturn(true);
        Blueprint blueprint = new Blueprint();
        blueprint.setStackType(StackType.HDP.name());
        given(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(eq("my-blueprint"), any())).willReturn(blueprint);
        // WHEN
        Cluster result = underTest.convert(request);
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason", "clusterManagerIp",
                "additionalFileSystem", "rdsConfigs", "attributes", "uptime", "ambariSecurityMasterKey", "proxyConfigCrn", "extendedBlueprintText",
                "environmentCrn", "variant", "description", "databaseServerCrn", "fqdn", "configStrategy", "customConfigurations"));
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
        assertAllFieldsNotNull(result, Arrays.asList("stack", "blueprint", "creationStarted", "creationFinished", "upSince", "statusReason", "clusterManagerIp",
                "fileSystem", "additionalFileSystem", "rdsConfigs", "attributes", "uptime", "ambariSecurityMasterKey", "proxyConfigCrn", "configStrategy",
                "extendedBlueprintText", "gateway", "environmentCrn", "variant", "description", "databaseServerCrn", "fqdn", "customConfigurations"));
        assertNull(result.getGateway());
    }

    @Override
    public Class<ClusterV4Request> getRequestClass() {
        return ClusterV4Request.class;
    }
}
