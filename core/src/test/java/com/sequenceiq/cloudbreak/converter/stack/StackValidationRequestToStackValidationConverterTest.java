package com.sequenceiq.cloudbreak.converter.stack;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackValidationV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackValidationV4RequestToStackValidationConverter;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@RunWith(MockitoJUnitRunner.class)
public class StackValidationRequestToStackValidationConverterTest {
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Mock
    private ClusterDefinitionService clusterDefinitionService;

    @Mock
    private NetworkService networkService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private ConversionService conversionService;

    @Mock
    private CloudParameterCache cloudParameterCache;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private UserService userService;

    @Mock
    private PlatformParameters parameters;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private EnvironmentViewService environmentViewService;

    @Mock
    private ConverterUtil converterUtil;

    @InjectMocks
    private StackValidationV4RequestToStackValidationConverter underTest;

    private StackValidationV4Request validationRequest;

    private String bpName = "HDF3.1 Datascience Pack";

    private String bpName2 = "HDP 3.1 Spark Pack";

    private Workspace workspace = TestUtil.workspace(1L, "myWorkspace");

    private Credential credential = TestUtil.gcpCredential();

    @Before
    public void init() {
        validationRequest = new StackValidationV4Request();
        mockUserRelated();
        mockClusterDefinitionsInWorkspace();
    }

    @Test
    public void invalidClusterDefinitionValidationRequest() {
        validationRequest = new StackValidationV4Request();
        Workspace workspace = TestUtil.workspace(1L, "myWorkspace");
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(TestUtil.cbAdminUser());
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        expectedEx.expect(BadRequestException.class);
        expectedEx.expectMessage("Cluster definition is not configured for the validation request!");
        underTest.convert(validationRequest);
    }

    @Test
    public void validClusterDefinitionByName() {
        validationRequest.setNetworkId(442L);
        validationRequest.setClusterDefinitionName(bpName);
        validationRequest.setCredentialName("credName");

        when(credentialService.getByNameForWorkspace(validationRequest.getCredentialName(), workspace)).thenReturn(credential);

        Map<Platform, PlatformParameters> platformParametersMap = new HashMap<>();
        platformParametersMap.put(Platform.platform("GCP"), parameters);
        when(cloudParameterCache.getPlatformParameters()).thenReturn(platformParametersMap);
        when(networkService.get(any())).thenReturn(TestUtil.network());

        StackValidation result = underTest.convert(validationRequest);

        assertEquals(bpName, result.getClusterDefinition().getName());
    }

    @Test
    public void convertShouldUseEnvironmentCredentialWhenItisGiven() {
        // GIVEN
        validationRequest.setNetworkId(442L);
        validationRequest.setClusterDefinitionName(bpName);
        EnvironmentView environmentView = new EnvironmentView();
        environmentView.setName("env");
        environmentView.setCredential(credential);
        validationRequest.setEnvironmentName(environmentView.getName());
        Map<Platform, PlatformParameters> platformParametersMap = new HashMap<>();
        platformParametersMap.put(Platform.platform("GCP"), parameters);
        when(cloudParameterCache.getPlatformParameters()).thenReturn(platformParametersMap);
        when(networkService.get(any())).thenReturn(TestUtil.network());
        when(environmentViewService.getByNameForWorkspace(environmentView.getName(), workspace)).thenReturn(environmentView);
        // WHEN
        StackValidation actualResult = underTest.convert(validationRequest);
        // THEN
        assertEquals(credential, actualResult.getCredential());
        assertEquals(environmentView, actualResult.getEnvironment());
    }

    @Test
    public void convertShouldThrowAccessDeniedExceptinWhenNoEnvironmentAndCredentialAreGiven() {
        // GIVEN
        validationRequest.setNetworkId(442L);
        validationRequest.setClusterDefinitionName(bpName);
        expectedEx.expect(BadRequestException.class);
        expectedEx.expectMessage("Credential is not configured for the validation request!");
        // WHEN
        underTest.convert(validationRequest);
        // THEN expected exception should be thrown
    }

    private void mockClusterDefinitionsInWorkspace() {
        Set<ClusterDefinition> clusterDefinitions = new HashSet<>();
        clusterDefinitions.add(TestUtil.clusterDefinition(1L, bpName, "{}"));
        clusterDefinitions.add(TestUtil.clusterDefinition(2L, bpName2, "{}"));
        when(clusterDefinitionService.getAllAvailableInWorkspace(any())).thenReturn(clusterDefinitions);
    }

    private void mockUserRelated() {
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(TestUtil.cbAdminUser());
    }

}