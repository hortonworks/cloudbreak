package com.sequenceiq.datalake.job;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.datalake.entity.SdxCluster;

class SdxClusterJobResourcesTest {

    @Test
    void fromSdxClusterMatchesFindAllAliveViewShape() {
        SdxCluster cluster = new SdxCluster();
        cluster.setId(99L);
        cluster.setCrn("crn:cdp:datalake:us-west-1:acct:datalake:name");
        cluster.setClusterName("my-dl");

        JobResource jobResource = SdxClusterJobResources.fromSdxCluster(cluster);

        assertThat(jobResource.getLocalId()).isEqualTo("99");
        assertThat(jobResource.getRemoteResourceId()).isEqualTo("crn:cdp:datalake:us-west-1:acct:datalake:name");
        assertThat(jobResource.getName()).isEqualTo("my-dl");
        assertThat(jobResource.getProvider()).isEmpty();
    }
}
