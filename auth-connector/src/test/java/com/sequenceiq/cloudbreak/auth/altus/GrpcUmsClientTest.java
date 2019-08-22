package com.sequenceiq.cloudbreak.auth.altus;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.auth.altus.config.UmsConfig;

@RunWith(MockitoJUnitRunner.class)
public class GrpcUmsClientTest {

    @Mock
    private UmsConfig umsConfigMock;

    @InjectMocks
    private GrpcUmsClient testedClass = new GrpcUmsClient();

    @Test
    public void checkIfReadRightCorrect() {
        assertTrue(testedClass.isReadRight("environment/read"));
    }

    @Test
    public void checkIfReadRightInvalid() {
        assertFalse(testedClass.isReadRight("environmentsinvalidread"));
    }

    @Test
    public void checkIfReadRightRight() {
        assertFalse(testedClass.isReadRight("datalake/write"));
        assertFalse(testedClass.isReadRight("datahub/write"));
    }

    @Test
    public void checkIfReadRightNull() {
        assertFalse(testedClass.isReadRight(null));
    }
}
