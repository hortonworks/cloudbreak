package com.sequenceiq.it.cloudbreak;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.ProxyConfig;
import com.sequenceiq.it.util.LongStringGeneratorUtil;

public class ProxyConfigTests extends CloudbreakTest {
    private static final String VALID_PROXY_CONFIG = "e2e-proxy";

    private static final String SPECIAL_PROXY_NAME = "a-@#$%|:&*;";

    private static final String VALID_PROXY_DESC = "Valid proxy config description";

    private final List<String> proxyConfigsToDelete = new ArrayList<>();

    private String proxyUser;

    private String proxyPassword;

    private String proxyHost;

    private Integer proxyPort;

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;

    @BeforeTest
    public void setup() throws Exception {
        given(CloudbreakClient.isCreated());
        proxyHost = getTestParameter().get("integrationtest.proxyconfig.proxyHost").split(":")[0];
        proxyUser = getTestParameter().get("integrationtest.proxyconfig.proxyUser");
        proxyPassword = getTestParameter().get("integrationtest.proxyconfig.proxyPassword");
        proxyPort = Integer.valueOf(getTestParameter().get("integrationtest.proxyconfig.proxyHost").split(":")[1]);
    }

    @Test
    public void testCreateValidProxy() throws Exception {
        given(ProxyConfig.request()
                .withName(VALID_PROXY_CONFIG)
                .withServerHost(proxyHost)
                .withServerUser(proxyUser)
                .withPassword(proxyPassword)
                .withServerPort(proxyPort)
                .withProtocol("http")
                .withDescription(VALID_PROXY_DESC), "create valid proxy config"
            );
        when(ProxyConfig.post(), "post the request");
        then(ProxyConfig.assertThis(
                (proxyconfig, t) -> Assert.assertNotNull(proxyconfig.getResponse().getId(), "proxy config id must not be null"))
        );
        proxyConfigsToDelete.add(VALID_PROXY_CONFIG);

    }

