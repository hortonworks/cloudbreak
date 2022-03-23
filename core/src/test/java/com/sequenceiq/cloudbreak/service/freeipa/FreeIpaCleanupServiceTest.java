package com.sequenceiq.cloudbreak.service.freeipa;

import static com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupStep.REMOVE_HOSTS;
import static com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupStep.REMOVE_ROLES;
import static com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupStep.REMOVE_USERS;
import static com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupStep.REMOVE_VAULT_ENTRIES;
import static com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupStep.REVOKE_CERTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentConfigProvider;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.OperationV1Endpoint;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;

@ExtendWith(MockitoExtension.class)
public class FreeIpaCleanupServiceTest {

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String STACK_NAME = "stack name";

    private static final String CLOUD_PLATFORM = "cloudPlatform";

    private static final String KERBEROS_USER_PREFIX = "krbbind-";

    private static final String KEYTAB_USER_PREFIX = "kerberosbind-";

    private static final String LDAP_USER_PREFIX = "ldapbind-";

    private static final String ROLE_NAME_PREFIX = "hadoopadminrole-";

    private ExtendedPollingResult pollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
            .success()
            .build();

    @Mock
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @Mock
    private PollingService<FreeIpaOperationPollerObject> freeIpaOperationChecker;

    @Mock
    private OperationV1Endpoint operationV1Endpoint;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private KerberosDetailService kerberosDetailService;

    @Mock
    private EnvironmentConfigProvider environmentConfigProvider;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Spy
    private InstanceMetadataProcessor instanceMetadataProcessor;

    @InjectMocks
    private FreeIpaCleanupService victim;

    @Test
    public void shouldSendCleanupRequestInCaseOfKeytabNeedsToBeUpdated() {
        Stack stack = aStack();
        Optional<KerberosConfig> kerberosConfig = Optional.of(mock(KerberosConfig.class));
        OperationStatus operationStatus = new OperationStatus(null, OperationType.CLEANUP, null, null, null, null, 0L, null);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(kerberosConfigService.get(ENVIRONMENT_CRN, STACK_NAME)).thenReturn(kerberosConfig);
        when(environmentConfigProvider.isChildEnvironment(ENVIRONMENT_CRN)).thenReturn(true);
        when(kerberosDetailService.keytabsShouldBeUpdated(CLOUD_PLATFORM, true, kerberosConfig)).thenReturn(true);
        when(freeIpaV1Endpoint.internalCleanup(any(CleanupRequest.class), anyString())).thenReturn(operationStatus);
        when(freeIpaOperationChecker.pollWithAbsoluteTimeout(any(), any(), anyLong(), anyLong(), anyInt())).thenReturn(pollingResult);

        victim.cleanupButIp(stack);

        verify(freeIpaV1Endpoint).internalCleanup(any(), anyString());
    }

    @Test
    public void shouldNotSendCleanupRequestInCaseOfKeytabDoesNotNeedToBeUpdated() {
        Stack stack = aStack();
        Optional<KerberosConfig> kerberosConfig = Optional.of(mock(KerberosConfig.class));

        when(kerberosConfigService.get(ENVIRONMENT_CRN, STACK_NAME)).thenReturn(kerberosConfig);
        when(environmentConfigProvider.isChildEnvironment(ENVIRONMENT_CRN)).thenReturn(true);
        when(kerberosDetailService.keytabsShouldBeUpdated(CLOUD_PLATFORM, true, kerberosConfig)).thenReturn(false);

        victim.cleanupButIp(stack);

        verifyNoMoreInteractions(freeIpaV1Endpoint);
    }

