package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import com.sequenceiq.it.cloudbreak.dto.mock.SparkUri;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.JsonRequestAnswer;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.SaltMultipartRequestAnswer;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.StringRequestAnswer;

public final class SaltEndpoints {
    public static final String SALT_BOOT_ROOT = "/saltboot";

    private SaltEndpoints() {
    }

    @SparkUri(url = SALT_BOOT_ROOT + "/health")
    public interface SaltHealt {
        StringRequestAnswer<String> post();
    }

    @SparkUri(url = SALT_BOOT_ROOT + "/file/distribute")
    public interface SaltFileDistribute {
        SaltMultipartRequestAnswer<String> post();
    }

    // SALT_BOOT_ROOT + "/salt/server/pillar/distribute"
    @SparkUri(url = SALT_BOOT_ROOT + "/salt/server/pillar/distribute")
    public interface SaltPillar {
        JsonRequestAnswer<String> post();
    }
}