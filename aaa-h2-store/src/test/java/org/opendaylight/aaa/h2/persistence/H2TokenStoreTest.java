/*
 * Copyright (c) 2016 Inocybe Technologies. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.aaa.h2.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import org.opendaylight.aaa.AuthenticationBuilder;
import org.opendaylight.aaa.ClaimBuilder;
import org.opendaylight.aaa.api.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mserngawy
 *
 */

public class H2TokenStoreTest {
    private static final Logger LOG = LoggerFactory.getLogger(H2TokenStoreTest.class);
    private final H2TokenStore h2TokenStore = new H2TokenStore();

    @After
    public void teardown() {
        try {
            h2TokenStore.close();
        } catch (Exception e) {
            LOG.error("Error while closing H2 TokenStore", e);
        }
    }

    @Test
    public void testTokenStore() throws InterruptedException {
        final String fooToken = "foo_token";
        Authentication auth = new AuthenticationBuilder(new ClaimBuilder()
                                                            .setUser("foo")
                                                            .setUserId("1234")
                                                            .addRole("admin").build()).build();
        h2TokenStore.put(fooToken, auth);
        assertEquals(auth, h2TokenStore.get(fooToken));
        h2TokenStore.delete(fooToken);
        assertNull(h2TokenStore.get(fooToken));
        Map<String, Object> configParameters = new HashMap<>();
        configParameters.put(h2TokenStore.SECS_TO_LIVE, Long.toString(2));
        h2TokenStore.updateConfigParameter(configParameters);
        h2TokenStore.put(fooToken, auth);
        Thread.sleep(3000);
        assertNull(h2TokenStore.get(fooToken));
    }

}
