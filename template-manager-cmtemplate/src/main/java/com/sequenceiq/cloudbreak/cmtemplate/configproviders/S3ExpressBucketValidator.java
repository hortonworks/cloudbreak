package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_18;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.util.S3ExpressBucketNameValidator;

@Component
public class S3ExpressBucketValidator extends S3ExpressBucketNameValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ExpressBucketValidator.class);

    public boolean validateVersionForS3ExpressBucket(TemplatePreparationObject source) {
        LOGGER.info("Getting cdh product version for product details in template object: {}", source.getProductDetailsView());
        Optional<ClouderaManagerProduct> cdhProduct = AbstractRoleConfigProvider.getCdhProduct(source);
        if (cdhProduct.isPresent() && isVersionNewerOrEqualThanLimited(cdhProduct.get().getVersion(), CLOUDERA_STACK_VERSION_7_2_18)) {
            LOGGER.info("CDH product version is : {}", cdhProduct.get().getVersion());
            return true;
        }
        return false;
    }
}
