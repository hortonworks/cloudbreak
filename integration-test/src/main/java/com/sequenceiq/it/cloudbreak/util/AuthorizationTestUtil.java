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
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
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

    public static String environmentPattern(TestContext testContext) {
        return String.format("[\\[]name: %s, crn: crn:cdp:environments:us-west-1:.*:environment:.*[]]\\.",
                testContext.get(EnvironmentTestDto.class).getName());
    }

    public static String environmentDatahubPattern(TestContext testContext) {
        return "environment[(]s[)] [\\[]crn: crn:cdp:environments:us-west-1:.*:environment:.*";
    }

    public static String environmentFreeIpaPattern(TestContext testContext) {
        return "[\\[]crn: crn:cdp:environments:us-west-1:.*:environment:.*[]]\\.";
    }

    public static String datalakePattern(TestContext testContext) {
        return datalakePattern(testContext.get(SdxInternalTestDto.class).getName());
    }

    public static String datalakePattern(String name) {
        return String.format("datalake[(]s[)] [\\[]name: %s, crn: crn:cdp:datalake:us-west-1:.*:datalake:.*\\.", name);
    }

    public static String datahubPattern(TestContext testContext) {
        return String.format("cluster[(]s[)] [\\[]name: %s, crn: crn:cdp:datahub:us-west-1:.*:cluster:.*",
                testContext.get(DistroXTestDto.class).getName());
    }

    public static String datahubRecipePattern(TestContext testContext) {
        return datahubRecipePattern(testContext.get(RecipeTestDto.class).getName());
    }

    public static String datahubRecipePattern(String recipeName) {
        return String.format("[\\[]name: %s, crn: crn:cdp:datahub:us-west-1:.*:recipe:.*[]]\\.", recipeName);
    }

}
