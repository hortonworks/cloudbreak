package com.sequenceiq.cloudbreak.cloud.aws.common;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;

@Service
public class AwsConstants implements CloudConstant {

    public static final String[] VARIANTS = new String[] {
                CloudConstants.AWS,
                CloudConstants.AWS_NATIVE_GOV,
                CloudConstants.AWS_NATIVE
    };

    public static final Platform AWS_PLATFORM = Platform.platform(CloudConstants.AWS);

    public static final Variant AWS_DEFAULT_VARIANT = AwsVariant.AWS_VARIANT.variant();

    private AwsConstants() {
    }

    @Override
    public Platform platform() {
        return AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsVariant.AWS_VARIANT.variant();
    }

    @Override
    public String[] variants() {
        return VARIANTS;
    }

    public enum AwsVariant {
        AWS_VARIANT(CloudConstants.AWS),
        AWS_NATIVE_GOV_VARIANT(CloudConstants.AWS_NATIVE_GOV),
        AWS_NATIVE_VARIANT(CloudConstants.AWS_NATIVE);

        private final Variant variant;

        AwsVariant(String variant) {
            this.variant = Variant.variant(variant);
        }

        public Variant variant() {
            return variant;
        }
    }
}
