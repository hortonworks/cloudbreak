package com.sequenceiq.cloudbreak.cluster.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class HostnameTransformerTest {

    @Test
    void getHostnamePatterns() {
        List<String> fqdns = List.of(
                "dbajzath7-dh2-worker0.dbajzath.xcu2-8y8x.wl.cloudera.site",
                "dbajzath7-dh2-worker1.dbajzath.xcu2-8y8x.wl.cloudera.site",
                "dbajzath7-dh2-worker3.dbajzath.xcu2-8y8x.wl.cloudera.site",
                "dbajzath7-dh2-worker4.dbajzath.xcu2-8y8x.wl.cloudera.site",
                "dbajzath7-dh2-master0.dbajzath.xcu2-8y8x.wl.cloudera.site",
                "dbajzath7-dh2-master1.dbajzath.xcu2-8y8x.wl.cloudera.site",
                "dbajzath7-dh2-compute0.dbajzath.xcu2-8y8x.wl.cloudera.site",
                "fake1",
                "mock"
        );

        List<String> result = HostnameTransformer.getHostnamePatterns(fqdns);

        assertEquals(List.of("compute0", "fake1", "master[0-1]", "mock", "worker[0-1,3-4]"), result);
    }

}
