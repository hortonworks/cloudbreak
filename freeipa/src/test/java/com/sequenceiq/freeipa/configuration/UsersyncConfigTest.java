package com.sequenceiq.freeipa.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MDCUtils;
import com.sequenceiq.freeipa.configuration.UsersyncConfigTest.UsersyncConfigTestConfig;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentracing.Tracer;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { UsersyncConfig.class, UsersyncConfigTestConfig.class })
@TestPropertySource(properties = {
        "freeipa.usersync.threadpool.core.size=1",
        "freeipa.usersync.threadpool.capacity.size=10"
})
class UsersyncConfigTest {
    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:"
            + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    @Inject
    @Qualifier(UsersyncConfig.USERSYNC_TASK_EXECUTOR)
    ExecutorService usersyncTaskExecutor;

    @MockBean
    Tracer tracer;

    @Test
    void testAsyncTaskExecutorDecoration() throws Exception {
        String expectedRequestId = "requestId";
        MDCBuilder.addRequestId(expectedRequestId);
        ThreadBasedUserCrnProvider.setUserCrn(USER_CRN);

        Future<?> future = usersyncTaskExecutor.submit(() -> {
                assertEquals(expectedRequestId, MDCUtils.getRequestId().get());
                assertEquals(USER_CRN, ThreadBasedUserCrnProvider.getUserCrn());
            });
        future.get(1L, TimeUnit.SECONDS);
    }

    @TestConfiguration
    public static class UsersyncConfigTestConfig {

        @Bean
        public MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}