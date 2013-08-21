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

import javax.ws.rs.client.Client;

/**
 * Abstract implementation of {@link OAuth2CodeGrantFlow.Builder}.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 * @since 2.3
 */
class AbstractAuthorizationCodeGrantBuilder<T extends OAuth2CodeGrantFlow.Builder> implements OAuth2CodeGrantFlow.Builder<T> {

    private final OAuth2CodeGrantFlow.Builder<T> delegate;

    public AbstractAuthorizationCodeGrantBuilder(OAuth2CodeGrantFlow.Builder<T> delegate) {
        this.delegate = delegate;
    }

    public OAuth2CodeGrantFlow.Builder<T> getDelegate() {
        return delegate;
    }

    @Override
    public T setAccessTokenUri(String accessTokenUri) {
        delegate.setAccessTokenUri(accessTokenUri);
        return (T) this;
    }

    @Override
    public T setAuthorizationUri(String authorizationUri) {
        delegate.setAuthorizationUri(authorizationUri);
        return (T) this;
    }

    @Override
    public T setRedirectUri(String redirectUri) {
        delegate.setRedirectUri(redirectUri);
        return (T) this;
    }

    @Override
    public T setClientIdentifier(ClientIdentifier consumerCredentials) {
        delegate.setClientIdentifier(consumerCredentials);
        return (T) this;
    }

    @Override
    public T setScope(String scope) {
        delegate.setScope(scope);
        return (T) this;
    }

    @Override
    public T setClient(Client client) {
        delegate.setClient(client);
        return (T) this;
    }

    @Override
    public T setRefreshTokenUri(String refreshTokenUri) {
        delegate.setRefreshTokenUri(refreshTokenUri);
        return (T) this;
    }

    @Override
    public T property(OAuth2CodeGrantFlow.Phase phase, String key, String value) {
        delegate.property(phase, key, value);
        return (T) this;
    }

    @Override
    public OAuth2CodeGrantFlow build() {
        return delegate.build();
    }
}
