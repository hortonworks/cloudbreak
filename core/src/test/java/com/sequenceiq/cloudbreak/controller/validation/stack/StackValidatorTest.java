package com.sequenceiq.cloudbreak.controller.validation.stack;

import static com.sequenceiq.cloudbreak.validation.ValidationResult.State.ERROR;
import static com.sequenceiq.cloudbreak.validation.ValidationResult.State.VALID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.controller.validation.template.InstanceTemplateValidator;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;

@RunWith(MockitoJUnitRunner.class)
public class StackValidatorTest extends StackRequestValidatorTestBase {

    private static final String TEST_BP_NAME = "testBpName";

    @Spy
    private final InstanceTemplateValidator templateValidator = new InstanceTemplateValidator();

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @InjectMocks
    private StackValidator underTest;

    @Mock
    private Blueprint blueprint;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private Credential credential;

    @Mock
    private CloudRegions cloudRegions;

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private EntitlementService entitlementService;

    public StackValidatorTest() {
        super(LoggerFactory.getLogger(StackValidatorTest.class));
    }

    @Test
    public void testWithZeroRootVolumeSize() {
        assertNotNull(templateValidator);
        Stack stackRequest = stackWithRootVolumeSize(0);
        ValidationResult.ValidationResultBuilder builder = new ValidationResult.ValidationResultBuilder();

        entitlementMock(stackRequest, false);

        underTest.validate(stackRequest, builder);
        assertEquals(ERROR, builder.build().getState());
    }

    @Test
    public void testWithNegativeRootVolumeSize() {
        Stack stack = stackWithRootVolumeSize(-1);
        ValidationResult.ValidationResultBuilder builder = new ValidationResult.ValidationResultBuilder();

        entitlementMock(stack, false);

        underTest.validate(stack, builder);
        assertEquals(ERROR, builder.build().getState());
    }

    @Test
    public void testNullValueIsAllowedForRootVolumeSize() {
        Stack stack = stackWithRootVolumeSize(null);
        ValidationResult.ValidationResultBuilder builder = new ValidationResult.ValidationResultBuilder();

        entitlementMock(stack, false);

        underTest.validate(stack, builder);
        assertEquals(VALID, builder.build().getState());
    }

    @Test
    public void testWithPositiveRootVolumeSize() {
        Stack stack = stackWithRootVolumeSize(1);
        ValidationResult.ValidationResultBuilder builder = new ValidationResult.ValidationResultBuilder();

        entitlementMock(stack, false);

        underTest.validate(stack, builder);
        assertEquals(VALID, builder.build().getState());
    }

    @Test
    public void testWithNativeVariantWhenEntitlementEnabledShouldBeValid() {
        Stack stack = stackWithRootVolumeSize(1);
        stack.setPlatformVariant(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value());
        ValidationResult.ValidationResultBuilder builder = new ValidationResult.ValidationResultBuilder();

        entitlementMock(stack, true);

        underTest.validate(stack, builder);
        assertEquals(VALID, builder.build().getState());
    }

    @Test
    public void testWithNativeVariantWhenEntitlementNOTEnabledShouldNOTBeValid() {
        Stack stack = stackWithRootVolumeSize(1);
        stack.setPlatformVariant(AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.variant().value());
        ValidationResult.ValidationResultBuilder builder = new ValidationResult.ValidationResultBuilder();

        entitlementMock(stack, false);

        underTest.validate(stack, builder);
        assertEquals(ERROR, builder.build().getState());
    }

    private void entitlementMock(Stack stack, boolean enabled) {
        Tenant tenant = new Tenant();
        tenant.setName("tenant1");

        User user = new User();
        user.setTenant(tenant);

        stack.setCreator(user);

        when(entitlementService.awsNativeEnabled(anyString())).thenReturn(enabled);
    }

    private Stack stackWithRootVolumeSize(Integer rootVolumeSize) {
        Template templateRequest = new Template();
        templateRequest.setRootVolumeSize(rootVolumeSize);
        InstanceGroup instanceGroup = getInstanceGroup(templateRequest);
        Cluster clusterRequest = getCluster();
        return getStack(Sets.newHashSet(instanceGroup), clusterRequest);
    }

    private InstanceGroup getInstanceGroup(Template templateRequest) {
        InstanceGroup instanceGroupRequest = new InstanceGroup();
        instanceGroupRequest.setGroupName("master");
        instanceGroupRequest.setTemplate(templateRequest);
        return instanceGroupRequest;
    }

    private Cluster getCluster() {
        Cluster cluster = new Cluster();
        Blueprint blueprint = new Blueprint();
        blueprint.setName(TEST_BP_NAME);
        cluster.setBlueprint(blueprint);
        return cluster;
    }

    private Stack getStack(Set<InstanceGroup> instanceGroupRequests, Cluster clusterRequest) {
        Stack stackRequest = new Stack();
        stackRequest.setCluster(clusterRequest);
        stackRequest.setInstanceGroups(instanceGroupRequests);
        stackRequest.setEnvironmentCrn("envCrn");
        stackRequest.setRegion("region");
        stackRequest.setResourceCrn("crn:cdp:datahub:us-west-1:account:cluster:cluster");
        return stackRequest;
    }

}