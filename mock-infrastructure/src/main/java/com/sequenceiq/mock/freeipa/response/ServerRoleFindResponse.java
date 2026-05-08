package com.sequenceiq.mock.freeipa.response;

import java.util.List;
import java.util.stream.Stream;

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
                        metadata.getCloudVmInstanceStatus().getCloudInstance().getStringParameter(CloudInstance.DISCOVERY_NAME))
                .flatMap(serverFqdn -> Stream.of(
                        createServerRole(serverFqdn, "CA server"),
                        createServerRole(serverFqdn, "DNS server")
                ))
                .toList();
    }

    private ServerRole createServerRole(String serverFqdn, String role) {
        ServerRole serverRole = new ServerRole();
        serverRole.setServerFqdn(serverFqdn);
        serverRole.setRole(role);
        serverRole.setStatus("enabled");
        return serverRole;
    }
}
