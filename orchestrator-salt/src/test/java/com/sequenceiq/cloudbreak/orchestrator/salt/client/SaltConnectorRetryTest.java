package com.sequenceiq.cloudbreak.orchestrator.salt.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.CircuitBreaker;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.aspect.MeasureAspects;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyWebApplicationException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltErrorResolver;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Glob;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.util.BouncyCastleFipsProviderLoader;
import com.sun.net.httpserver.HttpServer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SaltConnectorRetryTest.TestConfig.class)
@TestPropertySource(properties = {"salt.connector.retry.delay-ms=1", "salt.connector.limited-retry.delay-ms=1"})
class SaltConnectorRetryTest {

    private static final String RETRYABLE_ERROR = "{\"status\":\"503\",\"code\":\"TEST\",\"message\":\"cluster proxy unavailable\",\"retryable\":true}";

    private static final String NON_RETRYABLE_ERROR = "{\"status\":\"503\",\"code\":\"TEST\",\"message\":\"cluster proxy unavailable\",\"retryable\":false}";

    private static final String HEALTH_OK = "{\"address\":\"localhost\",\"statusCode\":200}";

    private static final String GENERIC_RESPONSES_OK = "{\"responses\":[{\"address\":\"localhost\",\"statusCode\":200}]}";

    private static final String HEALTH_PATH = "/saltboot/health";

    private static final String RUN_PATH = "/saltapi/run";

    private static final String PILLAR_PATH = "/saltboot/salt/server/pillar/distribute";

    private static final String ACTION_PATH = "/saltboot/salt/action/distribute";

    private static final String UPLOAD_PATH = "/saltboot/file/distribute";

    @Autowired
    private SaltConnectorExecutor executor;

    private HttpServer server;

    private final Map<String, AtomicInteger> invocations = new ConcurrentHashMap<>();

    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

    @BeforeAll
    static void loadSecurityProvider() {
        BouncyCastleFipsProviderLoader.load();
    }

    @BeforeEach
    void setUp() {
        Logger connectorLogger = (Logger) LoggerFactory.getLogger(SaltConnector.class);
        connectorLogger.setLevel(Level.DEBUG);
        logAppender.start();
        connectorLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(SaltConnector.class)).detachAppender(logAppender);
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void executorIsAProxyOtherwiseRetryableAndMeasureAreInert() {
        assertTrue(AopUtils.isAopProxy(executor));
    }

    @Test
    void healthIsRetriedWhenClusterProxyReportsRetryableError() throws Exception {
        startServer(RETRYABLE_ERROR, 1);

        try (SaltConnector connector = connector()) {
            GenericResponse response = connector.health();

            assertEquals(200, response.getStatusCode());
            assertEquals(2, calls(HEALTH_PATH));
        }
    }

    @Test
    void healthGivesUpAfterMaxAttemptsAndPropagatesTheClusterProxyException() throws Exception {
        startServer(RETRYABLE_ERROR, Integer.MAX_VALUE);

        try (SaltConnector connector = connector()) {
            assertThrows(ClusterProxyWebApplicationException.class, connector::health);

            assertEquals(3, calls(HEALTH_PATH));
        }
    }

    @Test
    void healthIsNotRetriedWhenErrorIsNotRetryable() throws Exception {
        startServer(NON_RETRYABLE_ERROR, Integer.MAX_VALUE);

        try (SaltConnector connector = connector()) {
            WebApplicationException exception = assertThrows(WebApplicationException.class, connector::health);

            assertFalse(exception instanceof ClusterProxyWebApplicationException);
            assertEquals(1, calls(HEALTH_PATH));
        }
    }

    @Test
    void runWithoutTargetDoesNotMultiplyAttempts() throws Exception {
        startServer(RETRYABLE_ERROR, Integer.MAX_VALUE);

        try (SaltConnector connector = connector()) {
            assertThrows(ClusterProxyWebApplicationException.class, () -> connector.run("test.ping", SaltClientType.LOCAL, String.class));

            assertEquals(3, calls(RUN_PATH));
        }
    }

