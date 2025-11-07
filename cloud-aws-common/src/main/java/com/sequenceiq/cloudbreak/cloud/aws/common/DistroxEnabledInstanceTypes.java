package com.sequenceiq.cloudbreak.cloud.aws.common;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class DistroxEnabledInstanceTypes {

    private static final String ENABLED_X86_TYPES =
            "h1.2xlarge,h1.4xlarge,h1.8xlarge," +
            "x1e.2xlarge," +

            "d2.xlarge,d2.2xlarge,d2.4xlarge,d2.8xlarge," +

            "d3.xlarge,d3.2xlarge,d3.4xlarge,d3.8xlarge," +
            "d3en.2xlarge,d3en.4xlarge,d3en.6xlarge,d3en.8xlarge," +
            "i3.2xlarge,i3.4xlarge,i3.8xlarge,i3en.2xlarge,i3en.3xlarge,i3en.6xlarge,i3en.12xlarge," +

            "i4i.xlarge,i4i.2xlarge,i4i.4xlarge,i4i.8xlarge,i4i.12xlarge,i4i.16xlarge,i4i.24xlarge,i4i.32xlarge," +
            "r4.xlarge,r4.2xlarge,r4.4xlarge,r4.8xlarge," +

            "m5.xlarge,m5.2xlarge,m5.4xlarge,m5.8xlarge,m5.12xlarge,m5.16xlarge,m5.24xlarge," +
            "m5a.xlarge,m5a.2xlarge,m5a.4xlarge,m5a.8xlarge,m5a.12xlarge,m5a.16xlarge,m5a.24xlarge," +
            "m5ad.xlarge,m5ad.2xlarge,m5ad.4xlarge,m5ad.8xlarge,m5ad.12xlarge,m5ad.16xlarge,m5ad.24xlarge," +
            "m5d.xlarge,m5d.2xlarge,m5d.4xlarge,m5d.8xlarge,m5d.12xlarge,m5d.16xlarge,m5d.24xlarge," +
            "m5n.2xlarge,m5n.4xlarge,m5n.8xlarge," +
            "m5dn.xlarge,m5dn.2xlarge,m5dn.4xlarge,m5dn.8xlarge,m5dn.12xlarge,m5dn.16xlarge,m5dn.24xlarge," +
            "r5.2xlarge,r5.4xlarge,r5.8xlarge,r5.16xlarge,c5.2xlarge,c5.4xlarge,c5.9xlarge,c5.12xlarge," +
            "r5a.2xlarge,r5a.4xlarge,r5a.8xlarge," +
            "r5d.24xlarge,r5d.16xlarge,r5d.12xlarge,r5d.8xlarge,r5d.4xlarge,r5d.2xlarge,r5d.xlarge," +
            "r5ad.xlarge,r5ad.2xlarge,r5ad.4xlarge,r5ad.8xlarge,r5ad.12xlarge,r5ad.16xlarge,r5ad.24xlarge," +
            "r5n.2xlarge,r5n.4xlarge,r5n.8xlarge,r5n.16xlarge," +
            "r5dn.xlarge,r5dn.2xlarge,r5dn.4xlarge,r5dn.8xlarge,r5dn.16xlarge," +
            "c5a.2xlarge,c5a.4xlarge,c5a.8xlarge,c5a.12xlarge," +

            "m6i.xlarge,m6i.2xlarge,m6i.4xlarge,m6i.8xlarge,m6i.12xlarge,m6i.16xlarge,m6i.24xlarge,m6i.32xlarge," +
            "m6in.xlarge,m6in.2xlarge,m6in.4xlarge,m6in.8xlarge,m6in.12xlarge,m6in.16xlarge,m6in.24xlarge,m6in.32xlarge,"  +
            "m6idn.xlarge,m6idn.2xlarge,m6idn.4xlarge,m6idn.8xlarge,m6idn.12xlarge,m6idn.16xlarge,m6idn.24xlarge,m6idn.32xlarge," +
            "m6id.xlarge,m6id.2xlarge,m6id.4xlarge,m6id.8xlarge,m6id.12xlarge,m6id.16xlarge,m6id.24xlarge,m6id.32xlarge," +
            "m6a.xlarge,m6a.2xlarge,m6a.4xlarge,m6a.8xlarge,m6a.12xlarge,m6a.16xlarge,m6a.24xlarge,m6a.32xlarge,m6a.48xlarge," +
            "r6a.xlarge,r6a.2xlarge,r6a.4xlarge,r6a.8xlarge,r6a.12xlarge,r6a.16xlarge,r6a.24xlarge,r6a.32xlarge," +
            "r6i.xlarge,r6i.2xlarge,r6i.4xlarge,r6i.8xlarge,r6i.12xlarge,r6i.16xlarge,r6i.24xlarge,r6i.32xlarge," +
            "r6id.xlarge,r6id.2xlarge,r6id.4xlarge,r6id.8xlarge,r6id.12xlarge,r6id.16xlarge,r6id.24xlarge,r6id.32xlarge," +
            "r6in.xlarge,r6in.2xlarge,r6in.4xlarge,r6in.8xlarge,r6in.12xlarge,r6in.16xlarge,r6in.24xlarge,r6in.32xlarge," +
            "r6idn.xlarge,r6idn.2xlarge,r6idn.4xlarge,r6idn.8xlarge,r6idn.12xlarge,r6idn.16xlarge,r6idn.24xlarge,r6idn.32xlarge," +
            "c6i.xlarge,c6i.2xlarge,c6i.4xlarge,c6i.8xlarge,c6i.12xlarge,c6i.16xlarge,c6i.24xlarge,c6i.32xlarge," +
            "c6id.xlarge,c6id.2xlarge,c6id.4xlarge,c6id.8xlarge,c6id.12xlarge,c6id.16xlarge,c6id.24xlarge,c6id.32xlarge," +
            "c6in.xlarge,c6in.2xlarge,c6in.4xlarge,c6in.8xlarge,c6in.12xlarge,c6in.16xlarge,c6in.24xlarge,c6in.32xlarge," +
            "c6a.xlarge,c6a.2xlarge,c6a.4xlarge,c6a.8xlarge,c6a.12xlarge,c6a.16xlarge,c6a.24xlarge,c6a.32xlarge,c6a.48xlarge," +

            "m7i.xlarge,m7i.2xlarge,m7i.4xlarge,m7i.8xlarge,m7i.12xlarge,m7i.16xlarge,m7i.24xlarge,m7i.48xlarge," +
            "m7i-flex.xlarge,m7i-flex.2xlarge,m7i-flex.4xlarge,m7i-flex.8xlarge," +
            "m7a.xlarge,m7a.2xlarge,m7a.4xlarge,m7a.8xlarge,m7a.12xlarge,m7a.16xlarge,m7a.24xlarge,m7a.32xlarge,m7a.48xlarge," +
            "r7a.xlarge,r7a.2xlarge,r7a.4xlarge,r7a.8xlarge,r7a.12xlarge,r7a.16xlarge,r7a.24xlarge,r7a.32xlarge,r7a.48xlarge," +
            "r7i.xlarge,r7i.2xlarge,r7i.4xlarge,r7i.8xlarge,r7i.12xlarge,r7i.16xlarge,r7i.24xlarge,r7i.48xlarge," +
            "r7iz.xlarge,r7iz.2xlarge,r7iz.4xlarge,r7iz.8xlarge,r7iz.12xlarge,r7iz.16xlarge,r7iz.32xlarge," +
            "c7i.xlarge,c7i.2xlarge,c7i.4xlarge,c7i.8xlarge,c7i.12xlarge,c7i.16xlarge,c7i.24xlarge,c7i.48xlarge," +
            "c7a.xlarge,c7a.2xlarge,c7a.4xlarge,c7a.8xlarge,c7a.12xlarge,c7a.16xlarge,c7a.24xlarge,c7a.32xlarge,c7a.48xlarge," +

            "m8a.xlarge,m8a.2xlarge,m8a.4xlarge,m8a.8xlarge,m8a.12xlarge,m8a.16xlarge,m8a.24xlarge,m8a.48xlarge," +
            "m8i.xlarge,m8i.2xlarge,m8i.4xlarge,m8i.8xlarge,m8i.12xlarge,m8i.16xlarge,m8i.24xlarge,m8i.32xlarge,m8i.48xlarge," +
            "r8i.xlarge,r8i.2xlarge,r8i.4xlarge,r8i.8xlarge,r8i.12xlarge,r8i.16xlarge,r8i.24xlarge,r8i.32xlarge,r8i.48xlarge," +
            "c8i.2xlarge,c8i.4xlarge,c8i.8xlarge,c8i.12xlarge,c8i.16xlarge,c8i.24xlarge,c8i.32xlarge,c8i.48xlarge," +
            "i7i.xlarge,i7i.2xlarge,i7i.4xlarge,i7i.8xlarge,i7i.12xlarge,i7i.16xlarge,i7i.24xlarge,i7i.48xlarge";

    private static final String ENABLED_ARM64_TYPES =
            "i4g.xlarge,i4g.2xlarge,i4g.4xlarge,i4g.8xlarge,i4g.16xlarge," +
            "im4gn.xlarge,im4gn.2xlarge,im4gn.4xlarge,im4gn.8xlarge,im4gn.16xlarge," +
            "is4gen.xlarge,is4gen.2xlarge,is4gen.4xlarge,is4gen.8xlarge," +

            "m6g.xlarge,m6g.2xlarge,m6g.4xlarge,m6g.8xlarge,m6g.12xlarge,m6g.16xlarge," +
            "m6gd.xlarge,m6gd.2xlarge,m6gd.4xlarge,m6gd.8xlarge,m6gd.12xlarge,m6gd.16xlarge," +
            "r6g.xlarge,r6g.2xlarge,r6g.4xlarge,r6g.8xlarge,r6g.12xlarge,r6g.16xlarge," +
            "r6gd.xlarge,r6gd.2xlarge,r6gd.4xlarge,r6gd.8xlarge,r6gd.12xlarge,r6gd.16xlarge," +
            "c6g.xlarge,c6g.2xlarge,c6g.4xlarge,c6g.8xlarge,c6g.12xlarge,c6g.16xlarge," +
            "c6gn.xlarge,c6gn.2xlarge,c6gn.4xlarge,c6gn.8xlarge,c6gn.12xlarge,c6gn.16xlarge," +
            "c6gd.xlarge,c6gd.2xlarge,c6gd.4xlarge,c6gd.8xlarge,c6gd.12xlarge,c6gd.16xlarge," +

            "m7g.xlarge,m7g.2xlarge,m7g.4xlarge,m7g.8xlarge,m7g.12xlarge,m7g.16xlarge," +
            "r7g.xlarge,r7g.2xlarge,r7g.4xlarge,r7g.8xlarge,r7g.12xlarge,r7g.16xlarge," +
            "r7gd.xlarge,r7gd.2xlarge,r7gd.4xlarge,r7gd.8xlarge,r7gd.12xlarge,r7gd.16xlarge," +
            "c7g.xlarge,c7g.2xlarge,c7g.4xlarge,c7g.8xlarge,c7g.12xlarge,c7g.16xlarge," +
            "c7gn.xlarge,c7gn.2xlarge,c7gn.4xlarge,c7gn.8xlarge,c7gn.12xlarge,c7gn.16xlarge," +
            "c7gd.xlarge,c7gd.2xlarge,c7gd.4xlarge,c7gd.8xlarge,c7gd.12xlarge,c7gd.16xlarge," +

            "m8g.xlarge,m8g.2xlarge,m8g.4xlarge,m8g.8xlarge,m8g.12xlarge,m8g.16xlarge,m8g.24xlarge," +
            "r8g.xlarge,r8g.2xlarge,r8g.4xlarge,r8g.8xlarge,r8g.12xlarge,r8g.16xlarge,r8g.24xlarge," +
            "i8g.xlarge,i8g.2xlarge,i8g.4xlarge,i8g.8xlarge,i8g.16xlarge,i8g.24xlarge," +
            "c8g.xlarge,c8g.2xlarge,c8g.4xlarge,c8g.8xlarge,c8g.12xlarge,c8g.16xlarge,c8g.24xlarge," +
            "m8gd.xlarge,m8gd.2xlarge,m8gd.4xlarge,m8gd.8xlarge,m8gd.12xlarge,m8gd.16xlarge,m8gd.24xlarge,m8gd.48xlarge," +
            "c8gd.xlarge,c8gd.2xlarge,c8gd.4xlarge,c8gd.8xlarge,c8gd.12xlarge,c8gd.16xlarge,c8gd.24xlarge,c8gd.48xlarge," +
            "c8gn.2xlarge,c8gn.4xlarge,c8gn.8xlarge,c8gn.12xlarge,c8gn.16xlarge,c8gn.24xlarge,c8gn.48xlarge," +
            "r8gb.xlarge,r8gb.2xlarge,r8gb.4xlarge,r8gb.8xlarge,r8gb.12xlarge,r8gb.16xlarge,r8gb.24xlarge,r8gd.xlarge," +
            "r8gd.2xlarge,r8gd.4xlarge,r8gd.8xlarge,r8gd.12xlarge,r8gd.16xlarge,r8gd.24xlarge,r8gd.48xlarge," +
            "r8gn.xlarge,r8gn.2xlarge,r8gn.4xlarge,r8gn.8xlarge,r8gn.12xlarge,r8gn.16xlarge,r8gn.24xlarge,r8gn.48xlarge," +
            "i8ge.xlarge,i8ge.2xlarge,i8ge.3xlarge,i8ge.6xlarge,i8ge.12xlarge,i8ge.18xlarge,i8ge.24xlarge,i8ge.48xlarge";

    public static final Set<String> AWS_ENABLED_X86_TYPES_LIST = splitInstanceList(ENABLED_X86_TYPES);

    public static final Set<String> AWS_ENABLED_ARM64_TYPES_LIST = splitInstanceList(ENABLED_ARM64_TYPES);

    private DistroxEnabledInstanceTypes() {
    }

    private static Set<String> splitInstanceList(String values) {
        return Arrays.stream(values.trim().split(","))
                .filter(it -> !it.isEmpty())
                .collect(Collectors.toSet());
    }

}
