package com.sequenceiq.it.cloudbreak.newway.dto;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.authentication.StackAuthenticationV4Response;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class StackAuthenticationTestDto extends AbstractCloudbreakTestDto<
        StackAuthenticationV4Request, StackAuthenticationV4Response, StackAuthenticationTestDto> {

    protected StackAuthenticationTestDto(StackAuthenticationV4Request request, TestContext testContext) {
        super(request, testContext);
    }

    protected StackAuthenticationTestDto(TestContext testContext) {
        super(new StackAuthenticationV4Request(), testContext);
    }

    public StackAuthenticationTestDto() {
        super(StackAuthenticationTestDto.class.getSimpleName().toUpperCase());
    }

    public StackAuthenticationTestDto valid() {
        return getCloudProvider().stackAuthentication(this);
    }

    public StackAuthenticationTestDto withPublicKey(String publicKey) {
        getRequest().setPublicKey(publicKey);
        return this;
    }

    public StackAuthenticationTestDto withPublicKeyId(String publicKeyId) {
        getRequest().setPublicKeyId(publicKeyId);
        return this;
    }

    public StackAuthenticationTestDto withLoginUserName(String loginUserName) {
        getRequest().setLoginUserName(loginUserName);
        return this;
    }
}
