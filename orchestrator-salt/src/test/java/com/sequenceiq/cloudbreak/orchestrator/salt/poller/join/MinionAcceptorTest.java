package com.sequenceiq.cloudbreak.orchestrator.salt.poller.join;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Fingerprint;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.FingerprintsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionFingersOnMasterResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionKeysOnMasterResponse;

class MinionAcceptorTest {

    private SaltConnector sc = mock(SaltConnector.class);

    @Test
    public void handleIfAMinionInUnacceptedAndDeniedAtTheSameTime() {
        MinionKeysOnMasterResponse response = mock(MinionKeysOnMasterResponse.class);

        Minion m1 = new Minion();
        m1.setHostName("m1");
        m1.setDomain("d");
        Minion m2 = new Minion();
        m2.setHostName("m2");
        m2.setDomain("d");
        Minion m3 = new Minion();
        m3.setHostName("m3");
        m3.setDomain("d");

        when(response.getAllMinions()).thenReturn(List.of("m1.d", "m2.d", "m3.d"));
        when(response.getDeniedMinions()).thenReturn(List.of("m2.d", "m3.d"));
        when(response.getUnacceptedMinions()).thenReturn(List.of("m2.d", "m3.d"));
        when(sc.wheel(eq("key.list_all"), isNull(), eq(MinionKeysOnMasterResponse.class))).thenReturn(response);

        MinionAcceptor underTest = new MinionAcceptor(List.of(sc), List.of(m1, m2, m3), new EqualMinionFpMatcher(), new FingerprintFromSbCollector());

        CloudbreakOrchestratorFailedException exception = assertThrows(CloudbreakOrchestratorFailedException.class, underTest::acceptMinions);
        assertEquals("There were minions in denied and unaccepted state at the same time: [m2.d, m3.d]", exception.getMessage());
    }

    @Test
    public void fetchUnacceptedMinionsFromMasterMissingMinions() {
        MinionKeysOnMasterResponse response = mock(MinionKeysOnMasterResponse.class);

        Minion m1 = new Minion();
        m1.setHostName("m1");
        m1.setDomain("d");
        Minion m2 = new Minion();
        m2.setHostName("m2");
        m2.setDomain("d");

        when(response.getAllMinions()).thenReturn(List.of("m2.d"));
        when(sc.wheel(eq("key.list_all"), isNull(), eq(MinionKeysOnMasterResponse.class))).thenReturn(response);

        MinionAcceptor underTest = new MinionAcceptor(List.of(sc), List.of(m1, m2), new EqualMinionFpMatcher(), new FingerprintFromSbCollector());

        assertThrows(CloudbreakOrchestratorFailedException.class, underTest::acceptMinions);
    }

    @Test
    public void testEmptyUnacceptedMinionFromMaster() throws CloudbreakOrchestratorFailedException {
        MinionKeysOnMasterResponse response = mock(MinionKeysOnMasterResponse.class);

        Minion m1 = new Minion();
        m1.setHostName("m1");
        m1.setDomain("d");
        Minion m2 = new Minion();
        m2.setHostName("m2");
        m2.setDomain("d");

        when(response.getAllMinions()).thenReturn(List.of("m2.d", "m1.d"));
        when(response.getUnacceptedMinions()).thenReturn(List.of());
        when(sc.wheel(eq("key.list_all"), isNull(), eq(MinionKeysOnMasterResponse.class))).thenReturn(response);

        MinionAcceptor underTest = new MinionAcceptor(List.of(sc), List.of(m1, m2), new EqualMinionFpMatcher(), new FingerprintFromSbCollector());
        underTest.acceptMinions();
    }

    @Test
    public void testFetchFingerprintsFromMasterFails() {
        MinionKeysOnMasterResponse response = mock(MinionKeysOnMasterResponse.class);

        Minion m1 = new Minion();
        m1.setHostName("m1");
        m1.setDomain("d");
        Minion m2 = new Minion();
        m2.setHostName("m2");
        m2.setDomain("d");

        when(response.getAllMinions()).thenReturn(List.of("m2.d", "m1.d"));
        when(response.getUnacceptedMinions()).thenReturn(List.of("m2.d", "m1.d"));
        when(sc.wheel(eq("key.list_all"), isNull(), eq(MinionKeysOnMasterResponse.class))).thenReturn(response);
        when(sc.wheel(eq("key.finger"), anyCollection(), eq(MinionFingersOnMasterResponse.class))).thenThrow(new RuntimeException("failure"));

        MinionAcceptor underTest = new MinionAcceptor(List.of(sc), List.of(m1, m2), new EqualMinionFpMatcher(), new FingerprintFromSbCollector());

        assertThrows(CloudbreakOrchestratorFailedException.class, underTest::acceptMinions);
    }

