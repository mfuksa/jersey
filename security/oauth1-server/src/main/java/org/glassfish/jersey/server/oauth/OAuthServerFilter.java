/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.oauth;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Inject;

import javax.inject.Provider;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.oauth.signature.OAuthParameters;
import org.glassfish.jersey.oauth.signature.OAuthSecrets;
import org.glassfish.jersey.oauth.signature.OAuthSignature;
import org.glassfish.jersey.oauth.signature.OAuthSignatureException;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.oauth.internal.OAuthServerRequest;


/**
 * OAuth request filter that filters all requests indicating in the Authorization
 * header they use OAuth. Checks if the incoming requests are properly authenticated
 * and populates the security context with the corresponding user principal and roles.
 * <p>
 *
 * @author Paul C. Bryan <pbryan@sun.com>
 * @author Martin Matula
 */
class OAuthServerFilter implements ContainerRequestFilter {

    /** OAuth Server */
    @Inject
    private OAuthProvider provider;

    /** Manages and validates incoming nonces. */
    private final NonceManager nonces;

    /** Value to return in www-authenticate header when 401 response returned. */
    private final String wwwAuthenticateHeader;

    /** OAuth protocol versions that are supported. */
    private final Set<String> versions;

    /** Regular expression pattern for path to ignore. */
    private final Pattern ignorePathPattern;

    @Inject
    private OAuthSignature oAuthSignature;

    @Inject
    private Provider<ExtendedUriInfo> uriInfo;


    private final boolean optional;

    /**
     * Create a new filter.
     * @param rc Resource config.
     */
    @Inject
    public OAuthServerFilter(Configuration rc) {
        // establish supported OAuth protocol versions
        HashSet<String> v = new HashSet<String>();
        v.add(null);
        v.add("1.0");
        versions = Collections.unmodifiableSet(v);

        // optional initialization parameters (defaulted)
        String realm = PropertiesHelper.getValue(rc.getProperties(), OAuthProperties.REALM, "default", String.class);
        /* Maximum age (in milliseconds) of timestamp to accept in incoming messages. */
        int maxAge = PropertiesHelper.getValue(rc.getProperties(), OAuthProperties.MAX_AGE, 300000);
        /* Average requests to process between nonce garbage collection passes. */
        int gcPeriod = PropertiesHelper.getValue(rc.getProperties(), OAuthProperties.GC_PERIOD, 100);
        ignorePathPattern = pattern(PropertiesHelper.getValue(rc.getProperties(), OAuthProperties.IGNORE_PATH_PATTERN,
                null, String.class)); // no pattern
        optional = PropertiesHelper.isProperty(rc.getProperties(), OAuthProperties.NO_FAIL);
        nonces = new NonceManager(maxAge, gcPeriod);

        // www-authenticate header for the life of the object
        wwwAuthenticateHeader = "OAuth realm=\"" + realm + "\"";
    }


    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        // do not filter requests that do not use OAuth authentication
        String authHeader = request.getHeaderString(OAuthParameters.AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.toUpperCase().startsWith(OAuthParameters.SCHEME.toUpperCase())) {
            return;
        }

        // do not filter requests that matches to access or token resources
        final Method handlingMethod = uriInfo.get().getMatchedResourceMethod().getInvocable().getHandlingMethod();
        if (handlingMethod.isAnnotationPresent(TokenResource.class)
                || handlingMethod.getDeclaringClass().isAnnotationPresent(TokenResource.class)) {
            return;
        }



        // do not filter if the request path matches pattern to ignore
        if (match(ignorePathPattern, request.getUriInfo().getPath())) {
            return;
        }

        OAuthSecurityContext sc;

        try {
            sc = getSecurityContext(request);
        } catch (OAuthException e) {
            if (optional) {
                return;
            } else {
                throw e;
            }
        }

        request.setSecurityContext(sc);
    }


    private OAuthSecurityContext getSecurityContext(ContainerRequestContext request) throws OAuthException {
        OAuthServerRequest osr = new OAuthServerRequest(request);
        OAuthParameters params = new OAuthParameters().readRequest(osr);

        // apparently not signed with any OAuth parameters; unauthorized
        if (params.size() == 0) {
            throw newUnauthorizedException();
        }

        // get required OAuth parameters
        String consumerKey = requiredOAuthParam(params.getConsumerKey());
        String token = params.getToken();
        String timestamp = requiredOAuthParam(params.getTimestamp());
        String nonce = requiredOAuthParam(params.getNonce());

        // enforce other supported and required OAuth parameters
        requiredOAuthParam(params.getSignature());
        supportedOAuthParam(params.getVersion(), versions);

        // retrieve secret for consumer key
        OAuthConsumer consumer = provider.getConsumer(consumerKey);
        if (consumer == null) {
            throw newUnauthorizedException();
        }

        OAuthSecrets secrets = new OAuthSecrets().consumerSecret(consumer.getSecret());
        OAuthSecurityContext sc;
        String nonceKey;

        if (token == null) {
            if (consumer.getPrincipal() == null) {
                throw newUnauthorizedException();
            }
            nonceKey = "c:" + consumerKey;
            sc = new OAuthSecurityContext(consumer, request.getSecurityContext().isSecure());
        } else {
            OAuthToken accessToken = provider.getAccessToken(token);
            if (accessToken == null) {
                throw newUnauthorizedException();
            }

            OAuthConsumer atConsumer = accessToken.getConsumer();
            if (atConsumer == null || !consumerKey.equals(atConsumer.getKey())) {
                throw newUnauthorizedException();
            }

            nonceKey = "t:" + token;
            secrets.tokenSecret(accessToken.getSecret());
            sc = new OAuthSecurityContext(accessToken, request.getSecurityContext().isSecure());
        }

        if (!verifySignature(osr, params, secrets)) {
            throw newUnauthorizedException();
        }

        if (!nonces.verify(nonceKey, timestamp, nonce)) {
            throw newUnauthorizedException();
        }

        return sc;
    }

    private static String requiredOAuthParam(String value) throws OAuthException {
        if (value == null) {
            throw newBadRequestException();
        }
        return value;
    }

    private static String supportedOAuthParam(String value, Set<String> set) throws OAuthException {
        if (!set.contains(value)) {
            throw newBadRequestException();
        }
        return value;
    }

    private static Pattern pattern(String p) {
        if (p == null) {
            return null;
        }
        return Pattern.compile(p);
    }

    private static boolean match(Pattern pattern, String value) {
        return (pattern != null && value != null && pattern.matcher(value).matches());
    }

    private boolean verifySignature(OAuthServerRequest osr, OAuthParameters params, OAuthSecrets secrets) {
        try {
            return  oAuthSignature.verify(osr, params, secrets);
        } catch (OAuthSignatureException ose) {
            throw newBadRequestException();
        }
    }

    private static OAuthException newBadRequestException() throws OAuthException {
        return new OAuthException(Response.Status.BAD_REQUEST, null);
    }

    private OAuthException newUnauthorizedException() throws OAuthException {
        return new OAuthException(Response.Status.UNAUTHORIZED, wwwAuthenticateHeader);
    }

}
