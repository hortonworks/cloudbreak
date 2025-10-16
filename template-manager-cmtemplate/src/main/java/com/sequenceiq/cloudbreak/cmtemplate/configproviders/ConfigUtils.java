package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateVariable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.views.RdsView;

public class ConfigUtils {

    private static final String CM_SAFETY_VALVE_PROPERTY_FORMAT = "<property><name>%s</name><value>%s</value></property>";

    private static final String CM_SAFETY_VALVE_CONFIGURATION_FORMAT = "<configuration>%s</configuration>";

    private static final Pattern STORAGE_LOCATION_PATH_PATTERN = Pattern.compile("(\\w+)(://([\\w-@\\.]+))(?:/(.*))?");

    private ConfigUtils() { }

    /**
     * Convenience method for creating an {@link ApiClusterTemplateConfig} with a value.
     *
     * @param name config name
     * @param value config value
     * @return config object
     */
    public static ApiClusterTemplateConfig config(String name, String value) {
        return new ApiClusterTemplateConfig().name(name).value(value);
    }

    /**
     * Convenience method for creating an {@link ApiClusterTemplateConfig} with a variable.
     *
     * @param name config name
     * @param variable config variable
     * @return config object
     */
    public static ApiClusterTemplateConfig configVar(String name, String variable) {
        return new ApiClusterTemplateConfig().name(name).variable(variable);
    }

    /**
     * Convenience method for creating an {@link ApiClusterTemplateVariable}.
     *
     * @param name variable name
     * @param value variable value
     * @return variable object
     */
    public static ApiClusterTemplateVariable variable(String name, String value) {
        return new ApiClusterTemplateVariable().name(name).value(value);
    }

    public static RdsView getRdsViewOfType(DatabaseType databaseType, TemplatePreparationObject source) {
        return source.getRdsView(databaseType);
    }

    public static Optional<StorageLocationView> getStorageLocationForServiceProperty(TemplatePreparationObject source, String serviceProperty) {
        return source.getFileSystemConfigurationView().flatMap(configview -> configview.getLocations().stream()
                .filter(s -> s.getProperty().equalsIgnoreCase(serviceProperty))
                .findFirst());
    }

    public static String getSafetyValveProperty(String key, String value) {
        return String.format(CM_SAFETY_VALVE_PROPERTY_FORMAT, key, StringEscapeUtils.escapeXml11(value));
    }

    public static String getSafetyValveConfiguration(String value) {
        return String.format(CM_SAFETY_VALVE_CONFIGURATION_FORMAT, value);
    }

    public static String getBasePathFromStorageLocation(String path) {
        return STORAGE_LOCATION_PATH_PATTERN.matcher(path).replaceAll("$1$2");
    }

    public static String getDbTypePostgres(RdsView rdsView, String serviceType) {
        if (!(rdsView != null && rdsView.getDatabaseVendor() == DatabaseVendor.POSTGRES)) {
            DatabaseVendor dbVendor = rdsView != null ? rdsView.getDatabaseVendor() : null;
            throw new CloudbreakServiceException(String.format("Unsupported database type: %s for service %s", dbVendor, serviceType));
        }
        return DatabaseVendor.POSTGRES.name();
    }

    public static String getCmVersion(TemplatePreparationObject source) {
        return source.getProductDetailsView().getCm().getVersion();
    }

    public static String getCdhVersion(TemplatePreparationObject source) {
        return source.getBlueprintView().getProcessor().getVersion().orElse("");
    }

}