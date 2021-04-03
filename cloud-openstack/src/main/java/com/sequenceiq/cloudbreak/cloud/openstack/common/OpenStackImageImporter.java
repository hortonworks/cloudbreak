package com.sequenceiq.cloudbreak.cloud.openstack.common;

import java.util.Map;

import javax.inject.Inject;

import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.image.v2.Task;
import org.openstack4j.model.image.v2.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.common.service.url.UrlAccessValidationService;

/**
 * Import image doc: http://www.openstack4j.com/learn/image-v2
 */
@Component
public class OpenStackImageImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackImageImporter.class);

    @Inject
    private OpenStackImageImportTaskParameters openStackImageImportTaskParameters;

    @Inject
    private UrlAccessValidationService urlAccessValidationService;

    public void importImage(OSClient osClient, String name) {

        String importLocation = openStackImageImportTaskParameters.getImportLocation(name);
        LOGGER.info("Import OpenStack image from: {}", importLocation);
        if (!urlAccessValidationService.isAccessible(importLocation, null)) {
            throw new CloudConnectorException(String.format("OpenStack image '%s' is not accessible, therefore it cannot be imported automatically",
                    importLocation));
        }

        Map<String, Object> input = openStackImageImportTaskParameters.buildInput(name);
        LOGGER.info("Executing of the following import Task: {}", input);
        Task task = osClient.imagesV2().tasks().create(Builders.taskBuilder().type("import").input(input).build());
        evaluateTaskStatus(task, name);
        LOGGER.info("Task of importing {} image, returned with {}", name, task.getStatus());
    }

    private void evaluateTaskStatus(Task task, String name) {
        if (task != null) {
            TaskStatus status = task.getStatus();
            LOGGER.info("Task status: {}", status);

            if (status == null) {
                throw new CloudConnectorException(String.format("Import of %s did not return any status, message: %s", name, task.getMessage()));

            } else if (status == TaskStatus.FAILURE || status == TaskStatus.UNKNOWN) {
                throw new CloudConnectorException(String.format("Import of %s failed with status: %s, message: %s", name, status, task.getMessage()));
            }
        } else {
            throw new CloudConnectorException(String.format("Import of %s did not return any task or status object", name));
        }
    }
}
