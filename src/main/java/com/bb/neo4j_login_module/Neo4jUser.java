/* 
 * Copyright 2023 Scott Alan Stanley
 */
package com.bb.neo4j_login_module;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.jaas.JAASRole;
import org.eclipse.jetty.security.UserPrincipal;
import org.eclipse.jetty.util.security.Credential;

/**
 * Neo4jUser represents the user retrieved from a Neo4J graph database. 
 * 
 * @author Scott Stanley
 */
class Neo4jUser {
    private final UserPrincipal m_principal;
    private final List<JAASRole> m_roles;
    
    
    /**
     * Create a new Neo4jUser.
     * 
     * @param neo4jUsername The username from Neo4J
     * @param neo4jCredential The credentials field from Neo4J
     * @param neo4jRoles The roles retrieved from Neo4J
     */
    Neo4jUser(final String neo4jUsername, final String neo4jCredential, 
              final List<String> neo4jRoles) {
        Credential credential = Credential.getCredential(neo4jCredential);
        m_principal = new UserPrincipal(neo4jUsername, credential);
        m_roles = new ArrayList<JAASRole>();
        
        for (String role : neo4jRoles) {
            m_roles.add(new JAASRole(role));
        }
    }
    
    /**
     * Get the user principal for this user
     * 
     * @return The UserPrincipal based on the user in Neo4J
     */
    UserPrincipal getUserPrincipal() {
        return m_principal;
    }
    
    /**
     * Get the JAASRoles based on the information provided from Neo4J.
     * 
     * @return the JAASRole instances
     */
    List<JAASRole> getRoles() {
        return m_roles;
    }
}
