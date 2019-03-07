package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;

public class ClouderaManagerRepositoryV4RequestToClouderaManagerRepoConverterTest {

    @Test
    void testConvert() {
        ClouderaManagerRepositoryV4Request request = new ClouderaManagerRepositoryV4Request();
        ClouderaManagerRepo repo = ClouderaManagerRepositoryV4RequestToClouderaManagerRepoConverter.convert(request);

        assertAll(
                () -> assertEquals(request.getBaseUrl(), repo.getBaseUrl()),
                () -> assertEquals(request.getGpgKeyUrl(), repo.getGpgKeyUrl()),
                () -> assertEquals(request.getVersion(), repo.getVersion())
        );
    }
}