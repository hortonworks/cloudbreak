package com.sequenceiq.consumption.flow.consumption.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.common.event.ResourceCrnPayload;
import com.sequenceiq.consumption.converter.CredentialResponseToCloudCredentialConverter;
import com.sequenceiq.consumption.domain.Consumption;
import com.sequenceiq.consumption.flow.consumption.ConsumptionContext;
import com.sequenceiq.consumption.flow.consumption.storage.event.StorageConsumptionCollectionStateSelectors;
import com.sequenceiq.consumption.service.ConsumptionService;
import com.sequenceiq.consumption.service.CredentialService;
import com.sequenceiq.flow.core.FlowParameters;

@ExtendWith(MockitoExtension.class)
public class AbstractStorageConsumptionCollectionActionTest {

    private static final long RESOURCE_ID = 1L;

    private static final String RESOURCE_CRN = "resourceCrn";

    @Mock
    private ConsumptionService consumptionService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialResponseToCloudCredentialConverter credentialConverter;

    @InjectMocks
    private TestAction underTest;

    private Consumption consumption;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StateContext<StorageConsumptionCollectionState, StorageConsumptionCollectionStateSelectors> stateContext;

    @BeforeEach
    public void setUp() {
        consumption = new Consumption();
        consumption.setId(RESOURCE_ID);
    }

    @Test
    public void testCreateFlowContext() {
        when(consumptionService.findConsumptionById(RESOURCE_ID)).thenReturn(consumption);

        ConsumptionContext ctx = underTest.createFlowContext(flowParameters, stateContext, new TestPayload());

        assertEquals(consumption, ctx.getConsumption());
    }

    private static class TestPayload implements ResourceCrnPayload {
        @Override
        public Long getResourceId() {
            return RESOURCE_ID;
        }

        @Override
        public String getResourceCrn() {
            return RESOURCE_CRN;
        }
    }

    private static class TestAction extends AbstractStorageConsumptionCollectionAction<TestPayload> {
        TestAction() {
            super(TestPayload.class);
        }

        @Override
        protected void doExecute(ConsumptionContext context, TestPayload payload, Map<Object, Object> variables) throws Exception {
            sendEvent(context);
        }
    }
}
