package com.sequenceiq.it.cloudbreak.dto.environment;

import java.util.List;

import jakarta.inject.Inject;

import com.sequenceiq.environment.api.v2.environment.model.request.AddCrossRealmTrustV2Request;
import com.sequenceiq.environment.api.v2.environment.model.request.SetupCrossRealmTrustV2ActiveDirectoryRequest;
import com.sequenceiq.environment.api.v2.environment.model.request.SetupCrossRealmTrustV2KdcServerRequest;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.config.TrustProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractEnvironmentTestDto;

@Prototype
public class EnvironmentDirectionalTrustSetupDto extends
        AbstractEnvironmentTestDto<AddCrossRealmTrustV2Request, AddCrossRealmTrustV2Request, EnvironmentDirectionalTrustSetupDto> {

    @Inject
    private TrustProperties trustProperties;

    public EnvironmentDirectionalTrustSetupDto(TestContext testContext) {
        super(new AddCrossRealmTrustV2Request(), testContext);
    }

    @Override
    public EnvironmentDirectionalTrustSetupDto valid() {
        String remoteEnvironmentCrn = trustProperties.getRemoteEnvironmentCrn(getTestContext().getActingUserCrn().getAccountId());
        getRequest().setRemoteEnvironmentCrn(remoteEnvironmentCrn);
        getRequest().setDnsServerIps(List.of(trustProperties.getActiveDirectoryIp()));
        SetupCrossRealmTrustV2ActiveDirectoryRequest ad = new SetupCrossRealmTrustV2ActiveDirectoryRequest();
        ad.setRealm(trustProperties.getActiveDirectoryRealm());
        SetupCrossRealmTrustV2KdcServerRequest server = new SetupCrossRealmTrustV2KdcServerRequest();
        server.setIp(trustProperties.getActiveDirectoryIp());
        server.setFqdn(trustProperties.getActiveDirectoryFqdn());
        ad.setServers(List.of(server));
        getRequest().setAd(ad);
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
}
