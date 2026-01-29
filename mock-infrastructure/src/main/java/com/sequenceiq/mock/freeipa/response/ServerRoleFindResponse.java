package com.sequenceiq.mock.freeipa.response;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.ServerRole;

@Component
public class ServerRoleFindResponse extends AbstractFreeIpaResponse<List<ServerRole>> {
    @Override
    public String method() {
        return "server_role_find";
    }

    @Override
    protected List<ServerRole> handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        return metadatas.stream()
                .map(metadata ->
                        createServerRole(metadata.getCloudVmInstanceStatus().getCloudInstance().getStringParameter(CloudInstance.DISCOVERY_NAME)))
                .toList();
    }

    private ServerRole createServerRole(String serverFqdn) {
        ServerRole serverRole = new ServerRole();
        serverRole.setServerFqdn(serverFqdn);
        serverRole.setRole("CA server");
        serverRole.setStatus("enabled");
        return serverRole;
    }
}
