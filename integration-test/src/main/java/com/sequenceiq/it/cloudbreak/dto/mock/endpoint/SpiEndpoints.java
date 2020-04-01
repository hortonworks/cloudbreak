package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import com.sequenceiq.it.cloudbreak.dto.mock.SparkUri;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.StringRequestAnswer;

public final class SpiEndpoints {
    public static final String SPI_ROOT = "/spi";

    private SpiEndpoints() {

    }

    @SparkUri(url = SPI_ROOT + "/register_public_key")
    public interface RegisterPublicKey {
        StringRequestAnswer<String> post();
    }

    @SparkUri(url = SPI_ROOT + "/unregister_public_key")
    public interface UnregisterPublicKey {
        StringRequestAnswer<String> post();
    }

    @SparkUri(url = SPI_ROOT + "/get_public_key/:publicKeyId")
    public interface GetPublicKey {
        StringRequestAnswer<String> get();
    }
}
