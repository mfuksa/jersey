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

import java.lang.management.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import javax.inject.*;
import javax.management.*;
import javax.ws.rs.*;

import org.glassfish.jersey.server.*;
import org.glassfish.jersey.server.internal.monitoring.statistics.*;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class MBeanExposer implements MonitoringStatisticsListener {

    private final ExecutionStatisticsDynamicBean requestMBean;
    private final ResponseMXBeanImpl responseMXBean;
    private final ResourcesMBeanGroup resourcesGroup;
    private volatile ApplicationMXBeanImpl applicationMXBean;
    private final AtomicBoolean exposed = new AtomicBoolean(false);
    private volatile String domain;
    private final ExceptionMapperMXBeanImpl exceptionMapperMXBean;

    private static final Logger LOGGER = Logger.getLogger(MBeanExposer.class.getName());

    @Inject
    public MBeanExposer(ExtendedResourceContext resourceContext)
            throws MalformedObjectNameException, NotCompliantMBeanException,
            InstanceAlreadyExistsException, MBeanRegistrationException {

        MonitoringStatisticsImpl blankStatistics = new MonitoringStatisticsImpl.Builder(resourceContext.getResourceModel()).build();
        resourcesGroup = new ResourcesMBeanGroup(blankStatistics.getRootResourceStatistics());
        responseMXBean = new ResponseMXBeanImpl();
        requestMBean = new ExecutionStatisticsDynamicBean(blankStatistics.getRequestStatistics(), "GlobalRequestStatistics");
        exceptionMapperMXBean = new ExceptionMapperMXBeanImpl();
    }

    void registerMBean(Object mbean, String namePostfix) {
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            final ObjectName objectName = new ObjectName(domain + ":" + namePostfix);
            if (mBeanServer.isRegistered(objectName)) {
                // TODO: M: loc
                LOGGER.log(Level.SEVERE, "Monitoring Mbeans for Jersey application " + objectName.getCanonicalName() +
                        " are already registered. Unregistering the current mbean and registering a new one instead.");
                mBeanServer.unregisterMBean(objectName);
            }

            mBeanServer.registerMBean(mbean, objectName);
        } catch (JMException e) {

            throw new ProcessingException("Error when registering Jersey monitoring MXBeans.", e);
        }
    }

    @Override
    public void onStatistics(MonitoringStatisticsImpl statistics) {
        if (exposed.compareAndSet(false, true)) {
            String appName = statistics.getApplicationStatistics().getResourceConfig().getApplicationName();
            if (appName == null) {
                appName = "App_" + Integer.toHexString(statistics.getApplicationStatistics().getResourceConfig().hashCode());
            }
            domain = "org.glassfish.jersey." + appName;
            registerMBean(requestMBean,  "type=Requests");
            registerMBean(responseMXBean, "type=Responses");
            resourcesGroup.register(this, "");
            applicationMXBean = new ApplicationMXBeanImpl(statistics.getApplicationStatistics());
            applicationMXBean.register(this, "");
            exceptionMapperMXBean.register(this, "");
        }

        requestMBean.setExecutionStatisticsImpl(statistics.getRequestStatistics());
        resourcesGroup.setResourcesStatistics(statistics.getRootResourceStatistics());
        responseMXBean.setResponseCodesToCountMap(statistics.getResponseStatistics());
        exceptionMapperMXBean.setNewStatistics(statistics.getExceptionMapperStatistics());
    }


}