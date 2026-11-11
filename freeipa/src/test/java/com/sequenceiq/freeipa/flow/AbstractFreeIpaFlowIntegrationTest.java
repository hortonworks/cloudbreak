package com.sequenceiq.freeipa.flow;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.client.Client;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.flow.core.stats.FlowOperationStatisticsService;
import com.sequenceiq.flow.repository.FlowChainLogRepository;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.freeipa.FreeIpaFlowInformation;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.repository.OperationRepository;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaParallelFlowValidator;
import com.sequenceiq.notification.WebSocketNotificationService;

/**
 * Holds the bean overrides shared by the freeipa flow integration tests.
 *
 * <p>These used to be declared on {@link FlowIntegrationTestConfig}, but a bean override is only
 * applied when it is declared on the test class or one of its superclasses. On a {@code @Configuration}
 * or {@code @TestConfiguration} it is ignored and the real bean is built instead. Boot's
 * {@code @MockBean} did honour those declarations, which is why this only matters once Boot 4 removes
 * it (CB-35263).
 *
 * <p>Most of these mocks are never read by the tests - they exist only to keep the real beans out of
 * the context - so they stay private. {@code freeIpaService} is protected because one subclass stubs it.
 */
public abstract class AbstractFreeIpaFlowIntegrationTest {

    // protected: MultiAzMigrationInitFlowIntegrationTest stubs this one
    @MockitoBean
    protected FreeIpaService freeIpaService;

    @MockitoBean
    private FlowLogRepository flowLogRepository;

    @MockitoBean
    private FlowChainLogRepository flowChainLogRepository;

    @MockitoBean
    private OwnerAssignmentService ownerAssignmentService;

    @MockitoBean
    private WebSocketNotificationService webSocketNotificationService;

    @MockitoBean
    private Client client;

    @MockitoBean
    private SecretService secretService;

    @MockitoBean
    private FreeIpaV1Endpoint freeIpaV1Endpoint;

    @MockitoBean
    private TransactionalScheduler scheduler;

    @MockitoBean
    private FlowOperationStatisticsService flowOperationStatisticsService;

    @MockitoBean
    private OperationRepository operationRepository;

    @MockitoBean
    private FreeIpaFlowInformation freeIpaFlowInformation;

    @MockitoBean
    private FreeIpaParallelFlowValidator freeIpaParallelFlowValidator;

    @MockitoBean
    private CrnUserDetailsService crnUserDetailsService;

    // This stubbing lived in a @BeforeEach on FlowIntegrationTestConfig, where JUnit never invoked it,
    // because lifecycle callbacks only run on the test class hierarchy. Here it actually applies.
    @BeforeEach
    void stubCrnUserDetails() {
        when(crnUserDetailsService.getUmsUser(anyString())).thenReturn(mock(CrnUser.class));
    }
}
