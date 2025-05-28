package com.sequenceiq.cloudbreak.service;

import static java.lang.String.format;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.clusterproxy.ReadConfigResponse;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class ClusterProxyRotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyRotationService.class);

    private static final String MODULUS = "n";

    private static final String PUBLIC_EXPONENT = "e";

    private static final String PRIVATE_EXPONENT = "d";

    private static final String PRIME_P = "p";

    private static final String PRIME_Q = "q";

    private static final String PRIME_EXPONENT_P = "dp";

    private static final String PRIME_EXPONENT_Q = "dq";

    private static final String CRT_COEFFICIENT = "qi";

    private static final int SECRETREF_MAX_LENGTH = 3;

    private static final String BASE64_ENCODING = "BASE64";

    private static final String NO_ENCODING = "TEXT";

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Inject
    private ClusterProxyService clusterProxyService;

    public KeyPair readClusterProxyTokenKeys(StackView stack) {
        ReadConfigResponse readConfigResponse = clusterProxyService.readConfig(stack);
        return readClusterProxyTokenKeys(readConfigResponse);
    }

    public KeyPair readClusterProxyTokenKeys(ReadConfigResponse readConfigResponse) {
        LOGGER.info("Reading token keys from cluster-proxy vault. Path: '{}'", readConfigResponse.getKnoxSecretRef());
        String knoxSecretRef = readConfigResponse.getKnoxSecretRef();
        String[] pathAndField = knoxSecretRef.split(":", SECRETREF_MAX_LENGTH);
        try {
            checkPathAndField(pathAndField);
            String jwkJson = uncachedSecretServiceForRotation.getBySecretPath(pathAndField[0], pathAndField[1]);
            if (isBase64Encoded(pathAndField)) {
                jwkJson = new String(Base64.getDecoder().decode(jwkJson));
            }
            LOGGER.info("JWK json read from cluster-proxy, length: {}", StringUtils.length(jwkJson));
            return convertJWKToPEMKeys(jwkJson);
        } catch (IllegalArgumentException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new CloudbreakServiceException("Cannot read JWK format token keys from cluster-proxy.", e);
        }
    }

    private boolean isBase64Encoded(String[] pathAndField) {
        return pathAndField.length == SECRETREF_MAX_LENGTH && BASE64_ENCODING.equalsIgnoreCase(pathAndField[2]);
    }

    public String generateClusterProxySecretFormat(String knoxSecretJson) {
        Map<String, Object> knoxSecretMap = new Json(knoxSecretJson).getMap();
        String knoxSecretPath = (String) knoxSecretMap.get("path");
        return format("%s:%s", knoxSecretPath, "secret");
    }

    private void checkPathAndField(String[] pathAndField) {
        if (pathAndField == null || !(pathAndField.length == 2 || pathAndField.length == SECRETREF_MAX_LENGTH)) {
            throw new IllegalArgumentException("Cannot read jwk from cluster-proxy, secret path invalid.");
        }
        if (pathAndField.length == SECRETREF_MAX_LENGTH && pathAndField[2] != null && !Set.of(NO_ENCODING, BASE64_ENCODING)
                .contains(pathAndField[2].toUpperCase())) {
            throw new IllegalArgumentException(format("Cannot read jwk from cluster-proxy, unknown encoding: '%s'", pathAndField[2]));
        }
        if (!pathAndField[0].startsWith("cluster-proxy/")) {
            throw new IllegalArgumentException(format("Cannot read jwk from cluster-proxy, not a cluster-proxy vault path. Path: '%s'", pathAndField[0]));
        }
    }

    private KeyPair convertJWKToPEMKeys(String secretValue) throws NoSuchAlgorithmException, InvalidKeySpecException {
        Map<String, Object> jwkMap = new Json(secretValue).getMap();
        checkJwkMap(jwkMap);
        RSAPrivateCrtKeySpec rsaCrtSpec =
                new RSAPrivateCrtKeySpec(
                        new BigInteger(1, Base64.getUrlDecoder().decode((String) jwkMap.get(MODULUS))),
                        new BigInteger(1, Base64.getUrlDecoder().decode((String) jwkMap.get(PUBLIC_EXPONENT))),
                        new BigInteger(1, Base64.getUrlDecoder().decode((String) jwkMap.get(PRIVATE_EXPONENT))),
                        new BigInteger(1, Base64.getUrlDecoder().decode((String) jwkMap.get(PRIME_P))),
                        new BigInteger(1, Base64.getUrlDecoder().decode((String) jwkMap.get(PRIME_Q))),
                        new BigInteger(1, Base64.getUrlDecoder().decode((String) jwkMap.get(PRIME_EXPONENT_P))),
                        new BigInteger(1, Base64.getUrlDecoder().decode((String) jwkMap.get(PRIME_EXPONENT_Q))),
                        new BigInteger(1, Base64.getUrlDecoder().decode((String) jwkMap.get(CRT_COEFFICIENT))));

        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPrivateCrtKey rsaPrivateKey = (RSAPrivateCrtKey) kf.generatePrivate(rsaCrtSpec);
        RSAPublicKeySpec publicKeySpec = new java.security.spec.RSAPublicKeySpec(rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent());
        PublicKey rsaPublicKey = kf.generatePublic(publicKeySpec);
        return new KeyPair(rsaPublicKey, rsaPrivateKey);
    }

    /*
     * These are the elements of the RSA encryption, and the JWK token we get from cluster-proxy
     * Please refer to: https://en.wikipedia.org/wiki/RSA_(cryptosystem)
     */
    private void checkJwkMap(Map<String, Object> jwkMap) {
        if (jwkMap == null || !jwkMap.keySet().containsAll(Set.of(MODULUS, PUBLIC_EXPONENT, PRIVATE_EXPONENT, PRIME_P, PRIME_Q,
                PRIME_EXPONENT_P, PRIME_EXPONENT_Q, CRT_COEFFICIENT))) {
            throw new IllegalArgumentException("JWK key from cluster-proxy cannot converted to PEM, key elements missing.");
        }
    }

    public TokenCertInfo generateTokenCert() {
        try {
            KeyPair identityKey = PkiUtil.generateKeypair();
            KeyPair signKeyPair = PkiUtil.generateKeypair();
            X509Certificate cert = PkiUtil.cert(identityKey, "signing", signKeyPair);

            String tokenKey = PkiUtil.convert(identityKey.getPrivate());
            String tokenPub = PkiUtil.convertPemPublicKey(identityKey.getPublic());
            String tokenCert = Base64.getEncoder().encodeToString(cert.getEncoded());
            return new TokenCertInfo(tokenKey, tokenPub, tokenCert);
        } catch (CertificateEncodingException e) {
            throw new CloudbreakServiceException("Cannot generate new tokenCert for Gateway rotation.", e);
        }
    }

}
