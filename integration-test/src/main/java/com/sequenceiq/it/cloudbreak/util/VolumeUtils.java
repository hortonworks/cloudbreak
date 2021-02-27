package com.sequenceiq.it.cloudbreak.util;

import static java.lang.String.format;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

public class VolumeUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(VolumeUtils.class);

    private VolumeUtils() {
    }

    public static <T> T compareVolumeIdsAfterRepair(T t, List<String> actualVolumeIds, List<String> expectedVolumeIds) {
        actualVolumeIds.sort(Comparator.naturalOrder());
        expectedVolumeIds.sort(Comparator.naturalOrder());

        if (!actualVolumeIds.equals(expectedVolumeIds)) {
            LOGGER.error("Host Group does not have the desired volume IDs!");
            actualVolumeIds.forEach(volumeid -> Log.log(LOGGER, format(" Actual volume ID: %s ", volumeid)));
            expectedVolumeIds.forEach(volumeId -> Log.log(LOGGER, format(" Desired volume ID: %s ", volumeId)));
            throw new TestFailException("Host Group does not have the desired volume IDs!");
        } else {
            actualVolumeIds.forEach(volumeId -> Log.log(LOGGER, format(" Before and after SDX repair volume IDs are equal [%s]. ", volumeId)));
        }
        return t;
    }
}
