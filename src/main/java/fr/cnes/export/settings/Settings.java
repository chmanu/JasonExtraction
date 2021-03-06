 /******************************************************************************
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of Regards.
 *
 * Regards is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Regards is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Regards.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fr.cnes.export.settings;

import fr.cnes.export.jason.Utils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.restlet.data.LocalReference;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

/**
 * Singleton to load and use the defined variables in the config.properties.
 *
 * @author Jean-Christophe (jean-christophe.malapert@cnes.fr)
 */
public class Settings {

    /**
     * Configuration files in JAR.
     */
    public static final String CONFIG_PROPERTIES = "config.properties";

    private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

    /**
     * private constructor
     */
    private Settings() {
        init();
    }

    /**
     * Loads configuration file and set it in memory.
     */
    private void init() {
        Properties properties = loadConfigurationFile(CONFIG_PROPERTIES);
        fillConcurrentMap(properties);
    }

    /**
     * Load configuration file.
     *
     * @param path path to the configuration file.
     * @return the configuration file content
     */
    private Properties loadConfigurationFile(String path) {
        Properties properties = new Properties();
        ClientResource client = new ClientResource(LocalReference.createClapReference("class/config.properties"));
        Representation configurationFile = client.get();
        try {
            properties.load(configurationFile.getStream());
        } catch (IOException e) {           
            throw new RuntimeException("Unable to load " + path);
        } finally {
            client.release();
        }
        return properties;
    }

    /**
     * Sets the configuration as a map.
     *
     * @param properties the configuration file content
     */
    private void fillConcurrentMap(final Properties properties) {
        for (final Entry<Object, Object> entry : properties.entrySet()) {
            map.put((String) entry.getKey(), (String) entry.getValue());
        }
    }

    /**
     * Holder
     */
    private static class DoiSettingsHolder {

        /**
         * Unique Instance unique not pre-initiliaze
         */
        private final static Settings INSTANCE = new Settings();
    }

    /**
     * Access to unique INSTANCE of Settings
     *
     * @return the configuration instance.
     */
    public static Settings getInstance() {
        return DoiSettingsHolder.INSTANCE;
    }
    
    /**
     * Tests if the key has a value.
     * @param key key to test
     * @return True when the value is different or null and empty
     */
    public boolean hasValue(final String key) {
        return Utils.isNotEmpty(getString(key));
    }


    /**
     * Returns the value of the key as string.
     *
     * @param key key to search
     * @param defaultValue Default value if the key is not found
     * @return the value of the key
     */
    public String getString(final String key, final String defaultValue) {
        return map.getOrDefault(key, defaultValue);
    }

    /**
     * Returns the value of the key or null if no mapping for the key
     *
     * @param key key to search
     * @return the value of the key
     */
    public String getString(final String key) {
        return map.get(key);
    }

    /**
     * Returns the value of the key as an integer. NumberFormatException is
     * raisen when the value of the key in not compatible with an integer
     *
     * @param key key to search
     * @return the value
     */
    public int getInt(final String key) {
        return Integer.parseInt(getString(key));
    }

    /**
     * Returns the value of the key as an integer. NumberFormatException is
     * raisen when the value of the key in not compatible
     *
     * @param key key to search
     * @param defaultValue default value
     * @return the value
     */
    public int getInt(final String key, final String defaultValue) {
        return Integer.parseInt(getString(key, defaultValue));
    }

    /**
     * Returns the value of the key as a boolean. An exception is raisen when
     * the value of the key in not compatible with a boolean
     *
     * @param key key to search
     * @return the value
     */
    public boolean getBoolean(final String key) {
        if(getString(key) == null) {
            throw new IllegalArgumentException("Key not found");
        } else {
            return Boolean.getBoolean(getString(key));   
        }        
    }

    /**
     * Returns the value of the key as a long. NumberFormatException is raisen
     * when the value of the key in not compatible
     *
     * @param key key to search
     * @return the value
     */
    public Long getLong(final String key) {
        return Long.parseLong(getString(key));
    }

    /**
     * Returns the value of the key as a long. NumberFormatException is raisen
     * when the value of the key in not compatible
     *
     * @param key key to search
     * @param defaultValue default value
     * @return the value
     */
    public Long getLong(String key, String defaultValue) {
        return Long.parseLong(getString(key, defaultValue));
    }

    /**
     * Displays the configuration file.
     */
    public void displayConfigFile() {
        ClientResource client = new ClientResource(LocalReference.createClapReference("class/"+CONFIG_PROPERTIES));
        Representation configurationFile = client.get();
        try {
            copyStream(configurationFile.getStream(), System.out);
        } catch (IOException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            client.release();
        }
    }

    /**
     * Copy input stream to output stream.
     * @param is input stream
     * @param os output stream
     */
    private void copyStream(final InputStream is, final OutputStream os) {
        final int buffer_size = 1024;
        try {
            byte[] bytes = new byte[buffer_size];
            for (;;) {
                int count = is.read(bytes, 0, buffer_size);
                if (count == -1) {
                    break;
                }
                os.write(bytes, 0, count);
            }
            is.close();
            os.flush();
            os.close();
        } catch (IOException ex) {
        } 
    }

    /**
     * Sets a custom properties file.
     * @param path Path to the properties file
     * @throws java.io.FileNotFoundException
     */
    public void setPropertiesFile(final String path) throws FileNotFoundException, IOException {
        Properties properties = new Properties();
        try (FileInputStream is = new FileInputStream(new File(path))) {
            properties.load(is);
            fillConcurrentMap(properties);
        }
    }
    
    /**
     * Sets a custom properties file.
     * @param is Input stream
     * @throws java.io.IOException
     */
    public void setPropertiesFile(final InputStream is) throws IOException {
        Properties properties = new Properties();
        properties.load(is);
        fillConcurrentMap(properties);
    }    
}
