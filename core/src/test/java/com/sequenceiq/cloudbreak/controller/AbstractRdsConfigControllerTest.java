package com.sequenceiq.cloudbreak.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RDSTestRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsTestResult;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

public class AbstractRdsConfigControllerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private RdsConfigV3Controller underTest;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private ConversionService conversionService;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testRdsConnectionTestIfArgumentsAreNull() {
        thrown.expect(BadRequestException.class);

        underTest.testRdsConnection(new RDSTestRequest(), null);

        verifyZeroInteractions(rdsConfigService);
        verifyZeroInteractions(conversionService);
    }

    @Test
    public void testRdsConnectionTestWithExistingRdsConfigName() {
        String expectedConnectionResult = "connected";
        when(rdsConfigService.testRdsConnection(anyString(), any())).thenReturn(expectedConnectionResult);

        RDSTestRequest rdsTestRequest = new RDSTestRequest();
        rdsTestRequest.setName("TestRdsConfig");
        RdsTestResult result = underTest.testRdsConnection(rdsTestRequest, null);

        verify(rdsConfigService, times(1)).testRdsConnection(anyString(), any());
        verifyZeroInteractions(conversionService);
        assertEquals(expectedConnectionResult, result.getConnectionResult());
    }

    @Test
    public void testRdsConnectionTestWithRdsConfigRequest() {
        String expectedConnectionResult = "connected";
        when(rdsConfigService.testRdsConnection(any(RDSConfig.class))).thenReturn(expectedConnectionResult);
        when(conversionService.convert(any(RDSConfigRequest.class), eq(RDSConfig.class))).thenReturn(new RDSConfig());

        RDSTestRequest rdsTestRequest = new RDSTestRequest();
        rdsTestRequest.setRdsConfig(new RDSConfigRequest());
        RdsTestResult result = underTest.testRdsConnection(rdsTestRequest, null);

        verify(rdsConfigService, times(1)).testRdsConnection(any(RDSConfig.class));
        verify(conversionService, times(1)).convert(any(RDSConfigRequest.class), eq(RDSConfig.class));
        assertEquals(expectedConnectionResult, result.getConnectionResult());
    }

}
