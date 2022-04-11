package com.sequenceiq.cloudbreak.filter;

public class FilterOrderConstants {

    public static final int SPAN_FINISHING_ORDER = Integer.MIN_VALUE;

    public static final int MDC_REQUEST_ID_FILTER_ORDER = 0;

    public static final int CRN_FILTER_ORDER = 1;

    public static final int CLOUDBREAK_USER_CONFIGURATOR_ORDER = 2;

    public static final int MDC_FILTER_ORDER = 3;

    public static final int WORKSPACE_FILTER_ORDER = 4;

    public static final int AUDIT_FILTER_ORDER = 5;

    public static final int REQUEST_RESPONSE_LOGGER_FILTER_ORDER = 6;

    private FilterOrderConstants() {
    }

}
