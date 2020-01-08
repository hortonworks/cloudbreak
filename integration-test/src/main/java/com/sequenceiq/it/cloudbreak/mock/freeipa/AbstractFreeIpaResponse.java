package com.sequenceiq.it.cloudbreak.mock.freeipa;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.springframework.http.HttpStatus;

import com.sequenceiq.freeipa.client.model.RPCMessage;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;

import spark.Request;
import spark.Response;

public abstract class AbstractFreeIpaResponse<T> extends ITResponse {
    @Override
    public Object handle(Request request, Response response) throws Exception {
        response.type(MediaType.APPLICATION_JSON);
        response.status(HttpStatus.OK.value());
        RPCResponse<T> rpcResponse = new RPCResponse<>();
        rpcResponse.setSummary("summary");
        rpcResponse.setResult(handleInternal(request, response));
        rpcResponse.setCount(1);
        rpcResponse.setTruncated(Boolean.FALSE);
        rpcResponse.setMessages(getMessages());
        return Map.of("result", rpcResponse);
    }

    public abstract String method();

    protected List<RPCMessage> getMessages() {
        return null;
    }

    protected abstract T handleInternal(Request request, Response response);
}
