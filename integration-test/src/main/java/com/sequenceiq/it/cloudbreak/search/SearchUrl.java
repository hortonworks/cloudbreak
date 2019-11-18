package com.sequenceiq.it.cloudbreak.search;

import java.util.Date;
import java.util.List;

public interface SearchUrl {
    String getSearchUrl(List<Searchable> searchables, Date testStartDate, Date testStopDate);
}

