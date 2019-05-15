package com.sequenceiq.cloudbreak.validation;

import java.util.Arrays;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class SubnetValidator implements ConstraintValidator<ValidSubnet, String> {

    private SubnetType subnetType;

    @Override
    public void initialize(ValidSubnet constraintAnnotation) {
        subnetType = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        } else if (value.isEmpty()) {
            return false;
        }
        try {
            SubnetInfo info = new SubnetUtils(value).getInfo();
            if (!info.getAddress().equals(info.getNetworkAddress())) {
                return false;
            }
            if (subnetType.equals(SubnetType.RFC_1918_COMPLIANT_ONLY)) {
                return isRfc1918CompliantSubnet(info);
            }
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private boolean isRfc1918CompliantSubnet(SubnetInfo info) {
        Ip ip = new Ip(info.getAddress());
        long addr = toLong(info.getAddress());
        long lowerAddr = toLong(info.getLowAddress()) - 1;
        if (addr != lowerAddr) {
            return false;
        }
        return (new Ip("10.0.0.0").compareTo(ip) <= 0 && new Ip("10.255.255.255").compareTo(ip) >= 0)
                || (new Ip("172.16.0.0").compareTo(ip) <= 0 && new Ip("172.31.255.255").compareTo(ip) >= 0)
                || (new Ip("192.168.0.0").compareTo(ip) <= 0 && new Ip("192.168.255.255").compareTo(ip) >= 0);
    }

    private long toLong(String addr) {
        return Long.parseLong(addr.replace(".", ""));
    }

    private static class Ip implements Comparable<Ip> {
        private final int[] parts;

        Ip(String ip) {
            parts = Arrays.stream(ip.split("\\.")).mapToInt(Integer::parseInt).toArray();
        }

        @SuppressFBWarnings("EQ_COMPARETO_USE_OBJECT_EQUALS")
        @Override
        public int compareTo(Ip o) {
            if (equals(o)) {
                return 0;
            }
            for (int i = 0; i < parts.length; i++) {
                if (parts[i] < o.parts[i]) {
                    return -1;
                } else if (parts[i] > o.parts[i]) {
                    return 1;
                }
            }
            return 0;
        }
    }
}