[description]
Configures the Neo4J JAAS login module.

[depend]
server
jaas

[lib]
lib/neo4j_login_module-1.0.jar

[xml]
etc/neo4j-authentication.xml

[ini-template]
# --------------------------------------- 
# Module: neo4j-authentication
# Enables the JAAS Login service for authentication against Neo4J. 
# --------------------------------------- 
--module=neo4j-authentication
