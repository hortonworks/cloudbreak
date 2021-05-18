package com.sequenceiq.cloudbreak.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.common.json.Json;

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
            if (!response.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
                throw handleUnexpectedError(response);
            }
        }
        try {
            response.bufferEntity();
            return response.readEntity(clazz);
        } catch (Exception e) {
            LOGGER.warn("Couldn't parse response: [{}]", response, e);
            throw handleUnexpectedError(response);
        } finally {
            LOGGER.debug("Original salt response: {}", AnonymizerUtil.anonymize(response.readEntity(String.class)));
            response.close();
        }
    }

    private static WebApplicationException handleUnexpectedError(Response response) {
        String textResponse = response.readEntity(String.class);
        LOGGER.debug("Received error: {}", textResponse);
        String errorMessage = transformErrorMessage(textResponse);
        String errormsg = "Status: " + response.getStatusInfo().getStatusCode() + ' ' + response.getStatusInfo().getReasonPhrase()
                + " Response: " + errorMessage;
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
