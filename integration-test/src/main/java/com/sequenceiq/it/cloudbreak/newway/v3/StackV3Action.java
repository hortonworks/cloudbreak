package com.sequenceiq.it.cloudbreak.newway.v3;


import static org.springframework.util.StringUtils.isEmpty;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.DatalakeCluster;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class StackV3Action {
    private static final String VPC_ID_KEY = "vpcId";

    private static final String SUBNET_ID_KEY = "subnetId";

    private static final String NETWORK_ID_KEY = "networkId";

    private static final String RESOURCE_GROUP_NAME_KEY = "resourceGroupName";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackV3Action.class);

    private StackV3Action() {
    }

    public static void get(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        StackEntity stackEntity = (StackEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" get stack " + stackEntity.getName());
        stackEntity.setResponse(
                client.getCloudbreakClient().stackV3Endpoint()
                        .getByNameInWorkspace(workspaceId, stackEntity.getName(), null));
        Log.logJSON(" stack get response: ", stackEntity.getResponse());
    }

    public static void getAll(IntegrationTestContext integrationTestContext, Entity entity) {
        StackEntity stackEntity = (StackEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" get all stack");
        stackEntity.setResponses(toStackResponseSet(client, workspaceId, client.getCloudbreakClient().stackV3Endpoint().listByWorkspace(workspaceId)));
    }

    private static Set<StackResponse> toStackResponseSet(CloudbreakClient client, Long workspaceId, Set<StackViewResponse> stacks) {
        Set<StackResponse> detailedStacks = new HashSet<>();
        stacks.stream().forEach(
                stack -> detailedStacks.add(client.getCloudbreakClient().stackV3Endpoint().getByNameInWorkspace(workspaceId, stack.getName(), null)));
        return detailedStacks;
    }

    public static void delete(IntegrationTestContext integrationTestContext, Entity entity) {
        delete(integrationTestContext, (StackEntity) entity, Boolean.FALSE);
    }

    public static void deleteWithForce(IntegrationTestContext integrationTestContext, Entity entity) {
        delete(integrationTestContext, (StackEntity) entity, Boolean.TRUE);
    }

    private static void delete(IntegrationTestContext integrationTestContext, StackEntity entity, Boolean forced) {
        StackEntity stackEntity = entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" delete: " + stackEntity.getName());
        client.getCloudbreakClient().stackV3Endpoint()
                .deleteInWorkspace(workspaceId, stackEntity.getName(), forced, false);
    }

    public static void delete(IntegrationTestContext integrationTestContext, StackEntity entity, CloudbreakClient cloudbreakClient, Boolean forced) {
        StackEntity stackEntity = entity;
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" delete: " + stackEntity.getName());
        cloudbreakClient.getCloudbreakClient().stackV3Endpoint()
                .deleteInWorkspace(workspaceId, stackEntity.getName(), forced, false);
    }

    public static StackEntity deleteV2(TestContext testContext, StackEntity entity, CloudbreakClient cloudbreakClient) {
        Log.log(LOGGER, " delete: " + entity.getName());
        cloudbreakClient.getCloudbreakClient().stackV3Endpoint()
                .deleteInWorkspace(cloudbreakClient.getWorkspaceId(), entity.getName(), false, false);
        return entity;
    }

    public static void deleteWithKerberos(IntegrationTestContext integrationTestContext, Entity entity) {
        StackEntity stackEntity = (StackEntity) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        Log.log(" delete: " + stackEntity.getName());
        client.getCloudbreakClient().stackV3Endpoint()
                .deleteWithKerberosInWorkspace(workspaceId, stackEntity.getName(), true, false);
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        try {
            get(integrationTestContext, entity);
            // TODO: Exception class is too wide. A narrower exception should be caught (e.g. NotFound or something like that.)
        } catch (Exception e) {
            LOGGER.info("Failed to get stack. Trying to create it.", e);
            new StackPostV3Strategy().doAction(integrationTestContext, entity);
        }
    }

    public static void determineNetworkAwsFromDatalakeStack(IntegrationTestContext integrationTestContext, Entity entity) {
        var stackEntity = (StackEntity) entity;
        var datalakeStack = DatalakeCluster.getTestContextDatalakeCluster().apply(integrationTestContext);
        if (isDatalakeExistAndHasNetwork(datalakeStack)) {
            String subnetId = obtainFromNetworkParam(datalakeStack.getResponse().getNetwork().getParameters(), SUBNET_ID_KEY);
            String networkId = obtainFromNetworkParam(datalakeStack.getResponse().getNetwork().getParameters(), NETWORK_ID_KEY);

            prepareNetworkParam(stackEntity);
            stackEntity.getRequest().getNetwork().getParameters().put(SUBNET_ID_KEY, subnetId);
            stackEntity.getRequest().getNetwork().getParameters().put(VPC_ID_KEY, networkId);
        } else {
            throw new AssertionError("Datalake cluster does not cointain network or datalake cluster does not exist");
        }
    }

    public static void determineNetworkAzureFromDatalakeStack(IntegrationTestContext integrationTestContext, Entity entity) {
        var stackEntity = (StackEntity) entity;
        var datalakeStack = DatalakeCluster.getTestContextDatalakeCluster().apply(integrationTestContext);
        if (isDatalakeExistAndHasNetwork(datalakeStack)) {
            String subnetId = obtainFromNetworkParam(datalakeStack.getResponse().getNetwork().getParameters(), SUBNET_ID_KEY);
            String networkId = obtainFromNetworkParam(datalakeStack.getResponse().getNetwork().getParameters(), NETWORK_ID_KEY);

            prepareNetworkParam(stackEntity);
            stackEntity.getRequest().getNetwork().getParameters().put(SUBNET_ID_KEY, subnetId);
            stackEntity.getRequest().getNetwork().getParameters().put(NETWORK_ID_KEY, networkId);
            stackEntity.getRequest().getNetwork().getParameters().put(RESOURCE_GROUP_NAME_KEY, networkId);
        } else {
            throw new AssertionError("Datalake cluster does not cointain network or datalake cluster does not exist");
        }
    }

    public static void determineNetworkFromDatalakeStack(IntegrationTestContext integrationTestContext, Entity entity) {
        var stackEntity = (StackEntity) entity;
        var datalakeStack = DatalakeCluster.getTestContextDatalakeCluster().apply(integrationTestContext);
        if (isDatalakeExistAndHasNetwork(datalakeStack)) {
            String subnetId = obtainFromNetworkParam(datalakeStack.getResponse().getNetwork().getParameters(), SUBNET_ID_KEY);
            String networkId = obtainFromNetworkParam(datalakeStack.getResponse().getNetwork().getParameters(), NETWORK_ID_KEY);

            prepareNetworkParam(stackEntity);
            stackEntity.getRequest().getNetwork().getParameters().put(SUBNET_ID_KEY, subnetId);
            stackEntity.getRequest().getNetwork().getParameters().put(NETWORK_ID_KEY, networkId);
        } else {
            throw new AssertionError("Datalake cluster does not cointain network or datalake cluster does not exist");
        }
    }

    private static String obtainFromNetworkParam(Map<String, Object> parameters, String key) {
        if (parameters == null) {
            return null;
        }
        Object value = parameters.get(key);
        if (isEmpty(parameters.get(key))) {
            return null;
        }

        return value.toString();
    }

    private static void prepareNetworkParam(StackEntity stackEntity) {
        if (stackEntity.getRequest().getNetwork() == null) {
            var network = new NetworkV2Request();
            stackEntity.getRequest().setNetwork(network);
        }
        if (stackEntity.getRequest().getNetwork().getParameters() == null) {
            var params = new LinkedHashMap<String, Object>();
            stackEntity.getRequest().getNetwork().setParameters(params);
        }
    }

    private static boolean isDatalakeExistAndHasNetwork(DatalakeCluster datalakeStack) {
        return datalakeStack != null && datalakeStack.getResponse() != null && datalakeStack.getResponse().getNetwork() != null;
    }
}
