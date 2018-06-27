package com.sequenceiq.it.cloudbreak.newway;

import java.util.Optional;

import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;

public class RDSConfigRequestDataCollector {

    private static final String CONFIG_NAME_KEY = "NN_%s_DB_%s_CONFIG_NAME";

    private static final String USER_NAME_KEY = "NN_%s_DB_%s_USER_NAME";

    private static final String PASSWORD_KEY = "NN_%s_DB_%s_PASSWORD";

    private static final String CONNECTION_URL_KEY = "NN_%s_DB_%s_CONNECTION_URL";

    private RDSConfigRequestDataCollector() {
    }

    public static RDSConfigRequest createRdsRequestWithProperties(TestParameter testParameter, RdsType infix, String providerShortcut) {
        RDSConfigRequest request = new RDSConfigRequest();
        request.setName(getParam(CONFIG_NAME_KEY, providerShortcut, infix.name(), testParameter));
        request.setConnectionUserName(getParam(USER_NAME_KEY, providerShortcut, infix.name(), testParameter));
        request.setConnectionPassword(getParam(PASSWORD_KEY, providerShortcut, infix.name(), testParameter));
        request.setConnectionURL(getParam(CONNECTION_URL_KEY, providerShortcut, infix.name(), testParameter));
        request.setType(infix.name());
        return request;
    }

    private static String getParam(String key, String providerShortcut, String infix, TestParameter testParameter) {
        String combinedKey = createReplacedInfixContent(key, providerShortcut.toUpperCase(), infix);
        return Optional.ofNullable(testParameter.get(combinedKey)).orElseThrow(() -> new MissingExpectedParameterException(combinedKey));
    }

    private static String createReplacedInfixContent(String content, String providerShortcut, String infix) {
        return String.format(content, providerShortcut, infix);
    }
}
