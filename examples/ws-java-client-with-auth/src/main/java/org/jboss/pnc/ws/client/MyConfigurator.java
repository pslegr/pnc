package org.jboss.pnc.ws.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.websocket.HandshakeResponse;
import javax.websocket.ClientEndpointConfig;

import org.apache.http.client.ClientProtocolException;
import org.jboss.pnc.ws.client.SimpleOAuthConnect;

/**
 * @author Arun Gupta
 * @author Pavel Slegr
 */
public class MyConfigurator extends ClientEndpointConfig.Configurator {
    
    private String authDomainServer;
    private String authApp;
    private String username;
    private String password;

    @Override
    public void beforeRequest(Map<String, List<String>> headers) {
        System.out.println(" >>> beforeRequest modified by pnc:");
        // add Authentication header
        // "Authorization", "Bearer " + provider.getTokenString()
        List<String> authHeader = new ArrayList<String>();
        authHeader.add("Bearer " + authenticate());
        headers.put("Authorization", authHeader);
        for (String h : headers.keySet()) {
            for (String k : headers.get(h)) {
                System.out.println("Header: " + h + ", " + k);
            }
        }
    }

    @Override
    public void afterResponse(HandshakeResponse response) {
        System.out.println("afterResponse:");
        for (String h : response.getHeaders().keySet()) {
            for (String k : response.getHeaders().get(h)) {
                System.out.println("Header: " + h + ", " + k);
            }
        }
    }
    
    private String authenticate() {
        try {
            // load authentication properties
            loadAuthenticationProps();
            // connect to Direct Access Grant service and obtain access_token for given credentials
            return SimpleOAuthConnect.getAccessToken(
                    this.authDomainServer,
                    this.authApp,
                    this.username,
                    this.password);
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    private void loadAuthenticationProps() throws Exception{
        this.authDomainServer = System.getenv("PNC_EXT_AUTH_DOMAIN_GRANT_ACCESS");
        this.authApp = System.getenv("PNC_EXT_OAUTH_APP");        
        this.username = System.getenv("PNC_EXT_OAUTH_USERNAME");
        this.password = System.getenv("PNC_EXT_OAUTH_PASSWORD");     
        
        if(this.authDomainServer == null || this.authDomainServer.equals("")) 
            throw new Exception("Could not load authentication property "
                    + "PNC_EXT_AUTH_DOMAIN_GRANT_ACCESS => Direct Access Grant server location");
        
        if(this.authApp == null || this.authApp.equals("")) 
            throw new Exception("Could not load authentication property "
                    + "PNC_EXT_OAUTH_APP => Direct Access Grant application name");
        
        if(this.username == null || this.username.equals("")) 
            throw new Exception("Could not load authentication property "
                    + "PNC_EXT_OAUTH_USERNAME => Direct Access Grant username");

        if(this.password == null || this.password.equals("")) 
            throw new Exception("Could not load authentication property "
                    + "PNC_EXT_OAUTH_PASSWORD => Direct Access Grant password");
        
    }

}
