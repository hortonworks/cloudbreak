package com.sequenceiq.mock.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.mock.service.FailureService;

@RestController
@RequestMapping("/{mock_uuid}/failure/")
public class FailureController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FailureController.class);

    @Inject
    private FailureService failureService;

    @PostMapping(value = "salt_run", produces = MediaType.APPLICATION_JSON)
    public void setRunFailure(@PathVariable("mock_uuid") String mockUuid, @RequestParam("run_arg") String runArg) {
        failureService.setFailure(mockUuid, runArg);
        LOGGER.trace("Set failure for {} when arg is {}", mockUuid, runArg);
    }

    @DeleteMapping(value = "salt_run", produces = MediaType.APPLICATION_JSON)
    public void deleteRunFailure(@PathVariable("mock_uuid") String mockUuid, @RequestParam("run_arg") String runArg) {
        failureService.deleteFailure(mockUuid, runArg);
        LOGGER.trace("Removed failure for {} when arg is {}", mockUuid, runArg);
    }

    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON)
    public void setRunFailureCount(@PathVariable("mock_uuid") String mockUuid, @RequestParam("run_arg") String runArg, @RequestParam("group") String group,
            @RequestParam("all_node_count") int allNodeCount, @RequestParam("failed_node_count") int failedNodeCount) {
        failureService.scheduleNodeFailureOnCommand(mockUuid, runArg, group, allNodeCount, failedNodeCount);
    }
}
