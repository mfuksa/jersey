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

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ExtendedUriInfo;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class RequestEvent implements Event {


    // TODO: M: volatile
    public static class Builder implements RequestEventBuilder {
        private ContainerRequest containerRequest;
        private ContainerResponse containerResponse;
        private Throwable throwable;
        private ExtendedUriInfo extendedUriInfo;
        private Iterable<ContainerResponseFilter> containerResponseFilters;
        private Iterable<ContainerRequestFilter> containerRequestFilters;
        private ExceptionMapper<?> exceptionMapper;
        private boolean success;
        private boolean responseSuccessfullyMapped;
        private ExceptionCause exceptionCause;


        public Builder() {
        }

        public Builder setExceptionMapper(ExceptionMapper<?> exceptionMapper) {
            this.exceptionMapper = exceptionMapper;
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

        public Builder setSuccess(boolean success) {
            this.success = success;
            return this;
        }

        public Builder setThrowable(Throwable throwable, ExceptionCause exceptionCause) {
            this.throwable = throwable;
            this.exceptionCause = exceptionCause;
            return this;
        }


        public Builder setExtendedUriInfo(ExtendedUriInfo extendedUriInfo) {
            this.extendedUriInfo = extendedUriInfo;
            return this;
        }

        public Builder setContainerResponseFilters(Iterable<ContainerResponseFilter> containerResponseFilters) {
            this.containerResponseFilters = containerResponseFilters;
            return this;
        }

        public Builder setContainerRequestFilters(Iterable<ContainerRequestFilter> containerRequestFilters) {
            this.containerRequestFilters = containerRequestFilters;
            return this;
        }

        public Builder setResponseSuccessfullyMapped(boolean responseSuccessfullyMapped) {
            this.responseSuccessfullyMapped = responseSuccessfullyMapped;
            return this;
        }

        @Override
        public RequestEvent build(Type type) {
            return new RequestEvent(type, containerRequest, containerResponse, throwable,
                    extendedUriInfo, containerResponseFilters, containerRequestFilters, exceptionMapper, success,
                    responseSuccessfullyMapped, exceptionCause);
        }
    }


    private RequestEvent(Type type, ContainerRequest containerRequest, ContainerResponse containerResponse,
                         Throwable throwable,
                         ExtendedUriInfo extendedUriInfo, Iterable<ContainerResponseFilter> containerResponseFilters,
                         Iterable<ContainerRequestFilter> containerRequestFilters,
                         ExceptionMapper<?> exceptionMapper,
                         boolean success,
                         boolean responseSuccessfullyMapped, ExceptionCause exceptionCause) {
        this.type = type;
        this.containerRequest = containerRequest;
        this.containerResponse = containerResponse;
        this.throwable = throwable;
        this.extendedUriInfo = extendedUriInfo;
        this.containerResponseFilters = containerResponseFilters;
        this.containerRequestFilters = containerRequestFilters;
        this.exceptionMapper = exceptionMapper;
        this.success = success;
        this.responseSuccessfullyMapped = responseSuccessfullyMapped;
        this.exceptionCause = exceptionCause;
    }

    public static enum Type {
        /**
         * This event type is used only when calling {@link ApplicationEventListener#onNewRequest(RequestEvent)}.
         */
        START,

        MATCHING_START,
        MATCHED_LOCATOR,
        MATCHED_SUB_RESOURCE,

        /**
         * similar to MATCHING_FINISHED
         */
        REQ_FILTERS_START,
        REQ_FILTERS_FINISHED,
        /**
         * Directly before execution
         */
        RESOURCE_METHOD_START,
        RESOURCE_METHOD_FINISHED,


        RESP_FILTERS_START,
        RESP_FILTERS_FINISHED,


        ON_EXCEPTION,

        /**
         * After the ExceptionMapper is successfully found and before execution of this mapper
         */
        EXCEPTION_MAPPER_FOUND,
        EXCEPTION_MAPPING_FINISHED,


        // RESPONSE_MAPPED?


        RESP_WRITTEN,

        FINISHED;
    }


    public static enum ExceptionCause {
        STANDARD_PROCESSING,
        MAPPED_RESPONSE_PROCESSING;
    }


    private static class ResourceLocator {

    }

    private final Type type;
    private final ContainerRequest containerRequest;
    private final ContainerResponse containerResponse;
    private final Throwable throwable;
    //    private final ContainerResponse mappedResponse;
//    private final ContainerResponse responseWritten;
    private final ExtendedUriInfo extendedUriInfo;
    // TODO: M: maybe List?
    private final Iterable<ContainerResponseFilter> containerResponseFilters;
    private final Iterable<ContainerRequestFilter> containerRequestFilters;
    private final ExceptionMapper<?> exceptionMapper;
    private final boolean success;
    private final boolean responseSuccessfullyMapped;
    private final ExceptionCause exceptionCause;


    public ContainerRequest getContainerRequest() {
        return containerRequest;
    }

    public ContainerResponse getContainerResponse() {
        return containerResponse;
    }


//    /***
//     * TODO: M: lot of responses: simplify
//     * @return
//     */
//    public ContainerResponse getLatestResponse() {
//        return mappedResponse != null ? mappedResponse : containerResponse;
//    }


    public Throwable getThrowable() {
        return throwable;
    }

    public Type getType() {
        return type;
    }

//    public ContainerResponse getMappedResponse() {
//        return mappedResponse;
//    }
//
//    public ContainerResponse getResponseWritten() {
//        return responseWritten;
//    }

    public ExtendedUriInfo getUriInfo() {
        return extendedUriInfo;
    }

    public ExceptionMapper<?> getExceptionMapper() {
        return exceptionMapper;
    }

    public Iterable<ContainerRequestFilter> getContainerRequestFilters() {
        return containerRequestFilters;
    }

    public Iterable<ContainerResponseFilter> getContainerResponseFilters() {
        return containerResponseFilters;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isResponseSuccessfullyMapped() {
        return responseSuccessfullyMapped;
    }

    public ExceptionCause getExceptionCause() {
        return exceptionCause;
    }
}
