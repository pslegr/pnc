package org.jboss.pnc.auth;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

public class ExternalAuthenticationTest {

    @Test
    public void testDirectAccessGrant() {
        try {
            InputStream is = this.getClass().getResourceAsStream("/keycloak.json");
            ExternalAuthentication ea = new ExternalAuthentication(is);
            AuthenticationProvider provider = ea.authenticate(System.getenv("PNC_EXT_OAUTH_USERNAME"), System.getenv("PNC_EXT_OAUTH_PASSWORD"));
            Assert.assertNotNull(provider);
            Assert.assertNotNull(provider.getPrefferedUserName());
            
        } catch (IOException e) {
            Assert.fail();
        }
    }

}
