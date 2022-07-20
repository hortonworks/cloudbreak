package com.sequenceiq.mock.freeipa.response;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.client.RPCMessage;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;

public abstract class AbstractFreeIpaResponse<T> {

    public Object handle(List<CloudVmMetaDataStatus> metadatas, String body) throws Exception {
        RPCResponse<T> rpcResponse = new RPCResponse<>();
        rpcResponse.setSummary("summary");
        T result = handleInternal(metadatas, body);
        rpcResponse.setResult(result);
        rpcResponse.setCount(1);
        rpcResponse.setTruncated(Boolean.FALSE);
        rpcResponse.setMessages(getMessages(result, metadatas, body));
        return Map.of("result", rpcResponse);
    }

    public abstract String method();

    protected List<RPCMessage> getMessages(T result, List<CloudVmMetaDataStatus> metadatas, String body) {
        return null;
    }

    protected abstract T handleInternal(List<CloudVmMetaDataStatus> metadatas, String body);
}
