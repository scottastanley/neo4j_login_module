package com.bb.neo4j_login_module;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.jaas.JAASRole;
import org.eclipse.jetty.security.UserPrincipal;
import org.eclipse.jetty.util.security.Credential;

class Neo4jUser {
    private final UserPrincipal m_principal;
    private final List<JAASRole> m_roles;
    
    
    Neo4jUser(final String neo4jUsername, final String neo4jCredential, 
              final List<String> neo4jRoles) {
        Credential credential = Credential.getCredential(neo4jCredential);
        m_principal = new UserPrincipal(neo4jUsername, credential);
        m_roles = new ArrayList<JAASRole>();
        
        for (String role : neo4jRoles) {
            m_roles.add(new JAASRole(role));
        }
    }
    
    UserPrincipal getUserPrincipal() {
        return m_principal;
    }
    
    List<JAASRole> getRoles() {
        return m_roles;
    }
}
