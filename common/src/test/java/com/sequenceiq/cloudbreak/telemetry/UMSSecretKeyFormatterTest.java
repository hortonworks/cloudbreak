package com.sequenceiq.cloudbreak.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UMSSecretKeyFormatterTest {

    @Test
    public void testECDSAKeyIsFormattedInOneLine() {
        String secretKey = "-----BEGIN PRIVATE KEY-----\n" +
                "MIH3AgEAMBAGByqGSM49AgEGBSuBBAAjBIHfMIHcAgEBBEIA/aEOb3cox2HlI8OV\n" +
                "citV0heZE3++uAu2HmkwNEMBMRDOGuSw+9YaoAc+k0nioxJZ/IRCt7KGkT2Zm5rO\n" +
                "j6KS67agBwYFK4EEACOhgYkDgYYABAA798mhJb0V24eOfLmSpo4Odp+dgdc6DqlE\n" +
                "piZXdHME1CvU96nPax8KUYG776GrqQsmSk36SjdiqYyNVnUGqHTOYgBPhaOX8JU0\n" +
                "HlP8C4GzBCBoIJGE+ItGil6gO44Xzd3Phs8gnUOb2uhJKJ9niim1UCgT3UKk5HaY\n" +
                "PDIxdZ0cCY/W0Q==\n" +
                "-----END PRIVATE KEY-----";
        assertEquals("-----BEGIN PRIVATE KEY-----\\n" +
                "MIH3AgEAMBAGByqGSM49AgEGBSuBBAAjBIHfMIHcAgEBBEIA/aEOb3cox2HlI8OV\\n" +
                "citV0heZE3++uAu2HmkwNEMBMRDOGuSw+9YaoAc+k0nioxJZ/IRCt7KGkT2Zm5rO\\n" +
                "j6KS67agBwYFK4EEACOhgYkDgYYABAA798mhJb0V24eOfLmSpo4Odp+dgdc6DqlE\\n" +
                "piZXdHME1CvU96nPax8KUYG776GrqQsmSk36SjdiqYyNVnUGqHTOYgBPhaOX8JU0\\n" +
                "HlP8C4GzBCBoIJGE+ItGil6gO44Xzd3Phs8gnUOb2uhJKJ9niim1UCgT3UKk5HaY\\n" +
                "PDIxdZ0cCY/W0Q==\\n" +
                "-----END PRIVATE KEY-----", UMSSecretKeyFormatter.formatSecretKey("ECDSA", secretKey));
    }

}