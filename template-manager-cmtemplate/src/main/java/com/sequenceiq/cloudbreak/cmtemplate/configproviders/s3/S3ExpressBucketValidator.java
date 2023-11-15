package com.sequenceiq.cloudbreak.cmtemplate.configproviders.s3;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_18;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.validation.S3ExpressBucketNameValidator;

@Component
public class S3ExpressBucketValidator extends S3ExpressBucketNameValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ExpressBucketValidator.class);

    public static boolean validateVersionForS3ExpressBucket(List<ClouderaManagerProduct> products) {
        LOGGER.info("Getting cdh product version for product details in template object: {}", products);
        if (products != null && !products.isEmpty()) {
            Optional<ClouderaManagerProduct> cdhProduct = products.stream().filter(p -> "CDH".equals(p.getName())).findAny();
            if (cdhProduct.isPresent() && isVersionNewerOrEqualThanLimited(cdhProduct.get().getVersion(), CLOUDERA_STACK_VERSION_7_2_18)) {
                LOGGER.info("CDH product version is : {}", cdhProduct.get().getVersion());
                return true;
            }
        }
        return false;
    }
}
