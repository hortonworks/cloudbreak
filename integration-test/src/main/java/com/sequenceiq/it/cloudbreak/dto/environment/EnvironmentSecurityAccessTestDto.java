package com.sequenceiq.it.cloudbreak.dto.environment;

import com.sequenceiq.environment.api.v1.environment.model.request.SecurityAccessRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class EnvironmentSecurityAccessTestDto extends AbstractCloudbreakTestDto<SecurityAccessRequest, SecurityAccessResponse,
        EnvironmentSecurityAccessTestDto> {

    protected EnvironmentSecurityAccessTestDto(SecurityAccessRequest request, TestContext testContext) {
        super(request, testContext);
    }

    protected EnvironmentSecurityAccessTestDto(TestContext testContext) {
        super(new SecurityAccessRequest(), testContext);
    }

    public EnvironmentSecurityAccessTestDto() {
        super(EnvironmentSecurityAccessTestDto.class.getSimpleName().toUpperCase());
    }

    public EnvironmentSecurityAccessTestDto valid() {
        return getCloudProvider().environmentSecurityAccess(this);
    }

    public EnvironmentSecurityAccessTestDto withCidr(String publicKey) {
        getRequest().setCidr(publicKey);
        return this;
    }

    public EnvironmentSecurityAccessTestDto withSecurityGroupIdForKnox(String publicKeyId) {
        getRequest().setSecurityGroupIdForKnox(publicKeyId);
        return this;
    }

    public EnvironmentSecurityAccessTestDto withDefaultSecurityGroupId(String loginUserName) {
        getRequest().setDefaultSecurityGroupId(loginUserName);
        return this;
    }
}
