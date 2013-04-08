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

package org.glassfish.jersey.server.internal.monitoring.jmx;

import java.lang.management.ManagementFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.glassfish.jersey.server.ExtendedResourceContext;
import org.glassfish.jersey.server.internal.monitoring.MonitoringQueue;
import org.glassfish.jersey.server.internal.monitoring.event.ApplicationEventListener;
import org.glassfish.jersey.server.internal.monitoring.statistics.MonitoringStatistics;
import org.glassfish.jersey.server.internal.monitoring.statistics.MonitoringStatisticsCallback;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
public class MBeanExposer implements MonitoringStatisticsCallback {

    private final RequestMXBeanImpl requestMXBean;
    private final ResponseMXBeanImpl responseMXBean;
    private final ResourcesMXBeanImpl resourcesMXBean;

    @Inject
    public MBeanExposer(ExtendedResourceContext resourceContext)
            throws MalformedObjectNameException, NotCompliantMBeanException,
            InstanceAlreadyExistsException, MBeanRegistrationException {
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        requestMXBean = new RequestMXBeanImpl();
        mBeanServer.registerMBean(requestMXBean, new ObjectName("org.glassfish.jersey:name=Requests"));
        responseMXBean = new ResponseMXBeanImpl();
        mBeanServer.registerMBean(responseMXBean, new ObjectName("org.glassfish.jersey:name=Responses"));

        MonitoringStatistics blankStatistics = new MonitoringStatistics.Builder(resourceContext.getResourceModel()).build();
        resourcesMXBean = new ResourcesMXBeanImpl(blankStatistics.getRootResourceStatistics(), mBeanServer);
//        mBeanServer.registerMBean(resourcesMXBean, new ObjectName("org.glassfish.jersey:type=Resources,name="));
    }


    @Override
    public void onNewStatistics(MonitoringStatistics statistics) {
        requestMXBean.setMonitoringStatistics(statistics);
        resourcesMXBean.setResourcesStatistics(statistics.getRootResourceStatistics());
        responseMXBean.setResponseCodesToCountMap(statistics.getResponseStatistics());
    }


    public static class Binder extends AbstractBinder {
        @Override
        protected void configure() {
            bind(MBeanExposer.class).to(MonitoringStatisticsCallback.class).in(Singleton.class);
            // TODO: M: move to factory
            bind(MonitoringQueue.class).to(ApplicationEventListener.class);
        }
    }
}