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

package org.glassfish.jersey.server.internal.monitoring.event;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
public class RequestEvent implements Event {

    // TODO: M: volatile
    public static class Builder {
        private ContainerRequest containerRequest;
        private ContainerResponse containerResponse;
        private ContainerResponse mappedResponse;
        private ContainerResponse responseWritten;
        private Throwable throwable;
        private Resource parentResource;
        private Resource childResource;
        private ResourceMethod resourceMethod;

        public Builder() {
        }

        public Builder setChildResource(Resource childResource) {
            this.childResource = childResource;
            return this;
        }

        public Builder setContainerRequest(ContainerRequest containerRequest) {
            this.containerRequest = containerRequest;
            return this;
        }

        public Builder setContainerResponse(ContainerResponse containerResponse) {
            this.containerResponse = containerResponse;
            return this;
        }

        public Builder setParentResource(Resource parentResource) {
            this.parentResource = parentResource;
            return this;
        }

        public Builder setResourceMethod(ResourceMethod resourceMethod) {
            this.resourceMethod = resourceMethod;
            return this;
        }

        public Builder setThrowable(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        public void setMappedResponse(ContainerResponse mappedResponse) {
            this.mappedResponse = mappedResponse;
        }

        public void setResponseWritten(ContainerResponse responseWritten) {
            this.responseWritten = responseWritten;
        }

        public RequestEvent build(Type type) {
            return new RequestEvent(type, containerRequest, containerResponse, throwable, parentResource,
                    childResource, resourceMethod, mappedResponse, responseWritten);
        }
    }


    private RequestEvent(Type type, ContainerRequest containerRequest, ContainerResponse containerResponse,
                         Throwable throwable, Resource parentResource, Resource childResource, ResourceMethod resourceMethod,
                         ContainerResponse mappedResponse, ContainerResponse responseWritten) {
        this.type = type;
        this.containerRequest = containerRequest;
        this.containerResponse = containerResponse;
        this.throwable = throwable;
        this.parentResource = parentResource;
        this.childResource = childResource;
        this.resourceMethod = resourceMethod;
        this.mappedResponse = mappedResponse;
        this.responseWritten = responseWritten;
    }

    public static enum Type {
        START_PROCESSING,
        RESOURCE_METHOD_START,
        RESOURCE_METHOD_FINISHED,
        SUCCESS,
        FAILURE,
        EXCEPTION_MAPPER,
        RESPONSE_WRITTEN,
        FINISHED;
    }

    private final Type type;
    private final ContainerRequest containerRequest;
    private final ContainerResponse containerResponse;
    private final Throwable throwable;
    private final Resource parentResource;
    private final Resource childResource;
    private final ResourceMethod resourceMethod;
    private final ContainerResponse mappedResponse;
    private final ContainerResponse responseWritten;


    public Resource getChildResource() {
        return childResource;
    }

    public ContainerRequest getContainerRequest() {
        return containerRequest;
    }

    public ContainerResponse getContainerResponse() {
        return containerResponse;
    }

    public Resource getParentResource() {
        return parentResource;
    }

    public ResourceMethod getResourceMethod() {
        return resourceMethod;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public Type getType() {
        return type;
    }

    public ContainerResponse getMappedResponse() {
        return mappedResponse;
    }

    public ContainerResponse getResponseWritten() {
        return responseWritten;
    }
}
