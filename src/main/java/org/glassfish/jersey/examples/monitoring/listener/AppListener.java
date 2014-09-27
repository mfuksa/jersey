package org.glassfish.jersey.examples.monitoring.listener;

import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

/**
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class AppListener implements ApplicationEventListener {
    @Override
    public void onEvent(ApplicationEvent applicationEvent) {
    }

    @Override
    public RequestEventListener onRequest(RequestEvent requestEvent) {
        return new MyRequestListener();
    }

    private static class MyRequestListener implements RequestEventListener {
        private volatile long startTime;

        @Override
        public void onEvent(RequestEvent requestEvent) {
            switch (requestEvent.getType()) {
                case RESOURCE_METHOD_START:
                    startTime = System.currentTimeMillis();
                    break;
                case RESOURCE_METHOD_FINISHED:
                    long now = System.currentTimeMillis();
                    final long duration = now - startTime;
                    final String methodName = requestEvent.getUriInfo().getMatchedResourceMethod().getInvocable().getHandlingMethod().getName();
                    System.out.println("Method " + methodName + " took " + duration + " ms.");
                    break;
            }
        }
    }
}
