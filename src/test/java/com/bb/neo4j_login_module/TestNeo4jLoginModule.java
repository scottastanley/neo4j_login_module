/* 
 * Copyright 2023 Scott Alan Stanley
 */
package com.bb.neo4j_login_module;

public class TestNeo4jLoginModule extends Neo4jLoginModule {
    private boolean m_failCommit = false;
    private boolean m_failAbort = false;
    private boolean m_failLogout = false;

    void setFailCommit(final boolean failCommit) {
        m_failCommit = failCommit;
    }

    @Override
    void setIsCommitted(Boolean isCommitted) {
        super.setIsCommitted(isCommitted);
        
        if (m_failCommit && isCommitted) {
            throw new RuntimeException("forced failure");
        }
    }

    void setFailAbort(final boolean failAbort) {
        m_failAbort = failAbort;
    }

    @Override
    void setIsAborted(Boolean isAborted) {
        super.setIsAborted(isAborted);
        
        if (m_failAbort && isAborted) {
            throw new RuntimeException("forced failure");
        }
    }

    void setFailLogout(boolean failLogout) {
        m_failLogout = failLogout;
    }

    @Override
    void setIsLoggedOut(Boolean isLoggedOut) {
        super.setIsLoggedOut(isLoggedOut);
        
        if (m_failLogout && isLoggedOut) {
            throw new RuntimeException("forced failure");
        }
    }
    
}
