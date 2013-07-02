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

package org.glassfish.jersey.server.monitoring;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.model.Resource;

/**
 * An event informing about details of a request processing. The event is created by a Jersey runtime and
 * handled by {@link RequestEventListener} (javadoc of listener describes how to register the listener for
 * particular request).
 * <p/>
 * The event contains the {@link Type} which distinguishes between types of event. There are various
 * properties in the event (accessible by getters) and some of them might be relevant only to specific event types.
 * <p/>
 * Note that internal state of the event must be modified. Even the event is immutable it exposes objects
 * which might be mutable and the code of event listener must not change state of these objects.

 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public interface RequestEvent {

    public static enum Type {
        /**
         * The request processing has started. This event type is handled only by
         * {@link org.glassfish.jersey.server.monitoring.ApplicationEventListener#onNewRequest(RequestEvent)} and will
         * never be called for {@link RequestEventListener#onEvent(RequestEvent)}.
         */
        START,

        /**
         * The matching of the resource and resource method has started.
         */
        MATCHING_START,
        /**
         * The sub resource locator method is found and it will be called.
         * The locator method can be retrieved from {@link #getUriInfo()} by method
         * {@link org.glassfish.jersey.server.ExtendedUriInfo#getMatchedResourceLocators()}.
         */
        MATCHED_LOCATOR,

        /**
         * The sub resource has been returned from sub resource locator, model was constructed, enhanced by
         * {@link org.glassfish.jersey.server.model.ModelProcessor model processor}, validated and the matching
         * is going to be performed on the sub {@link Resource resource}.
         * The sub resource can be retrieved from {@link #getUriInfo()} by method
         * {@link org.glassfish.jersey.server.ExtendedUriInfo#getLocatorSubResources()}.
         *
         */
        MATCHED_SUB_RESOURCE,

        /**
         * The matching has been finished and {@link ContainerRequestFilter container request filters}
         * are going to be executed. The request filters can be retrieved from event by
         * {@link #getContainerRequestFilters()} method. This method also determines end of the matching
         * process and therefore the matching results can be retrieved using {@link #getUriInfo()}.
         */
        REQ_FILTERS_START,

        /**
         * Execution of {@link ContainerRequestFilter container request filters} has been finished.
         */
        REQ_FILTERS_FINISHED,

        /**
         * Resource method is going to be executed. The resource method can be extracted from {@link ExtendedUriInfo}
         * returned by {@link #getUriInfo()}.
         */
        RESOURCE_METHOD_START,

        /**
         * Resource method execution has finished. The response is not available yet.
         */
        RESOURCE_METHOD_FINISHED,


        /**
         * {@link ContainerResponseFilter Container response filters} are going to be executed. In this point
         * the response is already available and can be retrieved by {@link #getContainerResponse()}. The
         * response filters can be retrieved by {@link #getContainerResponseFilters()}.
         * <p/>
         * This phase is executed in the regular response processing but might also been executed for
         * processing on response mapped from exceptions by {@link ExceptionMapper exception mappers}.
         * In this case the {@link #ON_EXCEPTION} event type precedes this event.
         */
        RESP_FILTERS_START,

        /**
         * Execution of {@link ContainerResponseFilter Container response filters} has finished.
         * <p/>
         * This phase is executed in the regular response processing but might also been executed for
         * processing on response mapped from exceptions by {@link ExceptionMapper exception mappers}.
         * In this case the {@link #ON_EXCEPTION} event type precedes this event.
         */
        RESP_FILTERS_FINISHED,


        /**
         * Exception has been thrown during the request/response processing. This situation can
         * occur in almost all phases of request processing and therefore there is no fixed order of
         * events in which this event type can be triggered.
         * <p/>
         * The origin of exception can be retrieved
         * by {@link #getExceptionCause()}. This event type can be received even two types in the case
         * when first exception is thrown during the standard request processing and the second one
         * is thrown during the processing of the response mapped from the exception.
         * <p/>
         * The exception thrown can be retrieved by {@link #getThrowable()}.
         */
        ON_EXCEPTION,

        /**
         * An {@link ExceptionMapper} is successfully found and it is going to be executed. The
         * {@code ExceptionMapper} can be retrieved by {@link #getExceptionMapper()}.
         */
        EXCEPTION_MAPPER_FOUND,

        /**
         * Exception mapping is finished. The result of exception mapping can be checked by
         * {@link #isResponseSuccessfullyMapped()} which returns true when the exception mapping
         * was successful. In this case the new response is available in the {@link #getContainerResponse()}.
         */
        EXCEPTION_MAPPING_FINISHED,


        FINISHED;
    }


    public static enum ExceptionCause {
        STANDARD_PROCESSING,
        MAPPED_RESPONSE_PROCESSING;
    }

    public ContainerRequest getContainerRequest();

    public ContainerResponse getContainerResponse();

    public Throwable getThrowable();

    public Type getType();

    public ExtendedUriInfo getUriInfo();

    public ExceptionMapper<?> getExceptionMapper();

    public Iterable<ContainerRequestFilter> getContainerRequestFilters();

    public Iterable<ContainerResponseFilter> getContainerResponseFilters();

    public boolean isSuccess();

    public boolean isResponseSuccessfullyMapped();

    public ExceptionCause getExceptionCause();

    public boolean isResponseWritten();


}
