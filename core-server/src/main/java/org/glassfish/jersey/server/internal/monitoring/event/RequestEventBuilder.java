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
public interface RequestEventBuilder {

    public RequestEventBuilder setExceptionMapper(ExceptionMapper<?> exceptionMapper);

    public RequestEventBuilder setContainerRequest(ContainerRequest containerRequest);

    public RequestEventBuilder setContainerResponse(ContainerResponse containerResponse);

    public RequestEventBuilder setSuccess(boolean success);

    public RequestEventBuilder setResponseWritten(boolean responseWritten);

    public RequestEventBuilder setThrowable(Throwable throwable, RequestEvent.ExceptionCause exceptionCause);


    public RequestEventBuilder setExtendedUriInfo(ExtendedUriInfo extendedUriInfo);

    public RequestEventBuilder setContainerResponseFilters(Iterable<ContainerResponseFilter> containerResponseFilters);

    public RequestEventBuilder setContainerRequestFilters(Iterable<ContainerRequestFilter> containerRequestFilters);


    public RequestEventBuilder setResponseSuccessfullyMapped(boolean responseSuccessfullyMapped);


    public RequestEvent build(RequestEvent.Type eventType);
}
