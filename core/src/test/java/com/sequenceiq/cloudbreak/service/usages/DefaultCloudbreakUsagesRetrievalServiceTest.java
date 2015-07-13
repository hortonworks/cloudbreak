package com.sequenceiq.cloudbreak.service.usages;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.jpa.domain.Specification;

import com.sequenceiq.cloudbreak.domain.CbUsageFilterParameters;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository;

public class DefaultCloudbreakUsagesRetrievalServiceTest {
    private static final String DUMMY_ACCOUNT = "account";
    private static final Long DUMMY_SINCE = new Date().getTime();
    private static final Long DUMMY_END_DATE = new Date().getTime();
    private static final String DUMMY_OWNER = "owner";
    private static final String DUMMY_REGION = "region";
    private static final String DUMMY_CLOUD = "GCP";

    @InjectMocks
    private DefaultCloudbreakUsagesRetrievalService underTest;

    private CbUsageFilterParameters filterParameters;

    private CloudbreakUsage usage;

    @Mock
    private CloudbreakUsageRepository cloudbreakUsageRepository;

    @Before
    public void setUp() {
        underTest = new DefaultCloudbreakUsagesRetrievalService();
        filterParameters = new CbUsageFilterParameters.Builder()
                .setFilterEndDate(DUMMY_END_DATE).setAccount(DUMMY_ACCOUNT).setSince(DUMMY_SINCE)
                .setOwner(DUMMY_OWNER).setRegion(DUMMY_REGION).setCloud(DUMMY_CLOUD).build();
        usage = new CloudbreakUsage();
        usage.setAccount(DUMMY_ACCOUNT);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testFindUsagesFor() {
        // GIVEN
        given(cloudbreakUsageRepository.findAll(any(Specification.class))).willReturn(Arrays.asList(usage));
        // WHEN
        List<CloudbreakUsage> result = underTest.findUsagesFor(filterParameters);
        // THEN
        verify(cloudbreakUsageRepository, times(1)).findAll(any(Specification.class));
        assertEquals(usage, result.get(0));
    }

    @Test
    public void testFindUsagesForWhenUsagesNotFound() {
        // GIVEN
        given(cloudbreakUsageRepository.findAll(any(Specification.class))).willReturn(new ArrayList<CloudbreakUsage>());
        // WHEN
        List<CloudbreakUsage> result = underTest.findUsagesFor(filterParameters);
        // THEN
        verify(cloudbreakUsageRepository, times(1)).findAll(any(Specification.class));
        assertEquals(0, result.size());
    }
}
