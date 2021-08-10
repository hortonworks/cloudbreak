package com.sequenceiq.cloudbreak.service.publicendpoint;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.certificate.service.CertificateCreationService;
import com.sequenceiq.cloudbreak.certificate.service.DnsManagementService;
import com.sequenceiq.cloudbreak.dns.EnvironmentBasedDomainNameProvider;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public abstract class BasePublicEndpointManagementService {

    @Value("${gateway.cert.generation.enabled:false}")
    private boolean certGenerationEnabled;

    @Inject
    private DnsManagementService dnsManagementService;

    @Inject
    private EnvironmentBasedDomainNameProvider domainNameProvider;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private CertificateCreationService certificateCreationService;

    public boolean manageCertificateAndDnsInPem() {
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

    String getWorkloadSubdomain(String accountId) {
        Optional<String> requestIdOptional = Optional.ofNullable(MDCBuilder.getOrGenerateRequestId());
        UserManagementProto.Account account = grpcUmsClient.getAccountDetails(accountId, requestIdOptional);
        return account.getWorkloadSubdomain();
    }

    protected void setCertGenerationEnabled(boolean enabled) {
        this.certGenerationEnabled = enabled;
    }
}
