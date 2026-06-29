package com.sequenceiq.cloudbreak.orchestrator.salt.poller.join;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.dyngr.core.AttemptState;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Fingerprint;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.FingerprintsResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionFingersOnMasterResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionIpAddressesResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionKeysOnMasterResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;

class MinionAcceptorTest {

    private final SaltConnector sc = mock(SaltConnector.class);

    private final SaltStateService saltStateService = mock(SaltStateService.class);

    @BeforeEach
    void init() {
        when(sc.getHostname()).thenReturn("hostname.domain");
        MinionIpAddressesResponse minionIpAddressesResponse = new MinionIpAddressesResponse();
        minionIpAddressesResponse.setResult(List.of(Map.of()));
        when(saltStateService.collectMinionIpAddresses(eq(sc), any())).thenReturn(minionIpAddressesResponse);
    }

    @Test
    void handleIfAMinionInUnacceptedAndDeniedAtTheSameTime() {
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

        MinionAcceptor underTest = new MinionAcceptor(List.of(sc), List.of(m1, m2, m3), List.of(m1, m2, m3),
                new EqualMinionFpMatcher(), new FingerprintFromSbCollector(), saltStateService);

        CloudbreakOrchestratorFailedException exception = assertThrows(CloudbreakOrchestratorFailedException.class, underTest::acceptMinions);
        assertEquals("Minion(s) were removed, restart bootstrap to ensure all minion present", exception.getMessage());
    }

    @Test
    void fetchUnacceptedMinionsFromMasterMissingMinions() {
        MinionKeysOnMasterResponse response = mock(MinionKeysOnMasterResponse.class);

        Minion m1 = new Minion();
        m1.setHostName("m1");
        m1.setDomain("d");
        Minion m2 = new Minion();
        m2.setHostName("m2");
        m2.setDomain("d");

        when(response.getAllMinions()).thenReturn(List.of("m2.d"));
        when(sc.wheel(eq("key.list_all"), isNull(), eq(MinionKeysOnMasterResponse.class))).thenReturn(response);

        MinionAcceptor underTest = new MinionAcceptor(List.of(sc), List.of(m1, m2), List.of(m1, m2),
                new EqualMinionFpMatcher(), new FingerprintFromSbCollector(), saltStateService);

        assertThrows(CloudbreakOrchestratorFailedException.class, underTest::acceptMinions);
    }

    @Test
    void testEmptyUnacceptedMinionFromMaster() throws CloudbreakOrchestratorFailedException {
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

        MinionAcceptor underTest = new MinionAcceptor(List.of(sc), List.of(m1, m2), List.of(m1, m2),
                new EqualMinionFpMatcher(), new FingerprintFromSbCollector(), saltStateService);
        underTest.acceptMinions();
    }

    @Test
    void testOldUnacceptedMinionFromMaster() throws CloudbreakOrchestratorFailedException {
        MinionKeysOnMasterResponse response = mock(MinionKeysOnMasterResponse.class);
        FingerprintCollector fingerprintCollector = mock(FingerprintCollector.class);

        Minion m1 = new Minion();
        m1.setHostName("m1");
        m1.setDomain("d");
        Minion m2 = new Minion();
        m2.setHostName("m2");
        m2.setDomain("d");

        List<String> unacceptedMinions = List.of("md.d");

        when(response.getAllMinions()).thenReturn(List.of("m2.d", "m1.d"));
        when(response.getUnacceptedMinions()).thenReturn(unacceptedMinions);
        when(sc.wheel(eq("key.list_all"), isNull(), eq(MinionKeysOnMasterResponse.class))).thenReturn(response);

        MinionAcceptor underTest = new MinionAcceptor(List.of(sc), List.of(m1, m2), List.of(m1, m2), new EqualMinionFpMatcher(), fingerprintCollector,
                saltStateService);
        // A not-expected ("rogue") minion is removed, but this no longer triggers the bootstrap restart exception
        underTest.acceptMinions();
        verify(sc).wheel(eq("key.delete"), eq(unacceptedMinions), eq(Object.class));
        verify(fingerprintCollector, never()).collectFingerprintFromMinions(any(), any());
    }

