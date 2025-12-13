package com.sequenceiq.environment.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@ExtendWith(MockitoExtension.class)
class EnvironmentStructuredFlowEventFactoryTest {

    @Mock
    private Clock clock;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EnvironmentDtoConverter environmentDtoConverter;

    @Mock
    private NodeConfig nodeConfig;

    @InjectMocks
    private EnvironmentStructuredFlowEventFactory underTest;

    @Test
    void createStructuredFlowEventWithUserCrn() {
        String userCrn = "exampleCrn";
        FlowDetails flowDetails = mock(FlowDetails.class);
        Environment environment = mock(Answers.RETURNS_DEEP_STUBS);
        when(environmentService.findEnvironmentByIdOrThrow(anyLong())).thenReturn(environment);

        CDPStructuredFlowEvent cdpStructuredFlowEvent = ThreadBasedUserCrnProvider.doAs(userCrn,
                () -> underTest.createStructuredFlowEvent(1L, flowDetails));

        assertNotNull(cdpStructuredFlowEvent);
        assertEquals(userCrn, cdpStructuredFlowEvent.getOperation().getUserCrn());
    }
}