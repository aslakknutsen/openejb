/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.openejb.resource.jdbc;

import org.apache.commons.dbcp.SQLNestedException;
import org.apache.xbean.finder.ResourceFinder;

import java.sql.SQLException;
import java.util.Map;
import java.io.IOException;

public final class BasicDataSourceUtil {
    private BasicDataSourceUtil() {
    }

    public static DataSourcePlugin getDataSourcePlugin(String jdbcUrl) throws SQLException {
        // determine the vendor based on the jdbcUrl stirng "jdbc:${Vendor}:properties"
        String vendor = getJdbcName(jdbcUrl);

        // no vendor so no plugin
        if (vendor == null) return null;

        // find the plugin class
        String pluginClassName = null;
        try {
            ResourceFinder finder = new ResourceFinder("META-INF");
            Map<String,String> plugins = finder.mapAvailableStrings(DataSourcePlugin.class.getName());
            pluginClassName = plugins.get(vendor);
        } catch (IOException ignored) {
            // couldn't determine the plugins, which isn't fatal
        }

        // no plugin found
        if (pluginClassName == null || pluginClassName.length() <= 0) {
            return null;
        }

        // create the plugin
        try {
            Class pluginClass = Class.forName(pluginClassName);
            DataSourcePlugin plugin = (DataSourcePlugin) pluginClass.newInstance();
            return plugin;
        } catch (ClassNotFoundException e) {
            throw new SQLException("Unable to load data source helper class '" + pluginClassName + "' for database '" + vendor + "'");
        } catch (Exception e) {
            throw (SQLException) new SQLException("Unable to create data source helper class '" + pluginClassName + "' for database '" + vendor + "'").initCause(e);
        }
    }

    public static String getJdbcName(String jdbcUrl) {
        // nothing gets you nothing
        if (jdbcUrl == null) return null;
        
        // strip off "jdbc:"
        if (!jdbcUrl.startsWith("jdbc:")) {
            return null;
        }
        jdbcUrl = jdbcUrl.substring("jdbc:".length());

        // return text up to first ":" if present
        int index = jdbcUrl.indexOf(':');

        // It is ok to have no trailing ':'.  This may be a url like jdbc:specialDB.
        if (index >= 0) {
            jdbcUrl = jdbcUrl.substring(0, index);
        }

        return jdbcUrl;
    }
    
    /**
     * Create a {@link PasswordCipher} instance from the
     *  passwordCipher class name.
     * 
     * @param passwordCipherClass the password cipher to look for
     * @return the password cipher from the passwordCipher class name
     *         optionally set.
     * @throws SQLException
     *             if the driver can not be found.
     */
    public static PasswordCipher getPasswordCipher(String passwordCipherClass) throws SQLException {
        // Load the password cipher class
        Class<? extends PasswordCipher> pwdCipher = null;

        // try looking for implementation in /META-INF/org.apache.openejb.resource.jdbc.PasswordCipher
        ResourceFinder finder = new ResourceFinder("META-INF/");
        Map<String, Class<? extends PasswordCipher>> impls;
        try {
            impls = finder.mapAllImplementations(PasswordCipher.class);
            
        } catch (Throwable t) {
            String message = 
                "Password cipher '" + passwordCipherClass +
                "' not found in META-INF/org.apache.openejb.resource.jdbc.PasswordCipher.";
            throw new SQLNestedException(message, t);
        }
        pwdCipher = impls.get(passwordCipherClass);

        // if not found in META-INF/org.apache.openejb.resource.jdbc.PasswordCipher
        // we can try to load the class.
        if (null == pwdCipher) {
            try {
                try {
                    pwdCipher = Class.forName(passwordCipherClass).asSubclass(PasswordCipher.class);
                    
                } catch (ClassNotFoundException cnfe) {
                    pwdCipher = Thread.currentThread().getContextClassLoader().loadClass(passwordCipherClass).asSubclass(PasswordCipher.class);
                }
            } catch (Throwable t) {
                String message = "Cannot load password cipher class '" + passwordCipherClass + "'";
                throw new SQLNestedException(message, t);
            }
        }

        // Create an instance
        PasswordCipher cipher = null;
        try {
            cipher = pwdCipher.newInstance();

        } catch (Throwable t) {
            String message = "Cannot create password cipher instance";
            throw new SQLNestedException(message, t);
        }

        return cipher;
    }
}