    @Test
    void testFetchFingerprintsFromMasterFails() {
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

        MinionAcceptor underTest = new MinionAcceptor(List.of(sc), List.of(m1, m2), List.of(m1, m2),
                new EqualMinionFpMatcher(), new FingerprintFromSbCollector(), saltStateService);

        assertThrows(CloudbreakOrchestratorFailedException.class, underTest::acceptMinions);
    }

    @Test
    void testFetchFingerprintsFromMasterValidation() {
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

        MinionAcceptor underTest = new MinionAcceptor(List.of(sc), List.of(m1, m2), List.of(m1, m2),
                new EqualMinionFpMatcher(), new FingerprintFromSbCollector(), saltStateService);

        assertThrows(CloudbreakOrchestratorFailedException.class, underTest::acceptMinions);
    }

    @Test
    void testNonMatchingFringerprint() throws CloudbreakOrchestratorFailedException {
        MinionKeysOnMasterResponse keysOnMasterResponse = mock(MinionKeysOnMasterResponse.class);
        MinionFingersOnMasterResponse fingersOnMasterResponse = mock(MinionFingersOnMasterResponse.class);
        FingerprintFromSbCollector fingerprintCollector = mock(FingerprintFromSbCollector.class);
        FingerprintsResponse fingerprintsResponse = mock(FingerprintsResponse.class);

        Minion m1 = new Minion();
        m1.setHostName("m1");
        m1.setDomain("d");
        m1.setAddress("1.2.3.4");
        Minion m2 = new Minion();
        m2.setHostName("m2");
        m2.setDomain("d");

        when(keysOnMasterResponse.getAllMinions()).thenReturn(List.of("m2.d", "m1.d"));
        when(keysOnMasterResponse.getUnacceptedMinions()).thenReturn(List.of("m1.d"));
        when(fingersOnMasterResponse.getUnacceptedMinions()).thenReturn(Map.of("m1.d", "finger1"));
        when(sc.wheel(eq("key.list_all"), isNull(), eq(MinionKeysOnMasterResponse.class))).thenReturn(keysOnMasterResponse);
        when(sc.wheel(eq("key.finger"), anyCollection(), eq(MinionFingersOnMasterResponse.class))).thenReturn(fingersOnMasterResponse);
        Fingerprint fingerprint = new Fingerprint();
        fingerprint.setAddress("1.2.3.4");
        fingerprint.setFingerprint("different");
        when(fingerprintsResponse.getFingerprints()).thenReturn(List.of(fingerprint));
        when(fingerprintCollector.collectFingerprintFromMinions(eq(sc), argThat(arg -> arg.containsAll(List.of(m1))))).thenReturn(fingerprintsResponse);

        MinionAcceptor underTest = new MinionAcceptor(List.of(sc), List.of(m1, m2), List.of(m1, m2), new EqualMinionFpMatcher(), fingerprintCollector,
                saltStateService);

        assertThrows(CloudbreakOrchestratorFailedException.class, underTest::acceptMinions);
    }

    private static List<List<String>> testAllMinionsAcceptedWithMatchingFingerprintParams() {
        return List.of(
                // Same saltboot https setting on all nodes
                List.of("", "", ""),
                // Different saltboot https setting on some nodes
                List.of(":7070", ":7071", ":7070")
        );
    }

