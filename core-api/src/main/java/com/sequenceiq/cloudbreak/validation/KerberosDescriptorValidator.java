package com.sequenceiq.cloudbreak.validation;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class KerberosDescriptorValidator implements ConstraintValidator<ValidKerberosDescriptor, String> {

    private ObjectMapper objectMapper;

    private List<String> requiredFileds;

    @Override
    public void initialize(ValidKerberosDescriptor constraintAnnotation) {
        objectMapper = new ObjectMapper();
        requiredFileds = Arrays.asList("kdc_type", "kdc_hosts", "admin_server_host", "realm");
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        try {
            JsonNode json = objectMapper.readTree(value);
            return requiredFileds.stream().allMatch(k -> StringUtils.hasText(json.get("kerberos-env").get("properties").get(k).asText()));
        } catch (NullPointerException | IOException ignored) {
        }
        return false;
    }
}