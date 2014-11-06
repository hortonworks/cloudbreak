package com.sequenceiq.cloudbreak.service.usages;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository;

public class DefaultCloudbreakUsageGeneratorServiceTest {

    @InjectMocks
    private DefaultCloudbreakUsageGeneratorService underTest;
    @Mock
    private CloudbreakUsageRepository usageRepository;
    @Mock
    private CloudbreakEventRepository eventRepository;
    @Mock
    private StackUsageGenerator stackUsageGenerator;

    @Before
    public void before() {
        underTest = new DefaultCloudbreakUsageGeneratorService();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGenerateUsagesShouldCallGeneratorOnAllEventsWhenUsagesHasNeverGenerated() {
        //GIVEN
        List<CloudbreakEvent> events = new ArrayList<>();
        List<CloudbreakUsage> usages = new ArrayList<>();
        given(usageRepository.count()).willReturn(0L);
        given(eventRepository.findAll(any(Sort.class))).willReturn(events);
        given(stackUsageGenerator.generate(events)).willReturn(usages);
        //WHEN
        underTest.generate();
        //THEN
        verify(eventRepository).findAll(any(Sort.class));
        verify(usageRepository).save(usages);
    }

    @Test
    public void testGenerateUsagesShouldCallGeneratorOnLastDaysEventsWhenUsagesHasPreviouslyGenerated() {
        //GIVEN
        List<CloudbreakEvent> events = new ArrayList<>();
        List<CloudbreakUsage> usages = new ArrayList<>();
        given(usageRepository.count()).willReturn(1L);
        given(eventRepository.findAll(any(Specification.class), any(Sort.class))).willReturn(events);
        given(stackUsageGenerator.generate(events)).willReturn(usages);
        //WHEN
        underTest.generate();
        //THEN
        verify(eventRepository).findAll(any(Specification.class), any(Sort.class));
        verify(usageRepository).save(usages);
    }
}