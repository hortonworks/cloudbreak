package com.sequenceiq.it.cloudbreak.dto.mock;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class HttpMock extends AbstractCloudbreakTestDto<Void, Void, HttpMock> {

    protected HttpMock(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public HttpMock valid() {
        return this;
    }

    @Override
    public String getCrn() {
        return null;
    }
}
