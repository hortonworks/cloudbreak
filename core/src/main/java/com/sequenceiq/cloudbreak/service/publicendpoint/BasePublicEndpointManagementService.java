package com.sequenceiq.cloudbreak.service.publicendpoint;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.certificate.service.CertificateCreationService;
import com.sequenceiq.cloudbreak.certificate.service.DnsManagementService;
import com.sequenceiq.cloudbreak.dns.EnvironmentBasedDomainNameProvider;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public class BasePublicEndpointManagementService {

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

    public boolean isCertGenerationEnabled() {
        return certGenerationEnabled;
    }

    public DnsManagementService getDnsManagementService() {
        return dnsManagementService;
    }

    public EnvironmentBasedDomainNameProvider getDomainNameProvider() {
        return domainNameProvider;
    }

    public GrpcUmsClient getGrpcUmsClient() {
        return grpcUmsClient;
    }

    public CertificateCreationService getCertificateCreationService() {
        return certificateCreationService;
    }

    String getWorkloadSubdomain(String actorCrn) {
        Optional<String> requestIdOptional = Optional.ofNullable(MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString()));
        UserManagementProto.Account account = grpcUmsClient.getAccountDetails(actorCrn, Crn.safeFromString(actorCrn).getAccountId(), requestIdOptional);
        return account.getWorkloadSubdomain();
    }
}
