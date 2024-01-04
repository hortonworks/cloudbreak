package com.sequenceiq.cloudbreak.validation;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.ws.rs.core.Response.Status.Family;
import jakarta.ws.rs.core.Response.StatusType;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.helper.HttpHelper;
import com.sequenceiq.common.api.util.ValidatorUtil;

@Component
public class ImageCatalogValidator implements ConstraintValidator<ValidImageCatalog, String> {

    public static final String FAILED_TO_GET_BY_FAMILY_TYPE = "Failed to get response by the specified URL '%s' due to: '%s'!";

    public static final String FAILED_TO_GET_WITH_EXCEPTION = "Failed to get response by the specified URL '%s'!";

    public static final String INVALID_JSON_IN_RESPONSE = "The file on the specified URL couldn't be parsed as JSON!";

    public static final String INVALID_JSON_STRUCTURE_IN_RESPONSE = "The JSON on the specified URL does not match structure expected for an Image Catalog!";

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageCatalogValidator.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    private HttpContentSizeValidator httpContentSizeValidator;

    @Inject
    private HttpHelper httpHelper;

    @Override
    public void initialize(ValidImageCatalog constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        LOGGER.info("Image Catalog validation was called: {}", value);
        try {
            if (value == null || !httpContentSizeValidator.isValid(value, context)) {
                return false;
            }
            Pair<StatusType, String> content = httpHelper.getContent(value);
            if (content.getKey().getFamily().equals(Family.SUCCESSFUL)) {
                return imageCatalogParsable(context, content.getValue());
            }
            String msg = String.format(FAILED_TO_GET_BY_FAMILY_TYPE, value, content.getKey().getReasonPhrase());
            ValidatorUtil.addConstraintViolation(context, msg);
        } catch (Throwable throwable) {
            ValidatorUtil.addConstraintViolation(context, String.format(FAILED_TO_GET_WITH_EXCEPTION, value));
            LOGGER.debug("Failed to validate the specified image catalog URL: " + value, throwable);
        }
        return false;
    }

    private boolean imageCatalogParsable(ConstraintValidatorContext context, String responseContent) throws java.io.IOException {
        try {
            OBJECT_MAPPER.readValue(responseContent, CloudbreakImageCatalog.class);
            return true;
        } catch (JsonParseException jPE) {
            ValidatorUtil.addConstraintViolation(context, INVALID_JSON_IN_RESPONSE);
        } catch (JsonMappingException jME) {
            ValidatorUtil.addConstraintViolation(context, INVALID_JSON_STRUCTURE_IN_RESPONSE);
        }
        return false;
    }

    public static class CloudbreakImageCatalog {

        private final Map<String, Object> images;

        private final Map<String, Object> versions;

        @JsonCreator
        public CloudbreakImageCatalog(
                @JsonProperty(value = "images", required = true) Map<String, Object> images,
                @JsonProperty(value = "versions") Map<String, Object> versions) {
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
