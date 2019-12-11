package com.sequenceiq.it.cloudbreak.listener;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import com.google.common.collect.Iterables;
import com.sequenceiq.it.cloudbreak.context.MeasuredTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.search.KibanaSearchUrl;
import com.sequenceiq.it.cloudbreak.search.SearchUrl;
import com.sequenceiq.it.cloudbreak.search.Searchable;

public class ReportListener extends TestListenerAdapter {
    public static final String SEARCH_URL = "searchUrl";

    public static final String MEASUREMENTS = "measurements";

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportListener.class);

    @Override
    public void onTestFailure(ITestResult tr) {
        super.onTestFailure(tr);
        logUrl(tr);
        logMeasurements(tr);
        Throwable throwable = tr.getThrowable();
        Log.log(throwable.getMessage());
    }

    @Override
    public void onTestSuccess(ITestResult tr) {
        logUrl(tr);
        logMeasurements(tr);
        super.onTestSuccess(tr);
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
        Iterable<Searchable> searchables = Iterables.filter(testContext.getResources().values(), Searchable.class);
        List<Searchable> listOfSearchables = StreamSupport.stream(searchables.spliterator(), false).collect(Collectors.toList());
        if (listOfSearchables.size() == 0) {
            return;
        }
        SearchUrl searchUrl = new KibanaSearchUrl();
        tr.getTestContext().setAttribute(tr.getName() + SEARCH_URL,
                searchUrl.getSearchUrl(listOfSearchables, new Date(tr.getStartMillis()), new Date(tr.getEndMillis())));
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
}


