package com.sequenceiq.cloudbreak.validation;

import java.util.Map;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.client.RestClientUtil;

public class ImageCatalogValidator implements ConstraintValidator<ValidImageCatalog, String> {

    private static final Client CLIENT = RestClientUtil.get();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public void initialize(ValidImageCatalog constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || !value.startsWith("http")) {
            return false;
        }
        try {
            WebTarget target = CLIENT.target(value);
            try (Response response = target.request().get()) {
                if (response.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
                    String responseContent = response.readEntity(String.class);
                    OBJECT_MAPPER.readValue(responseContent, CloudbreakImageCatalog.class);
                    return true;
                }
            }
        } catch (Throwable ignore) {
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