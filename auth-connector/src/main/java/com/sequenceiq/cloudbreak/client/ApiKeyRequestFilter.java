package com.sequenceiq.cloudbreak.client;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable.ED_25519_CURVE_SPEC;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;

public class ApiKeyRequestFilter implements ClientRequestFilter {

    private static final String AUTH_METHOD = "ed25519v1";

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyRequestFilter.class);

    private static final String X_ALTUS_DATE = "x-altus-date";

    private static final String X_ALTUS_AUTH = "x-altus-auth";

    private String accessKey;

    private String secretKey;

    public ApiKeyRequestFilter(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        MultivaluedMap<String, Object> headers = requestContext.getHeaders();
        String dateStringForAltus = RFC_1123_DATE_TIME.format(OffsetDateTime.now(ZoneOffset.UTC));
        if (headers.get("Content-Type") == null) {
            headers.add("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        }
        if (headers.get("Content-Type").size() == 0) {
            throw new BadRequestException("Content-Type header is empty");
        }
        headers.add(X_ALTUS_DATE, dateStringForAltus);
        headers.add(X_ALTUS_AUTH, authHeader(accessKey, secretKey, requestContext.getMethod(), headers.get("Content-Type").get(0).toString(),
                requestContext.getUri().getPath(), dateStringForAltus));
    }

    private String authHeader(String accessKeyID, String privateKey, String contentType, String method, String path, String date) {
        return urlsafeMeta(accessKeyID) + "." + urlsafeSignature(privateKey, contentType, method, path, date);
    }

    private String urlsafeMeta(String accessKeyID) {
        String metadata = JsonUtil.writeValueAsStringSilent(new AccessKeyAuthMethod(accessKeyID, AUTH_METHOD));
        return new String(Base64.getUrlEncoder().encode(metadata.getBytes(StandardCharsets.UTF_8)));
    }

    private String urlsafeSignature(String seedBase64, String method, String contentType, String path, String date) {
        byte[] seed = Base64.getDecoder().decode(seedBase64);
        EdDSAPrivateKeySpec privKeySpec = new EdDSAPrivateKeySpec(seed, ED_25519_CURVE_SPEC);
        PrivateKey privateKey = new EdDSAPrivateKey(privKeySpec);
        try {
            Signature sgr = new EdDSAEngine(MessageDigest.getInstance("SHA-512"));
            sgr.initSign(privateKey);
            String messageToSign = method + "\n" + contentType + "\n" + date + "\n" + path + "\n" + AUTH_METHOD;
            LOGGER.info("Message to sign: \n'{}'", messageToSign);
            sgr.update(messageToSign.getBytes(StandardCharsets.UTF_8));
            return new String(Base64.getUrlEncoder().encode(sgr.sign()), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Can not find SHA-512", e);
            throw new IllegalStateException("Can not find SHA-512", e);
        } catch (InvalidKeyException e) {
            LOGGER.error("Invalid private key for signing", e);
            throw new IllegalArgumentException("Invalid private key for signing", e);
        } catch (SignatureException e) {
            LOGGER.error("Signing failed", e);
            throw new IllegalArgumentException("Signing failed", e);
        }
    }
}
