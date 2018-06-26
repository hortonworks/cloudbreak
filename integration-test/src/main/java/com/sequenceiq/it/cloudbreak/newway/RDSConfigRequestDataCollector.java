package com.sequenceiq.it.cloudbreak.newway;

import java.util.Optional;

import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;

public class RDSConfigRequestDataCollector {

    private static final String CONFIG_NAME_KEY = "NN_RDS%_CONFIG_NAME";

    private static final String USER_NAME_KEY = "NN_RDS%_USER_NAME";

    private static final String PASSWORD_KEY = "NN_RDS%_PASSWORD";

    private static final String CONNECTION_URL_KEY = "NN_RDS%_CONNECTION_URL";

    private static final String INFIX_REPLACE_CHAR = "%";

    private RDSConfigRequestDataCollector() {
    }

    public static RDSConfigRequest createRdsRequestWithProperties(TestParameter testParameter, RdsType infix) {
        RDSConfigRequest request = new RDSConfigRequest();
        request.setName(createReplacedInfixContent(getParam(CONFIG_NAME_KEY, infix.name(), testParameter), infix.name()));
        request.setConnectionUserName(getParam(USER_NAME_KEY, infix.name(), testParameter));
        request.setConnectionPassword(getParam(PASSWORD_KEY, infix.name(), testParameter));
        request.setConnectionURL(getParam(CONNECTION_URL_KEY, infix.name(), testParameter));
        request.setType(infix.name());
        return request;
    }

    private static String getParam(String key, String infix, TestParameter testParameter) {
        String combinedKey = createReplacedInfixContent(key, infix);
        return Optional.ofNullable(testParameter.get(combinedKey)).orElseThrow(() -> new MissingExpectedParameterException(combinedKey));
    }

    private static String createReplacedInfixContent(String content, String infix) {
        return content.replaceFirst(INFIX_REPLACE_CHAR, String.format("_%s", infix));
    }
}
