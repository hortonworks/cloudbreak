package com.sequenceiq.cloudbreak.service.environment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.convert.ConversionService;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentDetachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentRequest;
import com.sequenceiq.cloudbreak.api.model.environment.response.DetailedEnvironmentResponse;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentCreationValidator;
import com.sequenceiq.cloudbreak.converter.environment.EnvironmentToDetailedEnvironmentResponseConverter;
import com.sequenceiq.cloudbreak.converter.users.WorkspaceToWorkspaceResourceResponseConverter;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentRepository;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String CREDENTIAL_NAME = "cred1";

    private static final String ENVIRONMENT_NAME = "EnvName";

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private RestRequestThreadLocalService restRequestThreadLocalService;

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

    @Mock
    private EnvironmentRepository environmentRepository;

    @InjectMocks
    private EnvironmentService environmentService;

    private final Workspace workspace = new Workspace();

    @InjectMocks
    private EnvironmentToDetailedEnvironmentResponseConverter environmentConverter;

    private final WorkspaceToWorkspaceResourceResponseConverter workspaceConverter = new WorkspaceToWorkspaceResourceResponseConverter();

    @Before
    public void setup() {
        workspace.setId(WORKSPACE_ID);
        workspace.setName("ws");
        when(ldapConfigService.findByNamesInWorkspace(anySet(), anyLong())).thenReturn(Collections.emptySet());
        when(rdsConfigService.findByNamesInWorkspace(anySet(), anyLong())).thenReturn(Collections.emptySet());
        when(proxyConfigService.findByNamesInWorkspace(anySet(), anyLong())).thenReturn(Collections.emptySet());
        when(environmentCreationValidator.validate(any())).thenReturn(ValidationResult.builder().build());
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(new CloudbreakUser("", "", "", ""));
        when(userService.getOrCreate(any())).thenReturn(new User());
        when(conversionService.convert(any(Environment.class), eq(DetailedEnvironmentResponse.class))).thenReturn(new DetailedEnvironmentResponse());
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(workspaceService.retrieveForUser(any())).thenReturn(Set.of(workspace));

    }

    @Test
    public void testCreateWithCredentialName() {
        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        environmentRequest.setName(ENVIRONMENT_NAME);

        environmentRequest.setCredentialName(CREDENTIAL_NAME);
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setName("IgnoredCredRequestName");
        environmentRequest.setCredential(credentialRequest);

        when(conversionService.convert(any(EnvironmentRequest.class), eq(Environment.class))).thenReturn(new Environment());
        when(environmentRepository.save(any(Environment.class))).thenReturn(new Environment());
        when(credentialService.getByNameForWorkspaceId(CREDENTIAL_NAME, WORKSPACE_ID)).thenReturn(new Credential());

        DetailedEnvironmentResponse response = environmentService.createForLoggedInUser(environmentRequest, WORKSPACE_ID);

        assertNotNull(response);
    }

    @Test
    public void testCreateWithCredential() {
        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        environmentRequest.setName(ENVIRONMENT_NAME);

        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setName("CredRequestName");
        environmentRequest.setCredential(credentialRequest);

        when(conversionService.convert(any(EnvironmentRequest.class), eq(Environment.class))).thenReturn(new Environment());
        when(environmentRepository.save(any(Environment.class))).thenReturn(new Environment());
        when(credentialService.createForLoggedInUser(any(), anyLong())).thenReturn(new Credential());
        when(conversionService.convert(any(CredentialRequest.class), eq(Credential.class))).thenReturn(new Credential());

        DetailedEnvironmentResponse response = environmentService.createForLoggedInUser(environmentRequest, WORKSPACE_ID);

        assertNotNull(response);
    }

    @Test
    public void testDetach() {
        String notAttachedLdap = "not-attached-ldap";
        String ldapName1 = "ldap1";
        String proxyName1 = "proxy1";
        String rdsName1 = "rds1";
        String ldapName2 = "ldap2";
        String proxyName2 = "proxy2";
        String rdsName2 = "rds2";

        Environment environment = new Environment();
        LdapConfig ldap1 = new LdapConfig();
        ldap1.setId(1L);
        ldap1.setName(ldapName1);
        LdapConfig ldap2 = new LdapConfig();
        ldap2.setId(2L);
        ldap2.setName(ldapName2);
        environment.setLdapConfigs(Sets.newHashSet(ldap1, ldap2));
        ProxyConfig proxy1 = new ProxyConfig();
        proxy1.setId(1L);
        proxy1.setName(proxyName1);
        ProxyConfig proxy2 = new ProxyConfig();
        proxy2.setId(2L);
        proxy2.setName(proxyName2);
        environment.setProxyConfigs(Sets.newHashSet(proxy1, proxy2));
        RDSConfig rds1 = new RDSConfig();
        rds1.setId(1L);
        rds1.setName(rdsName1);
        RDSConfig rds2 = new RDSConfig();
        rds2.setId(2L);
        rds2.setName(rdsName2);
        environment.setRdsConfigs(Sets.newHashSet(rds1, rds2));

        Credential credential = new Credential();
        credential.setName("credential1");
        environment.setCredential(credential);

        when(environmentRepository.findByNameAndWorkspaceId(ENVIRONMENT_NAME, WORKSPACE_ID)).thenReturn(environment);
        when(environmentRepository.save(any(Environment.class)))
                .thenAnswer((Answer<Environment>) invocation -> (Environment) invocation.getArgument(0));
        when(conversionService.convert(any(Environment.class), eq(DetailedEnvironmentResponse.class)))
                .thenAnswer((Answer<DetailedEnvironmentResponse>) invocation -> environmentConverter.convert((Environment) invocation.getArgument(0)));

        EnvironmentDetachRequest detachRequest = new EnvironmentDetachRequest();
        detachRequest.getLdapConfigs().add(ldapName1);
        detachRequest.getLdapConfigs().add(notAttachedLdap);
        detachRequest.getProxyConfigs().add(proxyName1);
        detachRequest.getRdsConfigs().add(rdsName1);

        DetailedEnvironmentResponse detachResponse = environmentService.detachResources(ENVIRONMENT_NAME, detachRequest, WORKSPACE_ID);

        assertFalse(detachResponse.getLdapConfigs().contains(notAttachedLdap));
        assertFalse(detachResponse.getLdapConfigs().contains(ldapName1));
        assertFalse(detachResponse.getProxyConfigs().contains(proxyName1));
        assertFalse(detachResponse.getRdsConfigs().contains(rdsName1));
        assertTrue(detachResponse.getLdapConfigs().contains(ldapName2));
        assertTrue(detachResponse.getProxyConfigs().contains(proxyName2));
        assertTrue(detachResponse.getRdsConfigs().contains(rdsName2));
    }
}