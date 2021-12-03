package com.sequenceiq.mock.freeipa.response;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.RPCMessage;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;

@Component
public class ServerConnCheckFreeipaRpcResponse extends AbstractFreeIpaResponse<Boolean> {

    private Boolean result;

    private List<RPCMessage> messages;

    public ServerConnCheckFreeipaRpcResponse() {
        this(true, new ArrayList<>());
        RPCMessage message = new RPCMessage();
        message.setName("name");
        message.setMessage("message");
        messages.add(message);
    }

    private ServerConnCheckFreeipaRpcResponse(Boolean result, List<RPCMessage> messages) {
        this.result = result;
        this.messages = messages;
    }

    @Override
    public String method() {
        return "server_conncheck";
    }

    @Override
    protected Boolean handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        return result;
    }

    @Override
    protected List<RPCMessage> getMessages() {
        return messages;
    }

    public static ServerConnCheckFreeipaRpcResponse unreachable() {
        RPCMessage rpcMessage = new RPCMessage();
        rpcMessage.setMessage("Unreachable mock");
        return new ServerConnCheckFreeipaRpcResponse(false, List.of(rpcMessage));
    }
}
