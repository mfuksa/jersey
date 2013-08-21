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

import java.net.URI;

import org.glassfish.jersey.oauth.signature.OAuthParameters;


/** Thrown from a client request by the {@link OAuthClientFilter} if
 * the request is not properly authorized. I.e. either when the user authorization
 * of a request token is required and has not been provided by the {@link AuthorizationHandler}
 * or if the request token got revoked by the user - i.e. the verifier provided by the
 * {@link AuthorizationHandler} was either null or invalid.
 * In the first case, client may redirect user to the URI returned
 * from {@link #getAuthorizationUri()}.
 * Once authorization is obtained, client should add verifier code returned
 * by the server into OAuth parameters object returned from
 * {@link UnauthorizedRequestException#getOAuthParameters()}.
 * In the second case {@link #getAuthorizationUri()} returns null.
 *
 * @author Martin Matula <martin.matula@oracle.com>
 */
public class UnauthorizedRequestException extends RuntimeException {
    private final OAuthParameters params;
    private final URI authUri;

    /**
     * Create a new exception instance.
     * @param parameters OAuth parameters used when initiating the request.
     * @param authorizationUri Authorization URI.
     */
    public UnauthorizedRequestException(OAuthParameters parameters, URI authorizationUri) {
        params = parameters;
        authUri = authorizationUri;
    }

    /** Returns OAuthParameters structure used by the {@link OAuthClientFilter}.
     * Can be used to update parameters to make the next request not fail (i.e. set a new
     * verification code).
     *
     * @return OAuth request parameters
     */
    public OAuthParameters getOAuthParameters() {
        return params;
    }

    /** Returns authorization URI the user can be redirected to to provide authorization,
     * or null if there is no request token to be authorized (i.e. user revoked access
     * to the request token obtained by the client).
     *
     * @return authorization URI or null
     */
    public URI getAuthorizationUri() {
        return authUri;
    }
}
