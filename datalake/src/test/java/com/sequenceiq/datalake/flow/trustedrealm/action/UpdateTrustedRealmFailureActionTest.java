package com.sequenceiq.datalake.flow.trustedrealm.action;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.flow.SdxContext;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.trustedrealm.UpdateTrustedRealmTrackerEvent;
import com.sequenceiq.datalake.flow.trustedrealm.event.UpdateTrustedRealmFailureResponse;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.flow.core.ActionTest;

@ExtendWith(MockitoExtension.class)
class UpdateTrustedRealmFailureActionTest extends ActionTest {

    private static final long DATALAKE_ID = 1L;

    private static final String CAUSE = "cause";

    @Mock
    private SdxStatusService sdxStatusService;

    @InjectMocks
    private UpdateTrustedRealmFailureAction underTest;

    private SdxContext context;

    @Mock
    private UpdateTrustedRealmFailureResponse payload;

    @BeforeEach
    void setUp() {
        context = new SdxContext(flowParameters, DATALAKE_ID, "user-id");
        lenient().when(payload.getException()).thenReturn(new Exception(CAUSE));
    }

    @Test
    void createRequest() {
        Selectable result = underTest.createRequest(context);

        Assertions.assertThat(result)
                .isInstanceOf(SdxEvent.class)
                .extracting(SdxEvent.class::cast)
                .returns(UpdateTrustedRealmTrackerEvent.UPDATE_TRUSTED_REALM_FAIL_HANDLED_EVENT.selector(), SdxEvent::selector)
                .returns(context.getSdxId(), SdxEvent::getResourceId)
                .returns(context.getUserId(), SdxEvent::getUserId);
    }

    @Test
    void doExecute() throws Exception {
        underTest.doExecute(context, payload, Map.of());

        verifySendEvent();
        verify(sdxStatusService).setStatusForDatalakeAndNotify(DatalakeStatusEnum.DATALAKE_UPDATE_TRUSTED_REALM_FAILED, CAUSE, DATALAKE_ID);
    }

}
