package com.sequenceiq.it.cloudbreak.dto.environment;

import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAuthenticationRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class EnvironmentAuthenticationTestDto extends AbstractCloudbreakTestDto<EnvironmentAuthenticationRequest, EnvironmentAuthenticationResponse,
        EnvironmentAuthenticationTestDto> {

    protected EnvironmentAuthenticationTestDto(EnvironmentAuthenticationRequest request, TestContext testContext) {
        super(request, testContext);
    }

    protected EnvironmentAuthenticationTestDto(TestContext testContext) {
        super(new EnvironmentAuthenticationRequest(), testContext);
    }

    public EnvironmentAuthenticationTestDto() {
        super(EnvironmentAuthenticationTestDto.class.getSimpleName().toUpperCase());
    }

    public EnvironmentAuthenticationTestDto valid() {
        return getCloudProvider().environmentAuthentication(this);
    }

    public EnvironmentAuthenticationTestDto withPublicKey(String publicKey) {
        getRequest().setPublicKey(publicKey);
        return this;
    }

    public EnvironmentAuthenticationTestDto withPublicKeyId(String publicKeyId) {
        getRequest().setPublicKeyId(publicKeyId);
        return this;
    }

    public EnvironmentAuthenticationTestDto withLoginUserName(String loginUserName) {
        getRequest().setLoginUserName(loginUserName);
        return this;
    }
}
