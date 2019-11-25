package com.sequenceiq.freeipa.service.freeipa.cleanup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.util.Pair;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.Cert;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.controller.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;
import com.sequenceiq.freeipa.ldap.LdapConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class CleanupServiceTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private CleanupService cleanupService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private StackService stackService;

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

        Pair<Set<String>, Map<String, String>> result = cleanupService.revokeCerts(STACK_ID, hosts);

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

        Pair<Set<String>, Map<String, String>> result = cleanupService.revokeCerts(STACK_ID, hosts);

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

        Pair<Set<String>, Map<String, String>> result = cleanupService.revokeCerts(STACK_ID, hosts);

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

        Pair<Set<String>, Map<String, String>> result = cleanupService.revokeCerts(STACK_ID, hosts);

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

        Pair<Set<String>, Map<String, String>> result = cleanupService.revokeCerts(STACK_ID, hosts);

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

        Pair<Set<String>, Map<String, String>> result = cleanupService.revokeCerts(STACK_ID, hosts);

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

        Pair<Set<String>, Map<String, String>> result = cleanupService.revokeCerts(STACK_ID, hosts);

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

        Pair<Set<String>, Map<String, String>> result = cleanupService.revokeCerts(STACK_ID, hosts);

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

        Pair<Set<String>, Map<String, String>> result = cleanupService.revokeCerts(STACK_ID, hosts);

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
        Set<User> ipaUsers = Set.of(
                createUser("ldapbind-test-wl-1"),
                createUser("ldapbind-test-wl-2"),
                createUser("kerberosbind-test-wl-1"),
                createUser("kerberosbind-test-wl-2"),
                createUser("mockuser0"),
                createUser("csso_khorvath")
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);
        when(freeIpaClient.userFindAll()).thenReturn(ipaUsers);
        when(stackService.getStackById(anyLong())).thenReturn(createStack());

        Pair<Set<String>, Map<String, String>> result = cleanupService.removeUsers(STACK_ID, usersNames, "test-wl-1");

        verify(freeIpaClient, times(1)).deleteUser("ldapbind-test-wl-1");
        verify(freeIpaClient, times(1)).deleteUser("kerberosbind-test-wl-1");
        verifyUserDeleteNotInvoked(freeIpaClient, "ldapbind-test-wl-2", "kerberosbind-test-wl-2", "mockuser0", "csso_khorvath");
        assertEquals(2, result.getFirst().size());
        assertTrue(result.getFirst().stream().anyMatch("ldapbind-test-wl-1"::equals));
        assertTrue(result.getFirst().stream().anyMatch("kerberosbind-test-wl-1"::equals));
        verify(kerberosConfigService, times(1)).delete("envCrn", "accountId", "test-wl-1");
        verify(ldapConfigService, times(1)).delete("envCrn", "accountId", "test-wl-1");
    }

    @Test
    public void testRemoveUsersWhenNoClusterNameProvided() throws FreeIpaClientException {
        Set<String> usersNames = Set.of(
                "ldapbind-test-wl-1",
                "kerberosbind-test-wl-1");
        Set<User> ipaUsers = Set.of(
                createUser("ldapbind-test-wl-1"),
                createUser("ldapbind-test-wl-2"),
                createUser("kerberosbind-test-wl-1"),
                createUser("kerberosbind-test-wl-2"),
                createUser("mockuser0"),
                createUser("csso_khorvath")
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);
        when(freeIpaClient.userFindAll()).thenReturn(ipaUsers);

        Pair<Set<String>, Map<String, String>> result = cleanupService.removeUsers(STACK_ID, usersNames, "");

        verify(freeIpaClient, times(1)).deleteUser("ldapbind-test-wl-1");
        verify(freeIpaClient, times(1)).deleteUser("kerberosbind-test-wl-1");
        verifyUserDeleteNotInvoked(freeIpaClient, "ldapbind-test-wl-2", "kerberosbind-test-wl-2", "mockuser0", "csso_khorvath");
        assertEquals(2, result.getFirst().size());
        assertTrue(result.getFirst().stream().anyMatch("ldapbind-test-wl-1"::equals));
        assertTrue(result.getFirst().stream().anyMatch("kerberosbind-test-wl-1"::equals));
        verify(kerberosConfigService, times(0)).delete("envCrn", "accountId", "test-wl-1");
        verify(ldapConfigService, times(0)).delete("envCrn", "accountId", "test-wl-1");
    }

    @Test
    public void testRemoveUsersWhenNoClusterNameProvidedAndDeleteFails() throws FreeIpaClientException {
        Set<String> usersNames = Set.of(
                "ldapbind-test-wl-1",
                "kerberosbind-test-wl-1");
        Set<User> ipaUsers = Set.of(
                createUser("ldapbind-test-wl-1"),
                createUser("ldapbind-test-wl-2"),
                createUser("kerberosbind-test-wl-1"),
                createUser("kerberosbind-test-wl-2"),
                createUser("mockuser0"),
                createUser("csso_khorvath")
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);
        when(freeIpaClient.userFindAll()).thenReturn(ipaUsers);
        doThrow(new FreeIpaClientException("Connection failed")).when(freeIpaClient).deleteUser(anyString());

        Pair<Set<String>, Map<String, String>> result = cleanupService.removeUsers(STACK_ID, usersNames, "");

        verify(freeIpaClient, times(1)).deleteUser("ldapbind-test-wl-1");
        verify(freeIpaClient, times(1)).deleteUser("kerberosbind-test-wl-1");
        verifyUserDeleteNotInvoked(freeIpaClient, "ldapbind-test-wl-2", "kerberosbind-test-wl-2", "mockuser0", "csso_khorvath");
        assertEquals(0, result.getFirst().size());
        assertEquals(2, result.getSecond().size());
        assertEquals("Connection failed", result.getSecond().get("ldapbind-test-wl-1"));
        assertEquals("Connection failed", result.getSecond().get("kerberosbind-test-wl-1"));
        verify(kerberosConfigService, times(0)).delete("envCrn", "accountId", "test-wl-1");
        verify(ldapConfigService, times(0)).delete("envCrn", "accountId", "test-wl-1");
    }

    @Test
    public void testRemoveUsersWhenKerberosConfigAlreadyDeleted() throws FreeIpaClientException {
        Set<String> usersNames = Set.of(
                "ldapbind-test-wl-1",
                "kerberosbind-test-wl-1");
        Set<User> ipaUsers = Set.of(
                createUser("ldapbind-test-wl-1"),
                createUser("ldapbind-test-wl-2"),
                createUser("kerberosbind-test-wl-1"),
                createUser("kerberosbind-test-wl-2"),
                createUser("mockuser0"),
                createUser("csso_khorvath")
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);
        when(freeIpaClient.userFindAll()).thenReturn(ipaUsers);
        when(stackService.getStackById(anyLong())).thenReturn(createStack());
        doThrow(new NotFoundException("Kerberos config not found")).when(kerberosConfigService).delete("envCrn", "accountId", "test-wl-1");

        Pair<Set<String>, Map<String, String>> result = cleanupService.removeUsers(STACK_ID, usersNames, "test-wl-1");

        verify(freeIpaClient, times(1)).deleteUser("ldapbind-test-wl-1");
        verify(freeIpaClient, times(1)).deleteUser("kerberosbind-test-wl-1");
        verifyUserDeleteNotInvoked(freeIpaClient, "ldapbind-test-wl-2", "kerberosbind-test-wl-2", "mockuser0", "csso_khorvath");
        assertEquals(2, result.getFirst().size());
        assertTrue(result.getFirst().stream().anyMatch("ldapbind-test-wl-1"::equals));
        assertTrue(result.getFirst().stream().anyMatch("kerberosbind-test-wl-1"::equals));
        verify(kerberosConfigService, times(1)).delete("envCrn", "accountId", "test-wl-1");
        verify(ldapConfigService, times(1)).delete("envCrn", "accountId", "test-wl-1");
    }

    @Test
    public void testRemoveUsersWhenLdapConfigAlreadyDeleted() throws FreeIpaClientException {
        Set<String> usersNames = Set.of(
                "ldapbind-test-wl-1",
                "kerberosbind-test-wl-1");
        Set<User> ipaUsers = Set.of(
                createUser("ldapbind-test-wl-1"),
                createUser("ldapbind-test-wl-2"),
                createUser("kerberosbind-test-wl-1"),
                createUser("kerberosbind-test-wl-2"),
                createUser("mockuser0"),
                createUser("csso_khorvath")
        );
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStackId(STACK_ID)).thenReturn(freeIpaClient);
        when(freeIpaClient.userFindAll()).thenReturn(ipaUsers);
        when(stackService.getStackById(anyLong())).thenReturn(createStack());
        doThrow(new NotFoundException("Ldap config not found")).when(ldapConfigService).delete("envCrn", "accountId", "test-wl-1");

        Pair<Set<String>, Map<String, String>> result = cleanupService.removeUsers(STACK_ID, usersNames, "test-wl-1");

        verify(freeIpaClient, times(1)).deleteUser("ldapbind-test-wl-1");
        verify(freeIpaClient, times(1)).deleteUser("kerberosbind-test-wl-1");
        verifyUserDeleteNotInvoked(freeIpaClient, "ldapbind-test-wl-2", "kerberosbind-test-wl-2", "mockuser0", "csso_khorvath");
        assertEquals(2, result.getFirst().size());
        assertTrue(result.getFirst().stream().anyMatch("ldapbind-test-wl-1"::equals));
        assertTrue(result.getFirst().stream().anyMatch("kerberosbind-test-wl-1"::equals));
        verify(kerberosConfigService, times(1)).delete("envCrn", "accountId", "test-wl-1");
        verify(ldapConfigService, times(1)).delete("envCrn", "accountId", "test-wl-1");
    }

    private Cert createCert(String subject, int serialNumber, boolean revoked) {
        Cert cert = new Cert();
        cert.setSubject(subject);
        cert.setRevoked(revoked);
        cert.setSerialNumber(serialNumber);
        return cert;
    }

    private void verifyRevokeNotInvoked(FreeIpaClient freeIpaClient, int... serialNumber) throws FreeIpaClientException {
        for (int num : serialNumber) {
            verify(freeIpaClient, times(0)).revokeCert(num);
        }
    }

    private void verifyUserDeleteNotInvoked(FreeIpaClient freeIpaClient, String... userIds) throws FreeIpaClientException {
        for (String user : userIds) {
            verify(freeIpaClient, times(0)).deleteUser(user);
        }
    }

    private User createUser(String userId) {
        User user = new User();
        user.setUid(userId);
        return user;
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn("envCrn");
        stack.setAccountId("accountId");
        return stack;
    }
}
