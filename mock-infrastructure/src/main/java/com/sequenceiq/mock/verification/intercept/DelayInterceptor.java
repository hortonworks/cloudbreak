package com.sequenceiq.mock.verification.intercept;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.mock.config.ConfigParams;
import com.sequenceiq.mock.config.MockConfig;
import com.sequenceiq.mock.model.TestMode;

@Component
public class DelayInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelayInterceptor.class);

    private static final int HUNDRED = 100;

    @Inject
    private MockConfig mockConfig;

    private Map<String, Integer> requestCountMap = new ConcurrentHashMap<>();

    private Set<String> badClusters = ConcurrentHashMap.newKeySet();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (mockConfig.getTestMode() != TestMode.LOAD) {
            requestCountMap.clear();
            badClusters.clear();
            return true;
        }
        ConfigParams configParams = mockConfig.getConfigParams(uri);
        if (configParams != null) {
            Integer errorRate = configParams.getErrorRate();
            String uriPattern = configParams.getUriPattern();
            if (errorRate != null && errorRate > 0) {
                requestCountMap.put(uriPattern, requestCountMap.getOrDefault(uriPattern, 0) + 1);
                if (requestCountMap.get(uriPattern) % (HUNDRED / errorRate) == 0) {
                    LOGGER.error("Processing failed due to {} percent failure rate configured for {}", errorRate, uri);
                    throw new BadRequestException(String.format("Processing failed due to %d percent failure rate configured", errorRate));
                }
            }
            String delay = configParams.getDelay();
            if (StringUtils.isNotEmpty(delay)) {
                LOGGER.info("Add delay of {} seconds for {}", delay, uri);
                introduceDelay(delay, uri);
            }
            introduceLongDelay(uri, uriPattern);
        }
        return true;
    }

    private Integer getRandomDelay(String delay) {
        if (delay.indexOf("-") == -1) {
            return Integer.valueOf(delay);
        } else {
            List<Integer> delayRange =  Arrays.stream(delay.split("-")).map(Integer::valueOf).collect(Collectors.toList());
            return new Random().nextInt(delayRange.get(1) + 1 - delayRange.get(0)) + delayRange.get(0);
        }
    }

    private String parseMockUuid(String requestUri) {
        String toSplit = requestUri;
        if (toSplit.startsWith("/")) {
            toSplit = toSplit.replaceFirst("/", "");
        }
        String[] split = toSplit.split("/");
        if (Crn.isCrn(split[0])) {
            return split[0];
        }
        return requestUri;
    }

    private void introduceLongDelay(String uri, String uriPattern) throws Exception {
        String clusterCrn = parseMockUuid(uri);
        Crn crn = Crn.fromString(clusterCrn);
        if (badClusters.size() < mockConfig.getBadClustersConfig().getNumBadClusters() && crn != null && crn.getResourceType() == Crn.ResourceType.CLUSTER) {
            badClusters.add(clusterCrn);
        } else if (badClusters.size() > mockConfig.getBadClustersConfig().getNumBadClusters()) {
            LOGGER.info("NumBadClusters config is updated. reset the list for bad clusters");
            badClusters.clear();
        }
        if (badClusters.contains(clusterCrn) && mockConfig.getBadClustersConfig().getUrisForLongDelay().contains(uriPattern)) {
            String longDelay = mockConfig.getBadClustersConfig().getLongDelayInSecs();
            LOGGER.info("Add additional long delay of {} seconds for bad cluster {} and uri {}", longDelay, clusterCrn, uri);
            introduceDelay(longDelay, uri);
        }
    }

    private void introduceDelay(String delay, String uri) throws Exception {
        Integer randomLongDelay = getRandomDelay(delay);
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            LOGGER.info("Delay of {} seconds is over for uri {}", randomLongDelay, uri);
        }, randomLongDelay, TimeUnit.SECONDS).get();
    }
}
