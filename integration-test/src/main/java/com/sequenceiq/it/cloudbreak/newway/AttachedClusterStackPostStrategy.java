package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.CloudbreakClient.getTestContextCloudbreakClient;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;

public class AttachedClusterStackPostStrategy extends StackPostStrategyRoot {

    private static final String SUBNET_ID_KEY = "subnetId";

    private static final String NETWORK_ID_KEY = "networkId";

    @Override
    public void doAction(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        var stackEntity = (StackTestDto) entity;
        var client = getTestContextCloudbreakClient().apply(integrationTestContext);

        var credential = setCredentialIfNeededAndReturnIt(stackEntity, integrationTestContext);

        setClusterIfNeeded(stackEntity, integrationTestContext, credential);
        setKerberosIfNeeded(stackEntity, integrationTestContext);
        setGatewayIfNeeded(stackEntity, integrationTestContext);
        setImageSettingsIfNeeded(stackEntity, integrationTestContext);
        setHostGroupIfNeeded(stackEntity, integrationTestContext);

        postStackAndSetRequestForEntity(integrationTestContext, client, stackEntity);
    }
}
