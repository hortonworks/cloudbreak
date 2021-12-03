package com.sequenceiq.mock.freeipa.response;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;

@Component
public class DummyResponse extends AbstractFreeIpaResponse<Object> {
    @Override
    public String method() {
        return "dummy";
    }

    @Override
    protected Object handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        return "";
    }
}
