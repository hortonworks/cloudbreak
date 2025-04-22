package com.sequenceiq.flow.component;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.ReflectionUtils;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.usage.UsageReportProcessor;
import com.sequenceiq.flow.api.model.FlowCheckResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.flow.component.ComponentTestConfig.TestEnvironmentInitializer;
import com.sequenceiq.flow.component.sleep.SleepChainEventFactory;
import com.sequenceiq.flow.component.sleep.SleepTriggerCondition;
import com.sequenceiq.flow.component.sleep.event.NestedSleepChainTriggerEvent;
import com.sequenceiq.flow.component.sleep.event.SleepChainTriggerEvent;
import com.sequenceiq.flow.component.sleep.event.SleepConfig;
import com.sequenceiq.flow.component.sleep.event.SleepEvent;
import com.sequenceiq.flow.component.sleep.event.SleepStartEvent;
import com.sequenceiq.flow.core.Flow2Handler;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowEventListener;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.core.FlowTriggerConditionResult;
import com.sequenceiq.flow.core.cache.FlowStatCache;
import com.sequenceiq.flow.core.chain.FlowChains;
import com.sequenceiq.flow.core.edh.FlowUsageSender;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.core.model.ResultType;
import com.sequenceiq.flow.domain.ClassValue;
import com.sequenceiq.flow.domain.FlowCancel;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.flow.repository.FlowCancelRepository;
import com.sequenceiq.flow.repository.FlowLogRepository;
import com.sequenceiq.flow.service.FlowService;
import com.sequenceiq.flow.service.flowlog.FlowChainLogService;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(initializers = TestEnvironmentInitializer.class, classes = ComponentTestConfig.class)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FlowComponentTest {

    @Container
    public static final PostgreSQLContainer POSTGRES_CONTAINER = new PostgreSQLContainer("postgres:13.2-alpine")
            .withUsername("postgres")
            .withPassword("postgres");

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowComponentTest.class);

    private static final String USER_CRN = "crn:altus:iam:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:user:63e312c2-d36a-45a7-bb93-fa46c97ffb6b";

    private static final long FLOW_ACCEPT_TIMEOUT = 5L;

    private static final Duration SLEEP_TIME = Duration.ofSeconds(3);

    private static final Duration POLLING_PERIOD = Duration.ofMillis(500);

    private static final long WAIT_FACTOR = 10L;

    private static final AtomicInteger RESOURCE_ID_SEC = new AtomicInteger(0);

    @Inject
    private EventBus eventBus;

    @Inject
    private FlowService flowService;

    @Inject
    private FlowChains flowChains;

    @Inject
    private FlowRegister flowRegister;

    @Inject
    private FlowStatCache flowStatCache;

    @Inject
    private Flow2Handler flow2Handler;

    @Inject
    private FlowLogRepository flowLogRepository;

    @MockBean
    private SleepTriggerCondition sleepTriggerCondition;

    @MockBean
    private MetricService metricService;

    @MockBean
    private TransactionalScheduler scheduler;

    @SpyBean
    private SimpleMeterRegistry meterRegistry;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private FlowChainLogService flowChainLogService;

    @MockBean
    private FlowUsageSender flowUsageSender;

    @MockBean
    private FlowEventListener flowEventListener;

    @MockBean
    private UsageReportProcessor usageReportProcessor;

    @MockBean
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private FlowCancelRepository flowCancelRepository;

    @AfterAll
    public static void afterAll() throws IOException, InterruptedException {
        exportTable("flowlog");
        exportTable("flowchainlog");
        exportTable("flowoperationstats");
    }

    private static void exportTable(String table) throws IOException, InterruptedException {
        ExecResult command = POSTGRES_CONTAINER
                .execInContainer("psql", "-U", "postgres", "--pset=pager=off", "-d", "test", "-c", "select * from " + table + " order by id desc;", "--html");
        Files.writeString(Path.of("build/" + table + ".html"), command.getStdout());
        String error = command.getStderr();
        if (!Strings.isNullOrEmpty(error)) {
            LOGGER.error("Error during {} table export: {}", table, error);
        }
    }

    @BeforeEach
    public void setUp() {
        when(sleepTriggerCondition.isFlowTriggerable(any(Payload.class))).thenReturn(FlowTriggerConditionResult.ok());
    }

    @AfterEach
    public void afterEach() {
        detectMemoryLeakInMap(flowChains, "flowChainMap");
        detectMemoryLeakInSet(flowChains, "notSavedFlowChains");

        detectMemoryLeakInMap(flowRegister, "runningFlows");

        detectMemoryLeakInMap(flowStatCache, "flowIdStatCache");
        detectMemoryLeakInMap(flowStatCache, "flowChainIdStatCache");
        detectMemoryLeakInMap(flowStatCache, "resourceCrnFlowStatCache");
        detectMemoryLeakInMap(flowStatCache, "resourceCrnFlowChainStatCache");
    }

    private void detectMemoryLeakInMap(Object target, String fieldName) {
        Map map = (Map) getFieldValue(target, fieldName);
        if (!map.isEmpty()) {
            throwMemoryLeakException(target, fieldName, map);
        }
    }

    private void detectMemoryLeakInSet(Object target, String fieldName) {
        Set set = (Set) getFieldValue(target, fieldName);
        if (!set.isEmpty()) {
            throwMemoryLeakException(target, fieldName, set);
        }
    }

    private Object getFieldValue(Object target, String fieldName) {
        Field field = ReflectionUtils.findField(target.getClass(), fieldName);
        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, target);
    }

    private void throwMemoryLeakException(Object target, String fieldName, Object data) {
        throw new RuntimeException("Memory leak detected in " + target.getClass().getSimpleName() + '.' + fieldName + " field. " +
                "Make sure unnecessary data is removed at the end of the flow / flow request.\n" +
                "Possible reasons:\n" +
                "- software error,\n" +
                "- test did not wait for a flow to complete or fail.\n" +
                "Content:\n" + data);
    }

    @Test
    @Order(1)
    public void testPurgeSuccessfulAndFailedFlowLogs() throws InterruptedException {
        FlowAcceptResult acceptResult1 = startSleepFlow(SleepStartEvent.neverFail(getNextResourceId(), SLEEP_TIME));
        FlowAcceptResult acceptResult2 = startSleepFlow(SleepStartEvent.alwaysFail(getNextResourceId(), SLEEP_TIME));
        FlowAcceptResult acceptResult3 = startSleepFlow(SleepStartEvent.neverFail(getNextResourceId(), SLEEP_TIME));

        waitFlowToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult1);
        waitFlowToFail(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult2);
        waitFlowToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult3);

        assertEquals(8, flowLogService.purgeFinalizedSuccessfulFlowLogs(0));
        assertEquals(4, flowLogService.purgeFinalizedFailedFlowLogs(0));
        assertEquals(0, flowChainLogService.purgeOrphanFlowChainLogs());
    }

    @Test
    @Order(2)
    public void testPurgeSuccessfulAndFailedFlowChainLogs() throws InterruptedException {
        FlowAcceptResult acceptResult = startSleepFlowChain(new SleepChainTriggerEvent(getNextResourceId(), Lists.newArrayList(
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL),
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL)
        )));

        waitFlowChainToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult);

        assertEquals(8, flowLogService.purgeFinalizedSuccessfulFlowLogs(0));
        assertEquals(0, flowLogService.purgeFinalizedFailedFlowLogs(0));
        assertEquals(3, flowChainLogService.purgeOrphanFlowChainLogs());
    }

    @Test
    @Order(3)
    public void testPurgeSuccessfulAndFailedNestedFlowChainLogs() throws InterruptedException {
        long resourceId = getNextResourceId();
        Promise<AcceptResult> promise = new Promise<>();
        NestedSleepChainTriggerEvent nestedSleepChainTriggerEvent1 = new NestedSleepChainTriggerEvent(resourceId, Lists.newArrayList(
                new SleepChainTriggerEvent(resourceId, Lists.newArrayList(
                        new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL),
                        new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL)
                ), promise),
                new SleepChainTriggerEvent(resourceId, Lists.newArrayList(
                        new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL),
                        new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL)
                ), promise)), promise);

        FlowAcceptResult acceptResult1 = startNestedSleepFlowChain(nestedSleepChainTriggerEvent1);

        waitFlowChainToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult1);

        assertEquals(16, flowLogService.purgeFinalizedSuccessfulFlowLogs(0));
        assertEquals(0, flowLogService.purgeFinalizedFailedFlowLogs(0));
        assertEquals(6, flowChainLogService.purgeOrphanFlowChainLogs());
    }

    @Test
    public void startFlowThenWaitForComplete() throws InterruptedException {
        long resourceId = getNextResourceId();
        SleepStartEvent sleepStartEvent = SleepStartEvent.neverFail(resourceId, SLEEP_TIME);

        FlowAcceptResult acceptResult = startSleepFlow(sleepStartEvent);
        assertRunningInFlow(acceptResult);

        waitFlowToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult);
    }

    @Test
    public void startSecondFlowWhenFirstCompletedThenWaitForComplete() throws InterruptedException {
        long resourceId = getNextResourceId();
        SleepStartEvent sleepStartEvent1 = SleepStartEvent.neverFail(resourceId, SLEEP_TIME);

        FlowAcceptResult acceptResult1 = startSleepFlow(sleepStartEvent1);

        assertRunningInFlow(acceptResult1);
        waitFlowToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult1);

        SleepStartEvent sleepStartEvent2 = SleepStartEvent.neverFail(resourceId, SLEEP_TIME);

        FlowAcceptResult acceptResult2 = startSleepFlow(sleepStartEvent2);

        assertRunningInFlow(acceptResult2);
        waitFlowToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult2);
    }

    @Test
    public void startFlowThenWaitForFail() throws InterruptedException {
        long resourceId = getNextResourceId();
        SleepStartEvent sleepStartEvent = SleepStartEvent.alwaysFail(resourceId, SLEEP_TIME);

        FlowAcceptResult acceptResult = startSleepFlow(sleepStartEvent);

        assertRunningInFlow(acceptResult);
        waitFlowToFail(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult);
    }

    @Test
    public void startingDifferentFlowFailsBeforeFirstCompletes() throws InterruptedException {
        long resourceId = getNextResourceId();
        SleepStartEvent sleepStartEvent1 = SleepStartEvent.neverFail(resourceId, SLEEP_TIME);
        SleepStartEvent sleepStartEvent2 = SleepStartEvent.alwaysFail(resourceId, SLEEP_TIME);

        FlowAcceptResult acceptResult = startSleepFlow(sleepStartEvent1);
        FlowAcceptResult rejectResult = startSleepFlow(sleepStartEvent2);

        assertRunningInFlow(acceptResult);
        assertRejected(rejectResult);
        waitFlowToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult);
    }

    @Test
    public void startIdempotentFlowWhileTheOtherIsRunningReturnsOriginalFlowId() throws InterruptedException {
        long resourceId = getNextResourceId();
        SleepStartEvent sleepStartEvent1 = SleepStartEvent.neverFail(resourceId, SLEEP_TIME);
        SleepStartEvent sleepStartEvent2 = SleepStartEvent.neverFail(resourceId, SLEEP_TIME);

        FlowAcceptResult acceptResult1 = startSleepFlow(sleepStartEvent1);
        FlowAcceptResult acceptResult2 = startSleepFlow(sleepStartEvent2);

        assertRunningInFlow(acceptResult1);
        assertRunningInFlow(acceptResult2);
        waitFlowToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult2);
    }

    @Test
    public void startFlowChainThenWaitForComplete() throws InterruptedException {
        long resourceId = getNextResourceId();
        SleepChainTriggerEvent sleepChainTriggerEvent = new SleepChainTriggerEvent(resourceId, Lists.newArrayList(
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL),
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL)
        ));
        FlowAcceptResult acceptResult = startSleepFlowChain(sleepChainTriggerEvent);

        assertRunningInFlowChain(acceptResult);
        waitFlowChainToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult);
    }

    @Test
    public void startFlowChainWhenSecondFlowTriggerConditionFailsItShouldFail() throws InterruptedException {
        reset(sleepTriggerCondition);

        when(sleepTriggerCondition.isFlowTriggerable(any(Payload.class)))
                .thenReturn(FlowTriggerConditionResult.ok())
                .thenReturn(FlowTriggerConditionResult.fail("Error"));

        long resourceId = getNextResourceId();
        SleepChainTriggerEvent sleepChainTriggerEvent = new SleepChainTriggerEvent(resourceId, Lists.newArrayList(
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL),
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL)
        ));
        FlowAcceptResult acceptResult = startSleepFlowChain(sleepChainTriggerEvent);

        assertRunningInFlowChain(acceptResult);
        waitFlowChainToFail(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult);
    }

    @Test
    public void startFlowChainWhenSecondFlowTriggerConditionSkipItShouldFinish() throws InterruptedException {
        reset(sleepTriggerCondition);

        when(sleepTriggerCondition.isFlowTriggerable(any(Payload.class)))
                .thenReturn(FlowTriggerConditionResult.ok())
                .thenReturn(FlowTriggerConditionResult.skip("Skip"));

        long resourceId = getNextResourceId();
        SleepChainTriggerEvent sleepChainTriggerEvent = new SleepChainTriggerEvent(resourceId, Lists.newArrayList(
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL),
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL)
        ));
        FlowAcceptResult acceptResult = startSleepFlowChain(sleepChainTriggerEvent);

        assertRunningInFlowChain(acceptResult);
        waitFlowChainToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult);
    }

    @Test
    public void startFlowChainThenWaitForFail() throws InterruptedException {
        long resourceId = getNextResourceId();
        SleepChainTriggerEvent sleepChainTriggerEvent = new SleepChainTriggerEvent(resourceId, Lists.newArrayList(
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL),
                new SleepConfig(SLEEP_TIME, SleepStartEvent.ALWAYS_FAIL)
        ));
        FlowAcceptResult acceptResult = startSleepFlowChain(sleepChainTriggerEvent);

        assertRunningInFlowChain(acceptResult);
        waitFlowChainToFail(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult);
    }

    @Test
    public void startingDifferentFlowChainFailsBeforeFirstCompletes() throws InterruptedException {
        long resourceId = getNextResourceId();
        SleepChainTriggerEvent sleepChainTriggerEvent1 = new SleepChainTriggerEvent(resourceId, Lists.newArrayList(
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL),
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL)
        ));
        SleepChainTriggerEvent sleepChainTriggerEvent2 = new SleepChainTriggerEvent(resourceId, Lists.newArrayList(
                new SleepConfig(SLEEP_TIME, SleepStartEvent.ALWAYS_FAIL),
                new SleepConfig(SLEEP_TIME, SleepStartEvent.ALWAYS_FAIL)
        ));

        FlowAcceptResult acceptResult = startSleepFlowChain(sleepChainTriggerEvent1);
        FlowAcceptResult rejectResult = startSleepFlowChain(sleepChainTriggerEvent2);

        assertRunningInFlowChain(acceptResult);
        assertRejected(rejectResult);
        waitFlowChainToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult);
    }

    @Test
    public void startIdempotentFlowChainWhileTheOtherIsRunningReturnsOriginalFlowChainId() throws InterruptedException {
        long resourceId = getNextResourceId();
        SleepChainTriggerEvent sleepChainTriggerEvent1 = new SleepChainTriggerEvent(resourceId, Lists.newArrayList(
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL),
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL)
        ));
        SleepChainTriggerEvent sleepChainTriggerEvent2 = new SleepChainTriggerEvent(resourceId, Lists.newArrayList(
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL),
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL)
        ));

        FlowAcceptResult acceptResult1 = startSleepFlowChain(sleepChainTriggerEvent1);
        FlowAcceptResult acceptResult2 = startSleepFlowChain(sleepChainTriggerEvent2);

        assertRunningInFlowChain(acceptResult1);
        assertRunningInFlowChain(acceptResult2);
        waitFlowChainToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult2);
    }

    @Test
    public void startingFlowChainFailsWhenFlowIsAlreadyRunning() throws InterruptedException {
        long resourceId = getNextResourceId();
        SleepStartEvent sleepStartEvent = SleepStartEvent.neverFail(resourceId, SLEEP_TIME);
        SleepChainTriggerEvent sleepChainTriggerEvent = new SleepChainTriggerEvent(resourceId, Lists.newArrayList(
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL),
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL)
        ));

        FlowAcceptResult acceptResult = startSleepFlow(sleepStartEvent);
        FlowAcceptResult rejectResult = startSleepFlowChain(sleepChainTriggerEvent);

        assertRunningInFlow(acceptResult);
        assertRejected(rejectResult);
        waitFlowToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult);
    }

    @Test
    public void startNestedFlowChainThenWaitForComplete() throws InterruptedException {
        long resourceId = getNextResourceId();
        Promise<AcceptResult> accepted1 = new Promise<>();
        SleepChainTriggerEvent sleepChainTriggerEvent1 = new SleepChainTriggerEvent(resourceId, Lists.newArrayList(
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL)
        ), accepted1);
        SleepChainTriggerEvent sleepChainTriggerEvent2 = new SleepChainTriggerEvent(resourceId, Lists.newArrayList(
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL)
        ), accepted1);
        NestedSleepChainTriggerEvent nestedSleepChainTriggerEvent1 = new NestedSleepChainTriggerEvent(resourceId,
                Lists.newArrayList(sleepChainTriggerEvent1, sleepChainTriggerEvent2), accepted1);

        Promise<AcceptResult> accepted2 = new Promise<>();
        SleepChainTriggerEvent sleepChainTriggerEvent3 = new SleepChainTriggerEvent(resourceId, Lists.newArrayList(
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL)
        ), accepted2);
        SleepChainTriggerEvent sleepChainTriggerEvent4 = new SleepChainTriggerEvent(resourceId, Lists.newArrayList(
                new SleepConfig(SLEEP_TIME, SleepStartEvent.NEVER_FAIL)
        ), accepted2);
        NestedSleepChainTriggerEvent nestedSleepChainTriggerEvent2 = new NestedSleepChainTriggerEvent(resourceId,
                Lists.newArrayList(sleepChainTriggerEvent3, sleepChainTriggerEvent4), accepted2);


        FlowAcceptResult acceptResult1 = startNestedSleepFlowChain(nestedSleepChainTriggerEvent1);
        FlowAcceptResult acceptResult2 = startNestedSleepFlowChain(nestedSleepChainTriggerEvent2);

        assertRunningInFlowChain(acceptResult1);
        assertRunningInFlowChain(acceptResult2);
        waitFlowChainToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult1);
    }

    @Test
    public void retryPendingFlowFails() throws InterruptedException {
        long resourceId = getNextResourceId();
        SleepStartEvent sleepStartEvent = SleepStartEvent.neverFail(resourceId, SLEEP_TIME);

        FlowAcceptResult acceptResult = startSleepFlow(sleepStartEvent);

        assertRunningInFlow(acceptResult);
        BadRequestException exception = assertThrows(BadRequestException.class, () -> flow2Handler.retryLastFailedFlow(resourceId, noOp()));
        assertEquals("Retry cannot be performed, because there is already an active flow.", exception.getMessage());
        waitFlowToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult);
    }

    @Test
    public void retryCompletedFlowFails() throws InterruptedException {
        long resourceId = getNextResourceId();
        SleepStartEvent sleepStartEvent = SleepStartEvent.neverFail(resourceId, SLEEP_TIME);

        FlowAcceptResult acceptResult = startSleepFlow(sleepStartEvent);

        assertRunningInFlow(acceptResult);
        waitFlowToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> flow2Handler.retryLastFailedFlow(resourceId, noOp()));
        assertEquals("Retry cannot be performed, because the last action was successful.", exception.getMessage());
    }

    @Test
    public void retryFirstFailedThenCompletedFlowFails() throws InterruptedException {
        long resourceId = getNextResourceId();
        SleepStartEvent sleepStartEvent = new SleepStartEvent(resourceId, SLEEP_TIME, LocalDateTime.now().plus(SLEEP_TIME.multipliedBy(2)));

        FlowAcceptResult acceptResult = startSleepFlow(sleepStartEvent);

        assertRunningInFlow(acceptResult);
        waitFlowToFail(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult);

        FlowIdentifier flowIdentifier = flow2Handler.retryLastFailedFlow(resourceId, noOp());

        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals(acceptResult.getAsFlowId(), flowIdentifier.getPollableId());
        waitFlowToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult);

        assertThrows(BadRequestException.class, () -> flow2Handler.retryLastFailedFlow(resourceId, noOp()));
    }

    @Test
    public void retryFailedFlowAllowedMultipleTimes() throws InterruptedException {
        long resourceId = getNextResourceId();
        SleepStartEvent sleepStartEvent = SleepStartEvent.alwaysFail(resourceId, SLEEP_TIME);

        FlowAcceptResult acceptResult = startSleepFlow(sleepStartEvent);

        assertRunningInFlow(acceptResult);
        waitFlowToFail(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult);

        FlowIdentifier flowIdentifier = flow2Handler.retryLastFailedFlow(resourceId, noOp());

        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals(acceptResult.getAsFlowId(), flowIdentifier.getPollableId());
        waitFlowToFail(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult);

        flowIdentifier = flow2Handler.retryLastFailedFlow(resourceId, noOp());

        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals(acceptResult.getAsFlowId(), flowIdentifier.getPollableId());
        waitFlowToFail(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult);
    }

    @Test
    public void shouldTolerateWhenFlowLogTypeOrPayloadTypeClassIsNotOnClassPath() {
        long resourceId = getNextResourceId();
        FlowLog flowLog = new FlowLog(
                resourceId,
                "test-flow-id",
                null,
                "userCrn",
                "NEXT_EVENT",
                "payload",
                ClassValue.ofUnknown("nope.NopePayload"),
                "variables",
                ClassValue.ofUnknown("nope.NopeFlowType"),
                "CURRENT_STATE");
        flowLog.setFinalized(true);
        flowLogRepository.save(flowLog);

        List<FlowLog> flowLogs = flowLogRepository.findAllByResourceIdOrderByCreatedDesc(resourceId);

        assertEquals(1, flowLogs.size());
        ClassValue flowType = flowLogs.get(0).getFlowType();
        ClassValue payloadType = flowLogs.get(0).getPayloadType();
        assertFalse(flowType.isOnClassPath());
        assertFalse(payloadType.isOnClassPath());
    }

    @Test
    public void testFlowCancelWithPolling() throws InterruptedException {
        long resourceId = getNextResourceId();
        TestStateStore.put(resourceId, PollGroup.POLLABLE);
        FlowAcceptResult acceptResult = startSleepFlow(SleepStartEvent.neverFail(resourceId, Duration.ofMillis(200)));

        TimeUnit.SECONDS.sleep(3);

        FlowCancel flowCancel = new FlowCancel();
        flowCancel.setResourceId(resourceId);
        flowCancelRepository.save(flowCancel);

        waitFlowToFail(SLEEP_TIME.multipliedBy(WAIT_FACTOR * 5), acceptResult);

        List<FlowLog> flowLogs = flowLogService.findAllByResourceIdOrderByCreatedDesc(resourceId);
        assertEquals(2, flowLogs.size());
        assertEquals("CANCELLED", flowLogs.get(0).getCurrentState());
        assertEquals("INIT_STATE", flowLogs.get(1).getCurrentState());

        TimeUnit.SECONDS.sleep(3);

        FlowIdentifier flowIdentifier = flow2Handler.retryLastFailedFlow(resourceId, noOp());

        assertEquals(FlowType.FLOW, flowIdentifier.getType());
        assertEquals(acceptResult.getAsFlowId(), flowIdentifier.getPollableId());
        waitFlowToComplete(SLEEP_TIME.multipliedBy(WAIT_FACTOR), acceptResult);

        flowLogs = flowLogService.findAllByResourceIdOrderByCreatedDesc(resourceId);
        assertEquals(6, flowLogs.size());
        assertEquals("FINISHED", flowLogs.get(0).getCurrentState());
        assertEquals("SLEEP_FINISHED_STATE", flowLogs.get(1).getCurrentState());
        assertEquals("SLEEP_STARTED_STATE", flowLogs.get(2).getCurrentState());
        assertEquals("INIT_STATE", flowLogs.get(3).getCurrentState());
        assertEquals("CANCELLED", flowLogs.get(4).getCurrentState());
        assertEquals("INIT_STATE", flowLogs.get(5).getCurrentState());
    }

    private long getNextResourceId() {
        return RESOURCE_ID_SEC.incrementAndGet();
    }

    private void assertRunningInFlow(FlowAcceptResult acceptResult) {
        assertNotNull(acceptResult);
        assertEquals(ResultType.RUNNING_IN_FLOW, acceptResult.getResultType());
        assertNotNull(acceptResult.getAsFlowId());
    }

    private void assertRunningInFlowChain(FlowAcceptResult acceptResult) {
        assertNotNull(acceptResult);
        assertEquals(ResultType.RUNNING_IN_FLOW_CHAIN, acceptResult.getResultType());
        assertNotNull(acceptResult.getAsFlowChainId());
    }

    private void assertRejected(FlowAcceptResult acceptResult) {
        assertNotNull(acceptResult);
        assertEquals(ResultType.ALREADY_EXISTING_FLOW, acceptResult.getResultType());
    }

    private FlowAcceptResult startSleepFlow(SleepStartEvent event) throws InterruptedException {
        return notify(SleepEvent.SLEEP_STARTED_EVENT.selector(), event);
    }

    private FlowAcceptResult startSleepFlowChain(SleepChainTriggerEvent event) throws InterruptedException {
        return notify(SleepChainEventFactory.SLEEP_CHAIN_TRIGGER_EVENT, event);
    }

    private FlowAcceptResult startNestedSleepFlowChain(NestedSleepChainTriggerEvent event) throws InterruptedException {
        return notify(NestedSleepChainTriggerEvent.NESTED_SLEEP_CHAIN_TRIGGER_EVENT, event);
    }

    private FlowAcceptResult notify(String selector, Acceptable event) throws InterruptedException {
        Map<String, Object> extendedHeaders = new HashMap<>(Map.of(FlowConstants.FLOW_TRIGGER_USERCRN, USER_CRN));
        extendedHeaders.put(MDCBuilder.MDC_CONTEXT_ID, MDCBuilder.getMdcContextMap());
        eventBus.notify(selector, new Event<>(new Event.Headers(extendedHeaders), event));
        return (FlowAcceptResult) event.accepted().await(FLOW_ACCEPT_TIMEOUT, TimeUnit.SECONDS);
    }

    private void waitFlowToComplete(Duration timeout, FlowAcceptResult acceptResult) {
        baseAwaitConfig("Expected flow to complete: " + acceptResult.getAsFlowId(), timeout)
                .until(() -> {
                    FlowCheckResponse flowState = flowService.getFlowState(acceptResult.getAsFlowId());
                    return !flowState.getHasActiveFlow() && !flowState.getLatestFlowFinalizedAndFailed();
                });
    }

    private void waitFlowToFail(Duration timeout, FlowAcceptResult acceptResult) {
        baseAwaitConfig("Expected flow to fail: " + acceptResult.getAsFlowId(), timeout)
                .until(() -> {
                    FlowCheckResponse flowState = flowService.getFlowState(acceptResult.getAsFlowId());
                    return !flowState.getHasActiveFlow() && flowState.getLatestFlowFinalizedAndFailed();
                });
    }

    private void waitFlowChainToComplete(Duration timeout, FlowAcceptResult acceptResult) {
        baseAwaitConfig("Expected flow chain to complete: " + acceptResult.getAsFlowChainId(), timeout)
                .until(() -> {
                    FlowCheckResponse flowState = flowService.getFlowChainState(acceptResult.getAsFlowChainId());
                    return !flowState.getHasActiveFlow() && !flowState.getLatestFlowFinalizedAndFailed();
                });
    }

    private void waitFlowChainToFail(Duration timeout, FlowAcceptResult acceptResult) {
        baseAwaitConfig("Expected flow chain to fail: " + acceptResult.getAsFlowChainId(), timeout)
                .until(() -> {
                    FlowCheckResponse flowState = flowService.getFlowChainState(acceptResult.getAsFlowChainId());
                    return !flowState.getHasActiveFlow() && flowState.getLatestFlowFinalizedAndFailed();
                });
    }

    private ConditionFactory baseAwaitConfig(String name, Duration timeout) {
        return await(name).dontCatchUncaughtExceptions().atMost(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .pollInterval(POLLING_PERIOD.toMillis(), TimeUnit.MILLISECONDS);
    }

    private Consumer<FlowLog> noOp() {
        return flowLog -> {
        };
    }
}