    @Test
    public void testCreateValidProxyHttps() throws Exception {
        given(ProxyConfig.request()
                .withName(VALID_PROXY_CONFIG + "-https")
                .withServerHost(proxyHost)
                .withServerUser(proxyUser)
                .withPassword(proxyPassword)
                .withServerPort(proxyPort)
                .withProtocol("https"), "create valid proxy config with https protocol"
        );
        when(ProxyConfig.post(), "post the request");
        then(ProxyConfig.assertThis(
                (proxyconfig, t) -> Assert.assertNotNull(proxyconfig.getResponse().getId(), "proxy config id must not be null"))
        );
        proxyConfigsToDelete.add(VALID_PROXY_CONFIG + "-https");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateProxyLongName() throws Exception {
        given(ProxyConfig.request()
                .withName(longStringGeneratorUtil.stringGenerator(101))
                .withServerHost(proxyHost)
                .withServerUser(proxyUser)
                .withPassword(proxyPassword)
                .withServerPort(proxyPort)
                .withProtocol("http"), "create invalid proxy config with long name"
        );
        when(ProxyConfig.post(), "post the request");
        then(ProxyConfig.assertThis(
                (proxyconfig, t) -> Assert.assertNull(proxyconfig.getResponse().getId(), "proxy config id must be null"))
        );
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateProxyShortName() throws Exception {
        given(ProxyConfig.request()
                .withName("abc")
                .withServerHost(proxyHost)
                .withServerUser(proxyUser)
                .withPassword(proxyPassword)
                .withServerPort(proxyPort)
                .withProtocol("http"), "create invalid proxy config with short name"
        );
        when(ProxyConfig.post(), "post the request");
        then(ProxyConfig.assertThis(
                (proxyconfig, t) -> Assert.assertNull(proxyconfig.getResponse().getId(), "proxy config id must be null"))
        );
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateProxySpecialName() throws Exception {
        given(ProxyConfig.request()
                .withName(SPECIAL_PROXY_NAME)
                .withServerHost(proxyHost)
                .withServerUser(proxyUser)
                .withPassword(proxyPassword)
                .withServerPort(proxyPort)
                .withProtocol("http"), "create invalid proxy config with special name"
        );
        when(ProxyConfig.post(), "post the request");
        then(ProxyConfig.assertThis(
                (proxyconfig, t) -> Assert.assertNull(proxyconfig.getResponse().getId(), "proxy config id must be null"))
        );
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateProxyWithoutName() throws Exception {
        given(ProxyConfig.request()
                .withName("")
                .withServerHost(proxyHost)
                .withServerUser(proxyUser)
                .withPassword(proxyPassword)
                .withServerPort(proxyPort)
                .withProtocol("http"), "create invalid proxy config without name"
        );
        when(ProxyConfig.post(), "post the request");
        then(ProxyConfig.assertThis(
                (proxyconfig, t) -> Assert.assertNull(proxyconfig.getResponse().getId(), "proxy config id must be null"))
        );
    }

    @Test(expectedExceptions = BadRequestException.class, enabled = false)
    // Existing bug: BUG-99536
    public void testCreateProxyLongDesc() throws Exception {
        given(ProxyConfig.request()
                .withName(VALID_PROXY_CONFIG + "-longdescr")
                .withServerHost(proxyHost)
                .withServerUser(proxyUser)
                .withPassword(proxyPassword)
                .withServerPort(proxyPort)
                .withProtocol("http")
                .withDescription(longStringGeneratorUtil.stringGenerator(1001)), "create invalid proxy config with long description"
        );
        when(ProxyConfig.post(), "post the request");
        then(ProxyConfig.assertThis(
                (proxyconfig, t) -> Assert.assertNull(proxyconfig.getResponse().getId(), "proxy config id must be null"))
        );
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateProxyWithoutHost() throws Exception {
        given(ProxyConfig.request()
                .withName(VALID_PROXY_CONFIG + "-nohost")
                .withServerHost("")
                .withServerUser(proxyUser)
                .withPassword(proxyPassword)
                .withServerPort(proxyPort)
                .withProtocol("http"), "create invalid proxy config without host"
        );
        when(ProxyConfig.post(), "post the request");
        then(ProxyConfig.assertThis(
                (proxyconfig, t) -> Assert.assertNull(proxyconfig.getResponse().getId(), "proxy config id must be null"))
        );
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateProxyWithoutPort() throws Exception {
        given(ProxyConfig.request()
                .withName(VALID_PROXY_CONFIG + "-noport")
                .withServerHost(proxyHost)
                .withServerUser(proxyUser)
                .withPassword(proxyPassword)
                .withServerPort(null)
                .withProtocol("http"), "create invalid proxy config without port"
        );
        when(ProxyConfig.post(), "post the request");
        then(ProxyConfig.assertThis(
                (proxyconfig, t) -> Assert.assertNull(proxyconfig.getResponse().getId(), "proxy config id must be null"))
        );
    }

    @Test
    public void testCreateDeleteCreateAgain() throws Exception {
        given(ProxyConfig.isCreatedDeleted()
                .withName(VALID_PROXY_CONFIG + "-again")
                .withServerHost(proxyHost)
                .withServerUser(proxyUser)
                .withPassword(proxyPassword)
                .withServerPort(proxyPort)
                .withProtocol("http"), "create proxy config, then delete"
        );
        given(ProxyConfig.request()
                .withName(VALID_PROXY_CONFIG + "-again")
                .withServerHost(proxyHost)
                .withServerUser(proxyUser)
                .withPassword(proxyPassword)
                .withServerPort(proxyPort)
                .withProtocol("http"), "create proxy config with same name again"
        );
        when(ProxyConfig.post(), "post the request");
        then(ProxyConfig.assertThis(
                (proxyconfig, t) -> Assert.assertNotNull(proxyconfig.getResponse().getId(), "proxy config id must be null"))
        );
        proxyConfigsToDelete.add(VALID_PROXY_CONFIG + "-again");
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void testCreateProxyWithSameName() throws Exception {
        given(ProxyConfig.isCreated()
                .withName(VALID_PROXY_CONFIG + "-same")
                .withServerHost(proxyHost)
                .withServerUser(proxyUser)
                .withPassword(proxyPassword)
                .withServerPort(proxyPort)
                .withProtocol("http"), "create proxy config"
        );

        given(ProxyConfig.request()
                .withName(VALID_PROXY_CONFIG + "-same")
                .withServerHost(proxyHost)
                .withServerUser(proxyUser)
                .withPassword(proxyPassword)
                .withServerPort(proxyPort)
                .withProtocol("http"), "create proxy with name already exists"
        );
        try {
            when(ProxyConfig.post(), "post the request");
            then(ProxyConfig.assertThis(
                    (proxyconfig, t) -> Assert.assertNull(proxyconfig.getResponse().getId(), "proxy config id must be null"))
            );
        } finally {
            proxyConfigsToDelete.add(VALID_PROXY_CONFIG + "-same");
        }
    }

    @AfterSuite
    public void cleanAll() throws Exception {
        for (String proxyConfig : proxyConfigsToDelete) {
            given(ProxyConfig.request()
                    .withName(proxyConfig)
            );
            when(ProxyConfig.delete());
        }
    }
}