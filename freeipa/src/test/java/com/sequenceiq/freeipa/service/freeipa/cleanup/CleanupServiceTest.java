package com.sequenceiq.freeipa.service.freeipa.cleanup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@RunWith(MockitoJUnitRunner.class)
public class CleanupServiceTest {

    private static final long STACK_ID = 1L;

    @InjectMocks
    private CleanupService cleanupService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

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
}