    @Test
    public void testCleanup() {
        Stack stack = spy(aStack());
        Optional<KerberosConfig> kerberosConfig = Optional.of(mock(KerberosConfig.class));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(kerberosConfigService.get(ENVIRONMENT_CRN, STACK_NAME)).thenReturn(kerberosConfig);
        when(environmentConfigProvider.isChildEnvironment(ENVIRONMENT_CRN)).thenReturn(false);
        when(kerberosDetailService.keytabsShouldBeUpdated(CLOUD_PLATFORM, false, kerberosConfig)).thenReturn(true);
        when(stack.getInstanceMetaDataAsList()).thenReturn(List.of(createInstanceMetadata("asdf", "1.1.1.1"), createInstanceMetadata("qwer", "1.1.1.2")));
        OperationStatus operationStatus = new OperationStatus("opId", OperationType.CLEANUP, null, null, null, null, 0L, null);
        ArgumentCaptor<CleanupRequest> captor = ArgumentCaptor.forClass(CleanupRequest.class);
        when(freeIpaV1Endpoint.internalCleanup(captor.capture(), anyString())).thenReturn(operationStatus);
        when(freeIpaOperationChecker.pollWithAbsoluteTimeout(any(), any(), anyLong(), anyLong(), anyInt())).thenReturn(pollingResult);

        victim.cleanupButIp(stack);

        CleanupRequest cleanupRequest = captor.getValue();
        assertEquals(STACK_NAME, cleanupRequest.getClusterName());
        assertEquals(ENVIRONMENT_CRN, cleanupRequest.getEnvironmentCrn());
        assertTrue(cleanupRequest.getCleanupStepsToSkip().isEmpty());
        assertEquals(Set.of(KERBEROS_USER_PREFIX + stack.getName(), KEYTAB_USER_PREFIX + stack.getName(), LDAP_USER_PREFIX + stack.getName()),
                cleanupRequest.getUsers());
        assertEquals(Set.of(ROLE_NAME_PREFIX + stack.getName()), cleanupRequest.getRoles());
        assertTrue(cleanupRequest.getIps().isEmpty());
        assertEquals(Set.of("asdf", "qwer"), cleanupRequest.getHosts());
    }

    @Test
    public void testCleanupPollFailed() {
        Stack stack = spy(aStack());
        Optional<KerberosConfig> kerberosConfig = Optional.of(mock(KerberosConfig.class));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(kerberosConfigService.get(ENVIRONMENT_CRN, STACK_NAME)).thenReturn(kerberosConfig);
        when(environmentConfigProvider.isChildEnvironment(ENVIRONMENT_CRN)).thenReturn(false);
        when(kerberosDetailService.keytabsShouldBeUpdated(CLOUD_PLATFORM, false, kerberosConfig)).thenReturn(true);
        when(stack.getInstanceMetaDataAsList()).thenReturn(List.of(createInstanceMetadata("asdf", "1.1.1.1"), createInstanceMetadata("qwer", "1.1.1.2")));
        OperationStatus operationStatus = new OperationStatus("opId", OperationType.CLEANUP, null, null, null, null, 0L, null);
        ArgumentCaptor<CleanupRequest> captor = ArgumentCaptor.forClass(CleanupRequest.class);
        when(freeIpaV1Endpoint.internalCleanup(captor.capture(), anyString())).thenReturn(operationStatus);
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .failure()
                .withException(new Exception("message"))
                .build();
        when(freeIpaOperationChecker.pollWithAbsoluteTimeout(any(), any(), anyLong(), anyLong(), anyInt())).thenReturn(extendedPollingResult);

        assertThrows(FreeIpaOperationFailedException.class, () -> victim.cleanupButIp(stack));

        CleanupRequest cleanupRequest = captor.getValue();
        assertEquals(STACK_NAME, cleanupRequest.getClusterName());
        assertEquals(ENVIRONMENT_CRN, cleanupRequest.getEnvironmentCrn());
        assertTrue(cleanupRequest.getCleanupStepsToSkip().isEmpty());
        assertEquals(Set.of(KERBEROS_USER_PREFIX + stack.getName(), KEYTAB_USER_PREFIX + stack.getName(), LDAP_USER_PREFIX + stack.getName()),
                cleanupRequest.getUsers());
        assertEquals(Set.of(ROLE_NAME_PREFIX + stack.getName()), cleanupRequest.getRoles());
        assertTrue(cleanupRequest.getIps().isEmpty());
        assertEquals(Set.of("asdf", "qwer"), cleanupRequest.getHosts());
    }

