package com.sequenceiq.cloudbreak.service.environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.Optional;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.requests.CredentialV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.base.EnvironmentNetworkAwsV4Params;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.base.EnvironmentNetworkAzureV4Params;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentChangeCredentialV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentDetachV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentEditV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentNetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.LocationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.DetailedEnvironmentV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.LocationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4ResponseBase;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.LdapV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.responses.LdapV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.ProxyV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentCreationValidator;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentDetachValidator;
import com.sequenceiq.cloudbreak.controller.validation.environment.EnvironmentRegionValidator;
import com.sequenceiq.cloudbreak.converter.v4.database.RDSConfigToDatabaseV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.environment.EnvironmentToDetailedEnvironmentV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.environment.EnvironmentToLocationV4RequestConverter;
import com.sequenceiq.cloudbreak.converter.v4.environment.EnvironmentToLocationV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.environment.RegionConverter;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.AwsEnvironmentNetworkConverter;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.AzureEnvironmentNetworkConverter;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.EnvironmentNetworkConverter;
import com.sequenceiq.cloudbreak.converter.v4.kerberos.KerberosConfigToKerberosV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.ldaps.LdapConfigToLdapV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.proxies.ProxyConfigToProxyV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.view.StackApiViewToStackViewV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.environment.AwsNetwork;
import com.sequenceiq.cloudbreak.domain.environment.AzureNetwork;
import com.sequenceiq.cloudbreak.domain.environment.BaseNetwork;
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
import com.sequenceiq.cloudbreak.service.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.platform.PlatformParameterService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.stack.StackApiViewService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.util.EnvironmentUtils;

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
    private KerberosConfigService kerberosConfigService;

    @Mock
    private EnvironmentCredentialOperationService environmentCredentialOperationService;

    @Mock
    private EnvironmentCreationValidator environmentCreationValidator;

    @Mock
    private StackApiViewService stackApiViewService;

    @Mock
    private StackService stackService;

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

    @Mock
    private EnvironmentNetworkService environmentNetworkService;

    @Mock
    private Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    @Mock
    private AwsEnvironmentNetworkConverter awsEnvironmentNetworkConverter;

    @Mock
    private AzureEnvironmentNetworkConverter azureEnvironmentNetworkConverter;

    @Spy
    private final EnvironmentDetachValidator environmentDetachValidator = new EnvironmentDetachValidator();

    @Spy
    private final EnvironmentRegionValidator environmentRegionValidator = new EnvironmentRegionValidator();

    private final ArgumentCaptor<StackApiView> stackApiViewCaptor = ArgumentCaptor.forClass(StackApiView.class);

    @InjectMocks
    private EnvironmentService underTest;

    private final Workspace workspace = new Workspace();

    private final Answer<Environment> answerWithFirstEnvironmentArgument = invocation -> {
        Object[] args = invocation.getArguments();
        return (Environment) args[0];
    };

    @Spy
    private final RegionConverter regionConverter = new RegionConverter();

    @InjectMocks
    private EnvironmentToLocationV4ResponseConverter environmentToLocationResponseConverter;

    @InjectMocks
    private EnvironmentToLocationV4RequestConverter environmentToLocationRequestConverter;

    @InjectMocks
    private EnvironmentToDetailedEnvironmentV4ResponseConverter environmentConverter;

    @InjectMocks
    private LdapConfigToLdapV4ResponseConverter ldapConfigResponseConverter;

    @InjectMocks
    private ProxyConfigToProxyV4ResponseConverter proxyConfigResponseConverter;

    @InjectMocks
    private RDSConfigToDatabaseV4ResponseConverter rdsConfigResponseConverter;

    @InjectMocks
    private KerberosConfigToKerberosV4ResponseConverter kerberosConfigResponseConverter;

    @InjectMocks
    private StackApiViewToStackViewV4ResponseConverter stackApiViewToStackViewResponseConverter;

    @Before
    public void setup() throws TransactionExecutionException {
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any());
        workspace.setId(WORKSPACE_ID);
        workspace.setName("ws");
        when(ldapConfigService.findByNamesInWorkspace(anySet(), anyLong())).thenReturn(Collections.emptySet());
        when(rdsConfigService.findByNamesInWorkspace(anySet(), anyLong())).thenReturn(Collections.emptySet());
        when(proxyConfigService.findByNamesInWorkspace(anySet(), anyLong())).thenReturn(Collections.emptySet());
        when(kerberosConfigService.findByNamesInWorkspace(anySet(), anyLong())).thenReturn(Collections.emptySet());
        when(environmentCreationValidator.validate(any(), any(), any())).thenReturn(ValidationResult.builder().build());
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(new CloudbreakUser("", "", "", "", ""));
        when(userService.getOrCreate(any())).thenReturn(new User());
        when(conversionService.convert(any(Environment.class), eq(DetailedEnvironmentV4Response.class))).thenReturn(new DetailedEnvironmentV4Response());
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(workspaceService.retrieveForUser(any())).thenReturn(Set.of(workspace));
        assertNotNull(regionConverter);
        assertNotNull(environmentDetachValidator);
        assertNotNull(environmentRegionValidator);
    }

    @Test
    public void testCreateWithCredentialName() {
        EnvironmentV4Request environmentV4Request = new EnvironmentV4Request();
        environmentV4Request.setName(ENVIRONMENT_NAME);

        environmentV4Request.setCredentialName(CREDENTIAL_NAME);
        CredentialV4Request credentialRequest = new CredentialV4Request();
        credentialRequest.setName("IgnoredCredRequestName");
        environmentV4Request.setCredential(credentialRequest);
        LocationV4Request locationV4Request = new LocationV4Request();
        locationV4Request.setName("region1");
        environmentV4Request.setLocation(locationV4Request);
        Credential credential = new Credential();
        credential.setCloudPlatform(CloudPlatform.MOCK.name());

        when(conversionService.convert(any(EnvironmentV4Request.class), eq(Environment.class))).thenReturn(new Environment());
        when(environmentRepository.save(any(Environment.class))).thenAnswer(answerWithFirstEnvironmentArgument);
        when(environmentCredentialOperationService.getCredentialFromRequest(any(), anyLong())).thenReturn(credential);
        CloudRegions cloudRegions = EnvironmentUtils.getCloudRegions();
        when(platformParameterService.getRegionsByCredential(any())).thenReturn(cloudRegions);
        DetailedEnvironmentV4Response response = underTest.createForLoggedInUser(environmentV4Request, WORKSPACE_ID);

        assertNotNull(response);
    }

    @Test
    public void testCreateWithCredential() {
        EnvironmentV4Request environmentV4Request = new EnvironmentV4Request();
        environmentV4Request.setName(ENVIRONMENT_NAME);

        CredentialV4Request credentialRequest = new CredentialV4Request();
        credentialRequest.setName("CredRequestName");
        environmentV4Request.setCredential(credentialRequest);
        LocationV4Request locationV4Request = new LocationV4Request();
        locationV4Request.setName("region1");
        environmentV4Request.setLocation(locationV4Request);
        Credential cred = new Credential();
        cred.setCloudPlatform(CloudPlatform.MOCK.name());

        when(conversionService.convert(any(EnvironmentV4Request.class), eq(Environment.class))).thenReturn(new Environment());
        when(environmentRepository.save(any(Environment.class))).thenAnswer(answerWithFirstEnvironmentArgument);
        when(environmentCredentialOperationService.getCredentialFromRequest(any(), anyLong())).thenReturn(cred);
        CloudRegions cloudRegions = EnvironmentUtils.getCloudRegions();
        when(platformParameterService.getRegionsByCredential(any())).thenReturn(cloudRegions);
        DetailedEnvironmentV4Response response = underTest.createForLoggedInUser(environmentV4Request, WORKSPACE_ID);

        assertNotNull(response);
    }

    @Test
    public void testCreateWithRegions() {
        // GIVEN
        EnvironmentV4Request environmentV4Request = new EnvironmentV4Request();
        environmentV4Request.setName(ENVIRONMENT_NAME);

        environmentV4Request.setCredentialName(CREDENTIAL_NAME);
        CredentialV4Request credentialRequest = new CredentialV4Request();
        credentialRequest.setName("IgnoredCredRequestName");
        environmentV4Request.setCredential(credentialRequest);
        environmentV4Request.setRegions(Set.of("region1", "region2"));
        LocationV4Request locationV4Request = new LocationV4Request();
        locationV4Request.setName("region1");
        environmentV4Request.setLocation(locationV4Request);
        Credential credential = new Credential();
        credential.setCloudPlatform(CloudPlatform.MOCK.name());

        when(conversionService.convert(any(EnvironmentV4Request.class), eq(Environment.class))).thenReturn(new Environment());
        ArgumentCaptor<Environment> envCaptor = ArgumentCaptor.forClass(Environment.class);
        when(environmentCredentialOperationService.getCredentialFromRequest(any(), anyLong())).thenReturn(credential);
        when(environmentRepository.save(envCaptor.capture())).thenAnswer(answerWithFirstEnvironmentArgument);

        CloudRegions cloudRegions = EnvironmentUtils.getCloudRegions();
        when(platformParameterService.getRegionsByCredential(any())).thenReturn(cloudRegions);
        // WHEN
        DetailedEnvironmentV4Response response = underTest.createForLoggedInUser(environmentV4Request, WORKSPACE_ID);
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

        when(environmentRepository.findByNameAndWorkspaceId(ENVIRONMENT_NAME, WORKSPACE_ID)).thenReturn(Optional.of(environment));
        when(environmentRepository.save(any(Environment.class)))
                .thenAnswer((Answer<Environment>) invocation -> (Environment) invocation.getArgument(0));
        mockConverters();

        EnvironmentDetachV4Request detachRequest = new EnvironmentDetachV4Request();
        detachRequest.getLdaps().add(ldapName1);
        detachRequest.getLdaps().add(notAttachedLdap);
        detachRequest.getProxies().add(proxyName1);
        detachRequest.getDatabases().add(rdsName1);
        detachRequest.getKerberoses().add(kdcName1);

        DetailedEnvironmentV4Response detachResponse = underTest.detachResources(ENVIRONMENT_NAME, detachRequest, WORKSPACE_ID);

        assertFalse(detachResponse.getLdaps().stream().map(LdapV4Base::getName).collect(Collectors.toSet()).contains(notAttachedLdap));
        assertFalse(detachResponse.getLdaps().stream().map(LdapV4Base::getName).collect(Collectors.toSet()).contains(ldapName1));
        assertFalse(detachResponse.getProxies().stream().map(ProxyV4Base::getName).collect(Collectors.toSet()).contains(proxyName1));
        assertFalse(detachResponse.getDatabases().stream().map(DatabaseV4Base::getName).collect(Collectors.toSet()).contains(rdsName1));
        assertFalse(detachResponse.getKerberoses().stream().map(KerberosV4ResponseBase::getName).collect(Collectors.toSet()).contains(kdcName1));
        assertTrue(detachResponse.getLdaps().stream().map(LdapV4Base::getName).collect(Collectors.toSet()).contains(ldapName2));
        assertTrue(detachResponse.getProxies().stream().map(ProxyV4Base::getName).collect(Collectors.toSet()).contains(proxyName2));
        assertTrue(detachResponse.getDatabases().stream().map(DatabaseV4Base::getName).collect(Collectors.toSet()).contains(rdsName2));
        assertTrue(detachResponse.getKerberoses().stream().map(KerberosV4ResponseBase::getName).collect(Collectors.toSet()).contains(kdcName2));
    }

    @Test
    public void testChangeCredentialHappyPath() {
        Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        when(environmentRepository.findByNameAndWorkspaceId(ENVIRONMENT_NAME, WORKSPACE_ID)).thenReturn(Optional.of(environment));

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
        when(conversionService.convert(any(Environment.class), eq(DetailedEnvironmentV4Response.class)))
                .thenAnswer((Answer<DetailedEnvironmentV4Response>) invocation -> environmentConverter.convert((Environment) invocation.getArgument(0)));
        when(conversionService.convert(any(StackApiView.class), eq(StackViewV4Response.class)))
                .thenAnswer(invocation -> stackApiViewToStackViewResponseConverter.convert((StackApiView) invocation.getArgument(0)));

        EnvironmentChangeCredentialV4Request request = new EnvironmentChangeCredentialV4Request();
        request.setCredentialName(credentialName2);

        DetailedEnvironmentV4Response response = underTest.changeCredential(ENVIRONMENT_NAME, WORKSPACE_ID, request);

        verify(stackApiViewService, times(2)).save(stackApiViewCaptor.capture());
        assertEquals(ENVIRONMENT_NAME, response.getName());
        assertEquals(credentialName2, response.getCredentialName());
    }

    @Test(expected = BadRequestException.class)
    public void testChangeCredentialWithUnchangableStack() {
        Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        when(environmentRepository.findByNameAndWorkspaceId(ENVIRONMENT_NAME, WORKSPACE_ID)).thenReturn(Optional.of(environment));

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

        EnvironmentChangeCredentialV4Request request = new EnvironmentChangeCredentialV4Request();
        request.setCredentialName(credentialName2);

        underTest.changeCredential(ENVIRONMENT_NAME, WORKSPACE_ID, request);
    }

    @Test(expected = BadRequestException.class)
    public void testChangeCredentialWithInvalidCloudPlatform() {
        Environment environment = new Environment();
        environment.setName(ENVIRONMENT_NAME);
        when(environmentRepository.findByNameAndWorkspaceId(ENVIRONMENT_NAME, WORKSPACE_ID)).thenReturn(Optional.of(environment));

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

        EnvironmentChangeCredentialV4Request request = new EnvironmentChangeCredentialV4Request();
        request.setCredentialName("credential2");

        underTest.changeCredential(ENVIRONMENT_NAME, WORKSPACE_ID, request);
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

        EnvironmentDetachV4Request request = new EnvironmentDetachV4Request();
        request.setLdaps(Sets.newHashSet(ldapName1, ldapName2));
        request.setProxies(Sets.newHashSet(proxyName1));
        request.setDatabases(Sets.newHashSet(rdsName1));
        request.setKerberoses(Sets.newHashSet(kdcName1));

        when(environmentRepository.findByNameAndWorkspaceId(ENVIRONMENT_NAME, WORKSPACE_ID)).thenReturn(Optional.of(environment));
        when(environmentRepository.save(any(Environment.class)))
                .thenAnswer((Answer<Environment>) invocation -> (Environment) invocation.getArgument(0));
        mockConverters();

        DetailedEnvironmentV4Response result = underTest.detachResources(ENVIRONMENT_NAME, request, WORKSPACE_ID);

        assertEquals(1, result.getLdaps().size());
        assertEquals(1, result.getProxies().size());
        assertEquals(1, result.getDatabases().size());
        assertEquals(1, result.getKerberoses().size());
        assertEquals(ldapName3, result.getLdaps().iterator().next().getName());
        assertEquals(proxyName2, result.getProxies().iterator().next().getName());
        assertEquals(rdsName2, result.getDatabases().iterator().next().getName());
        assertEquals(kdcName2, result.getKerberoses().iterator().next().getName());
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

        EnvironmentDetachV4Request request = new EnvironmentDetachV4Request();
        request.setLdaps(Sets.newHashSet(ldapName1, ldapName2));
        request.setProxies(Sets.newHashSet(proxyName1));
        request.setDatabases(Sets.newHashSet(rdsName1));
        request.setKerberoses(Sets.newHashSet(kdcName1));

        when(environmentRepository.findByNameAndWorkspaceId(ENVIRONMENT_NAME, WORKSPACE_ID)).thenReturn(Optional.of(environment));
        when(ldapConfigService.getClustersUsingResourceInEnvironment(ldap1, ENVIRONMENT_ID)).thenReturn(Sets.newHashSet(cluster1));
        when(ldapConfigService.getClustersUsingResourceInEnvironment(ldap2, ENVIRONMENT_ID)).thenReturn(Sets.newHashSet(cluster1, cluster2));
        when(proxyConfigService.getClustersUsingResourceInEnvironment(proxy1, ENVIRONMENT_ID)).thenReturn(Sets.newHashSet(cluster1));
        when(rdsConfigService.getClustersUsingResourceInEnvironment(rds1, ENVIRONMENT_ID)).thenReturn(Sets.newHashSet(cluster1));
        when(kerberosConfigService.getClustersUsingResourceInEnvironment(kdc1, ENVIRONMENT_ID)).thenReturn(Sets.newHashSet(cluster1));

        exceptionRule.expect(BadRequestException.class);
        exceptionRule.expectMessage(String.format("database config '%s' cannot be detached from environment 'EnvName' "
                + "because it is used by the following cluster(s): [%s]", rdsName1, clusterName1));
        exceptionRule.expectMessage(String.format("Proxy config '%s' cannot be detached from environment 'EnvName' "
                + "because it is used by the following cluster(s): [%s]", proxyName1, clusterName1));
        exceptionRule.expectMessage(String.format("LDAP config '%s' cannot be detached from environment 'EnvName' "
                + "because it is used by the following cluster(s): [%s]", ldapName1, clusterName1));
        exceptionRule.expectMessage(String.format("LDAP config '%s' cannot be detached from environment 'EnvName' "
                + "because it is used by the following cluster(s): [%s, %s]", ldapName2, clusterName1, clusterName2));
        exceptionRule.expectMessage(String.format("Kerberos Config '%s' cannot be detached from environment 'EnvName' "
                + "because it is used by the following cluster(s): [%s]", kdcName1, clusterName1));

        underTest.detachResources(ENVIRONMENT_NAME, request, WORKSPACE_ID);
    }

    @Test
    public void testEditLocationAndRegion() {
        String editedDescription = "edited description";
        String newLocation = "eu-west-1";
        String newRegion1 = "eu-west-2";
        String newRegion2 = "eu-west-3";
        EnvironmentEditV4Request editRequest = EnvironmentUtils
                .getEnvironmentEditRequest(editedDescription, newLocation, Set.of(newLocation, newRegion1, newRegion2));

        Environment environment = EnvironmentUtils.getEnvironment("us-west-1", Set.of("us-west-1", "us-west-2", "us-west-3"));
        environment.setCloudPlatform("aws");
        environment.setName(ENVIRONMENT_NAME);
        environment.setDescription("original descreption");
        setCredential(environment);

        when(environmentRepository.findByNameAndWorkspaceId(ENVIRONMENT_NAME, WORKSPACE_ID))
                .thenReturn(Optional.of(environment));

        CloudRegions cloudRegions = EnvironmentUtils.getCloudRegions(Set.of(newLocation, newRegion1, newRegion2, "us-west-1", "us-west-2", "us-west-3"));
        when(platformParameterService.getRegionsByCredential(any()))
                .thenReturn(cloudRegions);

        when(environmentRepository.save(any(Environment.class)))
                .thenAnswer((Answer<Environment>) invocation -> (Environment) invocation.getArgument(0));
        when(conversionService.convert(any(Environment.class), eq(DetailedEnvironmentV4Response.class)))
                .thenAnswer((Answer<DetailedEnvironmentV4Response>) invocation -> environmentConverter.convert((Environment) invocation.getArgument(0)));
        when(conversionService.convert(any(Environment.class), eq(LocationV4Response.class)))
                .thenAnswer((Answer<LocationV4Response>) invocation -> environmentToLocationResponseConverter.convert((Environment) invocation.getArgument(0)));

        DetailedEnvironmentV4Response result = underTest.edit(WORKSPACE_ID, ENVIRONMENT_NAME, editRequest);

        assertEquals(editedDescription, result.getDescription());
        assertEquals(newLocation, result.getLocation().getName());
        assertTrue(result.getRegions().getRegions().contains(newLocation));
        assertTrue(result.getRegions().getRegions().contains(newRegion1));
        assertTrue(result.getRegions().getRegions().contains(newRegion2));
    }

    @Test
    public void testEditOnlyLocation() {
        String region1 = "us-west-1";
        String region2 = "us-west-2";
        String region3 = "us-west-3";
        String newLocation = region3;
        EnvironmentEditV4Request editRequest = new EnvironmentEditV4Request();
        LocationV4Request locationRequest = new LocationV4Request();
        locationRequest.setName(newLocation);
        editRequest.setLocation(locationRequest);

        Environment environment = EnvironmentUtils.getEnvironment(region1, Set.of(region1, region2, region3));
        environment.setCloudPlatform("aws");
        environment.setName(ENVIRONMENT_NAME);
        environment.setDescription("original descreption");
        setCredential(environment);

        when(environmentRepository.findByNameAndWorkspaceId(ENVIRONMENT_NAME, WORKSPACE_ID))
                .thenReturn(Optional.of(environment));

        CloudRegions cloudRegions = EnvironmentUtils.getCloudRegions(Set.of(newLocation, region1, region2));
        when(platformParameterService.getRegionsByCredential(any()))
                .thenReturn(cloudRegions);

        when(environmentRepository.save(any(Environment.class)))
                .thenAnswer((Answer<Environment>) invocation -> (Environment) invocation.getArgument(0));
        when(conversionService.convert(any(Environment.class), eq(DetailedEnvironmentV4Response.class)))
                .thenAnswer((Answer<DetailedEnvironmentV4Response>) invocation -> environmentConverter.convert((Environment) invocation.getArgument(0)));
        when(conversionService.convert(any(Environment.class), eq(LocationV4Response.class)))
                .thenAnswer((Answer<LocationV4Response>) invocation -> environmentToLocationResponseConverter.convert((Environment) invocation.getArgument(0)));

        DetailedEnvironmentV4Response result = underTest.edit(WORKSPACE_ID, ENVIRONMENT_NAME, editRequest);

        assertEquals(newLocation, result.getLocation().getName());
        assertTrue(result.getRegions().getRegions().contains(region1));
        assertTrue(result.getRegions().getRegions().contains(region2));
        assertTrue(result.getRegions().getRegions().contains(region3));
    }

    @Test
    public void testEditOnlyRegions() {
        String region1 = "eu-west-1";
        String region2 = "eu-west-2";
        String region3 = "eu-west-3";
        EnvironmentEditV4Request editRequest = new EnvironmentEditV4Request();
        editRequest.setRegions(Set.of(region2, region3));

        Environment environment = EnvironmentUtils.getEnvironment(region2, Set.of(region1, region2));
        environment.setCloudPlatform("aws");
        environment.setName(ENVIRONMENT_NAME);
        environment.setDescription("original descreption");
        setCredential(environment);

        when(environmentRepository.findByNameAndWorkspaceId(ENVIRONMENT_NAME, WORKSPACE_ID))
                .thenReturn(Optional.of(environment));

        CloudRegions cloudRegions = EnvironmentUtils.getCloudRegions(Set.of(region1, region2, region3));
        when(platformParameterService.getRegionsByCredential(any()))
                .thenReturn(cloudRegions);

        when(environmentRepository.save(any(Environment.class)))
                .thenAnswer((Answer<Environment>) invocation -> (Environment) invocation.getArgument(0));
        when(conversionService.convert(any(Environment.class), eq(LocationV4Request.class)))
                .thenAnswer((Answer<LocationV4Request>) invocation -> environmentToLocationRequestConverter.convert((Environment) invocation.getArgument(0)));
        when(conversionService.convert(any(Environment.class), eq(DetailedEnvironmentV4Response.class)))
                .thenAnswer((Answer<DetailedEnvironmentV4Response>) invocation -> environmentConverter.convert((Environment) invocation.getArgument(0)));
        when(conversionService.convert(any(Environment.class), eq(LocationV4Response.class)))
                .thenAnswer((Answer<LocationV4Response>) invocation -> environmentToLocationResponseConverter.convert((Environment) invocation.getArgument(0)));

        DetailedEnvironmentV4Response result = underTest.edit(WORKSPACE_ID, ENVIRONMENT_NAME, editRequest);

        assertEquals(region2, result.getLocation().getName());
        assertTrue(result.getRegions().getRegions().contains(region2));
        assertTrue(result.getRegions().getRegions().contains(region3));
    }

    @Test
    public void testCreateWithAwsNetworkShouldCallSaveOnNetworkService() {
        EnvironmentV4Request environmentV4Request = new EnvironmentV4Request();
        environmentV4Request.setName(ENVIRONMENT_NAME);

        CredentialV4Request credentialRequest = new CredentialV4Request();
        credentialRequest.setName("CredRequestName");
        environmentV4Request.setCredential(credentialRequest);
        LocationV4Request locationV4Request = new LocationV4Request();
        locationV4Request.setName("region1");
        environmentV4Request.setLocation(locationV4Request);
        Credential cred = new Credential();
        cred.setCloudPlatform(CloudPlatform.AWS.name());

        EnvironmentNetworkV4Request network = new EnvironmentNetworkV4Request();
        network.setSubnetIds(Set.of("subnet-id"));
        network.setAws(new EnvironmentNetworkAwsV4Params());
        environmentV4Request.setNetwork(network);
        when(environmentNetworkConverterMap.get(any(CloudPlatform.class))).thenReturn(awsEnvironmentNetworkConverter);
        when(awsEnvironmentNetworkConverter.convert(any(EnvironmentNetworkV4Request.class), any(Environment.class))).thenReturn(new AwsNetwork());

        when(conversionService.convert(any(EnvironmentV4Request.class), eq(Environment.class))).thenReturn(new Environment());
        when(environmentRepository.save(any(Environment.class))).thenAnswer(answerWithFirstEnvironmentArgument);
        when(environmentCredentialOperationService.getCredentialFromRequest(any(), anyLong())).thenReturn(cred);
        CloudRegions cloudRegions = EnvironmentUtils.getCloudRegions();
        when(platformParameterService.getRegionsByCredential(any())).thenReturn(cloudRegions);

        DetailedEnvironmentV4Response response = underTest.createForLoggedInUser(environmentV4Request, WORKSPACE_ID);

        assertNotNull(response);
        verify(environmentNetworkService, times(1)).save(any(BaseNetwork.class));
    }

    @Test
    public void testCreateWithAwsCredentialWhenNoAwsNetworkSpecifiedShouldDoNothingWithNetworkService() {
        EnvironmentV4Request environmentV4Request = new EnvironmentV4Request();
        environmentV4Request.setName(ENVIRONMENT_NAME);

        CredentialV4Request credentialRequest = new CredentialV4Request();
        credentialRequest.setName("CredRequestName");
        environmentV4Request.setCredential(credentialRequest);
        LocationV4Request locationV4Request = new LocationV4Request();
        locationV4Request.setName("region1");
        environmentV4Request.setLocation(locationV4Request);
        Credential cred = new Credential();
        cred.setCloudPlatform(CloudPlatform.AWS.name());

        EnvironmentNetworkV4Request network = new EnvironmentNetworkV4Request();
        network.setSubnetIds(Set.of("subnet-id"));
        environmentV4Request.setNetwork(network);

        when(conversionService.convert(any(EnvironmentV4Request.class), eq(Environment.class))).thenReturn(new Environment());
        when(environmentRepository.save(any(Environment.class))).thenAnswer(answerWithFirstEnvironmentArgument);
        when(environmentCredentialOperationService.getCredentialFromRequest(any(), anyLong())).thenReturn(cred);
        CloudRegions cloudRegions = EnvironmentUtils.getCloudRegions();
        when(platformParameterService.getRegionsByCredential(any())).thenReturn(cloudRegions);
        DetailedEnvironmentV4Response response = underTest.createForLoggedInUser(environmentV4Request, WORKSPACE_ID);

        assertNotNull(response);
        verify(environmentNetworkService, times(0)).save(any(BaseNetwork.class));
    }

    @Test
    public void testCreateWithAzureNetworkShouldCallSaveOnNetworkService() {
        EnvironmentV4Request environmentV4Request = new EnvironmentV4Request();
        environmentV4Request.setName(ENVIRONMENT_NAME);

        CredentialV4Request credentialRequest = new CredentialV4Request();
        credentialRequest.setName("CredRequestName");
        environmentV4Request.setCredential(credentialRequest);
        LocationV4Request locationV4Request = new LocationV4Request();
        locationV4Request.setName("region1");
        environmentV4Request.setLocation(locationV4Request);
        Credential cred = new Credential();
        cred.setCloudPlatform(CloudPlatform.AZURE.name());

        EnvironmentNetworkV4Request network = new EnvironmentNetworkV4Request();
        network.setSubnetIds(Set.of("subnet-id"));
        network.setAzure(new EnvironmentNetworkAzureV4Params());
        environmentV4Request.setNetwork(network);
        when(environmentNetworkConverterMap.get(any(CloudPlatform.class))).thenReturn(azureEnvironmentNetworkConverter);
        when(azureEnvironmentNetworkConverter.convert(any(EnvironmentNetworkV4Request.class), any(Environment.class))).thenReturn(new AwsNetwork());

        when(conversionService.convert(any(EnvironmentV4Request.class), eq(Environment.class))).thenReturn(new Environment());
        when(environmentRepository.save(any(Environment.class))).thenAnswer(answerWithFirstEnvironmentArgument);
        when(environmentCredentialOperationService.getCredentialFromRequest(any(), anyLong())).thenReturn(cred);
        CloudRegions cloudRegions = EnvironmentUtils.getCloudRegions();
        when(platformParameterService.getRegionsByCredential(any())).thenReturn(cloudRegions);
        DetailedEnvironmentV4Response response = underTest.createForLoggedInUser(environmentV4Request, WORKSPACE_ID);

        assertNotNull(response);
        verify(environmentNetworkService, times(1)).save(any(BaseNetwork.class));
    }

    @Test
    public void testCreateWithAzureCredentialWhenNoAzureNetworkSpecifiedShouldDoNothingWithNetworkService() {
        EnvironmentV4Request environmentV4Request = new EnvironmentV4Request();
        environmentV4Request.setName(ENVIRONMENT_NAME);

        CredentialV4Request credentialRequest = new CredentialV4Request();
        credentialRequest.setName("CredRequestName");
        environmentV4Request.setCredential(credentialRequest);
        LocationV4Request locationV4Request = new LocationV4Request();
        locationV4Request.setName("region1");
        environmentV4Request.setLocation(locationV4Request);
        Credential cred = new Credential();
        cred.setCloudPlatform(CloudPlatform.AZURE.name());

        EnvironmentNetworkV4Request network = new EnvironmentNetworkV4Request();
        network.setSubnetIds(Set.of("subnet-id"));
        environmentV4Request.setNetwork(network);

        when(conversionService.convert(any(EnvironmentV4Request.class), eq(Environment.class))).thenReturn(new Environment());
        when(environmentRepository.save(any(Environment.class))).thenAnswer(answerWithFirstEnvironmentArgument);
        when(environmentCredentialOperationService.getCredentialFromRequest(any(), anyLong())).thenReturn(cred);
        CloudRegions cloudRegions = EnvironmentUtils.getCloudRegions();
        when(platformParameterService.getRegionsByCredential(any())).thenReturn(cloudRegions);
        DetailedEnvironmentV4Response response = underTest.createForLoggedInUser(environmentV4Request, WORKSPACE_ID);

        assertNotNull(response);
        verify(environmentNetworkService, times(0)).save(any(AzureNetwork.class));
    }

    @Test
    public void testCreateWithMockProviderShouldDoNothingWithNetworkService() {
        EnvironmentV4Request environmentV4Request = new EnvironmentV4Request();
        environmentV4Request.setName(ENVIRONMENT_NAME);

        CredentialV4Request credentialRequest = new CredentialV4Request();
        credentialRequest.setName("CredRequestName");
        environmentV4Request.setCredential(credentialRequest);
        environmentV4Request.setNetwork(new EnvironmentNetworkV4Request());
        LocationV4Request locationV4Request = new LocationV4Request();
        locationV4Request.setName("region1");
        environmentV4Request.setLocation(locationV4Request);
        Credential cred = new Credential();
        cred.setCloudPlatform(CloudPlatform.MOCK.name());
        when(environmentNetworkConverterMap.get(any(CloudPlatform.class))).thenReturn(null);

        when(conversionService.convert(any(EnvironmentV4Request.class), eq(Environment.class))).thenReturn(new Environment());
        when(environmentRepository.save(any(Environment.class))).thenAnswer(answerWithFirstEnvironmentArgument);
        when(environmentCredentialOperationService.getCredentialFromRequest(any(), anyLong())).thenReturn(cred);
        CloudRegions cloudRegions = EnvironmentUtils.getCloudRegions();
        when(platformParameterService.getRegionsByCredential(any())).thenReturn(cloudRegions);
        DetailedEnvironmentV4Response response = underTest.createForLoggedInUser(environmentV4Request, WORKSPACE_ID);

        assertNotNull(response);
        verify(environmentNetworkService, times(0)).save(any(AwsNetwork.class));
        verify(environmentNetworkService, times(0)).save(any(AzureNetwork.class));
    }

    @Test
    public void testCreateWithoutNetworkInRequestShouldDoNothingWithNetworkService() {
        EnvironmentV4Request environmentV4Request = new EnvironmentV4Request();
        environmentV4Request.setName(ENVIRONMENT_NAME);

        CredentialV4Request credentialRequest = new CredentialV4Request();
        credentialRequest.setName("CredRequestName");
        environmentV4Request.setCredential(credentialRequest);
        LocationV4Request locationV4Request = new LocationV4Request();
        locationV4Request.setName("region1");
        environmentV4Request.setLocation(locationV4Request);
        Credential cred = new Credential();
        cred.setCloudPlatform(CloudPlatform.MOCK.name());

        when(conversionService.convert(any(EnvironmentV4Request.class), eq(Environment.class))).thenReturn(new Environment());
        when(environmentRepository.save(any(Environment.class))).thenAnswer(answerWithFirstEnvironmentArgument);
        when(environmentCredentialOperationService.getCredentialFromRequest(any(), anyLong())).thenReturn(cred);
        CloudRegions cloudRegions = EnvironmentUtils.getCloudRegions();
        when(platformParameterService.getRegionsByCredential(any())).thenReturn(cloudRegions);
        DetailedEnvironmentV4Response response = underTest.createForLoggedInUser(environmentV4Request, WORKSPACE_ID);

        assertNotNull(response);
        verify(environmentNetworkService, times(0)).save(any(AwsNetwork.class));
        verify(environmentNetworkService, times(0)).save(any(AzureNetwork.class));
    }

    private void setCredential(Environment environment) {
        Credential credential = new Credential();
        credential.setName("credential1");
        environment.setCredential(credential);
        environment.setStacks(new HashSet<>());
    }

    private void mockConverters() {
        when(conversionService.convert(any(Environment.class), eq(DetailedEnvironmentV4Response.class)))
                .thenAnswer((Answer<DetailedEnvironmentV4Response>) invocation -> environmentConverter.convert((Environment) invocation.getArgument(0)));
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