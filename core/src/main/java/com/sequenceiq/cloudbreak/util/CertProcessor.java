package com.sequenceiq.cloudbreak.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.util.Locale;

import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Component
public class CertProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CertProcessor.class);

    private static final String CERT_DELIMITER = "-----END CERTIFICATE-----\\n";

    /**
     * A cert looks like in this string:
     *-----BEGIN CERTIFICATE-----
     * ...
     * -----END CERTIFICATE-----
     * These repeats multiple times, and this method splits this large input into an array of single valid certs.
     * @param certs contains multiple certificates
     * @return every item is a single cert
     */
    public String[] itemizeSingleLargeCertInput(String certs) {
        return certs.split("(?<=" + CERT_DELIMITER + ")");
    }

    public String calculateSha256FingerprintForCert(String cert) {
        try {
            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256Digest.digest(PkiUtil.fromCertificatePem(cert).getEncoded());
            return new String(Hex.encode(digest)).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            LOGGER.error("Failed to get SHA-256 digest for cert: {}", cert, e);
            throw new CloudbreakServiceException("Failed to get SHA-256 digest for cert", e);
        }
    }
}
