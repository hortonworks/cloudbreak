package com.sequenceiq.cloudbreak.blueprint.moduletest;

import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.testrepeater.TestFile;
import com.sequenceiq.cloudbreak.blueprint.testrepeater.TripleTestData;

class BlueprintDataProvider extends TripleTestData<TestFile, TestFile, BlueprintPreparationObject> {

    BlueprintDataProvider(TestFile input, TestFile output, BlueprintPreparationObject model) {
        super(input, output, model);
    }

    TestFile getInput() {
        return getData1();
    }

    TestFile getOutput() {
        return getData2();
    }

    BlueprintPreparationObject getModel() {
        return getData3();
    }

}
