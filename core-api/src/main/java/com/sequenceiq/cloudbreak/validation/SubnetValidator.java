package com.sequenceiq.cloudbreak.validation;

import java.util.Arrays;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.net.util.SubnetUtils;

public class SubnetValidator implements ConstraintValidator<ValidSubnet, String> {

    @Override
    public void initialize(ValidSubnet constraintAnnotation) {
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        } else if (value.isEmpty()) {
            return false;
        }
        try {
            SubnetUtils.SubnetInfo info = new SubnetUtils(value).getInfo();
            int addr = toInt(info.getAddress());
            int lowerAddr = toInt(info.getLowAddress()) - 1;
            if (addr != lowerAddr) {
                return false;
            }
            Ip ip = new Ip(info.getAddress());
            return (new Ip("10.0.0.0").compareTo(ip) <= 0 && new Ip("10.255.255.255").compareTo(ip) >= 0)
                    || (new Ip("172.16.0.0").compareTo(ip) <= 0 && new Ip("172.31.255.255").compareTo(ip) >= 0)
                    || (new Ip("192.168.0.0").compareTo(ip) <= 0 && new Ip("192.168.255.255").compareTo(ip) >= 0);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private int toInt(String addr) {
        return Integer.parseInt(addr.replace(".", ""));
    }

    private static class Ip implements Comparable<Ip> {
        private final int[] parts;

        Ip(String ip) {
            parts = Arrays.asList(ip.split("\\.")).stream().mapToInt(Integer::parseInt).toArray();
        }

        @Override
        public int compareTo(Ip o) {
            if (this == o) {
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