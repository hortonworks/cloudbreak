package com.sequenceiq.cloudbreak.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigJson;

public class RDSConfigJsonValidator implements ConstraintValidator<ValidRDSConfigJson, RDSConfigJson> {

    private static final int MIN_NAME_LENGTH = 4;

    private static final int MAX_NAME_LENGTH = 50;

    private static final int MIN_TYPE_LENGTH = 3;

    private static final int MAX_TYPE_LENGTH = 12;

    private static final int MAX_CONNECTOR_JAR_URL_LENGTH = 150;

    private static final int HOST_GROUP_INDEX = 1;

    private static final int HOST_PORT_INDEX = 2;

    private static final int DATABASE_GROUP_INDEX = 3;

    private String failMessage = "";

    @Override
    public void initialize(ValidRDSConfigJson constraintAnnotation) {
    }

    @Override
    public boolean isValid(RDSConfigJson value, ConstraintValidatorContext context) {
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
        if (!url.matches("^(?:jdbc:(?:oracle|mysql|postgresql)(:(?:.*))?):(@|//)(?:.*?):(?:\\d*)[:/](?:\\w+)(?:-*\\w*)*(?:[?](?:[^=&]*=[^&=]*&?)*)?")) {
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
        String splitter;
        splitter = connectionURL.indexOf("//") > 0 ? "//" : "@";
        String[] split = connectionURL.split(splitter);

        String withoutJDBCPrefix = split[split.length - 1];

        Pattern compile = Pattern.compile("^(.*?):(\\d*)[:/]?(\\w+)?");
        Matcher matcher = compile.matcher(withoutJDBCPrefix);

        return matcher.find() && matcher.groupCount() == DATABASE_GROUP_INDEX
                && !StringUtils.isEmpty(matcher.group(HOST_GROUP_INDEX))
                && !StringUtils.isEmpty(matcher.group(HOST_PORT_INDEX))
                && !StringUtils.isEmpty(matcher.group(DATABASE_GROUP_INDEX));
    }

    private boolean isSupportedDatabseType(String connectionURL) {
        return connectionURL.matches("jdbc:(oracle|mysql|postgresql).*");
    }

}
