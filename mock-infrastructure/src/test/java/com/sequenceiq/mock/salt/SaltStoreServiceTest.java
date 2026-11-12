package com.sequenceiq.mock.salt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltMaster;

class SaltStoreServiceTest {

    private static final String MOCK_UUID = "uuid";

    private final SaltStoreService underTest = new SaltStoreService();

    @BeforeEach
    void setUp() {
        underTest.create(MOCK_UUID);
    }

    @Test
    void runActionRegistersItsMinions() {
        SaltAction run = new SaltAction(SaltActionType.RUN);
        run.addMinion(minion("host-1"));

        underTest.setSaltAction(MOCK_UUID, run);

        assertThat(underTest.getMinions(MOCK_UUID)).extracting(Minion::getHostName).containsExactly("host-1");
    }

    @Test
    void stopActionWithoutMinionsKeepsTheRegisteredMinions() {
        SaltAction run = new SaltAction(SaltActionType.RUN);
        run.addMinion(minion("host-1"));
        underTest.setSaltAction(MOCK_UUID, run);
        SaltAction stop = new SaltAction(SaltActionType.STOP);
        SaltMaster master = new SaltMaster();
        master.setAddress("10.0.0.1");
        stop.addMaster(master);

        underTest.setSaltAction(MOCK_UUID, stop);

        assertThat(underTest.read(MOCK_UUID).getSaltAction()).isSameAs(stop);
        assertThat(underTest.getMinions(MOCK_UUID)).extracting(Minion::getHostName).containsExactly("host-1");
    }

    private Minion minion(String hostName) {
        Minion minion = new Minion();
        minion.setHostName(hostName);
        return minion;
    }
}
