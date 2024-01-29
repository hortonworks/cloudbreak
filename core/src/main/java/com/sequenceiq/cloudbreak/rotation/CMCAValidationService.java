package com.sequenceiq.cloudbreak.rotation;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaClientService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Service
public class CMCAValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CMCAValidationService.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private FreeipaClientService freeipaClientService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    public void checkCMCAWithRootCert(Long stackId) {
        try {
            StackDto stack = stackDtoService.getById(stackId);
            ClusterApi connector = clusterApiConnectors.getConnector(stack);
            Optional<String> trustStoreFromCM = connector.clusterSecurityService().getTrustStoreForValidation();
            if (trustStoreFromCM.isPresent()) {
                String rootCertFromFMS = freeipaClientService.getRootCertificateByEnvironmentCrn(stack.getEnvironmentCrn());
                List<X509Certificate> x509CertificatesFromCM = readPEMCertificatesFromString(trustStoreFromCM.get());
                List<X509Certificate> x509CertificatesFromFMS = readPEMCertificatesFromString(rootCertFromFMS);
                X509Certificate latestRootCertFromFMS = x509CertificatesFromFMS.stream()
                        .min(Comparator.comparing(X509Certificate::getNotAfter, Comparator.nullsLast(Comparator.reverseOrder())))
                        .orElseThrow(() -> new CloudbreakServiceException("FreeIPA root cert cannot be found!"));
                X509Certificate cmcaCertificate = x509CertificatesFromCM.stream()
                        .filter(cert -> cert.getSubjectX500Principal().getName().contains(stack.getResourceName()))
                        .findFirst()
                        .orElseThrow(() -> new CloudbreakServiceException(String.format("CM intermediate certificate cannot be found for stack %S",
                                stack.getResourceName())));
                cmcaCertificate.verify(latestRootCertFromFMS.getPublicKey());
            } else {
                LOGGER.info("Couldn't get trust store from CM, thus skipping validation.");
            }
        } catch (Exception e) {
            throw new CloudbreakServiceException(e);
        }
    }

    private static List<X509Certificate> readPEMCertificatesFromString(String pemData) {
        List<X509Certificate> certificates = new ArrayList<>();
        Pattern pattern = Pattern.compile("-----BEGIN CERTIFICATE-----(.*?)-----END CERTIFICATE-----", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(pemData);
        while (matcher.find()) {
            String pemCertificate = matcher.group(1).trim().replace("\n", "").replace("\r", "");
            try {
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(
                        new ByteArrayInputStream(Base64.getDecoder().decode(pemCertificate)));
                certificates.add(certificate);
            } catch (CertificateException e) {
                LOGGER.error("Cannot read certificate.");
            }
        }
        return certificates;
    }
}
