package com.sequenceiq.cloudbreak.concurrent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

@ExtendWith(MockitoExtension.class)
class ThreadBasedUserCrnDecoratorTest {

    private static final String USER_CRN = "userCrn";

    @Spy
    @InjectMocks
    private ThreadBasedUserCrnDecorator underTest;

    private AtomicReference<String> userCrnInThread;

    private Runnable runnable;

    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp() {
        userCrnInThread = new AtomicReference<>();
        runnable = () -> userCrnInThread.set(ThreadBasedUserCrnProvider.getUserCrn());

        executor = new ThreadPoolTaskExecutor();
        executor.setVirtualThreads(true);
        // prestart all threads to avoid ThreadBasedUserCrnProvider's InheritableThreadLocal to fill the usercrn
        executor.setPrestartAllCoreThreads(true);
        executor.setTaskDecorator(underTest);
        executor.initialize();
    }

    @Test
    void runnable() throws Exception {
        Future<?> future = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> executor.submit(runnable));
        future.get();

        verifyUserCrn();
    }

    @Test
    void callable() throws Exception {
        Callable<Void> callable = () -> {
            runnable.run();
            return null;
        };

        Future<Void> future = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> executor.submit(callable));
        future.get();

        verifyUserCrn();
    }

    private void verifyUserCrn() {
        assertThat(userCrnInThread.get()).isEqualTo(USER_CRN);
    }

}
