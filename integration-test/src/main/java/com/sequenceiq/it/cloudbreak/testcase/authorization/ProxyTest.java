package com.sequenceiq.it.cloudbreak.testcase.authorization;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil.proxyConfigPattern;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.environment.api.v1.proxy.model.response.ProxyResponse;
import com.sequenceiq.it.cloudbreak.client.ProxyTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.config.user.TestUserSelectors;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class ProxyTest extends AbstractIntegrationTest {

    @Inject
    private ProxyTestClient proxyTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getTestUsers().setSelector(TestUserSelectors.UMS_ONLY);

        testContext.as(AuthUserKeys.USER_ACCOUNT_ADMIN);
        testContext.as(AuthUserKeys.USER_ENV_CREATOR_A);
        testContext.as(AuthUserKeys.USER_ENV_CREATOR_B);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid proxy",
            when = "a valid proxy create request sent",
            then = "only owner of the proxy and account admin should be able to describe or delete it")
    public void testProxyActions(TestContext testContext) {
        Map<String, Pair<Crn, String>> proxyMap = Maps.newHashMap();
        createTest(testContext, AuthUserKeys.USER_ENV_CREATOR_A, AuthUserKeys.USER_ENV_CREATOR_B, proxyMap);
        createTest(testContext, AuthUserKeys.USER_ENV_CREATOR_B, AuthUserKeys.USER_ENV_CREATOR_A, proxyMap);
        listTest(testContext, AuthUserKeys.USER_ENV_CREATOR_A, proxyMap);
        listTest(testContext, AuthUserKeys.USER_ENV_CREATOR_B, proxyMap);
        deleteTest(testContext, AuthUserKeys.USER_ENV_CREATOR_A, proxyMap);
        deleteTest(testContext, AuthUserKeys.USER_ENV_CREATOR_B, proxyMap);
    }

    private void createTest(TestContext testContext, String owner, String other, Map<String, Pair<Crn, String>> proxyMap) {
        testContext.given(ProxyTestDto.class)
                .withGeneratedName()
                .when(proxyTestClient.create(), RunningParameter.who(testContext.getTestUsers().getUserByLabel(AuthUserKeys.USER_ACCOUNT_ADMIN)))
                .then((context, dto, client) -> {
                    Pair<Crn, String> crnNamePair = Pair.of(Crn.safeFromString(dto.getCrn()), dto.getName());
                    proxyMap.put(owner, crnNamePair);
                    return dto;
                })
                .given(UmsTestDto.class)
                .assignTargetByCrn(proxyMap.get(owner).getKey().toString())
                .withOwner()
                .when(umsTestClient.assignResourceRole(owner, regionAwareInternalCrnGeneratorFactory))
                .given(ProxyTestDto.class)
                .withName(proxyMap.get(owner).getValue())
                .when(proxyTestClient.get(), RunningParameter.who(testContext.getTestUsers().getUserByLabel((owner))))
                .then((context, dto, client) -> {
                    Assertions.assertThat(dto.getResponse().getName()).isEqualTo(context.get(ProxyTestDto.class).getName());
                    return dto;
                })
                .whenException(proxyTestClient.get(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'environments/useSharedResource' right on proxyConfig " +
                                proxyConfigPattern(testContext.get(ProxyTestDto.class).getName())).withWho(testContext.getTestUsers().getUserByLabel((other))))
                .whenException(proxyTestClient.delete(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'environments/deleteProxyConfig' right on proxyConfig " +
                                proxyConfigPattern(testContext.get(ProxyTestDto.class).getName())).withWho(testContext.getTestUsers().getUserByLabel((other))))
                .validate();
    }

    private void deleteTest(TestContext testContext, String creator, Map<String, Pair<Crn, String>> proxyMap) {
        testContext.given(ProxyTestDto.class)
                .withName(proxyMap.get(creator).getValue())
                .when(proxyTestClient.delete(), RunningParameter.who(testContext.getTestUsers().getUserByLabel(creator)))
                .validate();
    }

    private void listTest(TestContext testContext, String creator, Map<String, Pair<Crn, String>> proxyMap) {
        testContext.given(ProxyTestDto.class)
                .when(proxyTestClient.list(), RunningParameter.who(testContext.getTestUsers().getUserByLabel((creator))))
                .then((context, dto, client) -> envCreatorListAssertion(creator, proxyMap, dto))
                .when(proxyTestClient.list(), RunningParameter.who(testContext.getTestUsers().getUserByLabel((AuthUserKeys.USER_ACCOUNT_ADMIN))))
                .then((context, dto, client) -> adminListAssertion(proxyMap, dto))
                .validate();
    }

    private ProxyTestDto envCreatorListAssertion(String creator, Map<String, Pair<Crn, String>> proxyMap, ProxyTestDto dto) {
        Set<String> foreignCrns = collectCrnsFromMapByFilter(proxyMap, entry -> !StringUtils.equals(entry.getKey(), creator));
        Set<String> ownCrns = collectCrnsFromMapByFilter(proxyMap, entry -> StringUtils.equals(entry.getKey(), creator));
        Set<String> crnsInResponse = dto.getResponses().stream().map(ProxyResponse::getCrn).collect(Collectors.toSet());
        Assertions.assertThat(Sets.intersection(crnsInResponse, foreignCrns).size()).isEqualTo(0);
        Assertions.assertThat(Sets.intersection(crnsInResponse, ownCrns).size()).isEqualTo(ownCrns.size());
        return dto;
    }

    private ProxyTestDto adminListAssertion(Map<String, Pair<Crn, String>> proxyMap, ProxyTestDto dto) {
        Set<String> allCrns = dto.getResponses().stream().map(ProxyResponse::getCrn).collect(Collectors.toSet());
        Set<String> crnsDuringTest = collectCrnsFromMapByFilter(proxyMap, entry -> true);
        Assertions.assertThat(Sets.intersection(allCrns, crnsDuringTest).size()).isEqualTo(crnsDuringTest.size());
        return dto;
    }

    private Set<String> collectCrnsFromMapByFilter(Map<String, Pair<Crn, String>> proxyMap,
            Predicate<Map.Entry<String, Pair<Crn, String>>> filter) {
        return proxyMap.entrySet().stream()
                .filter(filter)
                .map(entry -> entry.getValue().getKey().toString())
                .collect(Collectors.toSet());
    }

}
