package com.sequenceiq.cloudbreak.rotation.context.provider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.rotation.secret.SecretGenerator;

@Component
public class CMUserNameSecretGenerator implements SecretGenerator {

    public static final String USER_PREFIX_MAP_KEY = "USER_PREFIX";

    private static final String DATETIMEFORMAT = "ddMMyyHHmmss";

    private static final String DEFAULT_USER_PREFIX = "CB";

    @Override
    public String generate(Map<String, Object> arguments) {
        String userPrefix = DEFAULT_USER_PREFIX;
        if (arguments.containsKey(USER_PREFIX_MAP_KEY) && arguments.get(USER_PREFIX_MAP_KEY).getClass().isAssignableFrom(String.class)) {
            userPrefix = (String) arguments.get(USER_PREFIX_MAP_KEY);
        }
        return userPrefix + new SimpleDateFormat(DATETIMEFORMAT).format(new Date());
    }
}
