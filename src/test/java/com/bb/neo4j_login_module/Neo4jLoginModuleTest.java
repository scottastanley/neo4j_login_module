package com.bb.neo4j_login_module;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.eclipse.jetty.jaas.JAASRole;
import org.eclipse.jetty.security.UserPrincipal;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

import com.bb.neo4j_login_module.TestNeo4JInstance.Neo4jParams;

public class Neo4jLoginModuleTest {
    private static String NODE_TYPE = "CredentialsNode";
    private static String USERNAME_FIELD = "username";
    private static String CREDS_FIELD = "creds";
    private static String ROLES_FIELD = "roles";
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        try {
            TestNeo4JInstance.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        try {
            TestNeo4JInstance.stop();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        try {
            TestNeo4JInstance.reset();
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
    
    private Map<String,?> getOptions(final String nodeType, final String usernameProp, 
                                     final String credsProp, final String rolesProp) {
        Map<String,String> options = new HashMap<String,String>();
        options.put(Neo4jLoginModule.NODE_TYPE, nodeType);
        options.put(Neo4jLoginModule.USERNAME_PROP, usernameProp);
        options.put(Neo4jLoginModule.CREDS_PROP, credsProp);
        options.put(Neo4jLoginModule.ROLES_PROP, rolesProp);
        
        Neo4jParams neo4jParams = TestNeo4JInstance.getNeo4jParams();
        options.put(Neo4jLoginModule.NEO4J_USER_PROP, neo4jParams.m_neo4jUser);
        options.put(Neo4jLoginModule.NEO4J_PASSWORD_PROP, neo4jParams.m_neo4jPassword);
        options.put(Neo4jLoginModule.NEO4J_URI_PROP, neo4jParams.m_neo4jUri);
        return options;
    }
    
    private void createTestNeo4jNode(final String nodeType,
                                     final String usernameField, final String username, 
                                     final String credsField, final Object password,
                                     final String rolesField, final String[] roles) {
        
        Assert.assertTrue("Username is undefined", username != null);
        Assert.assertTrue("Password is undefined", password != null);
        Assert.assertTrue("Roles are undefined", roles != null && roles.length > 0);
        
        Transaction tx = null;
        
        Driver drv = TestNeo4JInstance.getNeo4jDriver();
        try (Session sess = drv.session()) {
            tx = sess.beginTransaction();
            
            String queryStr = "CREATE (n:%s {%s: $username, %s:$creds, %s:$roles})";
            Query query = new Query(String.format(queryStr, NODE_TYPE, USERNAME_FIELD, 
                                                  CREDS_FIELD, ROLES_FIELD));
            Map<String,Object> params = new HashMap<String,Object>();
            params.put("username", username);
            params.put("creds", password);
            
            if (roles.length == 1) {
                params.put("roles", roles[0]);
                tx.run(query.withParameters(params));
            } else {
                params.put("roles", roles);
                tx.run(query.withParameters(params));
            }
            
            tx.commit();
        } catch (Throwable th) {
            th.printStackTrace();
            tx.rollback();
        } finally {
            if (tx != null) 
                tx.close();
        }
    }
//    
//    private Neo4jUser getMockNeo4jUser(final boolean userFound, final boolean authenticatePasses) {
//        UserPrincipal userPrincipal = mock(UserPrincipal.class);
//        when(userPrincipal.authenticate(anyString())).thenReturn(authenticatePasses);
//        
//        Neo4jUser neo4jUser = mock(Neo4jUser.class);
//        
//        when(neo4jUser.foundUser()).thenReturn(userFound);
//        if (userFound) {
//            when(neo4jUser.getUserPrincipal()).thenReturn(userPrincipal);
//            
//            
//            List<JAASRole> roles = new ArrayList<JAASRole>();
//            roles.add(new JAASRole("user"));
//            roles.add(new JAASRole("special_user"));
//            when(neo4jUser.getRoles()).thenReturn(roles);
//        }
//        
//        return neo4jUser;
//    }
    
    /**
     * Verify that the provided SUbject contains the UserPrincipal with the givne username.
     * 
     * @param subject
     * @param username
     * @return
     */
    private boolean hasUserPrincipal(final Subject subject, final String username) {
        for (Principal o : subject.getPrincipals()) {
            if (UserPrincipal.class.isInstance(o)) {
                if (o.getName().equals(username)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean hasJAASRoles(final Subject subject, final String[] roles) {
        Set<String> remainingRoles = new HashSet<String>();
        remainingRoles.addAll(Arrays.asList(roles));
        
        for (Principal o : subject.getPrincipals()) {
            if (JAASRole.class.isInstance(o)) {
                if (remainingRoles.contains(o.getName())) {
                    remainingRoles.remove(o.getName());
                }
            }
            
            if (remainingRoles.size() == 0) {
                return true;
            }
        }
        
        return false;
        
    }
    
    private TestNeo4jLoginModule getLoginModule(final boolean failCommit,
                                                final boolean failAbort, final boolean failLogout) {
        TestNeo4jLoginModule mod = new TestNeo4jLoginModule();
        mod.setFailCommit(failCommit);
        mod.setFailAbort(failAbort);
        mod.setFailLogout(failLogout);
        
        return mod;
    }

    @Test
    public void testLogin_UserNotFound() {
        String nodeType = NODE_TYPE;
        String usernameProp = USERNAME_FIELD;
        String username = "testuser3";
        String credsProp = CREDS_FIELD;
        Object password = "password3";
        String rolesProp = ROLES_FIELD;

        CallbackHandler handler = new TestCallbackHandler(username + "ZZZZ", password);
        Map<String,?> sharedState = new HashMap<String,Object>();
        Map<String,?> options = getOptions(nodeType, usernameProp, credsProp, rolesProp);

        boolean expLoginResponse = false;
        boolean expCommitResponse = false;
        
        try {
            TestNeo4jLoginModule mod = getLoginModule(false, false, false);
            
            Subject subject = new Subject();
            mod.initialize(subject, handler, sharedState, options);
            
            Assert.assertEquals("Invalid login response", expLoginResponse, mod.login());
            Assert.assertEquals("Invalid commit response", expCommitResponse, mod.commit());
            Assert.assertTrue("Subject should have no Principals", subject.getPrincipals().size() == 0);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected Exception: " + ex.getMessage());
        }
    }

    @Test
    public void testLogin_NoUsername() {
        String username = null;
        Object password = "password3";
        CallbackHandler handler = new TestCallbackHandler(username, password);
        Map<String,?> sharedState = new HashMap<String,Object>();
        Map<String,?> options = new HashMap<String,String>();

        boolean expCommitResponse = false;
        
        try {
            TestNeo4jLoginModule mod = getLoginModule(false, false, false);
            
            Subject subject = new Subject();
            mod.initialize(subject, handler, sharedState, options);
            
            try {
                mod.login();
                Assert.fail("Should have thrown an exception");
            } catch (FailedLoginException le) {
                // Ignore expected exception
            }
            
            Assert.assertEquals("Invalid commit response", expCommitResponse, mod.commit());
            Assert.assertTrue("Subject should have no Principals", subject.getPrincipals().size() == 0);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected Exception: " + ex.getMessage());
        }
    }

    @Test
    public void testLogin_NoPassword() {
        String username = "testuser3";
        Object password = null;
        CallbackHandler handler = new TestCallbackHandler(username, password);
        Map<String,?> sharedState = new HashMap<String,Object>();
        Map<String,?> options = new HashMap<String,String>();

        boolean expCommitResponse = false;
        
        try {
            TestNeo4jLoginModule mod = getLoginModule(false, false, false);
            
            Subject subject = new Subject();
            mod.initialize(subject, handler, sharedState, options);
            
            try {
                mod.login();
                Assert.fail("Should have thrown an exception");
            } catch (FailedLoginException le) {
                // Ignore expected exception
            }
            
            Assert.assertEquals("Invalid commit response", expCommitResponse, mod.commit());
            Assert.assertTrue("Subject should have no Principals", subject.getPrincipals().size() == 0);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected Exception: " + ex.getMessage());
        }
    }

    @Test
    public void testLogin_AuthenticateFail() {
        String nodeType = NODE_TYPE;
        String usernameProp = USERNAME_FIELD;
        String username = "testuser3";
        String credsProp = CREDS_FIELD;
        Object password = "password3";
        String rolesProp = ROLES_FIELD;
        String[] roles = {"user"};
        
        CallbackHandler handler = new TestCallbackHandler(username, password + "ZZZZ");
        Map<String,?> sharedState = new HashMap<String,Object>();
        Map<String,?> options = getOptions(nodeType, usernameProp, credsProp, rolesProp);

        boolean expCommitResponse = false;
        
        try {
            createTestNeo4jNode(nodeType, usernameProp, username, 
                                credsProp, password, rolesProp, roles);

            TestNeo4jLoginModule mod = getLoginModule(false, false, false);
            
            Subject subject = new Subject();
            mod.initialize(subject, handler, sharedState, options);
            
            try {
                mod.login();
                Assert.fail("Should have thrown an exception");
            } catch (FailedLoginException le) {
                // Ignore expected exception
            }
            
            Assert.assertEquals("Invalid commit response", expCommitResponse, mod.commit());
            Assert.assertTrue("Subject should have no Principals", subject.getPrincipals().size() == 0);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected Exception: " + ex.getMessage());
        }
    }

    @Test
    public void testLogin_CallbackHandlerFails() {
        String username = "testuser3";
        Object password = "password3";
        TestCallbackHandler handler = new TestCallbackHandler(username, password);
        
        Map<String,?> sharedState = new HashMap<String,Object>();
        Map<String,?> options = new HashMap<String,String>();

        boolean expCommitResponse = false;
        
        try {
            TestNeo4jLoginModule mod = getLoginModule(false, false, false);
            
            Subject subject = new Subject();
            mod.initialize(subject, handler, sharedState, options);
            
            try {
                handler.setFailHandle();
                mod.login();
                Assert.fail("Should have thrown an exception");
            } catch (LoginException le) {
                // Ignore expected exception
            }
            
            Assert.assertEquals("Invalid commit response", expCommitResponse, mod.commit());
            Assert.assertTrue("Subject should have no Principals", subject.getPrincipals().size() == 0);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected Exception: " + ex.getMessage());
        }
    }

    @Test
    public void testLogin_NullCallbackHandler() {
        Map<String,?> sharedState = new HashMap<String,Object>();
        Map<String,?> options = new HashMap<String,String>();

        boolean expCommitResponse = false;
        
        try {
            TestNeo4jLoginModule mod = getLoginModule(false, false, false);
            
            Subject subject = new Subject();
            mod.initialize(subject, null, sharedState, options);
            
            try {
                mod.login();
                Assert.fail("Should have thrown an exception");
            } catch (LoginException le) {
                // Ignore expected exception
            }
            
            Assert.assertEquals("Invalid commit response", expCommitResponse, mod.commit());
            Assert.assertTrue("Subject should have no Principals", subject.getPrincipals().size() == 0);
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected Exception: " + ex.getMessage());
        }
    }
    
    @Test
    public void test_LoginSuccessCommitSuccessAbortSuccess() {
        String nodeType = NODE_TYPE;
        String usernameProp = USERNAME_FIELD;
        String username = "testuser3";
        String credsProp = CREDS_FIELD;
        Object password = "password3";
        String rolesProp = ROLES_FIELD;
        String[] roles = {"user", "admin"};
        
        CallbackHandler handler = new TestCallbackHandler(username, password);
        Map<String,?> sharedState = new HashMap<String,Object>();
        Map<String,?> options = getOptions(nodeType, usernameProp, credsProp, rolesProp);

        boolean expLoginResponse = true;
        boolean expCommitResponse = true;
        boolean expAbortResponse = true;
        
        try {
            createTestNeo4jNode(nodeType, usernameProp, username, 
                                credsProp, password, rolesProp, roles);
            TestNeo4jLoginModule mod = getLoginModule(false, false, false);
            
            Subject subject = new Subject();
            mod.initialize(subject, handler, sharedState, options);
            
            // Login
            Assert.assertEquals("Invalid login response", expLoginResponse, mod.login());
            
            // Commit
            Assert.assertEquals("Invalid commit response", expCommitResponse, mod.commit());
            Assert.assertTrue("Subject should contain Principal", hasUserPrincipal(subject, username));
            
            Assert.assertTrue("Subject should contain Roles", hasJAASRoles(subject, roles));            
            
            // Abort
            Assert.assertEquals("Invalid abort response", expAbortResponse, mod.abort());
            Assert.assertFalse("Subject should not contain Principal", hasUserPrincipal(subject, username));            
            Assert.assertFalse("Subject should not contain Roles", hasJAASRoles(subject, roles));            
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected Exception: " + ex.getMessage());
        }
    }

    @Test
    public void test_LoginSuccessCommitFailureAbortSuccess() {
        String nodeType = NODE_TYPE;
        String usernameProp = USERNAME_FIELD;
        String username = "testuser3";
        String credsProp = CREDS_FIELD;
        Object password = "password3";
        String rolesProp = ROLES_FIELD;
        String[] roles = {"user"};

        CallbackHandler handler = new TestCallbackHandler(username, password);
        Map<String,?> sharedState = new HashMap<String,Object>();
        Map<String,?> options = getOptions(nodeType, usernameProp, credsProp, rolesProp);

        boolean expLoginResponse = true;
        boolean expAbortResponse = true;
        
        try {
            createTestNeo4jNode(nodeType, usernameProp, username, 
                                credsProp, password, rolesProp, roles);

            TestNeo4jLoginModule mod = getLoginModule(true, false, false);
            
            Subject subject = new Subject();
            mod.initialize(subject, handler, sharedState, options);
            
            // Login
            Assert.assertEquals("Invalid login response", expLoginResponse, mod.login());
            
            // Commit
            try {
                mod.commit();
                Assert.fail("Should have thrown an exception");
            } catch (LoginException le) {
                // Ignore expected exception
            }
            Assert.assertFalse("Subject should not contain Principal", hasUserPrincipal(subject, username));
            Assert.assertFalse("Subject should not contain Roles", hasJAASRoles(subject, roles));            
            
            // Abort
            Assert.assertEquals("Invalid abort response", expAbortResponse, mod.abort());            
            Assert.assertFalse("Subject should not contain Principal", hasUserPrincipal(subject, username));
            Assert.assertFalse("Subject should not contain Roles", hasJAASRoles(subject, roles));            
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected Exception: " + ex.getMessage());
        }
    }

    @Test
    public void test_LoginFailCommitSuccessAbortSuccess() {
        String nodeType = NODE_TYPE;
        String usernameProp = USERNAME_FIELD;
        String username = "testuser3";
        String credsProp = CREDS_FIELD;
        Object password = "password3";
        String rolesProp = ROLES_FIELD;
        String[] roles = {"user"};

        CallbackHandler handler = new TestCallbackHandler(username, password + "ZZZZ");
        Map<String,?> sharedState = new HashMap<String,Object>();
        Map<String,?> options = getOptions(nodeType, usernameProp, credsProp, rolesProp);

        boolean expCommitResponse = false;
        boolean expAbortResponse = false;
        
        try {
            createTestNeo4jNode(nodeType, usernameProp, username, 
                                credsProp, password, rolesProp, roles);

            TestNeo4jLoginModule mod = getLoginModule(false, false, false);
            
            Subject subject = new Subject();
            mod.initialize(subject, handler, sharedState, options);
                        
            // Login
            try {
                mod.login();
                Assert.fail("Should have thrown an exception");
            } catch (FailedLoginException le) {
                // Ignore expected exception
            }
            
            // Commit
            Assert.assertEquals("Invalid commit response", expCommitResponse, mod.commit());
            
            Assert.assertFalse("Subject should not contain Principal, post commit", hasUserPrincipal(subject, username));
            Assert.assertFalse("Subject should not contain Roles, post commit", hasJAASRoles(subject, roles));            

            // Abort
            Assert.assertEquals("Invalid abort response", expAbortResponse, mod.abort());            
            Assert.assertFalse("Subject should not contain Principal, post abort", hasUserPrincipal(subject, username));
            Assert.assertFalse("Subject should not contain Roles, post abort", hasJAASRoles(subject, roles));            
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected Exception: " + ex.getMessage());
        }
    }

    @Test
    public void test_LoginFailCommitFailAbortSuccess() {
        String nodeType = NODE_TYPE;
        String usernameProp = USERNAME_FIELD;
        String username = "testuser3";
        String credsProp = CREDS_FIELD;
        Object password = "password3";
        String rolesProp = ROLES_FIELD;
        String[] roles = {"user"};

        CallbackHandler handler = new TestCallbackHandler(username, password + "ZZZZ");
        Map<String,?> sharedState = new HashMap<String,Object>();
        Map<String,?> options = getOptions(nodeType, usernameProp, credsProp, rolesProp);

        boolean expCommitResponse = false;
        boolean expAbortResponse = false;
        
        try {
            createTestNeo4jNode(nodeType, usernameProp, username, 
                                credsProp, password, rolesProp, roles);

            TestNeo4jLoginModule mod = getLoginModule(true, false, false);
            
            Subject subject = new Subject();
            mod.initialize(subject, handler, sharedState, options);
            
            // Login
            try {
                mod.login();
                Assert.fail("Should have thrown an exception");
            } catch (FailedLoginException le) {
                // Ignore expected exception
            }
            
            // Commit
            Assert.assertEquals("Invalid commit response", expCommitResponse, mod.commit());
            
            Assert.assertFalse("Subject should not contain Principal, post commit", hasUserPrincipal(subject, username));
            Assert.assertFalse("Subject should not contain Roles, post commit", hasJAASRoles(subject, roles));            

            // Abort
            Assert.assertEquals("Invalid abort response", expAbortResponse, mod.abort());            
            Assert.assertFalse("Subject should not contain Principal, post abort", hasUserPrincipal(subject, username));
            Assert.assertFalse("Subject should not contain Roles, post abort", hasJAASRoles(subject, roles));            
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected Exception: " + ex.getMessage());
        }
    }
    
    
    @Test
    public void test_LoginSuccessCommitSuccessAbortFail() {
        String nodeType = NODE_TYPE;
        String usernameProp = USERNAME_FIELD;
        String username = "testuser3";
        String credsProp = CREDS_FIELD;
        Object password = "password3";
        String rolesProp = ROLES_FIELD;
        String[] roles = {"user"};

        CallbackHandler handler = new TestCallbackHandler(username, password);
        Map<String,?> sharedState = new HashMap<String,Object>();
        Map<String,?> options = getOptions(nodeType, usernameProp, credsProp, rolesProp);

        boolean expLoginResponse = true;
        boolean expCommitResponse = true;
        
        try {
            createTestNeo4jNode(nodeType, usernameProp, username, 
                                credsProp, password, rolesProp, roles);

            TestNeo4jLoginModule mod = getLoginModule(false, true, false);
            
            Subject subject = new Subject();
            mod.initialize(subject, handler, sharedState, options);
            
            // Login
            Assert.assertEquals("Invalid login response", expLoginResponse, mod.login());
            
            // Commit
            Assert.assertEquals("Invalid commit response", expCommitResponse, mod.commit());
            
            Assert.assertTrue("Subject should contain Principal, post commit", hasUserPrincipal(subject, username));
            Assert.assertTrue("Subject should contain Roles, post commit", hasJAASRoles(subject, roles));            
            
            // Abort
            try {
                mod.abort();
                Assert.fail("Should have thrown an exception");
            } catch (LoginException le) {
                // Ignore expected exception
            }
            Assert.assertFalse("Subject should not contain Principal, post abort", hasUserPrincipal(subject, username));
            Assert.assertFalse("Subject should not contain Roles, post abort", hasJAASRoles(subject, roles));            
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected Exception: " + ex.getMessage());
        }
    }

    @Test
    public void test_LoginSuccessCommitFailureAbortFail() {
        String nodeType = NODE_TYPE;
        String usernameProp = USERNAME_FIELD;
        String username = "testuser3";
        String credsProp = CREDS_FIELD;
        Object password = "password3";
        String rolesProp = ROLES_FIELD;
        String[] roles = {"user"};

        CallbackHandler handler = new TestCallbackHandler(username, password);
        Map<String,?> sharedState = new HashMap<String,Object>();
        Map<String,?> options = getOptions(nodeType, usernameProp, credsProp, rolesProp);

        boolean expLoginResponse = true;
        
        try {
            createTestNeo4jNode(nodeType, usernameProp, username, 
                                credsProp, password, rolesProp, roles);

            TestNeo4jLoginModule mod = getLoginModule(true, true, false);
            
            Subject subject = new Subject();
            mod.initialize(subject, handler, sharedState, options);
            
            // Login
            Assert.assertEquals("Invalid login response", expLoginResponse, mod.login());
            
            // Commit
            try {
                mod.commit();
            } catch (LoginException le) {
                // Ignore expected exception
            }
            
            Assert.assertFalse("Subject should not contain Principal, post commit", hasUserPrincipal(subject, username));
            Assert.assertFalse("Subject should not contain Roles, post commit", hasJAASRoles(subject, roles));            
            
            // Abort
            try {
                mod.abort();
                Assert.fail("Should have thrown an exception");
            } catch (LoginException le) {
                // Ignore expected exception
            }
            Assert.assertFalse("Subject should not contain Principal, post abort", hasUserPrincipal(subject, username));
            Assert.assertFalse("Subject should not contain Roles, post abort", hasJAASRoles(subject, roles));            
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected Exception: " + ex.getMessage());
        }
    }

    @Test
    public void test_LoginFailCommitSuccessAbortFail() {
        String nodeType = NODE_TYPE;
        String usernameProp = USERNAME_FIELD;
        String username = "testuser3";
        String credsProp = CREDS_FIELD;
        Object password = "password3";
        String rolesProp = ROLES_FIELD;
        String[] roles = {"user"};

        CallbackHandler handler = new TestCallbackHandler(username, password + "ZZZZ");
        Map<String,?> sharedState = new HashMap<String,Object>();
        Map<String,?> options = getOptions(nodeType, usernameProp, credsProp, rolesProp);

        boolean expCommitResponse = false;
        boolean expAbortResponse = false;
        
        try {
            createTestNeo4jNode(nodeType, usernameProp, username, 
                                credsProp, password, rolesProp, roles);
            
            TestNeo4jLoginModule mod = getLoginModule(false, true, false);
            
            Subject subject = new Subject();
            mod.initialize(subject, handler, sharedState, options);
                        
            // Login
            try {
                mod.login();
                Assert.fail("Should have thrown an exception");
            } catch (FailedLoginException le) {
                // Ignore expected exception
            }
            
            // Commit
            Assert.assertEquals("Invalid commit response", expCommitResponse, mod.commit());
            
            Assert.assertFalse("Subject should not contain Principal, post commit", hasUserPrincipal(subject, username));
            Assert.assertFalse("Subject should not contain Roles, post commit", hasJAASRoles(subject, roles));            

            // Abort
            Assert.assertEquals("Invalid abort response", expAbortResponse, mod.abort());            
            Assert.assertFalse("Subject should not contain Principal, post abort", hasUserPrincipal(subject, username));
            Assert.assertFalse("Subject should not contain Roles, post abort", hasJAASRoles(subject, roles));            
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected Exception: " + ex.getMessage());
        }
    }

    @Test
    public void test_LoginFailCommitFailAbortFail() {
        String nodeType = NODE_TYPE;
        String usernameProp = USERNAME_FIELD;
        String username = "testuser3";
        String credsProp = CREDS_FIELD;
        Object password = "password3";
        String rolesProp = ROLES_FIELD;
        String[] roles = {"user"};

        CallbackHandler handler = new TestCallbackHandler(username, password + "ZZZZ");
        Map<String,?> sharedState = new HashMap<String,Object>();
        Map<String,?> options = getOptions(nodeType, usernameProp, credsProp, rolesProp);

        boolean expCommitResponse = false;
        boolean expAbortResponse = false;
        
        try {
            createTestNeo4jNode(nodeType, usernameProp, username, 
                                credsProp, password, rolesProp, roles);
            
            TestNeo4jLoginModule mod = getLoginModule(true, true, false);
            
            Subject subject = new Subject();
            mod.initialize(subject, handler, sharedState, options);
            
            // Login
            try {
                mod.login();
                Assert.fail("Should have thrown an exception");
            } catch (FailedLoginException le) {
                // Ignore expected exception
            }
            
            // Commit
            Assert.assertEquals("Invalid commit response", expCommitResponse, mod.commit());
            
            Assert.assertFalse("Subject should not contain Principal, post commit", hasUserPrincipal(subject, username));
            Assert.assertFalse("Subject should not contain Roles, post commit", hasJAASRoles(subject, roles));            

            // Abort
            Assert.assertEquals("Invalid abort response", expAbortResponse, mod.abort());            
            Assert.assertFalse("Subject should not contain Principal, post abort", hasUserPrincipal(subject, username));
            Assert.assertFalse("Subject should not contain Roles, post abort", hasJAASRoles(subject, roles));            
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected Exception: " + ex.getMessage());
        }
    }
    
    @Test
    public void test_LoginSuccessCommitSuccessLogoutSuccess() {
        String nodeType = NODE_TYPE;
        String usernameProp = USERNAME_FIELD;
        String username = "testuser3";
        String credsProp = CREDS_FIELD;
        Object password = "password3";
        String rolesProp = ROLES_FIELD;
        String[] roles = {"user"};

        CallbackHandler handler = new TestCallbackHandler(username, password);
        Map<String,?> sharedState = new HashMap<String,Object>();
        Map<String,?> options = getOptions(nodeType, usernameProp, credsProp, rolesProp);

        boolean expLoginResponse = true;
        boolean expCommitResponse = true;
        boolean expLogoutResponse = true;
        
        try {
            createTestNeo4jNode(nodeType, usernameProp, username, 
                                credsProp, password, rolesProp, roles);

            TestNeo4jLoginModule mod = getLoginModule(false, false, false);
            
            Subject subject = new Subject();
            mod.initialize(subject, handler, sharedState, options);
            
            // Login
            Assert.assertEquals("Invalid login response", expLoginResponse, mod.login());
            
            // Commit
            Assert.assertEquals("Invalid commit response", expCommitResponse, mod.commit());
                        
            Assert.assertTrue("Subject should contain Principal, post commit", hasUserPrincipal(subject, username));
            Assert.assertTrue("Subject should contain Roles, post commit", hasJAASRoles(subject, roles));            
            
            // Logout
            Assert.assertEquals("Invalid logout response", expLogoutResponse, mod.logout());            
            Assert.assertFalse("Subject should not contain Principal, post logout", hasUserPrincipal(subject, username));
            Assert.assertFalse("Subject should not contain Roles, post logout", hasJAASRoles(subject, roles));            
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected Exception: " + ex.getMessage());
        }
    }
    
    @Test
    public void test_LoginSuccessCommitSuccessLogoutFail() {
        String nodeType = NODE_TYPE;
        String usernameProp = USERNAME_FIELD;
        String username = "testuser3";
        String credsProp = CREDS_FIELD;
        Object password = "password3";
        String rolesProp = ROLES_FIELD;
        String[] roles = {"user"};

        CallbackHandler handler = new TestCallbackHandler(username, password);
        Map<String,?> sharedState = new HashMap<String,Object>();
        Map<String,?> options = getOptions(nodeType, usernameProp, credsProp, rolesProp);

        boolean expLoginResponse = true;
        boolean expCommitResponse = true;
        
        try {
            createTestNeo4jNode(nodeType, usernameProp, username, 
                                credsProp, password, rolesProp, roles);

            TestNeo4jLoginModule mod = getLoginModule(false, false, true);
            
            Subject subject = new Subject();
            mod.initialize(subject, handler, sharedState, options);
            
            // Login
            Assert.assertEquals("Invalid login response", expLoginResponse, mod.login());
            
            // Commit
            Assert.assertEquals("Invalid commit response", expCommitResponse, mod.commit());
            
            Assert.assertTrue("Subject should contain Principal, post commit", hasUserPrincipal(subject, username));
            Assert.assertTrue("Subject should contain Roles, post commit", hasJAASRoles(subject, roles));            
            
            // Abort
            try {
                mod.logout();
                Assert.fail("Should have thrown an exception");
            } catch (LoginException le) {
                // Ignore expected exception
            }
            Assert.assertFalse("Subject should not contain Principal, post logout", hasUserPrincipal(subject, username));
            Assert.assertFalse("Subject should not contain Roles, post logout", hasJAASRoles(subject, roles));            
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected Exception: " + ex.getMessage());
        }
    }
    
    @Test
    public void test_LogoutReadOnlySubject() {
        String nodeType = NODE_TYPE;
        String usernameProp = USERNAME_FIELD;
        String username = "testuser3";
        String credsProp = CREDS_FIELD;
        Object password = "password3";
        String rolesProp = ROLES_FIELD;
        String[] roles = {"user"};

        CallbackHandler handler = new TestCallbackHandler(username, password);
        Map<String,?> sharedState = new HashMap<String,Object>();
        Map<String,?> options = getOptions(nodeType, usernameProp, credsProp, rolesProp);

        boolean expLoginResponse = true;
        boolean expCommitResponse = true;
        
        try {
            createTestNeo4jNode(nodeType, usernameProp, username, 
                                credsProp, password, rolesProp, roles);

            TestNeo4jLoginModule mod = getLoginModule(false, false, true);
            
            Subject subject = new Subject();
            mod.initialize(subject, handler, sharedState, options);
            
            // Login
            Assert.assertEquals("Invalid login response", expLoginResponse, mod.login());
            
            // Commit
            Assert.assertEquals("Invalid commit response", expCommitResponse, mod.commit());
            
            Assert.assertTrue("Subject should contain Principal, post commit", hasUserPrincipal(subject, username));
            Assert.assertTrue("Subject should contain Roles, post commit", hasJAASRoles(subject, roles));            
            
            // Abort
            subject.setReadOnly();
            try {
                mod.logout();
                Assert.fail("Should have thrown an exception");
            } catch (LoginException le) {
                // Ignore expected exception
            }
            Assert.assertTrue("Subject should contain Principal, post logout", hasUserPrincipal(subject, username));
            Assert.assertTrue("Subject should contain Roles, post logout", hasJAASRoles(subject, roles));            
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected Exception: " + ex.getMessage());
        }
    }
}
