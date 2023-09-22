package com.sequenceiq.cloudbreak.common.base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

public class Base64UtilTest {

    @Test
    public void testEncodeString() {
        String input = "Hello, World!";
        String encoded = Base64Util.encode(input);
        String expected = Base64.getEncoder().encodeToString(input.getBytes());
        assertEquals(expected, encoded);
    }

    @Test
    public void testEncodeByteArray() {
        byte[] inputBytes = "Hello, World!".getBytes();
        String encoded = Base64Util.encode(inputBytes);
        String expected = Base64.getEncoder().encodeToString(inputBytes);
        assertEquals(expected, encoded);
    }

    @Test
    public void testDecodeAsByteArray() {
        String input = "SGVsbG8sIFdvcmxkIQ==";
        byte[] decodedBytes = Base64Util.decodeAsByteArray(input);
        String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
        assertEquals("Hello, World!", decodedString);
    }

    @Test
    public void testDecodeString() {
        String input = "SGVsbG8sIFdvcmxkIQ==";
        String decoded = Base64Util.decode(input);
        assertEquals("Hello, World!", decoded);
    }
}