package com.sequenceiq.cloudbreak.cm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiConfigEnforcement;
import com.cloudera.api.swagger.model.ApiConfigPolicy;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Service
public class ClouderaManagerFedRAMPService {

    public static final String TLS_CIPHER_SUITE_JAVA_EXCLUDE = "TLS_CIPHER_SUITE_JAVA_EXCLUDE";

    public static final String TLS_CIPHER_SUITE_JAVA = "TLS_CIPHER_SUITE_JAVA";

    public static final String TLS_CIPHERS_LIST_OPENSSL = "TLS_CIPHERS_LIST_OPENSSL";

    public static final String TLS_CIPHER_SUITE = "TLS_CIPHER_SUITE";

    public static final String LOGIN_BANNER = "LOGIN_BANNER";

    public static final String INTERMEDIATE2018 = "intermediate2018";

    public static final String POLICY_SEPARATOR = ",";

    public static final String POLICY_VERSION = "1.0";

    public static final String POLICY_DESCRIPTION = "Cloudera configured FISMA Policy For Login Banner";

    public static final String POLICY_NAME = "FISMA Policy For Login Banner";

    public static final String OPENSSL_INTERMEDIATE2018 =
            "ECDHE-ECDSA-AES128-GCM-SHA256," +
                    "ECDHE-RSA-AES128-GCM-SHA256," +
                    "ECDHE-ECDSA-AES256-GCM-SHA384," +
                    "ECDHE-RSA-AES256-GCM-SHA384," +
                    "DHE-RSA-AES128-GCM-SHA256," +
                    "DHE-RSA-AES256-GCM-SHA384," +
                    "ECDHE-ECDSA-AES128-SHA256," +
                    "ECDHE-RSA-AES128-SHA256," +
                    "ECDHE-ECDSA-AES128-SHA," +
                    "ECDHE-RSA-AES256-SHA384," +
                    "ECDHE-RSA-AES128-SHA," +
                    "ECDHE-ECDSA-AES256-SHA384," +
                    "ECDHE-ECDSA-AES256-SHA," +
                    "ECDHE-RSA-AES256-SHA," +
                    "DHE-RSA-AES128-SHA256," +
                    "DHE-RSA-AES128-SHA," +
                    "DHE-RSA-AES256-SHA256," +
                    "DHE-RSA-AES256-SHA," +
                    "ECDHE-ECDSA-DES-CBC3-SHA," +
                    "ECDHE-RSA-DES-CBC3-SHA," +
                    "DHE-RSA-DES-CBC3-SHA," +
                    "AES128-GCM-SHA256," +
                    "AES256-GCM-SHA384," +
                    "AES128-SHA256," +
                    "AES256-SHA256," +
                    "AES128-SHA," +
                    "AES256-SHA," +
                    "DES-CBC3-SHA";

    public static final String JAVA_INTERMEDIATE2018 =
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256," +
                    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256," +
                    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384," +
                    "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384," +
                    "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256," +
                    "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384," +
                    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256," +
                    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256," +
                    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA," +
                    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384," +
                    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA," +
                    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384," +
                    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA," +
                    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA," +
                    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256," +
                    "TLS_DHE_RSA_WITH_AES_128_CBC_SHA," +
                    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256," +
                    "TLS_DHE_RSA_WITH_AES_256_CBC_SHA," +
                    "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA," +
                    "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA," +
                    "TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA," +
                    "TLS_RSA_WITH_AES_128_GCM_SHA256," +
                    "TLS_RSA_WITH_AES_256_GCM_SHA384," +
                    "TLS_RSA_WITH_AES_128_CBC_SHA256," +
                    "TLS_RSA_WITH_AES_256_CBC_SHA256," +
                    "TLS_RSA_WITH_AES_128_CBC_SHA," +
                    "TLS_RSA_WITH_AES_256_CBC_SHA," +
                    "TLS_RSA_WITH_3DES_EDE_CBC_SHA";

    public static final String JAVA_EXCLUDE_INTERMEDIATE2018 =
            "^.*MD5.*$," +
                    "^TLS_DH_.*$," +
                    "^.*RC4.*$," +
                    "^.*CCM.*$";

    private String banner;

    @PostConstruct
    public void initApiClient() throws ClusterClientInitException, IOException {
        banner = FileReaderUtils.readFileFromClasspath("banner.txt");
    }

    public ApiConfigPolicy getApiConfigPolicy() {
        ApiConfigPolicy apiConfigPolicy = new ApiConfigPolicy();
        apiConfigPolicy.setVersion(POLICY_VERSION);
        apiConfigPolicy.setDescription(POLICY_DESCRIPTION);
        apiConfigPolicy.setName(POLICY_NAME);

        List<ApiConfigEnforcement> apiConfigEnforcements = new ArrayList<>();

        ApiConfigEnforcement loginBannerEnforcement = new ApiConfigEnforcement();
        loginBannerEnforcement.setLabel(LOGIN_BANNER);
        loginBannerEnforcement.setDefaultValue(banner);

        ApiConfigEnforcement tlsChipherSuiteEnforcement = new ApiConfigEnforcement();
        tlsChipherSuiteEnforcement.setLabel(TLS_CIPHER_SUITE);
        tlsChipherSuiteEnforcement.setDefaultValue(INTERMEDIATE2018);

        ApiConfigEnforcement tlsChipherSuiteJavaEnforcement = new ApiConfigEnforcement();
        tlsChipherSuiteJavaEnforcement.setLabel(TLS_CIPHER_SUITE_JAVA);
        tlsChipherSuiteJavaEnforcement.setDefaultValue(JAVA_INTERMEDIATE2018);
        tlsChipherSuiteJavaEnforcement.setSeparator(POLICY_SEPARATOR);

        ApiConfigEnforcement tlsChipherSuiteJavaExcludedEnforcement = new ApiConfigEnforcement();
        tlsChipherSuiteJavaExcludedEnforcement.setLabel(TLS_CIPHER_SUITE_JAVA_EXCLUDE);
        tlsChipherSuiteJavaExcludedEnforcement.setDefaultValue(JAVA_EXCLUDE_INTERMEDIATE2018);
        tlsChipherSuiteJavaExcludedEnforcement.setSeparator(POLICY_SEPARATOR);

        ApiConfigEnforcement tlsChipherListOpenSslEnforcement = new ApiConfigEnforcement();
        tlsChipherListOpenSslEnforcement.setLabel(TLS_CIPHERS_LIST_OPENSSL);
        tlsChipherListOpenSslEnforcement.setDefaultValue(OPENSSL_INTERMEDIATE2018);
        tlsChipherListOpenSslEnforcement.setSeparator(POLICY_SEPARATOR);

        apiConfigEnforcements.add(loginBannerEnforcement);
        apiConfigEnforcements.add(tlsChipherSuiteEnforcement);
        apiConfigEnforcements.add(tlsChipherSuiteJavaEnforcement);
        apiConfigEnforcements.add(tlsChipherSuiteJavaExcludedEnforcement);
        apiConfigEnforcements.add(tlsChipherListOpenSslEnforcement);
        apiConfigPolicy.setConfigEnforcements(apiConfigEnforcements);
        return apiConfigPolicy;
    }
}
