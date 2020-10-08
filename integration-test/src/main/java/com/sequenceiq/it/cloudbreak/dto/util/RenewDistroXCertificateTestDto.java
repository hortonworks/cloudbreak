package com.sequenceiq.it.cloudbreak.dto.util;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;

@Prototype
public class RenewDistroXCertificateTestDto extends AbstractCloudbreakTestDto<Object, Response, RenewDistroXCertificateTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenewDistroXCertificateTestDto.class);

    private String stackCrn;

    protected RenewDistroXCertificateTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public RenewDistroXCertificateTestDto valid() {
        DistroXTestDto distroXTestDto = getTestContext().get(DistroXTestDto.class);
        this.stackCrn = distroXTestDto.getCrn();
        return this;
    }

    public RenewDistroXCertificateTestDto withStackCrn(String stackCrn) {
        this.stackCrn = stackCrn;
        return this;
    }

    public String getStackCrn() {
        return stackCrn;
    }

    @Override
    public int order() {
        return 500;
    }

}
