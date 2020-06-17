package com.sequenceiq.cloudbreak.orchestrator.salt.poller.join;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Fingerprint;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.FingerprintRequest;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.FingerprintsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;

class FingerprintCollectorTest {

    private FingerprintCollector underTest = new FingerprintCollector();

    private SaltConnector sc = mock(SaltConnector.class);

    @Test
    public void testAnyExceptionConverted() {
        when(sc.collectFingerPrints(any(FingerprintRequest.class))).thenThrow(new RuntimeException("random"));
        assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.collectFingerprintFromMinions(sc, List.of()));
    }

    @Test
    public void testHttpStatusValidation() {
        FingerprintsResponse response = new FingerprintsResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
        when(sc.collectFingerPrints(any(FingerprintRequest.class))).thenReturn(response);
        assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.collectFingerprintFromMinions(sc, List.of()));
    }

    @Test
    public void testAllMinionsCollectedValidation() {
        FingerprintsResponse response = new FingerprintsResponse();
        response.setStatusCode(HttpStatus.OK.value());
        Fingerprint fp = new Fingerprint();
        fp.setAddress("1.1.1.1");
        response.setFingerprints(List.of(fp));
        when(sc.collectFingerPrints(any(FingerprintRequest.class))).thenReturn(response);

        Minion m1 = new Minion();
        m1.setAddress("1.1.1.1");
        m1.setHostName("m1");
        m1.setDomain("domain");
        Minion m2 = new Minion();
        m2.setAddress("1.1.1.2");
        m2.setHostName("m2");
        m2.setDomain("domain");

        assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.collectFingerprintFromMinions(sc, List.of(m1, m2)));
    }

    @Test
    public void testSuccessfulCollection() throws CloudbreakOrchestratorFailedException {
        FingerprintsResponse response = new FingerprintsResponse();
        response.setStatusCode(HttpStatus.OK.value());
        Fingerprint fp = new Fingerprint();
        fp.setAddress("1.1.1.1");
        fp.setFingerprint("asdf");
        Fingerprint fp2 = new Fingerprint();
        fp2.setAddress("1.1.1.2");
        fp2.setFingerprint("gfsd");
        response.setFingerprints(List.of(fp, fp2));
        when(sc.collectFingerPrints(any(FingerprintRequest.class))).thenReturn(response);

        Minion m1 = new Minion();
        m1.setAddress("1.1.1.1");
        m1.setHostName("m1");
        m1.setDomain("domain");
        Minion m2 = new Minion();
        m2.setAddress("1.1.1.2");
        m2.setHostName("m2");
        m2.setDomain("domain");

        FingerprintsResponse result = underTest.collectFingerprintFromMinions(sc, List.of(m1, m2));

        assertEquals(response, result);
    }
}