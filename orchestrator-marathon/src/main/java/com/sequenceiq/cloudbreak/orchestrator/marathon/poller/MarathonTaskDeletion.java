package com.sequenceiq.cloudbreak.orchestrator.marathon.poller;

import java.util.Collection;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;

import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.model.v2.Task;
import mesosphere.marathon.client.utils.MarathonException;

public class MarathonTaskDeletion implements OrchestratorBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarathonAppDeletion.class);
    private static final Integer STATUS_NOT_FOUND = 404;

    private final Marathon client;
    private final String appId;
    private final Set<String> taskIds;

    public MarathonTaskDeletion(Marathon client, String appId, Set<String> taskIds) {
        this.client = client;
        this.appId = appId;
        this.taskIds = taskIds;
    }

    @Override
    public Boolean call() throws Exception {
        try {
            Collection<Task> tasks = client.getApp(appId).getApp().getTasks();
            for (Task task : tasks) {
                if (taskIds.contains(task.getId())) {
                    throw new CloudbreakOrchestratorFailedException(String.format("Task '%s' hasn't been deleted yet.", task.getId()));
                }
            }
        } catch (MarathonException me) {
            if (STATUS_NOT_FOUND.equals(me.getStatus())) {
                LOGGER.info("Marathon app '{}' has already been deleted.", appId);
            } else {
                throw new CloudbreakOrchestratorFailedException(me);
            }
        }
        return true;
    }
}

