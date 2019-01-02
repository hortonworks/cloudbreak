package com.sequenceiq.cloudbreak.validation;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.codec.binary.Base64;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.AmbariKerberosDescriptor;

public class AmbariKerberosDescriptorValidator implements ConstraintValidator<ValidAmbariKerberosDescriptor, AmbariKerberosDescriptor> {

    private static final String BASE64_ENCODE_MESSAGE_FORMAT = "%s have to be base64 encoded";

    private ObjectMapper objectMapper;

    @Override
    public void initialize(ValidAmbariKerberosDescriptor constraintAnnotation) {
        objectMapper = new ObjectMapper();
    }

    @Override
    public boolean isValid(AmbariKerberosDescriptor value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        boolean result = true;
        if (!Base64.isBase64(value.getKrb5Conf())) {
            setErrorMessage(String.format(BASE64_ENCODE_MESSAGE_FORMAT, "Krb5Conf"), context);
            result = false;
        }
        if (!Base64.isBase64(value.getDescriptor())) {
            setErrorMessage(String.format(BASE64_ENCODE_MESSAGE_FORMAT, "Descriptor"), context);
            result = false;
        }
        String base64DecodedDescriptor = new String(Base64.decodeBase64(value.getDescriptor()));
        if (!isValidDescriptor(base64DecodedDescriptor)) {
            setErrorMessage("Descriptor must be a valid JSON with the required fields", context);
            result = false;
        }
        String base64DecodedKrb5Conf = new String(Base64.decodeBase64(value.getKrb5Conf()));
        if (!isValidJson(base64DecodedKrb5Conf)) {
            setErrorMessage("Krb5Conf must be a valid JSON", context);
            result = false;
        }
        return result;
    }

    private boolean isValidDescriptor(String value) {
        return isValidJson(value) && isValidDescriptorFields(value);
    }

    private boolean isValidDescriptorFields(String jsonString) {
        Optional<JsonNode> jsonNodeFromJsonString = getJsonNodeFromJsonString(jsonString, objectMapper);
        if (jsonNodeFromJsonString.isPresent()) {
            JsonNode json = jsonNodeFromJsonString.get();
            JsonNode kerberosEnvJsonNode = json.get("kerberos-env");
            List<String> requiredFieldsForDescriptor = Arrays.asList("kdc_type", "kdc_hosts", "admin_server_host", "realm");
            return kerberosEnvJsonNode != null && kerberosEnvJsonNode.get("properties") != null
                    && requiredFieldsForDescriptor.stream().allMatch(k -> {
                        JsonNode propertiesJsonNode = kerberosEnvJsonNode.get("properties");
                        return propertiesJsonNode.get(k) != null && StringUtils.hasText(propertiesJsonNode.get(k).asText());
                    });
        }
        return false;
    }

    private boolean isValidJson(String value) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        return getJsonNodeFromJsonString(value, objectMapper).isPresent();
    }

    private Optional<JsonNode> getJsonNodeFromJsonString(String value, ObjectMapper objectMapper) {
        try {
            JsonNode json = objectMapper.readTree(value);
            return Optional.of(json);
        } catch (IOException ignored) {
        }
        return Optional.empty();
    }

    private void setErrorMessage(String message, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
