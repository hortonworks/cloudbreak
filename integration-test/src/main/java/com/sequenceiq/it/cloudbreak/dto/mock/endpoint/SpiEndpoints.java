package com.sequenceiq.it.cloudbreak.dto.mock.endpoint;

import com.sequenceiq.it.cloudbreak.dto.mock.SparkUri;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.BooleanRequestAnswer;
import com.sequenceiq.it.cloudbreak.dto.mock.answer.StringRequestAnswer;

public final class SpiEndpoints {

    public static final String SPI_ROOT = "/spi";

    public static final String REGISTER_PUBLIC_KEY = "/register_public_key";

    public static final String UNREGISTER_PUBLIC_KEY = "/unregister_public_key";

    public static final String GET_PUBLIC_KEY_BY_ID = "/get_public_key/{publicKeyId}";

    private SpiEndpoints() {

    }

    @SparkUri(url = SPI_ROOT + REGISTER_PUBLIC_KEY)
    public interface RegisterPublicKey {
        StringRequestAnswer<String> post();
    }

    @SparkUri(url = SPI_ROOT + UNREGISTER_PUBLIC_KEY)
    public interface UnregisterPublicKey {
        StringRequestAnswer<String> post();
    }

    @SparkUri(url = SPI_ROOT + GET_PUBLIC_KEY_BY_ID)
    public interface GetPublicKey {
        BooleanRequestAnswer<Boolean> get();
    }
}
