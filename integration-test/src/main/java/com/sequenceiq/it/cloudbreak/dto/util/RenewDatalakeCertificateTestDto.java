package com.sequenceiq.it.cloudbreak.dto.util;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;

@Prototype
public class RenewDatalakeCertificateTestDto extends AbstractSdxTestDto<Object, Response, RenewDatalakeCertificateTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenewDatalakeCertificateTestDto.class);

    private String stackCrn;

    protected RenewDatalakeCertificateTestDto(TestContext testContext) {
        super(null, testContext);
    }

    @Override
    public RenewDatalakeCertificateTestDto valid() {
        SdxTestDto sdxTestDto = getTestContext().get(SdxTestDto.class);
        if (sdxTestDto != null) {
            this.stackCrn = sdxTestDto.getCrn();
        } else {
            SdxInternalTestDto sdxInternalTestDto = getTestContext().get(SdxInternalTestDto.class);
            if (sdxInternalTestDto != null) {
                this.stackCrn = sdxInternalTestDto.getCrn();
            }
        }
        return this;
    }

    public RenewDatalakeCertificateTestDto withStackCrn(String stackCrn) {
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
