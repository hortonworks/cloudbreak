package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyLong;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.controller.json.ClusterRequest;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;

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
    private JsonHelper jsonHelper;

    @Mock
    private JsonNode jsonNode;

    private Cluster cluster;

    private ClusterRequest clusterRequest;

    private Blueprint blueprint;

    @Before
    public void setUp() {
        underTest = new ClusterConverter();
        MockitoAnnotations.initMocks(this);
        blueprint = new Blueprint();
        blueprint.setId(1L);
        cluster = createCluster();
        clusterRequest = createClusterRequest();
    }

    @Test
    public void testConvertClusterEntityToJson() {
        // GIVEN
        given(blueprintRepository.findOne(anyLong())).willReturn(blueprint);
        // WHEN
        Cluster result = underTest.convert(clusterRequest);
        // THEN
        assertEquals(result.getDescription(), clusterRequest.getDescription());
        assertEquals(result.getBlueprint().getId(), clusterRequest.getBlueprintId());
    }

    @Test(expected = AccessDeniedException.class)
    public void testConvertClusterEntityToJsonWhenAccessDeniedOnBlueprint() {
        // GIVEN
        given(blueprintRepository.findOne(anyLong())).willThrow(AccessDeniedException.class);
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
