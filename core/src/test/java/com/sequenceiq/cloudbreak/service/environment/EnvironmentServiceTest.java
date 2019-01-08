package com.sequenceiq.cloudbreak.service.environment;

import static com.sequenceiq.cloudbreak.cloud.model.Coordinate.coordinate;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.convert.ConversionService;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.cloudbreak.api.model.CredentialRequest;
import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentDetachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.LocationRequest;
import com.sequenceiq.cloudbreak.api.model.environment.response.DetailedEnvironmentResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentCreationValidator;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentDetachValidator;
import com.sequenceiq.cloudbreak.converter.KerberosConfigToKerberosV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.RDSConfigToDatabaseV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.environment.EnvironmentToDetailedEnvironmentResponseConverter;
import com.sequenceiq.cloudbreak.converter.environment.RegionConverter;
import com.sequenceiq.cloudbreak.converter.stack.StackApiViewToStackViewResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.ldaps.LdapConfigToLdapV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.proxies.ProxyConfigToProxyV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.environment.Environment;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentRepository;
import com.sequenceiq.cloudbreak.service.KubernetesConfigService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosService;
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

    private static final Long ENVIRONMENT_ID = 1L;

    private static final String CREDENTIAL_NAME = "cred1";

    private static final String ENVIRONMENT_NAME = "EnvName";

    @Rule
    public final ExpectedException exceptionRule = ExpectedException.none();

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private KubernetesConfigService kubernetesConfigService;

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private ProxyConfigService proxyConfigService;

    @Mock
    private KerberosService kerberosService;

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

    @Spy
    private EnvironmentDetachValidator environmentDetachValidator = new EnvironmentDetachValidator();

    private ArgumentCaptor<StackApiView> stackApiViewCaptor = ArgumentCaptor.forClass(StackApiView.class);

    @InjectMocks
    private EnvironmentService environmentService;

    private final Workspace workspace = new Workspace();

    @Spy
    private RegionConverter regionConverter = new RegionConverter();

    @InjectMocks
    private EnvironmentToDetailedEnvironmentResponseConverter environmentConverter;

    @InjectMocks
    private LdapConfigToLdapV4ResponseConverter ldapConfigResponseConverter;

    @InjectMocks
    private ProxyConfigToProxyV4ResponseConverter proxyConfigResponseConverter;

    @InjectMocks
    private RDSConfigToDatabaseV4ResponseConverter rdsConfigResponseConverter;

    @InjectMocks
    private KerberosConfigToKerberosV4ResponseConverter kerberosConfigResponseConverter;

    @InjectMocks
    private StackApiViewToStackViewResponseConverter stackApiViewToStackViewResponseConverter;

    @Before
    public void setup() throws TransactionExecutionException {
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any());
        workspace.setId(WORKSPACE_ID);
        workspace.setName("ws");
        when(ldapConfigService.findByNamesInWorkspace(anySet(), anyLong())).thenReturn(Collections.emptySet());
        when(rdsConfigService.findByNamesInWorkspace(anySet(), anyLong())).thenReturn(Collections.emptySet());
        when(proxyConfigService.findByNamesInWorkspace(anySet(), anyLong())).thenReturn(Collections.emptySet());
        when(kerberosService.findByNamesInWorkspace(anySet(), anyLong())).thenReturn(Collections.emptySet());
        when(environmentCreationValidator.validate(any(), any(), anyBoolean())).thenReturn(ValidationResult.builder().build());
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(new CloudbreakUser("", "", "", ""));
        when(userService.getOrCreate(any())).thenReturn(new User());
        when(conversionService.convert(any(Environment.class), eq(DetailedEnvironmentResponse.class))).thenReturn(new DetailedEnvironmentResponse());
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(workspaceService.retrieveForUser(any())).thenReturn(Set.of(workspace));
        assertNotNull(regionConverter);
        assertNotNull(environmentDetachValidator);
    }

    @Test
    public void testCreateWithCredentialName() {
        EnvironmentRequest environmentRequest = new EnvironmentRequest();
        environmentRequest.setName(ENVIRONMENT_NAME);

        environmentRequest.setCredentialName(CREDENTIAL_NAME);
        CredentialRequest credentialRequest = new CredentialRequest();
        credentialRequest.setName("IgnoredCredRequestName");
        environmentRequest.setCredential(credentialRequest);
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setLocationName("region1");
        environmentRequest.setLocation(locationRequest);

        when(cloudParameterCache.areRegionsSupported(any())).thenReturn(false);
        when(conversionService.convert(any(EnvironmentRequest.class), eq(Environment.class))).thenReturn(new Environment());
        when(environmentRepository.save(any(Environment.class))).thenReturn(new Environment());
        when(environmentCredentialOperationService.getCredentialFromRequest(any(), anyLong())).thenReturn(new Credential());
        CloudRegions cloudRegions = new CloudRegions();
        cloudRegions.setCloudRegions(Map.of(region("region1"), List.of(), region("region2"), List.of()));
        cloudRegions.setDisplayNames(Map.of(region("region1"), "display1", region("region2"), "display2"));
        cloudRegions.setCoordinates(Map.of(
                region("region1"), coordinate("1", "2", "region1"),
                region("region2"), coordinate("1", "2", "region2")));
        when(platformParameterService.getRegionsByCredential(any())).thenReturn(cloudRegions);
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
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setLocationName("region1");
        environmentRequest.setLocation(locationRequest);

        when(cloudParameterCache.areRegionsSupported(any())).thenReturn(false);
        when(conversionService.convert(any(EnvironmentRequest.class), eq(Environment.class))).thenReturn(new Environment());
        when(environmentRepository.save(any(Environment.class))).thenReturn(new Environment());
        when(environmentCredentialOperationService.getCredentialFromRequest(any(), anyLong())).thenReturn(new Credential());
        CloudRegions cloudRegions = new CloudRegions();
        cloudRegions.setCloudRegions(Map.of(region("region1"), List.of(), region("region2"), List.of()));
        cloudRegions.setDisplayNames(Map.of(region("region1"), "display1", region("region2"), "display2"));
        cloudRegions.setCoordinates(Map.of(
                region("region1"), coordinate("1", "2", "region1"),
                region("region2"), coordinate("1", "2", "region2")));
        when(platformParameterService.getRegionsByCredential(any())).thenReturn(cloudRegions);
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
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setLocationName("region1");
        environmentRequest.setLocation(locationRequest);

        when(conversionService.convert(any(EnvironmentRequest.class), eq(Environment.class))).thenReturn(new Environment());
        ArgumentCaptor<Environment> envCaptor = ArgumentCaptor.forClass(Environment.class);
        when(environmentRepository.save(envCaptor.capture())).thenReturn(new Environment());
        when(environmentCredentialOperationService.getCredentialFromRequest(any(), anyLong())).thenReturn(new Credential());

        CloudRegions cloudRegions = new CloudRegions();
        cloudRegions.setCloudRegions(Map.of(region("region1"), List.of(), region("region2"), List.of()));
        cloudRegions.setDisplayNames(Map.of(region("region1"), "display1", region("region2"), "display2"));
        cloudRegions.setCoordinates(Map.of(
                region("region1"), coordinate("1", "2", "region1"),
                region("region2"), coordinate("1", "2", "region2")));
        when(platformParameterService.getRegionsByCredential(any())).thenReturn(cloudRegions);
        when(cloudParameterCache.areRegionsSupported(any())).thenReturn(true);
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
        String kdcName1 = "kdc1";
        String kdcName2 = "kdc2";

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
        rds1.setDatabaseEngine(DatabaseVendor.POSTGRES);
        RDSConfig rds2 = new RDSConfig();
        rds2.setId(2L);
        rds2.setName(rdsName2);
        rds2.setDatabaseEngine(DatabaseVendor.POSTGRES);
        environment.setRdsConfigs(Sets.newHashSet(rds1, rds2));
        KerberosConfig kdc1 = new KerberosConfig();
        kdc1.setId(1L);
        kdc1.setName(kdcName1);
        KerberosConfig kdc2 = new KerberosConfig();
        kdc2.setId(2L);
        kdc2.setName(kdcName2);
        environment.setKerberosConfigs(Sets.newHashSet(kdc1, kdc2));

        setCredential(environment);

        when(environmentRepository.findByNameAndWorkspaceId(ENVIRONMENT_NAME, WORKSPACE_ID)).thenReturn(environment);
        when(environmentRepository.save(any(Environment.class)))
                .thenAnswer((Answer<Environment>) invocation -> (Environment) invocation.getArgument(0));
        mockConverters();

        EnvironmentDetachRequest detachRequest = new EnvironmentDetachRequest();
        detachRequest.getLdapConfigs().add(ldapName1);
        detachRequest.getLdapConfigs().add(notAttachedLdap);
        detachRequest.getProxyConfigs().add(proxyName1);
        detachRequest.getRdsConfigs().add(rdsName1);
        detachRequest.getKerberosConfigs().add(kdcName1);

        DetailedEnvironmentResponse detachResponse = environmentService.detachResources(ENVIRONMENT_NAME, detachRequest, WORKSPACE_ID);

        assertFalse(detachResponse.getLdapConfigs().stream().map(o -> o.getName()).collect(Collectors.toSet()).contains(notAttachedLdap));
        assertFalse(detachResponse.getLdapConfigs().stream().map(o -> o.getName()).collect(Collectors.toSet()).contains(ldapName1));
        assertFalse(detachResponse.getProxyConfigs().stream().map(o -> o.getName()).collect(Collectors.toSet()).contains(proxyName1));
        assertFalse(detachResponse.getRdsConfigs().stream().map(o -> o.getName()).collect(Collectors.toSet()).contains(rdsName1));
        assertFalse(detachResponse.getKerberosConfigs().stream().map(o -> o.getName()).collect(Collectors.toSet()).contains(kdcName1));
        assertTrue(detachResponse.getLdapConfigs().stream().map(o -> o.getName()).collect(Collectors.toSet()).contains(ldapName2));
        assertTrue(detachResponse.getProxyConfigs().stream().map(o -> o.getName()).collect(Collectors.toSet()).contains(proxyName2));
        assertTrue(detachResponse.getRdsConfigs().stream().map(o -> o.getName()).collect(Collectors.toSet()).contains(rdsName2));
        assertTrue(detachResponse.getKerberosConfigs().stream().map(o -> o.getName()).collect(Collectors.toSet()).contains(kdcName2));
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
        stackApiView1.setName("name1");
        StackApiView stackApiView2 = new StackApiView();
        stackApiView2.setName("name2");
        workloadClusters.add(stackApiView1);
        workloadClusters.add(stackApiView2);
        environment.setStacks(workloadClusters);
        String credentialName2 = "credential2";
        Credential credential2 = new Credential();
        credential2.setName(credentialName2);
        when(environmentCredentialOperationService.validatePlatformAndGetCredential(any(), any(), anyLong())).thenReturn(credential2);
        when(stackApiViewService.canChangeCredential(any())).thenReturn(true);
        when(environmentRepository.save(any(Environment.class)))
                .thenAnswer((Answer<Environment>) invocation -> (Environment) invocation.getArgument(0));
        when(conversionService.convert(any(Environment.class), eq(DetailedEnvironmentResponse.class)))
                .thenAnswer((Answer<DetailedEnvironmentResponse>) invocation -> environmentConverter.convert((Environment) invocation.getArgument(0)));
        when(conversionService.convert(any(StackApiView.class), eq(StackViewResponse.class)))
                .thenAnswer(invocation -> stackApiViewToStackViewResponseConverter.convert((StackApiView) invocation.getArgument(0)));

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
        environment.setStacks(workloadClusters);
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
        environment.setStacks(workloadClusters);
        when(environmentCredentialOperationService.validatePlatformAndGetCredential(any(), any(), anyLong()))
                .thenThrow(new BadRequestException(""));

        EnvironmentChangeCredentialRequest request = new EnvironmentChangeCredentialRequest();
        request.setCredentialName("credential2");

        environmentService.changeCredential(ENVIRONMENT_NAME, WORKSPACE_ID, request);
    }

    @Test
    public void testDetachHappyPath() {
        Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        setCredential(environment);

        LdapConfig ldap1 = new LdapConfig();
        ldap1.setId(1L);
        String ldapName1 = "ldap1";
        ldap1.setName(ldapName1);

        LdapConfig ldap2 = new LdapConfig();
        ldap2.setId(2L);
        String ldapName2 = "ldap2";
        ldap2.setName(ldapName2);

        LdapConfig ldap3 = new LdapConfig();
        ldap3.setId(3L);
        String ldapName3 = "ldap3";
        ldap3.setName(ldapName3);

        ProxyConfig proxy1 = new ProxyConfig();
        proxy1.setId(1L);
        String proxyName1 = "proxy1";
        proxy1.setName(proxyName1);

        ProxyConfig proxy2 = new ProxyConfig();
        proxy2.setId(2L);
        String proxyName2 = "proxy2";
        proxy2.setName(proxyName2);

        RDSConfig rds1 = new RDSConfig();
        rds1.setId(1L);
        String rdsName1 = "rds1";
        rds1.setName(rdsName1);
        rds1.setDatabaseEngine(DatabaseVendor.POSTGRES);

        RDSConfig rds2 = new RDSConfig();
        rds2.setId(2L);
        String rdsName2 = "rds2";
        rds2.setName(rdsName2);
        rds2.setDatabaseEngine(DatabaseVendor.POSTGRES);

        KerberosConfig kdc1 = new KerberosConfig();
        kdc1.setId(1L);
        String kdcName1 = "kdc1";
        kdc1.setName(kdcName1);

        KerberosConfig kdc2 = new KerberosConfig();
        kdc2.setId(2L);
        String kdcName2 = "kdc2";
        kdc2.setName(kdcName2);

        environment.setLdapConfigs(Sets.newHashSet(ldap1, ldap2, ldap3));
        environment.setProxyConfigs(Sets.newHashSet(proxy1, proxy2));
        environment.setRdsConfigs(Sets.newHashSet(rds1, rds2));
        environment.setKerberosConfigs(Sets.newHashSet(kdc1, kdc2));

        EnvironmentDetachRequest request = new EnvironmentDetachRequest();
        request.setLdapConfigs(Sets.newHashSet(ldapName1, ldapName2));
        request.setProxyConfigs(Sets.newHashSet(proxyName1));
        request.setRdsConfigs(Sets.newHashSet(rdsName1));
        request.setKerberosConfigs(Sets.newHashSet(kdcName1));

        when(environmentRepository.findByNameAndWorkspaceId(ENVIRONMENT_NAME, WORKSPACE_ID)).thenReturn(environment);
        when(environmentRepository.save(any(Environment.class)))
                .thenAnswer((Answer<Environment>) invocation -> (Environment) invocation.getArgument(0));
        mockConverters();

        DetailedEnvironmentResponse result = environmentService.detachResources(ENVIRONMENT_NAME, request, WORKSPACE_ID);

        assertEquals(1, result.getLdapConfigs().size());
        assertEquals(1, result.getProxyConfigs().size());
        assertEquals(1, result.getRdsConfigs().size());
        assertEquals(1, result.getKerberosConfigs().size());
        assertEquals(ldapName3, result.getLdapConfigs().iterator().next().getName());
        assertEquals(proxyName2, result.getProxyConfigs().iterator().next().getName());
        assertEquals(rdsName2, result.getRdsConfigs().iterator().next().getName());
        assertEquals(kdcName2, result.getKerberosConfigs().iterator().next().getName());
    }

    @Test
    public void testDetachWithUsedResources() {
        Environment environment = new Environment();
        environment.setId(ENVIRONMENT_ID);
        environment.setName(ENVIRONMENT_NAME);
        setCredential(environment);

        LdapConfig ldap1 = new LdapConfig();
        ldap1.setId(1L);
        String ldapName1 = "ldap1";
        ldap1.setName(ldapName1);

        LdapConfig ldap2 = new LdapConfig();
        ldap2.setId(2L);
        String ldapName2 = "ldap2";
        ldap2.setName(ldapName2);

        LdapConfig ldap3 = new LdapConfig();
        ldap3.setId(3L);
        String ldapName3 = "ldap3";
        ldap3.setName(ldapName3);

        ProxyConfig proxy1 = new ProxyConfig();
        proxy1.setId(1L);
        String proxyName1 = "proxy1";
        proxy1.setName(proxyName1);

        ProxyConfig proxy2 = new ProxyConfig();
        proxy2.setId(2L);
        String proxyName2 = "proxy2";
        proxy2.setName(proxyName2);

        RDSConfig rds1 = new RDSConfig();
        rds1.setId(1L);
        String rdsName1 = "rds1";
        rds1.setName(rdsName1);
        rds1.setDatabaseEngine(DatabaseVendor.POSTGRES);

        RDSConfig rds2 = new RDSConfig();
        rds2.setId(2L);
        String rdsName2 = "rds2";
        rds2.setName(rdsName2);
        rds2.setDatabaseEngine(DatabaseVendor.POSTGRES);

        KerberosConfig kdc1 = new KerberosConfig();
        kdc1.setId(1L);
        String kdcName1 = "kdc1";
        kdc1.setName(kdcName1);

        KerberosConfig kdc2 = new KerberosConfig();
        kdc2.setId(2L);
        String kdcName2 = "kdc2";
        kdc2.setName(kdcName2);

        Cluster cluster1 = new Cluster();
        cluster1.setId(1L);
        String clusterName1 = "cluster1";
        cluster1.setName(clusterName1);

        Cluster cluster2 = new Cluster();
        cluster2.setId(2L);
        String clusterName2 = "cluster2";
        cluster2.setName(clusterName2);

        environment.setLdapConfigs(Sets.newHashSet(ldap1, ldap2, ldap3));
        environment.setProxyConfigs(Sets.newHashSet(proxy1, proxy2));
        environment.setRdsConfigs(Sets.newHashSet(rds1, rds2));
        environment.setKerberosConfigs(Sets.newHashSet(kdc1, kdc2));

        EnvironmentDetachRequest request = new EnvironmentDetachRequest();
        request.setLdapConfigs(Sets.newHashSet(ldapName1, ldapName2));
        request.setProxyConfigs(Sets.newHashSet(proxyName1));
        request.setRdsConfigs(Sets.newHashSet(rdsName1));
        request.setKerberosConfigs(Sets.newHashSet(kdcName1));

        when(environmentRepository.findByNameAndWorkspaceId(ENVIRONMENT_NAME, WORKSPACE_ID)).thenReturn(environment);
        when(ldapConfigService.getClustersUsingResourceInEnvironment(ldap1, ENVIRONMENT_ID)).thenReturn(Sets.newHashSet(cluster1));
        when(ldapConfigService.getClustersUsingResourceInEnvironment(ldap2, ENVIRONMENT_ID)).thenReturn(Sets.newHashSet(cluster1, cluster2));
        when(proxyConfigService.getClustersUsingResourceInEnvironment(proxy1, ENVIRONMENT_ID)).thenReturn(Sets.newHashSet(cluster1));
        when(rdsConfigService.getClustersUsingResourceInEnvironment(rds1, ENVIRONMENT_ID)).thenReturn(Sets.newHashSet(cluster1));
        when(kerberosService.getClustersUsingResourceInEnvironment(kdc1, ENVIRONMENT_ID)).thenReturn(Sets.newHashSet(cluster1));

        exceptionRule.expect(BadRequestException.class);
        exceptionRule.expectMessage(String.format("RDS config '%s' cannot be detached from environment 'EnvName' "
                + "because it is used by the following cluster(s): [%s]", rdsName1, clusterName1));
        exceptionRule.expectMessage(String.format("Proxy config '%s' cannot be detached from environment 'EnvName' "
                + "because it is used by the following cluster(s): [%s]", proxyName1, clusterName1));
        exceptionRule.expectMessage(String.format("LDAP config '%s' cannot be detached from environment 'EnvName' "
                + "because it is used by the following cluster(s): [%s]", ldapName1, clusterName1));
        exceptionRule.expectMessage(String.format("LDAP config '%s' cannot be detached from environment 'EnvName' "
                + "because it is used by the following cluster(s): [%s, %s]", ldapName2, clusterName1, clusterName2));
        exceptionRule.expectMessage(String.format("Kerberos Config '%s' cannot be detached from environment 'EnvName' "
                + "because it is used by the following cluster(s): [%s]", kdcName1, clusterName1));

        environmentService.detachResources(ENVIRONMENT_NAME, request, WORKSPACE_ID);
    }

    private void setCredential(Environment environment) {
        Credential credential = new Credential();
        credential.setName("credential1");
        environment.setCredential(credential);
        environment.setStacks(new HashSet<>());
    }

    private void mockConverters() {
        when(conversionService.convert(any(Environment.class), eq(DetailedEnvironmentResponse.class)))
                .thenAnswer((Answer<DetailedEnvironmentResponse>) invocation -> environmentConverter.convert((Environment) invocation.getArgument(0)));
        when(conversionService.convert(any(LdapConfig.class), eq(LdapV4Response.class)))
                .thenAnswer((Answer<LdapV4Response>) invocation -> ldapConfigResponseConverter.convert((LdapConfig) invocation.getArgument(0)));
        when(conversionService.convert(any(ProxyConfig.class), eq(ProxyV4Response.class)))
                .thenAnswer((Answer<ProxyV4Response>) invocation -> proxyConfigResponseConverter.convert((ProxyConfig) invocation.getArgument(0)));
        when(conversionService.convert(any(RDSConfig.class), eq(DatabaseV4Response.class)))
                .thenAnswer((Answer<DatabaseV4Response>) invocation -> rdsConfigResponseConverter.convert((RDSConfig) invocation.getArgument(0)));
        when(conversionService.convert(any(KerberosConfig.class), eq(KerberosV4Response.class)))
                .thenAnswer((Answer<KerberosV4Response>) invocation -> kerberosConfigResponseConverter.convert((KerberosConfig) invocation.getArgument(0)));
    }
}