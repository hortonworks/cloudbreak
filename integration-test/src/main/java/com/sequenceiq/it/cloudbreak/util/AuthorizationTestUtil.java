package com.sequenceiq.it.cloudbreak.util;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.base.RightV4;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.actor.Actor;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.CheckResourceRightTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.CheckRightTestDto;

public class AuthorizationTestUtil {

    private AuthorizationTestUtil() {

    }

    public static void testCheckRightUtil(TestContext testContext, String testUmsUser, Assertion<CheckRightTestDto, CloudbreakClient> assertion,
            List<RightV4> rightsToCheck, UtilTestClient utilTestClient) {
        testContext
                .given(CheckRightTestDto.class).withRightsToCheck(rightsToCheck)
                .when(utilTestClient.checkRight(), RunningParameter.who(Actor.useRealUmsUser(testUmsUser)))
                .then((context, dto, client) -> assertion.doAssertion(context, dto, client));
    }

    public static void testCheckResourceRightUtil(TestContext testContext, String testUmsUser, Assertion<CheckResourceRightTestDto, CloudbreakClient> assertion,
            Map<String, List<RightV4>> rightsToCheck, UtilTestClient utilTestClient) {
        testContext
                .given(CheckResourceRightTestDto.class).withRightsToCheck(rightsToCheck)
                .when(utilTestClient.checkResourceRight(), RunningParameter.who(Actor.useRealUmsUser(testUmsUser)))
                .then((context, dto, client) -> assertion.doAssertion(context, dto, client));
    }
}
