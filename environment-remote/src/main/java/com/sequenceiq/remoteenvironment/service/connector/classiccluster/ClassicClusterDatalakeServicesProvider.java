package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.cloudera.cdp.servicediscovery.model.Application;
import com.cloudera.cdp.servicediscovery.model.DeploymentType;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.cloudbreak.cm.DataView;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.remoteenvironment.RemoteEnvironmentException;
import com.sequenceiq.remoteenvironment.exception.OnPremCMApiException;

@Component
class ClassicClusterDatalakeServicesProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassicClusterDatalakeServicesProvider.class);

    private static final String HDFS_SERVICE = "HDFS";

    private static final XPathExpression PROPERTY_EXPRESSION;

    static {
        try {
            PROPERTY_EXPRESSION = XPathFactory.newInstance().newXPath().compile("/configuration/property");
        } catch (XPathExpressionException e) {
            throw new IllegalStateException("Failed to create property expression", e);
        }
    }

    @Inject
    private ClassicClusterClouderaManagerApiClientProvider apiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    public DescribeDatalakeServicesResponse getDatalakeServices(OnPremisesApiProto.Cluster cluster) {
        ApiClient apiClient = apiClientProvider.getClouderaManagerV51Client(cluster);

        DescribeDatalakeServicesResponse response = new DescribeDatalakeServicesResponse();
        response.setClusterid(cluster.getClusterCrn());
        response.setDeploymentType(DeploymentType.PDL);
        response.setApplications(Map.of(
                HDFS_SERVICE, getHdfsApplication(cluster, apiClient)
        ));
        return response;
    }

    private Application getHdfsApplication(OnPremisesApiProto.Cluster cluster, ApiClient apiClient) {
        Application application = new Application();
        File hdfsClientConfigFile = getHdfsClientConfigFile(cluster, apiClient);
        try (ZipFile zipFile = new ZipFile(hdfsClientConfigFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                switch (entry.getName()) {
                    case "hadoop-conf/hdfs-site.xml" -> parseHdfsSiteXml(zipFile.getInputStream(entry), application);
                    case "hadoop-conf/core-site.xml" -> parseCoreSiteXml(zipFile.getInputStream(entry), application);
                    default -> LOGGER.trace("Skipping file {}", entry.getName());
                }
            }
            return application;
        } catch (IOException e) {
            String message = "Failed to read HDFS client config zip downloaded from Cloudera Manager";
            LOGGER.error(message, e);
            throw new OnPremCMApiException(message + ". Please contact Cloudera support to get this resolved.");
        }
    }

    private File getHdfsClientConfigFile(OnPremisesApiProto.Cluster cluster, ApiClient apiClient) {
        try {
            ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(apiClient);
            String clusterName = cluster.getName();
            ApiServiceList apiServiceList = servicesResourceApi.readServices(clusterName, DataView.SUMMARY.name());
            ApiService hdfsService = Objects.requireNonNullElse(apiServiceList.getItems(), List.<ApiService>of()).stream()
                    .filter(apiService -> HDFS_SERVICE.equals(apiService.getType()))
                    .findFirst()
                    .orElseThrow(() -> new RemoteEnvironmentException(
                            String.format("HDFS service not found on on-premises cluster '%s'.", clusterName)));
            return servicesResourceApi.getClientConfig(clusterName, hdfsService.getName());
        } catch (ApiException e) {
            String message = "Failed to get HDFS client config from Cloudera Manager";
            LOGGER.error(message, e);
            throw new OnPremCMApiException(message, e);
        }
    }

    private void parseHdfsSiteXml(InputStream hdfsSiteXml, Application application) {
        LOGGER.debug("Parsing hdfs-site.xml");
        Map<String, String> hdfsSiteConfiguration = parseConfigurationXml(hdfsSiteXml);
        addConfigurationItem(application, "dfs_nameservices", hdfsSiteConfiguration.get("dfs.nameservices"));
        hdfsSiteConfiguration.entrySet().stream()
                .filter(this::isHdfsHAConfig)
                .forEach(entry -> addConfigurationItem(application, entry.getKey(), entry.getValue()));
        LOGGER.debug("Finished parsing configuration entries from hdfs-site.xml");
    }

    private boolean isHdfsHAConfig(Map.Entry<String, String> entry) {
        return entry.getKey().startsWith("dfs.ha.namenodes.") || entry.getKey().matches("dfs\\.namenode\\..*-address\\..*");
    }

    private void parseCoreSiteXml(InputStream coreSiteXml, Application application) {
        LOGGER.debug("Parsing core-site.xml");
        Map<String, String> coreSiteConfiguration = parseConfigurationXml(coreSiteXml);
        coreSiteConfiguration.entrySet().stream()
                .filter(this::isFileSystemConfig)
                .forEach(entry -> addConfigurationItem(application, entry.getKey(), entry.getValue()));
        LOGGER.debug("Finished parsing configuration entries from core-site.xml");
    }

    private boolean isFileSystemConfig(Map.Entry<String, String> entry) {
        return "fs.defaultFS".equals(entry.getKey());
    }

    private Map<String, String> parseConfigurationXml(InputStream inputStream) {
        try {
            Map<String, String> configuration = new HashMap<>();
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
            NodeList nodes = (NodeList) PROPERTY_EXPRESSION.evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Element node = (Element) nodes.item(i);
                String name = node.getElementsByTagName("name").item(0).getTextContent();
                String value = node.getElementsByTagName("value").item(0).getTextContent();
                LOGGER.trace("Parsed configuration key {} from XML", name);
                configuration.put(name, value);
            }
            return configuration;
        } catch (Exception e) {
            String message = "Failed to parse XML configuration received from Cloudera Manager";
            LOGGER.error(message, e);
            throw new OnPremCMApiException(message + ". Please contact Cloudera support to get this resolved.");
        }
    }

    private void addConfigurationItem(Application application, String key, String value) {
        if (value != null) {
            LOGGER.debug("Adding {}={} configuration entry", key, value);
            application.putConfigItem(key, value);
        } else {
            LOGGER.debug("Configuration key {} does not have value", key);
        }
    }

}
