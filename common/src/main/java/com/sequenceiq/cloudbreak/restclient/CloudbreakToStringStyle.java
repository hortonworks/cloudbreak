package com.sequenceiq.cloudbreak.restclient;

import org.apache.commons.lang3.builder.StandardToStringStyle;

public class CloudbreakToStringStyle extends StandardToStringStyle {

    public static final CloudbreakToStringStyle INSTANCE = new CloudbreakToStringStyle();

    private CloudbreakToStringStyle() {
        setUseShortClassName(true);
        setUseIdentityHashCode(false);
        setUseFieldNames(true);
        setContentStart("{");
        setContentEnd("}");
        setFieldSeparator(", ");
        setFieldNameValueSeparator("=");
    }

    public static CloudbreakToStringStyle getInstance() {
        return INSTANCE;
    }
}
