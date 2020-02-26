package com.sequenceiq.freeipa.kerberosmgmt;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.ActorKerberosKey;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.freeipa.kerberosmgmt.v1.UserKeytabGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserKeytabGeneratorTest {

    private static final long MOCK_TIME = 1576171731141L;

    private static final String TEST_USER_NAME = "csso_testuser";

    private static final String TEST_REALM = "TESTREALM-AWS.XCU2-8Y8X.DEV.CLDR.WORK";

    private static final ActorKerberosKey KEY_17 = ActorKerberosKey.newBuilder()
            .setKeyType(17)
            .setKeyValue("fRq3DlywzqVgH/zbJDXuXw==")
            .setSaltType(4)
            .setSaltValue("HfSH3pSI7Sw8OzZK")
            .build();

    private static final ActorKerberosKey KEY_18 = ActorKerberosKey.newBuilder()
            .setKeyType(18)
            .setKeyValue("613hgEgwn5f/9bH29FGOT4lBc6bCs9XBStAeBHeJB+w=")
            .setSaltType(4)
            .setSaltValue("x5Ffi1dg2KGeCBPX")
            .build();

    @InjectMocks
    UserKeytabGenerator underTest;

    @Mock
    private Clock clock;

    @Test
    void testGenerateKeytabBase64() throws IOException {
        when(clock.getCurrentTimeMillis()).thenReturn(MOCK_TIME);
        String keytabBase64 = underTest.generateKeytabBase64(TEST_USER_NAME, TEST_REALM, List.of(KEY_17, KEY_18));
        String expectedKeytab = "BQIAAABVAAEAJVRFU1RSRUFMTS1BV1MuWENVMi04WThYLkRFVi5DTERSLldPUksADWNz" +
                "c29fdGVzdHVzZXIAAAABXfJ40wAAEQAQfRq3DlywzqVgH/zbJDXuXwAAAGUAAQAlVEVTVFJFQUxNLUFXUy5Y" +
                "Q1UyLThZOFguREVWLkNMRFIuV09SSwANY3Nzb190ZXN0dXNlcgAAAAFd8njTAAASACDrXeGASDCfl//1sfb0" +
                "UY5PiUFzpsKz1cFK0B4Ed4kH7A==";
        assertEquals(expectedKeytab, keytabBase64);
    }

    @Test
    void testGenerateKeytabBase64NullUser() {
        assertThrows(NullPointerException.class,
                () -> underTest.generateKeytabBase64(null, TEST_REALM, List.of(KEY_17, KEY_18)));
    }

    @Test
    void testGenerateKeytabBase64NullRealm() {
        assertThrows(NullPointerException.class,
                () -> underTest.generateKeytabBase64(TEST_USER_NAME, null, List.of(KEY_17, KEY_18)));
    }

    @Test
    void testGenerateKeytabBase64NullKeys() {
        assertThrows(NullPointerException.class,
                () -> underTest.generateKeytabBase64(TEST_USER_NAME, TEST_REALM, null));
    }

    @Test
    void testGenerateKeytabBase64EmptyKeys() {
        assertThrows(IllegalArgumentException.class,
                () -> underTest.generateKeytabBase64(TEST_USER_NAME, TEST_REALM, List.of()));
    }

}
