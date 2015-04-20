package com.sequenceiq.cloudbreak.repository;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.times;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Sets;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.persistence.OptimisticLockException;
import java.util.List;
import java.util.Set;

public class RetryingStackUpdaterTest {

    private static final Long DUMMY_ID = 1L;
    private static final Status DUMMY_STATUS = Status.AVAILABLE;
    private static final String DUMMY_GROUP_NAME = "dummyGroup";
    private static final String DUMMY_STATUS_REASON = "dummyReason";
    private static final String DUMMY_RESOURCE_NAME = "dummyResourceName";
    private static final String DUMMY_AMBARI_IP = "52.51.103.112";
    private static final Integer DUMMY_NODE_COUNT = 2;

    @InjectMocks
    private RetryingStackUpdater underTest;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Before
    public void setUp() {
        underTest = new RetryingStackUpdater();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testUpdateStackSuccessFirst() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        saveMock(dummyStack);
        // WHEN
        Stack result = underTest.updateStack(dummyStack);
        // THEN
        verify(stackRepository, times(1)).save(dummyStack);
        assertEquals(result, dummyStack);
    }

    @Test
    public void testUpdateStackFailThreeTimes() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        failSaveThreeTimes(dummyStack);
        //WHEN
        Stack result = underTest.updateStack(dummyStack);
        // THEN
        verify(stackRepository, times(4)).save(dummyStack);
        assertEquals(result, dummyStack);
    }

    @Test(expected = InternalServerException.class)
    public void testUpdateStackFail() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        failSaveMock(dummyStack);
        //WHEN
        underTest.updateStack(dummyStack);
    }

    @Test
    public void testUpdateStackStatusSuccessFirst() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        defaultMocks(dummyStack);
        // WHEN
        Stack result = underTest.updateStackStatus(DUMMY_ID, DUMMY_STATUS, DUMMY_STATUS_REASON);
        // THEN
        verify(stackRepository, times(1)).save(dummyStack);
        assertEquals(result.getStatusReason(), dummyStack.getStatusReason());
    }

    @Test
    public void testUpdateStackStatusFailThreeTimes() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        failThreeTimesMocks(dummyStack);
        //WHEN
        Stack result = underTest.updateStackStatus(DUMMY_ID, DUMMY_STATUS, DUMMY_STATUS_REASON);
        // THEN
        verify(stackRepository, times(4)).findById(DUMMY_ID);
        assertEquals(result.getStatusReason(), dummyStack.getStatusReason());
    }

    @Test(expected = InternalServerException.class)
    public void testUpdateStackStatusFail() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        failMocks(dummyStack);
        //WHEN
        underTest.updateStackStatus(DUMMY_ID, DUMMY_STATUS, DUMMY_STATUS_REASON);
    }

    @Test
    public void testUpdateStackStatusReason() {
        // GIVEN
        String newStatusReason = "newReason";
        Stack dummyStack = createDummyStack();
        defaultMocks(dummyStack);
        // WHEN
        Stack result = underTest.updateStackStatusReason(DUMMY_ID, newStatusReason);
        // THEN
        assertEquals(result.getStatusReason(), newStatusReason);
    }

    @Test
    public void testUpdateStackStatusReasonFailTreeTimes() {
        // GIVEN
        String newStatusReason = "newReason";
        Stack dummyStack = createDummyStack();
        failThreeTimesMocks(dummyStack);
        // WHEN
        Stack result = underTest.updateStackStatusReason(DUMMY_ID, newStatusReason);
        // THEN
        assertEquals(result.getStatusReason(), newStatusReason);
    }

    @Test(expected = InternalServerException.class)
    public void testUpdateStackStatusReasonFail() {
        // GIVEN
        String newStatusReason = "newReason";
        Stack dummyStack = createDummyStack();
        failMocks(dummyStack);
        // WHEN
        underTest.updateStackStatusReason(DUMMY_ID, newStatusReason);
    }

    @Test
    public void testUpdateStackMetaDataSuccessFirst() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        Set<InstanceMetaData> metaData = createInstanceMetadata();
        defaultMocks(dummyStack);
        // WHEN
        Stack result = underTest.updateStackMetaData(DUMMY_ID, metaData, DUMMY_GROUP_NAME);
        // THEN
        verify(stackRepository, times(1)).save(dummyStack);
        assertEquals(result, dummyStack);
    }

    @Test
    public void testUpdateStackMetaDataFailThreeTimes() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        Set<InstanceMetaData> metaData = createInstanceMetadata();
        failThreeTimesMocks(dummyStack);
        // WHEN
        Stack result = underTest.updateStackMetaData(DUMMY_ID, metaData, DUMMY_GROUP_NAME);
        // THEN
        verify(stackRepository, times(4)).save(dummyStack);
        assertEquals(result, dummyStack);
    }

    @Test(expected = InternalServerException.class)
    public void testUpdateStackMetaDataFail() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        Set<InstanceMetaData> metaData = createInstanceMetadata();
        failMocks(dummyStack);
        // WHEN
        underTest.updateStackMetaData(DUMMY_ID, metaData, DUMMY_GROUP_NAME);
    }

    @Test
    public void testUpdateStackResourcesSuccessFirst() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        Set<Resource> resources = Sets.newHashSet();
        resources.add(new Resource());
        defaultMocks(dummyStack);
        // WHEN
        Stack result = underTest.updateStackResources(DUMMY_ID, resources);
        // THEN
        assertEquals(result.getResources(), resources);
    }

    @Test
    public void testUpdateStackResourcesFailThreeTimes() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        Set<Resource> resources = Sets.newHashSet();
        resources.add(new Resource());
        failThreeTimesMocks(dummyStack);
        // WHEN
        Stack result = underTest.updateStackResources(DUMMY_ID, resources);
        // THEN
        assertEquals(result.getResources(), resources);
        verify(stackRepository, times(4)).save(dummyStack);
    }

    @Test(expected = InternalServerException.class)
    public void testUpdateStackResourcesFail() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        Set<Resource> resources = Sets.newHashSet();
        resources.add(new Resource());
        failMocks(dummyStack);
        // WHEN
        underTest.updateStackResources(DUMMY_ID, resources);
    }

    @Test
    public void testAddStackResourcesSuccessFirst() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        List<Resource> resources = Lists.newArrayList();
        resources.add(new Resource());
        defaultMocks(dummyStack);
        // WHEN
        Stack result = underTest.addStackResources(DUMMY_ID, resources);
        // THEN
        assertEquals(result.getResources().size(), 2);
    }

    @Test
    public void testAddStackResourcesFailThreeTimes() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        List<Resource> resources = Lists.newArrayList();
        resources.add(new Resource());
        failThreeTimesMocks(dummyStack);
        // WHEN
        Stack result = underTest.addStackResources(DUMMY_ID, resources);
        // THEN
        assertEquals(result.getResources().size(), 2);
        verify(stackRepository, times(4)).save(dummyStack);
    }

    @Test(expected = InternalServerException.class)
    public void testAddStackResourcesFail() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        List<Resource> resources = Lists.newArrayList();
        resources.add(new Resource());
        failMocks(dummyStack);
        // WHEN
        underTest.addStackResources(DUMMY_ID, resources);
    }

    @Test
    public void testRemoveStackResourcesSuccessFirst() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        List<Resource> resources = createRemovableResources();
        defaultMocks(dummyStack);
        // WHEN
        Stack result = underTest.removeStackResources(DUMMY_ID, resources);
        // THEN
        assertEquals(result.getResources().size(), 0);
    }

    @Test
    public void testRemoveStackResourcesFailThreeTimes() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        List<Resource> resources = createRemovableResources();
        failThreeTimesMocks(dummyStack);
        // WHEN
        Stack result = underTest.removeStackResources(DUMMY_ID, resources);
        // THEN
        assertEquals(result.getResources().size(), 0);
        verify(stackRepository, times(4)).save(dummyStack);
    }

    @Test(expected = InternalServerException.class)
    public void testRemoveStackResourcesFail() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        List<Resource> resources = createRemovableResources();
        failMocks(dummyStack);
        // WHEN
        underTest.removeStackResources(DUMMY_ID, resources);
    }

    @Test
    public void testUpdateAmbariIpSuccessFirst() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        defaultMocks(dummyStack);
        // WHEN
        Stack result = underTest.updateAmbariIp(DUMMY_ID, DUMMY_AMBARI_IP);
        // THEN
        assertEquals(result.getAmbariIp(), DUMMY_AMBARI_IP);
    }

    @Test
    public void testUpdateAmbariIpFailThreeTimes() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        failThreeTimesMocks(dummyStack);
        // WHEN
        Stack result = underTest.updateAmbariIp(DUMMY_ID, DUMMY_AMBARI_IP);
        // THEN
        verify(stackRepository, times(4)).save(dummyStack);
        assertEquals(result.getAmbariIp(), DUMMY_AMBARI_IP);
    }

    @Test(expected = InternalServerException.class)
    public void testUpdateAmbariIpFail() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        failMocks(dummyStack);
        // WHEN
        underTest.updateAmbariIp(DUMMY_ID, DUMMY_AMBARI_IP);
    }

    @Test
    public void testUpdateMetadataReadySuccessFirst() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        defaultMocks(dummyStack);
        // WHEN
        Stack result = underTest.updateMetadataReady(DUMMY_ID, true);
        // THEN
        assertEquals(result.isMetadataReady(), true);
    }

    @Test
    public void testUpdateMetadataReadyFailThreeTimes() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        failThreeTimesMocks(dummyStack);
        // WHEN
        Stack result = underTest.updateMetadataReady(DUMMY_ID, true);
        // THEN
        verify(stackRepository, times(4)).save(dummyStack);
        assertEquals(result.isMetadataReady(), true);
    }

    @Test(expected = InternalServerException.class)
    public void testUpdateMetadataReadyFail() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        failMocks(dummyStack);
        // WHEN
        underTest.updateMetadataReady(DUMMY_ID, true);
    }

    @Test
    public void testUpdateNodeCountSuccessFirst() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        defaultMocks(dummyStack);
        // WHEN
        Stack result = underTest.updateNodeCount(DUMMY_ID, DUMMY_NODE_COUNT, DUMMY_GROUP_NAME);
        // THEN
        assertEquals(result.getInstanceGroupByInstanceGroupName(DUMMY_GROUP_NAME).getNodeCount(),
                DUMMY_NODE_COUNT);
    }

    @Test
    public void testUpdateNodeCountFailThreeTimes() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        failThreeTimesMocks(dummyStack);
        // WHEN
        Stack result = underTest.updateNodeCount(DUMMY_ID, DUMMY_NODE_COUNT, DUMMY_GROUP_NAME);
        // THEN
        verify(stackRepository, times(4)).save(dummyStack);
        assertEquals(result.getInstanceGroupByInstanceGroupName(DUMMY_GROUP_NAME).getNodeCount(),
                DUMMY_NODE_COUNT);
    }

    @Test(expected = InternalServerException.class)
    public void testUpdateNodeCountFail() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        failMocks(dummyStack);
        // WHEN
        underTest.updateNodeCount(DUMMY_ID, DUMMY_NODE_COUNT, DUMMY_GROUP_NAME);
    }

    @Test
    public void testUpdateStackClusterSuccessFirst() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        Cluster cluster = new Cluster();
        defaultMocks(dummyStack);
        // WHEN
        Stack result = underTest.updateStackCluster(DUMMY_ID, cluster);
        // THEN
        assertEquals(result.getCluster(), cluster);
    }

    @Test
    public void testUpdateStackClusterFailThreeTimes() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        Cluster cluster = new Cluster();
        failThreeTimesMocks(dummyStack);
        // WHEN
        Stack result = underTest.updateStackCluster(DUMMY_ID, cluster);
        // THEN
        verify(stackRepository, times(4)).save(dummyStack);
        assertEquals(result.getCluster(), cluster);
    }

    @Test(expected = InternalServerException.class)
    public void testUpdateStackClusterFail() {
        // GIVEN
        Stack dummyStack = createDummyStack();
        Cluster cluster = new Cluster();
        failMocks(dummyStack);
        // WHEN
        underTest.updateStackCluster(DUMMY_ID, cluster);
    }

    private Set<InstanceMetaData> createInstanceMetadata() {
        Set<InstanceMetaData> metaData = Sets.newHashSet();
        metaData.add(new InstanceMetaData());
        metaData.add(new InstanceMetaData());
        return metaData;
    }

    private void defaultMocks(Stack dummyStack) {
        findByMockAnswer(dummyStack);
        saveMock(dummyStack);
    }

    private void saveMock(Stack dummyStack) {
        given(stackRepository.save(dummyStack)).willReturn(dummyStack);
    }

    private void failSaveMock(Stack dummyStack) {
        given(stackRepository.save(dummyStack)).willThrow(new OptimisticLockException());
    }

    private void failSaveThreeTimes(Stack dummyStack) {
        given(stackRepository.save(dummyStack))
                .willThrow(new OptimisticLockException())
                .willThrow(new OptimisticLockException())
                .willThrow(new OptimisticLockException())
                .willReturn(dummyStack);
    }

    private void failMocks(Stack dummyStack) {
        findByMockAnswer(dummyStack);
        failSaveMock(dummyStack);
    }

    private void failThreeTimesMocks(Stack dummyStack) {
        findByMockAnswer(dummyStack);
        failSaveThreeTimes(dummyStack);
    }

    private void findByMockAnswer(Stack dummyStack) {
        given(stackRepository.findById(DUMMY_ID)).willReturn(dummyStack);
        given(stackRepository.findOneWithLists(DUMMY_ID)).willReturn(dummyStack);
    }

    private List<Resource> createRemovableResources() {
        List<Resource> resources = Lists.newArrayList();
        Resource resource = new Resource();
        resource.setResourceName(DUMMY_RESOURCE_NAME);
        resource.setResourceType(ResourceType.AZURE_CLOUD_SERVICE);
        resources.add(resource);
        return resources;
    }

    private Stack createDummyStack() {
        Stack stack = new Stack();
        stack.setId(DUMMY_ID);
        stack.setStatus(DUMMY_STATUS);
        stack.setStatusReason(DUMMY_STATUS_REASON);
        Set<InstanceGroup> instanceGroups = Sets.newHashSet();
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(DUMMY_GROUP_NAME);
        instanceGroups.add(instanceGroup);
        stack.setInstanceGroups(instanceGroups);
        Set<Resource> stackResources = Sets.newHashSet();
        Resource resource = new Resource();
        resource.setResourceName(DUMMY_RESOURCE_NAME);
        resource.setResourceType(ResourceType.AZURE_CLOUD_SERVICE);
        stackResources.add(resource);
        stack.setResources(stackResources);
        return stack;
    }


}
