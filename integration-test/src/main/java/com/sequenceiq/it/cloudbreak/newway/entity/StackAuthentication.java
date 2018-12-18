package com.sequenceiq.it.cloudbreak.newway.entity;

import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationResponse;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class StackAuthentication extends AbstractCloudbreakEntity<StackAuthenticationRequest, StackAuthenticationResponse, StackAuthentication> {

    protected StackAuthentication(StackAuthenticationRequest request, TestContext testContext) {
        super(request, testContext);
    }

    protected StackAuthentication(TestContext testContext) {
        super(new StackAuthenticationRequest(), testContext);
    }

    public StackAuthentication() {
        super(StackAuthentication.class.getSimpleName().toUpperCase());
    }

    public StackAuthentication valid() {
        return withPublicKeyId("publicKeyId");
    }

    public StackAuthentication withPublicKey(String publicKey) {
        getRequest().setPublicKey(publicKey);
        return this;
    }

    public StackAuthentication withPublicKeyId(String publicKeyId) {
        getRequest().setPublicKeyId(publicKeyId);
        return this;
    }

    public StackAuthentication withLoginUserName(String loginUserName) {
        getRequest().setLoginUserName(loginUserName);
        return this;
    }
}
