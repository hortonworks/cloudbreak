package com.sequenceiq.it.cloudbreak.dto;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.redbeams.api.endpoint.v4.support.CertificateSwapV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.support.CertificateSwapV4Response;

@Prototype
public class CertificateSwapTestDto extends AbstractRedbeamsTestDto<CertificateSwapV4Request, CertificateSwapV4Response, CertificateSwapTestDto> {

    public CertificateSwapTestDto(TestContext testContext) {
        super(new CertificateSwapV4Request(), testContext);
    }

    @Override
    public CertificateSwapTestDto valid() {
        return withFirstCertificate(true).withSecondCertificate(true);
    }

    public CertificateSwapTestDto withFirstCertificate(Boolean firstCert) {
        getRequest().setFirstCert(firstCert);
        return this;
    }

    public CertificateSwapTestDto withSecondCertificate(Boolean secondCert) {
        getRequest().setSecondCert(secondCert);
        return this;
    }
}