    @ParameterizedTest
    @MethodSource("testAllMinionsAcceptedWithMatchingFingerprintParams")
    void testAllMinionsAcceptedWithMatchingFingerprint(List<String> ports) throws CloudbreakOrchestratorFailedException {
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
        fp1.setAddress("1.1.1.1:" + ports.get(0));
        Fingerprint fp2 = new Fingerprint();
        fp2.setFingerprint("finger2");
        fp2.setAddress("1.1.1.2:" + ports.get(1));
        Fingerprint fp3 = new Fingerprint();
        fp3.setFingerprint("badFinger");
        fp3.setAddress("1.1.1.3:" + ports.get(2));

        when(keysOnMasterResponse.getAllMinions()).thenReturn(List.of("m2.d", "m1.d", "m3.d", "m4.d"));
        when(keysOnMasterResponse.getUnacceptedMinions()).thenReturn(List.of("m2.d", "m1.d", "m3.d"));
        when(fingersOnMasterResponse.getUnacceptedMinions()).thenReturn(Map.of("m1.d", "finger1", "m2.d", "finger2", "m3.d", "finger3"));
        when(sc.wheel(eq("key.list_all"), isNull(), eq(MinionKeysOnMasterResponse.class))).thenReturn(keysOnMasterResponse);
        when(sc.wheel(eq("key.finger"), anyCollection(), eq(MinionFingersOnMasterResponse.class))).thenReturn(fingersOnMasterResponse);
        when(fingerprintCollector.collectFingerprintFromMinions(eq(sc), argThat(arg -> arg.containsAll(List.of(m1, m2, m3))))).thenReturn(fingerprintsResponse);
        when(fingerprintsResponse.getFingerprints()).thenReturn(List.of(fp2, fp1, fp3));

        MinionAcceptor underTest = spy(new MinionAcceptor(List.of(sc), List.of(m1, m2, m3, m4), List.of(m1, m2, m3, m4),
                new EqualMinionFpMatcher(), fingerprintCollector, saltStateService));

        assertThrows(CloudbreakOrchestratorFailedException.class, underTest::acceptMinions);
    }

    @Test
    void handleIfMinionDenied() throws Exception {
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

        MinionAcceptor underTest = new MinionAcceptor(List.of(sc), List.of(m1, m2, m3), List.of(m1, m2, m3),
                new EqualMinionFpMatcher(), new FingerprintFromSbCollector(), saltStateService);
        underTest.acceptMinions();
    }

    @Test
    void shouldPropagateWhenDeletePollingFails() throws Exception {
        MinionKeysOnMasterResponse response = mock(MinionKeysOnMasterResponse.class);

        Minion m1 = new Minion();
        m1.setHostName("m1");
        m1.setDomain("d");
        Minion m2 = new Minion();
        m2.setHostName("m2");
        m2.setDomain("d");

        when(response.getAllMinions()).thenReturn(List.of("m1.d", "m2.d"));
        when(response.getDeniedMinions()).thenReturn(List.of("m2.d"));
        when(response.getUnacceptedMinions()).thenReturn(List.of());
        when(sc.wheel(eq("key.list_all"), isNull(), eq(MinionKeysOnMasterResponse.class))).thenReturn(response);
        MinionIpAddressesResponse minionIpAddressesResponse = new MinionIpAddressesResponse();
        Map<String, JsonNode> minionResult = new HashMap<>();
        minionResult.put("m2.d", JsonNodeFactory.instance.arrayNode().add("10.0.0.2"));
        minionIpAddressesResponse.setResult(List.of(minionResult));
        when(saltStateService.collectMinionIpAddresses(sc, java.util.Optional.of(Set.of("m2.d")))).thenReturn(minionIpAddressesResponse);

        MinionAcceptor underTest = spy(new MinionAcceptor(List.of(sc), List.of(m1, m2), List.of(m1, m2),
                new EqualMinionFpMatcher(), new FingerprintFromSbCollector(), saltStateService));
        when(underTest.getMinionDeletionPollingTimeoutInMinutes()).thenReturn(0L);

        CloudbreakOrchestratorFailedException exception = assertThrows(CloudbreakOrchestratorFailedException.class, underTest::acceptMinions);
        assertEquals("Failed while polling deleted minion keys", exception.getMessage());
    }