    @Test
    void runWithTargetDoesNotMultiplyAttempts() throws Exception {
        startServer(RETRYABLE_ERROR, Integer.MAX_VALUE);

        try (SaltConnector connector = connector()) {
            assertThrows(ClusterProxyWebApplicationException.class, () -> connector.run(new Glob("*"), "test.ping", SaltClientType.LOCAL, String.class));

            assertEquals(3, calls(RUN_PATH));
        }
    }

    @Test
    void runWithTimeoutDoesNotMultiplyAttempts() throws Exception {
        startServer(RETRYABLE_ERROR, Integer.MAX_VALUE);

        try (SaltConnector connector = connector()) {
            assertThrows(ClusterProxyWebApplicationException.class,
                    () -> connector.run(new Glob("*"), "test.ping", SaltClientType.LOCAL, String.class, 5L));

            assertEquals(3, calls(RUN_PATH));
        }
    }

    @Test
    void runWithLimitedRetryAttemptsTwice() throws Exception {
        startServer(RETRYABLE_ERROR, Integer.MAX_VALUE);

        try (SaltConnector connector = connector()) {
            assertThrows(ClusterProxyWebApplicationException.class,
                    () -> connector.runWithLimitedRetry(new Glob("*"), "test.ping", SaltClientType.LOCAL, String.class));

            assertEquals(2, calls(RUN_PATH));
        }
    }

    @Test
    void asyncRunWithoutTargetIsSubmittedOnlyOnce() throws Exception {
        startServer(RETRYABLE_ERROR, Integer.MAX_VALUE);

        try (SaltConnector connector = connector()) {
            assertThrows(ClusterProxyWebApplicationException.class, () -> connector.run("state.highstate", SaltClientType.LOCAL_ASYNC, String.class));

            assertEquals(1, calls(RUN_PATH));
        }
    }

    @Test
    void asyncRunWithTargetIsSubmittedOnlyOnce() throws Exception {
        startServer(RETRYABLE_ERROR, Integer.MAX_VALUE);

        try (SaltConnector connector = connector()) {
            assertThrows(ClusterProxyWebApplicationException.class,
                    () -> connector.run(new Glob("*"), "state.apply", SaltClientType.LOCAL_ASYNC, String.class, "service"));

            assertEquals(1, calls(RUN_PATH));
        }
    }

    @Test
    void asyncRunWithTimeoutIsSubmittedOnlyOnce() throws Exception {
        startServer(RETRYABLE_ERROR, Integer.MAX_VALUE);

        try (SaltConnector connector = connector()) {
            assertThrows(ClusterProxyWebApplicationException.class,
                    () -> connector.run(new Glob("*"), "state.apply", SaltClientType.LOCAL_ASYNC, String.class, 5L, "service"));

            assertEquals(1, calls(RUN_PATH));
        }
    }

    @Test
    void asyncRunWithLimitedRetryIsSubmittedOnlyOnce() throws Exception {
        startServer(RETRYABLE_ERROR, Integer.MAX_VALUE);

        try (SaltConnector connector = connector()) {
            assertThrows(ClusterProxyWebApplicationException.class,
                    () -> connector.runWithLimitedRetry(new Glob("*"), "state.highstate", SaltClientType.LOCAL_ASYNC, String.class));

            assertEquals(1, calls(RUN_PATH));
        }
    }

    @Test
    void asyncRunStillWorksWhenTheCallSucceeds() throws Exception {
        startServer(RETRYABLE_ERROR, 0);

        try (SaltConnector connector = connector()) {
            assertEquals("{}", connector.run(new Glob("*"), "state.highstate", SaltClientType.LOCAL_ASYNC, String.class));

            assertEquals(1, calls(RUN_PATH));
        }
    }

    @Test
    void wheelIsRetried() throws Exception {
        startServer(RETRYABLE_ERROR, 1);

        try (SaltConnector connector = connector()) {
            assertEquals("{}", connector.wheel("key.list_all", List.of("minion"), String.class));

            assertEquals(2, calls(RUN_PATH));
        }
    }

