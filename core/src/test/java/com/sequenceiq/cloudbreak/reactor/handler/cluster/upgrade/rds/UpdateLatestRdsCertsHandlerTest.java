package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.rotate.RotateRdsCertificateService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpdateLatestCertsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpdateLatestCertsResult;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class UpdateLatestRdsCertsHandlerTest {

    private static final Long STACK_ID = 12L;

    private static final TargetMajorVersion TARGET_MAJOR_VERSION = TargetMajorVersion.VERSION_11;

    @Mock
    private RotateRdsCertificateService rotateRdsCertificateService;

    @Mock
    private HandlerEvent<UpgradeRdsUpdateLatestCertsRequest> event;

    @InjectMocks
    private UpdateLatestRdsCertsHandler underTest;

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo("UPGRADERDSUPDATELATESTCERTSREQUEST");
    }

    @Test
    void testDefaultFailureEvent() {
        UpgradeRdsUpdateLatestCertsRequest request = new UpgradeRdsUpdateLatestCertsRequest(STACK_ID, TARGET_MAJOR_VERSION);

        Selectable defaultFailureEvent = underTest.defaultFailureEvent(STACK_ID, new RuntimeException(), Event.wrap(request));

        assertEquals("UPGRADERDSFAILEDEVENT", defaultFailureEvent.selector());
    }

    @Test
    void doAcceptPushesLatestCertificateBundle() {
        UpgradeRdsUpdateLatestCertsRequest request = new UpgradeRdsUpdateLatestCertsRequest(STACK_ID, TARGET_MAJOR_VERSION);
        when(event.getData()).thenReturn(request);

        Selectable result = underTest.doAccept(event);

        verify(rotateRdsCertificateService).updateLatestRdsCertificate(STACK_ID);
        assertThat(result).isInstanceOf(UpgradeRdsUpdateLatestCertsResult.class);
        assertThat(result.selector()).isEqualTo("UPGRADERDSUPDATELATESTCERTSRESULT");
        assertEquals(STACK_ID, ((UpgradeRdsUpdateLatestCertsResult) result).getResourceId());
        assertEquals(TARGET_MAJOR_VERSION, ((UpgradeRdsUpdateLatestCertsResult) result).getVersion());
    }
}
