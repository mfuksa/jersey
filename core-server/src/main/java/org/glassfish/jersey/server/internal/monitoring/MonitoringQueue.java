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
import java.util.Queue;

import javax.inject.Inject;

import org.glassfish.jersey.server.*;
import org.glassfish.jersey.server.internal.monitoring.event.ApplicationEvent;
import org.glassfish.jersey.server.internal.monitoring.event.ApplicationEventListener;
import org.glassfish.jersey.server.internal.monitoring.event.RequestEvent;
import org.glassfish.jersey.server.internal.monitoring.event.RequestEventListener;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

import org.glassfish.hk2.api.ServiceLocator;

import com.google.common.collect.Queues;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
public class MonitoringQueue implements ApplicationEventListener {
    @Inject
    private ServiceLocator serviceLocator;


    private static class TimeEvent {
        private final long duration;
        private final Date time;

        public TimeEvent(long duration, Date time) {
            this.duration = duration;
            this.time = time;
        }

        public long getExecutionTime() {
            return duration;
        }

        public Date getTime() {
            return time;
        }
    }

    static class RequestQueuedItem extends TimeEvent {
        private final ResourceMethodQueuedItem methodItem;

        private RequestQueuedItem(long duration, Date time, ResourceMethodQueuedItem methodItem) {
            super(duration, time);
            this.methodItem = methodItem;
        }

        ResourceMethodQueuedItem getMethodItem() {
            return methodItem;
        }
    }

    static class ResourceMethodQueuedItem extends TimeEvent {
        private final ResourceMethod.Context methodContext;

        private ResourceMethodQueuedItem(ResourceMethod.Context methodContext,
                                         long duration, Date time) {
            super(duration, time);
            this.methodContext = methodContext;
        }

        ResourceMethod.Context getMethodContext() {
            return methodContext;
        }
    }

    static class ExceptionEvent {
        private final Throwable exception;

        public ExceptionEvent(Throwable exception) {this.exception = exception;}

        public Throwable getException() {
            return exception;
        }
    }

    static class MappedExceptionEvent extends ExceptionEvent {
        public MappedExceptionEvent(Throwable exception) {
            super(exception);
        }
    }

    static class UnMappedExceptionEvent extends ExceptionEvent {
        public UnMappedExceptionEvent(Throwable exception) {
            super(exception);
        }
    }

    // TODO: M: cache ?
    static class ResponseQueuedItem {
        private final int code;

        ResponseQueuedItem(int code) {this.code = code;}

        public int getCode() {
            return code;
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
        final ApplicationEvent.Type type = event.getType();
        switch (type) {
            case INITIALIZATION_START:
                break;
            case INITIALIZATION_FINISHED:
                this.resourceConfig = event.getResourceConfig();
                final MonitoringAggregator monitoringAggregator = new MonitoringAggregator(serviceLocator, this);
                monitoringAggregator.startMonitoringWorker();

                break;
            case UNDEPLOY_START:
                break;
        }
    }

    public class ReqEventListener implements RequestEventListener {
        private long requestTimeStart;
        private long methodTimeStart;
        private ResourceMethodQueuedItem methodItem;


        @Override
        public void onEvent(RequestEvent event) {
            switch (event.getType()) {
                case START:
                    this.requestTimeStart = System.currentTimeMillis();
                    break;
                case RESOURCE_METHOD_START:
                    this.methodTimeStart = System.currentTimeMillis();
                    break;
                case RESOURCE_METHOD_FINISHED:
                    final ResourceMethod.Context methodContext = event.getUriInfo().getMatchedResourceMethodContext();
                    methodItem = new ResourceMethodQueuedItem(methodContext,
                            System.currentTimeMillis() - methodTimeStart, new Date(methodTimeStart));
                    break;
                case RESP_WRITTEN:
                    responseQueuedItems.add(new ResponseQueuedItem(event.getResponseWritten().getStatus()));
                    break;
                case FINISHED:
                    requestQueuedItems.add(new RequestQueuedItem(System.currentTimeMillis() - requestTimeStart,
                            new Date(requestTimeStart), methodItem));
            }
        }

        private void methodItem(RequestEvent event) {
            final ResourceMethod.Context methodContext = event.getUriInfo().getMatchedResourceMethodContext();
            new ResourceMethodQueuedItem(methodContext,
                    System.currentTimeMillis() - methodTimeStart, new Date(methodTimeStart));
        }
    }

    private volatile String applicationClassName;
    private final Queue<RequestQueuedItem> requestQueuedItems = Queues.newArrayBlockingQueue(50000);
    private final Queue<ResponseQueuedItem> responseQueuedItems = Queues.newArrayBlockingQueue(50000);
    private volatile ResourceConfig resourceConfig;

    public ResourceConfig getResourceConfig() {
        return resourceConfig;
    }

    public String getApplicationClassName() {
        return applicationClassName;
    }


    public Queue<RequestQueuedItem> getRequestQueuedItems() {
        return requestQueuedItems;
    }

    public Queue<ResponseQueuedItem> getResponseQueuedItems() {
        return responseQueuedItems;
    }


}
