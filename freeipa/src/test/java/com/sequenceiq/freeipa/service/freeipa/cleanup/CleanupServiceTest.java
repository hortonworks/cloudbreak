package com.sequenceiq.freeipa.service.freeipa.cleanup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.util.Pair;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupStep;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientRunnable;
import com.sequenceiq.freeipa.client.FreeIpaErrorCodes;
import com.sequenceiq.freeipa.client.model.Cert;
import com.sequenceiq.freeipa.client.model.DnsRecord;
import com.sequenceiq.freeipa.client.model.DnsZone;
import com.sequenceiq.freeipa.converter.operation.OperationToOperationStatusConverter;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.CleanupEvent;
import com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupEvent;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;
import com.sequenceiq.freeipa.ldap.LdapConfigService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientRetryService;
import com.sequenceiq.freeipa.service.freeipa.dns.DnsZoneBatchedService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.freeipa.host.HostDeletionService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.FreeIpaStatusValidator;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CleanupServiceTest {

    private static final long STACK_ID = 1L;

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String ACCOUNT_ID = "accountId";

    @InjectMocks
    private CleanupService underTest;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private StackService stackService;

    @Mock
    private HostDeletionService hostDeletionService;

    @Mock
    private PollingService<FreeIpaServerDeletionPollerObject> freeIpaDeletionPollerService;

    @Mock
    private FreeIpaClientRetryService retryService;

    @Mock
    private FreeIpaStatusValidator statusValidator;

    @Mock
    private OperationService operationService;

    @Mock
    private CleanupStepToStateNameConverter cleanupStepToStateNameConverter;

    @Mock
    private FreeIpaFlowManager flowManager;

    @Mock
    private OperationToOperationStatusConverter operationToOperationStatusConverter;

    @Mock
    private CachedEnvironmentClientService environmentClientService;

    @Mock
    private DnsZoneBatchedService dnsZoneBatchedService;

    @BeforeEach
    void init() throws FreeIpaClientException {
        lenient().doAnswer(invocation -> {
            invocation.getArgument(0, FreeIpaClientRunnable.class).run();
            return null;
        }).when(retryService).retryWhenRetryableWithoutValue(any(FreeIpaClientRunnable.class));
    }

    @Test
    public void testRevokeCertsWithLongHostnames() throws FreeIpaClientException {
        Set<String> hosts = Set.of(
                "test-wl-1-worker0.env.xyz.wl.cloudera.site",
                "test-wl-1-worker1.env.xyz.wl.cloudera.site",
                "test-wl-1-master2.env.xyz.wl.cloudera.site",
                "test-wl-1-compute3.env.xyz.wl.cloudera.site"
        );
        Set<Cert> certs = Set.of(
                createCert("CN=test-wl-2-master2", 1, false),
                createCert("CN=test-wl-1-master2", 2, false),
                createCert("CN=test-wl-3-master1", 3, true),
                createCert("CN=test-datalake-1-master1", 4, false),
                createCert("CN=ipaserver0.env.xyz.wl.cloudera.site,O=ENV.XYZ.WL.CLOUDERA.SITE", 50, false)
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClient.findAllCert()).thenReturn(certs);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);

        Pair<Set<String>, Map<String, String>> result = underTest.revokeCerts(STACK_ID, hosts);

        verify(freeIpaClient, times(1)).revokeCert(2);
        verifyRevokeNotInvoked(freeIpaClient, 1, 3, 4, 50);
        assertEquals(1, result.getFirst().size());
        assertEquals(0, result.getSecond().size());
        assertTrue(result.getFirst().stream().allMatch("CN=test-wl-1-master2"::equals));
    }

    @Test
    public void testRevokeCertsWithShortHostnames() throws FreeIpaClientException {
        Set<String> hosts = Set.of(
                "test-wl-1-worker0",
                "test-wl-1-worker1",
                "test-wl-1-master2",
                "test-wl-1-compute3"
        );
        Set<Cert> certs = Set.of(
                createCert("CN=test-wl-2-master2", 1, false),
                createCert("CN=test-wl-1-master2", 2, false),
                createCert("CN=test-wl-3-master1", 3, false),
                createCert("CN=test-datalake-1-master1", 4, false),
                createCert("CN=ipaserver0.env.xyz.wl.cloudera.site,O=ENV.XYZ.WL.CLOUDERA.SITE", 50, false)
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClient.findAllCert()).thenReturn(certs);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);

        Pair<Set<String>, Map<String, String>> result = underTest.revokeCerts(STACK_ID, hosts);

        verify(freeIpaClient, times(1)).revokeCert(2);
        verifyRevokeNotInvoked(freeIpaClient, 1, 3, 4, 50);
        assertEquals(1, result.getFirst().size());
        assertEquals(0, result.getSecond().size());
        assertTrue(result.getFirst().stream().allMatch("CN=test-wl-1-master2"::equals));
    }

    @Test
    public void testRevokeCertsWithMixedHostnames() throws FreeIpaClientException {
        Set<String> hosts = Set.of(
                "test-wl-1-worker0.env.xyz.wl.cloudera.site",
                "test-wl-1-worker1",
                "test-wl-1-master2",
                "test-wl-1-compute3.env.xyz.wl.cloudera.site"
        );
        Set<Cert> certs = Set.of(
                createCert("CN=test-wl-2-master2", 1, false),
                createCert("CN=test-wl-1-master2", 2, false),
                createCert("CN=test-wl-3-master1", 3, false),
                createCert("CN=test-datalake-1-master1", 4, false),
                createCert("CN=ipaserver0.env.xyz.wl.cloudera.site,O=ENV.XYZ.WL.CLOUDERA.SITE", 50, false)
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClient.findAllCert()).thenReturn(certs);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);

        Pair<Set<String>, Map<String, String>> result = underTest.revokeCerts(STACK_ID, hosts);

        verify(freeIpaClient, times(1)).revokeCert(2);
        verifyRevokeNotInvoked(freeIpaClient, 1, 3, 4, 50);
        assertEquals(1, result.getFirst().size());
        assertEquals(0, result.getSecond().size());
        assertTrue(result.getFirst().stream().allMatch("CN=test-wl-1-master2"::equals));
    }

    @Test
    public void testRevokeCertsWithAlreadyRevokedCert() throws FreeIpaClientException {
        Set<String> hosts = Set.of(
                "test-wl-1-worker0.env.xyz.wl.cloudera.site",
                "test-wl-1-worker1.env.xyz.wl.cloudera.site",
                "test-wl-1-master2.env.xyz.wl.cloudera.site",
                "test-wl-1-compute3.env.xyz.wl.cloudera.site"
        );
        Set<Cert> certs = Set.of(
                createCert("CN=test-wl-2-master2", 1, false),
                createCert("CN=test-wl-1-master2", 2, true),
                createCert("CN=test-wl-3-master1", 3, true),
                createCert("CN=test-datalake-1-master1", 4, false),
                createCert("CN=ipaserver0.env.xyz.wl.cloudera.site,O=ENV.XYZ.WL.CLOUDERA.SITE", 50, false)
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClient.findAllCert()).thenReturn(certs);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);

        Pair<Set<String>, Map<String, String>> result = underTest.revokeCerts(STACK_ID, hosts);

        verifyRevokeNotInvoked(freeIpaClient, 1, 2, 3, 4, 50);
        assertEquals(0, result.getFirst().size());
        assertEquals(0, result.getSecond().size());
    }

    @Test
    public void testRevokeCertsWithAlreadyRevokedCertAndNewClusterWithSameName() throws FreeIpaClientException {
        Set<String> hosts = Set.of(
                "test-wl-1-worker0.env.xyz.wl.cloudera.site",
                "test-wl-1-worker1.env.xyz.wl.cloudera.site",
                "test-wl-1-master2.env.xyz.wl.cloudera.site",
                "test-wl-1-compute3.env.xyz.wl.cloudera.site"
        );
        Set<Cert> certs = Set.of(
                createCert("CN=test-wl-2-master2", 1, false),
                createCert("CN=test-wl-1-master2", 2, true),
                createCert("CN=test-wl-1-master2", 20, true),
                createCert("CN=test-wl-1-master2", 21, false),
                createCert("CN=test-wl-3-master1", 3, true),
                createCert("CN=test-datalake-1-master1", 4, false),
                createCert("CN=ipaserver0.env.xyz.wl.cloudera.site,O=ENV.XYZ.WL.CLOUDERA.SITE", 50, false)
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClient.findAllCert()).thenReturn(certs);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);

        Pair<Set<String>, Map<String, String>> result = underTest.revokeCerts(STACK_ID, hosts);

        verify(freeIpaClient, times(1)).revokeCert(21);
        verifyRevokeNotInvoked(freeIpaClient, 1, 2, 20, 3, 4, 50);
        assertEquals(1, result.getFirst().size());
        assertEquals(0, result.getSecond().size());
        assertTrue(result.getFirst().stream().allMatch("CN=test-wl-1-master2"::equals));
    }

    @Test
    public void testRevokeCertsWithLongCertAndHostnames() throws FreeIpaClientException {
        Set<String> hosts = Set.of(
                "test-wl-1-worker0.env.xyz.wl.cloudera.site",
                "test-wl-1-worker1.env.xyz.wl.cloudera.site",
                "test-wl-1-master2.env.xyz.wl.cloudera.site",
                "test-wl-1-compute3.env.xyz.wl.cloudera.site"
        );
        Set<Cert> certs = Set.of(
                createCert("CN=test-wl-2-master2.env.xyz.wl.cloudera.site", 1, false),
                createCert("CN=test-wl-1-master2.env.xyz.wl.cloudera.site", 2, false),
                createCert("CN=test-wl-3-master1.env.xyz.wl.cloudera.site", 3, true),
                createCert("CN=test-datalake-1-master1.env.xyz.wl.cloudera.site", 4, false),
                createCert("CN=ipaserver0.env.xyz.wl.cloudera.site,O=ENV.XYZ.WL.CLOUDERA.SITE", 50, false)
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClient.findAllCert()).thenReturn(certs);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);

        Pair<Set<String>, Map<String, String>> result = underTest.revokeCerts(STACK_ID, hosts);

        verify(freeIpaClient, times(1)).revokeCert(2);
        verifyRevokeNotInvoked(freeIpaClient, 1, 3, 4, 50);
        assertEquals(1, result.getFirst().size());
        assertEquals(0, result.getSecond().size());
        assertTrue(result.getFirst().stream().allMatch("CN=test-wl-1-master2.env.xyz.wl.cloudera.site"::equals));
    }

    @Test
    public void testRevokeCertsWithLongCertAndShortHostnames() throws FreeIpaClientException {
        Set<String> hosts = Set.of(
                "test-wl-1-worker0",
                "test-wl-1-worker1",
                "test-wl-1-master2",
                "test-wl-1-compute3"
        );
        Set<Cert> certs = Set.of(
                createCert("CN=test-wl-2-master2.env.xyz.wl.cloudera.site", 1, false),
                createCert("CN=test-wl-1-master2.env.xyz.wl.cloudera.site", 2, false),
                createCert("CN=test-wl-3-master1.env.xyz.wl.cloudera.site", 3, true),
                createCert("CN=test-datalake-1-master1.env.xyz.wl.cloudera.site", 4, false),
                createCert("CN=ipaserver0.env.xyz.wl.cloudera.site,O=ENV.XYZ.WL.CLOUDERA.SITE", 50, false)
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClient.findAllCert()).thenReturn(certs);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);

        Pair<Set<String>, Map<String, String>> result = underTest.revokeCerts(STACK_ID, hosts);

        verify(freeIpaClient, times(1)).revokeCert(2);
        verifyRevokeNotInvoked(freeIpaClient, 1, 3, 4, 50);
        assertEquals(1, result.getFirst().size());
        assertEquals(0, result.getSecond().size());
        assertTrue(result.getFirst().stream().allMatch("CN=test-wl-1-master2.env.xyz.wl.cloudera.site"::equals));
    }

    @Test
    public void testRevokeCertsWhenNoCmHostnameProvided() throws FreeIpaClientException {
        Set<String> hosts = Set.of(
                "test-wl-1-worker0.env.xyz.wl.cloudera.site",
                "test-wl-1-worker1.env.xyz.wl.cloudera.site",
                "test-wl-1-master2111111111.env.xyz.wl.cloudera.site",
                "test-wl-1-compute3.env.xyz.wl.cloudera.site"
        );
        Set<Cert> certs = Set.of(
                createCert("CN=test-wl-2-master2", 1, false),
                createCert("CN=test-wl-1-master2", 2, false),
                createCert("CN=test-wl-3-master1", 3, true),
                createCert("CN=test-datalake-1-master1", 4, false),
                createCert("CN=ipaserver0.env.xyz.wl.cloudera.site,O=ENV.XYZ.WL.CLOUDERA.SITE", 50, false)
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClient.findAllCert()).thenReturn(certs);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);

        Pair<Set<String>, Map<String, String>> result = underTest.revokeCerts(STACK_ID, hosts);

        verifyRevokeNotInvoked(freeIpaClient, 1, 2, 3, 4, 50);
        assertEquals(0, result.getFirst().size());
        assertEquals(0, result.getSecond().size());
    }

    @Test
    public void testRevokeCertsWhenExceptionOccurredDuringRevoke() throws FreeIpaClientException {
        Set<String> hosts = Set.of(
                "test-wl-1-worker0.env.xyz.wl.cloudera.site",
                "test-wl-1-worker1.env.xyz.wl.cloudera.site",
                "test-wl-1-master2.env.xyz.wl.cloudera.site",
                "test-wl-1-compute3.env.xyz.wl.cloudera.site"
        );
        Set<Cert> certs = Set.of(
                createCert("CN=test-wl-2-master2", 1, false),
                createCert("CN=test-wl-1-master2", 2, false),
                createCert("CN=test-wl-3-master1", 3, true),
                createCert("CN=test-datalake-1-master1", 4, false),
                createCert("CN=ipaserver0.env.xyz.wl.cloudera.site,O=ENV.XYZ.WL.CLOUDERA.SITE", 50, false)
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClient.findAllCert()).thenReturn(certs);
        doThrow(new FreeIpaClientException("Cannot connect to FreeIPA")).when(freeIpaClient).revokeCert(2);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);

        Pair<Set<String>, Map<String, String>> result = underTest.revokeCerts(STACK_ID, hosts);

        verifyRevokeNotInvoked(freeIpaClient, 1, 3, 4, 50);
        assertEquals(0, result.getFirst().size());
        assertEquals(1, result.getSecond().size());
        assertEquals("Cannot connect to FreeIPA", result.getSecond().get("CN=test-wl-1-master2"));
    }

    @Test
    public void testRemoveUsersForCluster() throws FreeIpaClientException {
        Set<String> usersNames = Set.of(
                "ldapbind-test-wl-1",
                "kerberosbind-test-wl-1");
        Set<String> ipaUserUids = Set.of(
                "ldapbind-test-wl-1",
                "ldapbind-test-wl-2",
                "kerberosbind-test-wl-1",
                "kerberosbind-test-wl-2",
                "mockuser0",
                "csso_khorvath"
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);
        when(freeIpaClient.userListAllUids()).thenReturn(ipaUserUids);
        when(stackService.getStackById(anyLong())).thenReturn(createStack());

        Pair<Set<String>, Map<String, String>> result = underTest.removeUsers(STACK_ID, usersNames, "test-wl-1", ENVIRONMENT_CRN);

        verify(freeIpaClient, times(1)).deleteUser("ldapbind-test-wl-1");
        verify(freeIpaClient, times(1)).deleteUser("kerberosbind-test-wl-1");
        verifyUserDeleteNotInvoked(freeIpaClient, "ldapbind-test-wl-2", "kerberosbind-test-wl-2", "mockuser0", "csso_khorvath");
        assertEquals(2, result.getFirst().size());
        assertTrue(result.getFirst().stream().anyMatch("ldapbind-test-wl-1"::equals));
        assertTrue(result.getFirst().stream().anyMatch("kerberosbind-test-wl-1"::equals));
        verify(kerberosConfigService, times(1)).delete(ENVIRONMENT_CRN, ACCOUNT_ID, "test-wl-1");
        verify(ldapConfigService, times(1)).delete(ENVIRONMENT_CRN, ACCOUNT_ID, "test-wl-1");
    }

    @Test
    public void testRemoveUsersWhenNoClusterNameProvided() throws FreeIpaClientException {
        Set<String> usersNames = Set.of(
                "ldapbind-test-wl-1",
                "kerberosbind-test-wl-1");
        Set<String> ipaUserUids = Set.of(
                "ldapbind-test-wl-1",
                "ldapbind-test-wl-2",
                "kerberosbind-test-wl-1",
                "kerberosbind-test-wl-2",
                "mockuser0",
                "csso_khorvath"
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);
        when(freeIpaClient.userListAllUids()).thenReturn(ipaUserUids);

        Pair<Set<String>, Map<String, String>> result = underTest.removeUsers(STACK_ID, usersNames, "", ENVIRONMENT_CRN);

        verify(freeIpaClient, times(1)).deleteUser("ldapbind-test-wl-1");
        verify(freeIpaClient, times(1)).deleteUser("kerberosbind-test-wl-1");
        verifyUserDeleteNotInvoked(freeIpaClient, "ldapbind-test-wl-2", "kerberosbind-test-wl-2", "mockuser0", "csso_khorvath");
        assertEquals(2, result.getFirst().size());
        assertTrue(result.getFirst().stream().anyMatch("ldapbind-test-wl-1"::equals));
        assertTrue(result.getFirst().stream().anyMatch("kerberosbind-test-wl-1"::equals));
        verify(kerberosConfigService, times(0)).delete(ENVIRONMENT_CRN, ACCOUNT_ID, "test-wl-1");
        verify(ldapConfigService, times(0)).delete(ENVIRONMENT_CRN, ACCOUNT_ID, "test-wl-1");
    }

    @Test
    public void testRemoveUsersWhenNoClusterNameProvidedAndDeleteFails() throws FreeIpaClientException {
        Set<String> usersNames = Set.of(
                "ldapbind-test-wl-1",
                "kerberosbind-test-wl-1");
        Set<String> ipaUserUids = Set.of(
                "ldapbind-test-wl-1",
                "ldapbind-test-wl-2",
                "kerberosbind-test-wl-1",
                "kerberosbind-test-wl-2",
                "mockuser0",
                "csso_khorvath"
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);
        when(freeIpaClient.userListAllUids()).thenReturn(ipaUserUids);
        doThrow(new FreeIpaClientException("Connection failed")).when(freeIpaClient).deleteUser(anyString());

        Pair<Set<String>, Map<String, String>> result = underTest.removeUsers(STACK_ID, usersNames, "", ENVIRONMENT_CRN);

        verify(freeIpaClient, times(1)).deleteUser("ldapbind-test-wl-1");
        verify(freeIpaClient, times(1)).deleteUser("kerberosbind-test-wl-1");
        verifyUserDeleteNotInvoked(freeIpaClient, "ldapbind-test-wl-2", "kerberosbind-test-wl-2", "mockuser0", "csso_khorvath");
        assertEquals(0, result.getFirst().size());
        assertEquals(2, result.getSecond().size());
        assertEquals("Connection failed", result.getSecond().get("ldapbind-test-wl-1"));
        assertEquals("Connection failed", result.getSecond().get("kerberosbind-test-wl-1"));
        verify(kerberosConfigService, times(0)).delete(ENVIRONMENT_CRN, ACCOUNT_ID, "test-wl-1");
        verify(ldapConfigService, times(0)).delete(ENVIRONMENT_CRN, ACCOUNT_ID, "test-wl-1");
    }

    @Test
    public void testRemoveUsersWhenKerberosConfigAlreadyDeleted() throws FreeIpaClientException {
        Set<String> usersNames = Set.of(
                "ldapbind-test-wl-1",
                "kerberosbind-test-wl-1");
        Set<String> ipaUserUids = Set.of(
                "ldapbind-test-wl-1",
                "ldapbind-test-wl-2",
                "kerberosbind-test-wl-1",
                "kerberosbind-test-wl-2",
                "mockuser0",
                "csso_khorvath"
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);
        when(freeIpaClient.userListAllUids()).thenReturn(ipaUserUids);
        when(stackService.getStackById(anyLong())).thenReturn(createStack());
        doThrow(new NotFoundException("Kerberos config not found")).when(kerberosConfigService).delete(ENVIRONMENT_CRN, ACCOUNT_ID, "test-wl-1");

        Pair<Set<String>, Map<String, String>> result = underTest.removeUsers(STACK_ID, usersNames, "test-wl-1", ENVIRONMENT_CRN);

        verify(freeIpaClient, times(1)).deleteUser("ldapbind-test-wl-1");
        verify(freeIpaClient, times(1)).deleteUser("kerberosbind-test-wl-1");
        verifyUserDeleteNotInvoked(freeIpaClient, "ldapbind-test-wl-2", "kerberosbind-test-wl-2", "mockuser0", "csso_khorvath");
        assertEquals(2, result.getFirst().size());
        assertTrue(result.getFirst().stream().anyMatch("ldapbind-test-wl-1"::equals));
        assertTrue(result.getFirst().stream().anyMatch("kerberosbind-test-wl-1"::equals));
        verify(kerberosConfigService, times(1)).delete(ENVIRONMENT_CRN, ACCOUNT_ID, "test-wl-1");
        verify(ldapConfigService, times(1)).delete(ENVIRONMENT_CRN, ACCOUNT_ID, "test-wl-1");
    }

    @Test
    public void testRemoveUsersWhenLdapConfigAlreadyDeleted() throws FreeIpaClientException {
        Set<String> usersNames = Set.of(
                "ldapbind-test-wl-1",
                "kerberosbind-test-wl-1");
        Set<String> ipaUserUids = Set.of(
                "ldapbind-test-wl-1",
                "ldapbind-test-wl-2",
                "kerberosbind-test-wl-1",
                "kerberosbind-test-wl-2",
                "mockuser0",
                "csso_khorvath"
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);
        when(freeIpaClient.userListAllUids()).thenReturn(ipaUserUids);
        when(stackService.getStackById(anyLong())).thenReturn(createStack());
        doThrow(new NotFoundException("Ldap config not found")).when(ldapConfigService).delete(ENVIRONMENT_CRN, ACCOUNT_ID, "test-wl-1");

        Pair<Set<String>, Map<String, String>> result = underTest.removeUsers(STACK_ID, usersNames, "test-wl-1", ENVIRONMENT_CRN);

        verify(freeIpaClient, times(1)).deleteUser("ldapbind-test-wl-1");
        verify(freeIpaClient, times(1)).deleteUser("kerberosbind-test-wl-1");
        verifyUserDeleteNotInvoked(freeIpaClient, "ldapbind-test-wl-2", "kerberosbind-test-wl-2", "mockuser0", "csso_khorvath");
        assertEquals(2, result.getFirst().size());
        assertTrue(result.getFirst().stream().anyMatch("ldapbind-test-wl-1"::equals));
        assertTrue(result.getFirst().stream().anyMatch("kerberosbind-test-wl-1"::equals));
        verify(kerberosConfigService, times(1)).delete(ENVIRONMENT_CRN, ACCOUNT_ID, "test-wl-1");
        verify(ldapConfigService, times(1)).delete(ENVIRONMENT_CRN, ACCOUNT_ID, "test-wl-1");
    }

    @Test
    public void testRemoveFreeIpaServer() throws FreeIpaClientException {
        Set<String> hosts = Set.of("example1.com", "example2.com");
        FreeIpaClient client = mock(FreeIpaClient.class);
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .success()
                .build();
        when(freeIpaClientFactory.getFreeIpaClientForStackId(anyLong())).thenReturn(client);
        when(hostDeletionService.removeServers(any(), any())).thenReturn(Pair.of(hosts, Map.of()));
        when(client.findAllService()).thenReturn(Set.of());
        when(freeIpaDeletionPollerService.pollWithAbsoluteTimeout(any(), any(), anyLong(), anyLong(), anyInt()))
                .thenReturn(extendedPollingResult);

        Pair<Set<String>, Map<String, String>> result = underTest.removeServers(STACK_ID, hosts);

        assertEquals(hosts, result.getFirst());
        assertTrue(result.getSecond().isEmpty());
        verify(freeIpaDeletionPollerService, times(1)).pollWithAbsoluteTimeout(any(), any(), anyLong(), anyLong(), anyInt());
    }

    @Test
    public void testRemoveDnsEntries() throws FreeIpaClientException {
        FreeIpaClient client = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(client);
        DnsZone dnsZone = new DnsZone();
        String domain = "test.com";
        dnsZone.setIdnsname(domain);
        DnsZone reverseZone = new DnsZone();
        reverseZone.setIdnsname("0.10.in-addr.arpa.");
        DnsZone disappearingZone = new DnsZone();
        disappearingZone.setIdnsname("disappear");
        when(client.findAllDnsZone()).thenReturn(Set.of(dnsZone, reverseZone, disappearingZone));
        DnsRecord deleteMe = new DnsRecord();
        deleteMe.setIdnsname("deleteMe");
        deleteMe.setArecord(List.of("ignored"));
        DnsRecord notFound = new DnsRecord();
        notFound.setIdnsname("notfound");
        notFound.setArecord(List.of("ignored"));
        DnsRecord failed = new DnsRecord();
        failed.setIdnsname("failed");
        failed.setArecord(List.of("ignored"));
        DnsRecord ptrRecord = new DnsRecord();
        ptrRecord.setIdnsname("1.0");
        ptrRecord.setPtrrecord(List.of("ptrRecord"));
        DnsRecord nsRecord = new DnsRecord();
        nsRecord.setIdnsname("@");
        nsRecord.setNsrecord(List.of("nsRecord"));
        nsRecord.setDn("generalZone");
        doReturn(Set.of(deleteMe, notFound, failed, nsRecord)).when(client).findAllDnsRecordInZone(eq(dnsZone.getIdnsname()));
        doReturn(new RPCResponse()).when(client).deleteDnsRecord("1.0", "0.10.in-addr.arpa.");
        doThrow(new FreeIpaClientException("delete failed"))
                .when(client).deleteDnsRecord("failed", "test.com");
        doThrow(new FreeIpaClientException("Not found", new JsonRpcClientException(FreeIpaErrorCodes.NOT_FOUND.getValue(), "Not found", null)))
                .when(client).deleteDnsRecord("notfound", "test.com");
        doReturn(Set.of(ptrRecord, nsRecord)).when(client).findAllDnsRecordInZone(eq(reverseZone.getIdnsname()));
        doThrow(new FreeIpaClientException("Not found zone", new JsonRpcClientException(FreeIpaErrorCodes.NOT_FOUND.getValue(), "Not found", null)))
                .when(client).findAllDnsRecordInZone(eq(disappearingZone.getIdnsname()));
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(new DetailedEnvironmentResponse());

        Pair<Set<String>, Map<String, String>> result = underTest.removeDnsEntries(STACK_ID,
                Set.of(deleteMe.getIdnsname(), notFound.getIdnsname(), failed.getIdnsname(), "ptrRecord"),
                Set.of("10.0.0.1", "10.1.0.1"), domain, ENVIRONMENT_CRN);

        verify(client).deleteDnsRecord(deleteMe.getIdnsname(), domain);
        assertTrue(result.getFirst().containsAll(Set.of(deleteMe.getIdnsname(), notFound.getIdnsname(), "10.0.0.1")));
        assertTrue(result.getSecond().containsKey(failed.getIdnsname()));
        assertEquals("delete failed", result.getSecond().get(failed.getIdnsname()));
        assertEquals(1, result.getSecond().size());
        assertEquals(3, result.getFirst().size());
    }

    @Test
    public void testRemoveDnsEntriesWhenHybridEnv() throws FreeIpaClientException, TimeoutException {
        FreeIpaClient client = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(client);
        DnsZone dnsZone = new DnsZone();
        String domain = "test.com";
        dnsZone.setIdnsname(domain);
        DnsZone reverseZone = new DnsZone();
        reverseZone.setIdnsname("0.10.in-addr.arpa.");
        DnsZone disappearingZone = new DnsZone();
        disappearingZone.setIdnsname("disappear");
        Set<DnsZone> zones = Set.of(dnsZone, reverseZone, disappearingZone);
        when(client.findAllDnsZone()).thenReturn(zones);
        DnsRecord deleteMe = new DnsRecord();
        deleteMe.setIdnsname("deleteMe");
        deleteMe.setArecord(List.of("ignored"));
        DnsRecord notFound = new DnsRecord();
        notFound.setIdnsname("notfound");
        notFound.setArecord(List.of("ignored"));
        DnsRecord failed = new DnsRecord();
        failed.setIdnsname("failed");
        failed.setArecord(List.of("ignored"));
        DnsRecord ptrRecord = new DnsRecord();
        ptrRecord.setIdnsname("1.0");
        ptrRecord.setPtrrecord(List.of("ptrRecord"));
        DnsRecord nsRecord = new DnsRecord();
        nsRecord.setIdnsname("@");
        nsRecord.setNsrecord(List.of("nsRecord"));
        nsRecord.setDn("generalZone");
        doReturn(new RPCResponse()).when(client).deleteDnsRecord("1.0", "0.10.in-addr.arpa.");
        doThrow(new FreeIpaClientException("delete failed"))
                .when(client).deleteDnsRecord("failed", "test.com");
        doThrow(new FreeIpaClientException("Not found", new JsonRpcClientException(FreeIpaErrorCodes.NOT_FOUND.getValue(), "Not found", null)))
                .when(client).deleteDnsRecord("notfound", "test.com");
        doThrow(new FreeIpaClientException("Not found zone", new JsonRpcClientException(FreeIpaErrorCodes.NOT_FOUND.getValue(), "Not found", null)))
                .when(client).findAllDnsRecordInZone(eq(disappearingZone.getIdnsname()));
        when(dnsZoneBatchedService.fetchDnsRecordsByZone(client, zones.stream().map(DnsZone::getIdnsname).collect(Collectors.toSet())))
                .thenReturn(Map.of(
                        dnsZone.getIdnsname(), Set.of(deleteMe, notFound, failed, nsRecord),
                        reverseZone.getIdnsname(), Set.of(ptrRecord, nsRecord)
                ));
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setEnvironmentType(EnvironmentType.HYBRID.name());
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environmentResponse);

        Pair<Set<String>, Map<String, String>> result = underTest.removeDnsEntries(STACK_ID,
                Set.of(deleteMe.getIdnsname(), notFound.getIdnsname(), failed.getIdnsname(), "ptrRecord"),
                Set.of("10.0.0.1", "10.1.0.1"), domain, ENVIRONMENT_CRN);

        verify(client).deleteDnsRecord(deleteMe.getIdnsname(), domain);
        assertTrue(result.getFirst().containsAll(Set.of(deleteMe.getIdnsname(), notFound.getIdnsname(), "10.0.0.1")));
        assertTrue(result.getSecond().containsKey(failed.getIdnsname()));
        assertEquals("delete failed", result.getSecond().get(failed.getIdnsname()));
        assertEquals(1, result.getSecond().size());
        assertEquals(3, result.getFirst().size());
    }

    @Test
    void testTriggerCleanup() {
        CleanupRequest request = new CleanupRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setCleanupStepsToSkip(Set.of(CleanupStep.REMOVE_HOSTS));
        request.setUsers(Set.of("u1", "u2"));
        request.setHosts(Set.of("h1", "h2"));
        request.setRoles(Set.of("r1", "r2"));
        request.setIps(Set.of("i1", "i2"));
        request.setClusterName("cname");
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        when(stackService.getFreeIpaStackWithMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        Operation operation = new Operation();
        operation.setStatus(OperationState.RUNNING);
        operation.setOperationId("asdf");
        when(operationService.startOperation(ACCOUNT_ID, OperationType.CLEANUP, Set.of(ENVIRONMENT_CRN), Collections.emptySet())).thenReturn(operation);
        Set<String> skip = Set.of("skip");
        when(cleanupStepToStateNameConverter.convert(request.getCleanupStepsToSkip())).thenReturn(skip);
        OperationStatus operationStatus = new OperationStatus();
        operationStatus.setStatus(OperationState.COMPLETED);
        when(operationToOperationStatusConverter.convert(operation)).thenReturn(operationStatus);

        OperationStatus result = underTest.cleanup(ACCOUNT_ID, request);

        verify(statusValidator).throwBadRequestIfFreeIpaIsUnreachable(stack);
        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        verify(flowManager).notify(eq(FreeIpaCleanupEvent.CLEANUP_EVENT.event()), captor.capture());
        CleanupEvent cleanupEvent = (CleanupEvent) captor.getValue();
        assertEquals(FreeIpaCleanupEvent.CLEANUP_EVENT.event(), cleanupEvent.selector());
        assertEquals(STACK_ID, cleanupEvent.getResourceId());
        assertEquals(operation.getOperationId(), cleanupEvent.getOperationId());
        assertEquals(ACCOUNT_ID, cleanupEvent.getAccountId());
        assertEquals(ENVIRONMENT_CRN, cleanupEvent.getEnvironmentCrn());
        assertEquals(request.getClusterName(), cleanupEvent.getClusterName());
        assertEquals(request.getUsers(), cleanupEvent.getUsers());
        assertEquals(request.getHosts(), cleanupEvent.getHosts());
        assertEquals(request.getRoles(), cleanupEvent.getRoles());
        assertEquals(request.getIps(), cleanupEvent.getIps());
        assertEquals(skip, cleanupEvent.getStatesToSkip());
        assertEquals(operationStatus, result);
    }

    @Test
    void testTriggerCleanupFailed() {
        CleanupRequest request = new CleanupRequest();
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setCleanupStepsToSkip(Set.of(CleanupStep.REMOVE_HOSTS));
        request.setUsers(Set.of("u1", "u2"));
        request.setHosts(Set.of("h1", "h2"));
        request.setRoles(Set.of("r1", "r2"));
        request.setIps(Set.of("i1", "i2"));
        request.setClusterName("cname");
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        when(stackService.getFreeIpaStackWithMdcContext(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        Operation operation = new Operation();
        operation.setStatus(OperationState.RUNNING);
        operation.setOperationId("asdf");
        when(operationService.startOperation(ACCOUNT_ID, OperationType.CLEANUP, Set.of(ENVIRONMENT_CRN), Collections.emptySet())).thenReturn(operation);
        Set<String> skip = Set.of("skip");
        when(cleanupStepToStateNameConverter.convert(request.getCleanupStepsToSkip())).thenReturn(skip);
        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        when(flowManager.notify(eq(FreeIpaCleanupEvent.CLEANUP_EVENT.event()), captor.capture())).thenThrow(new RuntimeException("bumm"));
        Operation failedOp = new Operation();
        failedOp.setOperationId(operation.getOperationId());
        failedOp.setStatus(OperationState.FAILED);
        when(operationService.failOperation(ACCOUNT_ID, operation.getOperationId(), "Couldn't start cleanup flow: bumm")).thenReturn(failedOp);
        OperationStatus operationStatus = new OperationStatus();
        operationStatus.setStatus(OperationState.FAILED);
        when(operationToOperationStatusConverter.convert(failedOp)).thenReturn(operationStatus);

        OperationStatus result = underTest.cleanup(ACCOUNT_ID, request);

        verify(statusValidator).throwBadRequestIfFreeIpaIsUnreachable(stack);
        CleanupEvent cleanupEvent = (CleanupEvent) captor.getValue();
        assertEquals(FreeIpaCleanupEvent.CLEANUP_EVENT.event(), cleanupEvent.selector());
        assertEquals(STACK_ID, cleanupEvent.getResourceId());
        assertEquals(operation.getOperationId(), cleanupEvent.getOperationId());
        assertEquals(ACCOUNT_ID, cleanupEvent.getAccountId());
        assertEquals(ENVIRONMENT_CRN, cleanupEvent.getEnvironmentCrn());
        assertEquals(request.getClusterName(), cleanupEvent.getClusterName());
        assertEquals(request.getUsers(), cleanupEvent.getUsers());
        assertEquals(request.getHosts(), cleanupEvent.getHosts());
        assertEquals(request.getRoles(), cleanupEvent.getRoles());
        assertEquals(request.getIps(), cleanupEvent.getIps());
        assertEquals(skip, cleanupEvent.getStatesToSkip());
        assertEquals(operationStatus, result);
    }

    private Cert createCert(String subject, long serialNumber, boolean revoked) {
        Cert cert = new Cert();
        cert.setSubject(subject);
        cert.setRevoked(revoked);
        cert.setSerialNumber(serialNumber);
        return cert;
    }

    private void verifyRevokeNotInvoked(FreeIpaClient freeIpaClient, long... serialNumber) throws FreeIpaClientException {
        for (long num : serialNumber) {
            verify(freeIpaClient, times(0)).revokeCert(num);
        }
    }

    private void verifyUserDeleteNotInvoked(FreeIpaClient freeIpaClient, String... userIds) throws FreeIpaClientException {
        for (String user : userIds) {
            verify(freeIpaClient, times(0)).deleteUser(user);
        }
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setAccountId(ACCOUNT_ID);
        return stack;
    }
}
