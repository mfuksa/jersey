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

import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.glassfish.jersey.server.internal.monitoring.statistics.MonitoringStatistics;
import org.glassfish.jersey.server.internal.monitoring.statistics.MonitoringStatisticsCallback;
import org.glassfish.jersey.server.internal.monitoring.statistics.ResourceStatistics;
import org.glassfish.jersey.server.model.Resource;

import com.google.common.collect.Maps;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
public class ResourcesMBeanGroup implements Registrable {
    private final Map<Resource, ResourceMxBeanImpl> resourceMBeans = Maps.newHashMap();
    private final Map<String, ResourceMxBeanImpl> exposedResourceMBeans = Maps.newHashMap();

    public ResourcesMBeanGroup(Map<Resource, ResourceStatistics> resourceStatistics)
            throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException,
            MBeanRegistrationException {
        for (Map.Entry<Resource, ResourceStatistics> entry : resourceStatistics.entrySet()) {
            final Resource resource = entry.getKey();
            final String path = resource.getPath();
            final ResourceMxBeanImpl mxBean = new ResourceMxBeanImpl(entry.getValue(), path);
            resourceMBeans.put(resource, mxBean);
            exposedResourceMBeans.put(path, mxBean);
        }
    }


    public void setResourcesStatistics(Map<Resource, ResourceStatistics> resourceStatistics) {
        for (Map.Entry<Resource, ResourceStatistics> entry : resourceStatistics.entrySet()) {
            resourceMBeans.get(entry.getKey()).setResourceStatistics(entry.getValue());
        }
    }

    @Override
    public void register(MBeanExposer mBeanExposer, String parentName) {
        for (Map.Entry<String, ResourceMxBeanImpl> entry : exposedResourceMBeans.entrySet()) {
            entry.getValue().register(mBeanExposer, parentName + "type=Resources" );
        }

    }
}