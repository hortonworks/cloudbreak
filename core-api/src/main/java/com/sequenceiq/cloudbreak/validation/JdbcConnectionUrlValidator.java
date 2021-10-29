package com.sequenceiq.cloudbreak.validation;

import com.sequenceiq.cloudbreak.common.database.DatabaseCommon;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.common.api.util.ValidatorUtil;

public class JdbcConnectionUrlValidator implements ConstraintValidator<ValidJdbcConnectionUrl, String> {

    private boolean databaseExpected;

    @Override
    public void initialize(ValidJdbcConnectionUrl constraintAnnotation) {
        databaseExpected = constraintAnnotation.databaseExpected();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        if (value == null) {
            return true;
        }
        if (value.isEmpty()) {
            ValidatorUtil.addConstraintViolation(context, "JDBC connection URL may not be empty");
            return false;
        }

        try {
            DatabaseCommon.JdbcConnectionUrlFields fields = new DatabaseCommon().parseJdbcConnectionUrl(value);
            if (databaseExpected != fields.getDatabase().isPresent()) {
                ValidatorUtil.addConstraintViolation(context, "JDBC connection URL must " + (databaseExpected ? "" : "not ") + "have a database name");
                return false;
            }
        } catch (IllegalArgumentException e) {
            ValidatorUtil.addConstraintViolation(context, "JDBC connection URL is not valid");
            return false;
        }

        return true;
    }

}
