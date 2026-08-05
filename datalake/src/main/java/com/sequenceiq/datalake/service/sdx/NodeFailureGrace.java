package com.sequenceiq.datalake.service.sdx;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.datalake.entity.SdxCluster;

public class NodeFailureGrace {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeFailureGrace.class);

    private final long graceSec;

    private final AtomicReference<Instant> start = new AtomicReference<>();

    public NodeFailureGrace(long graceSec) {
        this.graceSec = graceSec;
    }

    public void reset() {
        start.set(null);
    }

    public Optional<AttemptResult<StackStatusV4Response>> maybeContinue(String process, SdxCluster sdxCluster) {
        if (graceSec <= 0) {
            return Optional.empty();
        }
        Instant firstSeen = start.updateAndGet(existing -> {
            if (existing != null) {
                return existing;
            }
            LOGGER.info("{} flow finished with transient NODE_FAILURE on '{}' cluster, allowing up to {}s before failing",
                    process, sdxCluster.getClusterName(), graceSec);
            return Instant.now();
        });
        long elapsedSec = Duration.between(firstSeen, Instant.now()).getSeconds();
        if (elapsedSec < graceSec) {
            LOGGER.debug("{} still within NODE_FAILURE grace period on '{}' cluster ({}s elapsed, {}s remaining)",
                    process, sdxCluster.getClusterName(), elapsedSec, graceSec - elapsedSec);
            return Optional.of(AttemptResults.justContinue());
        }
        start.set(null);
        LOGGER.info("{} NODE_FAILURE grace period of {}s elapsed on '{}' cluster, failing poll",
                process, graceSec, sdxCluster.getClusterName());
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "NodeFailureGrace{" +
                "graceSec=" + graceSec +
                ", start=" + start.get() +
                '}';
    }
}
