package com.sequenceiq.cloudbreak.service.publicendpoint;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;

import com.sequenceiq.cloudbreak.certificate.service.CertificateCreationService;
import com.sequenceiq.cloudbreak.certificate.service.DnsManagementService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.view.StackView;

import software.amazon.awssdk.utils.StringUtils;

public abstract class BasePublicEndpointManagementService {

    @Value("${gateway.cert.generation.enabled:false}")
    private boolean certGenerationEnabled;

    @Value("${gateway.cert.generation.enabled-for-mock:false}")
    private boolean certGenerationEnabledForMock;

    @Inject
    private DnsManagementService dnsManagementService;

    @Inject
    private EnvironmentBasedDomainNameProvider domainNameProvider;

    @Inject
    private CertificateCreationService certificateCreationService;

    public boolean manageCertificateAndDnsInPem(StackView stackView) {
        if (StringUtils.equals(CloudPlatform.MOCK.name(), stackView.getCloudPlatform())) {
            return certGenerationEnabledForMock;
        }
        return certGenerationEnabled;
    }

    public DnsManagementService getDnsManagementService() {
        return dnsManagementService;
    }

    public EnvironmentBasedDomainNameProvider getDomainNameProvider() {
        return domainNameProvider;
    }

    public CertificateCreationService getCertificateCreationService() {
        return certificateCreationService;
    }

    protected void setCertGenerationEnabled(boolean enabled) {
        this.certGenerationEnabled = enabled;
    }

    public boolean isPemEnabled() {
        return certGenerationEnabled;
    }
}