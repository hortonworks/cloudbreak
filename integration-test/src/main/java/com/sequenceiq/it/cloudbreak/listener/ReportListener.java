package com.sequenceiq.it.cloudbreak.listener;

import static com.sequenceiq.it.cloudbreak.log.Log.log;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.TestListenerAdapter;

import com.google.common.collect.Iterables;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProviderProxy;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.context.MeasuredTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.search.ClusterLogsStorageUrl;
import com.sequenceiq.it.cloudbreak.search.KibanaSearchUrl;
import com.sequenceiq.it.cloudbreak.search.SearchUrl;
import com.sequenceiq.it.cloudbreak.search.Searchable;
import com.sequenceiq.it.cloudbreak.search.StorageUrl;

public class ReportListener extends TestListenerAdapter {

    public static final String SEARCH_URL = "searchUrl";

    public static final String FI_STORAGE_URL = "freeipaStorageUrl";

    public static final String DL_STORAGE_URL = "datalakeStorageUrl";

    public static final String DH_STORAGE_URL = "datahubStorageUrl";

    public static final String MEASUREMENTS = "measurements";

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportListener.class);

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Override
    public void onTestFailure(ITestResult tr) {
        logUrl(tr);
        logMeasurements(tr);
        log(tr);
        Reporter.setCurrentTestResult(tr);
        super.onTestFailure(tr);
    }

    @Override
    public void onTestSuccess(ITestResult tr) {
        logUrl(tr);
        logMeasurements(tr);
        Reporter.setCurrentTestResult(tr);
        super.onTestSuccess(tr);
    }

    @Override
    public void onTestSkipped(ITestResult tr) {
        logUrl(tr);
        logMeasurements(tr);
        Reporter.setCurrentTestResult(tr);
        super.onTestSkipped(tr);
    }

    private void logUrl(ITestResult tr) {
        TestContext testContext;
        Object[] parameters = tr.getParameters();
        if (parameters == null || parameters.length == 0) {
            return;
        }
        try {
            testContext = (TestContext) parameters[0];
        } catch (ClassCastException e) {
            return;
        }
        Iterable<Searchable> searchables = Iterables.filter(testContext.getResourceNames().values(), Searchable.class);
        List<Searchable> listOfSearchables = StreamSupport.stream(searchables.spliterator(), false).collect(Collectors.toList());
        if (listOfSearchables.size() == 0) {
            return;
        }
        SearchUrl searchUrl = new KibanaSearchUrl();
        tr.getTestContext().setAttribute(tr.getName() + SEARCH_URL,
                searchUrl.getSearchUrl(listOfSearchables, new Date(tr.getStartMillis()), new Date(tr.getEndMillis())));

        String baseLocation = getCloudStorageBaseLocation(testContext);
        CloudProviderProxy cloudProvider = testContext.getCloudProvider();
        generateClusterLogsUrl(FreeIpaTestDto.class, tr, testContext.getResourceNames(), testContext.getResourceCrns(), baseLocation, cloudProvider);
        generateClusterLogsUrl(SdxTestDto.class, tr, testContext.getResourceNames(), testContext.getResourceCrns(), baseLocation, cloudProvider);
        generateClusterLogsUrl(SdxInternalTestDto.class, tr, testContext.getResourceNames(), testContext.getResourceCrns(), baseLocation, cloudProvider);
        generateClusterLogsUrl(DistroXTestDto.class, tr, testContext.getResourceNames(), testContext.getResourceCrns(), baseLocation, cloudProvider);
    }

    private void logMeasurements(ITestResult tr) {
        Object[] parameters = tr.getParameters();
        MeasuredTestContext measuredTestContext;
        if (parameters == null || parameters.length == 0) {
            return;
        } else if (parameters[0] instanceof MeasuredTestContext) {
            measuredTestContext = (MeasuredTestContext) parameters[0];
        } else {
            return;
        }
        tr.setAttribute(tr.getName() + MEASUREMENTS, measuredTestContext.getMeasure());
    }

    private <T extends CloudbreakTestDto> void generateClusterLogsUrl(Class<T> dtoClass, ITestResult tr, Map<String, CloudbreakTestDto> resourceNames,
            Map<String, CloudbreakTestDto> resourceCrns, String baseLocation, CloudProviderProxy cloudProvider) {
        try {
            String resourceName = getResourceName(dtoClass, resourceNames);
            String resourceCrn = getResourceCrn(dtoClass, resourceCrns);
            if (StringUtils.isEmpty(resourceCrn)) {
                LOGGER.info("{} resource is not present in Cluster Logs", dtoClass.getSimpleName());
            } else {
                switch (dtoClass.getSimpleName()) {
                    case "FreeIpaTestDto":
                        getFreeIpaCloudStorageUrl(tr, resourceName, resourceCrn, baseLocation, cloudProvider);
                        break;
                    case "DistroXTestDto":
                        getDistroxCloudStorageUrl(tr, resourceName, resourceCrn, baseLocation, cloudProvider);
                        break;
                    case "SdxInternalTestDto":
                    case "SdxTestDto":
                        getSdxCloudStorageUrl(tr, resourceName, resourceCrn, baseLocation, cloudProvider);
                        break;
                    default:
                        LOGGER.warn("The given {} TestDTO is not in the list of Cluster Logs related testDTOs (freeIPA, Data Lake, Data Hub)!",
                                dtoClass.getSimpleName());
                        break;
                }
            }
        } catch (Exception e) {
            LOGGER.info("{} resource is not present in Test Context.", dtoClass.getSimpleName(), e);
        }
    }

    private String getFreeIpaCloudStorageUrl(ITestResult tr, String resourceName, String resourceCrn, String baseLocation, CloudProviderProxy cloudProvider) {
        StorageUrl storageUrl = new ClusterLogsStorageUrl();

        String freeIpaCloudStorageUrl = storageUrl.getFreeIpaStorageUrl(resourceName, resourceCrn, baseLocation, cloudProvider);
        tr.getTestContext().setAttribute(tr.getName() + FI_STORAGE_URL, freeIpaCloudStorageUrl);
        LOGGER.info("FreeIpa cloud storage: {}:{}", tr.getName() + FI_STORAGE_URL, freeIpaCloudStorageUrl);
        return freeIpaCloudStorageUrl;
    }

    private String getSdxCloudStorageUrl(ITestResult tr, String resourceName, String resourceCrn, String baseLocation, CloudProviderProxy cloudProvider) {
        StorageUrl storageUrl = new ClusterLogsStorageUrl();

        String datalakeCloudStorageUrl = storageUrl.getDatalakeStorageUrl(resourceName, resourceCrn, baseLocation, cloudProvider);
        tr.getTestContext().setAttribute(tr.getName() + DL_STORAGE_URL, datalakeCloudStorageUrl);
        LOGGER.info("Data lake cloud storage: {}:{}", tr.getName() + DL_STORAGE_URL, datalakeCloudStorageUrl);
        return datalakeCloudStorageUrl;
    }

    private String getDistroxCloudStorageUrl(ITestResult tr, String resourceName, String resourceCrn, String baseLocation, CloudProviderProxy cloudProvider) {
        StorageUrl storageUrl = new ClusterLogsStorageUrl();

        String datahubCloudStorageUrl = storageUrl.getDataHubStorageUrl(resourceName, resourceCrn, baseLocation, cloudProvider);
        tr.getTestContext().setAttribute(tr.getName() + DH_STORAGE_URL, datahubCloudStorageUrl);
        LOGGER.info("Data hub cloud storage: {}:{}", tr.getName() + DH_STORAGE_URL, datahubCloudStorageUrl);
        return datahubCloudStorageUrl;
    }

    private String getCloudStorageBaseLocation(TestContext testContext) {
        try {
            String storageBaseLocation = testContext.get(EnvironmentTestDto.class).getResponse().getTelemetry().getLogging().getStorageLocation();
            LOGGER.info("Cloud Storage base location: {}", storageBaseLocation);
            return storageBaseLocation;
        } catch (Exception e) {
            LOGGER.info("Cannot get Cloud Storage base location, because of {}", e.getMessage(), e);
            return null;
        }
    }

    private <T extends CloudbreakTestDto> String getResourceName(Class<T> dtoClass, Map<String, CloudbreakTestDto> resourceNames) {
        return resourceNames.values().stream()
                .filter(cloudbreakTestDto -> dtoClass.getSimpleName().equalsIgnoreCase(cloudbreakTestDto.getClass().getSimpleName()))
                .map(CloudbreakTestDto::getName)
                .findAny()
                .orElse(null);
    }

    private <T extends CloudbreakTestDto> String getResourceCrn(Class<T> dtoClass, Map<String, CloudbreakTestDto> resourceCrns) {
        return resourceCrns.entrySet().stream()
                .filter(resourceMap -> dtoClass.getSimpleName().equalsIgnoreCase(resourceMap.getValue().getClass().getSimpleName()))
                .map(Map.Entry::getKey)
                .findAny()
                .orElse(null);
    }
}


