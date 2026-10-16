package com.sequenceiq.cloudbreak.orchestrator.salt.client;

import java.io.IOException;
import java.util.Collection;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyWebApplicationException;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Pillar;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;

@Component
public class SaltConnectorExecutor {

    private static final String RETRY_DELAY_EXPRESSION = "${salt.connector.retry.delay-ms:1000}";

    private static final String LIMITED_RETRY_DELAY_EXPRESSION = "${salt.connector.limited-retry.delay-ms:500}";

    @Measure(SaltConnector.class)
    @Retryable(value = ClusterProxyWebApplicationException.class, backoff = @Backoff(delayExpression = RETRY_DELAY_EXPRESSION))
    public GenericResponse health(SaltConnector connector) {
        return connector.doHealth();
    }

    @Measure(SaltConnector.class)
    @Retryable(value = ClusterProxyWebApplicationException.class, backoff = @Backoff(delayExpression = RETRY_DELAY_EXPRESSION))
    public GenericResponses pillar(SaltConnector connector, Iterable<String> targets, Pillar pillar) {
        return connector.doPillar(targets, pillar);
    }

    @Measure(SaltConnector.class)
    @Retryable(value = ClusterProxyWebApplicationException.class, backoff = @Backoff(delayExpression = RETRY_DELAY_EXPRESSION))
    public GenericResponses action(SaltConnector connector, SaltAction saltAction) {
        return connector.doAction(saltAction);
    }

    @Measure(SaltConnector.class)
    @Retryable(value = ClusterProxyWebApplicationException.class, backoff = @Backoff(delayExpression = RETRY_DELAY_EXPRESSION))
    public <T> T run(SaltConnector connector, Target<String> target, String fun, SaltClientType clientType, Class<T> clazz, Long timeout, String... arg) {
        return connector.doRun(target, fun, clientType, clazz, timeout, arg);
    }

    @Measure(SaltConnector.class)
    @Retryable(maxAttempts = 2, value = ClusterProxyWebApplicationException.class, backoff = @Backoff(delayExpression = LIMITED_RETRY_DELAY_EXPRESSION))
    public <T> T runWithLimitedRetry(SaltConnector connector, Target<String> target, String fun, SaltClientType clientType, Class<T> clazz, String... arg) {
        return connector.doRun(target, fun, clientType, clazz, null, arg);
    }

    @Measure(SaltConnector.class)
    public <T> T runAsync(SaltConnector connector, Target<String> target, String fun, SaltClientType clientType, Class<T> clazz, Long timeout,
            String... arg) {
        return connector.doRun(target, fun, clientType, clazz, timeout, arg);
    }

    @Measure(SaltConnector.class)
    @Retryable(value = ClusterProxyWebApplicationException.class, backoff = @Backoff(delayExpression = RETRY_DELAY_EXPRESSION))
    public <T> T wheel(SaltConnector connector, String fun, Collection<String> match, Class<T> clazz) {
        return connector.doWheel(fun, match, clazz);
    }

    @Measure(SaltConnector.class)
    @Retryable(value = ClusterProxyWebApplicationException.class, backoff = @Backoff(delayExpression = RETRY_DELAY_EXPRESSION))
    public GenericResponses upload(SaltConnector connector, Iterable<String> targets, String path, String fileName, byte[] content) throws IOException {
        return connector.doUpload(targets, path, fileName, content);
    }

    @Measure(SaltConnector.class)
    @Retryable(value = ClusterProxyWebApplicationException.class, backoff = @Backoff(delayExpression = RETRY_DELAY_EXPRESSION))
    public GenericResponses upload(SaltConnector connector, Iterable<String> targets, String path, String fileName, String permission, byte[] content)
            throws IOException {
        return connector.doUpload(targets, path, fileName, permission, content);
    }
}
