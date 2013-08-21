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

package org.glassfish.jersey.client.oauth2;

/**
 * Main class to build the Authorization Flow instances. The {@link OAuth2ClientFilterFeature} can be
 * build directly by constructor of this feature.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 * @since 2.3
 */
public class OAuth2ClientSupport {


    /**
     * Get the builder of the {@link OAuth2CodeGrantFlow Authorization Code Grant Flow}.
     *
     * @param clientIdentifier Client identifier (id of application that wants to be approved). Issued by the
     *                         Service Provider.
     * @param authorizationUri The URI to which the user should be redirected to authorize our application. The URI points to the
     *                          authorization server and is defined by the Service Provider.
     * @param accessTokenUri The access token URI on which the access token can be requested. The URI points to the
     *                          authorization server and is defined by the Service Provider.
     * @return builder of the {@link OAuth2CodeGrantFlow Authorization Code Grant Flow}.
     */
    public static OAuth2CodeGrantFlow.Builder getAuthorizationCodeGranFlowBuilder(ClientIdentifier clientIdentifier,
                                                                                  String authorizationUri,
                                                                                  String accessTokenUri) {
        return new AuthCodeGrantImpl.Builder(clientIdentifier, authorizationUri, accessTokenUri);
    }

}

