package com.sequenceiq.periscope.rest.json;

import java.net.URL;

public class DynamicCbPolicyJson extends CloudbreakPolicyJson implements Json {

    private URL jarUrl;

    public URL getJarUrl() {
        return jarUrl;
    }

    public void setJarUrl(URL jarUrl) {
        this.jarUrl = jarUrl;
    }
}
