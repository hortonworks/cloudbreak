package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.SubnetSelector;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateLoadBalancerEntityRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateLoadBalancerEntitySuccess;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.loadbalancer.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.network.NetworkService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.TargetGroupPersistenceService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class CreateLoadBalancerEntityHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String ENVIRONMENT_CRN = "envCrn";

    @Mock
    private StackService stackService;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private LoadBalancerConfigService loadBalancerConfigService;

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Mock
    private TargetGroupPersistenceService targetGroupPersistenceService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private NetworkService networkService;

    @Mock
    private SubnetSelector subnetSelector;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private CreateLoadBalancerEntityHandler underTest;

    @Test
    void testDoAcceptFetchesInstanceGroupsWithTargetGroupsEagerlyToAvoidLazyInitialization() {
        Stack stack = mock(Stack.class);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        when(stackService.getById(STACK_ID)).thenReturn(stack);
        DetailedEnvironmentResponse environment = mock(DetailedEnvironmentResponse.class);
        when(environmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(environment);
        Set<InstanceGroup> instanceGroups = Set.of(mock(InstanceGroup.class));
        when(instanceGroupService.getByStackAndFetchTemplatesAndTargetGroups(STACK_ID)).thenReturn(instanceGroups);
        when(loadBalancerPersistenceService.findByStackId(STACK_ID)).thenReturn(Set.of());
        when(loadBalancerConfigService.createLoadBalancers(stack, environment, null)).thenReturn(Set.of());

        CreateLoadBalancerEntityRequest request = new CreateLoadBalancerEntityRequest(STACK_ID);
        HandlerEvent<CreateLoadBalancerEntityRequest> handlerEvent = new HandlerEvent<>(new Event<>(request));

        Selectable result = underTest.doAccept(handlerEvent);

        assertEquals(CreateLoadBalancerEntitySuccess.class, result.getClass());
        verify(instanceGroupService, times(1)).getByStackAndFetchTemplatesAndTargetGroups(STACK_ID);
        verify(instanceGroupService, never()).getByStackAndFetchTemplates(anyLong());
        verify(stack, times(1)).setInstanceGroups(instanceGroups);
    }
}
