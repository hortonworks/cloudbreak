package com.sequenceiq.cloudbreak.converter.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackValidationRequest;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
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
    private BlueprintService blueprintService;

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

    @InjectMocks
    private StackValidationRequestToStackValidationConverter underTest;

    private StackValidationRequest validationRequest = new StackValidationRequest();

    private String bpName = "HDF3.1 Datascience Pack";

    private String bpName2 = "HDP 3.1 Spark Pack";

    private Workspace workspace = TestUtil.workspace(1L, "myWorkspace");

    private Credential credential = TestUtil.gcpCredential();

    @Before
    public void init() {
        validationRequest = new StackValidationRequest();
        mockUserRelated();
        mockCredentialRelated();
        mockBlueprintsInWorkspace();
    }

    @Test
    public void invalidBlueprintValidationRequest() {
        validationRequest = new StackValidationRequest();
        Workspace workspace = TestUtil.workspace(1L, "myWorkspace");
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(TestUtil.cbAdminUser());
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        try {
            StackValidation result = underTest.convert(validationRequest);
            fail("Validation did not fail on invalid StackValidationRequest");
        } catch (BadRequestException e) {
            assertEquals("Blueprint is not configured for the validation request!", e.getMessage());
        }
    }

    @Test
    public void validBlueprintByName() {
        validationRequest.setNetworkId(442L);
        validationRequest.setBlueprintName(bpName);

        when(credentialService.get(any(), eq(workspace))).thenReturn(credential);

        Map<Platform, PlatformParameters> platformParametersMap = new HashMap<>();
        platformParametersMap.put(Platform.platform("GCP"), parameters);
        when(cloudParameterCache.getPlatformParameters()).thenReturn(platformParametersMap);
        when(networkService.get(any())).thenReturn(TestUtil.network());

        StackValidation result = underTest.convert(validationRequest);

        assertEquals(bpName, result.getBlueprint().getName());
    }

    @Test
    public void validBlueprintById() {
        validationRequest.setNetworkId(442L);
        validationRequest.setBlueprintId(1L);

        when(credentialService.get(any(), eq(workspace))).thenReturn(credential);

        Map<Platform, PlatformParameters> platformParametersMap = new HashMap<>();
        platformParametersMap.put(Platform.platform("GCP"), parameters);
        when(cloudParameterCache.getPlatformParameters()).thenReturn(platformParametersMap);
        when(networkService.get(any())).thenReturn(TestUtil.network());

        StackValidation result = underTest.convert(validationRequest);

        assertEquals(bpName, result.getBlueprint().getName());
    }

    @Test
    public void validBlueprintByIdAndName() {
        validationRequest.setNetworkId(442L);
        validationRequest.setBlueprintId(1L);
        validationRequest.setBlueprintName(bpName2);

        when(credentialService.get(any(), eq(workspace))).thenReturn(credential);

        Map<Platform, PlatformParameters> platformParametersMap = new HashMap<>();
        platformParametersMap.put(Platform.platform("GCP"), parameters);
        when(cloudParameterCache.getPlatformParameters()).thenReturn(platformParametersMap);
        when(networkService.get(any())).thenReturn(TestUtil.network());

        StackValidation result = underTest.convert(validationRequest);

        assertEquals("Blueprint ID overrides blueprint name", bpName, result.getBlueprint().getName());
    }

    @Test
    public void validBlueprintByText() {
        validationRequest.setNetworkId(442L);
        validationRequest.setBlueprint(new BlueprintRequest());

        when(credentialService.get(any(), eq(workspace))).thenReturn(credential);
        when(conversionService.convert(any(), eq(Blueprint.class))).thenReturn(TestUtil.blueprint(bpName));

        Map<Platform, PlatformParameters> platformParametersMap = new HashMap<>();
        platformParametersMap.put(Platform.platform("GCP"), parameters);
        when(cloudParameterCache.getPlatformParameters()).thenReturn(platformParametersMap);
        when(networkService.get(any())).thenReturn(TestUtil.network());

        StackValidation result = underTest.convert(validationRequest);

        assertEquals(bpName, result.getBlueprint().getName());
    }

    @Test
    public void convertShouldUseEnvironmentCredentialWhenItisGiven() {
        // GIVEN
        validationRequest.setNetworkId(442L);
        validationRequest.setBlueprintName(bpName);
        EnvironmentView environmentView = new EnvironmentView();
        environmentView.setName("env");
        environmentView.setCredential(credential);
        validationRequest.setEnvironment(environmentView.getName());
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
        validationRequest.setBlueprintName(bpName);
        validationRequest.setCredentialId(null);
        validationRequest.setCredential(null);
        expectedEx.expect(BadRequestException.class);
        expectedEx.expectMessage("Credential is not configured for the validation request!");
        // WHEN
        underTest.convert(validationRequest);
        // THEN expected exception should be thrown
    }

    private void mockCredentialRelated() {
        CredentialRequest credentialRequest = new CredentialRequest();
        validationRequest.setCredentialId(1L);
        credentialRequest.setCloudPlatform("AWS");
        validationRequest.setCredential(credentialRequest);
    }

    private void mockBlueprintsInWorkspace() {
        Set<Blueprint> blueprints = new HashSet<>();
        blueprints.add(TestUtil.blueprint(1L, bpName, "{}"));
        blueprints.add(TestUtil.blueprint(2L, bpName2, "{}"));
        when(blueprintService.getAllAvailableInWorkspace(any())).thenReturn(blueprints);
    }

    private void mockUserRelated() {
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(TestUtil.cbAdminUser());
    }

}