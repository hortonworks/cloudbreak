package com.sequenceiq.it.cloudbreak.search;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
@EnableConfigurationProperties(KibanaProps.class)
public class KibanaSearchUrl implements SearchUrl {

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
        this.testStartDate = testStartDate;
        this.testStopDate = testStopDate;
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
        return String.format("(refreshInterval:(display:Off,pause:!f,value:0),time:(from:'%s',mode:absolute,to:'%s'))", start, end);
    }

    private String getAppState() {
        return String.format("(columns:!('@message','@app',cloudbreak_name),filters:!(%s,('$state':(store:appState),"
                        + "meta:(alias:!n,disabled:!f,index:'logstash-*',key:cloudbreak_name,negate:!f,params:!(%s),"
                        + "type:phrases,value:'%s'),query:(bool:(minimum_should_match:1,should:!(%s"
                        + "))))),index:'logstash-*',interval:auto,query:(match_all:()),sort:!('@timestamp',desc))",
                getEnvironmentQuery(), getResourceList(), getResourceList(), getResourceQueries());

    }

    private String getResourceList() {
        List<String> list = searchables.stream().map(Searchable::getSearchId).collect(Collectors.toList());

        return String.join(",", list);
    }

    private String getEnvironmentQuery() {
        String envQuery = "";
        String env = kibanaProps.getApp();

        if (env == null || !"".equals(env)) {
            envQuery = String.format("('$state':(store:appState),meta:"
                    + "(alias:!n,disabled:!f,index:'logstash-*',key:'@app',negate:!f,type:phrase,value:%s),"
                    + "query:(match:('@app':(query:%s,type:phrase))))", env, env);
        }

        return envQuery;
    }

    private String getResourceQueries() {
        List<String> list = searchables.stream().map(Searchable::getSearchId).collect(Collectors.toList());
        List<String> queryList = list.stream().map(str -> String.format("(match_phrase:(cloudbreak_name:%s))", str)).collect(Collectors.toList());
        return String.join(",", queryList);
    }
}

