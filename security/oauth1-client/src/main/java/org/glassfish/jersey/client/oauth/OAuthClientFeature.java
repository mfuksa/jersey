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

import org.glassfish.jersey.oauth.signature.OAuthParameters;
import org.glassfish.jersey.oauth.signature.OAuthSecrets;
import org.glassfish.jersey.oauth.signature.OAuthSignatureFeature;

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
 * // create OAuthClientFeature and initialize it with filter
 *  final OAuthClientFeature oauthFeature = new OAuthClientFeature(oAuthClientFilter);
 *
 * // add the feature to the client
 * client.register(oauthFeature);
 *
 * // make calls to the protected resources (authorization is handled
 * // by the filter (and the passed AuthorizationHandler) as needed, transparently
 *
 * WebTarget target = client.target("http://my.service.uri/items");
 * String response = target.request().get(String.class);
 * </pre>
 *
 * @author Paul C. Bryan
 * @author Martin Matula
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */


/**
 * OAuth client feature that enables support for OAuth on the client.
 * <p>
 * The feature can be configured to work in two modes. First mode is the full 'Authorization Flow' mode
 * in which the feature takes care of OAuth Authorization flow and subsequently signs requests with
 * acquired Access Token. In the second 'Signature' mode only signs requests with already
 * provided Access Token.
 * </p><p>
 * Authorization mode should be used when Access token is not retrieved yet and a user still
 * needs to grant authorization to the
 * client application. The result of this grant is the Access Token and Access Token Secret which
 * is used for authenticated request to resource provider. After the token is retrieved, the filter switches to
 * the signature mode and the same instance of the {@link javax.ws.rs.client.Client} or
 * {@link javax.ws.rs.client.WebTarget} can be used to perform already authenticated requests. If Access Token
 * is already retrieved (e.g. stored already in the database), the user does not need to authorize the
 * client application and filter can be created in the signature mode.
 *
 * <p/>
 * The feature is using the {@link OAuthClientFeature.Builder feature builder}. The builder provides
 * methods to build both modes of filter.
 * <p/>
 * Example:
 * <p/>
 * For both modes, we need to create an instance of the {@code Builder}:
 * <pre>
 * // create a builder instance and initialize it with consumer key and consumer secret
 * final Builder featureBuilder = OAuthClientFeature.builder("myJerseyClientApplication", "s4f65s4fsskjhs54f6ds");
 * </pre>
 * <p>
 * Consumer key and consumer secret are keys that you will receive from the Service Provider when you
 * register your client application. This key pair will be used for requesting resources of all users
 * registered on the Service Provider side.
 * </p>
 *
 * <b>Authorization flow mode</b>:
 * <pre>
 * // create an authorization handler
 *
 * final AuthorizationHandler authorizationHandler = new AuthorizationHandler() {}
 *
 *     &#64;Override
 *     public String authorize(URI authorizationUri) &#123;
 *         // ... redirect user to Service Provider to authorize our application
 *         // and get back verification code
 *         String verificationCode = // ....
 *         return verificationCode;
 *     }
 *
 *     &#64;Override
 *     public void authorized(String token, String tokenSecret) {}
 *         // user has =authorized -> store Access Token + secret to database
 *     }
 * }
 *
 * final OAuthClientFeature oAuthClientFeature = featureBuilder.buildAuthorizationFlowMode(
 *         "http://serviceprovider.org/oauth/requestToken",
 *         "http://serviceprovider.org/oauth/accessToken",
 *         "http://serviceprovider.org/oauth/userAuthorization",
 *         authorizationHandler,
 *         "http://myserver/oauth/redirect/sdfjhgsd5d321d");
 * </pre>
 *
 * <b>Signature mode</b>:
 * <pre>
 * final OAuthClientFeature oAuthClientFeature = featureBuilder.buildSignatureModeFeature("userAccessToken", "ksjdfytjggrr54rgt51");
 * </pre>
 *
 *
 * In this point the feature is built. Note, that you might need to setup additional values to builder if the ServiceProvider
 * requires it (realm, signature method, etc).
 *
 * The following code is common for both feature modes.
 * <pre>
 * // register feature into the client:
 * final Client client = ClientBuilder.newBuilder().register(oAuthClientFeature).build();
 * final Response response = client.target("http://serviceprovider.com/rest/foo").request().get();
 * </pre>
 *
 * If the feature was created in the Authorization Flow mode, the Authorization flow will be performed
 * during the first request and {@code authorizationHandler} will be called.
 *
 * @see AuthorizationHandler AuthorizationHandler
 *
 * @since 2.3
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class OAuthClientFeature implements Feature {
    private final OAuthClientFilter oAuthClientFilter;

    /**
     * Create a new feature instance with configured filter.
     *
     * @param oAuthClientFilter Client OAuth filter.
     * @see OAuthClientFilter for information about how to configure the filter.
     */
    OAuthClientFeature(OAuthClientFilter oAuthClientFilter) {
        this.oAuthClientFilter = oAuthClientFilter;
    }

    @Override
    public boolean configure(FeatureContext context) {
        context.register(OAuthSignatureFeature.class);
        context.register(oAuthClientFilter);
        return true;
    }


    public static Builder builder(String consumerKey, String consumerSecret) {
        return new BuilderImpl(consumerKey, consumerSecret);
    }

    /**
     * The {@link OAuthClientFeature OAuth feature client builder}.
     */
    public static interface Builder {

        /**
         * Set the signature method name. The signature methods implement
         * {@link org.glassfish.jersey.oauth.signature.OAuthSignatureMethod} and the name is retrieved from
         * {@link org.glassfish.jersey.oauth.signature.OAuthSignatureMethod#name()} method. Build-in signature
         * methods are {@code HMAC-SHA1}, {@code RSA-SHA1} and {@code PLAINTEXT}.
         * <p>
         * Default value is {@code HMAC-SHA1}.
         * </p>
         *
         * @param signatureMethod Signature method name.
         * @return This builder instance.
         */
        public abstract Builder setSignatureMethod(String signatureMethod);

        /**
         * Set the realm to which the user wants to authenticate. The parameter
         *              will be sent in Authenticated request and used during Authorization Flow.
         *
         * @param realm Realm on the server to which the user authentication is required.
         * @return This builder instance.
         */
        public abstract Builder setRealm(String realm);

        /**
         * Set the timestamp. The timestamp if defined will be used in {@code Authorization} header. Usually this
         * parameter is not defined explicitly by this method and will be automatically filled with the current
         * time during the request.
         *
         * @param timestamp Timestamp value.
         * @return This builder instance.
         */
        public abstract Builder setTimestamp(String timestamp);

        /**
         * Set the nonce. Nonce (shortcut of "number used once") is used to uniquely identify the request and
         * prevent from multiple usage of the same signed request. The nonce if defined will be used
         * in the {@code Authorization} header if defined. Usually this
         * parameter is not defined explicitly by this method and will be automatically filled with the randomly
         * generated UID during the request.
         *
         * @param nonce Nonce value.
         * @return This builder instance.
         */
        public abstract Builder setNonce(String nonce);

        /**
         * Set the version of the OAuth protocol. The version, if defined, will be used in the {@code Authorization}
         * header otherwise default value {@code 1.1} will be used. Usually this parameter does not need to be
         * overwritten by this method.
         *
         * @param version OAuth protocol version parameter.
         * @return This builder instance.
         */
        public abstract Builder setVersion(String version);

        /**
         * Return {@link OAuthParameters OAuth parameters} that contains configuration of this builder. The
         * returned reference is a reference to internal store of parameters and therefore any changes done
         * on returned OAuth parameters will influence the OAuth configuration of the client. This
         * method should be used only when there is for example need to modify Access Tokens during the
         * client lifecycle.
         *
         * @return OAuth parameters.
         */
        public abstract OAuthParameters getOAuthParameters();

        /**
         * Return {@link OAuthSecrets OAuth secret parameters} that contains configuration of this builder. The
         * returned reference is a reference to internal store of parameters and therefore any changes done
         * on returned OAuth parameters will influence the OAuth configuration of the client. This
         * method should be used only when there is for example need to modify Access Tokens during the
         * client lifecycle.
         *
         * @return OAuth parameters.
         */
        public abstract OAuthSecrets getOAuthSecrets();

        /**
         * Build the feature instance configured in the Signature mode.
         *
         * @param accessToken Access Token.
         * @param accessTokenSecret Access Token secret.
         *
         * @return Configured feature.
         */
        public abstract OAuthClientFeature buildSignatureModeFeature(String accessToken, String accessTokenSecret);

        /**
         * Build the feature in the Authorization Flow mode for limited clients (not a web servers). Limited client
         * is a client that cannot handle redirections from the Authorization servers. In this mode the callback
         * URI is not specified and default value "oob" will be used during the authorization flow.
         *
         * @param requestTokenUri Request Token URI (defined by Service Provider).
         * @param accessTokenUri Access Token URI (defined by Service Provider).
         * @param authorizationUri Authorization URI where the client should
         *                         perform authorization of the application (defined by Service Provider).
         * @param authorizationHandler Authorization handler.
         * @return Configured feature.
         */
        public abstract OAuthClientFeature buildAuthorizationFlowModeForLimitedClients(String requestTokenUri,
                String accessTokenUri, String authorizationUri, AuthorizationHandler authorizationHandler);

        /**
         * Build the feature in the Authorization Flow mode. The client is capable of handling redirections
         * from the Authorization servers (for example the client is a web server) and therefore the
         * {@code callbackUri} is specified.
         *
         * @param requestTokenUri Request Token URI (defined by Service Provider).
         * @param accessTokenUri Access Token URI (defined by Service Provider).
         * @param authorizationUri Authorization URI where the client should
         *                         perform authorization of the application (defined by Service Provider).
         * @param authorizationHandler Authorization handler.
         * @param callbackUri Callback URI to which the client will be redirected after authorizing
         *                    the client application on the Service Provider side.
         * @return Configured feature.
         */
        public abstract OAuthClientFeature buildAuthorizationFlowMode(String requestTokenUri,
                                                                      String accessTokenUri,
                                                                      String authorizationUri,
                                                                      AuthorizationHandler authorizationHandler,
                                                                      String callbackUri);
    }

    private static class BuilderImpl implements Builder {

        private final OAuthParameters params;
        private final OAuthSecrets secrets;

        private BuilderImpl(String consumerKey, String consumerSecret) {
            this.params = new OAuthParameters();
            params.setConsumerKey(consumerKey);
            this.secrets = new OAuthSecrets();
            secrets.setConsumerSecret(consumerSecret);

            // spec defines that when no callback uri is used (e.g. client is unable to receive callback
            // as it is a mobile application), the "oob" value should be used.
            this.params.setCallback(OAuthParameters.NO_CALLBACK_URI_VALUE);

        }

        @Override
        public Builder setSignatureMethod(String signatureMethod) {
            params.setSignatureMethod(signatureMethod);
            return this;
        }

        @Override
        public Builder setRealm(String realm) {
            params.setRealm(realm);
            return this;
        }

        @Override
        public Builder setTimestamp(String timestamp) {
            params.setTimestamp(timestamp);
            return this;
        }

        @Override
        public Builder setNonce(String timestamp) {
            params.setNonce(timestamp);
            return this;
        }

        @Override
        public Builder setVersion(String timestamp) {
            params.setVersion(timestamp);
            return this;
        }

        @Override
        public OAuthParameters getOAuthParameters() {
            return params;
        }

        @Override
        public OAuthSecrets getOAuthSecrets() {
            return secrets;
        }

        @Override
        public OAuthClientFeature buildSignatureModeFeature(String accessToken, String accessTokenSecret) {
            params.setToken(accessToken);
            secrets.setTokenSecret(accessTokenSecret);
            OAuthClientFilter filter = new OAuthClientFilter(params, secrets);
            return new OAuthClientFeature(filter);
        }

        @Override
        public OAuthClientFeature buildAuthorizationFlowModeForLimitedClients(String requestTokenUri,
                                                                              String accessTokenUri, String authorizationUri, AuthorizationHandler authorizationHandler) {
            final OAuthClientFilter filter = new OAuthClientFilter(params, secrets, requestTokenUri,
                    accessTokenUri, authorizationUri, authorizationHandler);
            return new OAuthClientFeature(filter);
        }

        @Override
        public OAuthClientFeature buildAuthorizationFlowMode(String requestTokenUri,
                                                             String accessTokenUri, String authorizationUri,
                                                             AuthorizationHandler authorizationHandler,
                                                             String callbackUri) {
            params.setCallback(callbackUri);
            return buildAuthorizationFlowModeForLimitedClients(requestTokenUri, accessTokenUri,
                    authorizationUri, authorizationHandler);
        }
    }


}
