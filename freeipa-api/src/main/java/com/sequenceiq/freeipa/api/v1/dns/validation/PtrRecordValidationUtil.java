package com.sequenceiq.freeipa.api.v1.dns.validation;

import java.util.Arrays;
import java.util.List;

import jakarta.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.common.api.util.ValidatorUtil;

public class PtrRecordValidationUtil {
    private PtrRecordValidationUtil() {
    }

    static boolean isIpInZoneRange(String ip, String zone, ConstraintValidatorContext constraintValidatorContext) {
        if (StringUtils.isEmpty(zone)) {
            return true;
        } else if (StringUtils.isNotEmpty(ip)) {
            String modifiedZone = StringUtils.appendIfMissing(zone, ".");
            List<String> zoneStart = Arrays.asList(StringUtils.removeEnd(modifiedZone, ".in-addr.arpa.").split("\\.")).reversed();
            List<String> ipParts = Arrays.asList(ip.split("\\."));
            List<String> ipZoneParts = ipParts.subList(0, Math.min(zoneStart.size(), ipParts.size()));
            if (ipZoneParts.equals(zoneStart)) {
                return true;
            } else {
                ValidatorUtil.addConstraintViolation(constraintValidatorContext, String.format("Ip %s is not in the provided reverse dns zone: %s", ip, zone));
                return false;
            }
        } else {
            ValidatorUtil.addConstraintViolation(constraintValidatorContext, "Ip parameter is missing.");
            return false;
        }
    }
}
