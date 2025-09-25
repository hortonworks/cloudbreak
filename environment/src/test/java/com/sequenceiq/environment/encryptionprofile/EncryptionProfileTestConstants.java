package com.sequenceiq.environment.encryptionprofile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.common.api.encryptionprofile.TlsVersion;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileRequest;
import com.sequenceiq.environment.encryptionprofile.domain.EncryptionProfile;

public class EncryptionProfileTestConstants {

    public static final String NAME = "test-encryption-profile";

    public static final String ACCOUNT_ID = "cloudbreak";

    public static final String CREATOR = "test-creator";

    public static final String DESCRIPTION = "An encryption profile with TLS settings";

    public static final Set<TlsVersion> TLS_VERSIONS = new HashSet<>(Arrays.asList(TlsVersion.TLS_1_2, TlsVersion.TLS_1_3));

    public static final List<String> CIPHER_SUITES = Arrays.asList("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384");

    public static final String ENCRYPTION_PROFILE_CRN = "crn:cdp:environments:us-west-1:cloudbreak:encryptionProfile:ecb891ca-18f2-406f-9958-99da466fd0f2";

    public static final String DEFAULT_ENCRYPTION_PROFILE_NAME = "cdp_default_fips_v1";

    public static final String DEFAULT_ENCRYPTION_PROFILE_CRN = "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:cdp-default-v1";

    public static final String USER_CRN = "crn:cdp:iam:us-west-1:cloudbreak:user:ecb891ca-18f2-406f-9958-99da466fd0f2";

    private EncryptionProfileTestConstants() {
    }

    public static EncryptionProfileRequest getTestEncryptionProfileRequest() {
        EncryptionProfileRequest request = new EncryptionProfileRequest();
        request.setName(NAME);
        request.setDescription(DESCRIPTION);
        request.setTlsVersions(TLS_VERSIONS);
        request.setCipherSuites(CIPHER_SUITES);
        return request;
    }

    public static EncryptionProfile getTestEncryptionProfile() {
        return getTestEncryptionProfile(NAME, ResourceStatus.USER_MANAGED);
    }

    public static EncryptionProfile getTestEncryptionProfile(String name, ResourceStatus resourceStatus) {
        EncryptionProfile profile = new EncryptionProfile();
        profile.setId(1L);
        profile.setName(name);
        profile.setAccountId(ACCOUNT_ID);
        profile.setDescription(DESCRIPTION);
        profile.setResourceCrn(ENCRYPTION_PROFILE_CRN);
        profile.setCipherSuites(CIPHER_SUITES);
        profile.setTlsVersions(TLS_VERSIONS);
        profile.setResourceStatus(resourceStatus);
        return profile;
    }
}
