package com.sequenceiq.it.cloudbreak.config.server;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.ClientProperties;
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

    private static final int DEFAULT_SDX_PORT = 8086;

    private static final int DEFAULT_EXTERNALIZED_COMPUTE_PORT = 8091;

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

    @Value("${sdx.url:localhost:" + DEFAULT_SDX_PORT + "}")
    private String sdxUrl;

    @Value("${sdx.server.contextPath:/dl}")
    private String sdxRootContextPath;

    @Value("${integrationtest.externalizedcompute.server}")
    private String externalizedComputeServer;

    @Value("${externalizedcompute.url:localhost:" + DEFAULT_EXTERNALIZED_COMPUTE_PORT + "}")
    private String externalizedComputeUrl;

    @Value("${externalizedcompute.server.contextPath:/externalizedcompute}")
    private String externalizedComputeRootContextPath;

    @Value("${integrationtest.ums.host:localhost}")
    private String umsHost;

    @Value("${integrationtest.ums.port:8982}")
    private int umsPort;

    @Value("${integrationtest.authdistributor.host:localhost}")
    private String authDistributorHost;

    @Value("${mock.imagecatalog.server:localhost}")
    private String mockImageCatalogAddr;

    @Value("${mock.imagecatalog.port:10090}")
    private String mockImageCatalogPort;

    @Value("${integrationtest.periscope.server}")
    private String periscopeServer;

    @Value("${integrationtest.periscope.port:8085}")
    private int periscopePort;

    @Value("${periscope.server.contextPath:/as}")
    private String periscopeRootContextPath;

    @Value("${integrationtest.cloudbreak.port:0}")
    private int cloudbreakPort;

    @Value("${integrationtest.freeipa.port:0}")
    private int freeipaPort;

    @Value("${integrationtest.sdx.port:0}")
    private int sdxPort;

    @Value("${integrationtest.environment.port:0}")
    private int environmentPort;

    @Value("${integrationtest.redbeams.port:0}")
    private int redbeamsPort;

    @Value("${integrationtest.externalizedcompute.port:0}")
    private int externalizedComputePort;

    @Value("${integrationtest.remoteenvironment.server}")
    private String remoteEnvironmentServer;

    @Value("${integrationtest.remoteenvironment.port:8092}")
    private int remoteEnvironmentPort;

    @Value("${integrationtest.remoteenvironment.contextPath:/remoteenvironmentservice}")
    private String remoteEnvironmentContextPath;

    @Value("${integrationtest.environmentpublicapi.server}")
    private String environmentPublicApiServer;

    @Value("${integrationtest.environmentpublicapi.port:0}")
    private int environmentPublicApiPort;

    @Value("${integrationtest.environmentpublicapi.contextPath:}")
    private String environmentPublicApiContextPath;

    @Value("${integrationtest.cloudbreak.alternative.server:}")
    private String alternativeCloudbreakServer;

    @Value("${cloudbreak.alternative.url:localhost:" + DEFAULT_CLOUDBREAK_PORT + "}")
    private String alternativeCloudbreakUrl;

    @Value("${integrationtest.environment.alternative.server:}")
    private String alternativeEnvironmentServer;

    @Value("${environment.alternative.url:localhost:" + DEFAULT_ENVIRONMENT_PORT + "}")
    private String alternativeEnvironmentUrl;

    @Value("${integrationtest.freeipa.alternative.server:}")
    private String alternativeFreeipaServer;

    @Value("${freeipa.alternative.url:localhost:" + DEFAULT_FREEIPA_PORT + "}")
    private String alternativeFreeipaUrl;

    @Value("${integrationtest.redbeams.alternative.server:}")
    private String alternativeRedbeamsServer;

    @Value("${integrationtest.sdx.alternative.server:}")
    private String alternativeSdxServer;

    @Value("${sdx.alternative.url:localhost:" + DEFAULT_SDX_PORT + "}")
    private String alternativeSdxUrl;

    @Value("${integrationtest.externalizedcompute.alternative.server:}")
    private String alternativeExternalizedComputeServer;

    @Value("${externalizedcompute.alternative.url:localhost:" + DEFAULT_EXTERNALIZED_COMPUTE_PORT + "}")
    private String alternativeExternalizedComputeUrl;

    @Value("${integrationtest.remoteenvironment.alternative.server:}")
    private String alternativeRemoteEnvironmentServer;

    @Value("${integrationtest.environmentpublicapi.alternative.server:}")
    private String alternativeEnvironmentPublicApiServer;

    @Value("${integrationtest.periscope.alternative.server:}")
    private String alternativePeriscopeServer;

    private String cbVersion;

    @Inject
    private CliProfileReaderService cliProfileReaderService;

    @Inject
    private ServerUtil serverUtil;

    public String getCloudbreakAddress() {
        if (cloudbreakPort != 0) {
            return cloudbreakServer + ":" + cloudbreakPort + cbRootContextPath;
        } else {
            return cloudbreakServer + cbRootContextPath;
        }
    }

    public String getCloudbreak() {
        return cloudbreakServer;
    }

    public String getCloudbreakInternalAddress() {
        return "http://" + cloudbreakUrl + cbRootContextPath;
    }

    public String getAlternativeCloudbreakAddress() {
        if (StringUtils.isNotEmpty(alternativeCloudbreakServer)) {
            if (cloudbreakPort != 0) {
                return alternativeCloudbreakServer + ":" + cloudbreakPort + cbRootContextPath;
            } else {
                return alternativeCloudbreakServer + cbRootContextPath;
            }
        } else {
            return null;
        }
    }

    public String getAlternativeCloudbreakInternalAddress() {
        return "http://" + alternativeCloudbreakUrl + cbRootContextPath;
    }

    public String getAlternativeCloudbreak() {
        if (StringUtils.isNotEmpty(alternativeCloudbreakServer)) {
            return alternativeCloudbreakServer;
        } else {
            return null;
        }
    }

    public String getEnvironmentAddress() {
        if (environmentPort != 0) {
            return environmentServer + ":" + environmentPort + environmentRootContextPath;
        } else {
            return environmentServer + environmentRootContextPath;
        }
    }

    public String getEnvironmentInternalAddress() {
        return "http://" + environmentUrl + environmentRootContextPath;
    }

    public String getAlternativeEnvironmentAddress() {
        if (StringUtils.isNotEmpty(alternativeEnvironmentServer)) {
            if (environmentPort != 0) {
                return alternativeEnvironmentServer + ":" + environmentPort + environmentRootContextPath;
            } else {
                return alternativeEnvironmentServer + environmentRootContextPath;
            }
        } else {
            return null;
        }
    }

    public String getAlternativeEnvironmentInternalAddress() {
        return "http://" + alternativeEnvironmentUrl + environmentRootContextPath;
    }

    public String getFreeipaAddress() {
        if (freeipaPort != 0) {
            return freeipaServer + ":" + freeipaPort + freeIpaRootContextPath;
        } else {
            return freeipaServer + freeIpaRootContextPath;
        }
    }

    public String getFreeipaInternalAddress() {
        return "http://" + freeipaUrl + freeIpaRootContextPath;
    }

    public String getAlternativeFreeipaAddress() {
        if (StringUtils.isNotEmpty(alternativeFreeipaServer)) {
            if (freeipaPort != 0) {
                return alternativeFreeipaServer + ":" + freeipaPort + freeIpaRootContextPath;
            } else {
                return alternativeFreeipaServer + freeIpaRootContextPath;
            }
        } else {
            return null;
        }
    }

    public String getAlternativeFreeipaInternalAddress() {
        return "http://" + alternativeFreeipaUrl + freeIpaRootContextPath;
    }

    public String getRedbeamsAddress() {
        if (redbeamsPort != 0) {
            return redbeamsServer + ":" + redbeamsPort + redbeamsRootContextPath;
        } else {
            return redbeamsServer + redbeamsRootContextPath;
        }
    }

    public String getAlternativeRedbeamsAddress() {
        if (StringUtils.isNotEmpty(alternativeRedbeamsServer)) {
            if (redbeamsPort != 0) {
                return alternativeRedbeamsServer + ":" + redbeamsPort + redbeamsRootContextPath;
            } else {
                return alternativeRedbeamsServer + redbeamsRootContextPath;
            }
        } else {
            return null;
        }
    }

    public String getPeriscopeAddress() {
        return periscopeServer + ":" + periscopePort + periscopeRootContextPath;
    }

    public String getAlternativePeriscopeAddress() {
        if (StringUtils.isNotEmpty(alternativePeriscopeServer)) {
            return alternativePeriscopeServer + ":" + periscopePort + periscopeRootContextPath;
        } else {
            return null;
        }
    }

    public String getSdxAddress() {
        if (sdxPort != 0) {
            return sdxServer + ":" + sdxPort + sdxRootContextPath;
        } else {
            return sdxServer + sdxRootContextPath;
        }
    }

    public String getSdxInternalAddress() {
        return "http://" + sdxUrl + sdxRootContextPath;
    }

    public String getAlternativeSdxAddress() {
        if (StringUtils.isNotEmpty(alternativeSdxServer)) {
            if (sdxPort != 0) {
                return alternativeSdxServer + ":" + sdxPort + sdxRootContextPath;
            } else {
                return alternativeSdxServer + sdxRootContextPath;
            }
        } else {
            return null;
        }
    }

    public String getAlternativeSdxInternalAddress() {
        return "http://" + alternativeSdxUrl + sdxRootContextPath;
    }

    public String getRemoteEnvironmentAddress() {
        if (remoteEnvironmentPort != 0) {
            return remoteEnvironmentServer + ":" + remoteEnvironmentPort + remoteEnvironmentContextPath;
        } else {
            return remoteEnvironmentServer + remoteEnvironmentContextPath;
        }
    }

    public String getAlternativeRemoteEnvironmentAddress() {
        if (StringUtils.isNotEmpty(alternativeRemoteEnvironmentServer)) {
            if (remoteEnvironmentPort != 0) {
                return alternativeRemoteEnvironmentServer + ":" + remoteEnvironmentPort + remoteEnvironmentContextPath;
            } else {
                return alternativeRemoteEnvironmentServer + remoteEnvironmentContextPath;
            }
        } else {
            return null;
        }
    }

    public String getExternalizedComputeAddress() {
        if (externalizedComputePort != 0) {
            return externalizedComputeServer + ":" + externalizedComputePort + externalizedComputeRootContextPath;
        } else {
            return externalizedComputeServer + externalizedComputeRootContextPath;
        }
    }

    public String getExternalizedComputeInternalAddress() {
        return "http://" + externalizedComputeUrl + externalizedComputeRootContextPath;
    }

    public String getAlternativeExternalizedComputeAddress() {
        if (StringUtils.isNotEmpty(alternativeExternalizedComputeServer)) {
            if (externalizedComputePort != 0) {
                return alternativeExternalizedComputeServer + ":" + externalizedComputePort + externalizedComputeRootContextPath;
            } else {
                return alternativeExternalizedComputeServer + externalizedComputeRootContextPath;
            }
        } else {
            return null;
        }
    }

    public String getAlternativeExternalizedComputeInternalAddress() {
        return "http://" + alternativeExternalizedComputeUrl + externalizedComputeRootContextPath;
    }

    public String getEnvironmentPublicApiAddress() {
        if (environmentPublicApiPort != 0) {
            return environmentPublicApiServer + ":" + environmentPublicApiPort + environmentPublicApiContextPath;
        } else {
            return environmentPublicApiServer + environmentPublicApiContextPath;
        }
    }

    public String getAlternativeEnvironmentPublicApiAddress() {
        if (StringUtils.isNotEmpty(alternativeEnvironmentPublicApiServer)) {
            if (environmentPublicApiPort != 0) {
                return alternativeEnvironmentPublicApiServer + ":" + environmentPublicApiPort + environmentPublicApiContextPath;
            } else {
                return alternativeEnvironmentPublicApiServer + environmentPublicApiContextPath;
            }
        } else {
            return null;
        }
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
        return mockImageCatalogAddr + ":" + mockImageCatalogPort;
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
            target.property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE);
        } else {
            target = client.target(cbServerAddress + "/info");
        }
        LOGGER.info("CloudBreak Info endpoint: {}", target.getUri());
        Invocation.Builder request = target.request();
        Response response = null;
        try {
            response = request.get();
            if (response.getStatus() != 200) {
                response.bufferEntity();
                String responseBody = response.readEntity(String.class);
                LOGGER.info("HTTP response: Status code: {}, Reason: {}, Body: {}, Headers: {}",
                        response.getStatus(),
                        response.getStatusInfo().getReasonPhrase(),
                        responseBody != null ? responseBody : "No body content.",
                        response.getHeaders());
            }
            CBVersion cbVersion = response.readEntity(CBVersion.class);
            String appVersion = cbVersion.getApp().getVersion();
            LOGGER.info("CB version: Appname: {}, version: {}", cbVersion.getApp().getName(), appVersion);
            MDC.put("cbversion", appVersion);
            return appVersion;
        } catch (Exception e) {
            LOGGER.error("Cannot fetch the CB version at '{}'", target.getUri(), e);
            throw e;
        } finally {
            if (response != null) {
                response.close();
            }
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
