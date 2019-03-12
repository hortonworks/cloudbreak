package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Map;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterRequest;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterResponse;

public class AzurePlatformParametersTest {

    private static final String WORKER_GROUP_NAME = "worker";

    private static final String COMPUTE_GROUP_NAME = "compute";

    private static final String STACK_NAME = "test-stack";

    private static final String AS_KEY = "availabilitySet";

    private static final String AS_NAME_KEY = "name";

    private static final String AS_FAULT_DOMAIN_COUNTER_KEY = "faultDomainCount";

    private static final String AS_UPDATE_DOMAIN_COUNTER_KEY = "updateDomainCount";

    private static final int DEFAULT_FAULT_DOMAIN_COUNTER = 5;

    private static final int DEFAULT_UPDATE_DOMAIN_COUNTER = 50;

    @InjectMocks
    private AzurePlatformParameters underTest;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testAvailabilitySetParameterCollection() {
        Map<String, InstanceGroupParameterResponse> instanceGroupParameterResponseMap =
                underTest.collectInstanceGroupParameters(Sets.newHashSet(getRequestWithAs(WORKER_GROUP_NAME), getRequestWithoutAs(COMPUTE_GROUP_NAME)));

        assertTrue(instanceGroupParameterResponseMap.get(WORKER_GROUP_NAME).getParameters().containsKey(AS_KEY));
        assertTrue(instanceGroupParameterResponseMap.get(WORKER_GROUP_NAME).getParameters().get(AS_KEY) instanceof Map);

        Map<Object, Object> workerAsMap = (Map<Object, Object>) instanceGroupParameterResponseMap.get(WORKER_GROUP_NAME).getParameters().get(AS_KEY);
        assertEquals(workerAsMap.get(AS_NAME_KEY), STACK_NAME + "-" + WORKER_GROUP_NAME + "-customPostFix");
        assertEquals(workerAsMap.get(AS_FAULT_DOMAIN_COUNTER_KEY), 5);
        assertEquals(workerAsMap.get(AS_UPDATE_DOMAIN_COUNTER_KEY), 50);

        assertTrue(instanceGroupParameterResponseMap.get(COMPUTE_GROUP_NAME).getParameters().containsKey(AS_KEY));
        assertTrue(instanceGroupParameterResponseMap.get(COMPUTE_GROUP_NAME).getParameters().get(AS_KEY) instanceof Map);

        Map<Object, Object> computeAsMap = (Map<Object, Object>) instanceGroupParameterResponseMap.get(COMPUTE_GROUP_NAME).getParameters().get(AS_KEY);
        assertEquals(computeAsMap.get(AS_NAME_KEY), STACK_NAME + "-" + COMPUTE_GROUP_NAME + "-as");
        assertEquals(computeAsMap.get(AS_FAULT_DOMAIN_COUNTER_KEY), 2);
        assertEquals(computeAsMap.get(AS_UPDATE_DOMAIN_COUNTER_KEY), 20);
    }

    private InstanceGroupParameterRequest getRequestWithoutAs(String groupName) {
        InstanceGroupParameterRequest rq = new InstanceGroupParameterRequest();
        rq.setGroupName(groupName);
        rq.setStackName(STACK_NAME);
        rq.setNodeCount(new Random().nextInt(3));
        rq.setParameters(Maps.newHashMap());
        return rq;
    }

    private InstanceGroupParameterRequest getRequestWithAs(String groupName) {
        InstanceGroupParameterRequest rq = getRequestWithoutAs(groupName);
        Map<Object, Object> asMap = Maps.newHashMap();
        asMap.put(AS_NAME_KEY, String.format("%s-%s-customPostFix", rq.getStackName(), rq.getGroupName()));
        asMap.put(AS_FAULT_DOMAIN_COUNTER_KEY, DEFAULT_FAULT_DOMAIN_COUNTER);
        asMap.put(AS_UPDATE_DOMAIN_COUNTER_KEY, DEFAULT_UPDATE_DOMAIN_COUNTER);
        rq.getParameters().put(AS_KEY, asMap);
        return rq;
    }

}
