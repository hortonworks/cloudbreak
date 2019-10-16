package com.sequenceiq.cloudbreak.certificate.service;

import java.io.IOException;
import java.security.KeyPair;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.dyngr.Polling;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.certificate.poller.CreateCertificationPoller;
import com.sequenceiq.cloudbreak.client.GrpcClusterDnsClient;
import com.sequenceiq.cloudbreak.dns.EnvironmentBasedDomainNameProvider;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Service
public class CertificateCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateCreationService.class);

    @Value("${gateway.cert.polling.intervall:10}")
    private Long pollingIntervall;

    @Value("${gateway.cert.polling.attempt:80}")
    private Integer pollingAttempt;

    @Inject
    private GrpcClusterDnsClient grpcClusterDnsClient;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private EnvironmentBasedDomainNameProvider domainNameProvider;

    public List<String> create(String actorCrn, String accountId, String endpoint, String environment, boolean wildcard, KeyPair identity)
            throws IOException {
        LOGGER.info("Starting certificate creation for endpoint: {} in environment: {}", endpoint, environment);
        Optional<String> requestIdOptional = Optional.ofNullable(MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString()));
        UserManagementProto.Account account = grpcUmsClient.getAccountDetails(actorCrn, actorCrn, requestIdOptional);
        PKCS10CertificationRequest csr = generateCSR(endpoint, environment, identity, account);
        String pollingRequestId = grpcClusterDnsClient
                .createCertificate(actorCrn, accountId, endpoint, environment, wildcard, csr.getEncoded(), requestIdOptional);
        return polling(actorCrn, pollingRequestId);
    }

    public List<String> polling(String actorCrn, String pollingRequestId) {
        Optional<String> requestIdOptional = Optional.ofNullable(MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString()));
        return Polling.waitPeriodly(pollingIntervall, TimeUnit.SECONDS)
                .stopAfterAttempt(pollingAttempt)
                .stopIfException(true)
                .run(new CreateCertificationPoller(grpcClusterDnsClient, actorCrn, pollingRequestId, requestIdOptional));
    }

    private PKCS10CertificationRequest generateCSR(String endpoint, String environment, KeyPair identity, UserManagementProto.Account account) {
        String fullyQualifiedEndpointName = domainNameProvider.getFullyQualifiedEndpointName(endpoint, environment, account.getWorkloadSubdomain());
        List<String> subjectAlternativeNames = List.of();
        LOGGER.info("Creating certificate with fully qualified endpoint name: {}", fullyQualifiedEndpointName);
        return PkiUtil.csr(identity, fullyQualifiedEndpointName, subjectAlternativeNames);
    }

    private PKCS10CertificationRequest generateCSRWithSANs(String endpoint, String environment, KeyPair identity, UserManagementProto.Account account) {
        String commonName = domainNameProvider.getCommonName(endpoint, environment, account.getWorkloadSubdomain());
        String fullyQualifiedEndpointName = domainNameProvider.getFullyQualifiedEndpointName(endpoint, environment, account.getWorkloadSubdomain());
        List<String> subjectAlternativeNames = List.of(commonName, fullyQualifiedEndpointName);
        LOGGER.info("Creating certificate with common name:{} and fully qualified endpoint name: {}", commonName, fullyQualifiedEndpointName);
        return PkiUtil.csr(identity, fullyQualifiedEndpointName, subjectAlternativeNames);
    }
}
