package com.sequenceiq.periscope.utils;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;

import com.openpojo.reflection.PojoClass;
import com.openpojo.reflection.PojoClassFilter;
import com.openpojo.reflection.impl.PojoClassFactory;
import com.openpojo.validation.Validator;
import com.openpojo.validation.ValidatorBuilder;
import com.openpojo.validation.affirm.Affirm;
import com.openpojo.validation.rule.impl.GetterMustExistRule;
import com.openpojo.validation.rule.impl.NoNestedClassRule;
import com.openpojo.validation.rule.impl.NoStaticExceptFinalRule;
import com.openpojo.validation.rule.impl.SetterMustExistRule;
import com.openpojo.validation.test.impl.GetterTester;
import com.openpojo.validation.test.impl.SetterTester;

public class ModelTest {

    private static final int EXPECTED_CLASS_COUNT = 14;

    private static final String DOMAIN_PACKAGE = "com.sequenceiq.periscope.domain";

    private static final Pattern TESTCLASS_NAME_PATTERN = Pattern.compile("\\S+?Test$");

    private static final Pattern BUILDERCLASS_NAME_PATTERN = Pattern.compile("^\\S+?Builder$");

    private static final PojoClassFilter BASE_CLASS_FILTER = pojoClass -> !pojoClass.isSynthetic()
            && !TESTCLASS_NAME_PATTERN.matcher(pojoClass.getName()).matches()
            && !BUILDERCLASS_NAME_PATTERN.matcher(pojoClass.getName()).matches();

    @Test
    public void ensureExpectedPojoCount() {
        List<PojoClass> pojoClasses = PojoClassFactory.getPojoClasses(DOMAIN_PACKAGE, BASE_CLASS_FILTER);
        Affirm.affirmEquals("Classes added / removed?", EXPECTED_CLASS_COUNT, pojoClasses.size());
    }

    @Test
    public void testPojoStructureAndBehavior() {
        Validator validator = ValidatorBuilder.create()
                .with(new SetterMustExistRule(),
                        new GetterMustExistRule())
                .with(new SetterTester(),
                        new GetterTester())
                .with(new NoStaticExceptFinalRule())
                .with(new NoNestedClassRule())
                .build();
        validator.validate(DOMAIN_PACKAGE, BASE_CLASS_FILTER);
    }
}
