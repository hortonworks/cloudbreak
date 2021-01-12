package com.sequenceiq.it.cloudbreak.util;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.info.model.RightV4;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakActor;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.UtilTestClient;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.util.CheckResourceRightTestDto;
import com.sequenceiq.it.cloudbreak.dto.util.CheckRightTestDto;

@Component
public class AuthorizationTestUtil {

    @Inject
    private CloudbreakActor cloudbreakActor;

    public void testCheckRightUtil(TestContext testContext, String testUmsUser, Assertion<CheckRightTestDto, CloudbreakClient> assertion,
            List<RightV4> rightsToCheck, UtilTestClient utilTestClient) {
        testContext
                .given(CheckRightTestDto.class).withRightsToCheck(rightsToCheck)
                .when(utilTestClient.checkRight(), RunningParameter.who(cloudbreakActor.useRealUmsUser(testUmsUser)))
                .then(assertion);
    }

    public void testCheckResourceRightUtil(TestContext testContext, String testUmsUser, Assertion<CheckResourceRightTestDto, CloudbreakClient> assertion,
            Map<String, List<RightV4>> rightsToCheck, UtilTestClient utilTestClient) {
        testContext
                .given(CheckResourceRightTestDto.class).withRightsToCheck(rightsToCheck)
                .when(utilTestClient.checkResourceRight(), RunningParameter.who(cloudbreakActor.useRealUmsUser(testUmsUser)))
                .then(assertion);
    }
}