    @Test
    void minionDeletionPollerShouldContinueWhenMinionIsPresentAndReachable() {
        MinionIpAddressesResponse minionIpAddressesResponse = new MinionIpAddressesResponse();
        Map<String, JsonNode> minionResult = new HashMap<>();
        minionResult.put("m1.d", JsonNodeFactory.instance.arrayNode().add("10.0.0.2"));
        minionIpAddressesResponse.setResult(List.of(minionResult));
        when(saltStateService.collectMinionIpAddresses(sc, java.util.Optional.of(Set.of("m1.d")))).thenReturn(minionIpAddressesResponse);

        MinionDeletionPoller minionDeletionPoller = new MinionDeletionPoller(sc, Set.of("m1.d"), saltStateService);

        assertEquals(AttemptState.CONTINUE, minionDeletionPoller.process().getState());
        assertEquals(Set.of("m1.d"), minionDeletionPoller.getRemainingReachableMinions());
    }

    @Test
    void minionDeletionPollerShouldFinishWhenMinionIsUnreachable() {
        MinionIpAddressesResponse minionIpAddressesResponse = new MinionIpAddressesResponse();
        Map<String, JsonNode> minionResult = new HashMap<>();
        minionResult.put("m1.d", JsonNodeFactory.instance.textNode("false"));
        minionIpAddressesResponse.setResult(List.of(minionResult));
        when(saltStateService.collectMinionIpAddresses(sc, java.util.Optional.of(Set.of("m1.d")))).thenReturn(minionIpAddressesResponse);

        MinionDeletionPoller minionDeletionPoller = new MinionDeletionPoller(sc, Set.of("m1.d"), saltStateService);

        assertEquals(AttemptState.FINISH, minionDeletionPoller.process().getState());
        assertEquals(Set.of(), minionDeletionPoller.getRemainingReachableMinions());
    }

    @Test
    void knownButNotRequiredMinionIsAcceptedAndNotRemoved() throws CloudbreakOrchestratorFailedException {
        MinionKeysOnMasterResponse keysOnMasterResponse = mock(MinionKeysOnMasterResponse.class);
        MinionFingersOnMasterResponse fingersOnMasterResponse = mock(MinionFingersOnMasterResponse.class);
        FingerprintFromSbCollector fingerprintCollector = mock(FingerprintFromSbCollector.class);
        FingerprintsResponse fingerprintsResponse = mock(FingerprintsResponse.class);

        // m1 is the new upscale node (required), m2 is a pre-existing cluster node whose key is currently pending (known only)
        Minion m1 = new Minion();
        m1.setHostName("m1");
        m1.setDomain("d");
        m1.setAddress("1.1.1.1");
        Minion m2 = new Minion();
        m2.setHostName("m2");
        m2.setDomain("d");
        m2.setAddress("1.1.1.2");

        Fingerprint fp1 = new Fingerprint();
        fp1.setFingerprint("finger1");
        fp1.setAddress("1.1.1.1");
        Fingerprint fp2 = new Fingerprint();
        fp2.setFingerprint("finger2");
        fp2.setAddress("1.1.1.2");

        when(keysOnMasterResponse.getAllMinions()).thenReturn(List.of("m1.d", "m2.d"));
        when(keysOnMasterResponse.getUnacceptedMinions()).thenReturn(List.of("m1.d", "m2.d"));
        when(fingersOnMasterResponse.getUnacceptedMinions()).thenReturn(Map.of("m1.d", "finger1", "m2.d", "finger2"));
        when(sc.wheel(eq("key.list_all"), isNull(), eq(MinionKeysOnMasterResponse.class))).thenReturn(keysOnMasterResponse);
        when(sc.wheel(eq("key.finger"), anyCollection(), eq(MinionFingersOnMasterResponse.class))).thenReturn(fingersOnMasterResponse);
        when(fingerprintsResponse.getFingerprints()).thenReturn(List.of(fp1, fp2));
        when(fingerprintCollector.collectFingerprintFromMinions(eq(sc), argThat(arg -> arg.containsAll(List.of(m1, m2))))).thenReturn(fingerprintsResponse);

        MinionAcceptor underTest = new MinionAcceptor(List.of(sc), List.of(m1), List.of(m1, m2), new EqualMinionFpMatcher(), fingerprintCollector,
                saltStateService);

        underTest.acceptMinions();

        verify(sc, never()).wheel(eq("key.delete"), anyCollection(), eq(Object.class));
        verify(sc).wheel(eq("key.accept"), argThat(arg -> arg.containsAll(List.of("m1.d", "m2.d"))), eq(Object.class));
    }

