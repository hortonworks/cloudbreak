package com.sequenceiq.cloudbreak.recipe.moduletest;

import static com.sequenceiq.cloudbreak.TestUtil.hostGroup;
import static com.sequenceiq.cloudbreak.TestUtil.rdsConfig;
import static com.sequenceiq.cloudbreak.recipe.util.RecipeTestUtil.generalBlueprintView;
import static com.sequenceiq.cloudbreak.recipe.util.RecipeTestUtil.generalClusterConfigs;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.rds.RdsType;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.recipe.testrepeater.TestFile;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

class RecipeModulTestModelProvider {

    private RecipeModulTestModelProvider() {
    }

    static TemplatePreparationObject testTemplatePreparationObject() {
        return getPreparedBuilder("master")
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withRdsConfigs(new HashSet<>(Collections.singleton(rdsConfig(RdsType.HIVE))))
                .build();
    }

    static TemplatePreparationObject.Builder getPreparedBuilder(String... hostNames) {
        SmartSenseSubscription smartSenseSubscription = new SmartSenseSubscription();
        smartSenseSubscription.setSubscriptionId("A-99900000-C-00000000");
        return TemplatePreparationObject.Builder.builder()
                .withGeneralClusterConfigs(generalClusterConfigs())
                .withBlueprintView(generalBlueprintView("", "2.6", "HDP"))
                .withSmartSenseSubscription(smartSenseSubscription)
                .withSharedServiceConfigs(datalakeSharedServiceConfig().get())
                .withRdsConfigs(Sets.newHashSet(rdsConfig(RdsType.RANGER), rdsConfig(RdsType.HIVE)))
                .withHostgroups(hostNames.length == 0 ? getHostGroups("master", "worker", "compute") : getHostGroups(hostNames));
    }

    private static Optional<SharedServiceConfigsView> datalakeSharedServiceConfig() {
        SharedServiceConfigsView sharedServiceConfigsView = new SharedServiceConfigsView();
        sharedServiceConfigsView.setDatalakeCluster(true);
        return Optional.of(sharedServiceConfigsView);
    }

    private static Set<HostGroup> getHostGroups(String... names) {
        Set<HostGroup> hostGroups = new HashSet<>(names.length);
        for (String name : names) {
            hostGroups.add(hostGroup(name));
        }
        return hostGroups;
    }

    static TestFile getTestFile(String fileName) throws IOException {
        return new TestFile(new File(fileName).toPath(), FileReaderUtils.readFileFromClasspath(fileName));
    }

    static String getFileName(String folder, String filename) {
        return folder + '/' + filename + ".recipe";
    }

}