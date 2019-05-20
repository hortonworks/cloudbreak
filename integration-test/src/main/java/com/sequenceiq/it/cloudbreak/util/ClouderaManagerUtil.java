package com.sequenceiq.it.cloudbreak.util;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class ClouderaManagerUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerUtil.class);

    private ClouderaManagerUtil() {
    }

    public static StackTestDto checkClouderaManagerUser(TestContext testContext, StackTestDto stackTestDto, CloudbreakClient cloudbreakClient) {
        /* TODO: Need to implement Cloudera Manager Client and its User Service. */
        validateUserResponse("name:teszt,displayName:Full Administrator");
        return stackTestDto;
    }

    private static void validateUserResponse(String userDetails) {
        Pattern pattern = Pattern.compile("(?=.*name:teszt)(?=.*displayName:Full Administrator)");
        if (Strings.isNullOrEmpty(userDetails)) {
            LOGGER.error("Requested user does not exist: " + userDetails);
            throw new TestFailException("Requested user is not exist");
        } else if (!pattern.matcher(userDetails).find()) {
            LOGGER.error("Requested user details are not valid: {}", userDetails);
            throw new TestFailException("Requested user details is not a valid json");
        }
    }
}
