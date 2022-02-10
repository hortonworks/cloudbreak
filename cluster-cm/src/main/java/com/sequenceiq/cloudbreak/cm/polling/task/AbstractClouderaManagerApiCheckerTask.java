package com.sequenceiq.cloudbreak.cm.polling.task;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.cloudera.api.swagger.client.ApiException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterBasedStatusCheckerTask;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;

public abstract class AbstractClouderaManagerApiCheckerTask<T extends ClouderaManagerPollerObject> extends ClusterBasedStatusCheckerTask<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClouderaManagerApiCheckerTask.class);

    private static final int TOLERATED_ERROR_LIMIT = 5;

    //CHECKSTYLE:OFF
    protected final ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    protected int toleratedErrorCounter = 0;

    private final ClusterEventService clusterEventService;

    private boolean connectExceptionOccurred = false;
    //CHECKSTYLE:ON

    protected AbstractClouderaManagerApiCheckerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService) {
        this.clouderaManagerApiPojoFactory = clouderaManagerApiPojoFactory;
        this.clusterEventService = clusterEventService;
    }

    @Override
    public final boolean checkStatus(T pollerObject) {
        try {
            return doStatusCheck(pollerObject);
        } catch (ApiException e) {
            return handleApiException(pollerObject, e);
        }
    }

    private boolean handleApiException(T pollerObject, ApiException e) {
        if (e.getCode() == HttpStatus.BAD_GATEWAY.value()) {
            LOGGER.debug("Cloudera Manager is not (yet) available.", e);
            return false;
        } else if (e.getCause() instanceof ConnectException) {
            return handleConnectException(pollerObject, e);
        } else if (isToleratedError(e)) {
            return handleToleratedError(pollerObject, e);
        } else {
            throw new ClouderaManagerOperationFailedException(String.format("Cloudera Manager [%s] operation failed. %s", getPollingName(), e.getMessage()), e);
        }
    }

    private boolean handleConnectException(T pollerObject, ApiException e) {
        LOGGER.warn("{}. Notification is sent to the UI.", getErrorMessage(pollerObject, e), e.getMessage());
        if (!connectExceptionOccurred) {
            connectExceptionOccurred = true;
            Stack stack = pollerObject.getStack();
            clusterEventService.fireCloudbreakEvent(stack, ResourceEvent.CLUSTER_CM_SECURITY_GROUP_TOO_STRICT, List.of(e.getMessage()));
        }
        return false;
    }

    private boolean handleToleratedError(T pollerObject, ApiException e) {
        if (toleratedErrorCounter < TOLERATED_ERROR_LIMIT) {
            toleratedErrorCounter++;
            LOGGER.warn("{}. Tolerating till {} occasions.", getToleratedErrorMessage(pollerObject, e), TOLERATED_ERROR_LIMIT, e);
            return false;
        } else {
            throw new ClouderaManagerOperationFailedException(
                    String.format("{}. Operation is considered failed.", getToleratedErrorMessage(pollerObject, e)), e);
        }
    }

    private boolean isToleratedError(ApiException e) {
        // Retry for BAD_REQUEST is not ideal, but sometimes CM sends back BAD_REQUESTS even for INTERNAL_SERVER_ERROR
        return e.getCode() == HttpStatus.INTERNAL_SERVER_ERROR.value()
                || e.getCode() == HttpStatus.BAD_REQUEST.value()
                || e.getCause() instanceof SocketException
                || e.getCause() instanceof IOException
                || e.getCause() instanceof SocketTimeoutException;
    }

    public ClusterEventService getClusterEventService() {
        return clusterEventService;
    }

    protected abstract boolean doStatusCheck(T pollerObject) throws ApiException;

    protected abstract String getPollingName();

    protected String getToleratedErrorMessage(T pollerObject, ApiException e) {
        return String.format("API checking [%s] failed with a tolerated error '%s' for the %s. time(s).",
                getPollingName(), e.getMessage(), toleratedErrorCounter);
    }

    protected String getErrorMessage(T pollerObject, ApiException e) {
        return String.format("API checking [%s] failed with a %s.",
                getPollingName(), e.getClass().getSimpleName());
    }

    @Override
    public void handleTimeout(T t) {
        throw new ClouderaManagerOperationFailedException(String.format("Polling of [%s] timed out.",
                getPollingName()));
    }

    @Override
    public String successMessage(T t) {
        return String.format("Cloudera Manager API checking [%s] was a success", getPollingName());
    }
}
