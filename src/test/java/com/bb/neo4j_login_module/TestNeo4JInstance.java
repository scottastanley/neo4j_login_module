/* 
 * Copyright 2023 Scott Alan Stanley
 */
package com.bb.neo4j_login_module;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.api.util.FileUtils;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TestNeo4JInstance {
    private static final Logger LOG = LoggerFactory.getLogger(TestNeo4JInstance.class);

    private static final String TEST_HOSTNAME = "localhost";
    private static final int TEST_PORT = 7689;
    private static final String SYSTEM_DATABASE = "system";
    private static final String TEST_DATABASE = "neo4j";
    private static final String TEST_USER = "foouser";
    private static final String TEST_PASSWORD = "fooUserPass";
    private static final String NEO4J_URI = "neo4j://" + TEST_HOSTNAME + ":" + TEST_PORT;

    private static final File m_workDir = new File("work");
    private static final File m_graphDbDir = new File(m_workDir, "test_graphDb");


    private static DatabaseManagementService m_neo4jDbMgmtSvc = null;
    private static Driver m_neo4jDriver = null;

    /**
     * Prepare and start the Neo4J instance.
     * @throws IOException 
     */
    public static void start() 
            throws IOException {
        prepareDbDir();
        
        DatabaseManagementServiceBuilder dbMgmtSvcBuilder = new DatabaseManagementServiceBuilder(m_graphDbDir.toPath());
        m_neo4jDbMgmtSvc = dbMgmtSvcBuilder
            .setConfig(BoltConnector.enabled, true)
            .setConfig(BoltConnector.listen_address, new SocketAddress(TEST_HOSTNAME, TEST_PORT))
            .setConfig(GraphDatabaseSettings.default_database, TEST_DATABASE)
            .setConfig(GraphDatabaseSettings.default_advertised_address, new SocketAddress(TEST_HOSTNAME))
            .setUserLogProvider(new Neo4JLogProvider())
            .build();
        
        configureUserPassword();
    }
    
    
    /**
     * Stop and cleanup the Neo4J instance.
     * @throws IOException 
     */
    public static void stop() 
            throws IOException {
        if (m_neo4jDriver != null) {
            try {
                m_neo4jDriver.close();
            } catch (RuntimeException ex) {
                LOG.error("Failed closing Neo4jDriver", ex);
            } finally {
                m_neo4jDriver = null;
            }
        }
        
        if (m_neo4jDbMgmtSvc != null) {
            try {
                m_neo4jDbMgmtSvc.shutdown();
            } catch (RuntimeException ex) {
                LOG.error("Failed during shutdown of Neo4J service", ex);
            } finally {
                m_neo4jDbMgmtSvc = null;
            }
            
            cleanDbDir();
        }        
    }
    
    public static void reset() {
        Driver d = getNeo4jDriver();
        
        // Delete all constraints
        try (Session s = d.session(); Transaction tx = s.beginTransaction()) {
            Result res = tx.run("SHOW ALL CONSTRAINTS");
            while (res.hasNext()) {
                String name = res.next().get("name").asString();
                tx.run("DROP CONSTRAINT " + name + " IF EXISTS");
            }
            tx.commit();
        }
        
        // Delete all indexes
        try (Session s = d.session(); Transaction tx = s.beginTransaction()) {
            Result res = tx.run("SHOW INDEXES");
            while (res.hasNext()) {
                String name = res.next().get("name").asString();
                tx.run("DROP INDEX " + name + " IF EXISTS");
            }
            tx.commit();
        }
        
        // Delete all Nodes and Relationships
        try (Session s = d.session(); Transaction tx = s.beginTransaction()) {
            tx.run("MATCH (n) DETACH DELETE n");
            tx.commit();
        }
    }
    
    /**
     * Get a Driver instance for the test database.
     * 
     * @return The driver
     */
    public static synchronized  Driver getNeo4jDriver() {
        if (m_neo4jDriver == null) {
            AuthToken auth = AuthTokens.basic(TEST_USER, TEST_PASSWORD);
            m_neo4jDriver = GraphDatabase.driver(NEO4J_URI, auth);
        }
        return m_neo4jDriver;
    }
    
    public static Neo4jParams getNeo4jParams() {
        return new Neo4jParams();
    }
    
    /**
     * Get an instance of the GraphDatabaseService in order to have direct access to the DB.
     * 
     * @return The GraphDatabaseService
     */
    private static GraphDatabaseService getGraphDatabaseService() {
        return m_neo4jDbMgmtSvc.database(SYSTEM_DATABASE);
    }
    
    /**
     * Clean the test graph database
     * @throws IOException 
     */
    private static void cleanDbDir() 
            throws IOException {
        if (m_workDir.exists()) {
            FileUtils.cleanDirectory(m_workDir);
        }
    }
    
    /**
     * Prepare the test graph database
     * @throws IOException 
     */
    private static void prepareDbDir() 
            throws IOException {
        cleanDbDir();
        m_graphDbDir.mkdirs();        
    }
    
    /**
     * Configure the password for the test user.
     */
    private static void configureUserPassword() {
        System.out.println("Configuring Neo4J User/Password");
        GraphDatabaseService svc = getGraphDatabaseService();
        Map<String,Object> params = new HashMap<String,Object>();
        params.put("username", TEST_USER);
        params.put("password", TEST_PASSWORD);
        svc.executeTransactionally("CREATE USER $username SET PLAINTEXT PASSWORD $password SET PASSWORD CHANGE NOT REQUIRED", params);
    }
    
    /**
     * 
     */
    static class Neo4jParams {
        public String m_neo4jUser = TEST_USER;
        public String m_neo4jPassword = TEST_PASSWORD;
        public String m_neo4jUri = NEO4J_URI;
    }
}
