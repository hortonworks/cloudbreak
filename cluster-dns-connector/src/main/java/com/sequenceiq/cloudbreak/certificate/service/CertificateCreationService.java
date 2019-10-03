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

    @Value("${gateway.cert.polling.attempt:20}")
    private Integer pollingAttempt;

    @Inject
    private GrpcClusterDnsClient grpcClusterDnsClient;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private EnvironmentBasedDomainNameProvider environmentBasedDomainNameProvider;

    public List<String> create(String actorCrn, String accountId, String endpoint, String environment, boolean wildcard, KeyPair identity)
            throws IOException {
        LOGGER.info("Start cert creation");
        Optional<String> requestIdOptional = Optional.ofNullable(MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString()));
        UserManagementProto.Account account = grpcUmsClient.getAccountDetails(actorCrn, actorCrn, requestIdOptional);
        String externalFQDN = environmentBasedDomainNameProvider.getFullyQualifiedEndpointName(endpoint, environment, account.getWorkloadSubdomain());
        LOGGER.info("Create cert for {}", externalFQDN);
        PKCS10CertificationRequest csr = PkiUtil.csr(identity, externalFQDN);
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
}
