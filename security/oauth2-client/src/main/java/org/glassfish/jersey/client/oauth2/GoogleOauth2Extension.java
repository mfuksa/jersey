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
import javax.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.jackson.JacksonFeature;

/**
 * Class that provides methods to build {@link OAuth2CodeGrantFlow} pre-configured for usage
 * with Google provider.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 * @since 2.3
 */
public class GoogleOauth2Extension {

    /**
     * TODO javadocs in this class
     * @param clientIdentifier
     * @param redirectUri
     * @param scope
     * @return
     */
    public static GoogleAuthFlowBuilder authFlowBuilder(ClientIdentifier clientIdentifier,
                                                        String redirectUri, String scope) {

        return new GoogleAuthFlowBuilder()
                .setAuthorizationUri("https://accounts.google.com/o/oauth2/auth")
                .setAccessTokenUri("https://accounts.google.com/o/oauth2/token")
                .setRedirectUri(redirectUri == null ? Oauth2Parameters.REDIRECT_URI_UNDEFINED : redirectUri)
                .setClientIdentifier(clientIdentifier)
                .setScope(scope);
    }


    public static class GoogleAuthFlowBuilder extends AbstractAuthorizationCodeGrantBuilder<GoogleAuthFlowBuilder> {
        private Client client;

        public GoogleAuthFlowBuilder() {
            super(new AuthCodeGrantImpl.Builder());
        }

        public GoogleAuthFlowBuilder setAccessType(AccessType accessType) {
            getDelegate().property(OAuth2CodeGrantFlow.Phase.AUTHORIZATION, AccessType.getKey(), accessType.getValue());
            return this;
        }

        public GoogleAuthFlowBuilder setPromt(Promt promt) {
            getDelegate().property(OAuth2CodeGrantFlow.Phase.AUTHORIZATION, Promt.getKey(), promt.getValue());
            return this;
        }

        public GoogleAuthFlowBuilder setDisplay(Display display) {
            getDelegate().property(OAuth2CodeGrantFlow.Phase.AUTHORIZATION, Display.getKey(), display.getValue());
            return this;
        }

        public GoogleAuthFlowBuilder setLoginHint(String loginHint) {
            getDelegate().property(OAuth2CodeGrantFlow.Phase.AUTHORIZATION, Display.getKey(), loginHint);
            return this;
        }


        @Override
        public OAuth2CodeGrantFlow build() {
            if (client == null) {
                client = ClientBuilder.newClient();
            }
            if (!client.getConfiguration().isRegistered(AuthCodeGrantImpl.DefaultTokenMessageBodyReader.class)) {
                client.register(AuthCodeGrantImpl.DefaultTokenMessageBodyReader.class);
            }
            if (!client.getConfiguration().isRegistered(JacksonFeature.class)) {
                client.register(JacksonFeature.class);
            }

            return super.build();
        }
    }


    public static enum AccessType {
        ONLINE("online"),
        OFFLINE("offline");

        private final String propertyValue;

        private AccessType(String propertyValue) {
            this.propertyValue = propertyValue;
        }

        public String getValue() {
            return propertyValue;
        }

        public static String getKey() {
            return "access_type";
        }
    }

    public static enum Promt {
        NONE("none"),
        CONSENT("consent"),
        SELECT_ACCOUNT("select_account");

        private final String value;

        private Promt(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static String getKey() {
            return "promt";
        }

    }

    public static enum Display {
        PAGE("page"),
        POPUP("popup"),
        TOUCH("touch"),
        WAP("wap");

        private final String value;

        private Display(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static String getKey() {
            return "display";
        }
    }

    public static String LOGIN_HINT = "login_hint";


}
