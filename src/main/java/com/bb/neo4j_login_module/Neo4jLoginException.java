package com.bb.neo4j_login_module;

import javax.security.auth.login.LoginException;

public class Neo4jLoginException 
        extends LoginException {
    private static final long serialVersionUID = 638898727043559218L;



    public Neo4jLoginException(String msg) {
        super(msg);
    }
    
    public Neo4jLoginException(String msg, final Throwable reason) {
        super(msg);
        
        this.initCause(reason);
    }
}
