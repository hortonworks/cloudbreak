package com.sequenceiq.it.cloudbreak.newway.v3;

import java.io.IOException;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.Region;
import com.sequenceiq.it.cloudbreak.newway.VmTypeEntity;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class VmTypeV3Action {

    private VmTypeV3Action() {
    }

    public static void getVmTypesByCredentialId(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        VmTypeEntity vmtypeEntity = (VmTypeEntity) entity;
        CloudbreakClient client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);

        Credential credential = Credential.getTestContextCredential().apply(integrationTestContext);
        if (credential != null && vmtypeEntity.getRequest().getCredentialId() == null) {
            vmtypeEntity.getRequest().setCredentialName(credential.getName());
        }

        Region region = Region.getTestContextRegion().apply(integrationTestContext);
        if (region != null && vmtypeEntity.getRequest().getRegion() == null) {
            vmtypeEntity.getRequest().setRegion(region.getRegionV4Response().getDefaultRegion());
        }

        Log.log(String.join(" ", " get Virtual Machine Types to",
                vmtypeEntity.getRequest().getCredentialName(), "credential and to",
                vmtypeEntity.getRequest().getRegion(), "region "));
        vmtypeEntity.setResponse(client.getCloudbreakClient()
                .connectorV4Endpoint()
                .getVmTypesByCredential(workspaceId, vmtypeEntity.getRequest()));
        Log.logJSON(" get Virtual Machine Types response: ", vmtypeEntity.getResponse());
    }
}
