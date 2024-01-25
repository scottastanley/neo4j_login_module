# Neo4J Login Module for Jetty

This module implements a javax.security.auth.spi.LoginModule which is suitable for enabling
password authentication against a Neo4J graph database. This module is based on some of the 
logic in the JDBC Login Module provided with jetty.

## Using the module
This module is not set up with a clean automated installation process at this point.  But, manual usage is simple enough...

### Requirements
**Jetty Version**: > 11.0.x (build and tested using 11.0.7)

**Java Version**: > 15.0.1

### Installation
1. Build the module using Maven; `mvn clean package`

1. Copy the JAR file to the $JETTY_BASE/lib/ folder; `cp target/neo4j_login_module-1.0.jar $JETTY_BASE/lib/`

1. Copy the module definition to $JETTY_BASE/modules/; `cp src/main/resources/neo4j-authentication.mod $JETTY_BASE/modules/`

1. Copy the XML file to $JETTY_BASE/etc/; `cp src/main/resources/reload-ssl-keys.xml $JETTY_BASE/etc/`

1. Create a .ini file in the $JETTY_BASE/start.d/ directory; for example `$JETTY_BASE/start.d/neo4j-authentication.ini`.  The contents of this file should be;

```
# --------------------------------------- 
# Module: neo4j-authentication
# Enables the JAAS Login service for authentication against Neo4J. 
# --------------------------------------- 
--module=neo4j-authentication
```

1. Configure the setting controlling the access to Neo4J in the file `$JETTY_BASE/etc/login.conf`. The configuration file format for the login.conf file matches the standard format as documented in the Jetty documentation. The LOGIN_MODULE_NAME corresponds to the name provided in the `neo4j-authentication.xml` file. 

```
LOGIN_MODULE_NAME {
     com.bb.neo4j_login_module.Neo4jLoginModule required
        neo4jUser = "neo4jUsername"
        neo4jPassword = "neo4jPassword"
        neo4jUri = "neo4j://neo4jHost:7688"
        nodeType = "CredentialNode"
        usernameProp = "username"
        credsProp = "password"
        rolesProp = "roles";
};
```
 The parameters avaliable are;
   * neo4jUser : The username used to connect with the Neo4J database
   * neo4jPassword : The password used to connect with the Neo4J database
   * neo4jUri : The URI needed to connect to the Neo4J database
   * nodeType : The node type for the Neo4J node containing the credentials
   * usernameProp : The property in the Neo4J node containing the username
   * credsProp : The property in the Neo4J node containing the hashed password
   * rolesProp  : The property in the Neo4J node containing the roles for the user. This property may contain a single string, or an array of strings for multiple roles
   
   
