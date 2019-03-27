package com.sequenceiq.it.cloudbreak.newway.util;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;

public class AmbariUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariUtil.class);

    private AmbariUtil() {

    }

    public static StackTestDto checkAmbariUser(TestContext testContext, StackTestDto stackTestDto, CloudbreakClient cloudbreakClient) {
        String ambariIp = stackTestDto.getResponse().getCluster().getServerIp();
        String ambariPort = "8080";
        String ambariUser = stackTestDto.getRequest().getCluster().getUserName();
        String ambariPassword = stackTestDto.getRequest().getCluster().getPassword();
        AmbariClient ambariClient = new AmbariClient(ambariIp, ambariPort, ambariUser, ambariPassword);
        String userDetails = String.valueOf(ambariClient.getUser("teszt"));

        validateUserResponse(userDetails);
        return stackTestDto;
    }

    private static void validateUserResponse(String userDetails) {
        Pattern pattern = Pattern.compile("(?=.*active=true)(?=.*display_name=teszt)");
        if (Strings.isNullOrEmpty(userDetails)) {
            LOGGER.error("Requested user does not exist: " + userDetails);
            throw new TestFailException("Requested user is not exist");
        } else if (!pattern.matcher(userDetails).find()) {
            LOGGER.error("Requested user details are not valid: {}", userDetails);
            throw new TestFailException("Requested user details is not a valid json");
        }
    }
}
