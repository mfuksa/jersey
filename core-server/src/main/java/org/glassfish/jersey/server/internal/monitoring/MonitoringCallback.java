package org.glassfish.jersey.server.internal.monitoring;

import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

/**
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
// TODO: M: move to internal? or at least our mbeans?
public interface MonitoringCallback {

    public MonitoringApplicationContext onApplicationInitialized();



    public static interface MonitoringContext {
        public void finished();
    }

    public static interface MonitoringApplicationContext extends MonitoringContext {
        public MonitoringRequestContext onRequest();
    }

    public static interface MonitoringRequestContext extends MonitoringContext {
        public MonitoringContext onResourceMethod(Resource parentResource, Resource childResource,
                                                  ResourceMethod resourceMethod);
        public void onMappedException(Throwable exception);
        public void onUnmappedException(Throwable exception);
        public void onResponse(int responseCode);
    }





}
