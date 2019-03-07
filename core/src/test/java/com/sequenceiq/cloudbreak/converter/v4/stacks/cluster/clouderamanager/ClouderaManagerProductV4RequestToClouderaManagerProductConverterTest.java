package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

public class ClouderaManagerProductV4RequestToClouderaManagerProductConverterTest {

    @Test
    void testConvert() {
        ClouderaManagerProductV4Request request = new ClouderaManagerProductV4Request();
        request.setVersion("1.0");
        request.setParcel("parcel");
        request.setName("name");

        ClouderaManagerProduct product = ClouderaManagerProductV4RequestToClouderaManagerProductConverter.convert(request);

        assertAll(
                () -> assertEquals(request.getName(), product.getName()),
                () -> assertEquals(request.getVersion(), product.getVersion()),
                () -> assertEquals(request.getParcel(), product.getParcel())
        );
    }
}