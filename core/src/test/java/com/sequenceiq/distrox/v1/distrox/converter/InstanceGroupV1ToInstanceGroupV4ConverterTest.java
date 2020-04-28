package com.sequenceiq.distrox.v1.distrox.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import com.sequenceiq.common.api.type.ScalingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.instancegroup.AwsInstanceGroupV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.AwsInstanceGroupV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;

@ExtendWith(MockitoExtension.class)
class InstanceGroupV1ToInstanceGroupV4ConverterTest {

    private static final String INSTANE_GROUP_NAME = "ig1";

    private static final String SECURITY_GROUP1 = "sg1";

    private static final String SECURITY_GROUP2 = "sg2";

    private static final int NODE_COUNT_1 = 10;

    private static final AwsInstanceGroupV1Parameters AWS_INSTANCE_GROUP_V1_PARAMETERS = new AwsInstanceGroupV1Parameters();

    private static final Set<String> RECIPE_NAMES = Set.of("recipe1", "recipe2");

    private static final AwsInstanceGroupV4Parameters AWS_INSTANCE_GROUP_V4_PARAMETERS = new AwsInstanceGroupV4Parameters();

    private static final InstanceTemplateV4Request INSTENCE_TEMPLATE_V4_REQUEST = new InstanceTemplateV4Request();

    @Mock
    private InstanceTemplateV1ToInstanceTemplateV4Converter instanceTemplateConverter;

    @Mock
    private InstanceGroupParameterConverter instanceGroupParameterConverter;

    @InjectMocks
    private InstanceGroupV1ToInstanceGroupV4Converter underTest;

    @BeforeEach
    void setUp() {
        when(instanceGroupParameterConverter.convert(AWS_INSTANCE_GROUP_V1_PARAMETERS)).thenReturn(AWS_INSTANCE_GROUP_V4_PARAMETERS);
        when(instanceTemplateConverter.convert(any(InstanceTemplateV1Request.class))).thenReturn(INSTENCE_TEMPLATE_V4_REQUEST);
    }

