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

import java.io.IOException;
import java.net.URI;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.Priorities;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.oauth.signature.OAuthParameters;
import org.glassfish.jersey.oauth.signature.OAuthSecrets;
import org.glassfish.jersey.oauth.signature.OAuthSignature;
import org.glassfish.jersey.oauth.signature.OAuthSignatureException;

import com.sun.xml.internal.ws.client.RequestContext;

/**
 * Client filter adding OAuth authorization header to the HTTP request, if no
 * authorization header is already present.
 * <p>
 * The filter can be configured to work in two modes. First mode is the full mode when
 * the filter takes care of OAuth Authorization flow and subsequently sign requests with
 * acquired Access Token. In the second mode the filter only sign requests with
 * provided Access Token.
 * <p/>
 * If the URI's for requesting Request and Access Tokens and authorization URI are
 * provided, as well as the {@link AuthorizationHandler} implementation,
 * the filter works in the full mode and also takes care of handling the OAuth authorization
 * flow (process). The authorization will start even if the Access Token is supplied.
 * <p>
 * If the URI's are missing but Access Token (and Access Token secret) is provided then
 * the filter works only in signature mode.
 * </p>
 * <p>
 * If the URI's are missing and Access Token is not defined neither, the filter will sign the
 * request only with Consumer key and secret. This is an edge case configuration but can be use
 * to perform explicitly created request to Request Token resources.
 * </p>
 *
 * <p>
 * Note: This filter signs the request based on its request parameters.
 * For this reason, the filter should be invoked after any others that
 * modify any request parameters.
 * </p>
 * <p>
 * Example 1:
 *
 * <pre>
 * // baseline OAuth parameters for access to resource
 * OAuthParameters params = new OAuthParameters().signatureMethod("HMAC-SHA1").
 *  consumerKey("key").setToken("accesskey");
 *
 * // OAuth secrets to access resource
 * OAuthSecrets secrets = new OAuthSecrets().consumerSecret("secret").setTokenSecret("accesssecret");
 *
 * // if parameters and secrets remain static, filter can be added to OAuthClientFeature
 * OAuthClientFilter filter = new OAuthClientFilter(client.getProviders(), params, secrets);
 * OAuthClientFeature oAuthClientFeature = new OAuthClientFeature(oAuth1ClientFilter);
 *
 * // And finally the feature is used in the client configuration
 * Client client = ClientBuilder.newBuilder().register(oAuthClientFeature).build();
 *
 * // OAuth test server
 * WebTarget target = client.resource("http://term.ie/oauth/example/request_token.php");
 *
 * String response = target.request().get(String.class);
 * </pre>
 *
 * <p>
 * Example 2 (handling authorization flow):
 *
 * <pre>
 * OAuthClientFilter filter = new OAuthClientFilter(
 *     new OAuthParameters().consumerKey("key"),
 *     new OAuthSecrets().consumerSecret("secret"),
 *     "http://request.token.uri",
 *     "http://access.token.uri",
 *     "http://authorization.uri",
 *     new AuthorizationHandler() {
 *         @Override
 *         public void authorized(String token, String tokenSecret) {
 *             // store the access token for future use
 *             storeAccessToken(token, tokenSecret);
 *         }
 *
 *         @Override
 *         public String authorize(URI authorizationUri) {
 *             try {
 *                 // ask user to authorize the app and enter the verification code
 *                 // generated by the server
 *                 String verificationCode = askUserToGoToAuthUriAuthorizeAndEnterVerifier(authorizationUri);
 *                 return verificationCode;
 *             } catch (IOException ex) {
 *                 throw new RuntimeException(ex);
 *             }
 *         }
 *     }
 * );
 *
 * // add the filter to the client
 * client.addFilter(filter);
 *
 * // make calls to the protected resources (authorization is handled
 * // by the filter (and the passed AuthorizationHandler) as needed, transparently
 *
 * WebTarget target = client.resource("http://my.service.uri/items");
 * String response = resource.request().get(String.class);
 * </pre>
 *
 * @author Paul C. Bryan
 * @author Martin Matula
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
@Priority(Priorities.AUTHENTICATION)
public final class OAuthClientFilter implements ClientRequestFilter, ClientResponseFilter {

    private static final String JERSEY_OAUTH_FILTER_SIGN_ONLY = "jersey-oauth-filter-sign-only";

    /** The OAuth parameters to be used in generating signature. */
    private final OAuthParameters parameters;

    /** The OAuth secrets to be used in generating signature. */
    private final OAuthSecrets secrets;

    private final URI requestTokenUri;
    private final URI accessTokenUri;
    private final URI authorizationUri;

    private final AuthorizationHandler handler;

    @Inject
    private Provider<OAuthSignature> oAuthSignature;

    @Inject
    private Provider<MessageBodyWorkers> messageBodyWorkers;

    private enum State {
        NOT_AUTHORIZED_YET,
        AUTHORIZED
    }

    private State state;


    /**
     * Construct a new OAuth client filter that will only sign request with already
     * defined {@code parameters} and {@code secrets}.
     *
     *
     * @param parameters the OAuth parameters to be used in signing requests.
     *                   If {@link OAuthParameters#TOKEN access token} is not defined, the requests will be signed
     *                   only with consumer key as defined by the oauth specification. Parameters must contain
     *                   {@link OAuthParameters#CONSUMER_KEY consumer key} and secret.
     * @param secrets the OAuth secrets to be used in signing requests. Parameter
     *                   {@link OAuthParameters#TOKEN_SECRET access token secret} must be defined if
     *                   {@link OAuthParameters#TOKEN access token} is defined.
     */
    public OAuthClientFilter(final OAuthParameters parameters, final OAuthSecrets secrets) {
        this(parameters, secrets, null, null, null, null);
    }

    /**
     * Construct a new OAuth client filter providing URI's for requesting
     * request and access tokens and authorization. If {@code parameters} and {@code secrets}
     * contain {@link OAuthParameters#TOKEN access token} and {@link OAuthParameters#TOKEN_SECRET access token secret},
     * these values will be used as access token to sign the requests. If access token is not defined,
     * provided {@code requestTokenUri},
     * {@code accessTokenUri} and {@code authorizationUri} will
     * be used to perform the authorization process.
     *
     *
     * @param parameters the OAuthParameters to be used in signing requests. Parameters should contain
     *                   {@link OAuthParameters#CONSUMER_KEY consumer key} and secret.
     * @param secrets the OAuth secrets to be used in signing requests.
     * @param requestTokenUri URI for requesting new request tokens.
     * @param accessTokenUri URI for requesting access tokens.
     * @param authorizationUri URI for requesting authorization of request tokens.
     * @param handler Implementation of AuthHandler this filter calls to obtain user authorization
     *          and notify the application of a new access token obtained. If null is passed,
     *          instead of invoking the handler for user authorization when needed,
     *          {@link UnauthorizedRequestException} is thrown by the filter.
     */
    public OAuthClientFilter(OAuthParameters parameters,
                             OAuthSecrets secrets, String requestTokenUri, String accessTokenUri,
                             String authorizationUri, AuthorizationHandler handler) {
        if (parameters == null || secrets == null) {
            throw new NullPointerException();
        }
        if ((requestTokenUri != null || accessTokenUri != null || authorizationUri != null) &&
                (requestTokenUri == null || accessTokenUri == null || authorizationUri == null)) {
            throw new NullPointerException();
        }
        this.parameters = parameters;
        this.secrets = secrets;

        this.handler = handler == null ? new AuthorizationHandler() {
            @Override
            public void authorized(String token, String tokenSecret) {
            }

            @Override
            public String authorize(URI authorizationUri) {
                return null;
            }
        } : handler;

        if (parameters.getSignatureMethod() == null) {
            parameters.signatureMethod("HMAC-SHA1");
        }

        if (parameters.getVersion() == null) {
            parameters.version();
        }

        if (secrets.getConsumerSecret() == null || parameters.getConsumerKey() == null) {
            throw new IllegalStateException("Consumer secret and consumer key must not be null.");
        }

        if (parameters.getToken() != null && secrets.getTokenSecret() == null) {
            throw new IllegalStateException("Token is not null but token secret is null");
        }

        if (requestTokenUri == null) {
            this.requestTokenUri = this.accessTokenUri = this.authorizationUri = null;

            state = State.AUTHORIZED;
        } else {
            this.requestTokenUri = UriBuilder.fromUri(requestTokenUri).build();
            this.accessTokenUri = UriBuilder.fromUri(accessTokenUri).build();
            this.authorizationUri = UriBuilder.fromUri(authorizationUri).build();
            if (secrets.getTokenSecret() == null || parameters.getToken() == null) {
                state = State.NOT_AUTHORIZED_YET;
            }
        }
    }


    @Override
    public void filter(ClientRequestContext request) throws IOException {
        if (request.getHeaders().containsKey("Authorization")) {
            return;
        }
        final boolean signOnly = "true".equals(request.getHeaderString(JERSEY_OAUTH_FILTER_SIGN_ONLY));

        if (!signOnly) {
            if (state == State.NOT_AUTHORIZED_YET) {

                // request temporary credentials
                final Configuration clientConfiguration = request.getConfiguration();
                final Client client = ClientBuilder.newBuilder().withConfig(clientConfiguration).build();

                Response response = client.target(requestTokenUri)
                        .request().header(JERSEY_OAUTH_FILTER_SIGN_ONLY, "true").post(null);
                if (response.getStatus() != 200) {
                    throw new RuntimeException("Unsuccessful request for Request Token. Response status: " + response.getStatus());
                }
                Form form = response.readEntity(Form.class);
                String token = form.asMap().getFirst(OAuthParameters.TOKEN);
                parameters.token(token);
                secrets.tokenSecret(form.asMap().getFirst(OAuthParameters.TOKEN_SECRET));
                parameters.verifier(handler.authorize(getAuthorizationUri()));

                // GET TOKEN
                response = client.target(accessTokenUri)
                        .request().header(JERSEY_OAUTH_FILTER_SIGN_ONLY, "true").post(null);
                // accessToken request failed
                if (response.getStatus() >= 400) {
                    throw new RuntimeException("Error requesting access token.");
                }
                form = response.readEntity(Form.class);
                String accessToken = form.asMap().getFirst(OAuthParameters.TOKEN);
                final String accessTokenSecret = form.asMap().getFirst(OAuthParameters.TOKEN_SECRET);
                if (accessToken == null) {
                    throw new UnauthorizedRequestException(parameters, null);
                }

                parameters.token(accessToken);
                secrets.tokenSecret(accessTokenSecret);
                handler.authorized(parameters.getToken(), secrets.getTokenSecret());
                state = State.AUTHORIZED;
            }

        } else {
            request.getHeaders().remove(JERSEY_OAUTH_FILTER_SIGN_ONLY);
        }

        final OAuthParameters p = (OAuthParameters) parameters.clone(); // make modifications to clone

        if (p.getTimestamp() == null) {
            p.setTimestamp();
        }

        if (p.getNonce() == null) {
            p.setNonce();
        }

        try {
            oAuthSignature.get().sign(new RequestWrapper(request, messageBodyWorkers.get()), p, secrets);
        } catch (OAuthSignatureException se) {
            throw new ProcessingException("Error signing the request.", se);
        }
    }


    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext response) throws IOException {
        if (response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()) {
            parameters.token(null);
            secrets.tokenSecret(null);
            state = State.NOT_AUTHORIZED_YET;

        }
    }

    private URI getAuthorizationUri() {
        return UriBuilder.fromUri(authorizationUri)
                .queryParam(OAuthParameters.TOKEN, parameters.getToken())
                .build();
    }
}

