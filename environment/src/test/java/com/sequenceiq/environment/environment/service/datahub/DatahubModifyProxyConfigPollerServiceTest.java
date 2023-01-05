package com.sequenceiq.environment.environment.service.datahub;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.environment.flow.MultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@ExtendWith(MockitoExtension.class)
class DatahubModifyProxyConfigPollerServiceTest {

    private static final String EXTRACTED_MESSAGE = "extracted-message";

    private static final long ENV_ID = 1L;

    private static final String ENV_CRN = "env-crn";

    private static final String PREV_PROXY_CRN = "prev-proxy-crn";

    private static final String DH_CRN_1 = "crn-1";

    private static final String DH_CRN_2 = "crn-2";

    @Mock
    private DatahubService datahubService;

    @Mock
    private DatahubPollerProvider datahubPollerProvider;

    @Mock
    private MultipleFlowsResultEvaluator multipleFlowsResultEvaluator;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private DatahubModifyProxyConfigPollerService underTest;

    @Mock
    private FlowIdentifier flow1;

    @Mock
    private FlowIdentifier flow2;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "attempt", 3);
        ReflectionTestUtils.setField(underTest, "sleeptime", 1);
        StackViewV4Responses stackViewV4Responses = new StackViewV4Responses();
        stackViewV4Responses.setResponses(List.of(aStack(DH_CRN_1), aStack(DH_CRN_2)));
        when(datahubService.list(ENV_CRN)).thenReturn(stackViewV4Responses);
        when(datahubService.modifyProxy(DH_CRN_1, PREV_PROXY_CRN)).thenReturn(flow1);
        when(datahubService.modifyProxy(DH_CRN_2, PREV_PROXY_CRN)).thenReturn(flow2);
        lenient().when(datahubPollerProvider.multipleFlowsPoller(eq(ENV_ID), any())).thenReturn(AttemptResults::justFinish);
        lenient().when(webApplicationExceptionMessageExtractor.getErrorMessage(any())).thenReturn(EXTRACTED_MESSAGE);
    }

    private static StackViewV4Response aStack(String crn) {
        StackViewV4Response stack2 = new StackViewV4Response();
        stack2.setCrn(crn);
        return stack2;
    }

    @Test
    void startModifyProxyFails() {
        DatahubOperationFailedException cause = new DatahubOperationFailedException("error");
        when(datahubService.modifyProxy(DH_CRN_2, PREV_PROXY_CRN)).thenThrow(cause);

        assertThatThrownBy(() -> underTest.modifyProxyOnAttachedDatahubs(ENV_ID, ENV_CRN, PREV_PROXY_CRN))
                .isEqualTo(cause);
    }

    @Test
    void pollerFails() {
        PollerStoppedException cause = new PollerStoppedException("cause");
        when(datahubPollerProvider.multipleFlowsPoller(any(), any())).thenThrow(cause);

        assertThatThrownBy(() -> underTest.modifyProxyOnAttachedDatahubs(ENV_ID, ENV_CRN, PREV_PROXY_CRN))
                .isInstanceOf(DatahubOperationFailedException.class)
                .hasMessage("Data Hub modify proxy config timed out or error happened: cause");
    }

    @Test
    void flowFails() {
        when(multipleFlowsResultEvaluator.anyFailed(any())).thenReturn(true);

        assertThatThrownBy(() -> underTest.modifyProxyOnAttachedDatahubs(ENV_ID, ENV_CRN, PREV_PROXY_CRN))
                .isInstanceOf(DatahubOperationFailedException.class)
                .hasMessage("Data Hub modify proxy config error happened. One or more Data Hubs are not modified.");
    }

    @Test
    void success() {
        underTest.modifyProxyOnAttachedDatahubs(ENV_ID, ENV_CRN, PREV_PROXY_CRN);

        verify(datahubService).list(ENV_CRN);
        verify(datahubService).modifyProxy(DH_CRN_1, PREV_PROXY_CRN);
        verify(datahubService).modifyProxy(DH_CRN_2, PREV_PROXY_CRN);
        List<FlowIdentifier> flowIdentifiers = List.of(flow1, flow2);
        verify(datahubPollerProvider).multipleFlowsPoller(ENV_ID, flowIdentifiers);
        verify(multipleFlowsResultEvaluator).anyFailed(flowIdentifiers);
        verifyNoInteractions(webApplicationExceptionMessageExtractor);
    }

}