    @Test
    void rogueMinionIsRemovedButDoesNotTriggerRestart() throws CloudbreakOrchestratorFailedException {
        MinionKeysOnMasterResponse keysOnMasterResponse = mock(MinionKeysOnMasterResponse.class);
        MinionFingersOnMasterResponse fingersOnMasterResponse = mock(MinionFingersOnMasterResponse.class);
        FingerprintFromSbCollector fingerprintCollector = mock(FingerprintFromSbCollector.class);
        FingerprintsResponse fingerprintsResponse = mock(FingerprintsResponse.class);

        Minion m1 = new Minion();
        m1.setHostName("m1");
        m1.setDomain("d");
        m1.setAddress("1.1.1.1");

        Fingerprint fp1 = new Fingerprint();
        fp1.setFingerprint("finger1");
        fp1.setAddress("1.1.1.1");

        // rogue.d is unaccepted on the master but in neither required nor known set
        when(keysOnMasterResponse.getAllMinions()).thenReturn(List.of("m1.d", "rogue.d"));
        when(keysOnMasterResponse.getUnacceptedMinions()).thenReturn(List.of("m1.d", "rogue.d"));
        when(keysOnMasterResponse.getDeniedMinions()).thenReturn(List.of());
        when(fingersOnMasterResponse.getUnacceptedMinions()).thenReturn(Map.of("m1.d", "finger1"));
        when(sc.wheel(eq("key.list_all"), isNull(), eq(MinionKeysOnMasterResponse.class))).thenReturn(keysOnMasterResponse);
        when(sc.wheel(eq("key.finger"), anyCollection(), eq(MinionFingersOnMasterResponse.class))).thenReturn(fingersOnMasterResponse);
        when(fingerprintsResponse.getFingerprints()).thenReturn(List.of(fp1));
        when(fingerprintCollector.collectFingerprintFromMinions(eq(sc), argThat(arg -> arg.containsAll(List.of(m1))))).thenReturn(fingerprintsResponse);

        MinionAcceptor underTest = new MinionAcceptor(List.of(sc), List.of(m1), List.of(m1), new EqualMinionFpMatcher(), fingerprintCollector,
                saltStateService);

        underTest.acceptMinions();

        verify(sc).wheel(eq("key.delete"), eq(List.of("rogue.d")), eq(Object.class));
        verify(sc).wheel(eq("key.accept"), argThat(arg -> arg.containsAll(List.of("m1.d"))), eq(Object.class));
    }

    @Test
    void deniedAndUnacceptedConflictTriggersRestart() {
        MinionKeysOnMasterResponse response = mock(MinionKeysOnMasterResponse.class);

        Minion m1 = new Minion();
        m1.setHostName("m1");
        m1.setDomain("d");
        Minion m2 = new Minion();
        m2.setHostName("m2");
        m2.setDomain("d");

        when(response.getAllMinions()).thenReturn(List.of("m1.d", "m2.d"));
        when(response.getDeniedMinions()).thenReturn(List.of("m2.d"));
        when(response.getUnacceptedMinions()).thenReturn(List.of("m2.d"));
        when(sc.wheel(eq("key.list_all"), isNull(), eq(MinionKeysOnMasterResponse.class))).thenReturn(response);

        MinionAcceptor underTest = new MinionAcceptor(List.of(sc), List.of(m1, m2), List.of(m1, m2), new EqualMinionFpMatcher(),
                new FingerprintFromSbCollector(), saltStateService);

        CloudbreakOrchestratorFailedException exception = assertThrows(CloudbreakOrchestratorFailedException.class, underTest::acceptMinions);
        assertEquals("Minion(s) were removed, restart bootstrap to ensure all minion present", exception.getMessage());
    }
}
