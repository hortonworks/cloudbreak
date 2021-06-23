package com.sequenceiq.cloudbreak.cm.polling.task;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
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

    private final ClusterEventService clusterEventService;

    private int toleratedErrorCounter = 0;

    private boolean connectExceptionOccurred = false;
    //CHECKSTYLE:ON

    protected AbstractClouderaManagerApiCheckerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService) {
        this.clouderaManagerApiPojoFactory = clouderaManagerApiPojoFactory;
        this.clusterEventService = clusterEventService;
    }

    @Override
    public final boolean checkStatus(T pollerObject) {
        ApiClient apiClient = pollerObject.getApiClient();
        CommandsResourceApi commandsResourceApi = clouderaManagerApiPojoFactory.getCommandsResourceApi(apiClient);
        try {
            return doStatusCheck(pollerObject, commandsResourceApi);
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
            throw new ClouderaManagerOperationFailedException(String.format("Cloudera Manager [%s] operation failed. %s", getCommandName(), e.getMessage()), e);
        }
    }

    private boolean handleConnectException(T pollerObject, ApiException e) {
        LOGGER.warn("Command [{}] with id [{}] failed with a ConnectException '{}'. Notification is sent to the UI.",
                getCommandName(), getOperationIdentifier(pollerObject), e.getMessage());
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
            LOGGER.warn("Command [{}] with id [{}] failed with a tolerated error '{}' for the {}. time(s). Tolerating till {} occasions.",
                    getCommandName(), getOperationIdentifier(pollerObject), e.getMessage(), toleratedErrorCounter, TOLERATED_ERROR_LIMIT, e);
            return false;
        } else {
            throw new ClouderaManagerOperationFailedException(
                    String.format("Command [%s] with id [%s] failed with a tolerated error '%s' for %s times. Operation is considered failed.",
                            getCommandName(), getOperationIdentifier(pollerObject), e.getMessage(), TOLERATED_ERROR_LIMIT), e);
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

    protected abstract boolean doStatusCheck(T pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException;

    protected abstract String getCommandName();

    protected abstract String getOperationIdentifier(T pollerObject);
}