    @Test
    public void testCleanupOnScale() {
        Stack stack = spy(aStack());
        Optional<KerberosConfig> kerberosConfig = Optional.of(mock(KerberosConfig.class));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(kerberosConfigService.get(ENVIRONMENT_CRN, STACK_NAME)).thenReturn(kerberosConfig);
        when(environmentConfigProvider.isChildEnvironment(ENVIRONMENT_CRN)).thenReturn(false);
        when(kerberosDetailService.keytabsShouldBeUpdated(CLOUD_PLATFORM, false, kerberosConfig)).thenReturn(true);
        OperationStatus operationStatus = new OperationStatus("opId", OperationType.CLEANUP, null, null, null, null, 0L, null);
        ArgumentCaptor<CleanupRequest> captor = ArgumentCaptor.forClass(CleanupRequest.class);
        when(freeIpaV1Endpoint.internalCleanup(captor.capture(), anyString())).thenReturn(operationStatus);
        when(freeIpaOperationChecker.pollWithAbsoluteTimeout(any(), any(), anyLong(), anyLong(), anyInt())).thenReturn(pollingResult);

        victim.cleanupOnScale(stack, Set.of("asdf", "qwer"), Set.of("1.1.1.1", "1.1.1.2"));

        CleanupRequest cleanupRequest = captor.getValue();
        assertEquals(STACK_NAME, cleanupRequest.getClusterName());
        assertEquals(ENVIRONMENT_CRN, cleanupRequest.getEnvironmentCrn());
        assertEquals(Set.of(REMOVE_USERS, REMOVE_ROLES), cleanupRequest.getCleanupStepsToSkip());
        assertEquals(Set.of(KERBEROS_USER_PREFIX + stack.getName(), KEYTAB_USER_PREFIX + stack.getName(), LDAP_USER_PREFIX + stack.getName()),
                cleanupRequest.getUsers());
        assertEquals(Set.of(ROLE_NAME_PREFIX + stack.getName()), cleanupRequest.getRoles());
        assertEquals(Set.of("1.1.1.1", "1.1.1.2"), cleanupRequest.getIps());
        assertEquals(Set.of("asdf", "qwer"), cleanupRequest.getHosts());
    }

    @Test
    public void testCleanupOnRecover() {
        Stack stack = spy(aStack());
        Optional<KerberosConfig> kerberosConfig = Optional.of(mock(KerberosConfig.class));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(kerberosConfigService.get(ENVIRONMENT_CRN, STACK_NAME)).thenReturn(kerberosConfig);
        when(environmentConfigProvider.isChildEnvironment(ENVIRONMENT_CRN)).thenReturn(false);
        when(kerberosDetailService.keytabsShouldBeUpdated(CLOUD_PLATFORM, false, kerberosConfig)).thenReturn(true);
        OperationStatus operationStatus = new OperationStatus("opId", OperationType.CLEANUP, null, null, null, null, 0L, null);
        ArgumentCaptor<CleanupRequest> captor = ArgumentCaptor.forClass(CleanupRequest.class);
        when(freeIpaV1Endpoint.internalCleanup(captor.capture(), anyString())).thenReturn(operationStatus);
        when(freeIpaOperationChecker.pollWithAbsoluteTimeout(any(), any(), anyLong(), anyLong(), anyInt())).thenReturn(pollingResult);

        victim.cleanupOnRecover(stack, Set.of("asdf", "qwer"), Set.of("1.1.1.1", "1.1.1.2"));

        CleanupRequest cleanupRequest = captor.getValue();
        assertEquals(STACK_NAME, cleanupRequest.getClusterName());
        assertEquals(ENVIRONMENT_CRN, cleanupRequest.getEnvironmentCrn());
        assertEquals(Set.of(REMOVE_HOSTS, REMOVE_VAULT_ENTRIES, REMOVE_USERS, REMOVE_ROLES), cleanupRequest.getCleanupStepsToSkip());
        assertEquals(Set.of(KERBEROS_USER_PREFIX + stack.getName(), KEYTAB_USER_PREFIX + stack.getName(), LDAP_USER_PREFIX + stack.getName()),
                cleanupRequest.getUsers());
        assertEquals(Set.of(ROLE_NAME_PREFIX + stack.getName()), cleanupRequest.getRoles());
        assertEquals(Set.of("1.1.1.1", "1.1.1.2"), cleanupRequest.getIps());
        assertEquals(Set.of("asdf", "qwer"), cleanupRequest.getHosts());
    }

