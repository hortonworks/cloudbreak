package com.sequenceiq.it.cloudbreak.listener;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;

/**
 * Writes the cluster shapes a test actually provisioned - instance type and node count per instance
 * group, per cluster - so that cloud spend can be attributed without parsing the suite logs.
 *
 * The shapes are only recoverable from the logs for as long as Jenkins keeps them (~12 days on the
 * child jobs), which makes historical spend permanently unmeasurable. Emitting them as a small
 * artifact removes that dependency: ~1 KiB per test instead of ~1.5 MiB of log per test.
 *
 * The three roles come from three unrelated API hierarchies, so each needs its own branch:
 *   DATAHUB   StackV4Response            (core-api)      instanceGroups[].template.instanceType
 *   DATALAKE  SdxClusterDetailResponse   (datalake-api)  wraps a StackV4Response
 *   FREEIPA   DescribeFreeIpaResponse    (freeipa-api)   instanceGroups[].instanceTemplate.instanceType
 *   DATABASE  DatabaseServerV4Response   (redbeams-api)  a single server: instanceType + storageSize,
 *                                                        no instance groups at all
 *
 * Nothing here may ever fail a test: a shape we could not read is worth less than the test result,
 * so every failure is logged and swallowed.
 */
public class ClusterShapeCollector {

    /**
     * Deliberately starts with "resource_names" so that it is picked up by the Jenkins artifact
     * archiving pattern, which is configured in the job rather than in this repository and could not
     * be read here (config.xml is 403). A file that is written but never archived would make the whole
     * exercise silently produce nothing, which is a far worse failure than the one side effect of the
     * prefix: CleanupUtil also globs "resource_names*.json". That is harmless - it looks up specific
     * resource-name keys, which this file does not contain, so cleanup finds nothing in it.
     */
    static final String FILE_PREFIX = "resource_names_shapes_";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterShapeCollector.class);

    private static final String ROLE_DATAHUB = "DATAHUB";

    private static final String ROLE_DATALAKE = "DATALAKE";

    private static final String ROLE_FREEIPA = "FREEIPA";

    private static final String ROLE_DATABASE = "DATABASE";

    public void collectAndWrite(TestContext testContext) {
        try {
            JSONObject shapes = collect(testContext);
            if (shapes == null) {
                LOGGER.info("No provisioned cluster shapes found, no shape file needs to be created.");
                return;
            }
            Path path = Paths.get(FILE_PREFIX + shapes.getString("testMethod") + ".json");
            Files.writeString(path, shapes.toString());
            LOGGER.info("Cluster shape file has been created at: {} with content: {}", path.toAbsolutePath(), shapes);
        } catch (Exception e) {
            LOGGER.warn("Collecting cluster shapes failed, cloud spend for this test will have to fall back to "
                    + "configured defaults. Cause: {}", e.getMessage(), e);
        }
    }

    JSONObject collect(TestContext testContext) throws JSONException {
        JSONArray clusters = new JSONArray();
        for (CloudbreakTestDto testDto : testContext.getResourceNames().values()) {
            addCluster(clusters, testDto);
        }
        if (clusters.length() == 0) {
            return null;
        }
        JSONObject root = new JSONObject();
        root.put("testMethod", testContext.getTestMethodName().orElse("unknown"));
        root.put("cloudPlatform", String.valueOf(testContext.getCloudPlatform()));
        root.put("clusters", clusters);
        return root;
    }

    private void addCluster(JSONArray clusters, CloudbreakTestDto testDto) {
        if (!(testDto instanceof AbstractTestDto<?, ?, ?, ?> abstractTestDto)) {
            return;
        }
        try {
            Object response = abstractTestDto.getResponse();
            if (response instanceof SdxClusterDetailResponse sdxResponse) {
                // Must be checked before StackV4Response: a data lake reports its shape through a
                // wrapped stack, and treating it as a data hub would misattribute the spend.
                addStack(clusters, ROLE_DATALAKE, sdxResponse.getName(), sdxResponse.getStackV4Response());
            } else if (response instanceof StackV4Response stackResponse) {
                addStack(clusters, ROLE_DATAHUB, stackResponse.getName(), stackResponse);
            } else if (response instanceof DescribeFreeIpaResponse freeIpaResponse) {
                addFreeIpa(clusters, freeIpaResponse);
            } else if (response instanceof DatabaseServerV4Response databaseResponse) {
                addDatabaseServer(clusters, databaseResponse);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not read the cluster shape of {}, skipping it. Cause: {}",
                    testDto.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    private void addStack(JSONArray clusters, String role, String name, StackV4Response stack) throws JSONException {
        if (stack == null || stack.getInstanceGroups() == null || stack.getInstanceGroups().isEmpty()) {
            return;
        }
        JSONArray groups = new JSONArray();
        for (InstanceGroupV4Response instanceGroup : stack.getInstanceGroups()) {
            JSONObject group = new JSONObject();
            group.put("name", instanceGroup.getName());
            group.put("type", String.valueOf(instanceGroup.getType()));
            group.put("nodeCount", instanceGroup.getNodeCount());
            group.put("instanceType", instanceGroup.getTemplate() == null
                    ? JSONObject.NULL : instanceGroup.getTemplate().getInstanceType());
            groups.put(group);
        }
        clusters.put(cluster(role, name, stack.getRegion(), groups));
    }

    private void addFreeIpa(JSONArray clusters, DescribeFreeIpaResponse freeIpa) throws JSONException {
        if (freeIpa.getInstanceGroups() == null || freeIpa.getInstanceGroups().isEmpty()) {
            return;
        }
        JSONArray groups = new JSONArray();
        for (InstanceGroupResponse instanceGroup : freeIpa.getInstanceGroups()) {
            JSONObject group = new JSONObject();
            group.put("name", instanceGroup.getName());
            group.put("type", String.valueOf(instanceGroup.getType()));
            group.put("nodeCount", instanceGroup.getNodeCount());
            group.put("instanceType", instanceGroup.getInstanceTemplate() == null
                    ? JSONObject.NULL : instanceGroup.getInstanceTemplate().getInstanceType());
            groups.put(group);
        }
        // DescribeFreeIpaResponse carries no region of its own; it inherits the environment's.
        clusters.put(cluster(ROLE_FREEIPA, freeIpa.getName(), null, groups));
    }

    private void addDatabaseServer(JSONArray clusters, DatabaseServerV4Response databaseServer) throws JSONException {
        if (databaseServer.getInstanceType() == null) {
            return;
        }
        // A database server has no instance groups - it is one instance - but it is still emitted as a
        // single group so that every consumer can walk clusters[].groups[] uniformly. storageSizeGb is
        // carried here because managed database storage is billed separately from the instance.
        JSONObject group = new JSONObject();
        group.put("name", "server");
        group.put("type", ROLE_DATABASE);
        group.put("nodeCount", 1);
        group.put("instanceType", databaseServer.getInstanceType());
        group.put("storageSizeGb", databaseServer.getStorageSize() == null
                ? JSONObject.NULL : databaseServer.getStorageSize());
        clusters.put(cluster(ROLE_DATABASE, databaseServer.getName(), null, new JSONArray().put(group)));
    }

    private JSONObject cluster(String role, String name, String region, JSONArray groups) throws JSONException {
        JSONObject cluster = new JSONObject();
        cluster.put("role", role);
        cluster.put("name", name == null ? JSONObject.NULL : name);
        cluster.put("region", region == null ? JSONObject.NULL : region);
        cluster.put("groups", groups);
        return cluster;
    }
}
