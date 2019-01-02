package com.sequenceiq.it.cloudbreak.newway.entity;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.authentication.StackAuthenticationV4Response;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class StackAuthenticationEntity extends AbstractCloudbreakEntity<StackAuthenticationV4Request, StackAuthenticationV4Response, StackAuthenticationEntity> {

    protected StackAuthenticationEntity(StackAuthenticationV4Request request, TestContext testContext) {
        super(request, testContext);
    }

    protected StackAuthenticationEntity(TestContext testContext) {
        super(new StackAuthenticationV4Request(), testContext);
    }

    public StackAuthenticationEntity() {
        super(StackAuthenticationEntity.class.getSimpleName().toUpperCase());
    }

    public StackAuthenticationEntity valid() {
        return withPublicKeyId("publicKeyId");
    }

    public StackAuthenticationEntity withPublicKey(String publicKey) {
        getRequest().setPublicKey(publicKey);
        return this;
    }

    public StackAuthenticationEntity withPublicKeyId(String publicKeyId) {
        getRequest().setPublicKeyId(publicKeyId);
        return this;
    }

    public StackAuthenticationEntity withLoginUserName(String loginUserName) {
        getRequest().setLoginUserName(loginUserName);
        return this;
    }
}
