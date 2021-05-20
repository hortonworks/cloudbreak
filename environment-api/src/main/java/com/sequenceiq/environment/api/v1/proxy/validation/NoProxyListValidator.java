package com.sequenceiq.environment.api.v1.proxy.validation;

import java.util.Arrays;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

public class NoProxyListValidator implements ConstraintValidator<ValidNoProxyList, String> {

    private static final Pattern IP_CHARS = Pattern.compile("[\\d.:]+");

    private static final Pattern IP_PORT =
            Pattern.compile("^((([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5]))" +
                    "(:([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]))?$");

    private static final Pattern HOST_PORT =
            Pattern.compile("^((([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-])*[A-Za-z0-9])" +
                    "(:([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]))?$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isBlank(value)) {
            return true;
        }
        String[] elements = value.split(",");
        return Arrays.stream(elements)
                .map(StringUtils::deleteWhitespace)
                .allMatch(this::validHostOrIpAndOptionalPort);
    }

    private boolean validHostOrIpAndOptionalPort(String element) {
        if (IP_CHARS.matcher(element).matches()) {
            return IP_PORT.matcher(element).matches();
        }
        return HOST_PORT.matcher(element).matches();
    }
}
