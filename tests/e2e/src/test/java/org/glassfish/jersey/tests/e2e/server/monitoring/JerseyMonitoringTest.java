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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class JerseyMonitoringTest extends JerseyTest {
    @Override
    protected Application configure() {
        final ResourceConfig resourceConfig = new ResourceConfig(TestResource.class, MyExceptionMapper.class);
        resourceConfig.property("very-important", "yes");
        resourceConfig.property("another-property", 48);
        return resourceConfig;
    }


    public static class MyException extends RuntimeException {
        public MyException(String message) {
            super(message);
        }
    }

    public static class MyExceptionMapper implements ExceptionMapper<MyException> {

        @Override
        public Response toResponse(MyException exception) {
            return Response.ok("mapped").build();
        }
    }



    @Path("resource")
    public static class TestResource {
        @GET
        public String testGet() {
            return "get";
        }

        @POST
        public String testPost() {
            return "post";
        }

        @GET
        @Path("sub")
        public String testSubGet() {
            return "sub";
        }

        @GET
        @Path("exception")
        public String testException() {
            throw new MyException("test");
        }

        @POST
        @Path("sub2")
        @Produces("text/html")
        @Consumes("text/plain")
        public String testSu2bPost(String entity) {
            return "post";
        }
    }


    @Test
    public void test() throws Exception {
        final String path = "resource";
        do {
            assertEquals(200, target().path(path).request().get().getStatus());
            assertEquals(200, target().path(path).request().post(Entity.entity("post",
                    MediaType.TEXT_PLAIN_TYPE)).getStatus());
            assertEquals(200, target().path(path).request().post(Entity.entity("post",
                    MediaType.TEXT_PLAIN_TYPE)).getStatus());
            assertEquals(200, target().path(path).request().post(Entity.entity("post",
                    MediaType.TEXT_PLAIN_TYPE)).getStatus());
            assertEquals(200, target().path(path).request().post(Entity.entity("post",
                    MediaType.TEXT_PLAIN_TYPE)).getStatus());
            assertEquals(200, target().path(path + "/sub2").request().post(Entity.entity("post",
                    MediaType.TEXT_PLAIN_TYPE)).getStatus());
            final Response response = target().path(path + "/exception").request().get();
            assertEquals(200, response.getStatus());
            assertEquals("mapped", response.readEntity(String.class));

            assertEquals(200, target().path("resource/sub").request().get().getStatus());
            assertEquals(200, target().path("resource/sub").request().get().getStatus());
            assertEquals(404, target().path("resource/not-found-404").request().get().getStatus());

            // wait until statistics are propagated to mxbeans
            Thread.sleep(1500);
        } while (true);

//        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
//        final ObjectName name = new ObjectName("org.glassfish.jersey:name=Requests");
//        final CompositeDataSupport cds = (CompositeDataSupport) mBeanServer.getAttribute(name, "ExecutionStatisticsMxBean");
//        final Long executionCount = (Long) cds.get("executionCount");
//        Assert.assertEquals(Long.valueOf(9), executionCount);
    }
}
