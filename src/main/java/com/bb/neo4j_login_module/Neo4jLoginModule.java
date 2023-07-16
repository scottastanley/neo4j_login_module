/* 
 * Copyright 2023 Scott Alan Stanley
 */
package com.bb.neo4j_login_module;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.eclipse.jetty.jaas.callback.ObjectCallback;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Query;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.internal.types.InternalTypeSystem;
import org.neo4j.driver.types.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JAAS LoginModule for authenticating users against a Neo4J graph database. This LoginModule has
 * been developed to work with a Jetty server and matches the behavior of JDBCLoginModule provided
 * as part of the Jetty distribution.
 * 
 * 
 * Details for the expected behavior of a LoginModule is available 
 * here, 
 * https://docs.oracle.com/javase/10/security/java-authentication-and-authorization-service-jaas-loginmodule-developers-guide1.htm#JSSEC-GUID-CB46C30D-FFF1-466F-B2F5-6DE0BD5DA43A
 * and here,
 * https://docs.oracle.com/javase/8/docs/technotes/guides/security/jaas/JAASRefGuide.html
 * 
 * @author Scott Stanley
 */
public class Neo4jLoginModule 
        implements LoginModule {
    private static final Logger LOG = LoggerFactory.getLogger(Neo4jLoginModule.class);
    
    public static String NODE_TYPE = "nodeType";
    public static String USERNAME_PROP = "usernameProp";
    public static String CREDS_PROP = "credsProp";
    public static String ROLES_PROP = "rolesProp";
    public static String NEO4J_USER_PROP = "neo4jUser";
    public static String NEO4J_PASSWORD_PROP = "neo4jPassword";
    public static String NEO4J_URI_PROP = "neo4jUri";
    
    private String m_neo4jUser = null;
    private String m_neo4jPassword = null;
    private String m_neo4jUri = null;
    private String m_nodeType = null;
    private String m_usernameProp = null;
    private String m_credentialsProp = null;
    private String m_rolesProp = null;
    
    private CallbackHandler m_callbackHandler = null;
    private Subject m_subject = null;
    private Neo4jUser m_user = null;
    private Boolean m_isAuthenticated = null;
    private Boolean m_isCommitted = null;
    private Boolean m_isAborted = null;
    private Boolean m_isLoggedOut = null;

    
    /**
     * Initialize this login module.  Retrieve the configuration properties from the 
     * options and save the subject and callback handler.
     * 
     * @param subject
     * @param callbackHandler
     * @param sharedState
     * @param options
     */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, 
                           Map<String, ?> sharedState, Map<String, ?> options) {
        LOG.debug("initialize: " + subject + " options: " + options);

        this.m_callbackHandler = callbackHandler;
        this.m_subject = subject;
        
        m_nodeType = String.class.cast(options.get(NODE_TYPE));
        m_usernameProp = String.class.cast(options.get(USERNAME_PROP));
        m_credentialsProp = String.class.cast(options.get(CREDS_PROP));
        m_rolesProp = String.class.cast(options.get(ROLES_PROP));
        m_neo4jUser = String.class.cast(options.get(NEO4J_USER_PROP));
        m_neo4jPassword = String.class.cast(options.get(NEO4J_PASSWORD_PROP));
        m_neo4jUri = String.class.cast(options.get(NEO4J_URI_PROP));            
    }

    
    /**
     * Start the authentication process for the user.  
     * 
     * @return true if the authentication succeeded, or false if this LoginModule should be ignored.
     * @throws FailedLoginException if the provided credentials are not correct
     * @throws LoginException if the authentication fails for any other reason
     */
    @Override
    public boolean login() 
            throws LoginException {
        LOG.debug("login: ");
        
        setIsAuthenticated(false);
        
        //
        // get the username and password provided
        //
        UsernamePassword up = getUsernamePassword();

        if ((up.m_username == null) || (up.m_password == null)) {
            throw new FailedLoginException("Username or password undefined");
        }

        //
        // Get credentials from system and authenticate
        //
        m_user = getUser(up.m_username);
        if (m_user != null) {
            // If the user was found in the system, authenticate the credentials
            if (m_user.getUserPrincipal().authenticate(up.m_password)) {
                setIsAuthenticated(true);
            } else {
                throw new FailedLoginException("Failed to validate credentials");
            }
        } else {
            // Since we were unable to find the user/credentials in the 
            // store, we return false indicating we can not handle this
            // login attempt and should be ignored.  Some other LoginModule
            // may be able to handle this login attempt.
            return false;
        }
        
        return true;
    }
    
    /**
     * Abort the login process.  
     * 
     * @return true if this method succeeded, or false if this LoginModule should be ignored.
     * @throws LoginException if the abort fails
     */
    @Override
    public boolean abort() 
            throws LoginException {
        LOG.debug("abort: ");
        
        setIsAborted(false);
        
        // Clear any saved state in the Subject as well as locally
        if (m_user != null) {
            try {
                cleanPrincipals();
                setIsAborted(true);
            } catch (Throwable th) {
                LOG.error("Abort failed", th);
                setIsAborted(false);
            }
        }
        
        m_user = null;

        // Return the appropriate response
        if (isAuthenticated()) {
            if (isAborted()) {
                return true;
            } else {
                throw new LoginException("Abort failed");
            }
        } else {
            return false;
        }
    }

    /**
     * Commit the login process, finalizing the login for this LoginModule.
     * 
     * @return true if this method succeeded, or false if this LoginModule should be ignored.
     * @throws LoginException
     */
    @Override
    public boolean commit() 
            throws LoginException {
        LOG.debug("commit: ");
        
        //
        // Perform the expected behavior in the event of login success/failure
        // and set the commit status based on the results
        //
        if (! isAuthenticated()) {
            // If not logged in, clear all login state, set a false commit state
            // and return false
            m_user = null;
            setIsCommitted(false);
            
        } else {
            // If login was successful, apply the Principal and Roles, clear 
            // locally held state, set commit success and return true
            try {
                m_subject.getPrincipals().add(m_user.getUserPrincipal());
                m_subject.getPrincipals().addAll(m_user.getRoles());
                setIsCommitted(true);
            } catch (Throwable th) {
                // In the event of a failure trying to commit, throw an exception
                LOG.error("Failed to commit login", th);
                cleanPrincipals();
                setIsCommitted(false);
                throw new LoginException("Commit failed due to error");
            }
        }            

        return isAuthenticated() && isCommitted();
    }
    
    /**
     * Log the subject out, clearing all principals associated with this login module.
     * 
     * @return true if this method succeeded, or false if this LoginModule should be ignored.
     * @throws LoginException if the logout fails
     */
    @Override
    public boolean logout() 
            throws LoginException {
        LOG.debug("logout: ");
        
        if (! m_subject.isReadOnly()) {
            try {
                cleanPrincipals();
                setIsLoggedOut(true);
            } catch (Throwable th) {
                setIsLoggedOut(false);
                throw new LoginException("Failed to logout user");
            }
        } else {
            // Since the UserPrincipal implementation from Jetty does not
            // implement Destroyable, throw an exception
            throw new LoginException("Unable to destroy principal for read only subject");
        }
        
        return isLoggedOut();
    }
    
    /**
     * Clean up the principals from the subject.
     */
    private void cleanPrincipals() {
        m_subject.getPrincipals().remove(m_user.getUserPrincipal());
        m_subject.getPrincipals().removeAll(m_user.getRoles());
    }
    
    /**
     * Get the Neo4jUser for the given username.
     * 
     * @param username The username
     * @return The Neo4jUser
     */
    Neo4jUser getUser(final String username) 
            throws LoginException {
        Neo4jUser user = null;
        
        AuthToken token = AuthTokens.basic(m_neo4jUser, m_neo4jPassword);
        String query = "MATCH (n:%s {%s:$username}) RETURN n";
        Query neo4jQuery = new Query(String.format(query, m_nodeType, m_usernameProp));
        
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("username", username);

        
        try (Driver driver = GraphDatabase.driver(m_neo4jUri, token);
             Session sess = driver.session()) {
            
            Result res = sess.run(neo4jQuery.withParameters(params));
            
            if (res.hasNext()) {
                Record rec = res.next();
                Node n = rec.get(0).asNode();
                
                
                String nodeUsername = n.get(m_usernameProp).asString();
                String creds = n.get(m_credentialsProp).asString();
                Value rolesVal = n.get(m_rolesProp);
                
                List<String> roles = new ArrayList<String>();
                if (rolesVal.hasType(InternalTypeSystem.TYPE_SYSTEM.STRING())) {
                    roles.add(rolesVal.asString());
                } else if (rolesVal.hasType(InternalTypeSystem.TYPE_SYSTEM.LIST())) {
                    for (Object role : rolesVal.asList()) {
                     roles.add(String.class.cast(role));   
                    }
                } else {
                    throw new LoginException("Unexpected role value type, " + rolesVal.type());
                }
                
                user = new Neo4jUser(nodeUsername, creds, roles);
            }

        } catch (Throwable th) {
            LOG.error("Failed obtaining user", th);
            throw new LoginException("Failed obtaining user");
        }
        
        return user;
    }
    
    
    /**
     * Get the username and password provided for authentication.
     * 
     * @return 
     * @throws LoginException
     * @throws IOException
     * @throws UnsupportedCallbackException
     */
    private UsernamePassword getUsernamePassword() throws LoginException {
        if (m_callbackHandler == null)
            throw new LoginException("No callback handler defined");
        
        
        UsernamePassword up;
        try {
            Callback[] callbacks = new Callback[2];
            callbacks[0] = new NameCallback("Username: ");
            callbacks[1] = new ObjectCallback();
    
            m_callbackHandler.handle(callbacks);
            
            String username = ((NameCallback) callbacks[0]).getName();        
            Object password = ((ObjectCallback) callbacks[1]).getObject();
            up = new UsernamePassword(username, password);
        } catch (IOException | UnsupportedCallbackException e) {
            throw new LoginException("Login failed due to error: " + e.getMessage());
        }
        
        return up;
    }

    /**
     * Set the value for isAuthenticated.
     * 
     * @param isAuthenticated The value
     */
    void setIsAuthenticated(Boolean isAuthenticated) {
        m_isAuthenticated = isAuthenticated;
    }
    
    /**
     * Is the account authenticated
     * 
     * @return true if the account has been authenticated
     */
    private Boolean isAuthenticated() {
        return m_isAuthenticated;
    }

    /**
     * Set the value for isCommitted
     * 
     * @param isCommitted The value
     */
    void setIsCommitted(Boolean isCommitted) {
        m_isCommitted = isCommitted;
    }

    /**
     * Get the value for isCommitted
     * 
     * @return true if the account has been committed
     */
    private Boolean isCommitted() {
        return m_isCommitted;
    }

    /**
     * Set the value for isAborted
     * 
     * @param isAborted The value
     */
    void setIsAborted(final Boolean isAborted) {
        m_isAborted = isAborted;
    }

    /**
     * Get the value for isAborted
     * 
     * @return true if the account has been aborted
     */
    private Boolean isAborted() {
        return m_isAborted;
    }

    void setIsLoggedOut(final Boolean isLoggedOut) {
        m_isLoggedOut = isLoggedOut;
    }

    /**
     * Has the user been logged out successfully?
     * 
     * @return true if the account has been logged out
     */
    private Boolean isLoggedOut() {
        return m_isLoggedOut;
    }

    /**
     * A container class used to represent the username and password provided by the user.
     */
    class UsernamePassword {
        private String m_username = null;
        private Object m_password = null;
        
        UsernamePassword(final String username, final Object password) {
            m_username = username;
            m_password = password;
        }
    }
}
