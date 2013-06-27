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
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.ws.rs.ProcessingException;

import org.glassfish.jersey.server.monitoring.MonitoringStatistics;
import org.glassfish.jersey.server.monitoring.MonitoringStatisticsListener;
import org.glassfish.jersey.server.monitoring.ResourceStatistics;

import com.google.common.collect.Maps;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class MBeanExposer implements MonitoringStatisticsListener {

    // MBeans
    private volatile ExecutionStatisticsDynamicBean requestMBean;
    private volatile ResponseMXBeanImpl responseMXBean;
    private volatile ResourcesMBeanGroup uriStatsGroup;
    private volatile ResourcesMBeanGroup resourceClassStatsGroup;
    private volatile ApplicationMXBeanImpl applicationMXBean;
    private volatile ExceptionMapperMXBeanImpl exceptionMapperMXBean;

    private final AtomicBoolean exposed = new AtomicBoolean(false);
    private volatile String domain;

    private static final Logger LOGGER = Logger.getLogger(MBeanExposer.class.getName());


    private Map<String, ResourceStatistics> transformToStringKeys(Map<Class<?>, ResourceStatistics> stats) {
        Map<String, ResourceStatistics> newMap = Maps.newHashMap();
        for (Map.Entry<Class<?>, ResourceStatistics> entry : stats.entrySet()) {
            newMap.put(entry.getKey().getName(), entry.getValue());
        }
        return newMap;
    }

    void registerMBean(Object mbean, String namePostfix) {
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        final String name = domain + ":" + namePostfix;
        try {
            final ObjectName objectName = new ObjectName(name);
            if (mBeanServer.isRegistered(objectName)) {
                // TODO: M: loc
                LOGGER.log(Level.SEVERE, "Monitoring Mbeans for Jersey application " + objectName.getCanonicalName() +
                        " are already registered. Unregistering the current mbean and registering a new one instead.");
                mBeanServer.unregisterMBean(objectName);
            }

            mBeanServer.registerMBean(mbean, objectName);
        } catch (JMException e) {

            throw new ProcessingException(
                    "Error when registering Jersey monitoring MXBeans. MxBean with name '" + name + "' failed.", e);
        }
    }

    @Override
    public void onStatistics(MonitoringStatistics statistics) {
        if (exposed.compareAndSet(false, true)) {
            String appName = statistics.getApplicationStatistics().getResourceConfig().getApplicationName();
            if (appName == null) {
                appName = "App_" + Integer.toHexString(statistics.getApplicationStatistics().getResourceConfig().hashCode());
            }
            domain = "org.glassfish.jersey." + appName;

            uriStatsGroup = new ResourcesMBeanGroup(statistics.getUriStatistics(), true, this, "type=Uris");
            Map<String, ResourceStatistics> newMap = transformToStringKeys(statistics.getResourceClassStatistics());

            resourceClassStatsGroup = new ResourcesMBeanGroup(newMap, false, this, "type=ResourceClasses");

            responseMXBean = new ResponseMXBeanImpl();
            // TODO: M: move register to Respbean
            registerMBean(responseMXBean, "type=Responses");

            requestMBean = new ExecutionStatisticsDynamicBean(statistics.getRequestExecutionStatistics(),
                    this, "type=Requests", "GlobalRequestStatistics");

            exceptionMapperMXBean = new ExceptionMapperMXBeanImpl(statistics.getExceptionMapperStatistics(), this);

            applicationMXBean = new ApplicationMXBeanImpl(statistics.getApplicationStatistics(), this);

        }

        requestMBean.updateExecutionStatistics(statistics.getRequestExecutionStatistics());
        uriStatsGroup.updateResourcesStatistics(statistics.getUriStatistics());
        responseMXBean.updateResponseStatistics(statistics.getResponseStatistics());
        exceptionMapperMXBean.updateExceptionMapperStatistics(statistics.getExceptionMapperStatistics());
        this.resourceClassStatsGroup.updateResourcesStatistics(transformToStringKeys(statistics.getResourceClassStatistics()));

    }


}