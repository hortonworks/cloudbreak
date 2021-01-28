package com.sequenceiq.mock.freeipa.response;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.client.RPCMessage;
import com.sequenceiq.cloudbreak.client.RPCResponse;

public abstract class AbstractFreeIpaResponse<T> {

    public Object handle(String body) throws Exception {
        RPCResponse<T> rpcResponse = new RPCResponse<>();
        rpcResponse.setSummary("summary");
        rpcResponse.setResult(handleInternal(body));
        rpcResponse.setCount(1);
        rpcResponse.setTruncated(Boolean.FALSE);
        rpcResponse.setMessages(getMessages());
        return Map.of("result", rpcResponse);
    }

    public abstract String method();

    protected List<RPCMessage> getMessages() {
        return null;
    }

    protected abstract T handleInternal(String body);
}
