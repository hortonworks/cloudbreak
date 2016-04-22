package com.sequenceiq.it.mock.restito;

import java.io.IOException;

import org.glassfish.grizzly.http.server.Response;

import com.xebialabs.restito.semantics.Function;

public abstract class CustomRestitoFunction implements Function<Response, Response> {

    protected abstract String getContent();

    @Override
    public Response apply(Response response) {
        byte[] content = getContent().getBytes();
        response.setContentLength(content.length);
        try {
            response.getOutputStream().write(content);
        } catch (IOException e) {
            throw new RuntimeException("Can not write resource content for restito stubbing.");
        }
        return response;
    }
}