    @Test
    void pillarIsRetried() throws Exception {
        startServer(RETRYABLE_ERROR, 1);

        try (SaltConnector connector = connector()) {
            GenericResponses response = connector.pillar(List.of("localhost"), new Pillar("/path", Map.of("key", "value"), Set.of("localhost")));

            assertEquals(1, response.getResponses().size());
            assertEquals(2, calls(PILLAR_PATH));
        }
    }

    @Test
    void actionIsRetried() throws Exception {
        startServer(RETRYABLE_ERROR, 1);

        try (SaltConnector connector = connector()) {
            GenericResponses response = connector.action(new SaltAction());

            assertEquals(1, response.getResponses().size());
            assertEquals(2, calls(ACTION_PATH));
        }
    }

    @Test
    void uploadIsRetried() throws Exception {
        startServer(RETRYABLE_ERROR, 1);

        try (SaltConnector connector = connector()) {
            GenericResponses response = connector.upload(List.of("localhost"), "/path", "file", "content".getBytes(StandardCharsets.UTF_8));

            assertEquals(1, response.getResponses().size());
            assertEquals(2, calls(UPLOAD_PATH));
        }
    }

    @Test
    void uploadWithPermissionIsRetried() throws Exception {
        startServer(RETRYABLE_ERROR, 1);

        try (SaltConnector connector = connector()) {
            GenericResponses response = connector.upload(List.of("localhost"), "/path", "file", "0644", "content".getBytes(StandardCharsets.UTF_8));

            assertEquals(1, response.getResponses().size());
            assertEquals(2, calls(UPLOAD_PATH));
        }
    }

    @Test
    void measureEmitsTimingLogForFailedCall() throws Exception {
        startServer(NON_RETRYABLE_ERROR, Integer.MAX_VALUE);

        try (SaltConnector connector = connector()) {
            assertThrows(WebApplicationException.class, connector::health);
        }

        assertEquals(1, measurements("health"), "@Measure must fire once for the failed health call");
    }

    @Test
    void measureEmitsTimingLogForSuccessfulCall() throws Exception {
        startServer(RETRYABLE_ERROR, 0);

        try (SaltConnector connector = connector()) {
            connector.health();
        }

        assertEquals(1, measurements("health"));
    }

    @Test
    void measureEmitsOneTimingLogPerAttemptBecauseRetryWrapsMeasure() throws Exception {
        startServer(RETRYABLE_ERROR, 1);

        try (SaltConnector connector = connector()) {
            connector.health();
        }

        assertEquals(2, measurements("health"));
    }

    @Test
    void measureFiresForEveryPublicRunOverload() throws Exception {
        startServer(RETRYABLE_ERROR, 0);

        try (SaltConnector connector = connector()) {
            connector.run("test.ping", SaltClientType.LOCAL, String.class);
            connector.run(new Glob("*"), "test.ping", SaltClientType.LOCAL, String.class);
            connector.run(new Glob("*"), "test.ping", SaltClientType.LOCAL, String.class, 5L);
            connector.runWithLimitedRetry(new Glob("*"), "test.ping", SaltClientType.LOCAL, String.class);
            connector.run(new Glob("*"), "state.highstate", SaltClientType.LOCAL_ASYNC, String.class);
        }

        assertEquals(3, measurements("run"), "three sync run overloads, async run is logged as runAsync");
        assertEquals(1, measurements("runWithLimitedRetry"));
        assertEquals(1, measurements("runAsync"));
    }

