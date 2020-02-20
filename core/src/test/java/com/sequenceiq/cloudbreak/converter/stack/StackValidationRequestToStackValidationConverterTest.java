package com.sequenceiq.cloudbreak.converter.stack;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackValidationV4RequestToStackValidationConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.StackValidation;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@RunWith(MockitoJUnitRunner.class)
public class StackValidationRequestToStackValidationConverterTest {

    private static final String BP_NAME = "HDF3.1 Datascience Pack";

    private static final String BP_NAME_2 = "HDP 3.1 Spark Pack";

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private NetworkService networkService;

    @Mock
    private CredentialClientService credentialClientService;

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
    private ConverterUtil converterUtil;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private CredentialResponse credentialResponse;

    @Mock
    private CredentialConverter credentialConverter;

    @InjectMocks
    private StackValidationV4RequestToStackValidationConverter underTest;

    private StackValidationV4Request validationRequest;

    private DetailedEnvironmentResponse environment;

    private final Workspace workspace = TestUtil.workspace(1L, "myWorkspace");

    private final Credential credential = TestUtil.awsCredential();

    @Before
    public void init() {
        validationRequest = new StackValidationV4Request();
        validationRequest.setEnvironmentCrn("envCrn");
        mockUserRelated();
        mockBlueprintsInWorkspace();
        environment = new DetailedEnvironmentResponse();
        environment.setCredential(credentialResponse);
        when(environmentClientService.getByCrn(anyString())).thenReturn(environment);
        when(credentialConverter.convert(credentialResponse)).thenReturn(credential);
    }

    @Test
    public void invalidBlueprintValidationRequest() {
        validationRequest = new StackValidationV4Request();
        Workspace workspace = TestUtil.workspace(1L, "myWorkspace");
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(TestUtil.cbAdminUser());
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        expectedEx.expect(BadRequestException.class);
        expectedEx.expectMessage("Cluster definition is not configured for the validation request!");
        underTest.convert(validationRequest);
    }

    @Test
    public void validBlueprintByName() {
        validationRequest.setNetworkId(442L);
        validationRequest.setBlueprintName(BP_NAME);

        Map<Platform, PlatformParameters> platformParametersMap = new HashMap<>();
        platformParametersMap.put(Platform.platform("AWS"), parameters);
        when(cloudParameterCache.getPlatformParameters()).thenReturn(platformParametersMap);
        when(networkService.get(any())).thenReturn(TestUtil.network());

        StackValidation result = underTest.convert(validationRequest);

        assertEquals(BP_NAME, result.getBlueprint().getName());
    }

    @Test
    public void convertShouldThrowAccessDeniedExceptionWhenNoEnvironmentAndCredentialAreGiven() {
        // GIVEN
        validationRequest.setNetworkId(442L);
        validationRequest.setBlueprintName(BP_NAME);
        environment.setCredential(null);

        expectedEx.expect(BadRequestException.class);
        expectedEx.expectMessage("Credential is not configured for the validation request!");
        // WHEN
        underTest.convert(validationRequest);
        // THEN expected exception should be thrown
    }

    private void mockUserRelated() {
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(TestUtil.cbAdminUser());
    }

    private void mockBlueprintsInWorkspace() {
        Set<Blueprint> blueprints = new HashSet<>();
        blueprints.add(TestUtil.blueprint(1L, BP_NAME, "{}"));
        blueprints.add(TestUtil.blueprint(2L, BP_NAME_2, "{}"));
        when(blueprintService.getAllAvailableInWorkspace(any())).thenReturn(blueprints);
    }

}
