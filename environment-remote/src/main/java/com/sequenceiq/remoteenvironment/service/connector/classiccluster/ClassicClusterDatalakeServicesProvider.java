package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
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

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.cdp.servicediscovery.model.Application;
import com.cloudera.cdp.servicediscovery.model.DeploymentType;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.remoteenvironment.RemoteEnvironmentException;

@Component
class ClassicClusterDatalakeServicesProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassicClusterDatalakeServicesProvider.class);

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
                "HDFS", getHdfsApplication(cluster, apiClient)
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
            throw new RemoteEnvironmentException(message + ". Please contact Cloudera support to get this resolved.");
        }
    }

    private File getHdfsClientConfigFile(OnPremisesApiProto.Cluster cluster, ApiClient apiClient) {
        try {
            return clouderaManagerApiFactory.getServicesResourceApi(apiClient).getClientConfig(cluster.getName(), "hdfs");
        } catch (ApiException e) {
            String message = "Failed to get HDFS client config from Cloudera Manager";
            LOGGER.error(message, e);
            throw new RemoteEnvironmentException(message, e);
        }
    }

    private void parseHdfsSiteXml(InputStream hdfsSiteXml, Application application) {
        Map<String, String> hdfsSiteConfiguration = getConfigurationFromXml(hdfsSiteXml);
        if (hdfsSiteConfiguration.containsKey("dfs.nameservices")) {
            application.getConfig().put("dfs_nameservices", hdfsSiteConfiguration.get("dfs.nameservices"));
        }
        hdfsSiteConfiguration.entrySet().stream()
                .filter(this::isHdfsHAConfig)
                .forEach(entry -> application.putConfigItem(entry.getKey(), entry.getValue()));
    }

    private boolean isHdfsHAConfig(Map.Entry<String, String> entry) {
        return entry.getKey().startsWith("dfs.ha.namenodes.") || entry.getKey().matches("dfs\\.namenode\\..*-address\\..*");
    }

    private void parseCoreSiteXml(InputStream coreSiteXml, Application application) {
        Map<String, String> coreSiteConfiguration = getConfigurationFromXml(coreSiteXml);
        coreSiteConfiguration.entrySet().stream()
                .filter(this::isFileSystemConfig)
                .forEach(entry -> application.putConfigItem(entry.getKey(), entry.getValue()));
    }

    private boolean isFileSystemConfig(Map.Entry<String, String> entry) {
        return "fs.defaultFS".equals(entry.getKey());
    }

    private Map<String, String> getConfigurationFromXml(InputStream inputStream) {
        try {
            Map<String, String> configuration = new HashMap<>();
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
            NodeList nodes = (NodeList) PROPERTY_EXPRESSION.evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Element node = (Element) nodes.item(i);
                String name = node.getElementsByTagName("name").item(0).getTextContent();
                String value = node.getElementsByTagName("value").item(0).getTextContent();
                configuration.put(name, value);
            }
            return configuration;
        } catch (Exception e) {
            String message = "Failed to parse XML configuration received from Cloudera Manager";
            LOGGER.error(message, e);
            throw new RemoteEnvironmentException(message + ". Please contact Cloudera support to get this resolved.");
        }
    }

}
