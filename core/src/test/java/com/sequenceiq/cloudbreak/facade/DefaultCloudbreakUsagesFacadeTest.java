package com.sequenceiq.cloudbreak.facade;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.service.usages.CloudbreakUsageGeneratorService;
import com.sequenceiq.cloudbreak.service.usages.CloudbreakUsagesRetrievalService;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCloudbreakUsagesFacadeTest {

    @Mock
    private CloudbreakUsagesRetrievalService cloudbreakUsagesService;

    @Mock
    private CloudbreakUsageGeneratorService cloudbreakUsageGeneratorService;

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private DefaultCloudbreakUsagesFacade underTest;

    @Test
    public void findUsagesForParametersConvertUsagesToJson() {
        List<CloudbreakUsage> cloudbreakUsages = TestUtil.generateAzureCloudbreakUsages(10);
        when(cloudbreakUsagesService.findUsagesFor(any(CbUsageFilterParameters.class))).thenReturn(cloudbreakUsages);
        when(conversionService.convert(anyObject(), any(TypeDescriptor.class), any(TypeDescriptor.class))).thenReturn(new ArrayList<CloudbreakUsageJson>());

        underTest.getUsagesFor(new CbUsageFilterParameters.Builder().build());

        verify(cloudbreakUsagesService, times(1)).findUsagesFor(any(CbUsageFilterParameters.class));
        verify(conversionService, times(1)).convert(anyObject(), any(TypeDescriptor.class), any(TypeDescriptor.class));
    }

    @Test
    public void generateUserUsagesCallWithoutError() {
        doNothing().when(cloudbreakUsageGeneratorService).generate();

        underTest.generateUserUsages();

        verify(cloudbreakUsageGeneratorService, times(1)).generate();
    }
}