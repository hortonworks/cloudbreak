package com.sequenceiq.cloudbreak.service.environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.convert.ConversionService;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentDetachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentRequest;
import com.sequenceiq.cloudbreak.api.model.environment.response.DetailedEnvironmentResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentCreationValidator;
import com.sequenceiq.cloudbreak.converter.environment.EnvironmentToDetailedEnvironmentResponseConverter;
import com.sequenceiq.cloudbreak.converter.environment.RegionConverter;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentRepository;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.platform.PlatformParameterService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.stack.StackApiViewService;
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
    private EnvironmentCredentialOperationService environmentCredentialOperationService;

    @Mock
    private EnvironmentCreationValidator environmentCreationValidator;

    @Mock
    private StackApiViewService stackApiViewService;

    @Mock
    private ConversionService conversionService;

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private CloudParameterCache cloudParameterCache;

    @Mock
    private PlatformParameterService platformParameterService;

    @Mock
    private TransactionService transactionService;

    private ArgumentCaptor<StackApiView> stackApiViewCaptor = ArgumentCaptor.forClass(StackApiView.class);

    @InjectMocks
    private EnvironmentService environmentService;

    private final Workspace workspace = new Workspace();

    @Spy
    private RegionConverter regionConverter = new RegionConverter();

    @InjectMocks
    private EnvironmentToDetailedEnvironmentResponseConverter environmentConverter;

    @Before
    public void setup() throws TransactionExecutionException {
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any());
        workspace.setId(WORKSPACE_ID);
        workspace.setName("ws");
        when(ldapConfigService.findByNamesInWorkspace(anySet(), anyLong())).thenReturn(Collections.emptySet());
        when(rdsConfigService.findByNamesInWorkspace(anySet(), anyLong())).thenReturn(Collections.emptySet());
        when(proxyConfigService.findByNamesInWorkspace(anySet(), anyLong())).thenReturn(Collections.emptySet());
        when(environmentCreationValidator.validate(any(), any(), anyBoolean())).thenReturn(ValidationResult.builder().build());
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(new CloudbreakUser("", "", ""));
        when(userService.getOrCreate(any())).thenReturn(new User());
        when(conversionService.convert(any(Environment.class), eq(DetailedEnvironmentResponse.class))).thenReturn(new DetailedEnvironmentResponse());
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(workspaceService.retrieveForUser(any())).thenReturn(Set.of(workspace));
        assertNotNull(regionConverter);
    }

    @Test
    public void testCreateWithCredentialName() {
        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        environmentRequest.setName(ENVIRONMENT_NAME);

        environmentRequest.setCredentialName(CREDENTIAL_NAME);
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setName("IgnoredCredRequestName");
        environmentRequest.setCredential(credentialRequest);

        when(cloudParameterCache.areRegionsSupported(any())).thenReturn(false);
        when(conversionService.convert(any(EnvironmentRequest.class), eq(Environment.class))).thenReturn(new Environment());
        when(environmentRepository.save(any(Environment.class))).thenReturn(new Environment());
        when(environmentCredentialOperationService.getCredentialFromRequest(any(), anyLong())).thenReturn(new Credential());

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

        when(cloudParameterCache.areRegionsSupported(any())).thenReturn(false);
        when(conversionService.convert(any(EnvironmentRequest.class), eq(Environment.class))).thenReturn(new Environment());
        when(environmentRepository.save(any(Environment.class))).thenReturn(new Environment());
        when(environmentCredentialOperationService.getCredentialFromRequest(any(), anyLong())).thenReturn(new Credential());

        DetailedEnvironmentResponse response = environmentService.createForLoggedInUser(environmentRequest, WORKSPACE_ID);

        assertNotNull(response);
    }

    @Test
    public void testCreateWithRegions() {
        // GIVEN
        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        environmentRequest.setName(ENVIRONMENT_NAME);

        environmentRequest.setCredentialName(CREDENTIAL_NAME);
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setName("IgnoredCredRequestName");
        environmentRequest.setCredential(credentialRequest);
        environmentRequest.setRegions(Set.of("region1", "region2"));

        when(conversionService.convert(any(EnvironmentRequest.class), eq(Environment.class))).thenReturn(new Environment());
        ArgumentCaptor<Environment> envCaptor = ArgumentCaptor.forClass(Environment.class);
        when(environmentRepository.save(envCaptor.capture())).thenReturn(new Environment());
        when(environmentCredentialOperationService.getCredentialFromRequest(any(), anyLong())).thenReturn(new Credential());

        CloudRegions cloudRegions = new CloudRegions();
        cloudRegions.setCloudRegions(Map.of(Region.region("region1"), List.of(), Region.region("region2"), List.of()));
        cloudRegions.setDisplayNames(Map.of(Region.region("region1"), "display1", Region.region("region2"), "display2"));
        when(cloudParameterCache.areRegionsSupported(any())).thenReturn(true);
        when(platformParameterService.getRegionsByCredential(any())).thenReturn(cloudRegions);
        // WHEN
        DetailedEnvironmentResponse response = environmentService.createForLoggedInUser(environmentRequest, WORKSPACE_ID);
        // THEN
        assertNotNull(response);
        Set<com.sequenceiq.cloudbreak.domain.environment.Region> regions = envCaptor.getValue().getRegionSet();
        assertEquals(2, regions.size());
        List<com.sequenceiq.cloudbreak.domain.environment.Region> orderedRegions =
                regions.stream().sorted(Comparator.comparing(com.sequenceiq.cloudbreak.domain.environment.Region::getName)).collect(Collectors.toList());
        assertEquals("region1", orderedRegions.get(0).getName());
        assertEquals("display1", orderedRegions.get(0).getDisplayName());
        assertEquals("region2", orderedRegions.get(1).getName());
        assertEquals("display2", orderedRegions.get(1).getDisplayName());
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
        environment.setWorkloadClusters(new HashSet<>());

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

    @Test
    public void testChangeCredentialHappyPath() {
        Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        when(environmentRepository.findByNameAndWorkspaceId(ENVIRONMENT_NAME, WORKSPACE_ID)).thenReturn(environment);

        String credentialName1 = "credential1";
        Credential credential1 = new Credential();
        credential1.setName(credentialName1);
        environment.setCredential(credential1);
        Set<StackApiView> workloadClusters = new HashSet<>();
        StackApiView stackApiView1 = new StackApiView();
        StackApiView stackApiView2 = new StackApiView();
        workloadClusters.add(stackApiView1);
        workloadClusters.add(stackApiView2);
        environment.setWorkloadClusters(workloadClusters);
        String credentialName2 = "credential2";
        Credential credential2 = new Credential();
        credential2.setName(credentialName2);
        when(environmentCredentialOperationService.validatePlatformAndGetCredential(any(), any(), anyLong())).thenReturn(credential2);
        when(stackApiViewService.canChangeCredential(any())).thenReturn(true);
        when(environmentRepository.save(any(Environment.class)))
                .thenAnswer((Answer<Environment>) invocation -> (Environment) invocation.getArgument(0));
        when(conversionService.convert(any(Environment.class), eq(DetailedEnvironmentResponse.class)))
                .thenAnswer((Answer<DetailedEnvironmentResponse>) invocation -> environmentConverter.convert((Environment) invocation.getArgument(0)));


        EnvironmentChangeCredentialRequest request = new EnvironmentChangeCredentialRequest();
        request.setCredentialName(credentialName2);

        DetailedEnvironmentResponse response = environmentService.changeCredential(ENVIRONMENT_NAME, WORKSPACE_ID, request);

        verify(stackApiViewService, times(2)).save(stackApiViewCaptor.capture());
        assertEquals(ENVIRONMENT_NAME, response.getName());
        assertEquals(credentialName2, response.getCredentialName());
    }

    @Test(expected = BadRequestException.class)
    public void testChangeCredentialWithUnchangableStack() {
        Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        when(environmentRepository.findByNameAndWorkspaceId(ENVIRONMENT_NAME, WORKSPACE_ID)).thenReturn(environment);

        String credentialName1 = "credential1";
        Credential credential1 = new Credential();
        credential1.setName(credentialName1);
        environment.setCredential(credential1);
        Set<StackApiView> workloadClusters = new HashSet<>();
        StackApiView stackApiView1 = new StackApiView();
        StackApiView stackApiView2 = new StackApiView();
        workloadClusters.add(stackApiView1);
        workloadClusters.add(stackApiView2);
        environment.setWorkloadClusters(workloadClusters);
        String credentialName2 = "credential2";
        Credential credential2 = new Credential();
        credential2.setName(credentialName2);
        when(environmentCredentialOperationService.validatePlatformAndGetCredential(any(), any(), anyLong())).thenReturn(credential2);
        when(stackApiViewService.canChangeCredential(stackApiView1)).thenReturn(true);
        when(stackApiViewService.canChangeCredential(stackApiView2)).thenReturn(false);

        EnvironmentChangeCredentialRequest request = new EnvironmentChangeCredentialRequest();
        request.setCredentialName(credentialName2);

        environmentService.changeCredential(ENVIRONMENT_NAME, WORKSPACE_ID, request);
    }

    @Test(expected = BadRequestException.class)
    public void testChangeCredentialWithInvalidCloudPlatform() {
        Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        when(environmentRepository.findByNameAndWorkspaceId(ENVIRONMENT_NAME, WORKSPACE_ID)).thenReturn(environment);

        String credentialName1 = "credential1";
        Credential credential1 = new Credential();
        credential1.setName(credentialName1);
        environment.setCredential(credential1);
        Set<StackApiView> workloadClusters = new HashSet<>();
        StackApiView stackApiView1 = new StackApiView();
        StackApiView stackApiView2 = new StackApiView();
        workloadClusters.add(stackApiView1);
        workloadClusters.add(stackApiView2);
        environment.setWorkloadClusters(workloadClusters);
        when(environmentCredentialOperationService.validatePlatformAndGetCredential(any(), any(), anyLong()))
                .thenThrow(new BadRequestException(""));

        EnvironmentChangeCredentialRequest request = new EnvironmentChangeCredentialRequest();
        request.setCredentialName("credential2");

        environmentService.changeCredential(ENVIRONMENT_NAME, WORKSPACE_ID, request);
    }
}