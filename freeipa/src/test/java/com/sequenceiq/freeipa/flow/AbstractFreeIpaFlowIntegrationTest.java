package com.sequenceiq.freeipa.flow;

import jakarta.ws.rs.client.Client;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.sequenceiq.authorization.service.OwnerAssignmentService;
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
 * <p>A bean override is only applied when declared on the test class or a superclass - on a
 * {@code @Configuration} it is ignored and the real bean is built. These lived on
 * {@link FlowIntegrationTestConfig}, which worked only because Boot's {@code @MockBean} honoured it
 * (removed in Boot 4, CB-35263).
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

}
