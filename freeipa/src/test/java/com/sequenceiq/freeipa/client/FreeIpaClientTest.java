package com.sequenceiq.freeipa.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;

@ExtendWith(MockitoExtension.class)
class FreeIpaClientTest {

    @Mock
    private JsonRpcHttpClient jsonRpcHttpClient;

    private FreeIpaClient freeIpaClient;

    @BeforeEach
    void setUp() {
        freeIpaClient = new FreeIpaClient(jsonRpcHttpClient, "1.1.1.1", "localhost");
    }

    @Test
    void testFormatDateMax() {
        assertEquals(FreeIpaClient.MAX_PASSWORD_EXPIRATION_DATETIME, freeIpaClient.formatDate(Optional.empty()));
    }

    @Test
    void testFormatDate() {
        Instant instant = Instant.ofEpochMilli(1576118634916L);
        String expected = "20191212024354Z";
        assertEquals(expected, freeIpaClient.formatDate(Optional.of(instant)));
    }
}