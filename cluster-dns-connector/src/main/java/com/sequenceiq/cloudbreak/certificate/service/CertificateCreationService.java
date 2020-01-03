package com.sequenceiq.cloudbreak.certificate.service;

import com.dyngr.Polling;
import com.sequenceiq.cloudbreak.certificate.poller.CreateCertificationPoller;
import com.sequenceiq.cloudbreak.client.GrpcClusterDnsClient;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class CertificateCreationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateCreationService.class);

    @Value("${gateway.cert.polling.intervall:10}")
    private Long pollingIntervall;

    @Value("${gateway.cert.polling.attempt:80}")
    private Integer pollingAttempt;

    @Inject
    private GrpcClusterDnsClient grpcClusterDnsClient;

    public List<String> create(String actorCrn, String accountId, String endpoint, String environment, PKCS10CertificationRequest csr)
            throws IOException {
        LOGGER.info("Starting certificate creation for endpoint: {} in environment: {}", endpoint, environment);
        Optional<String> requestIdOptional = Optional.ofNullable(MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString()));
        String pollingRequestId = grpcClusterDnsClient.signCertificate(actorCrn, accountId, environment, csr.getEncoded(), requestIdOptional);
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
