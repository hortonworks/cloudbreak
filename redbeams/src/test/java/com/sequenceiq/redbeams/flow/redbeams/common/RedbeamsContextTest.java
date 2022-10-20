package com.sequenceiq.redbeams.flow.redbeams.common;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.domain.stack.DBStack;

public class RedbeamsContextTest {

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

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        underTest = new RedbeamsContext(flowParameters, cloudContext, cloudCredential, databaseStack, dbStack);
    }

    @Test
    public void testGetters() {
        assertEquals(cloudContext, underTest.getCloudContext());
        assertEquals(cloudCredential, underTest.getCloudCredential());
        assertEquals(databaseStack, underTest.getDatabaseStack());
        assertEquals(dbStack, underTest.getDBStack());
    }

}
