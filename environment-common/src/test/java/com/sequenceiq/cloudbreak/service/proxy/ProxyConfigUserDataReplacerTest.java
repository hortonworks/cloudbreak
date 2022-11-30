package com.sequenceiq.cloudbreak.service.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.dto.ProxyAuthentication;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@ExtendWith(MockitoExtension.class)
class ProxyConfigUserDataReplacerTest {

    private static final String ENV_CRN = "env-crn";

    private static String noProxyUserData;

    private static String proxyUserData;

    @Mock
    private ProxyConfigDtoService proxyConfigDtoService;

    @InjectMocks
    private ProxyConfigUserDataReplacer underTest;

    @BeforeAll
    static void init() throws IOException {
        noProxyUserData = FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/service/proxy/userdata/no-proxy-userdata");
        proxyUserData = FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/service/proxy/userdata/proxy-userdata");
    }

    @BeforeEach
    void setUp() {
        ProxyConfig proxyConfig = ProxyConfig.builder()
                .withCrn("crn")
                .withName("name")
                .withServerHost("1.2.3.4")
                .withServerPort(1234)
                .withProtocol("http")
                .withProxyAuthentication(ProxyAuthentication.builder().withUserName("username").withPassword("password").build())
                .withNoProxyHosts("noproxy")
                .build();
        lenient().when(proxyConfigDtoService.getByEnvironmentCrn(ENV_CRN)).thenReturn(Optional.of(proxyConfig));
    }

    @Test
    void addProxy() {
        String result = underTest.replaceProxyConfigInUserDataByEnvCrn(noProxyUserData, ENV_CRN);

        assertEquals(proxyUserData, result);
    }

    @Test
    void removeProxy() {
        when(proxyConfigDtoService.getByEnvironmentCrn(ENV_CRN)).thenReturn(Optional.empty());

        String result = underTest.replaceProxyConfigInUserDataByEnvCrn(proxyUserData, ENV_CRN);

        assertEquals(noProxyUserData, result);
    }

    @Test
    void changeProxy() {
        ProxyConfig newProxyConfig = ProxyConfig.builder()
                .withCrn("crn")
                .withName("name")
                .withServerHost("1.2.3.5")
                .withServerPort(1235)
                .withProtocol("https")
                .withProxyAuthentication(ProxyAuthentication.builder().withUserName("username2").withPassword("password2").build())
                .withNoProxyHosts("noproxy2")
                .build();
        when(proxyConfigDtoService.getByEnvironmentCrn(ENV_CRN)).thenReturn(Optional.of(newProxyConfig));

        String result = underTest.replaceProxyConfigInUserDataByEnvCrn(proxyUserData, ENV_CRN);

        assertFalse(result.contains("IS_PROXY_ENABLED=false\n"));
        assertTrue(result.contains("IS_PROXY_ENABLED=true\n"));
        assertFalse(result.contains("PROXY_HOST=1.2.3.4\n"));
        assertTrue(result.contains("PROXY_HOST=1.2.3.5\n"));
        assertFalse(result.contains("PROXY_PORT=1234\n"));
        assertTrue(result.contains("PROXY_PORT=1235\n"));
        assertFalse(result.contains("PROXY_PROTOCOL=http\n"));
        assertTrue(result.contains("PROXY_PROTOCOL=https\n"));
        assertFalse(result.contains("PROXY_USER=\"username\"\n"));
        assertTrue(result.contains("PROXY_USER=\"username2\"\n"));
        assertFalse(result.contains("PROXY_PASSWORD=\"password\"\n"));
        assertTrue(result.contains("PROXY_PASSWORD=\"password2\"\n"));
        assertFalse(result.contains("PROXY_NO_PROXY_HOSTS=\"noproxy\"\n"));
        assertTrue(result.contains("PROXY_NO_PROXY_HOSTS=\"noproxy2\"\n"));
    }

}
