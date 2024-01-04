package com.sequenceiq.cloudbreak.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status.Family;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyError;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyErrorTypeReference;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyWebApplicationException;
import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

public class JaxRSUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(JaxRSUtil.class);

    private static final Map<String, String> ERROR_MESSAGE_MAP;

    static {
        try {
            String file = FileReaderUtils.readFileFromClasspath("messages/jaxrs-messages.yaml");
            ERROR_MESSAGE_MAP = new Yaml().load(file);
            LOGGER.info("JaxRS error message mapping loaded: {}", ERROR_MESSAGE_MAP);
        } catch (IOException e) {
            throw new RuntimeException("Can't load error message mapping", e);
        }
    }

    private JaxRSUtil() {
    }

    public static <T> T response(Response response, Class<T> clazz) {
        if (Family.SUCCESSFUL != response.getStatusInfo().getFamily()) {
            throw handleUnexpectedError(response);
        }
        if (!MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType())) {
            throw handleIncompatibleMediaType(response);
        }
        try {
            response.bufferEntity();
            return response.readEntity(clazz);
        } catch (Exception e) {
            LOGGER.warn("Couldn't parse response: [{}]", response, e);
            throw handleUnexpectedError(response);
        } finally {
            LOGGER.trace("Original salt response: {}", AnonymizerUtil.anonymize(response.readEntity(String.class)));
            response.close();
        }
    }

    private static WebApplicationException handleIncompatibleMediaType(Response response) {
        String textResponse = response.readEntity(String.class);
        MediaType mediaType = response.getMediaType();
        LOGGER.debug("Received response with incompatible media type: {}, response: {}", mediaType, textResponse);
        String errormsg = "Status: " + response.getStatusInfo().getStatusCode() + ' ' + response.getStatusInfo().getReasonPhrase()
                + " Media Type: " + mediaType
                + " Response: " + textResponse;
        return new WebApplicationException(errormsg);
    }

    private static WebApplicationException handleUnexpectedError(Response response) {
        String textResponse = response.readEntity(String.class);
        LOGGER.debug("Received error: {}", textResponse);
        String errorMessage = transformErrorMessage(textResponse);
        String errormsg = "Status: " + response.getStatusInfo().getStatusCode() + ' ' + response.getStatusInfo().getReasonPhrase()
                + " Response: " + errorMessage;

        try {
            if (JsonUtil.isValid(errorMessage)) {
                ClusterProxyError clusterProxyError = JsonUtil.jsonToType(errorMessage, ClusterProxyErrorTypeReference.get());
                if (clusterProxyError.getRetryable()) {
                    return new ClusterProxyWebApplicationException(errormsg, clusterProxyError);
                }
            }
        } catch (IllegalArgumentException e) {
            LOGGER.trace("Error message is not a json or not a cluster proxy error, thus we cannot parse it.", e);
        }
        return new WebApplicationException(errormsg);
    }

    private static String transformErrorMessage(String textResponse) {
        try {
            Json jsonResponse = new Json(textResponse);
            Map<String, Object> mapResponse = jsonResponse.getMap();
            Integer status = (Integer) mapResponse.get("status");
            String code = (String) mapResponse.get("code");
            if (status != null && StringUtils.isNotEmpty(code)) {
                return ERROR_MESSAGE_MAP.getOrDefault(generateErrorMessageMapKey(status, code), textResponse);
            } else {
                return textResponse;
            }
        } catch (RuntimeException ex) {
            return textResponse;
        }
    }

    private static String generateErrorMessageMapKey(Object... parts) {
        return Arrays.stream(parts).map(Object::toString).collect(Collectors.joining("-"));
    }
}
