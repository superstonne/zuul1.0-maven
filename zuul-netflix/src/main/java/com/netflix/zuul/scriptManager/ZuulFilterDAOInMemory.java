/*
 * Copyright 2013 Netflix, Inc.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */
package com.netflix.zuul.scriptManager;

import com.netflix.zuul.event.ZuulEvent;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @Author:  Nick
 * @Date: 2019/10/16
 */
@ThreadSafe
public class ZuulFilterDAOInMemory extends Observable implements ZuulFilterDAO {
    private static final Logger logger = LoggerFactory.getLogger(ZuulFilterDAOInMemory.class);

    private static List<FilterInfo> filterList = new ArrayList<>();

    @Override
    public List<String> getAllFilterIDs() {
        return filterList.stream().map(item -> item.getFilterID()).collect(Collectors.toList());
    }

    @Override
    public List<FilterInfo> getZuulFiltersForFilterId(String filter_id) {
        List<FilterInfo> result = new ArrayList<>();
        for (FilterInfo filterInfo : filterList) {
            if (filterInfo.getFilterID().equals(filter_id)) {
                result.add(filterInfo);
            }
        }
        return result;
    }

    @Override
    public FilterInfo getFilterInfo(String filter_id, int revision) {
        for (FilterInfo filterInfo : filterList) {
            if (filterInfo.getFilterID().equals(filter_id) && filterInfo.getRevision() == revision) {
                return filterInfo;
            }
        }
        return null;
    }

    @Override
    public FilterInfo getFilterInfoForFilter(String filter_id, int revision) {
        for (FilterInfo filterInfo : filterList) {
            if (filterInfo.getFilterID().equals(filter_id) && filterInfo.getRevision() == revision) {
                return filterInfo;
            }
        }
        return null;
    }

    @Override
    public FilterInfo getLatestFilterInfoForFilter(String filter_id) {
        return filterList.get(filterList.size() - 1);
    }

    @Override
    public FilterInfo getActiveFilterInfoForFilter(String filter_id) {
        for (FilterInfo filterInfo : filterList) {
            if (filterInfo.getFilterID().equals(filter_id) && filterInfo.isActive()) {
                return filterInfo;
            }
        }
        return null;
    }

    @Override
    public List<FilterInfo> getAllCanaryFilters() {
        return filterList.stream().filter(item -> item.isCanary()).collect(Collectors.toList());
    }

    @Override
    public List<FilterInfo> getAllActiveFilters() {
        return filterList.stream().filter(item -> item.isActive()).collect(Collectors.toList());
    }

    @Override
    public FilterInfo setCanaryFilter(String filter_id, int revision) {
        for (FilterInfo filterInfo : filterList) {
            if (filterInfo.getFilterID().equals(filter_id) && filterInfo.getRevision() == revision) {
                filterInfo.setCanary();
                return filterInfo;
            }
        }
        return null;
    }

    @Override
    public FilterInfo setFilterActive(String filter_id, int revision) throws Exception {
        for (FilterInfo filterInfo : filterList) {
            if (filterInfo.getFilterID().equals(filter_id) && filterInfo.getRevision() == revision) {
                filterInfo.setActive();

                setChanged();
                notifyObservers(new ZuulEvent("ZUUL_SCRIPT_CHANGE", "ACTIVATED NEW ZUUL FILTER id = " + filter_id + " revision = " + revision));

                return filterInfo;
            }
        }
        return null;
    }

    @Override
    public FilterInfo deActivateFilter(String filter_id, int revision) throws Exception {
        for (FilterInfo filterInfo : filterList) {
            if (filterInfo.getFilterID().equals(filter_id) && filterInfo.getRevision() == revision) {
                filterInfo.deActive();
                setChanged();
                notifyObservers(new ZuulEvent("ZUUL_SCRIPT_CHANGE", "DEACTIVATED ZUUL FILTER id = " + filter_id + " revision = " + revision));
                return filterInfo;
            }
        }
        return null;
    }

    @Override
    public FilterInfo addFilter(String filtercode, String filter_type, String filter_name, String disableFilterPropertyName, String filter_order) {
        FilterInfo filterInfo = new FilterInfo(String.valueOf(System.currentTimeMillis()), filtercode, filter_type, filter_name, disableFilterPropertyName, filter_order, "Zuul");
        filterList.add(filterInfo);
        return filterInfo;
    }

    @Override
    public String getFilterIdsRaw(String index) {
        return null;
    }

    @Override
    public List<String> getFilterIdsIndex(String index) {
        return null;
    }
}
