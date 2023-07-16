/* 
 * Copyright 2023 Scott Alan Stanley
 */
package com.bb.neo4j_login_module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jetty.jaas.JAASRole;
import org.eclipse.jetty.security.UserPrincipal;
import org.junit.Assert;
import org.junit.Test;

public class Neo4jUserTest {

    @Test
    public void testFoundUser() {
        String username = "testuser";
        String credential = "fadsfkjalkjdsf";
        List<String> rolesStr = Arrays.asList("users", "admin");
        
        List<JAASRole> expRoles = new ArrayList<JAASRole>();
        for (String roleStr : rolesStr) 
            expRoles.add(new JAASRole(roleStr));
        
        try {
            Neo4jUser user = new Neo4jUser(username, credential, rolesStr);
            
            Assert.assertNotNull("Null user", user);
            UserPrincipal userPrincipal = user.getUserPrincipal();
            Assert.assertNotNull("Null user principal", userPrincipal);
            Assert.assertEquals("Incorrect username in user principal", username, userPrincipal.getName());
            Assert.assertTrue("Invalid credentials in principal", userPrincipal.authenticate(credential));
            
            List<JAASRole> roles = user.getRoles();
            Assert.assertNotNull("Null roles", roles);
            Assert.assertEquals("Wrong number of roles", expRoles.size(), roles.size());
            for (JAASRole expRole : expRoles) {
                Assert.assertTrue("Missing role: " + expRole, roles.contains(expRole));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Assert.fail("Unexpected Exception: " + ex.getMessage());
        }
    }
}
