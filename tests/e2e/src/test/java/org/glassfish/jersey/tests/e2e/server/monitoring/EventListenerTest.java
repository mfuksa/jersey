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

package org.glassfish.jersey.tests.e2e.server.monitoring;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.glassfish.jersey.server.*;
import org.glassfish.jersey.server.internal.monitoring.event.*;
import org.glassfish.jersey.test.*;

import org.junit.*;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class EventListenerTest extends JerseyTest {

    private static AppEventListener applicationEventListener;

    @Override
    protected Application configure() {
        applicationEventListener = new AppEventListener();
        final ResourceConfig resourceConfig = new ResourceConfig(MyResource.class);
        resourceConfig.register(applicationEventListener);
        return resourceConfig;
    }

    public static class AppEventListener implements ApplicationEventListener {
        private ApplicationEvent appEventStart;
        private RequestEvent newRequestEvent;

        @Override
        public void onEvent(ApplicationEvent event) {
            switch (event.getType()) {
                case INITIALIZED: this.appEventStart = event;
                    break;
            }
        }

        @Override
        public RequestEventListener onNewRequest(RequestEvent newRequestEvent) {
            this.newRequestEvent = newRequestEvent;
            return null;
        }
    }

    public static class ReqEventListener implements RequestEventListener {
        RequestEvent methodStart;

        @Override
        public void onEvent(RequestEvent event) {
            switch (event.getType()) {
                case RESOURCE_METHOD_START:
                    this.methodStart = event;
                    break;
            }
        }
    }

    @Path("resource")
    public static class MyResource {

        @GET
        public String get() {
            return "get";
        }
    }


    @Test
    public void test() {
        Assert.assertNotNull(applicationEventListener.appEventStart);
        Assert.assertNull(applicationEventListener.newRequestEvent);
        final Response response = target().path("resource").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertNotNull(applicationEventListener.newRequestEvent);

    }

    @Test
    public void test2() {
        final Response response = target().path("resource").request().get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertNotNull(applicationEventListener.appEventStart);
    }



}