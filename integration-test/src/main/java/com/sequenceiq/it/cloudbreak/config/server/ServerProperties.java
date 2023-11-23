package com.sequenceiq.it.cloudbreak.config.server;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.it.cloudbreak.mock.CBVersion;

@Configuration
public class ServerProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerProperties.class);

    private static final int DEFAULT_CLOUDBREAK_PORT = 9091;

    private static final int DEFAULT_ENVIRONMENT_PORT = 8088;

    private static final int DEFAULT_FREEIPA_PORT = 8090;

    @Value("${cloudbreak.url:localhost:" + DEFAULT_CLOUDBREAK_PORT + "}")
    private String cloudbreakUrl;

    @Value("${integrationtest.cloudbreak.server}")
    private String cloudbreakServer;

    @Value("${server.contextPath:/cb}")
    private String cbRootContextPath;

    @Value("${integrationtest.environment.server}")
    private String environmentServer;

    @Value("${environment.url:localhost:" + DEFAULT_ENVIRONMENT_PORT + "}")
    private String environmentUrl;

    @Value("${environment.server.contextPath:/environmentservice}")
    private String environmentRootContextPath;

    @Value("${integrationtest.freeipa.server}")
    private String freeipaServer;

    @Value("${freeipa.url:localhost:" + DEFAULT_FREEIPA_PORT + "}")
    private String freeipaUrl;

    @Value("${freeipa.server.contextPath:/freeipa}")
    private String freeIpaRootContextPath;

    @Value("${integrationtest.redbeams.server}")
    private String redbeamsServer;

    @Value("${redbeams.server.contextPath:/redbeams}")
    private String redbeamsRootContextPath;

    @Value("${integrationtest.sdx.server}")
    private String sdxServer;

    @Value("${sdx.server.contextPath:/dl}")
    private String sdxRootContextPath;

    @Value("${integrationtest.ums.host:localhost}")
    private String umsHost;

    @Value("${integrationtest.ums.port:8982}")
    private int umsPort;

    @Value("${integrationtest.authdistributor.host:localhost}")
    private String authDistributorHost;

    @Value("${mock.imagecatalog.server:localhost:10090}")
    private String mockImageCatalogAddr;

    @Value("${integrationtest.periscope.server}")
    private String periscopeServer;

    @Value("${integrationtest.periscope.port:8085}")
    private int periscopePort;

    @Value("${periscope.server.contextPath:/as}")
    private String periscopeRootContextPath;

    private String cbVersion;

    @Inject
    private CliProfileReaderService cliProfileReaderService;

    @Inject
    private ServerUtil serverUtil;

    public String getCloudbreakAddress() {
        return cloudbreakServer + cbRootContextPath;
    }

    public String getCloudbreak() {
        return cloudbreakServer;
    }

    public String getCloudbreakInternalAddress() {
        return "http://" + cloudbreakUrl + cbRootContextPath;
    }

    public String getEnvironmentAddress() {
        return environmentServer + environmentRootContextPath;
    }

    public String getEnvironmentInternalAddress() {
        return "http://" + environmentUrl + environmentRootContextPath;
    }

    public String getFreeipaAddress() {
        return freeipaServer + freeIpaRootContextPath;
    }

    public String getFreeipaInternalAddress() {
        return "http://" + freeipaUrl + freeIpaRootContextPath;
    }

    public String getRedbeamsAddress() {
        return redbeamsServer + redbeamsRootContextPath;
    }

    public String getPeriscopeAddress() {
        return periscopeServer + ":" + periscopePort + periscopeRootContextPath;
    }

    public String getSdxAddress() {
        return sdxServer + sdxRootContextPath;
    }

    public String getUmsHost() {
        return umsHost;
    }

    public int getUmsPort() {
        return umsPort;
    }

    public String getAuthDistributorHost() {
        return authDistributorHost;
    }

    public String getMockImageCatalogAddr() {
        return mockImageCatalogAddr;
    }

    public String getCbVersion() {
        return cbVersion;
    }

    @PostConstruct
    private void init() throws IOException {

        configureFromCliProfile();

        checkNonEmpty("integrationtest.cloudbreak.server", cloudbreakServer);
        checkNonEmpty("cloudbreak.url", cloudbreakUrl);
        checkNonEmpty("server.contextPath", cbRootContextPath);

        cbVersion = getCloudbreakUnderTestVersion(getCloudbreakAddress());
    }

    private String getCloudbreakUnderTestVersion(String cbServerAddress) {
        WebTarget target;
        Client client = RestClientUtil.get();
        if (cbServerAddress.contains("dps.mow") || cbServerAddress.contains("cdp.mow") || cbServerAddress.contains("cdp-priv.mow")) {
            target = client.target(getCloudbreak() + "/cloud/cb/info");
        } else {
            target = client.target(cbServerAddress + "/info");
        }
        Invocation.Builder request = target.request();
        try (Response response = request.get()) {
            CBVersion cbVersion = response.readEntity(CBVersion.class);
            String appVersion = cbVersion.getApp().getVersion();
            LOGGER.info("CB version: Appname: {}, version: {}", cbVersion.getApp().getName(), appVersion);
            MDC.put("cbversion", appVersion);
            return appVersion;
        } catch (Exception e) {
            LOGGER.error(String.format("Cannot fetch the CB version at '%s'", cbServerAddress), e);
            throw e;
        }
    }

    private void checkNonEmpty(String name, String value) {
        if (StringUtils.isBlank(value)) {
            throw new NullPointerException(String.format("Following variable must be set whether as environment variables or (test) application.yml:: %s",
                    name.replaceAll("\\.", "_").toUpperCase(Locale.ROOT)));
        }
    }

    private void configureFromCliProfile() throws IOException {
        Map<String, String> profiles = cliProfileReaderService.read();
        if (profiles == null) {
            LOGGER.warn("localhost in ~/.dp/config or "
                    + "integrationtest.dp.profile in application.yml or "
                    + "-Dintegrationtest.dp.profile should be added with exited profile");
            return;
        }

        cloudbreakServer = serverUtil.calculateServerAddressFromProfile(cloudbreakServer, profiles);
        freeipaServer = serverUtil.calculateServerAddressFromProfile(freeipaServer, profiles);
        sdxServer = serverUtil.calculateServerAddressFromProfile(sdxServer, profiles);
        environmentServer = serverUtil.calculateServerAddressFromProfile(environmentServer, profiles);
        redbeamsServer = serverUtil.calculateServerAddressFromProfile(redbeamsServer, profiles);
    }
}
