package com.sequenceiq.it.cloudbreak.dto.environment;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import com.sequenceiq.environment.api.v1.environment.model.request.SetupCrossRealmTrustRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractEnvironmentTestDto;

@Prototype
public class EnvironmentTrustSetupDto extends AbstractEnvironmentTestDto<SetupCrossRealmTrustRequest, SetupCrossRealmTrustRequest, EnvironmentTrustSetupDto> {
    @Value("${integrationtest.trust.remoteEnvironmentCrn}")
    private String remoteEnvironmentCrn;

    @Value("${integrationtest.trust.activedirectory.fqdn}")
    private String activeDirectoryFqdn;

    @Value("${integrationtest.trust.activedirectory.ip}")
    private String activeDirectoryIp;

    @Value("${integrationtest.trust.activedirectory.realm}")
    private String activeDirectoryRealm;

    public EnvironmentTrustSetupDto(TestContext testContext) {
        super(new SetupCrossRealmTrustRequest(), testContext);
    }

    @Override
    public EnvironmentTrustSetupDto valid() {
        getRequest().setRemoteEnvironmentCrn(remoteEnvironmentCrn);
        getRequest().setFqdn(activeDirectoryFqdn);
        getRequest().setIp(activeDirectoryIp);
        getRequest().setRealm(activeDirectoryRealm);
        return this;
    }

    @Override
    public String getCrn() {
        EnvironmentTestDto environmentTestDto = getTestContext().get(EnvironmentTestDto.class);
        if (environmentTestDto != null && environmentTestDto.getResponse() != null) {
            return environmentTestDto.getResponse().getCrn();
        } else {
            throw new IllegalArgumentException(String.format("Environment has not been provided for this Environment Trust Setup: '%s' response!", getName()));
        }
    }

    public EnvironmentTrustSetupDto await(EnvironmentStatus status) {
        return getTestContext().await(this, Map.of("status", status), emptyRunningParameter());
    }
}
