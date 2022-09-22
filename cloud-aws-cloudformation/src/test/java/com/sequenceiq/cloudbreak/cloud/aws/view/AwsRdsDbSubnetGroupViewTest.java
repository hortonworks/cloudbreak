package com.sequenceiq.cloudbreak.cloud.aws.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;

@ExtendWith(MockitoExtension.class)
public class AwsRdsDbSubnetGroupViewTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DatabaseServer server;

    private AwsRdsDbSubnetGroupView underTest;

    @BeforeEach
    public void setUp() {
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
