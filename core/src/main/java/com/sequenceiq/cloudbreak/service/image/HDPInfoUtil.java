package com.sequenceiq.cloudbreak.service.image;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.AmbariCatalog;
import com.sequenceiq.cloudbreak.cloud.model.AmbariInfo;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakImageCatalog;
import com.sequenceiq.cloudbreak.cloud.model.HDPInfo;

@Component
public class HdpInfoUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(HdpInfoUtil.class);

    @Inject
    private ImageCatalogProvider imageCatalogProvider;

    public HDPInfo getHDPInfo(String ambariVersion, String hdpVersion) {
        HDPInfo hdpInfo = null;
        if (ambariVersion != null && hdpVersion != null) {
            CloudbreakImageCatalog imageCatalog = imageCatalogProvider.getImageCatalog();
            if (imageCatalog != null) {
                hdpInfo = search(imageCatalog, ambariVersion, hdpVersion);
            }
        }
        return hdpInfo;
    }

    private HDPInfo search(CloudbreakImageCatalog imageCatalog, String ambariVersion, String hdpVersion) {
        Optional<AmbariInfo> ambari = imageCatalog.getAmbariVersions().stream().map(AmbariCatalog::getAmbariInfo)
                .filter(amb -> amb.getVersion().equals(ambariVersion)).findFirst();
        if (ambari.isPresent()) {
            Optional<HDPInfo> hdpInfo = ambari.get().getHdp().stream().filter(hdp -> hdp.getVersion().equals(hdpVersion)).findFirst();
            if (hdpInfo.isPresent()) {
                return hdpInfo.get();
            }
        }
        return null;
    }
}
