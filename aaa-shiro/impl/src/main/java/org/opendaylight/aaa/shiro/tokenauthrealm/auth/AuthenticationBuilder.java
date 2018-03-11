/*
 * Copyright (c) 2014, 2017 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.aaa.shiro.tokenauthrealm.auth;

import java.io.Serializable;
import java.util.Set;
import org.opendaylight.aaa.api.Authentication;
import org.opendaylight.aaa.api.Claim;
import org.opendaylight.aaa.shiro.tokenauthrealm.util.EqualUtil;
import org.opendaylight.aaa.shiro.tokenauthrealm.util.HashCodeUtil;

/**
 * A builder for the authentication context.
 *
 * <p>
 * The expiration DEFAULTS to 0.
 *
 * @author liemmn
 */
public class AuthenticationBuilder {

    private long expiration = 0L;
    private final Claim claim;

    public AuthenticationBuilder(Claim claim) {
        this.claim = claim;
    }

    public AuthenticationBuilder setExpiration(long expiration) {
        this.expiration = expiration;
        return this;
    }

    public Authentication build() {
        return new ImmutableAuthentication(this);
    }

    private static final class ImmutableAuthentication implements Authentication, Serializable {
        private static final long serialVersionUID = 4919078164955609987L;
        private int hashCode = 0;
        long expiration = 0L;
        Claim claim;

        private ImmutableAuthentication(AuthenticationBuilder base) {
            if (base.claim == null) {
                throw new IllegalStateException("The Claim is null.");
            }
            claim = new ClaimBuilder(base.claim).build();
            expiration = base.expiration;

            if (base.expiration < 0) {
                throw new IllegalStateException("The expiration is less than 0.");
            }
        }

        @Override
        public long expiration() {
            return expiration;
        }

        @Override
        public String clientId() {
            return claim.clientId();
        }

        @Override
        public String userId() {
            return claim.userId();
        }

        @Override
        public String user() {
            return claim.user();
        }

        @Override
        public String domain() {
            return claim.domain();
        }

        @Override
        public Set<String> roles() {
            return claim.roles();
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Authentication)) {
                return false;
            }
            Authentication authentication = (Authentication) object;
            return EqualUtil.areEqual(expiration, authentication.expiration()) && EqualUtil
                    .areEqual(claim.roles(), authentication.roles()) && EqualUtil
                    .areEqual(claim.domain(), authentication.domain()) && EqualUtil
                    .areEqual(claim.userId(), authentication.userId()) && EqualUtil
                    .areEqual(claim.user(), authentication.user()) && EqualUtil
                    .areEqual(claim.clientId(), authentication.clientId());
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                int result = HashCodeUtil.SEED;
                result = HashCodeUtil.hash(result, expiration);
                result = HashCodeUtil.hash(result, claim.hashCode());
                hashCode = result;
            }
            return hashCode;
        }

        @Override
        public String toString() {
            return "expiration:" + expiration + "," + claim.toString();
        }
    }
}
