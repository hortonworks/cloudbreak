package com.sequenceiq.cloudbreak.cloud.aws.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DistroxEnabledInstanceTypes {

    private static final String ENABLED_TYPES = "m5.xlarge,m5.2xlarge,m5.4xlarge,m5.8xlarge,m5.12xlarge,m5.16xlarge,m5.24xlarge," +
            "m5a.xlarge,m5a.2xlarge,m5a.4xlarge,m5a.8xlarge,m5a.12xlarge,m5a.16xlarge,m5a.24xlarge," +
            "m5ad.xlarge,m5ad.2xlarge,m5ad.4xlarge,m5ad.8xlarge,m5ad.12xlarge,m5ad.16xlarge,m5ad.24xlarge," +
            "m6i.xlarge,m6i.2xlarge,m6i.4xlarge,m6i.8xlarge,m6i.12xlarge,m6i.16xlarge,m6i.24xlarge,m6i.32xlarge," +
            "m6id.xlarge,m6id.2xlarge,m6id.4xlarge,m6id.8xlarge,m6id.12xlarge,m6id.16xlarge,m6id.24xlarge,m6id.32xlarge," +
            "m6a.xlarge,m6a.2xlarge,m6a.4xlarge,m6a.8xlarge,m6a.12xlarge,m6a.16xlarge,m6a.24xlarge,m6a.32xlarge,m6a.48xlarge," +
            "m5d.xlarge,m5d.2xlarge,m5d.4xlarge,m5d.8xlarge,m5d.12xlarge,m5d.16xlarge,m5d.24xlarge," +
            "m5dn.xlarge,m5dn.2xlarge,m5dn.4xlarge,m5dn.8xlarge,m5dn.12xlarge,m5dn.16xlarge,m5dn.24xlarge," +
            "r5a.2xlarge,r5a.4xlarge,r5a.8xlarge," +
            "r5ad.xlarge,r5ad.2xlarge,r5ad.4xlarge,r5ad.8xlarge,r5ad.16xlarge," +
            "c5a.2xlarge,c5a.4xlarge,c5a.8xlarge,c5a.12xlarge," +
            "r5.2xlarge,r5.4xlarge,r5.8xlarge,r5.16xlarge,c5.2xlarge,c5.4xlarge,c5.9xlarge,c5.12xlarge," +
            "i3.2xlarge,i3.4xlarge,i3.8xlarge,i3en.2xlarge,i3en.3xlarge,i3en.6xlarge,i3en.12xlarge," +
            "h1.2xlarge,h1.4xlarge,h1.8xlarge," +
            "d2.xlarge,d2.2xlarge,d2.4xlarge,d2.8xlarge," +
            "d3.xlarge,d3.2xlarge,d3.4xlarge,d3.8xlarge," +
            "d3en.2xlarge,d3en.4xlarge,d3en.6xlarge,d3en.8xlarge," +
            "r4.xlarge,r4.2xlarge,r4.4xlarge,r4.8xlarge," +
            "r5ad.12xlarge,r5ad.24xlarge," +
            "r5d.24xlarge,r5d.16xlarge,r5d.12xlarge,r5d.8xlarge,r5d.4xlarge,r5d.2xlarge,r5d.xlarge," +
            "m5n.2xlarge,m5n.4xlarge,m5n.8xlarge," +
            "r5n.2xlarge,r5n.4xlarge,r5n.8xlarge,r5n.16xlarge," +
            "r5dn.xlarge,r5dn.2xlarge,r5dn.4xlarge,r5dn.8xlarge,r5dn.16xlarge," +
            "x1e.2xlarge," +
            "i4i.2xlarge";

    public static final List<String> AWS_ENABLED_TYPES_LIST = new ArrayList<String>(Arrays.asList(ENABLED_TYPES.trim().split(",")));

    private DistroxEnabledInstanceTypes() {
    }

}
