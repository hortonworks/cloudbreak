package com.sequenceiq.cloudbreak.service.image;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.AmbariCatalog;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakImageCatalog;
import com.sequenceiq.cloudbreak.cloud.model.HDPInfo;

@Service
public class HdpInfoSearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HdpInfoSearchService.class);

    @Inject
    private ImageCatalogProvider imageCatalogProvider;

    public HDPInfo searchHDPInfo(String ambariVersion, String hdpVersion) {
        HDPInfo hdpInfo = null;
        if (ambariVersion != null && hdpVersion != null) {
            CloudbreakImageCatalog imageCatalog = imageCatalogProvider.getImageCatalog();
            hdpInfo = prefixSearch(imageCatalog, ambariVersion, hdpVersion);
        }
        return hdpInfo;
    }

    private List<AmbariCatalog> ambariPrefixMatch(CloudbreakImageCatalog imageCatalog, String ambariVersion) {
        if (imageCatalog == null) {
            return null;
        }
        List<AmbariCatalog> ambariCatalogs = imageCatalog.getAmbariVersions().stream()
                .filter(p -> p.getAmbariInfo().getVersion().startsWith(ambariVersion)).collect(Collectors.toList());
        Collections.sort(ambariCatalogs, Collections.reverseOrder(new VersionComparator()));
        LOGGER.info("Prefix matched Ambari versions: {}. Ambari search prefix: {}", ambariCatalogs, ambariVersion);
        return ambariCatalogs;
    }

    private AmbariCatalog selectLatestAmbariCatalog(List<AmbariCatalog> ambariCatalogs) {
        if (ambariCatalogs == null || ambariCatalogs.isEmpty()) {
            return null;
        }
        return ambariCatalogs.get(0);
    }

    private List<HDPInfo> hdpPrefixMatch(AmbariCatalog ambariCatalog, String hdpVersion) {
        if (ambariCatalog == null) {
            return null;
        }
        List<HDPInfo> hdpInfos = ambariCatalog.getAmbariInfo().getHdp().stream()
                .filter(p -> p.getVersion().startsWith(hdpVersion)).collect(Collectors.toList());
        Collections.sort(hdpInfos, Collections.reverseOrder(new VersionComparator()));
        LOGGER.info("Prefix matched HDP versions: {} for Ambari version: {}. HDP search prefix: {}", hdpInfos, ambariCatalog.getVersion(), hdpVersion);
        return hdpInfos;

    }

    private HDPInfo selectLatestHdpInfo(List<HDPInfo> hdpInfos) {
        if (hdpInfos == null || hdpInfos.isEmpty()) {
            return null;
        }
        return hdpInfos.get(0);
    }

    private HDPInfo prefixSearch(CloudbreakImageCatalog imageCatalog, String ambariVersion, String hdpVersion) {
        List<AmbariCatalog> ambariCatalogs = ambariPrefixMatch(imageCatalog, ambariVersion);
        AmbariCatalog ambariCatalog = selectLatestAmbariCatalog(ambariCatalogs);
        List<HDPInfo> hdpInfos = hdpPrefixMatch(ambariCatalog, hdpVersion);
        return selectLatestHdpInfo(hdpInfos);
    }

}
