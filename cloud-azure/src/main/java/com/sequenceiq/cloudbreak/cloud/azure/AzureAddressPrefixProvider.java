package com.sequenceiq.cloudbreak.cloud.azure;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.network.fluent.models.SubnetInner;
import com.azure.resourcemanager.network.models.Subnet;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@Component
public class AzureAddressPrefixProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureAddressPrefixProvider.class);

    public String getAddressPrefix(Subnet subnet) {
        String resultAddressPrefix = null;
        SubnetInner subnetInner = subnet.innerModel();
        String addressPrefix = subnet.addressPrefix();
        if (!Strings.isNullOrEmpty(addressPrefix)) {
            LOGGER.info("The root level addressPrefix not null so using that as addressPrefix: " + addressPrefix);
            resultAddressPrefix = addressPrefix;
        } else if (subnetInner != null) {
            if (CollectionUtils.isNotEmpty(subnetInner.addressPrefixes())) {
                resultAddressPrefix = subnetInner.addressPrefixes().get(0);
                LOGGER.info("The addressPrefix in inner subnet not null so using that as addressPrefix: " + resultAddressPrefix);
            }
        }
        if (Strings.isNullOrEmpty(resultAddressPrefix)) {
            LOGGER.info("The root level addressPrefix and inner level addressPrefix null so " +
                    "sending back null as addressPrefix for subnet: " + subnet.id());
            throw new BadRequestException("Your subnet " + subnet.id() + " does not have a proper address prefix. Please fix that.");
        }
        return resultAddressPrefix;
    }
}
