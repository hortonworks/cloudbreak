package com.sequenceiq.it.cloudbreak.action.v4.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.RenewDistroXCertificateTestDto;

public class RenewDistroXCertificateAction implements Action<RenewDistroXCertificateTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenewDistroXCertificateAction.class);

    @Override
    public RenewDistroXCertificateTestDto action(TestContext testContext, RenewDistroXCertificateTestDto renewCertificateTestDto,
            CloudbreakClient cloudbreakClient) throws Exception {
        cloudbreakClient.getDefaultClient().distroXV1Endpoint().renewCertificate(renewCertificateTestDto.getStackCrn());
        return renewCertificateTestDto;
    }
}
