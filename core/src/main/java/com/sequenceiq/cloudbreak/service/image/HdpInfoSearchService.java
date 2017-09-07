package com.sequenceiq.cloudbreak.service.image;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.cloud.model.AmbariCatalog;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakImageCatalog;
import com.sequenceiq.cloudbreak.cloud.model.HDPInfo;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;

@Service
public class HdpInfoSearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HdpInfoSearchService.class);

    @Value("${info.app.version:}")
    private String cbVersion;

    @Inject
    private ImageCatalogProvider imageCatalogProvider;

    public HDPInfo searchHDPInfo(String platform, String ambariVersion, String hdpVersion, String imageCatalogUrl) throws CloudbreakImageNotFoundException {
        HDPInfo hdpInfo = null;
        if (ambariVersion != null && hdpVersion != null) {
            CloudbreakImageCatalog imageCatalog = imageCatalogProvider.getImageCatalog(imageCatalogUrl);
            hdpInfo = prefixSearch(imageCatalog, platform, cbVersion, ambariVersion, hdpVersion);
            if (hdpInfo == null) {
                throw new CloudbreakImageNotFoundException(
                        String.format("Failed to determine VM image from catalog! Cloudbreak version: %s, "
                                        + "Ambari version: %s, HDP Version: %s, Image Catalog Url: %s",
                                cbVersion, ambariVersion, hdpVersion,
                                imageCatalogUrl != null ? imageCatalogUrl : imageCatalogProvider.getDefaultCatalogUrl()));
            }
        }
        return hdpInfo;
    }

    private List<AmbariCatalog> ambariPrefixMatch(CloudbreakImageCatalog imageCatalog, String platform, String cbVersion, String ambariVersion,
            String hdpVersion) {
        if (imageCatalog == null) {
            return null;
        }
        List<AmbariCatalog> ambariCatalog;
        if (StringUtils.isEmpty(cbVersion) || "unspecified".equals(cbVersion)) {
            ambariCatalog = new ArrayList<>(imageCatalog.getAmbariVersions());
        } else {
            ambariCatalog = imageCatalog.getAmbariVersions().stream().filter(p -> p.getAmbariInfo().getCbVersions().contains(cbVersion))
                    .collect(Collectors.toList());
            if (CollectionUtils.isEmpty(ambariCatalog) && cbVersion.contains("dev")) {
                ambariCatalog = new ArrayList<>(imageCatalog.getAmbariVersions());
            }
        }
        List<AmbariCatalog> ambariCatalogs = ambariCatalog.stream()
                .filter(p -> p.getAmbariInfo().getVersion().startsWith(ambariVersion))
                .filter(p -> p.getAmbariInfo().getHdp().stream().anyMatch(hdp -> hdp.getVersion().startsWith(hdpVersion)))
                .filter(p -> p.getAmbariInfo().getHdp().stream().anyMatch(hdp -> hdp.getImages().containsKey(platform)))
                .collect(Collectors.toList());
        if (!StringUtils.isEmpty(cbVersion) && cbVersion.contains("dev")) {
            List<AmbariCatalog> filteredCatalogs = cloudbreakVerionPrefixMatch(extractCbVersion(cbVersion), ambariCatalogs);
            ambariCatalogs = !filteredCatalogs.isEmpty() ? filteredCatalogs : ambariCatalogs;
        }
        ambariCatalogs.sort(new VersionComparator());
        LOGGER.info("Prefix matched Ambari versions: {}. Ambari search prefix: {}", ambariCatalogs, ambariVersion);
        return ambariCatalogs;
    }

    private AmbariCatalog selectLatestAmbariCatalog(List<AmbariCatalog> ambariCatalogs) {
        if (ambariCatalogs == null || ambariCatalogs.isEmpty()) {
            return null;
        }
        return ambariCatalogs.get(ambariCatalogs.size() - 1);
    }

    private List<HDPInfo> hdpPrefixMatch(AmbariCatalog ambariCatalog, String hdpVersion) {
        if (ambariCatalog == null) {
            return null;
        }
        List<HDPInfo> hdpInfos = ambariCatalog.getAmbariInfo().getHdp().stream()
                .filter(p -> p.getVersion().startsWith(hdpVersion)).collect(Collectors.toList());
        hdpInfos.sort(new VersionComparator());
        LOGGER.info("Prefix matched HDP versions: {} for Ambari version: {}. HDP search prefix: {}", hdpInfos, ambariCatalog.getVersion(), hdpVersion);
        return hdpInfos;

    }

    private HDPInfo selectLatestHdpInfo(List<HDPInfo> hdpInfos) {
        if (hdpInfos == null || hdpInfos.isEmpty()) {
            return null;
        }
        return hdpInfos.get(hdpInfos.size() - 1);
    }

    private List<AmbariCatalog> cloudbreakVerionPrefixMatch(String cbVersion, List<AmbariCatalog> ambariCatalogs) {
        return ambariCatalogs.stream()
                .filter(p -> p.getAmbariInfo().getCbVersions().stream().anyMatch(cb -> cb.startsWith(cbVersion)))
                .collect(Collectors.toList());
    }

    // E.g. 2.1.0-dev.62 --> 2.1.0
    private String extractCbVersion(String cbVersion) {
        Matcher matcher = Pattern.compile("^\\d+\\.\\d+\\.\\d+").matcher(cbVersion);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return cbVersion;
    }

    private HDPInfo prefixSearch(CloudbreakImageCatalog imageCatalog, String platform, String cbVersion, String ambariVersion, String hdpVersion) {
        List<AmbariCatalog> ambariCatalogs = ambariPrefixMatch(imageCatalog, platform, cbVersion, ambariVersion, hdpVersion);
        AmbariCatalog ambariCatalog = selectLatestAmbariCatalog(ambariCatalogs);
        List<HDPInfo> hdpInfos = hdpPrefixMatch(ambariCatalog, hdpVersion);
        return selectLatestHdpInfo(hdpInfos);
    }
}
