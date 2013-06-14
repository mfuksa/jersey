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
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.glassfish.jersey.server.*;
import org.glassfish.jersey.server.internal.monitoring.statistics.MonitoringStatistics;
import org.glassfish.jersey.server.internal.monitoring.statistics.MonitoringStatisticsCallback;
import org.glassfish.jersey.server.internal.monitoring.statistics.ResourceStatistics;
import org.glassfish.jersey.server.model.ResourceModel;

import org.glassfish.hk2.api.ServiceLocator;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
public class MonitoringAggregator {
    private final MonitoringQueue monitoringQueue;
    private final MonitoringStatistics.Builder statisticsBuilder;
    private final List<MonitoringStatisticsCallback> statisticsCallbackList;
    private final Queue<MonitoringQueue.RequestQueuedItem> recentRequestQueue;


    public MonitoringAggregator(ServiceLocator serviceLocator, MonitoringQueue monitoringQueue) {
        this.monitoringQueue = monitoringQueue;
        final ResourceModel resourceModel = serviceLocator.getService(ExtendedResourceContext.class).getResourceModel();
        this.statisticsBuilder = new MonitoringStatistics.Builder(resourceModel);
        this.statisticsCallbackList = serviceLocator.getAllServices(MonitoringStatisticsCallback.class);
        this.recentRequestQueue = Lists.newLinkedList();
    }

    public void startMonitoringWorker() {
        final String appName = monitoringQueue.getResourceConfig().getApplicationName();
        statisticsBuilder.setApplicationName(appName == null ?
                String.valueOf(monitoringQueue.getResourceConfig().hashCode()) : appName);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    processRequestEvents();
                    processResponseCodeEvents();
                    for (MonitoringStatisticsCallback monitoringStatisticsCallback : statisticsCallbackList) {
                        monitoringStatisticsCallback.onNewStatistics(statisticsBuilder.build());
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                        return;
                    }

                }
            }
        });
    }

    private void processRequestEvents() {
        final Queue<MonitoringQueue.RequestQueuedItem> requestQueuedItems = monitoringQueue.getRequestQueuedItems();
        MonitoringQueue.RequestQueuedItem event;
        long now = System.currentTimeMillis();
        long lastSecondTreshold = now - 1000;
        while (!recentRequestQueue.isEmpty()) {
            final MonitoringQueue.RequestQueuedItem recentRequest = recentRequestQueue.remove();
            if (recentRequest.getTime().getTime() > lastSecondTreshold) {
                break;
            }
        }
        int requestsPerSecond = recentRequestQueue.size();

        while (!requestQueuedItems.isEmpty()) {
            event = requestQueuedItems.remove();
            if (event.getTime().getTime() > lastSecondTreshold) {
                if (event.getTime().getTime() < now) {
                    requestsPerSecond++;
                }
                recentRequestQueue.add(event);
            }
            statisticsBuilder.getRequestStatisticsBuilder().addExecution(event.getExecutionTime(), event.getTime());
            final MonitoringQueue.ResourceMethodQueuedItem methodItem = event.getMethodItem();
            if (methodItem != null) {
                final ResourceStatistics.Builder resourceBuilder = statisticsBuilder.getRootResourceStatistics()
                        .get(methodItem.getParentResource());
                if (methodItem.getChildResource() == null) {
                    resourceBuilder.addResourceExecution(methodItem.getResourceMethod(), methodItem.getExecutionTime(),
                            methodItem.getTime(), event.getExecutionTime(), event.getTime());
                } else {
                    resourceBuilder.addResourceExecution(methodItem.getChildResource(), methodItem.getResourceMethod(),
                            methodItem.getExecutionTime(), methodItem.getTime(), event.getExecutionTime(), event.getTime());
                }
            }
        }
        statisticsBuilder.setRequestsPerSecond(requestsPerSecond);

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
