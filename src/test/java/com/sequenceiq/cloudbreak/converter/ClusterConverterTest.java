package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyLong;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.ClusterRequest;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

public class ClusterConverterTest {

    private static final String DUMMY_JSON = "dummyJson: {}";
    private static final String DUMMY_DESCRIPTION = "dummyDescription";
    private static final String DUMMY_NAME = "dummyName";
    private static final String DUMMY_STATUS_REASON = "dummyStatusReason";
    private static final String DUMMY_ACCOUNT = "DummyAccount";
    private static final String DUMMY_EMAIL = "john@doe.kom";

    @InjectMocks
    private ClusterConverter underTest;

    @Mock
    private BlueprintRepository blueprintRepository;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private JsonHelper jsonHelper;

    @Mock
    private JsonNode jsonNode;

    private Cluster cluster;

    private ClusterRequest clusterRequest;

    private Blueprint blueprint;

    @Before
    public void setUp() throws IOException {
        underTest = new ClusterConverter();
        MockitoAnnotations.initMocks(this);
        blueprint = new Blueprint();
        blueprint.setId(1L);
        blueprint.setBlueprintText(FileReaderUtils.readFileFromClasspath("multi-node-hdfs-yarn-test.bp"));
        blueprint.setBlueprintName("test-bp");
        blueprint.setHostGroupCount(2);
        cluster = createCluster();
        clusterRequest = createClusterRequest();
    }

    @Test(expected = BadRequestException.class)
    public void testConvertClusterEntityToJsonWhenStackHasNoInstanceGroup() {
        // GIVEN
        given(blueprintRepository.findOne(anyLong())).willReturn(blueprint);
        given(stackRepository.findById(anyLong())).willReturn(new Stack());
        // WHEN
        Cluster result = underTest.convert(clusterRequest, 1L);
    }

    @Test(expected = BadRequestException.class)
    public void testConvertClusterEntityToJsonWhenStackHasOnlyOneInstanceGroup() {
        // GIVEN
        Stack stack = new Stack();
        InstanceGroup instanceGroup1 = new InstanceGroup();
        instanceGroup1.setGroupName("master");
        stack.getInstanceGroups().add(instanceGroup1);
        given(blueprintRepository.findOne(anyLong())).willReturn(blueprint);
        given(stackRepository.findById(anyLong())).willReturn(stack);
        // WHEN
        Cluster result = underTest.convert(clusterRequest, 1L);
    }

    @Test
    public void testConvertClusterEntityToJsonWhenStackInstanceGroupsWasDefinedCorrect() {
        // GIVEN
        Stack stack = new Stack();
        InstanceGroup instanceGroup1 = new InstanceGroup();
        instanceGroup1.setGroupName("master");
        InstanceGroup instanceGroup2 = new InstanceGroup();
        instanceGroup2.setGroupName("slave_1");
        stack.getInstanceGroups().add(instanceGroup1);
        stack.getInstanceGroups().add(instanceGroup2);
        given(blueprintRepository.findOne(anyLong())).willReturn(blueprint);
        given(stackRepository.findById(anyLong())).willReturn(stack);
        // WHEN
        Cluster result = underTest.convert(clusterRequest, 1L);
        // THEN
        assertEquals(result.getDescription(), clusterRequest.getDescription());
        assertEquals(result.getBlueprint().getId(), clusterRequest.getBlueprintId());
    }

    @Test(expected = AccessDeniedException.class)
    public void testConvertClusterEntityToJsonWhenAccessDeniedOnBlueprint() {
        // GIVEN
        given(blueprintRepository.findOne(anyLong())).willThrow(AccessDeniedException.class);
        // WHEN
        underTest.convert(clusterRequest, 1L);
    }

    @Test(expected = BadRequestException.class)
    public void testConvertClusterEntityWhenStackIdNull() {
        // WHEN
        underTest.convert(clusterRequest);
    }

    @Test
    public void testConvertClusterJsonToEntity() {
        // GIVEN
        given(jsonHelper.createJsonFromString(DUMMY_JSON)).willReturn(jsonNode);
        // WHEN
        ClusterResponse response = underTest.convert(cluster, DUMMY_JSON);
        // THEN
        assertEquals(response.getStatus(), cluster.getStatus().toString());
        assertEquals(response.getBlueprintId(), cluster.getBlueprint().getId());
        assertEquals(response.getDescription(), cluster.getDescription());
    }

    @Test
    public void testConvertClusterJsonToEntityWhenCreationFinishedIsNull() {
        // GIVEN
        cluster.setCreationFinished(null);
        given(jsonHelper.createJsonFromString(DUMMY_JSON)).willReturn(jsonNode);
        // WHEN
        ClusterResponse response = underTest.convert(cluster, DUMMY_JSON);
        // THEN
        assertEquals(response.getHoursUp(), 0L);
        assertEquals(response.getMinutesUp(), 0L);
    }

    @Test
    public void testConvertClusterJsonToEntityWheDescriptionIsNull() {
        // GIVEN
        cluster.setDescription(null);
        given(jsonHelper.createJsonFromString(DUMMY_JSON)).willReturn(jsonNode);
        // WHEN
        ClusterResponse response = underTest.convert(cluster, DUMMY_JSON);
        // THEN
        assertEquals(response.getDescription(), "");
    }

    private Cluster createCluster() {
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        cluster.setCreationFinished(100L);
        cluster.setCreationStarted(0L);
        cluster.setDescription(DUMMY_DESCRIPTION);
        cluster.setId(1L);
        cluster.setName(DUMMY_NAME);
        cluster.setStatus(Status.AVAILABLE);
        cluster.setStatusReason(DUMMY_STATUS_REASON);
        cluster.setOwner(DUMMY_EMAIL);
        cluster.setAccount(DUMMY_ACCOUNT);
        return cluster;
    }

    private ClusterRequest createClusterRequest() {
        ClusterRequest clusterRequest = new ClusterRequest();
        clusterRequest.setBlueprintId(1L);
        clusterRequest.setName(DUMMY_NAME);
        clusterRequest.setDescription(DUMMY_DESCRIPTION);
        return clusterRequest;
    }
}
