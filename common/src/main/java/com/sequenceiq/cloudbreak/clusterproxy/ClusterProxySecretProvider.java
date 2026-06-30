package com.sequenceiq.cloudbreak.clusterproxy;

import static java.lang.String.format;

import java.security.KeyPair;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;

@Service
public class ClusterProxySecretProvider {

    public String generateClusterProxySecretFormat(String knoxSecretJson) {
        Map<String, Object> knoxSecretMap = new Json(knoxSecretJson).getMap();
        String knoxSecretPath = (String) knoxSecretMap.get("path");
        return format("%s:%s:%s", knoxSecretPath, "secret", "TEXT");
    }

    public TokenCertInfo generateSignKeys() {
        try {
            KeyPair identityKey = PkiUtil.generateKeypair();
            KeyPair signKeyPair = PkiUtil.generateKeypair();
            X509Certificate cert = PkiUtil.cert(identityKey, "signing", signKeyPair);

            String signKey = PkiUtil.convert(identityKey.getPrivate());
            String signPub = PkiUtil.convertPemPublicKey(identityKey.getPublic());
            String signCert = PkiUtil.convert(cert);
            String tokenCert = Base64.getEncoder().encodeToString(cert.getEncoded());

            return new TokenCertInfo(signKey, signPub, signCert, tokenCert);
        } catch (CertificateEncodingException e) {
            throw new CloudbreakServiceException("Cannot generate new tokenCert for Gateway rotation.", e);
        }
    }
}