    @Test
    void convertToAwsWithoutSecurityGroupsHappyPathTest() {
        Set<InstanceGroupV1Request> instanceGroups = prepareInstanceGroups(InstanceGroupType.CORE, ScalingMode.UNSPECIFIED);

        List<InstanceGroupV4Request> results = underTest.convertTo(instanceGroups, null);
        assertThat(results).hasSameSizeAs(instanceGroups);

        InstanceGroupV4Request first = results.get(0);

        assertThat(first.getAws()).isEqualTo(AWS_INSTANCE_GROUP_V4_PARAMETERS);
        assertThat(first.getCloudPlatform()).isEqualTo(CloudPlatform.AWS);
        assertThat(first.getName()).isEqualTo(INSTANE_GROUP_NAME);
        assertThat(first.getNodeCount()).isEqualTo(NODE_COUNT_1);
        assertThat(first.getRecipeNames()).isEqualTo(RECIPE_NAMES);
        assertThat(first.getRecoveryMode()).isEqualTo(RecoveryMode.AUTO);
        assertThat(first.getTemplate()).isEqualTo(INSTENCE_TEMPLATE_V4_REQUEST);
        assertThat(first.getType()).isEqualTo(InstanceGroupType.CORE);
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] securityAccessDataProvider() {
        return new Object[][] {
                // Testcase name                       InstanceGroupType          ScaleType               EnvironmentSet   SecurityAccessSet   CIDR         defaultSecurityGroupId  securityGroupIdKnox   securityGroupExpected  cidrExpected,  expectedSGs
                { "No Environment set",                InstanceGroupType.CORE,    ScalingMode.UNSPECIFIED,  false,           false,              null,        null,                   null,                 true,                  false,         Set.of() },
                { "No SecurityAccess set",             InstanceGroupType.CORE,    ScalingMode.NONE,         true,            false,              null,        null,                   null,                 false,                 false,         null },
                { "SecurityAccess w/ null props",      InstanceGroupType.CORE,    ScalingMode.UNSPECIFIED,  true,            true,               null,        null,                   null,                 false,                 false,         null },
                { "SecurityAccess w/ empty cidr",      InstanceGroupType.CORE,    ScalingMode.UNSPECIFIED,  true,            true,               "",          null,                   null,                 true,                  false,         Set.of() },
                { "SecurityAccess w/ empty group Ids", InstanceGroupType.CORE,    ScalingMode.UNSPECIFIED,  true,            true,               null,        "",                     "",                   true,                  false,         Set.of() },
                { "Core group Id",                     InstanceGroupType.CORE,    null,                     true,            true,               null,        SECURITY_GROUP1,        SECURITY_GROUP2,      true,                  false,         Set.of(SECURITY_GROUP1) },
                { "Gateway group Id",                  InstanceGroupType.GATEWAY, ScalingMode.UNSPECIFIED,  true,            true,               null,        SECURITY_GROUP1,        SECURITY_GROUP2,      true,                  false,         Set.of(SECURITY_GROUP2) },
                { "CIDR and SG uses SG",               InstanceGroupType.CORE,    ScalingMode.UNSPECIFIED,  true,            true,               "0.0.0.0/0", SECURITY_GROUP1,        SECURITY_GROUP2,      true,                  false,         Set.of(SECURITY_GROUP1) },
                { "CIDR",                              InstanceGroupType.CORE,    ScalingMode.NONE,         true,            true,               "0.0.0.0/0", null,                   null,                 true,                  true,          Set.of() },

        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("securityAccessDataProvider")
    void createSecurityGroupFromEnvironmentTest(String testCaseName, InstanceGroupType instanceGroupType, ScalingMode scalingMode, boolean environmentSet,
                                                boolean securityAccessSet, String cidr, String defaultSecurityGroupId, String securityGroupIdKnox,
                                                boolean securityGroupExpected, boolean cidrExpected, Set<String> expectedSecurityGroups) {

        DetailedEnvironmentResponse environment = environmentSet
                ? prepareEnvironment(securityAccessSet, cidr, defaultSecurityGroupId, securityGroupIdKnox)
                : null;
        Set<InstanceGroupV1Request> instanceGroups = prepareInstanceGroups(instanceGroupType, scalingMode);

        List<InstanceGroupV4Request> results = underTest.convertTo(instanceGroups, environment);
        assertThat(results).hasSameSizeAs(instanceGroups);
        InstanceGroupV4Request first = results.get(0);

        assertThat(first.getType()).isEqualTo(instanceGroupType);

        assertThat(first.getScalingMode()).isEqualTo(scalingMode);

        SecurityGroupV4Request securityGroup = first.getSecurityGroup();
        assertThat(securityGroup != null).isEqualTo(securityGroupExpected);

        if (securityGroupExpected) {
            if (securityGroup.getSecurityGroupIds() != null) {
                assertThat(securityGroup.getSecurityGroupIds()).hasSameElementsAs(expectedSecurityGroups);
                assertThat(securityGroup.getSecurityRules()).usingFieldByFieldElementComparator().hasSameElementsAs(cidrExpected
                        ? List.of(generateRule(instanceGroupType, cidr))
                        : List.of());
            }
        }
    }

    private DetailedEnvironmentResponse prepareEnvironment(boolean securityAccessSet,
            String cidr, String defaultSecurityGroupId, String securityGroupIdKnox) {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setSecurityAccess(securityAccessSet
                ? createSecurityAccessResponse(cidr, defaultSecurityGroupId, securityGroupIdKnox)
                : null);
        return environment;
    }

    private Set<InstanceGroupV1Request> prepareInstanceGroups(InstanceGroupType instanceGroupType, ScalingMode scalingMode) {
        InstanceGroupV1Request instanceGroup = new InstanceGroupV1Request();
        instanceGroup.setAws(AWS_INSTANCE_GROUP_V1_PARAMETERS);
        instanceGroup.setName(INSTANE_GROUP_NAME);
        instanceGroup.setNodeCount(NODE_COUNT_1);
        instanceGroup.setRecipeNames(RECIPE_NAMES);
        instanceGroup.setRecoveryMode(RecoveryMode.AUTO);
        instanceGroup.setTemplate(new InstanceTemplateV1Request());
        instanceGroup.setType(instanceGroupType);
        instanceGroup.setScalingMode(scalingMode);
        return Set.of(instanceGroup);
    }

    private SecurityAccessResponse createSecurityAccessResponse(String cidr, String defaultSecurityGroupId, String securityGroupIdKnox) {
        return SecurityAccessResponse.builder()
                .withCidr(cidr)
                .withDefaultSecurityGroupId(defaultSecurityGroupId)
                .withSecurityGroupIdForKnox(securityGroupIdKnox)
                .build();
    }

    private static SecurityRuleV4Request generateRule(InstanceGroupType instanceGroupType, String cidr) {
        SecurityRuleV4Request request = new SecurityRuleV4Request();
        request.setProtocol("tcp");
        request.setPorts(instanceGroupType == InstanceGroupType.CORE
                ? List.of("22")
                : List.of("22", "9443", "8443", "443"));
        request.setSubnet(cidr);
        return request;
    }

}
