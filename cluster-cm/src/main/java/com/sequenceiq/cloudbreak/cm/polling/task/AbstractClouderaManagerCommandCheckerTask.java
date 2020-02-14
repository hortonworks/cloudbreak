package com.sequenceiq.cloudbreak.cm.polling.task;

import java.net.ConnectException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cluster.service.ClusterBasedStatusCheckerTask;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

public abstract class AbstractClouderaManagerCommandCheckerTask<T extends ClouderaManagerPollerObject> extends ClusterBasedStatusCheckerTask<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClouderaManagerCommandCheckerTask.class);

    private static final int TOLERATED_ERROR_LIMIT = 5;

    //CHECKSTYLE:OFF
    protected final ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    private final CloudbreakEventService cloudbreakEventService;

    private int toleratedErrorCounter = 0;

    private boolean connectExceptionOccurred = false;
    //CHECKSTYLE:ON

    protected AbstractClouderaManagerCommandCheckerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            CloudbreakEventService cloudbreakEventService) {
        this.clouderaManagerApiPojoFactory = clouderaManagerApiPojoFactory;
        this.cloudbreakEventService = cloudbreakEventService;
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
            throw new ClouderaManagerOperationFailedException(String.format("Cloudera Manager [%s] operation  failed.", getCommandName()), e);
        }
    }

    private boolean handleConnectException(T pollerObject, ApiException e) {
        LOGGER.warn("Command [{}] with id [{}] failed with a ConnectException '{}'. Notification is sent to the UI.",
                getCommandName(), pollerObject.getId(), e.getMessage());
        if (!connectExceptionOccurred) {
            connectExceptionOccurred = true;
            Stack stack = pollerObject.getStack();
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), stack.getStatus().name(),
                    ResourceEvent.CLUSTER_CM_SECURITY_GROUP_TOO_STRICT, List.of(e.getMessage()));
        }
        return false;
    }

    private boolean handleToleratedError(T pollerObject, ApiException e) {
        if (toleratedErrorCounter < TOLERATED_ERROR_LIMIT) {
            toleratedErrorCounter++;
            LOGGER.warn("Command [{}] with id [{}] failed with a tolerated error '{}' for the {}. time(s). Tolerating till {} occasions.",
                    getCommandName(), pollerObject.getId(), e.getMessage(), toleratedErrorCounter, TOLERATED_ERROR_LIMIT);
            return false;
        } else {
            throw new ClouderaManagerOperationFailedException(
                    String.format("Command [%s] with id [%s] failed with a tolerated error '%s' for %s times. Operation is considered failed.",
                            getCommandName(), pollerObject.getId(), e.getMessage(), TOLERATED_ERROR_LIMIT));
        }
    }

    private boolean isToleratedError(ApiException e) {
        return e.getCode() == HttpStatus.INTERNAL_SERVER_ERROR.value() || e.getCause() instanceof SocketException;
    }

    protected boolean doStatusCheck(T pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {
        ApiCommand apiCommand = commandsResourceApi.readCommand(pollerObject.getId());
        if (apiCommand.getActive()) {
            LOGGER.debug("Command [" + getCommandName() + "] with id [" + pollerObject.getId() + "] is active, so it hasn't finished yet");
            return false;
        } else if (apiCommand.getSuccess()) {
            return true;
        } else {
            String resultMessage = apiCommand.getResultMessage();
            List<String> detailedMessages = parseResultMessageFromChildren(apiCommand.getChildren());
            String message = "Command [" + getCommandName() + "] failed: " + resultMessage + ". Detailed messages: " + String.join("\n", detailedMessages);
            LOGGER.info(message);
            throw new ClouderaManagerOperationFailedException(message);
        }
    }

    @VisibleForTesting
    List<String> parseResultMessageFromChildren(ApiCommandList apiCommandList) {
        if (CollectionUtils.isEmpty(apiCommandList.getItems())) {
            return Collections.emptyList();
        }
        List<String> ret = new ArrayList<>();
        apiCommandList.getItems().forEach(s -> {
            if (s.getChildren() == null) {
                ret.add(formatToLine(s));
            } else {
                ret.add(formatToLine(s));
                ret.addAll(parseResultMessageFromChildren(s.getChildren()));
            }
        });
        return ret;
    }

    private String formatToLine(ApiCommand s) {
        return s.getName() + "(" + s.getServiceRef().getServiceName() + "): " + s.getResultMessage();
    }

    protected abstract String getCommandName();
}
