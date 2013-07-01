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

import java.util.Queue;

import javax.inject.Inject;

import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.uri.UriTemplate;

import org.glassfish.hk2.api.ServiceLocator;

import com.google.common.collect.Queues;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class MonitoringEventListener implements ApplicationEventListener {
    @Inject
    private ServiceLocator serviceLocator;

    private final Queue<ApplicationEvent> applicationEvents = Queues.newArrayBlockingQueue(20);
    private final Queue<RequestStats> requestQueuedItems = Queues.newArrayBlockingQueue(50000);
    private final Queue<Integer> responseStatuses = Queues.newArrayBlockingQueue(50000);
    private final Queue<RequestEvent> exceptionMapperEvents = Queues.newArrayBlockingQueue(50000);
    private volatile long applicationStartTime;


    static class TimeStats {
        private final long duration;
        private final long startTime;

        private TimeStats(long startTime, long requestDuration) {
            this.duration = requestDuration;
            this.startTime = startTime;
        }

        long getDuration() {
            return duration;
        }

        long getStartTime() {
            return startTime;
        }
    }

    static class MethodStats extends TimeStats {
        private final ResourceMethod method;

        private MethodStats(ResourceMethod method, long startTime, long requestDuration) {
            super(startTime, requestDuration);
            this.method = method;
        }

        ResourceMethod getMethod() {
            return method;
        }
    }

    class RequestStats {
        private final TimeStats requestStats;
        private final MethodStats methodStats; // might be null if a method was not executed during a request
        private final String requestUri;

        private RequestStats(TimeStats requestStats, MethodStats methodStats, String requestUri) {
            this.requestStats = requestStats;
            this.methodStats = methodStats;
            this.requestUri = requestUri;
        }

        TimeStats getRequestStats() {
            return requestStats;
        }

        MethodStats getMethodStats() {
            return methodStats;
        }

        String getRequestUri() {
            return requestUri;
        }
    }

    @Override
    public ReqEventListener onNewRequest(RequestEvent requestEvent) {
        switch (requestEvent.getType()) {
            case START:
                return new ReqEventListener();

        }
        return null;
    }

    @Override
    public void onEvent(ApplicationEvent event) {
        final long now = System.currentTimeMillis();
        final ApplicationEvent.Type type = event.getType();
        switch (type) {
            case INITIALIZATION_START:
                break;
            case RELOAD_FINISHED:
            case INITIALIZATION_FINISHED:
                this.applicationStartTime = now;
                this.applicationEvents.add(event);
                final MonitoringStatisticsProcessor monitoringStatisticsProcessor = new MonitoringStatisticsProcessor(serviceLocator, this);
                monitoringStatisticsProcessor.startMonitoringWorker();
                break;
            case DESTROY_FINISHED:
                this.applicationEvents.add(event);
                break;

        }
    }

    public class ReqEventListener implements RequestEventListener {
        private volatile long requestTimeStart;
        private volatile long methodTimeStart;
        private volatile MethodStats methodStats;

        public ReqEventListener() {
            this.requestTimeStart = System.currentTimeMillis();
        }

        @Override
        public void onEvent(RequestEvent event) {
            final long now = System.currentTimeMillis();
            switch (event.getType()) {
                case RESOURCE_METHOD_START:
                    this.methodTimeStart = now;
                    break;
                case RESOURCE_METHOD_FINISHED:
                    final ResourceMethod method = event.getUriInfo().getMatchedResourceMethod();
                    methodStats = new MethodStats(method, methodTimeStart, now - methodTimeStart);
                    break;
                case EXCEPTION_MAPPING_FINISHED:
                    exceptionMapperEvents.add(event);
                    break;
                case FINISHED:
                    if (event.isResponseWritten()) {
                        responseStatuses.add(event.getContainerResponse().getStatus());
                    }
                    StringBuffer sb = new StringBuffer();
                    for (UriTemplate uriTemplate : event.getUriInfo().getMatchedTemplates()) {
                        sb.append(uriTemplate.getTemplate());
                        if (!uriTemplate.endsWithSlash()) {
                            sb.append("/");
                        }
                        sb.setLength(sb.length() - 1);
                    }

                    requestQueuedItems.add(new RequestStats(new TimeStats(requestTimeStart, now - requestTimeStart),
                            methodStats, sb.toString()));
            }
        }
    }


    long getApplicationStartTime() {
        return applicationStartTime;
    }

    public Queue<ApplicationEvent> getApplicationEvents() {
        return applicationEvents;
    }

    Queue<RequestEvent> getExceptionMapperEvents() {
        return exceptionMapperEvents;
    }

    Queue<RequestStats> getRequestQueuedItems() {
        return requestQueuedItems;
    }

    Queue<Integer> getResponseStatuses() {
        return responseStatuses;
    }


}
