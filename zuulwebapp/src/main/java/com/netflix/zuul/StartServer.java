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
package com.netflix.zuul;

import com.google.common.base.Throwables;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.servo.util.ThreadCpuStats;
import com.netflix.zuul.constants.ZuulConstants;
import com.netflix.zuul.context.NFRequestContext;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.groovy.GroovyCompiler;
import com.netflix.zuul.groovy.GroovyFileFilter;
import com.netflix.zuul.monitoring.CounterFactory;
import com.netflix.zuul.monitoring.TracerFactory;
import com.netflix.zuul.plugins.Counter;
import com.netflix.zuul.plugins.MetricPoller;
import com.netflix.zuul.plugins.ServoMonitor;
import com.netflix.zuul.plugins.Tracer;
import com.netflix.zuul.scriptManager.ZuulFilterDAO;
import com.netflix.zuul.scriptManager.ZuulFilterDAOInMemory;
import com.netflix.zuul.scriptManager.ZuulFilterPoller;
import com.netflix.zuul.stats.monitoring.MonitorRegistry;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import static com.netflix.zuul.constants.ZuulConstants.ZUUL_CASSANDRA_ENABLED;
import static com.netflix.zuul.constants.ZuulConstants.ZUUL_FILTER_CUSTOM_PATH;
import static com.netflix.zuul.constants.ZuulConstants.ZUUL_FILTER_POST_PATH;
import static com.netflix.zuul.constants.ZuulConstants.ZUUL_FILTER_PRE_PATH;
import static com.netflix.zuul.constants.ZuulConstants.ZUUL_FILTER_ROUTING_PATH;

/**
 * @author Mikey Cohen
 * Date: 10/18/11
 * Time: 11:14 AM
 */
public class StartServer implements ServletContextListener {

    public StartServer() {

        System.setProperty(DynamicPropertyFactory.ENABLE_JMX, "true");
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        try {
            FilterFileManager.shutdown();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }


    protected void initialize() throws Exception {
        String applicationID = ConfigurationManager.getConfigInstance().getString(ZuulConstants.DEPLOYMENT_APPLICATION_ID);
        if (StringUtils.isEmpty(applicationID)) {
            ConfigurationManager.getConfigInstance().setProperty(ZuulConstants.DEPLOYMENT_APPLICATION_ID, "zuul");
        }
        ConfigurationManager.loadCascadedPropertiesFromResources("zuul");
        initPlugins();
        initZuul();
        initPoller();
    }

    private void initPlugins() {
        MonitorRegistry.getInstance().setPublisher(new ServoMonitor());

        MetricPoller.startPoller();


        TracerFactory.initialize(new Tracer());

        CounterFactory.initialize(new Counter());

        final ThreadCpuStats stats = ThreadCpuStats.getInstance();
        stats.start();
    }

    void initZuul() throws Exception, IllegalAccessException, InstantiationException {

        RequestContext.setContextClass(NFRequestContext.class);

        CounterFactory.initialize(new Counter());
        TracerFactory.initialize(new Tracer());

        final AbstractConfiguration config = ConfigurationManager.getConfigInstance();
        final String preFiltersPath = config.getString(ZUUL_FILTER_PRE_PATH);
        final String postFiltersPath = config.getString(ZUUL_FILTER_POST_PATH);
        final String routingFiltersPath = config.getString(ZUUL_FILTER_ROUTING_PATH);;
        final String customPath = config.getString(ZUUL_FILTER_CUSTOM_PATH);

        FilterLoader.getInstance().setCompiler(new GroovyCompiler());
        FilterFileManager.setFilenameFilter(new GroovyFileFilter());
        if (customPath == null) {
            FilterFileManager.init(5, preFiltersPath, postFiltersPath, routingFiltersPath);
        } else {
            FilterFileManager.init(5, preFiltersPath, postFiltersPath, routingFiltersPath, customPath);
        }
    }

    void initPoller() throws Exception {
        ZuulFilterDAO dao = new ZuulFilterDAOInMemory();
        ZuulFilterPoller.start(dao);
    }
}
