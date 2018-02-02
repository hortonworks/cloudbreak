package com.sequenceiq.cloudbreak.cloud.openstack.common;

import java.text.DecimalFormat;

public class ConversionUtil {

    public static final int SHIFT_NUMBER = 1024;

    private ConversionUtil() {
    }

    public static String convertToGB(String sizeInMB) {
        float size = Float.parseFloat(sizeInMB);
        DecimalFormat formatter = new DecimalFormat("#0.0####");
        return formatter.format(size / SHIFT_NUMBER);
    }
}
