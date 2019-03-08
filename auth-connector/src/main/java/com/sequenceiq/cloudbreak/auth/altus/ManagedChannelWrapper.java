package com.sequenceiq.cloudbreak.auth.altus;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ManagedChannel;

/**
 * A managed wrapper channel to provide auto closeable functionality.
 */
public class ManagedChannelWrapper implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedChannelWrapper.class);

    private final ManagedChannel channel;

    /**
     * Constructor.
     *
     * @param channel the channel
     */
    public ManagedChannelWrapper(ManagedChannel channel) {
        this.channel = checkNotNull(channel);
    }

    /**
     * Get the channel.
     */
    public ManagedChannel getChannel() {
        return channel;
    }

    @Override
    public void close() {
        channel.shutdown();
        try {
            if (!channel.awaitTermination(1, TimeUnit.MINUTES)) {
                LOGGER.error("Timed out waiting for channel to shutdown cleanly.");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
