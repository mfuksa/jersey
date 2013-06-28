package org.glassfish.jersey.server.internal.monitoring;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ExtendedUriInfo;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class EmptyRequestEventBuilder implements RequestEventBuilder {
    public static EmptyRequestEventBuilder EMPTY_EVENT_BUILDER = new EmptyRequestEventBuilder();

    @Override
    public RequestEventBuilder setExceptionMapper(ExceptionMapper<?> exceptionMapper) {
        return this;
    }

    @Override
    public RequestEventBuilder setContainerRequest(ContainerRequest containerRequest) {
        return this;
    }

    @Override
    public RequestEventBuilder setContainerResponse(ContainerResponse containerResponse) {
        return this;
    }

    @Override
    public RequestEventBuilder setSuccess(boolean success) {
        return this;
    }

    @Override
    public RequestEventBuilder setResponseWritten(boolean responseWritten) {
        return this;
    }

    @Override
    public RequestEventBuilder setThrowable(Throwable throwable, RequestEventImpl.ExceptionCause exceptionCause) {
        return this;
    }

    @Override
    public RequestEventBuilder setExtendedUriInfo(ExtendedUriInfo extendedUriInfo) {
        return this;
    }

    @Override
    public RequestEventBuilder setContainerResponseFilters(Iterable<ContainerResponseFilter> containerResponseFilters) {
        return this;
    }

    @Override
    public RequestEventBuilder setContainerRequestFilters(Iterable<ContainerRequestFilter> containerRequestFilters) {
        return this;
    }

    @Override
    public RequestEventBuilder setResponseSuccessfullyMapped(boolean responseSuccessfullyMapped) {
        return this;
    }

    @Override
    public RequestEventImpl build(RequestEventImpl.Type eventType) {
        return null;
    }
}
