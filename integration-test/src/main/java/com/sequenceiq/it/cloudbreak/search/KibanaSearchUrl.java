package com.sequenceiq.it.cloudbreak.search;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
@EnableConfigurationProperties(KibanaProps.class)
public class KibanaSearchUrl implements SearchUrl {

    private static final String KEY = "context.resourceName";

    private static KibanaProps kibanaProps;

    private List<Searchable> searchables;

    private Date testStartDate;

    private Date testStopDate;

    private void kibanaSearchUrl(List<Searchable> searchables, Date testStartDate, Date testStopDate) {
        if (searchables.size() == 0) {
            throw new IllegalArgumentException("Searchables must be non empty");
        }
        if (testStartDate == null || testStopDate == null) {
            throw new IllegalArgumentException("Dates must be filled");
        }

        this.searchables = searchables;
        this.testStartDate = DateUtils.addHours(testStartDate, -2);
        this.testStopDate = DateUtils.addHours(testStopDate, -2);
    }

    @Override
    public String getSearchUrl(List<Searchable> searchables, Date testStartDate, Date testStopDate) {
        kibanaSearchUrl(searchables, testStartDate, testStopDate);
        return String.format("%s?_g=%s&_a=%s", kibanaProps.getUrl(), getGlobalState(), getAppState());
    }

    @Inject
    @SuppressFBWarnings("ST")
    public void setKibanaProps(KibanaProps kibanaProps) {
        this.kibanaProps = kibanaProps;
    }

    private String getGlobalState() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
        String end = formatter.format(testStopDate);
        String start = formatter.format(testStartDate);
        return String.format("(filters:!(),refreshInterval:(pause:!t,value:0),time:(from:'%s',to:'%s'))", start, end);
    }

    private String getAppState() {
        return String.format("(columns:!('@message','@app'," + KEY + "),filters:!(('$state':(store:appState),"
                        + "meta:(alias:!n,disabled:!f,index:'0ec45520-98e6-11eb-85ad-c5cb99d34f92',key:" + KEY + ",negate:!f,params:!(%s),"
                        + "type:phrases,value:'%s'),query:(bool:(minimum_should_match:1,should:!(%s"
                        + "))))),index:'0ec45520-98e6-11eb-85ad-c5cb99d34f92',interval:auto,query:(language:kuery,query:''),sort:!('@timestamp',desc))",
                getResourceList(), getResourceList(), getResourceQueries());

    }

    private String getResourceList() {
        List<String> list = searchables.stream().map(Searchable::getSearchId).collect(Collectors.toList());

        return String.join(",", list);
    }

    private String getResourceQueries() {
        List<String> list = searchables.stream().map(Searchable::getSearchId).collect(Collectors.toList());
        List<String> queryList = list.stream().map(str -> String.format("(match_phrase:(" + KEY + ":%s))", str)).collect(Collectors.toList());
        return String.join(",", queryList);
    }
}

