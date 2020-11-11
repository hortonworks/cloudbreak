package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import static com.sequenceiq.it.cloudbreak.mock.ITResponse.MOCK_ROOT;
import static com.sequenceiq.it.cloudbreak.mock.model.SPIMock.REGISTER_PUBIC_KEY;
import static com.sequenceiq.it.cloudbreak.mock.model.SPIMock.UNREGISTER_PUBIC_KEY;

import com.sequenceiq.it.cloudbreak.dto.mock.SparkUri;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.BooleanRequestAnswer;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.StringRequestAnswer;

public final class SpiEndpoints {

    private SpiEndpoints() {

    }

    @SparkUri(url = MOCK_ROOT + REGISTER_PUBIC_KEY)
    public interface RegisterPublicKey {
        StringRequestAnswer<String> post();
    }

    @SparkUri(url = MOCK_ROOT + UNREGISTER_PUBIC_KEY)
    public interface UnregisterPublicKey {
        StringRequestAnswer<String> post();
    }

    @SparkUri(url = MOCK_ROOT + "/get_public_key/{publicKeyId}")
    public interface GetPublicKey {
        BooleanRequestAnswer<Boolean> get();
    }
}
