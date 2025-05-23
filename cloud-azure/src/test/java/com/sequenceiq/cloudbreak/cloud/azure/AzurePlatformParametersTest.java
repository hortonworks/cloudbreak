package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.DistroxEnabledInstanceTypes.AZURE_ENABLED_TYPES_LIST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterRequest;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterResponse;
import com.sequenceiq.common.model.Architecture;

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

    @BeforeEach
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testAvailabilitySetParameterCollection() {
        Map<String, InstanceGroupParameterResponse> instanceGroupParameterResponseMap =
                underTest.collectInstanceGroupParameters(Sets.newHashSet(getRequestWithAs(WORKER_GROUP_NAME), getRequestWithoutAs(COMPUTE_GROUP_NAME)));

        assertTrue(instanceGroupParameterResponseMap.get(WORKER_GROUP_NAME).getParameters().containsKey(AS_KEY));
        assertThat(instanceGroupParameterResponseMap.get(WORKER_GROUP_NAME).getParameters().get(AS_KEY), IsInstanceOf.instanceOf(Map.class));

        Map<Object, Object> workerAsMap = (Map<Object, Object>) instanceGroupParameterResponseMap.get(WORKER_GROUP_NAME).getParameters().get(AS_KEY);
        assertEquals(workerAsMap.get(AS_NAME_KEY), STACK_NAME + "-" + WORKER_GROUP_NAME + "-customPostFix");
        assertEquals(workerAsMap.get(AS_FAULT_DOMAIN_COUNTER_KEY), 5);
        assertEquals(workerAsMap.get(AS_UPDATE_DOMAIN_COUNTER_KEY), 50);

        assertFalse(instanceGroupParameterResponseMap.get(COMPUTE_GROUP_NAME).getParameters().containsKey(AS_KEY));
    }

    @Test
    void testDistroxEnabledInstanceTypes() {
        List<String> expected = AZURE_ENABLED_TYPES_LIST;
        Set<String> result = underTest.getDistroxEnabledInstanceTypes(Architecture.X86_64);
        Assertions.assertThat(result).hasSameElementsAs(new HashSet<>(expected));
    }

    @Test
    public void testDiskTypeChangeSupported() {
        assertTrue(underTest.specialParameters().getSpecialParameters().get(PlatformParametersConsts.DISK_TYPE_CHANGE_SUPPORTED));
    }

    @Test
    public void testDeleteDiskSupported() {
        assertTrue(underTest.specialParameters().getSpecialParameters().get(PlatformParametersConsts.DELETE_VOLUMES_SUPPORTED));
    }

    @Test
    public void testAddDiskSupported() {
        assertTrue(underTest.specialParameters().getSpecialParameters().get(PlatformParametersConsts.ADD_VOLUMES_SUPPORTED));
    }

    private InstanceGroupParameterRequest getRequestWithoutAs(String groupName) {
        InstanceGroupParameterRequest rq = new InstanceGroupParameterRequest();
        rq.setGroupName(groupName);
        rq.setStackName(STACK_NAME);
        rq.setNodeCount(new SecureRandom().nextInt(3));
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
