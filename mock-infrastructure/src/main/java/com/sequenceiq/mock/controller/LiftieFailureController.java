package com.sequenceiq.mock.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sequenceiq.mock.experience.LiftieExperienceStoreService;
import com.sequenceiq.mock.experience.response.liftie.LiftieClusterView;

@RestController
@RequestMapping("/liftie/{clusterId}/failure/")
public class LiftieFailureController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiftieFailureController.class);

    @Inject
    private LiftieExperienceStoreService liftieExperienceStoreService;

    @PostMapping(value = "commands", produces = MediaType.APPLICATION_JSON)
    public void failCommands(@PathVariable("clusterId") String clusterId) {
        LiftieClusterView liftieClusterView = liftieExperienceStoreService.getById(clusterId);
        liftieClusterView.setFailCommands(true);
    }

    @DeleteMapping(value = "commands", produces = MediaType.APPLICATION_JSON)
    public void deleteFailCommands(@PathVariable("clusterId") String clusterId) {
        LiftieClusterView liftieClusterView = liftieExperienceStoreService.getById(clusterId);
        liftieClusterView.setFailCommands(false);
    }
}