    @Test
    void measureFiresForTheOtherOperations() throws Exception {
        startServer(RETRYABLE_ERROR, 0);

        try (SaltConnector connector = connector()) {
            connector.wheel("key.list_all", List.of("minion"), String.class);
            connector.pillar(List.of("localhost"), new Pillar("/path", Map.of("key", "value"), Set.of("localhost")));
            connector.action(new SaltAction());
            connector.upload(List.of("localhost"), "/path", "file", "content".getBytes(StandardCharsets.UTF_8));
            connector.upload(List.of("localhost"), "/path", "file", "0644", "content".getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(1, measurements("wheel"));
        assertEquals(1, measurements("pillar"));
        assertEquals(1, measurements("action"));
        assertEquals(2, measurements("upload"));
    }

    @Test
    void connectorItselfCarriesNoRetryOrMeasureAnnotation() {
        assertFalse(SaltConnector.class.isAnnotationPresent(Retryable.class));
        for (Method method : SaltConnector.class.getDeclaredMethods()) {
            assertFalse(method.isAnnotationPresent(Retryable.class), method + " must not be annotated, the connector is created per call");
            assertFalse(method.isAnnotationPresent(Measure.class), method + " must not be annotated, the connector is created per call");
        }
    }

    @Test
    void executorHasNoPerCallStateOrRecoverySupport() {
        for (Method method : SaltConnectorExecutor.class.getDeclaredMethods()) {
            assertFalse(method.isAnnotationPresent(Recover.class), method + " would make the retry interceptor hold the target");
            assertFalse(method.isAnnotationPresent(CircuitBreaker.class), method + " would share a breaker between stacks");
            Retryable retryable = method.getAnnotation(Retryable.class);
            if (retryable != null) {
                assertFalse(retryable.stateful(), method + " must be a stateless retry");
            }
        }
        assertEquals(0, Arrays.stream(SaltConnectorExecutor.class.getDeclaredFields()).filter(field -> !Modifier.isStatic(field.getModifiers())).count());
    }

    @Test
    void closedConnectorsAreNotRetainedBySpringRetry() throws Exception {
        startServer(RETRYABLE_ERROR, 0);
        List<WeakReference<SaltConnector>> references = new ArrayList<>();

        for (int i = 0; i < 40; i++) {
            try (SaltConnector connector = connector()) {
                connector.health();
                references.add(new WeakReference<>(connector));
            }
        }

        assertEquals(0, aliveAfterGc(references), "spring-retry caches interceptors per target, the target must not be the per call connector");
    }

    private long measurements(String method) {
        String prefix = "Measuring method execution: SaltConnectorExecutor." + method + " took";
        return logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.startsWith(prefix))
                .collect(Collectors.counting());
    }

    private int calls(String path) {
        return invocations.getOrDefault(path, new AtomicInteger()).get();
    }

    private long aliveAfterGc(List<WeakReference<SaltConnector>> references) throws InterruptedException {
        long alive = references.size();
        for (int i = 0; i < 10 && alive > 0; i++) {
            System.gc();
            Thread.sleep(100);
            alive = references.stream().filter(reference -> reference.get() != null).count();
        }
        return alive;
    }

    private SaltConnector connector() throws Exception {
        GatewayConfig gatewayConfig = GatewayConfig.builder()
                .withConnectionAddress("localhost")
                .withGatewayPort(server.getAddress().getPort())
                .withProtocol("http")
                .withHostname("localhost")
                .withSignatureKey(PkiUtil.convert(PkiUtil.generateKeypair().getPrivate()))
                .build();
        return new SaltConnector(gatewayConfig, SSLContext.getDefault(), mock(SaltErrorResolver.class), false, false, false,
                SaltConnectorTimeouts.defaults(), executor);
    }

    private void startServer(String errorBody, int failuresBeforeSuccess) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        register(HEALTH_PATH, errorBody, failuresBeforeSuccess, HEALTH_OK);
        register(RUN_PATH, errorBody, failuresBeforeSuccess, "{}");
        register(PILLAR_PATH, errorBody, failuresBeforeSuccess, GENERIC_RESPONSES_OK);
        register(ACTION_PATH, errorBody, failuresBeforeSuccess, GENERIC_RESPONSES_OK);
        register(UPLOAD_PATH, errorBody, failuresBeforeSuccess, GENERIC_RESPONSES_OK);
        server.start();
    }

    private void register(String path, String errorBody, int failuresBeforeSuccess, String okBody) {
        server.createContext(path, exchange -> {
            boolean fail = invocations.computeIfAbsent(path, key -> new AtomicInteger()).incrementAndGet() <= failuresBeforeSuccess;
            byte[] body = (fail ? errorBody : okBody).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(fail ? 503 : 200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
    }

    @Configuration
    @EnableRetry
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @Import({SaltConnectorExecutor.class, MeasureAspects.class})
    static class TestConfig {
    }
}
