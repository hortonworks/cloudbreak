package com.sequenceiq.cloudbreak.validation;

import java.util.Map;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.client.RestClientUtil;

public class ImageCatalogValidator implements ConstraintValidator<ValidImageCatalog, String> {

    public static final String INVALID_URL_MSG = "The value should be a valid URL and start with 'http(s)'!";

    public static final String FAILED_TO_GET_BY_FAMILY_TYPE = "Failed to get response by the specified URL '%s' due to: '%s'!";

    public static final String FAILED_TO_GET_WITH_EXCEPTION = "Failed to get response by the specified URL!";

    public static final String INVALID_JSON_IN_RESPONSE = "The file on the specified URL couldn't be parsed as JSON!";

    public static final String INVALID_JSON_STRUCTURE_IN_RESPONSE = "The JSON on the specified URL does not match structure expected for an Image Catalog!";

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogValidator.class);

    private static final Client CLIENT = RestClientUtil.get();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void initialize(ValidImageCatalog constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || !value.startsWith("http")) {
            context.buildConstraintViolationWithTemplate(INVALID_URL_MSG).addConstraintViolation();
            return false;
        }
        try {
            WebTarget target = CLIENT.target(value);
            Response response = target.request().get();
            StatusType responseStatusInfo = response.getStatusInfo();
            if (responseStatusInfo.getFamily().equals(Family.SUCCESSFUL)) {
                String responseContent = response.readEntity(String.class);
                if (imageCatalogParsable(context, responseContent)) {
                    return true;
                }
            } else {
                String msg = String.format(FAILED_TO_GET_BY_FAMILY_TYPE, value, responseStatusInfo.getReasonPhrase());
                context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
            }
        } catch (Throwable throwable) {
            context.buildConstraintViolationWithTemplate(FAILED_TO_GET_WITH_EXCEPTION).addConstraintViolation();
            LOGGER.debug("Failed to validate the specified image catalog URL: " + value, throwable);
        }
        return false;
    }

    private boolean imageCatalogParsable(ConstraintValidatorContext context, String responseContent) throws java.io.IOException {
        try {
            OBJECT_MAPPER.readValue(responseContent, CloudbreakImageCatalog.class);
            return true;
        } catch (JsonParseException jPE) {
            context.buildConstraintViolationWithTemplate(INVALID_JSON_IN_RESPONSE).addConstraintViolation();
        } catch (JsonMappingException jME) {
            context.buildConstraintViolationWithTemplate(INVALID_JSON_STRUCTURE_IN_RESPONSE)
                    .addConstraintViolation();
        }
        return false;
    }

    public static class CloudbreakImageCatalog {

        private final Map<String, Object> images;

        private final Map<String, Object> versions;

        @JsonCreator
        public CloudbreakImageCatalog(
                @JsonProperty(value = "images", required = true) Map<String, Object> images,
                @JsonProperty(value = "versions", required = true) Map<String, Object> versions) {
            this.images = images;
            this.versions = versions;
        }

        public Map<String, Object> getImages() {
            return images;
        }

        public Map<String, Object> getVersions() {
            return versions;
        }
    }
}