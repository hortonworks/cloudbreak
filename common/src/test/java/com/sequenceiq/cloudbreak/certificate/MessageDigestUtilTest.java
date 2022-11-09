package com.sequenceiq.cloudbreak.certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class MessageDigestUtilTest {

    private static final String AZURE_CERT = "-----BEGIN CERTIFICATE-----\n" +
            "MIICozCCAYugAwIBAgIBATANBgkqhkiG9w0BAQsFADAVMRMwEQYDVQQDDApjbG91\n" +
            "ZGJyZWFrMB4XDTIyMTAxMjA4NTQyMFoXDTIzMTAxMjA4NTQyMFowFTETMBEGA1UE\n" +
            "AxMKY2xvdWRicmVhazCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKc2\n" +
            "lIByoJRHXfbxdH2Zt4qnMsHkcgxgJkmW6gGGwXHZYrsyYXnhuQUbR2E9u3rZgV3l\n" +
            "u9ilZo5srUA1gX+ivVZLrAw22lYci1aJsecwMhGmZMCbEpNSnj6CygMhNoojEmZX\n" +
            "CelAlfXyywe1iVuccOlbFry03NXCvpPbYNYyiTk4AUl7SSefXKOaCKcS5WN7oehH\n" +
            "nepvZa4yMBvToN+BA3aggeuvpb0czICzA4WS3BDGtINsnZnqYXbBrwJ9vIcix4wp\n" +
            "dh0o2vP3G38j0VL1HC5Pv3OfTTnJCy1nhmpYHnt2cwIFFDY6sY6rN1QL0J5BeHQC\n" +
            "J/q3eZxhIfQW3Q/mMnECAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAmUEEhbMo4hNi\n" +
            "uS9Bbw67v5g/jHDnM1ur8Sq3+L9JfXNg17+ICv48vlUGJq6kX3a8NLJxMu9vjLP6\n" +
            "8xSB03M0bBqrH7hFRrvbf954NCb8uxACmzii7TERGFh1Ck4207zOt6jyN3U7z0Sd\n" +
            "zfkQk2Cdn3sczwbJ0Pl6FsXbu7ynA1hC9HxvUP/yONGRxK0HT7WhYKCv6XZGfmSU\n" +
            "2n7ruaYyJpgZS0IVksfxU6Ddsb+mtP4S+9mgYPTCCFSsPkyZM5CoibPRtH8jGaXS\n" +
            "ppFWX/JN/6yVtSohJi/tzM98zlEv8Hedp4YSGEx6hw65rWfCrPBdFPGlfoZ0Mopt\n" +
            "itQThNtnng==\n" +
            "-----END CERTIFICATE-----\n";

    @Test
    void sha512Test() {
        assertEquals(
                "c5dbf7d149a97d6c56641365ccc6edf423027c2ff42a253298ad4e59633fc1cba40d2af48b726d5c45681b19e471d68448febd89d989e9e9bd1bcd33c359924d",
                MessageDigestUtil.signatureSHA512(AZURE_CERT));
    }
}
