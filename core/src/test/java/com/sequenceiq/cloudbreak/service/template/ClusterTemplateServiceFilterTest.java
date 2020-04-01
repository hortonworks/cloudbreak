package com.sequenceiq.cloudbreak.service.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplateView;
import com.sequenceiq.cloudbreak.service.runtimes.SupportedRuntimes;
import com.sequenceiq.distrox.v1.distrox.service.EnvironmentServiceDecorator;

@ExtendWith(MockitoExtension.class)
class ClusterTemplateServiceFilterTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:altus:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String AWS = "AWS";

    private static final String AZURE = "AZURE";

    private static final Long WORKSPACE_ID = 123L;

    private static final String TEMPLATE_NAME = "templateName";

    private static final ClusterTemplateViewV4Response CLUSTER_TEMPLATE_VIEW_V4_RESPONSE_AWS = createClusterTemplateViewV4Response(AWS);

    private static final ClusterTemplateViewV4Response CLUSTER_TEMPLATE_VIEW_V4_RESPONSE_AZURE = createClusterTemplateViewV4Response(AZURE);

    @Mock
    private ClusterTemplateViewService clusterTemplateViewService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private ConverterUtil converterUtil;

    @Mock
    private EnvironmentServiceDecorator environmentServiceDecorator;

    @Mock
    private ClusterTemplateCloudPlatformValidator cloudPlatformValidator;

    @Spy
    private SupportedRuntimes supportedRuntimes;

    @InjectMocks
    private ClusterTemplateService underTest;

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] validateClusterTemplateCloudPlatformDataProvider() {
        return new Object[][]{
                // testCaseName         cloudPlatformValid
                {"invalid platform", false},
                {"valid platform", true},
        };
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] listInWorkspaceAndCleanUpInvalidsDataProvider() {
        return new Object[][]{
                // testCaseName                 awsEnabled  azureEnabled    expectedResult
                {"AWS invalid, AZURE invalid", false, false, Set.of()},
                {"AWS valid, AZURE invalid", true, false, Set.of(CLUSTER_TEMPLATE_VIEW_V4_RESPONSE_AWS)},
                {"AWS invalid, AZURE valid", false, true, Set.of(CLUSTER_TEMPLATE_VIEW_V4_RESPONSE_AZURE)},
                {"AWS valid, AZURE valid", true, true, Set.of(CLUSTER_TEMPLATE_VIEW_V4_RESPONSE_AWS, CLUSTER_TEMPLATE_VIEW_V4_RESPONSE_AZURE)},
        };
    }

    private static ClusterTemplateViewV4Response createClusterTemplateViewV4Response(String cloudPlatform) {
        ClusterTemplateViewV4Response response = new ClusterTemplateViewV4Response();
        response.setName(TEMPLATE_NAME + cloudPlatform);
        response.setCloudPlatform(cloudPlatform);
        response.setStatus(ResourceStatus.DEFAULT);
        return response;
    }

    @Test
    void testIfGettingUsableTemplateWhenTemplateIsDefaultThenTrueShouldCome() {
        ClusterTemplateViewV4Response templateViewV4Response = new ClusterTemplateViewV4Response();
        templateViewV4Response.setStatus(ResourceStatus.DEFAULT);

        boolean result = underTest.isUsableClusterTemplate(templateViewV4Response);

        assertTrue(result);
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @Test
    void testIfGettingUsableTemplateWhenTemplateIsUserManagedAndHasEnvironmentNameInItThenTrueShouldCome() {
        ClusterTemplateViewV4Response templateViewV4Response = new ClusterTemplateViewV4Response();
        templateViewV4Response.setStatus(ResourceStatus.USER_MANAGED);
        templateViewV4Response.setEnvironmentName("SomeEnvironmentName");

        boolean result = underTest.isUsableClusterTemplate(templateViewV4Response);

        assertTrue(result);
    }

    @Test
    void testIfGettingUsableTemplateWhenTemplateIsUserManagedButHasNullEnvironmentNameThenFalseShouldCome() {
        ClusterTemplateViewV4Response templateViewV4Response = new ClusterTemplateViewV4Response();
        templateViewV4Response.setStatus(ResourceStatus.USER_MANAGED);
        templateViewV4Response.setEnvironmentName(null);

        boolean result = underTest.isUsableClusterTemplate(templateViewV4Response);

        assertFalse(result);
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("validateClusterTemplateCloudPlatformDataProvider")
    void testIsClusterTemplateHasValidCloudPlatform(String testCaseName, boolean cloudPlatformValid) {
        ClusterTemplateViewV4Response response = new ClusterTemplateViewV4Response();
        response.setCloudPlatform(AWS);

        when(cloudPlatformValidator.isClusterTemplateCloudPlatformValid(AWS, ACCOUNT_ID)).thenReturn(cloudPlatformValid);

        boolean result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.isClusterTemplateHasValidCloudPlatform(response));

        assertThat(result).isEqualTo(cloudPlatformValid);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("listInWorkspaceAndCleanUpInvalidsDataProvider")
    void testListInWorkspaceAndCleanUpInvalidsWhenFilteringByCloudPlatform(String testCaseName, boolean awsEnabled, boolean azureEnabled,
            Set<ClusterTemplateViewV4Response> expectedResult) throws TransactionService.TransactionExecutionException {
        Set<ClusterTemplateView> views = Set.of(new ClusterTemplateView(), new ClusterTemplateView());
        Set<ClusterTemplateViewV4Response> responses = Set.of(CLUSTER_TEMPLATE_VIEW_V4_RESPONSE_AWS, CLUSTER_TEMPLATE_VIEW_V4_RESPONSE_AZURE);

        when(transactionService.required(isA(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
        when(clusterTemplateViewService.findAllActive(WORKSPACE_ID)).thenReturn(views);
        when(converterUtil.convertAllAsSet(views, ClusterTemplateViewV4Response.class)).thenReturn(responses);

        when(cloudPlatformValidator.isClusterTemplateCloudPlatformValid(AWS, ACCOUNT_ID)).thenReturn(awsEnabled);
        when(cloudPlatformValidator.isClusterTemplateCloudPlatformValid(AZURE, ACCOUNT_ID)).thenReturn(azureEnabled);

        Set<ClusterTemplateViewV4Response> result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.listInWorkspaceAndCleanUpInvalids(WORKSPACE_ID));

        assertThat(result).isEqualTo(expectedResult);
        verify(environmentServiceDecorator).prepareEnvironments(responses);
    }

}