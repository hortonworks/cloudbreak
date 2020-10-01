package com.sequenceiq.cloudbreak.cm.polling.task;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;

public class ApiCommandUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiCommandUtil.class);

    private ApiCommandUtil() {
    }

    public static List<String> getFailedCommandMessages(ApiCommandList apiCommandList) {
        List<ApiCommand> failedCommands = getFailedCommands(apiCommandList);
        LOGGER.debug("Failed commands: {}", failedCommands);
        return failedCommands.stream().map(ApiCommandUtil::formatToLine).collect(toList());
    }

    private static List<ApiCommand> getFailedCommands(ApiCommandList apiCommandList) {
        if (apiCommandList == null || CollectionUtils.isEmpty(apiCommandList.getItems())) {
            return List.of();
        }
        List<ApiCommand> failedCommands = new ArrayList<>();
        for (ApiCommand command : apiCommandList.getItems()) {
            if (Boolean.FALSE.equals(command.getActive()) && Boolean.FALSE.equals(command.getSuccess())) {
                failedCommands.add(command);
                failedCommands.addAll(getFailedCommands(command.getChildren()));
            }
        }
        return failedCommands;
    }

    private static String formatToLine(ApiCommand s) {
        String ret = "";
        if (s != null) {
            ret += s.getName();
            if (s.getServiceRef() != null) {
                ret += '(' + s.getServiceRef().getServiceName() + "): ";
            }
            ret += s.getResultMessage();
        }
        return ret;
    }
}
