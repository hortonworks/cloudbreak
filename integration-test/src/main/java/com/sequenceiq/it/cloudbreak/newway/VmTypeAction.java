package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

import java.io.IOException;

public class VmTypeAction {

    private VmTypeAction() {
    }

    public static void getVmTypesByCredentialId(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        VmTypeEntity vmtypeEntity = (VmTypeEntity) entity;
        CloudbreakClient client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);

        Credential credential = Credential.getTestContextCredential().apply(integrationTestContext);
        if (credential != null && vmtypeEntity.getRequest().getCredentialId() == null) {
            vmtypeEntity.getRequest().setCredentialName(credential.getName());
        }

        Region region = Region.getTestContextRegion().apply(integrationTestContext);
        if (region != null && vmtypeEntity.getRequest().getRegion() == null) {
            vmtypeEntity.getRequest().setRegion(region.getRegionResponse().getDefaultRegion());
        }

        Log.log(String.join(" ", " get Virtual Machine Types to",
                vmtypeEntity.getRequest().getCredentialName(), "credential and to",
                vmtypeEntity.getRequest().getRegion(), "region "));
        vmtypeEntity.setResponse(client.getCloudbreakClient()
                .connectorV2Endpoint()
                .getVmTypesByCredentialId(vmtypeEntity.getRequest()));
        Log.logJSON(" get Virtual Machine Types response: ", vmtypeEntity.getResponse());
    }
}
