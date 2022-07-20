package com.sequenceiq.mock.freeipa.response;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.RPCMessage;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@Component
public class ServerConnCheckFreeipaRpcResponse extends AbstractFreeIpaResponse<Boolean> {

    @Override
    public String method() {
        return "server_conncheck";
    }

    @Override
    protected Boolean handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        return metadatas.stream().anyMatch(m -> m.getCloudVmInstanceStatus().getStatus() == InstanceStatus.STARTED);
    }

    @Override
    protected List<RPCMessage> getMessages(Boolean result, List<CloudVmMetaDataStatus> metadatas, String body) {
        List<RPCMessage> messages = new ArrayList<>();
        if (result) {
            messages.add(message("Instances are started"));
        } else {
            metadatas.forEach(m -> {
                messages.add(message(m.getMetaData().getPublicIp() + " is " + m.getCloudVmInstanceStatus().getStatus()));
            });
        }
        return messages;
    }

    private RPCMessage message(String message) {
        RPCMessage rpcMessage = new RPCMessage();
        rpcMessage.setName(method());
        rpcMessage.setMessage(message);
        return rpcMessage;
    }
}
