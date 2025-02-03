package com.sequenceiq.cloudbreak.orchestrator.salt.poller.join;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Fingerprint;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.FingerprintRequest;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.FingerprintsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;

class FingerprintFromSbCollectorTest {

    private FingerprintFromSbCollector underTest = new FingerprintFromSbCollector();

    private SaltConnector sc = mock(SaltConnector.class);

    @Test
    void testAnyExceptionConverted() {
        when(sc.collectFingerPrints(any(FingerprintRequest.class))).thenThrow(new RuntimeException("random"));
        assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.collectFingerprintFromMinions(sc, List.of()));
    }

    @Test
    void testHttpStatusValidation() {
        FingerprintsResponse response = new FingerprintsResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE.value());
        when(sc.collectFingerPrints(any(FingerprintRequest.class))).thenReturn(response);
        assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.collectFingerprintFromMinions(sc, List.of()));
    }

    @Test
    void testAllMinionsCollectedValidation() {
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

    private static Stream<List<String>> testSuccessfulCollectionParams() {
        return Stream.of(
                // Same saltboot https setting on all nodes
                List.of("", ""),
                // Different saltboot https setting on some nodes
                List.of(":7070", ":7071")
        );
    }

    @ParameterizedTest
    @MethodSource("testSuccessfulCollectionParams")
    void testSuccessfulCollection(List<String> ports) throws CloudbreakOrchestratorFailedException {
        FingerprintsResponse response = new FingerprintsResponse();
        response.setStatusCode(HttpStatus.OK.value());
        Fingerprint fp = new Fingerprint();
        fp.setAddress("1.1.1.1" + ports.get(0));
        fp.setFingerprint("asdf");
        Fingerprint fp2 = new Fingerprint();
        fp2.setAddress("1.1.1.2" + ports.get(1));
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