    @Test
    public void testCleanupDnsOnly() {
        Stack stack = spy(aStack());
        Optional<KerberosConfig> kerberosConfig = Optional.of(mock(KerberosConfig.class));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(kerberosConfigService.get(ENVIRONMENT_CRN, STACK_NAME)).thenReturn(kerberosConfig);
        when(environmentConfigProvider.isChildEnvironment(ENVIRONMENT_CRN)).thenReturn(false);
        when(kerberosDetailService.keytabsShouldBeUpdated(CLOUD_PLATFORM, false, kerberosConfig)).thenReturn(true);
        OperationStatus operationStatus = new OperationStatus("opId", OperationType.CLEANUP, null, null, null, null, 0L, null);
        ArgumentCaptor<CleanupRequest> captor = ArgumentCaptor.forClass(CleanupRequest.class);
        when(freeIpaV1Endpoint.internalCleanup(captor.capture(), anyString())).thenReturn(operationStatus);
        when(freeIpaOperationChecker.pollWithAbsoluteTimeout(any(), any(), anyLong(), anyLong(), anyInt())).thenReturn(pollingResult);

        victim.cleanupDnsOnly(stack, Set.of("asdf", "qwer"), Set.of("1.1.1.1", "1.1.1.2"));

        CleanupRequest cleanupRequest = captor.getValue();
        assertEquals(STACK_NAME, cleanupRequest.getClusterName());
        assertEquals(ENVIRONMENT_CRN, cleanupRequest.getEnvironmentCrn());
        assertEquals(Set.of(REMOVE_HOSTS, REMOVE_VAULT_ENTRIES, REMOVE_USERS, REVOKE_CERTS, REMOVE_ROLES), cleanupRequest.getCleanupStepsToSkip());
        assertEquals(Set.of(KERBEROS_USER_PREFIX + stack.getName(), KEYTAB_USER_PREFIX + stack.getName(), LDAP_USER_PREFIX + stack.getName()),
                cleanupRequest.getUsers());
        assertEquals(Set.of(ROLE_NAME_PREFIX + stack.getName()), cleanupRequest.getRoles());
        assertEquals(Set.of("1.1.1.1", "1.1.1.2"), cleanupRequest.getIps());
        assertEquals(Set.of("asdf", "qwer"), cleanupRequest.getHosts());
    }

    @Test
    public void testNullChecks() {
        assertThrows(NullPointerException.class, () -> victim.cleanupOnScale(new Stack(), null, Set.of()));
        assertThrows(NullPointerException.class, () -> victim.cleanupOnScale(new Stack(), Set.of(), null));
        assertThrows(NullPointerException.class, () -> victim.cleanupOnRecover(new Stack(), null, Set.of()));
        assertThrows(NullPointerException.class, () -> victim.cleanupOnRecover(new Stack(), Set.of(), null));
        assertThrows(NullPointerException.class, () -> victim.cleanupDnsOnly(new Stack(), null, Set.of()));
        assertThrows(NullPointerException.class, () -> victim.cleanupDnsOnly(new Stack(), Set.of(), null));
    }

    private InstanceMetaData createInstanceMetadata(String fqdn, String ip) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN(fqdn);
        instanceMetaData.setPrivateIp(ip);
        return instanceMetaData;
    }

    private Stack aStack() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setName(STACK_NAME);
        stack.setCloudPlatform(CLOUD_PLATFORM);
        stack.setResourceCrn(CrnTestUtil.getFreeipaCrnBuilder()
                .setAccountId("accountId")
                .setResource("resource")
                .build().toString());
        return stack;
    }
}