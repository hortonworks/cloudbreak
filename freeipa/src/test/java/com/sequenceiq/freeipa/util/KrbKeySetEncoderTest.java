package com.sequenceiq.freeipa.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto.GetActorWorkloadCredentialsResponse;

import org.junit.jupiter.api.Test;

public class KrbKeySetEncoderTest {

    @Test
    void testASNEncoderGetASNEncodedKrbPrincipalKey() throws Exception {
            final int keyType17 = 17;
            final int keyType18 = 18;
            final int saltType = 4;

            GetActorWorkloadCredentialsResponse.Builder respBuilder = GetActorWorkloadCredentialsResponse.getDefaultInstance().toBuilder();
            respBuilder.addKerberosKeysBuilder(0)
                .setSaltType(saltType)
                .setKeyType(keyType17)
                .setKeyValue("testKeyValue17")
                .setSaltValue("NonIodizedGrainOfSalt")
                .build();

            respBuilder.addKerberosKeysBuilder(1)
                .setSaltType(saltType)
                .setKeyType(keyType18)
                .setKeyValue("testKeyValue18")
                .setSaltValue("IodizedGrainOfSalt")
                .build();

            String expectedValue = "MIGLoAMCAQGhAwIBAaIDAgEBowMCAQGkdTBzMDmgIDAeoAMCAQShFwQVTm9uSW9kaXplZE" +
                "dyYWluT2ZTYWx0oRUwE6ADAgERoQwECrXrLSnslWpbntcwNqAdMBugAwIBBKEUBBJJb2RpemVkR3JhaW5PZlNhbHShFTAToAMCARKhDAQKtestKeyValue1w==";
            String encodedValue = KrbKeySetEncoder.getASNEncodedKrbPrincipalKey(respBuilder.getKerberosKeysList());
            assertEquals(expectedValue, encodedValue, "Must be equal");
    }
}
