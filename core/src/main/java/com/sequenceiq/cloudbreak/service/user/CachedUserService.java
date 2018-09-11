package com.sequenceiq.cloudbreak.service.user;

import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.repository.workspace.UserRepository;
import com.sequenceiq.cloudbreak.service.Clock;

@Service
public class CachedUserService {

    private static final long CACHE_TTL = 5 * 60 * 1000;

    private static final Map<IdentityUser, Entry> CACHE = new ConcurrentHashMap<>();

    private static final Map<IdentityUser, Semaphore> UNDER_OPERATION = new ConcurrentHashMap<>();

    private static final Timer TIMER = new Timer();

    private static final long TIMER_PERIOD = 10 * 1000;

    static {
        TIMER.schedule(new TimerTask() {
            @Override
            public void run() {
                CACHE.entrySet().removeIf(e -> e.getValue().created + CACHE_TTL <= System.currentTimeMillis());
            }
        }, TIMER_PERIOD, TIMER_PERIOD);
    }

    @Inject
    private UserRepository userRepository;

    @Inject
    private Clock clock;

    User getCached(IdentityUser identityUser, Function<IdentityUser, User> createUser) throws InterruptedException {
        Semaphore semaphore = UNDER_OPERATION.computeIfAbsent(identityUser, iu -> new Semaphore(1));
        semaphore.acquire();
        try {
            return CACHE.computeIfAbsent(identityUser, iu -> {
                User user = Optional.ofNullable(userRepository.findByUserId(iu.getUsername())).orElseGet(() -> createUser.apply(iu));
                return new Entry(clock.getCurrentTime(), user);
            }).user;
        } finally {
            semaphore.release();
            UNDER_OPERATION.remove(identityUser);
        }
    }

    public void evictUser(IdentityUser identityUser) {
        CACHE.remove(identityUser);
    }

    private static class Entry {
        private final long created;

        private final User user;

        Entry(long created, User user) {
            this.created = created;
            this.user = user;
        }
    }
}
