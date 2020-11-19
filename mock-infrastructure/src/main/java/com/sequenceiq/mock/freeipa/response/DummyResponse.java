package com.sequenceiq.mock.freeipa.response;

import org.springframework.stereotype.Component;

@Component
public class DummyResponse extends AbstractFreeIpaResponse<Object> {
    @Override
    public String method() {
        return "dummy";
    }

    @Override
    protected Object handleInternal(String body) {
        return "";
    }
}
