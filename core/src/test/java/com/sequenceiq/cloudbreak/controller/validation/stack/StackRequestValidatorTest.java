package com.sequenceiq.cloudbreak.controller.validation.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.State;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateRequestValidator;

@RunWith(MockitoJUnitRunner.class)
public class StackRequestValidatorTest {

    @Spy
    private TemplateRequestValidator templateRequestValidator = new TemplateRequestValidator();

    @InjectMocks
    private StackRequestValidator underTest;

    @Test
    public void testWithZeroRootVolumeSize() {
        assertNotNull(templateRequestValidator);
        StackRequest stackRequest = stackRequestWithRootVolumeSize(0);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.ERROR, validationResult.getState());
    }

    @Test
    public void testWithNegativeRootVolumeSize() {
        StackRequest stackRequest = stackRequestWithRootVolumeSize(-1);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.ERROR, validationResult.getState());
    }

    @Test
    public void testNullValueIsAllowedForRootVolumeSize() {
        StackRequest stackRequest = stackRequestWithRootVolumeSize(null);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.VALID, validationResult.getState());
    }

    @Test
    public void testWithPositiveRootVolumeSize() {
        StackRequest stackRequest = stackRequestWithRootVolumeSize(1);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.VALID, validationResult.getState());
    }

    @Test
    public void testWithLargerInstanceGroupSetThanHostGroups() {
        String plusOne = "very master";
        StackRequest stackRequest = stackRequestWithInstanceAndHostGroups(
                Sets.newHashSet(plusOne, "master", "worker", "compute"),
                Sets.newHashSet("master", "worker", "compute")
        );

        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.ERROR, validationResult.getState());
        assertTrue(validationResult.getErrors().get(0).startsWith("There are instance groups in the request that do not have a corresponding host group: "
                + plusOne));
    }

    @Test
    public void testWithLargerHostGroupSetThanInstanceGroups() {
        StackRequest stackRequest = stackRequestWithInstanceAndHostGroups(
                Sets.newHashSet("master", "worker", "compute"),
                Sets.newHashSet("super master", "master", "worker", "compute")
        );

        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.ERROR, validationResult.getState());
        assertTrue(validationResult.getErrors().get(0).startsWith("There are host groups in the request that do not have a corresponding instance group"));
    }

    @Test
    public void testWithBothGroupContainsDifferentValues() {
        StackRequest stackRequest = stackRequestWithInstanceAndHostGroups(
                Sets.newHashSet("worker", "compute"),
                Sets.newHashSet("master", "worker")
        );

        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.ERROR, validationResult.getState());
        assertEquals(2L, validationResult.getErrors().size());
    }

    private StackRequest stackRequestWithInstanceAndHostGroups(Set<String> instanceGroups, Set<String> hostGroups) {
        List<InstanceGroupRequest> instanceGroupList = instanceGroups.stream()
                .map(ig -> getInstanceGroupRequest(new TemplateRequest(), ig))
                .collect(Collectors.toList());

        Set<HostGroupRequest> hostGroupSet = hostGroups.stream()
                .map(hg -> {
                    HostGroupRequest hostGroupRequest = new HostGroupRequest();
                    hostGroupRequest.setName(hg);
                    return hostGroupRequest;
                })
                .collect(Collectors.toSet());

        ClusterRequest clusterRequest = new ClusterRequest();
        clusterRequest.setHostGroups(hostGroupSet);

        return getStackRequest(instanceGroupList, clusterRequest);
    }

    private StackRequest stackRequestWithRootVolumeSize(Integer rootVolumeSize) {
        TemplateRequest templateRequest = new TemplateRequest();
        templateRequest.setRootVolumeSize(rootVolumeSize);
        InstanceGroupRequest instanceGroupRequest = getInstanceGroupRequest(templateRequest, "master");
        ClusterRequest clusterRequest = getClusterRequest();
        return getStackRequest(Collections.singletonList(instanceGroupRequest), clusterRequest);
    }

    private InstanceGroupRequest getInstanceGroupRequest(TemplateRequest templateRequest, String master) {
        InstanceGroupRequest instanceGroupRequest = new InstanceGroupRequest();
        instanceGroupRequest.setGroup(master);
        instanceGroupRequest.setTemplate(templateRequest);
        return instanceGroupRequest;
    }

    private ClusterRequest getClusterRequest() {
        HostGroupRequest hostGroupRequest = new HostGroupRequest();
        ClusterRequest clusterRequest = new ClusterRequest();
        hostGroupRequest.setName("master");
        clusterRequest.setHostGroups(Sets.newHashSet(hostGroupRequest));
        return clusterRequest;
    }

    private StackRequest getStackRequest(List<InstanceGroupRequest> instanceGroupRequests, ClusterRequest clusterRequest) {
        StackRequest stackRequest = new StackRequest();
        stackRequest.setClusterRequest(clusterRequest);
        stackRequest.setInstanceGroups(instanceGroupRequests);
        return stackRequest;
    }

}