package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.providers;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.AwsCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.azure.AzureCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.cumulus.CumulusYarnCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp.GcpCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.mock.MockCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack.OpenstackCredentialV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.yarn.YarnCredentialV4Parameters;

public enum CloudPlatform {

    AWS {
        @Override
        public Class<?> getRepresentativeClass() {
            return AwsCredentialV4Parameters.class;
        }
    },

    GCP {
        @Override
        public Class<?> getRepresentativeClass() {
            return GcpCredentialV4Parameters.class;
        }
    },

    AZURE {
        @Override
        public Class<?> getRepresentativeClass() {
            return AzureCredentialV4Parameters.class;
        }
    },

    OPENSTACK {
        @Override
        public Class<?> getRepresentativeClass() {
            return OpenstackCredentialV4Parameters.class;
        }
    },

    CUMULUS_YARN {
        @Override
        public Class<?> getRepresentativeClass() {
            return CumulusYarnCredentialV4Parameters.class;
        }
    },

    YARN {
        @Override
        public Class<?> getRepresentativeClass() {
            return YarnCredentialV4Parameters.class;
        }
    },

    MOCK {
        @Override
        public Class<?> getRepresentativeClass() {
            return MockCredentialV4Parameters.class;
        }
    };

    public abstract Class<?> getRepresentativeClass();

}
