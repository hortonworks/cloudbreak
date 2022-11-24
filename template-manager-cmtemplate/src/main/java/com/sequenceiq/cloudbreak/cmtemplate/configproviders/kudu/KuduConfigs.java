package com.sequenceiq.cloudbreak.cmtemplate.configproviders.kudu;

public class KuduConfigs {

    static final String RANGER_KUDU_PLUGIN_SERVICE_NAME = "ranger_kudu_plugin_service_name";

    static final String GENERATED_RANGER_SERVICE_NAME = "{{GENERATED_RANGER_SERVICE_NAME}}";

    static final String KUDU_FS_WAL_DIRS = "fs_wal_dir";

    static final String KUDU_FS_DATA_DIRS = "fs_data_dirs";

    private KuduConfigs() { }
}
