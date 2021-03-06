/*
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.cas.authentication;

import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.jasig.cas.authentication.handler.AuthenticationException;
import org.jasig.cas.authentication.handler.AuthenticationHandler;
import org.jasig.cas.authentication.handler.BadCredentialsAuthenticationException;
import org.jasig.cas.authentication.handler.UnsupportedCredentialsException;
import org.jasig.cas.authentication.principal.Credentials;
import org.jasig.cas.authentication.principal.CredentialsToPrincipalResolver;
import org.jasig.cas.authentication.principal.Principal;
import org.jasig.cas.util.Pair;

/**
 * Ensures that all authentication handlers are tried, but if one is tried,
 * the associated CredentialsToPrincipalResolver is used.
 *
 * @author Scott Battaglia

 * @since 3.3.5
 */
public class LinkedAuthenticationHandlerAndCredentialsToPrincipalResolverAuthenticationManager
            extends AbstractAuthenticationManager {

    @NotNull
    @Size(min = 1)
    private final Map<AuthenticationHandler, CredentialsToPrincipalResolver> linkedHandlers;

    public LinkedAuthenticationHandlerAndCredentialsToPrincipalResolverAuthenticationManager(
            final Map<AuthenticationHandler, CredentialsToPrincipalResolver> linkedHandlers) {
        this.linkedHandlers = linkedHandlers;
    }

    @Override
    protected Pair<AuthenticationHandler, Principal> authenticateAndObtainPrincipal(
            final Credentials credentials) throws AuthenticationException {
        boolean foundOneThatWorks = false;
        String handlerName;
        AuthenticationException authException = BadCredentialsAuthenticationException.ERROR;

        for (final AuthenticationHandler authenticationHandler : this.linkedHandlers.keySet()) {
            if (!authenticationHandler.supports(credentials)) {
                continue;
            }

            foundOneThatWorks = true;
            boolean authenticated = false;
            handlerName = authenticationHandler.getClass().getName();

            try {
                authenticated = authenticationHandler.authenticate(credentials);
            } catch (final AuthenticationException e) {
                authException = e;
                logAuthenticationHandlerError(handlerName, credentials, e);
            } catch (final Exception e) {
                logAuthenticationHandlerError(handlerName, credentials, e);
            }

            if (authenticated) {
                logger.info("{} successfully authenticated {}", handlerName, credentials);
                final Principal p = this.linkedHandlers.get(authenticationHandler).resolvePrincipal(credentials);
                return new Pair<AuthenticationHandler, Principal>(authenticationHandler, p);
            }
            logger.info("{} failed to authenticate {}", handlerName, credentials);
        }

        if (foundOneThatWorks) {
            throw authException;
        }

        throw UnsupportedCredentialsException.ERROR;
    }
}
