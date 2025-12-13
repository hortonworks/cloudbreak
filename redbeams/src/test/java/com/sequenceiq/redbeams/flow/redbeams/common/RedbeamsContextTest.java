package com.sequenceiq.redbeams.flow.redbeams.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.domain.stack.DBStack;

@ExtendWith(MockitoExtension.class)
class RedbeamsContextTest {

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private DBStack dbStack;

    private RedbeamsContext underTest;

    @BeforeEach
    public void setUp() throws Exception {
        underTest = new RedbeamsContext(flowParameters, cloudContext, cloudCredential, databaseStack, dbStack);
    }

    @Test
    void testGetters() {
        assertEquals(cloudContext, underTest.getCloudContext());
        assertEquals(cloudCredential, underTest.getCloudCredential());
        assertEquals(databaseStack, underTest.getDatabaseStack());
        assertEquals(dbStack, underTest.getDBStack());
    }

}
