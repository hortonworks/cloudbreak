package com.sequenceiq.cloudbreak.orchestrator.yarn.util;

import java.util.Locale;

import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;

public final class DomainUtils {

    private static final String DEV_DOMAIN = "root.hwxdev.site";

    private static final String DEV_ENDPOINT_SEARCH = "cn008.l42scl.hortonworks.com";

    private static final String INT_DOMAIN = "root.hwxint.site";

    private static final String INT_ENDPOINT_SEARCH = "yint01.l42scl.hortonworks.com";

    private static final String STAGE_DOMAIN = "root.hwxstg.site";

    private static final String STAGE_ENDPOINT_SEARCH = "y002.l42scl.hortonworks.com";

    private static final String PROD_DOMAIN = "root.hwx.site";

    private static final String PROD_ENDPOINT_SEARCH = "yprod001.l42scl.hortonworks.com";

    private DomainUtils() {

    }

    public static String getDomain(OrchestrationCredential cred) {
        if (cred.getApiEndpoint().contains(DEV_ENDPOINT_SEARCH.toLowerCase(Locale.ROOT))) {
            return DEV_DOMAIN;
        } else if (cred.getApiEndpoint().contains(INT_ENDPOINT_SEARCH.toLowerCase(Locale.ROOT))) {
            return INT_DOMAIN;
        } else if (cred.getApiEndpoint().contains(STAGE_ENDPOINT_SEARCH.toLowerCase(Locale.ROOT))) {
            return STAGE_DOMAIN;
        } else if (cred.getApiEndpoint().contains(PROD_ENDPOINT_SEARCH.toLowerCase(Locale.ROOT))) {
            return PROD_DOMAIN;
        } else {
            return "";
        }
    }
}
