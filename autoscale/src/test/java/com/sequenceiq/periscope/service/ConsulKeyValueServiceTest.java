package com.sequenceiq.periscope.service;

import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.sequenceiq.periscope.utils.ConsulUtils;

public class ConsulKeyValueServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulKeyValueServiceTest.class);

    private static final int DEFAULT_TIMEOUT_MS = 5000;

    @Before
    public void setUp() {

    }

//    @Test
    public void testConsul() {
        ConsulClient consulClient = new ConsulClient("http://52.214.127.187", 8500, DEFAULT_TIMEOUT_MS);
        QueryParams dc1QueryParams = new QueryParams("dc1");
        String valueToStore = "PERI_KEY_VALUEeee-HEEE";
        Response<Boolean> booleanResponse = consulClient.setKVValue("rules/alerting/peri-key", valueToStore, dc1QueryParams);
        LOGGER.info("Set key-value result: {}", booleanResponse);
        String value = ConsulUtils.getKVValue(consulClient, "rules/alerting/peri-key", dc1QueryParams);
        LOGGER.info(value);
        Assert.assertEquals(valueToStore, value);
    }
}