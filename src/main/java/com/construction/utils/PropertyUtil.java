package com.construction.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyUtil {

    private static final Properties PROPERTIES = new Properties();

    private static void loadPropertiesFile() {
        InputStream inputStream = null;
        try {
            inputStream = PropertyUtil.class.getClassLoader().getResourceAsStream("application.properties");
            PROPERTIES.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getProperty(String key) {
        if (PROPERTIES.isEmpty()) {
            loadPropertiesFile();
        }
        return PROPERTIES.getProperty(key);
    }
}
