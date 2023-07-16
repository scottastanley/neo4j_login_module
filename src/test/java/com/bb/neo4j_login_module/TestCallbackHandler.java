/* 
 * Copyright 2023 Scott Alan Stanley
 */
package com.bb.neo4j_login_module;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.eclipse.jetty.jaas.callback.ObjectCallback;
import org.junit.Assert;

public class TestCallbackHandler implements CallbackHandler {
    private final String m_username;
    private final Object m_password;
    private Boolean m_failHandle = false;
    
    TestCallbackHandler(final String username, final Object password) {
        m_username = username;
        m_password = password;
    }
    
    void setFailHandle() {
        m_failHandle = true;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        if (m_failHandle) 
            throw new IOException("Forced failure");
        
        Assert.assertEquals("Unexpected number of callbacks", 2, callbacks.length);
        Assert.assertTrue("1st callback should be a NameCallback", NameCallback.class.isInstance(callbacks[0]));
        Assert.assertTrue("2nd callback should be a ObjectCallback", ObjectCallback.class.isInstance(callbacks[1]));
        
        NameCallback nc = new NameCallback("TestUsername: ");
        nc.setName(m_username);
        callbacks[0] = nc;
        
        ObjectCallback oc = new ObjectCallback();
        oc.setObject(m_password);
        callbacks[1] = oc;
    }
}
