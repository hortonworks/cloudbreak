package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider.CipherSuitesLimitType.JAVA_INTERMEDIATE2018;
import static com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider.CipherSuitesLimitType.OPENSSL_INTERMEDIATE2018;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiConfigEnforcement;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;

@Service
public class ClouderaManagerCipherService {

    public static final String TLS_CIPHER_SUITE_JAVA_EXCLUDE = "TLS_CIPHER_SUITE_JAVA_EXCLUDE";

    public static final String TLS_CIPHER_SUITE_JAVA = "TLS_CIPHER_SUITE_JAVA";

    public static final String TLS_CIPHERS_LIST_OPENSSL = "TLS_CIPHERS_LIST_OPENSSL";

    public static final String TLS_CIPHER_SUITE = "TLS_CIPHER_SUITE";

    public static final String INTERMEDIATE2018 = "intermediate2018";

    public static final String POLICY_SEPARATOR = ",";

    public static final String JAVA_EXCLUDE_INTERMEDIATE2018 =
            "^.*MD5.*$," +
                    "^TLS_DH_.*$," +
                    "^.*RC4.*$," +
                    "^.*CCM.*$";

    @Inject
    private EncryptionProfileProvider encryptionProfileProvider;

    public List<ApiConfigEnforcement> getApiConfigEnforcements() {
        List<ApiConfigEnforcement> apiConfigEnforcements = new ArrayList<>();

        ApiConfigEnforcement tlsChipherSuiteEnforcement = new ApiConfigEnforcement();
        tlsChipherSuiteEnforcement.setLabel(TLS_CIPHER_SUITE);
        tlsChipherSuiteEnforcement.setDefaultValue(INTERMEDIATE2018);

        ApiConfigEnforcement tlsChipherSuiteJavaEnforcement = new ApiConfigEnforcement();
        tlsChipherSuiteJavaEnforcement.setLabel(TLS_CIPHER_SUITE_JAVA);

        tlsChipherSuiteJavaEnforcement.setDefaultValue(
                encryptionProfileProvider.getCipherSuiteString(
                        JAVA_INTERMEDIATE2018,
                        POLICY_SEPARATOR
                )
        );
        tlsChipherSuiteJavaEnforcement.setSeparator(POLICY_SEPARATOR);

        ApiConfigEnforcement tlsChipherSuiteJavaExcludedEnforcement = new ApiConfigEnforcement();
        tlsChipherSuiteJavaExcludedEnforcement.setLabel(TLS_CIPHER_SUITE_JAVA_EXCLUDE);
        tlsChipherSuiteJavaExcludedEnforcement.setDefaultValue(JAVA_EXCLUDE_INTERMEDIATE2018);
        tlsChipherSuiteJavaExcludedEnforcement.setSeparator(POLICY_SEPARATOR);

        ApiConfigEnforcement tlsChipherListOpenSslEnforcement = new ApiConfigEnforcement();
        tlsChipherListOpenSslEnforcement.setLabel(TLS_CIPHERS_LIST_OPENSSL);
        tlsChipherListOpenSslEnforcement.setDefaultValue(
                encryptionProfileProvider.getCipherSuiteString(
                        OPENSSL_INTERMEDIATE2018,
                        POLICY_SEPARATOR
                )
        );

        tlsChipherListOpenSslEnforcement.setSeparator(POLICY_SEPARATOR);

        apiConfigEnforcements.add(tlsChipherSuiteEnforcement);
        apiConfigEnforcements.add(tlsChipherSuiteJavaEnforcement);
        apiConfigEnforcements.add(tlsChipherSuiteJavaExcludedEnforcement);
        apiConfigEnforcements.add(tlsChipherListOpenSslEnforcement);
        return apiConfigEnforcements;
    }
}
