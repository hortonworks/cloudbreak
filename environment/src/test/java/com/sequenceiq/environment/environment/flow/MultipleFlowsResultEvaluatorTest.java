package com.sequenceiq.environment.environment.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class MultipleFlowsResultEvaluatorTest {

    @Mock
    private FlowEndpoint flowEndpoint;

    @InjectMocks
    private MultipleFlowsResultEvaluator underTest;

    // @formatter:off
    // CHECKSTYLE:OFF
    public static Object[][] scenarios() {
        return new Object[][] {
                // testName                     flow1Active flow1Failed flow2Active flow2Result anyFailed
                { "Flow1 and Flow2 finished",   false,      false,      false,      false,      false },
                { "Flow1 active",               true,       false,      false,      false,      true  },
                { "Flow1 failed",               false,      true,       false,      false,      true  },
                { "Flow2 active",               false,      false,      true,       false,      true  },
                { "Flow2 failed",               false,      false,      false,      true,       true  },
                { "Flow1 and Flow2 active",     true,       false,      true,       false,      true  },
                { "Flow1 and Flow2 failed",     false,      true,       false,      true,       true  },
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void testAnyFailed(String testName, boolean flow1Active, boolean flow1Failed, boolean flow2Active, boolean flow2Failed, boolean expectedResult) {
        FlowIdentifier flowId1 = createFlowIdentifier();
        FlowIdentifier flowId2 = createFlowIdentifier();
        FlowCheckResponse checkResponse1 = createFlowCheckResponse(flowId1, flow1Active, flow1Failed);
        FlowCheckResponse checkResponse2 = createFlowCheckResponse(flowId2, flow2Active, flow2Failed);
        lenient().when(flowEndpoint.hasFlowRunningByFlowId(flowId1.getPollableId())).thenReturn(checkResponse1);
        lenient().when(flowEndpoint.hasFlowRunningByFlowId(flowId2.getPollableId())).thenReturn(checkResponse2);

        boolean result = underTest.anyFailed(List.of(flowId1, flowId2));
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void testNoFlowIds() {
        assertThat(underTest.anyFailed(List.of())).isFalse();
    }

    private FlowIdentifier createFlowIdentifier() {
        return new FlowIdentifier(FlowType.FLOW, UUID.randomUUID().toString());
    }

    private FlowCheckResponse createFlowCheckResponse(FlowIdentifier flowId, boolean flowActive, boolean flowFailed) {
        FlowCheckResponse checkResponse = new FlowCheckResponse();
        checkResponse.setFlowId(flowId.getPollableId());
        checkResponse.setHasActiveFlow(flowActive);
        checkResponse.setLatestFlowFinalizedAndFailed(flowFailed);
        return checkResponse;
    }
    // CHECKSTYLE:ON
    // @formatter:on
}
