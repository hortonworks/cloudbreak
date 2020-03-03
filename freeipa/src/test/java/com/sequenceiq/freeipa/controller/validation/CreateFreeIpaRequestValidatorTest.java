package com.sequenceiq.freeipa.controller.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@SpringBootTest
@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
        "freeipa.max.instances=4",
        "freeipa.max.instance.groups=1"
})
@ContextConfiguration(classes = CreateFreeIpaRequestValidator.class)
class CreateFreeIpaRequestValidatorTest {

    private static final String ACCOUNT_ID = "accountId";

    @Autowired
    private CreateFreeIpaRequestValidator underTest;

    @MockBean
    private StackService stackService;

    @MockBean
    private CrnService crnService;

    @MockBean
    private EntitlementService entitlementService;

    @BeforeEach
    void setUp() {
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
    }

    @Test
    void validateShouldNotContainErrors() {
        when(entitlementService.freeIpaHaEnabled(any(), any())).thenReturn(Boolean.TRUE);
        CreateFreeIpaRequest request = new CreateFreeIpaRequest();
        InstanceGroupRequest instanceGroupRequest = new InstanceGroupRequest();
        instanceGroupRequest.setNodeCount(1);
        request.setInstanceGroups(List.of(instanceGroupRequest));

        ValidationResult result = underTest.validate(request);

        assertThat(result.hasError()).isFalse();
    }

    @Test
    void validateShouldNotContainErrorsForHA() {
        when(entitlementService.freeIpaHaEnabled(any(), any())).thenReturn(Boolean.TRUE);
        CreateFreeIpaRequest request = new CreateFreeIpaRequest();
        InstanceGroupRequest instanceGroupRequest = new InstanceGroupRequest();
        instanceGroupRequest.setNodeCount(2);
        request.setInstanceGroups(List.of(instanceGroupRequest));

        ValidationResult result = underTest.validate(request);

        assertThat(result.hasError()).isFalse();
    }

    @Test
    void validateShouldContainErrorsWhenHaEntitlementIsNotEnabled() {
        when(entitlementService.freeIpaHaEnabled(any(), any())).thenReturn(Boolean.FALSE);
        CreateFreeIpaRequest request = new CreateFreeIpaRequest();
        InstanceGroupRequest instanceGroupRequest = new InstanceGroupRequest();
        instanceGroupRequest.setNodeCount(2);
        request.setInstanceGroups(List.of(instanceGroupRequest));

        ValidationResult result = underTest.validate(request);

        assertThat(result.hasError()).isTrue();
    }

    @Test
    void validateShouldContainErrorsWhenThereAreNoInstanceGroups() {
        when(entitlementService.freeIpaHaEnabled(any(), any())).thenReturn(Boolean.TRUE);
        CreateFreeIpaRequest request = new CreateFreeIpaRequest();

        ValidationResult result = underTest.validate(request);

        assertThat(result.hasError()).isTrue();
    }

    @Test
    void validateShouldContainErrorsWhenThereAreZeroNodes() {
        when(entitlementService.freeIpaHaEnabled(any(), any())).thenReturn(Boolean.TRUE);
        CreateFreeIpaRequest request = new CreateFreeIpaRequest();
        InstanceGroupRequest instanceGroupRequest = new InstanceGroupRequest();
        instanceGroupRequest.setNodeCount(0);
        request.setInstanceGroups(List.of(instanceGroupRequest));

        ValidationResult result = underTest.validate(request);

        assertThat(result.hasError()).isTrue();
    }

    @Test
    void validateShouldContainErrorsWhenThereAreTooManyNodes() {
        when(entitlementService.freeIpaHaEnabled(any(), any())).thenReturn(Boolean.TRUE);
        CreateFreeIpaRequest request = new CreateFreeIpaRequest();
        InstanceGroupRequest instanceGroupRequest = new InstanceGroupRequest();
        instanceGroupRequest.setNodeCount(5);
        request.setInstanceGroups(List.of(instanceGroupRequest));

        ValidationResult result = underTest.validate(request);

        assertThat(result.hasError()).isTrue();
    }

    @Test
    void validateShouldContainErrorsWhenThereAreTooManyInstanceGroups() {
        when(entitlementService.freeIpaHaEnabled(any(), any())).thenReturn(Boolean.TRUE);
        CreateFreeIpaRequest request = new CreateFreeIpaRequest();
        InstanceGroupRequest instanceGroupRequest = new InstanceGroupRequest();
        instanceGroupRequest.setNodeCount(1);
        request.setInstanceGroups(List.of(instanceGroupRequest, instanceGroupRequest));

        ValidationResult result = underTest.validate(request);

        assertThat(result.hasError()).isTrue();
    }
}
