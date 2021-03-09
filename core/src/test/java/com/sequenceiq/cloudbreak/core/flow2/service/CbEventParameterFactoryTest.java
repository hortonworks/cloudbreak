package com.sequenceiq.cloudbreak.core.flow2.service;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.flow.core.FlowConstants;

@RunWith(MockitoJUnitRunner.class)
public class CbEventParameterFactoryTest {

    private static final String CRN = "crn";

    @InjectMocks
    private CbEventParameterFactory underTest;

    @Test
    public void testUserServiceReturnCrn() {
        ThreadBasedUserCrnProvider.doAs(CRN, () -> {
            Map<String, Object> eventParameters = underTest.createEventParameters(1L);

            assertEquals(1, eventParameters.size());
            assertEquals(CRN, eventParameters.get(FlowConstants.FLOW_TRIGGER_USERCRN));
        });
    }

    @Test(expected = IllegalStateException.class)
    public void testNoCrn() {
        ThreadBasedUserCrnProvider.doAs(null, () -> {
            Map<String, Object> eventParameters = underTest.createEventParameters(1L);

            assertEquals(0, eventParameters.size());
        });
    }
}