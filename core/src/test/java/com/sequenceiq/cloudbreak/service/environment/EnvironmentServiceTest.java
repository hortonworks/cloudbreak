package com.sequenceiq.cloudbreak.service.environment;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentRequest;
import com.sequenceiq.cloudbreak.api.model.environment.response.DetailedEnvironmentResponse;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentCreationValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentServiceTest {

    private static final String CREDENTIAL_NAME = "cred1";

    private static final String ENVIRONMENT_NAME = "EnvName";

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private ProxyConfigService proxyConfigService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private EnvironmentCreationValidator environmentCreationValidator;

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private EnvironmentService environmentService;

    @Test
    public void testCreateWithCredentialName() throws TransactionExecutionException {
        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        environmentRequest.setName(ENVIRONMENT_NAME);

        environmentRequest.setCredentialName(CREDENTIAL_NAME);
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setName("IgnoredCredRequestName");
        environmentRequest.setCredential(credentialRequest);
        Long workspaceId = 1L;

        when(credentialService.getByNameForWorkspaceId(CREDENTIAL_NAME, workspaceId)).thenReturn(new Credential());

        initMocks();

        DetailedEnvironmentResponse response = environmentService.createForLoggedInUser(environmentRequest, workspaceId);
        assertNotNull(response);
    }

    @Test
    public void testCreateWithCredential() throws TransactionExecutionException {
        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        environmentRequest.setName(ENVIRONMENT_NAME);

        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setName("CredRequestName");
        environmentRequest.setCredential(credentialRequest);
        Long workspaceId = 1L;

        when(credentialService.createForLoggedInUser(any(), anyLong())).thenReturn(new Credential());
        when(conversionService.convert(any(CredentialRequest.class), eq(Credential.class))).thenReturn(new Credential());

        initMocks();

        DetailedEnvironmentResponse response = environmentService.createForLoggedInUser(environmentRequest, workspaceId);
        assertNotNull(response);
    }

    private void initMocks() throws TransactionExecutionException {
        when(conversionService.convert(any(EnvironmentRequest.class), eq(Environment.class))).thenReturn(new Environment());
        when(ldapConfigService.findByNamesInWorkspace(anySet(), anyLong())).thenReturn(Collections.emptySet());
        when(rdsConfigService.findByNamesInWorkspace(anySet(), anyLong())).thenReturn(Collections.emptySet());
        when(proxyConfigService.findByNamesInWorkspace(anySet(), anyLong())).thenReturn(Collections.emptySet());
        when(environmentCreationValidator.validate(any())).thenReturn(ValidationResult.builder().build());
        Workspace workspace = new Workspace();
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(new CloudbreakUser("", "", ""));
        when(userService.getOrCreate(any())).thenReturn(new User());
        when(transactionService.required(any())).thenReturn(new Environment());
        when(conversionService.convert(any(Environment.class), eq(DetailedEnvironmentResponse.class))).thenReturn(new DetailedEnvironmentResponse());
    }

}