package com.sequenceiq.cloudbreak.cloud.gcp.service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.services.compute.model.CustomerEncryptionKey;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Service
public class CustomGcpDiskEncryptionCreatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomGcpDiskEncryptionCreatorService.class);

    private static final String RSA_CIPHER_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding";

    private static final String CERTIFICATE_ENCODING = "X.509";

    private static final String HASH_ALGORITHM = "SHA-256";

    @Value("${cb.gcp.disk.encryption.url}")
    private String googlePublicCertUrl;

    public CustomerEncryptionKey createCustomerEncryptionKey(InstanceTemplate template) {
        String key = Optional.ofNullable(template.getStringParameter(InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID)).orElse("");
        String method = Optional.ofNullable(template.getStringParameter("keyEncryptionMethod")).orElse("RSA");

        switch (method) {
            case "RAW":
                return rawKey(key);
            case "RSA":
                return rsaEncryptedKey(key);
            case "KMS":
                return kmsKey(key);
            default:
                throw new CloudbreakServiceException("Not supported key encryption method: " + method);
        }
    }

    private CustomerEncryptionKey kmsKey(String kmsKeyPath) {
        CustomerEncryptionKey customerEncryptionKey = new CustomerEncryptionKey();
        customerEncryptionKey.setKmsKeyName(kmsKeyPath);
        return customerEncryptionKey;
    }

    private CustomerEncryptionKey rawKey(String encryptionKey) {
        CustomerEncryptionKey customerEncryptionKey = new CustomerEncryptionKey();
        customerEncryptionKey.setRawKey(encode(getEncryptionKeyBytes(encryptionKey)));
        return customerEncryptionKey;
    }

    private byte[] getEncryptionKeyBytes(String encryptionKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            return digest.digest(Optional.ofNullable(encryptionKey).orElse("").getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            List<String> supportedAlgorithms = Arrays.stream(Security.getProviders())
                    .map(Provider::getServices).flatMap(Set::stream).map(Provider.Service::getAlgorithm).collect(Collectors.toList());
            throw new CloudbreakServiceException("Hashing algorithm, " + HASH_ALGORITHM + " not supported. Supported algorithms: " + supportedAlgorithms, e);
        }
    }

    private String encode(byte[] data) {
        Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(data);
    }

    private CustomerEncryptionKey rsaEncryptedKey(String encryptionKey) {
        String pemPublicKey = getGooglePublicKey();

        PublicKey publicKey = readPublicKeyFromCertificate(pemPublicKey.getBytes(StandardCharsets.UTF_8));
        byte[] rsaWrapped = encrypt(publicKey, getEncryptionKeyBytes(encryptionKey));

        CustomerEncryptionKey customerEncryptionKey = new CustomerEncryptionKey();
        customerEncryptionKey.setSha256(encode(rsaWrapped));
        return customerEncryptionKey;
    }

    private byte[] encrypt(PublicKey publicKey, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(RSA_CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Failed to encrypt provided key, with Google public key. [{}] algorithm not supported.", RSA_CIPHER_TRANSFORMATION);
            throw new CloudbreakServiceException("RSA algorithm not supported.", e);
        } catch (NoSuchPaddingException e) {
            LOGGER.error("Failed to encrypt provided key, with Google public key. [{}] padding not supported.", RSA_CIPHER_TRANSFORMATION);
            throw new CloudbreakServiceException("OAEPWithSHA-1AndMGF1Padding padding not supported.", e);
        } catch (InvalidKeyException e) {
            LOGGER.info("Failed to encrypt provided key, with Google public key. Public key invalid: [{}].", publicKey);
            throw new CloudbreakServiceException("Invalid public key.", e);
        } catch (IllegalBlockSizeException e) {
            LOGGER.info("Failed to encrypt provided key, with Google public key. Illegal block size: [{}].", e.getMessage());
            throw new CloudbreakServiceException("Failed to encrypt key: illegal block size.", e);
        } catch (BadPaddingException e) {
            LOGGER.info("Failed to encrypt provided key, with Google public key. Bad padding: [{}].", e.getMessage());
            throw new CloudbreakServiceException("Failed to encrypt key: bad padding", e);
        }
    }

    private PublicKey readPublicKeyFromCertificate(byte[] keyBytes) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance(CERTIFICATE_ENCODING);
            X509Certificate certificate = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(keyBytes));
            return certificate.getPublicKey();
        } catch (CertificateException e) {
            throw new CloudbreakServiceException("Failed to get public key from certificate", e);
        }
    }

    private String getGooglePublicKey() {
        Client client = RestClientUtil.get();
        WebTarget target = createWebTarget(client);
        try (Response response = target.request().get()) {
            if (!Family.SUCCESSFUL.equals(response.getStatusInfo().getFamily())) {
                throw new CloudbreakServiceException("GET request to [" + googlePublicCertUrl + "] failed with status code: " + response.getStatus());
            }

            return response.readEntity(String.class);
        } catch (ProcessingException e) {
            throw new CloudbreakServiceException("Failed to connect to URI: " + googlePublicCertUrl, e);
        }
    }

    private WebTarget createWebTarget(Client client) {
        try {
            return client.target(new URI(googlePublicCertUrl));
        } catch (URISyntaxException e) {
            throw new CloudbreakServiceException("Invalid Google certification URI provided: " + googlePublicCertUrl, e);
        }
    }
}