    @Test
    public void testFetchFingerprintsFromMasterValidation() {
        MinionKeysOnMasterResponse keysOnMasterResponse = mock(MinionKeysOnMasterResponse.class);
        MinionFingersOnMasterResponse fingersOnMasterResponse = mock(MinionFingersOnMasterResponse.class);

        Minion m1 = new Minion();
        m1.setHostName("m1");
        m1.setDomain("d");
        Minion m2 = new Minion();
        m2.setHostName("m2");
        m2.setDomain("d");

        when(keysOnMasterResponse.getAllMinions()).thenReturn(List.of("m2.d", "m1.d"));
        when(keysOnMasterResponse.getUnacceptedMinions()).thenReturn(List.of("m2.d", "m1.d"));
        when(fingersOnMasterResponse.getUnacceptedMinions()).thenReturn(Map.of("m1.d", "finger1"));
        when(sc.wheel(eq("key.list_all"), isNull(), eq(MinionKeysOnMasterResponse.class))).thenReturn(keysOnMasterResponse);
        when(sc.wheel(eq("key.finger"), anyCollection(), eq(MinionFingersOnMasterResponse.class))).thenReturn(fingersOnMasterResponse);

        MinionAcceptor underTest = new MinionAcceptor(List.of(sc), List.of(m1, m2), new EqualMinionFpMatcher(), new FingerprintFromSbCollector());

        assertThrows(CloudbreakOrchestratorFailedException.class, underTest::acceptMinions);
    }

    @Test
    public void testAllMinionsAcceptedWithMatchingFingerprint() throws CloudbreakOrchestratorFailedException {
        MinionKeysOnMasterResponse keysOnMasterResponse = mock(MinionKeysOnMasterResponse.class);
        MinionFingersOnMasterResponse fingersOnMasterResponse = mock(MinionFingersOnMasterResponse.class);
        FingerprintFromSbCollector fingerprintCollector = mock(FingerprintFromSbCollector.class);
        FingerprintsResponse fingerprintsResponse = mock(FingerprintsResponse.class);

        Minion m1 = new Minion();
        m1.setHostName("m1");
        m1.setDomain("d");
        m1.setAddress("1.1.1.1");
        Minion m2 = new Minion();
        m2.setHostName("m2");
        m2.setDomain("d");
        m2.setAddress("1.1.1.2");
        Minion m3 = new Minion();
        m3.setHostName("m3");
        m3.setDomain("d");
        m3.setAddress("1.1.1.3");
        Minion m4 = new Minion();
        m4.setHostName("m4");
        m4.setDomain("d");
        m4.setAddress("1.1.1.4");

        Fingerprint fp1 = new Fingerprint();
        fp1.setFingerprint("finger1");
        fp1.setAddress("1.1.1.1");
        Fingerprint fp2 = new Fingerprint();
        fp2.setFingerprint("finger2");
        fp2.setAddress("1.1.1.2");
        Fingerprint fp3 = new Fingerprint();
        fp3.setFingerprint("badFinger");
        fp3.setAddress("1.1.1.3");

        when(keysOnMasterResponse.getAllMinions()).thenReturn(List.of("m2.d", "m1.d", "m3.d", "m4.d"));
        when(keysOnMasterResponse.getUnacceptedMinions()).thenReturn(List.of("m2.d", "m1.d", "m3.d"));
        when(fingersOnMasterResponse.getUnacceptedMinions()).thenReturn(Map.of("m1.d", "finger1", "m2.d", "finger2", "m3.d", "finger3"));
        when(sc.wheel(eq("key.list_all"), isNull(), eq(MinionKeysOnMasterResponse.class))).thenReturn(keysOnMasterResponse);
        when(sc.wheel(eq("key.finger"), anyCollection(), eq(MinionFingersOnMasterResponse.class))).thenReturn(fingersOnMasterResponse);
        when(fingerprintCollector.collectFingerprintFromMinions(eq(sc), argThat(arg -> arg.containsAll(List.of(m1, m2, m3))))).thenReturn(fingerprintsResponse);
        when(fingerprintsResponse.getFingerprints()).thenReturn(List.of(fp2, fp1, fp3));

        MinionAcceptor underTest = spy(new MinionAcceptor(List.of(sc), List.of(m1, m2, m3, m4),  new EqualMinionFpMatcher(), fingerprintCollector));

        underTest.acceptMinions();

        verify(sc).wheel(eq("key.accept"), argThat(arg -> arg.containsAll(List.of("m2.d", "m1.d"))), eq(Object.class));
    }

    @Test
    public void handleIfMinionDenied() throws Exception {
        MinionKeysOnMasterResponse response = mock(MinionKeysOnMasterResponse.class);

        Minion m1 = new Minion();
        m1.setHostName("m1");
        m1.setDomain("d");
        Minion m2 = new Minion();
        m2.setHostName("m2");
        m2.setDomain("d");
        Minion m3 = new Minion();
        m3.setHostName("m3");
        m3.setDomain("d");

        when(response.getAllMinions()).thenReturn(List.of("m1.d", "m2.d", "m3.d"));
        when(response.getDeniedMinions()).thenReturn(List.of("m2.d", "m3.d"));
        when(response.getUnacceptedMinions()).thenReturn(List.of());
        when(sc.wheel(eq("key.list_all"), isNull(), eq(MinionKeysOnMasterResponse.class))).thenReturn(response);

        MinionAcceptor underTest = new MinionAcceptor(List.of(sc), List.of(m1, m2, m3), new EqualMinionFpMatcher(), new FingerprintFromSbCollector());
        underTest.acceptMinions();
    }
}