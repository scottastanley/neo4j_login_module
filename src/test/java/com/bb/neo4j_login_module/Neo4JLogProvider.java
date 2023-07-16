/* 
 * Copyright 2023 Scott Alan Stanley
 */
package com.bb.neo4j_login_module;
import java.util.Formatter;
import java.util.function.Consumer;

import org.neo4j.logging.Log;

public class Neo4JLogProvider implements org.neo4j.logging.LogProvider {

    @SuppressWarnings("rawtypes")
    @Override
    public Log getLog(Class loggingClass) {
        return new Neo4JLog(loggingClass);
    }

    @Override
    public Log getLog(String name) {
        return new Neo4JLog(name);
    }
    
    public class Neo4JLog implements Log {
        Neo4JLog(final Class<?> clazz) {
        }
        
        Neo4JLog(final String name) {
        }
        
        @Override
        public boolean isDebugEnabled() {
            return true;
        }

        @Override
        public void debug(String message) {
            System.out.println(message);
        }

        @Override
        public void debug(String message, Throwable throwable) {
            System.out.println("DEBUG: " + message);
            System.out.println("DEBUG: " + throwable);
        }

        @Override
        public void debug(String format, Object... arguments) {
            StringBuilder sb = new StringBuilder();
            try (Formatter formatter = new Formatter(sb);) {
                formatter.format(format, arguments).flush();
                System.out.println("DEBUG: " + sb);
            }
        }

        @Override
        public void info(String message) {
            System.out.println("INFO: " + message);
        }

        @Override
        public void info(String message, Throwable throwable) {
            System.out.println("INFO: " + message);
            System.out.println("INFO: " + throwable);
        }

        @Override
        public void info(String format, Object... arguments) {
            StringBuilder sb = new StringBuilder();
            try (Formatter formatter = new Formatter(sb);) {
                formatter.format(format, arguments).flush();
                System.out.println("INFO: " + sb);
            }
        }

        @Override
        public void warn(String message) {
            System.out.println("WARN: " + message);
        }

        @Override
        public void warn(String message, Throwable throwable) {
            System.out.println("WARN: " + message);
            System.out.println("WARN: " + throwable);
        }

        @Override
        public void warn(String format, Object... arguments) {
            StringBuilder sb = new StringBuilder();
            try (Formatter formatter = new Formatter(sb);) {
                formatter.format(format, arguments).flush();
                System.out.println("WARN: " + sb);
            }
        }

        @Override
        public void error(String message) {
            System.err.println("ERROR: " + message);
        }

        @Override
        public void error(String message, Throwable throwable) {
            System.err.println("ERROR: " + message);
            System.err.println("ERROR: " + throwable);
        }

        @Override
        public void error(String format, Object... arguments) {
            StringBuilder sb = new StringBuilder();
            try (Formatter formatter = new Formatter(sb);) {
                formatter.format(format, arguments).flush();
                System.out.println("ERROR: " + sb);
            }
        }

        //
        // Deprecated APIs
        //
        @Override
        public void bulk(Consumer<Log> consumer) {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("removal")
        @Override
        public org.neo4j.logging.Logger errorLogger() {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("removal")
        @Override
        public org.neo4j.logging.Logger warnLogger() {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("removal")
        @Override
        public org.neo4j.logging.Logger infoLogger() {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("removal")
        @Override
        public org.neo4j.logging.Logger debugLogger() {
            throw new UnsupportedOperationException();
        }
    }
}