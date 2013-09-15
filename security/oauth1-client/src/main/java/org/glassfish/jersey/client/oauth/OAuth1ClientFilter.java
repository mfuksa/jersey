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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import org.glassfish.jersey.client.oauth.internal.LocalizationMessages;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.oauth.signature.OAuthParameters;
import org.glassfish.jersey.oauth.signature.OAuthSecrets;
import org.glassfish.jersey.oauth.signature.OAuthSignature;
import org.glassfish.jersey.oauth.signature.OAuthSignatureException;

/**
 * Client filter that sign requests using OAuth 1 signatures and signature and other OAuth 1
 * parameters to the {@code Authorization} header. The filter can be used to perform authenticated
 * requests to Service Provider but also to perform requests needed for Authorization process (flow).
 *
 * @author Paul C. Bryan
 * @author Martin Matula
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 * @since 2.3
 */
class OAuth1ClientFilter implements ClientRequestFilter {

    @Inject
    private Provider<OAuthSignature> oAuthSignature;

    @Inject
    private Provider<MessageBodyWorkers> messageBodyWorkers;

    private final OAuthParameters parameters;
    private final OAuthSecrets secrets;

    public OAuth1ClientFilter() {
        parameters = new OAuthParameters();
        secrets = new OAuthSecrets();
    }

    public OAuth1ClientFilter(OAuthParameters parameters, OAuthSecrets secrets) {
        this.parameters = parameters;
        this.secrets = secrets;
    }

    public OAuth1ClientFilter(ConsumerCredentials consumerCredentials) {
        this();
        this.parameters.consumerKey(consumerCredentials.getConsumerKey());
        this.secrets.consumerSecret(consumerCredentials.getConsumerSecret());
    }


    public OAuth1ClientFilter(ConsumerCredentials consumerCredentials, String signatureMethod, String realm) {
        this(consumerCredentials);
        parameters.setSignatureMethod(signatureMethod);
        parameters.realm(realm);
    }


    @Override
    public void filter(ClientRequestContext request) throws IOException {
        final ConsumerCredentials consumerFromProperties
                = (ConsumerCredentials) request.getProperty(OAuth1ClientFilterFeature.OAUTH_PROPERTY_CONSUMER_CREDENTIALS);
        request.removeProperty(OAuth1ClientFilterFeature.OAUTH_PROPERTY_CONSUMER_CREDENTIALS);

        final AccessToken tokenFromProperties
                = (AccessToken) request.getProperty(OAuth1ClientFilterFeature.OAUTH_PROPERTY_ACCESS_TOKEN);
        request.removeProperty(OAuth1ClientFilterFeature.OAUTH_PROPERTY_ACCESS_TOKEN);


        final OAuthParameters paramFromProps
                = (OAuthParameters) request.getProperty(OAuth1ClientFilterFeature.OAUTH_PROPERTY_OAUTH_PARAMETERS);
        request.removeProperty(OAuth1ClientFilterFeature.OAUTH_PROPERTY_OAUTH_PARAMETERS);

        final OAuthSecrets secretsFromProps
                = (OAuthSecrets) request.getProperty(OAuth1ClientFilterFeature.OAUTH_PROPERTY_OAUTH_SECRETS);
        request.removeProperty(OAuth1ClientFilterFeature.OAUTH_PROPERTY_OAUTH_SECRETS);



        if (request.getHeaders().containsKey("Authorization")) {
            return;
        }

        final OAuthParameters paramCopy = paramFromProps != null ? (OAuthParameters) paramFromProps.clone() :
                            (OAuthParameters) parameters.clone(); // make modifications to clone

        final OAuthSecrets secretsCopy = secretsFromProps != null ? secretsFromProps.clone() : secrets.clone();

        checkParametersConsistency(paramCopy, secretsCopy);

        if (consumerFromProperties != null) {
            paramCopy.consumerKey(consumerFromProperties.getConsumerKey());
            secretsCopy.consumerSecret(consumerFromProperties.getConsumerSecret());
        }

        if (tokenFromProperties != null) {
            paramCopy.token(tokenFromProperties.getAccessToken());
            secretsCopy.tokenSecret(tokenFromProperties.getAccessTokenSecret());
        }

        if (paramCopy.getTimestamp() == null) {
            paramCopy.setTimestamp();
        }

        if (paramCopy.getNonce() == null) {
            paramCopy.setNonce();
        }

        try {
            oAuthSignature.get().sign(new RequestWrapper(request, messageBodyWorkers.get()), paramCopy, secretsCopy);
        } catch (OAuthSignatureException se) {
            throw new ProcessingException(LocalizationMessages.ERROR_REQUEST_SIGNATURE(), se);
        }
    }

    private void checkParametersConsistency(OAuthParameters oauthParameters, OAuthSecrets oauthSecrets) {
        if (oauthParameters.getSignatureMethod() == null) {
            oauthParameters.signatureMethod("HMAC-SHA1");
        }

        if (oauthParameters.getVersion() == null) {
            oauthParameters.version();
        }

        if (oauthSecrets.getConsumerSecret() == null || oauthParameters.getConsumerKey() == null) {
            throw new ProcessingException(LocalizationMessages.ERROR_CONFIGURATION_MISSING_CONSUMER());
        }

        if (oauthParameters.getToken() != null && oauthSecrets.getTokenSecret() == null) {
            throw new ProcessingException(LocalizationMessages.ERROR_CONFIGURATION_MISSING_TOKEN_SECRET());
        }
    }
}
