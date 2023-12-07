package com.sequenceiq.mock.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sequenceiq.mock.clouderamanager.DataProviderService;
import com.sequenceiq.mock.config.MockConfig;
import com.sequenceiq.mock.model.RecommendationType;
import com.sequenceiq.mock.model.YarnRecommendation;
import com.sequenceiq.mock.model.YarnScalingServiceV1Request;
import com.sequenceiq.mock.model.YarnScalingServiceV1Response;
import com.sequenceiq.mock.swagger.model.ApiHostList;

@Controller
@RequestMapping(value = "/{mockUuid}/resourcemanager/v1/cluster")
public class YarnController {

    private static final Logger LOGGER = LoggerFactory.getLogger(YarnController.class);

    private static final String ACTION_TYPE_VERIFY = "verify";

    private static final int UPSCALE_NODE_COUNT = 100;

    private static final int RANDOM_WINDOW_INITIAL_RECOMMENDATION_TS = 30;

    private Map<String, YarnRecommendation> stackToOperationMap = new HashMap<>();

    @Inject
    private DataProviderService dataProviderService;

    @Inject
    private MockConfig mockConfig;

    @PostMapping("/scaling")
    public ResponseEntity<YarnScalingServiceV1Response> getYarnMetrics(@PathVariable("mockUuid") String mockUuid,
            @QueryParam("actionType") Optional<String> actionType, @RequestBody YarnScalingServiceV1Request yarnScalingServiceV1Request) {
        LOGGER.info("getYarnMetrics called with {} and {}", mockUuid, yarnScalingServiceV1Request);
        LOGGER.info("actionType is {}", actionType.orElse(null));
        YarnScalingServiceV1Response yarnScalingServiceV1Response = new YarnScalingServiceV1Response();
        YarnRecommendation lastRecommendation = stackToOperationMap.get(mockUuid);
        Long yarnRecommendationIntervalInMillis = getYarnRecommendationIntervalInMillis();
        if (lastRecommendation == null || ACTION_TYPE_VERIFY.equalsIgnoreCase(actionType.orElse(null))
                || System.currentTimeMillis() - yarnRecommendationIntervalInMillis >= lastRecommendation.getLastRecommendedTs()) {
            if (lastRecommendation == null) {
                lastRecommendation = new YarnRecommendation(RecommendationType.UPSCALE,
                        System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(new Random().nextInt(RANDOM_WINDOW_INITIAL_RECOMMENDATION_TS)));
            }
            YarnRecommendation nextRecommendation = new YarnRecommendation(lastRecommendation.getRecommendationType().toggle(), System.currentTimeMillis());
            if (ACTION_TYPE_VERIFY.equalsIgnoreCase(actionType.orElse(null))) {
                LOGGER.info("actionType is verify so use lastRecommendation as {}", lastRecommendation);
                nextRecommendation = lastRecommendation;
            }
            if (nextRecommendation.getRecommendationType() == RecommendationType.UPSCALE) {
                yarnScalingServiceV1Response = populateUpscale();
            } else {
                yarnScalingServiceV1Response = populateDownscale(mockUuid);
            }
            stackToOperationMap.put(mockUuid, nextRecommendation);
        } else {
            LOGGER.info("No recommendation for Upscale/Downscale since recommended interval of {} has not elapsed since last recommendation at {} for {}",
                    yarnRecommendationIntervalInMillis, lastRecommendation.getLastRecommendedTs(), mockUuid);
        }
        return new ResponseEntity<>(yarnScalingServiceV1Response, HttpStatus.OK);
    }

    private YarnScalingServiceV1Response populateUpscale() {
        YarnScalingServiceV1Response yarnScalingServiceV1Response = new YarnScalingServiceV1Response();
        YarnScalingServiceV1Response.NewNodeManagerCandidates newNodeManagerCandidates = new YarnScalingServiceV1Response.NewNodeManagerCandidates();
        YarnScalingServiceV1Response.NewNodeManagerCandidates.Candidate candidate = new YarnScalingServiceV1Response.NewNodeManagerCandidates.Candidate();
        candidate.setModelName("compute");
        candidate.setCount(UPSCALE_NODE_COUNT);
        newNodeManagerCandidates.setCandidates(List.of(candidate));
        yarnScalingServiceV1Response.setNewNMCandidates(newNodeManagerCandidates);
        return yarnScalingServiceV1Response;
    }

    private YarnScalingServiceV1Response populateDownscale(String stackCrn) {
        YarnScalingServiceV1Response yarnScalingServiceV1Response = new YarnScalingServiceV1Response();
        ApiHostList apiHostList = dataProviderService.readHosts(stackCrn);
        List<YarnScalingServiceV1Response.DecommissionCandidate> decommissionCandidates = apiHostList.getItems().stream().map(apiHost -> {
            YarnScalingServiceV1Response.DecommissionCandidate decommissionCandidate = new YarnScalingServiceV1Response.DecommissionCandidate();
            decommissionCandidate.setNodeId(apiHost.getHostname());
            return decommissionCandidate;
            }).collect(Collectors.toList());
        yarnScalingServiceV1Response.setDecommissionCandidates(Map.of(YarnScalingServiceV1Response.YARN_RESPONSE_DECOMMISSION_CANDIDATES_KEY,
                decommissionCandidates));
        return yarnScalingServiceV1Response;
    }

    private Long getYarnRecommendationIntervalInMillis() {
        String yarnRecommendationInterval = mockConfig.getYarnRecommendationInterval();
        if (yarnRecommendationInterval == null) {
            return 0L;
        }
        Integer yarnRecommendedInterval;
        if (yarnRecommendationInterval.indexOf("-") == -1) {
            yarnRecommendedInterval =  Integer.valueOf(yarnRecommendationInterval);
        } else {
            List<Integer> delayRange =  Arrays.stream(yarnRecommendationInterval.split("-")).map(Integer::valueOf).collect(Collectors.toList());
            yarnRecommendedInterval = new Random().nextInt(delayRange.get(1) + 1 - delayRange.get(0)) + delayRange.get(0);
        }
        return TimeUnit.MINUTES.toMillis(yarnRecommendedInterval);
    }
}
