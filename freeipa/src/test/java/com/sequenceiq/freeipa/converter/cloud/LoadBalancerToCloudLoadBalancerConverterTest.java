package com.sequenceiq.freeipa.converter.cloud;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.NetworkProtocol;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.entity.TargetGroup;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;

@ExtendWith(MockitoExtension.class)
class LoadBalancerToCloudLoadBalancerConverterTest {

    private static final long STACK_ID = 1L;

    private static final int HEALTH_CHECK_PORT = 1234;

    private static final String HEALTH_CHECK_PATH = "/check";

    private static final String HEALTH_CHECK_PROTOCOL = "HTTPS";

    private static final int TRAFFIC_PORT = 5678;

    @InjectMocks
    private LoadBalancerToCloudLoadBalancerConverter underTest;

    @Mock
    private FreeIpaLoadBalancerService freeIpaLoadBalancerService;

    @BeforeEach
    public void before() {
        ReflectionTestUtils.setField(underTest, "healthCheckPath", HEALTH_CHECK_PATH);
        ReflectionTestUtils.setField(underTest, "healthCheckPort", HEALTH_CHECK_PORT);
        ReflectionTestUtils.setField(underTest, "healthCheckProtocol", HEALTH_CHECK_PROTOCOL);
    }

    @Test
    void testConvertLoadBalancerShouldReturnsCloudLoadBalancerIfLoadBalancerIsPresentForTheStack() {
        Set<Group> groups = Set.of(mock(Group.class));
        LoadBalancer loadBalancer = createLoadBalancer();
        when(freeIpaLoadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.of(loadBalancer));

        List<CloudLoadBalancer> actual = underTest.convertLoadBalancer(STACK_ID, groups);

        CloudLoadBalancer cloudLoadBalancer = actual.getFirst();
        assertTrue(cloudLoadBalancer.getPortToTargetGroupMapping().entrySet().stream()
                .anyMatch(
                        entry -> entry.getKey().getHealthCheckPort().equals(HEALTH_CHECK_PORT) &&
                                entry.getKey().getTrafficPort().equals(TRAFFIC_PORT) &&
                                entry.getKey().getHealthProbeParameters().getPath().equals(HEALTH_CHECK_PATH) &&
                                entry.getKey().getTrafficProtocol().equals(NetworkProtocol.TCP) &&
                                entry.getKey().getHealthProbeParameters().getProtocol().equals(NetworkProtocol.HTTPS) &&
                                entry.getValue().equals(groups)));
    }

    @Test
    void testConvertLoadBalancerShouldReturnsEmptyListIfLoadBalancerIsNotPresentForTheStack() {
        Set<Group> groups = Set.of(mock(Group.class));
        when(freeIpaLoadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.empty());

        List<CloudLoadBalancer> actual = underTest.convertLoadBalancer(STACK_ID, groups);

        assertTrue(actual.isEmpty());
    }

    private LoadBalancer createLoadBalancer() {
        TargetGroup targetGroup = new TargetGroup();
        targetGroup.setTrafficPort(TRAFFIC_PORT);
        targetGroup.setProtocol(NetworkProtocol.TCP.name());
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setTargetGroups(Set.of(targetGroup));
        return loadBalancer;
    }
}