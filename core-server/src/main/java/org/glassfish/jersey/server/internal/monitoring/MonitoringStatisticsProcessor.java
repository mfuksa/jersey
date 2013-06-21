/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.jersey.server.internal.monitoring;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;

import org.glassfish.jersey.server.ExtendedResourceContext;
import org.glassfish.jersey.server.internal.RuntimeExecutorsBinder;
import org.glassfish.jersey.server.internal.monitoring.event.RequestEvent;
import org.glassfish.jersey.server.internal.monitoring.statistics.ApplicationStatistics;
import org.glassfish.jersey.server.internal.monitoring.statistics.ExceptionMapperStatistics;
import org.glassfish.jersey.server.internal.monitoring.statistics.MonitoringStatistics;
import org.glassfish.jersey.server.internal.monitoring.statistics.MonitoringStatisticsCallback;
import org.glassfish.jersey.server.internal.monitoring.statistics.ResourceStatistics;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class MonitoringStatisticsProcessor {

    private static final Logger LOGGER = Logger.getLogger(MonitoringStatisticsProcessor.class.getName());
    private final MonitoringQueue monitoringQueue;
    private final MonitoringStatistics.Builder statisticsBuilder;
    private final List<MonitoringStatisticsCallback> statisticsCallbackList;
    private final ScheduledExecutorService scheduler;


    public MonitoringStatisticsProcessor(ServiceLocator serviceLocator, MonitoringQueue monitoringQueue) {
        this.monitoringQueue = monitoringQueue;
        final ResourceModel resourceModel = serviceLocator.getService(ExtendedResourceContext.class).getResourceModel();
        this.statisticsBuilder = new MonitoringStatistics.Builder(resourceModel);
        this.statisticsCallbackList = serviceLocator.getAllServices(MonitoringStatisticsCallback.class);
        this.scheduler = serviceLocator.getService(ScheduledExecutorService.class,
                new RuntimeExecutorsBinder.BackgroundSchedulerLiteral());
    }

    public void startMonitoringWorker() {
        final ApplicationStatistics appStatistics = new ApplicationStatistics(monitoringQueue.getResourceConfig(),
                new Date(monitoringQueue.getApplicationStartTime()));
        statisticsBuilder.setApplicationStatistics(appStatistics);
        final String appName = monitoringQueue.getResourceConfig().getApplicationName();
        statisticsBuilder.setApplicationName(appName == null ?
                String.valueOf(monitoringQueue.getResourceConfig().hashCode()) : appName);

        scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    processRequestItems();
                    processResponseCodeEvents();
                    processExceptionMapperEvents();
                } catch (Throwable t) {
                    // TODO: M: loc
                    LOGGER.log(Level.SEVERE, "Error generating MonitoringStatistics.", t);
                    // rethrowing exception stops further task execution
                    throw new ProcessingException("Error generating statistics.", t);
                }

                final Iterator<MonitoringStatisticsCallback> iterator = statisticsCallbackList.iterator();
                while (iterator.hasNext()) {
                    MonitoringStatisticsCallback monitoringStatisticsCallback = iterator.next();
                    try {
                        monitoringStatisticsCallback.onNewStatistics(statisticsBuilder.build());
                    } catch (Throwable t) {
                        // TODO: M: loc
                        LOGGER.log(Level.SEVERE, "Exception thrown when provider "
                                + monitoringStatisticsCallback + " was processing MonitoringStatistics. " +
                                "Removing provider from further processing.", t);
                        iterator.remove();
                    }
                }

            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    private void processExceptionMapperEvents() {
        final Queue<RequestEvent> eventQueue = monitoringQueue.getExceptionMapperEvents();
        while (!eventQueue.isEmpty()) {
            final RequestEvent event = eventQueue.remove();
            final ExceptionMapperStatistics.Builder mapperStats = statisticsBuilder.getExceptionMapperStatisticsBuilder();

            if (event.getExceptionMapper() != null) {
                mapperStats.addExceptionMapperExecution(event.getExceptionMapper().getClass(), 1);
            }

            mapperStats.addMapping(event.isResponseSuccessfullyMapped(), 1);
        }

    }

    private void processRequestItems() {
        final Queue<MonitoringQueue.RequestQueuedItem> requestQueuedItems = monitoringQueue.getRequestQueuedItems();

        while (!requestQueuedItems.isEmpty()) {
            MonitoringQueue.RequestQueuedItem event = requestQueuedItems.remove();
            statisticsBuilder.getRequestStatisticsBuilder().addExecution(event.getExecutionTime(), event.getTime().getTime());
            final MonitoringQueue.ResourceMethodQueuedItem methodItem = event.getMethodItem();


            if (methodItem != null) {
                final ResourceMethod.Context methodContext = methodItem.getMethodContext();
                final Resource resource = (methodContext.getParentResource() == null) ? methodContext.getResource()
                        : methodContext.getParentResource();
                final Resource childResource = (methodContext.getParentResource() == null) ? null
                        : methodContext.getResource();

                final ResourceStatistics.Builder resourceBuilder = statisticsBuilder.getRootResourceStatistics()
                        .get(resource);
                final ResourceMethod method = methodContext.getResourceMethod();
                if (childResource == null) {
                    resourceBuilder.addResourceExecution(method, methodItem.getExecutionTime(),
                            methodItem.getTime().getTime(), event.getExecutionTime(), event.getTime().getTime());
                } else {
                    resourceBuilder.addResourceExecution(childResource, method,
                            methodItem.getExecutionTime(), methodItem.getTime().getTime(), event.getExecutionTime(),
                            event.getTime().getTime());
                }
            }
        }
    }


    private void processResponseCodeEvents() {
        final Queue<MonitoringQueue.ResponseQueuedItem> responseEvents = monitoringQueue.getResponseQueuedItems();
        MonitoringQueue.ResponseQueuedItem event;
        while (!responseEvents.isEmpty()) {
            event = responseEvents.remove();
            statisticsBuilder.addResponseCode(event.getCode());
        }

    }


}
