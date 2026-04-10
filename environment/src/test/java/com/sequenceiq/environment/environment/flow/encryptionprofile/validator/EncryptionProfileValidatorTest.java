package com.sequenceiq.environment.environment.flow.encryptionprofile.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.environment.experience.ExperienceCluster;
import com.sequenceiq.environment.experience.common.CommonExperienceService;

@ExtendWith(MockitoExtension.class)
class EncryptionProfileValidatorTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:accountId:environment:ac5ba74b-c35e-45e9-9f47-123456789876";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackService stackService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private CommonExperienceService commonExperienceService;

    @InjectMocks
    private EncryptionProfileValidator underTest;

    @Test
    void testEntitlement() {
        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(false);

        CloudbreakServiceException ex = assertThrows(CloudbreakServiceException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(ENVIRONMENT_CRN)));

        assertEquals("Account not entitled for encryption profile. Please contact your CDP administrator to enable it.", ex.getMessage());
    }

    @Test
    void testRuntimeVersion() {
        StackViewV4Response stack1 = new StackViewV4Response();
        stack1.setName("stack1");
        stack1.setStackVersion("7.3.1");
        StackViewV4Response stack2 = new StackViewV4Response();
        stack2.setName("stack2");
        stack2.setStackVersion("7.3.2");
        StackViewV4Response stack3 = new StackViewV4Response();
        stack3.setName("stack3");
        stack3.setStackVersion("7.2.18");
        StackViewV4Response stack4 = new StackViewV4Response();
        stack4.setName("stack4");
        stack4.setStackVersion("7.3.3");
        List<StackViewV4Response> stacks = List.of(stack1, stack2, stack3, stack4);

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(stacks);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(ENVIRONMENT_CRN)));

        assertEquals("All clusters runtime need to be 7.3.2 or above to enable encryption profile. Upgrade cluster(s): stack1,stack3", ex.getMessage());
    }

    @Test
    void testStackStatus() {
        StackViewV4Response stack1 = new StackViewV4Response();
        stack1.setName("stack1");
        stack1.setStackVersion("7.3.3");
        stack1.setStatus(Status.AVAILABLE);
        StackViewV4Response stack2 = new StackViewV4Response();
        stack2.setName("stack2");
        stack2.setStackVersion("7.3.4");
        stack2.setStatus(Status.STOPPED);
        StackViewV4Response stack3 = new StackViewV4Response();
        stack3.setName("stack3");
        stack3.setStackVersion("7.3.2");
        stack3.setStatus(Status.DELETE_FAILED);
        StackViewV4Response stack4 = new StackViewV4Response();
        stack4.setName("stack4");
        stack4.setStackVersion("7.4.0");
        stack4.setStatus(Status.AVAILABLE);
        List<StackViewV4Response> stacks = List.of(stack1, stack2, stack3, stack4);

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(ENVIRONMENT_CRN)).thenReturn(stacks);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(ENVIRONMENT_CRN)));

        assertEquals("All clusters need to be available to enable encryption profile. Cluster(s) not available: stack2,stack3", ex.getMessage());
    }

    @Test
    void testEnvironmentWithExperiences() {
        StackViewV4Response stack1 = new StackViewV4Response();
        stack1.setName("stack1");
        stack1.setStackVersion("7.3.2");
        stack1.setStatus(Status.AVAILABLE);
        StackViewV4Response stack2 = new StackViewV4Response();
        stack2.setName("stack2");
        stack2.setStackVersion("7.3.2");
        stack2.setStatus(Status.AVAILABLE);
        List<StackViewV4Response> stacks = List.of(stack1, stack2);
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setName("env1");

        when(entitlementService.isConfigureEncryptionProfileEnabled(any())).thenReturn(true);
        when(stackService.getAllNotDeletedClustersByEnvironmentCrn(eq(ENVIRONMENT_CRN))).thenReturn(stacks);
        when(environmentService.getByCrnAndAccountId(eq(ENVIRONMENT_CRN), any())).thenReturn(environmentDto);
        when(commonExperienceService.getConnectedClustersForEnvironment(any()))
                .thenReturn(Set.of(ExperienceCluster.builder().withExperienceName("liftie").build(),
                        ExperienceCluster.builder().withExperienceName("anotherExperience").build()));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validate(ENVIRONMENT_CRN)));

        assertEquals("Environment env1 contains experience(s) [anotherExperience,liftie]. Experiences do not support encryption profile yet", ex.getMessage());
    }
}