package com.sequenceiq.cloudbreak.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseV4Base;
import com.sequenceiq.cloudbreak.util.DatabaseCommon;

public class RDSConfigJsonValidator implements ConstraintValidator<ValidRDSConfigJson, DatabaseV4Base> {

    private static final int MIN_NAME_LENGTH = 4;

    private static final int MAX_NAME_LENGTH = 50;

    private static final int MIN_TYPE_LENGTH = 3;

    private static final int MAX_TYPE_LENGTH = 16;

    private static final int MAX_CONNECTOR_JAR_URL_LENGTH = 150;

    private String failMessage = "";

    @Override
    public void initialize(ValidRDSConfigJson constraintAnnotation) {
    }

    @Override
    public boolean isValid(DatabaseV4Base value, ConstraintValidatorContext context) {
        if (!isConnectionUrlValid(value.getConnectionURL())
                || !isNameValid(value.getName())
                || !isTypeValid(value.getType())
                || !isConnectorJarUrlValid(value.getConnectorJarUrl())) {
            ValidatorUtil.addConstraintViolation(context, failMessage, "status");
            return false;
        }
        return true;
    }

    private boolean isConnectionUrlValid(String url) {
        if (!url.matches(DatabaseCommon.JDBC_REGEX)) {
            if (!isSupportedDatabseType(url)) {
                failMessage = "Unsupported database type. Supported databases: PostgreSQL, Oracle, MySQL.";
            } else if (!isValidSeparator(url)) {
                failMessage = "Unknown separator. Valid formation: jdbc:postgresql://host:1234/tablename or jdbc:oracle:thin:@host:1234:tablename";
            } else if (!isValidHostPortAndDatabaseName(url)) {
                failMessage = "Wrong host, port or table name. Valid form: host:1234/tablename or host:1234:tablename";
            } else {
                failMessage = "Unknown error in JDBC URL.";
            }
            return false;
        }
        return true;
    }

    private boolean isNameValid(String name) {
        if (!name.matches("(^[a-z][-a-z0-9]*[a-z0-9]$)")) {
            failMessage = "The name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character";
            return false;
        } else if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            failMessage = "The length of the name has to be in range of 4 to 50";
            return false;
        }
        return true;
    }

    private boolean isTypeValid(String type) {
        if (!type.matches("(^[a-zA-Z_][-a-zA-Z0-9_]*[a-zA-Z0-9_]$)")) {
            failMessage = "The type can only contain alphanumeric characters and hyphens and has start with an alphanumeric character. "
                    + "The length of the name has to be in range of 3 to 12";
            return false;
        } else if (type.length() < MIN_TYPE_LENGTH || type.length() > MAX_TYPE_LENGTH) {
            failMessage = "The length of the type has to be in range of 3 to 12";
            return false;
        }
        return true;
    }

    private boolean isConnectorJarUrlValid(String connectorJarUrl) {
        if (!StringUtils.isEmpty(connectorJarUrl) && !connectorJarUrl.matches("^http[s]?://[\\w-/?=+&:,#.]*")) {
            failMessage = "The URL must be proper and valid!";
            return false;
        } else if (!StringUtils.isEmpty(connectorJarUrl) && connectorJarUrl.length() > MAX_CONNECTOR_JAR_URL_LENGTH) {
            failMessage = "The length of the connectorJarUrl has to be in range of 0 to 150";
            return false;
        }
        return true;
    }

    private boolean isValidSeparator(String connectionURL) {
        return connectionURL.indexOf("://") > 0 || connectionURL.indexOf(":@") > 0;
    }

    private boolean isValidHostPortAndDatabaseName(String connectionURL) {
        return DatabaseCommon.getHostPortAndDatabaseName(connectionURL).isPresent();
    }

    private boolean isSupportedDatabseType(String connectionURL) {
        return DatabaseCommon.getDatabaseType(connectionURL).isPresent();
    }

}
