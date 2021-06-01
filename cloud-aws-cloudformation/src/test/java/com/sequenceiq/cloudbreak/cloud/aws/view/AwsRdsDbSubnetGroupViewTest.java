package com.sequenceiq.cloudbreak.cloud.aws.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;

public class AwsRdsDbSubnetGroupViewTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DatabaseServer server;

    private AwsRdsDbSubnetGroupView underTest;

    @Before
    public void setUp() {
        initMocks(this);

        underTest = new AwsRdsDbSubnetGroupView(server);
    }

    @Test
    public void testDBSubnetGroupName() {
        when(server.getServerId()).thenReturn("myserver");
        assertEquals("dsg-myserver", underTest.getDBSubnetGroupName());
    }

    @Test
    public void testNoDBSubnetGroupName() {
        when(server.getServerId()).thenReturn(null);
        assertNull(underTest.getDBSubnetGroupName());
    }
}
