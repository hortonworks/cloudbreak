package com.sequenceiq.cloudbreak.service.usages;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.AwsInstanceType;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.price.AwsPriceGenerator;
import com.sequenceiq.cloudbreak.service.price.PriceGenerator;

public class IntervalStackUsageGeneratorTest {

    private static final Date DUMMY_START_DATE = new Date();
    private static final Date DUMMY_END_DATE = new Date();

    @InjectMocks
    private IntervalStackUsageGenerator underTest;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private IntervalInstanceUsageGenerator instanceUsageGenerator;

    private List<PriceGenerator> priceGenerators;

    private CloudbreakEvent cloudbreakEvent;

    private Stack stack;

    private Map<String, Long> instanceHours;

    @Before
    public void setUp() {
        underTest = new IntervalStackUsageGenerator();
        priceGenerators = new ArrayList<>();
        priceGenerators.add(new AwsPriceGenerator());
        ReflectionTestUtils.setField(underTest, "priceGenerators", priceGenerators);
        stack = TestUtil.stack();
        cloudbreakEvent = TestUtil.azureCloudbreakEvent(stack.getId());
        instanceHours = new HashMap<>();
        instanceHours.put("2012-12-12", 1L);
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGenerateUsages() throws ParseException {
        // GIVEN
        given(stackRepository.findById(anyLong())).willReturn(stack);
        given(instanceUsageGenerator.getInstanceHours(any(InstanceMetaData.class),
                any(Date.class), any(Date.class))).willReturn(instanceHours);
        // WHEN
        List<CloudbreakUsage> result = underTest.generateUsages(DUMMY_START_DATE,
                DUMMY_END_DATE, cloudbreakEvent);
        // THEN
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0).getInstanceHours().longValue());
    }

    @Test
    public void testGenerateUsagesWhenStackNotFound() throws ParseException {
        // GIVEN
        given(stackRepository.findById(anyLong())).willReturn(null);
        // WHEN
        List<CloudbreakUsage> result = underTest.generateUsages(DUMMY_START_DATE,
                DUMMY_END_DATE, cloudbreakEvent);
        // THEN
        assertEquals(0, result.size());
    }

    @Test
    public void testGenerateUsagesWithAwsInstanceType() throws ParseException {
        // GIVEN
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            AwsTemplate template = new AwsTemplate();
            template.setInstanceType(AwsInstanceType.C3Xlarge);
            instanceGroup.setTemplate(template);
        }
        given(stackRepository.findById(anyLong())).willReturn(stack);
        given(instanceUsageGenerator.getInstanceHours(any(InstanceMetaData.class),
                any(Date.class), any(Date.class))).willReturn(instanceHours);
        // WHEN
        List<CloudbreakUsage> result = underTest.generateUsages(DUMMY_START_DATE,
                DUMMY_END_DATE, cloudbreakEvent);
        // THEN
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0).getInstanceHours().longValue());
    }

    @Test
    public void testGenerateUsagesWithGcpInstanceType() throws ParseException {
        // GIVEN
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            AwsTemplate template = new AwsTemplate();
            template.setInstanceType(AwsInstanceType.C3Xlarge);
            instanceGroup.setTemplate(template);
        }
        given(stackRepository.findById(anyLong())).willReturn(stack);
        given(instanceUsageGenerator.getInstanceHours(any(InstanceMetaData.class),
                any(Date.class), any(Date.class))).willReturn(instanceHours);
        // WHEN
        List<CloudbreakUsage> result = underTest.generateUsages(DUMMY_START_DATE,
                DUMMY_END_DATE, cloudbreakEvent);
        // THEN
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0).getInstanceHours().longValue());
    }

    @Test
    public void testGenerateUsagesWithOpenstackInstanceType() throws ParseException {
        // GIVEN
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            AwsTemplate template = new AwsTemplate();
            template.setInstanceType(AwsInstanceType.C3Xlarge);
            instanceGroup.setTemplate(template);
        }
        given(stackRepository.findById(anyLong())).willReturn(stack);
        given(instanceUsageGenerator.getInstanceHours(any(InstanceMetaData.class),
                any(Date.class), any(Date.class))).willReturn(instanceHours);
        // WHEN
        List<CloudbreakUsage> result = underTest.generateUsages(DUMMY_START_DATE,
                DUMMY_END_DATE, cloudbreakEvent);
        // THEN
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0).getInstanceHours().longValue());
    }

    @Test
    public void testGenerateUsagesWithoutPriceGenerator() throws ParseException {
        // GIVEN
        priceGenerators.clear();
        given(stackRepository.findById(anyLong())).willReturn(stack);
        // WHEN
        List<CloudbreakUsage> result = underTest.generateUsages(DUMMY_START_DATE,
                DUMMY_END_DATE, cloudbreakEvent);
        // THEN
        assertEquals(0, result.size());
    }
}
