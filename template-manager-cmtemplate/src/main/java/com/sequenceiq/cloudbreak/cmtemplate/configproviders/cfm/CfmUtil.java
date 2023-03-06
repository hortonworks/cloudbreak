package com.sequenceiq.cloudbreak.cmtemplate.configproviders.cfm;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.nifi.NifiKnoxRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

public class CfmUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(NifiKnoxRoleConfigProvider.class);

    private static final String CFM = "CFM";

    private CfmUtil() {
    }

    public static Optional<ClouderaManagerProduct> getCfmProduct(TemplatePreparationObject source) {
        if (source.getProductDetailsView() != null) {
            Optional<ClouderaManagerProduct> cfm = source.getProductDetailsView().getProducts()
                    .stream()
                    .filter(e -> e.getName().equalsIgnoreCase(CFM))
                    .findFirst();
            if (cfm.isEmpty()) {
                LOGGER.info("CFM is not presented as a Production");
            }
            return cfm;
        }
        LOGGER.info("Product null for Cfm Util");
        return Optional.empty();
    }

}
