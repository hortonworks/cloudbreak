package com.sequenceiq.cloudbreak.service.lifetime;

import java.util.Calendar;

import org.springframework.stereotype.Service;

@Service
public class TtlValidatorService {

    private static final Long EXTREMAL_VALUE = 0L;

    public boolean isClusterTtlExceeded(Long created, Long clusterTimeToLive) {
        if (needToValidateField(clusterTimeToLive)) {
            long now = getTimeInMillis();
            long clusterRunningTime = now - created;
            return clusterRunningTime > clusterTimeToLive;
        }
        return false;
    }

    private boolean needToValidateField(Long field) {
        return field != null && !EXTREMAL_VALUE.equals(field);
    }

    long getTimeInMillis() {
        return Calendar.getInstance().getTimeInMillis();
    }
}
