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
package org.glassfish.jersey.client.oauth;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.jersey.oauth.signature.OAuthSignatureFeature;

/**
 * OAuth1 client filter feature registers the support for performing authenticated requests to the
 * Service Provider. The feature does not perform Authorization Flow (see {@link OAuth1AuthorizationFlow}
 * for details how to use Authorization Flow and retrieve Access Token). The feature uses {@link ConsumerCredentials}
 * and {@link AccessToken} to initialize the internal {@link javax.ws.rs.container.ContainerRequestFilter filter}
 * which will add {@code Authorization} headers containing OAuth authorization information including
 * the oauth signature.
 * <p>
 * The internal filter can be controlled by properties put into
 * the {@link javax.ws.rs.client.ClientRequestContext client request}
 * using {@link javax.ws.rs.client.ClientRequestContext#setProperty(String, Object)} method. The property keys
 * are defined in this class as a static variables (see their javadocs for usage). Using these properties a specific
 * {@link AccessToken} can be defined for each request for example.
 * </p>
 * Example of using specific access token for one request:
 * <pre>
 * final Response response = client.target("foo").request()
 *           .property(OAUTH_PROPERTY_ACCESS_TOKEN, new AccessToken("ab454f84e", "f454de848a54b")).get();
 * </pre>
 * <p>
 * See {@link OAuth1Builder} for more information of how to build this feature.
 * </p>
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 * @since 2.3
 */
public final class OAuth1ClientFilterFeature implements Feature {

    /**
     * Key of the property that can be attached to the
     * {@link javax.ws.rs.client.ClientRequestContext client request} using
     * {@link javax.ws.rs.client.ClientRequestContext#setProperty(String, Object)} and that
     * defines {@link ConsumerCredentials consumer credentials} that should be used when generating OAuth {@code Authorization}
     * http header. The property will override the setting of the internal
     * {@link javax.ws.rs.client.ClientRequestFilter filter} for the current request only.
     * <p>
     * The value of the property must be {@link ConsumerCredentials} instance.
     * </p>
     */
    public static final String OAUTH_PROPERTY_CONSUMER_CREDENTIALS = "JERSEY-OAUTH1-CONSUMER-CREDENTIALS";

    /**
     * Key of the property that can be attached to the
     * {@link javax.ws.rs.client.ClientRequestContext client request} using
     * {@link javax.ws.rs.client.ClientRequestContext#setProperty(String, Object)} and that
     * defines {@link AccessToken access token} that should be used when generating OAuth {@code Authorization}
     * http header. The property will override the setting of the internal
     * {@link javax.ws.rs.client.ClientRequestFilter filter} for the current request only.
     * <p>
     * The value of the property must be {@link AccessToken} instance.
     * </p>
     */
    public static final String OAUTH_PROPERTY_ACCESS_TOKEN = "JERSEY-OAUTH1-ACCESS-TOKEN";

    /**
     * Key of the property that can be attached to the
     * {@link javax.ws.rs.client.ClientRequestContext client request} using
     * {@link javax.ws.rs.client.ClientRequestContext#setProperty(String, Object)} and that
     * defines {@link org.glassfish.jersey.oauth.signature.OAuthParameters}
     * that should be used when generating OAuth {@code Authorization}
     * http header. The property will override the setting of the internal
     * {@link javax.ws.rs.client.ClientRequestFilter filter} for the current request only.
     * <p>
     * The value of the property must be {@link org.glassfish.jersey.oauth.signature.OAuthParameters} instance.
     * </p>
     * <p>
     * This property is for advanced usage and should not be used if not needed as it can make the filter
     * configuration inconsistent for
     * the request and can produce unwanted results.
     * </p>
     */
    public static final String OAUTH_PROPERTY_OAUTH_PARAMETERS = "JERSEY-OAUTH1-PARAMETERS";


    /**
     * Key of the property that can be attached to the
     * {@link javax.ws.rs.client.ClientRequestContext client request} using
     * {@link javax.ws.rs.client.ClientRequestContext#setProperty(String, Object)} and that
     * defines {@link org.glassfish.jersey.oauth.signature.OAuthSecrets}
     * that should be used when generating OAuth {@code Authorization}
     * http header. The property will override the setting of the internal
     * {@link javax.ws.rs.client.ClientRequestFilter filter} for the current request only.
     * <p>
     * The value of the property must be {@link org.glassfish.jersey.oauth.signature.OAuthSecrets} instance.
     * </p>
     * <p>
     * This
     * property is for advanced usage and should not be used if not needed as it can make the filter
     * configuration inconsistent for
     * the request and can produce unwanted results.
     * </p>
     */
    public static final String OAUTH_PROPERTY_OAUTH_SECRETS = "JERSEY-OAUTH1-SECRETS";

    private final OAuth1ClientFilter filter;


    /**
     * Create a new feature.
     *
     * @param filter The Oauth filter that should be used in request processing.
     */
    OAuth1ClientFilterFeature(OAuth1ClientFilter filter) {
        this.filter = filter;
    }


    @Override
    public boolean configure(FeatureContext context) {
        context.register(OAuthSignatureFeature.class);
        context.register(filter);
        return true;
    }
}
