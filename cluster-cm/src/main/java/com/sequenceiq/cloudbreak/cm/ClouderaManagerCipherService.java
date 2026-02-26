package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.tls.CipherSuitesLimitType.JAVA_INTERMEDIATE2018;
import static com.sequenceiq.cloudbreak.tls.CipherSuitesLimitType.TLS_1_2_RECOMMENDED;
import static com.sequenceiq.cloudbreak.tls.CipherSuitesLimitType.TLS_1_3_RECOMMENDED;

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

        ApiConfigEnforcement tlsCipherSuiteEnforcement = new ApiConfigEnforcement();
        tlsCipherSuiteEnforcement.setLabel(TLS_CIPHER_SUITE);
        tlsCipherSuiteEnforcement.setDefaultValue(INTERMEDIATE2018);

        ApiConfigEnforcement tlsCipherSuiteJavaEnforcement = new ApiConfigEnforcement();
        tlsCipherSuiteJavaEnforcement.setLabel(TLS_CIPHER_SUITE_JAVA);
        tlsCipherSuiteJavaEnforcement.setDefaultValue(getTlsCipherSuite(true));

        tlsCipherSuiteJavaEnforcement.setSeparator(POLICY_SEPARATOR);

        ApiConfigEnforcement tlsCipherSuiteJavaExcludedEnforcement = new ApiConfigEnforcement();
        tlsCipherSuiteJavaExcludedEnforcement.setLabel(TLS_CIPHER_SUITE_JAVA_EXCLUDE);
        tlsCipherSuiteJavaExcludedEnforcement.setDefaultValue(JAVA_EXCLUDE_INTERMEDIATE2018);
        tlsCipherSuiteJavaExcludedEnforcement.setSeparator(POLICY_SEPARATOR);

        ApiConfigEnforcement tlsCipherListOpenSslEnforcement = new ApiConfigEnforcement();
        tlsCipherListOpenSslEnforcement.setLabel(TLS_CIPHERS_LIST_OPENSSL);
        tlsCipherListOpenSslEnforcement.setDefaultValue(getTlsCipherSuite(false));
        tlsCipherListOpenSslEnforcement.setSeparator(POLICY_SEPARATOR);

        apiConfigEnforcements.add(tlsCipherSuiteEnforcement);
        apiConfigEnforcements.add(tlsCipherSuiteJavaEnforcement);
        apiConfigEnforcements.add(tlsCipherSuiteJavaExcludedEnforcement);
        apiConfigEnforcements.add(tlsCipherListOpenSslEnforcement);
        return apiConfigEnforcements;
    }

    private String getTlsCipherSuite(boolean useIanaNames) {
        String intermediate2018 = encryptionProfileProvider.getCipherSuiteString(
                JAVA_INTERMEDIATE2018,
                POLICY_SEPARATOR,
                useIanaNames
        );
        String tls12Recommended = encryptionProfileProvider.getCipherSuiteString(
                TLS_1_2_RECOMMENDED,
                POLICY_SEPARATOR,
                useIanaNames
        );
        String tls13Recommended = encryptionProfileProvider.getCipherSuiteString(
                TLS_1_3_RECOMMENDED,
                POLICY_SEPARATOR,
                useIanaNames
        );
        return String.join(POLICY_SEPARATOR, tls13Recommended, tls12Recommended, intermediate2018);
    }
}
