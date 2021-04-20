package com.sequenceiq.environment.environment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.tag.CentralTagUpdater;
import com.sequenceiq.cloudbreak.tag.DefaultApplicationTag;
import com.sequenceiq.cloudbreak.tag.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.tag.TagTemplateProcessor;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponse;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.tags.service.AccountTagService;
import com.sequenceiq.environment.tags.service.DefaultInternalAccountTagService;
import com.sequenceiq.environment.tags.v1.converter.AccountTagToAccountTagResponsesConverter;

@SpringBootTest
@ContextConfiguration(classes = {
        DefaultCostTaggingService.class,
        CentralTagUpdater.class,
        TagTemplateProcessor.class,
        Clock.class,
        EnvironmentTagProvider.class})
class EnvironmentTagProviderTest {

    private static final EnvironmentDto ENVIRONMENT = new EnvironmentDto();

    private static final String USERNAME = "username";

    private static final String USER_CRN = "crn:user";

    private static final String ENV_CRN = "crn:env";

    private static final String NETWORK_CRN = "crn:network";

    private static final String ACCOUNT_ID = "123456";

    private static final String OWNER_TAG = "owner";

    private static final List<AccountTagResponse> ACCOUNT_TAGS = new ArrayList<>();

    private static final Map<String, String> USER_DEFINED_TAGS = new HashMap<>();

    @MockBean
    private AccountTagService accountTagService;

    @MockBean
    private AccountTagToAccountTagResponsesConverter accountTagToAccountTagResponsesConverter;

    @MockBean
    private CrnUserDetailsService crnUserDetailsService;

    @MockBean
    private EntitlementService entitlementService;

    @MockBean
    private DefaultInternalAccountTagService defaultInternalAccountTagService;

    @Inject
    private EnvironmentTagProvider underTest;

    @BeforeAll
    static void init() {
        AccountTagResponse accountTagResponse = new AccountTagResponse();
        accountTagResponse.setAccountId(ACCOUNT_ID);
        accountTagResponse.setKey(OWNER_TAG);
        accountTagResponse.setValue("{{{userName}}}");
        ACCOUNT_TAGS.add(accountTagResponse);

        USER_DEFINED_TAGS.put("userKey", "userValue");

        ENVIRONMENT.setAccountId(ACCOUNT_ID);
        ENVIRONMENT.setTags(new EnvironmentTags(USER_DEFINED_TAGS, Map.of()));
        ENVIRONMENT.setCreator(USER_CRN);
        ENVIRONMENT.setResourceCrn(ENV_CRN);
        ENVIRONMENT.setCloudPlatform("platform");
        ENVIRONMENT.setNetwork(NetworkDto.builder()
                .withResourceCrn(NETWORK_CRN)
                .build());
    }

    @BeforeEach
    void setUp() {
        when(accountTagService.get(ACCOUNT_ID)).thenReturn(Set.of());

        when(accountTagToAccountTagResponsesConverter.convert(anySet())).thenReturn(ACCOUNT_TAGS);

        CrnUser crnUser = new CrnUser("userId", USER_CRN, USERNAME, "email", "tenant", "role");
        when(crnUserDetailsService.loadUserByUsername(USER_CRN)).thenReturn(crnUser);
    }

    @Test
    void tagsShouldIncludeUserDefinedTags() {
        Map<String, String> result = underTest.getTags(ENVIRONMENT, NETWORK_CRN);

        assertThat(result)
                .containsAllEntriesOf(USER_DEFINED_TAGS);
    }

    @Test
    void tagsShouldIncludeDefaultTags() {
        Map<String, String> result = underTest.getTags(ENVIRONMENT, NETWORK_CRN);

        assertThat(result)
                .containsEntry(DefaultApplicationTag.CREATOR_CRN.key(), USER_CRN)
                .containsEntry(DefaultApplicationTag.ENVIRONMENT_CRN.key(), ENV_CRN)
                .containsEntry(DefaultApplicationTag.RESOURCE_CRN.key(), NETWORK_CRN);
    }

    @Test
    void tagsShouldIncludeApplicationTags() {
        Map<String, String> result = underTest.getTags(ENVIRONMENT, NETWORK_CRN);

        assertThat(result)
                .containsEntry(OWNER_TAG, USERNAME);
    }

}
