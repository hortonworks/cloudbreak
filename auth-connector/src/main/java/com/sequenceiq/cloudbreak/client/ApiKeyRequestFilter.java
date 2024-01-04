package com.sequenceiq.cloudbreak.client;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.common.base64.Base64Util;

public class ApiKeyRequestFilter implements ClientRequestFilter {

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
        headers.add(X_ALTUS_AUTH, authHeader(accessKey, secretKey, headers.get("Content-Type").get(0).toString(), requestContext.getMethod(),
                requestContext.getUri().toURL().getFile(), dateStringForAltus));
    }

    private String authHeader(String accessKeyID, String privateKey, String contentType, String method, String path, String date) {
        return getHeaderSigner(privateKey).authHeader(accessKeyID, privateKey, contentType, method, path, date);
    }

    private HeaderSigner getHeaderSigner(String privateKey) {
        try {
            Base64Util.decodeAsByteArray(privateKey);
            return new Ed25519HeaderSigner();
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Error during decoding v2 key format for singing the request header, trying v3 format.", e);
        }
        return new EcdsaHeaderSigner();
    }
}
