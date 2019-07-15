package com.sequenceiq.it.cloudbreak.mock.freeipa;

import org.springframework.stereotype.Component;

import spark.Request;
import spark.Response;

@Component
public class DummyResponse extends AbstractFreeIpaResponse<Object> {
    @Override
    public String method() {
        return "dummy";
    }

    @Override
    protected Object handleInternal(Request request, Response response) {
        return "";
    }
}
