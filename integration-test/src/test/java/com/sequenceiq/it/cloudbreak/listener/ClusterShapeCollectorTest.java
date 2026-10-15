package com.sequenceiq.it.cloudbreak.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

@ExtendWith(MockitoExtension.class)
class ClusterShapeCollectorTest {

    private static final String TEST_METHOD = "testSomething";

    @InjectMocks
    private ClusterShapeCollector underTest;

    @Mock
    private TestContext testContext;

    @Mock
    private DistroXTestDto distroXTestDto;

    @Mock
    private SdxInternalTestDto sdxInternalTestDto;

    @Mock
    private FreeIpaTestDto freeIpaTestDto;

    @Mock
    private EnvironmentTestDto environmentTestDto;

    @Mock
    private RedbeamsDatabaseServerTestDto databaseServerTestDto;

    @Test
    void testCollectReadsDataHubShapeFromTheStackResponse() throws Exception {
        when(distroXTestDto.getResponse()).thenReturn(stack("my-dh", "eu-central-1", "worker", 3, "m5.2xlarge"));
        givenResources(Map.of("dh", distroXTestDto));
        givenTestMethodAndCloudPlatform();

        JSONObject group = firstGroupOf(underTest.collect(testContext), 0);

        assertThat(group.getString("instanceType")).isEqualTo("m5.2xlarge");
        assertThat(group.getInt("nodeCount")).isEqualTo(3);
        assertThat(group.getString("name")).isEqualTo("worker");
    }

    @Test
    void testCollectLabelsAWrappedStackAsDataLakeRatherThanDataHub() throws Exception {
        SdxClusterDetailResponse sdx = new SdxClusterDetailResponse();
        sdx.setName("my-dl");
        sdx.setStackV4Response(stack("my-dl", "eu-west-1", "master", 2, "r5.4xlarge"));
        when(sdxInternalTestDto.getResponse()).thenReturn(sdx);
        givenResources(Map.of("dl", sdxInternalTestDto));
        givenTestMethodAndCloudPlatform();

        JSONObject result = underTest.collect(testContext);

        // A data lake reports through a wrapped StackV4Response. If the StackV4Response branch won,
        // its spend would be attributed to DATAHUB and the role split would be silently wrong.
        assertThat(result.getJSONArray("clusters").getJSONObject(0).getString("role")).isEqualTo("DATALAKE");
        assertThat(firstGroupOf(result, 0).getString("instanceType")).isEqualTo("r5.4xlarge");
    }

    @Test
    void testCollectReadsFreeIpaShapeFromItsOwnApiHierarchy() throws Exception {
        when(freeIpaTestDto.getResponse()).thenReturn(freeIpa("my-ipa", "m5.large", 3));
        givenResources(Map.of("ipa", freeIpaTestDto));
        givenTestMethodAndCloudPlatform();

        JSONObject result = underTest.collect(testContext);

        assertThat(result.getJSONArray("clusters").getJSONObject(0).getString("role")).isEqualTo("FREEIPA");
        assertThat(firstGroupOf(result, 0).getString("instanceType")).isEqualTo("m5.large");
        assertThat(firstGroupOf(result, 0).getInt("nodeCount")).isEqualTo(3);
    }

    @Test
    void testCollectReturnsNullWhenNothingWasProvisioned() throws Exception {
        when(environmentTestDto.getResponse()).thenReturn(null);
        givenResources(Map.of("env", environmentTestDto));

        assertThat(underTest.collect(testContext)).isNull();
    }

    @Test
    void testCollectSkipsAStackWithoutInstanceGroupsInsteadOfFailing() throws Exception {
        StackV4Response empty = new StackV4Response();
        empty.setName("half-created");
        when(distroXTestDto.getResponse()).thenReturn(empty);
        givenResources(Map.of("dh", distroXTestDto));

        assertThat(underTest.collect(testContext)).isNull();
    }

    @Test
    void testCollectKeepsEveryClusterTheTestCreated() throws Exception {
        when(distroXTestDto.getResponse()).thenReturn(stack("my-dh", "eu-central-1", "worker", 3, "m5.2xlarge"));
        when(freeIpaTestDto.getResponse()).thenReturn(freeIpa("my-ipa", "m5.large", 3));
        givenResources(Map.of("dh", distroXTestDto, "ipa", freeIpaTestDto));
        givenTestMethodAndCloudPlatform();

        JSONObject result = underTest.collect(testContext);

        assertThat(result.getJSONArray("clusters").length()).isEqualTo(2);
        assertThat(result.getString("testMethod")).isEqualTo(TEST_METHOD);
    }

    @Test
    void testCollectReadsTheDatabaseServerWhichHasNoInstanceGroups() throws Exception {
        DatabaseServerV4Response database = new DatabaseServerV4Response();
        database.setName("my-db");
        database.setInstanceType("db.m5.xlarge");
        database.setStorageSize(100L);
        when(databaseServerTestDto.getResponse()).thenReturn(database);
        givenResources(Map.of("db", databaseServerTestDto));
        givenTestMethodAndCloudPlatform();

        JSONObject result = underTest.collect(testContext);

        // The whole database component was previously 100% inferred, so this is the branch that turns
        // it into a measurement.
        assertThat(result.getJSONArray("clusters").getJSONObject(0).getString("role")).isEqualTo("DATABASE");
        assertThat(firstGroupOf(result, 0).getString("instanceType")).isEqualTo("db.m5.xlarge");
        assertThat(firstGroupOf(result, 0).getInt("nodeCount")).isEqualTo(1);
        assertThat(firstGroupOf(result, 0).getLong("storageSizeGb")).isEqualTo(100L);
    }

    private void givenResources(Map<String, CloudbreakTestDto> resources) {
        when(testContext.getResourceNames()).thenReturn(resources);
    }

    private void givenTestMethodAndCloudPlatform() {
        when(testContext.getTestMethodName()).thenReturn(Optional.of(TEST_METHOD));
        when(testContext.getCloudPlatform()).thenReturn(CloudPlatform.AWS);
    }

    private JSONObject firstGroupOf(JSONObject result, int clusterIndex) throws Exception {
        return result.getJSONArray("clusters").getJSONObject(clusterIndex).getJSONArray("groups").getJSONObject(0);
    }

    private StackV4Response stack(String name, String region, String groupName, int nodeCount, String instanceType) {
        InstanceTemplateV4Response template = new InstanceTemplateV4Response();
        template.setInstanceType(instanceType);
        InstanceGroupV4Response group = new InstanceGroupV4Response();
        group.setName(groupName);
        group.setNodeCount(nodeCount);
        group.setType(InstanceGroupType.CORE);
        group.setTemplate(template);
        StackV4Response stack = new StackV4Response();
        stack.setName(name);
        stack.setRegion(region);
        stack.setInstanceGroups(List.of(group));
        return stack;
    }

    private DescribeFreeIpaResponse freeIpa(String name, String instanceType, int nodeCount) {
        InstanceTemplateResponse template = new InstanceTemplateResponse();
        template.setInstanceType(instanceType);
        InstanceGroupResponse group = new InstanceGroupResponse();
        group.setName("master");
        group.setNodeCount(nodeCount);
        group.setInstanceTemplate(template);
        DescribeFreeIpaResponse freeIpa = new DescribeFreeIpaResponse();
        freeIpa.setName(name);
        freeIpa.setInstanceGroups(List.of(group));
        return freeIpa;
    }
}
