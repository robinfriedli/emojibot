package core;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Set;

public class SettingsLoader {

    public static boolean loadBoolProperty(String propertyName) {
        try {
            FileInputStream in = new FileInputStream("./resources/settings.conf");
            Properties properties = new Properties();
            properties.load(in);
            in.close();
            String boolProp = properties.getProperty(propertyName);

            if (boolProp != null) {
                if (boolProp.equalsIgnoreCase("true") || boolProp.equalsIgnoreCase("false")) {
                    return Boolean.parseBoolean(boolProp);
                } else {
                    throw new IllegalArgumentException("Property not a boolean");
                }
            } else {
                throw new IllegalArgumentException("No property found for " + propertyName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        throw new IllegalStateException("Property loading failed");
    }

    public static void setBoolProperty(String propertyName, boolean bool) {
        try {
            FileInputStream in = new FileInputStream("./resources/settings.conf");
            Properties properties = new Properties();
            properties.load(in);
            in.close();

            if (properties.getProperty(propertyName) != null) {
                properties.setProperty(propertyName, Boolean.toString(bool));
            } else {
                throw new IllegalArgumentException("No property found for " + propertyName);
            }

            FileOutputStream out = new FileOutputStream("./resources/settings.conf");
            properties.store(out, null);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String displaySettings() {
        StringBuilder builder = new StringBuilder();
        try {
            FileInputStream in = new FileInputStream("./resources/settings.conf");
            Properties properties = new Properties();
            properties.load(in);
            in.close();
            Set<String> propertieNames = properties.stringPropertyNames();

            for (String propertyName : propertieNames) {
                builder.append(propertyName).append("\t").append(properties.getProperty(propertyName)).append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return builder.toString();
    }

    public static String displaySettings(String propertyName) {
        try {
            FileInputStream in = new FileInputStream("./resources/settings.conf");
            Properties properties = new Properties();
            properties.load(in);
            in.close();
            String property = properties.getProperty(propertyName);

            if (property != null) {
                return String.format("%s\t%s", propertyName, property);
            } else {
                throw new IllegalArgumentException("No property found for " + propertyName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        throw new IllegalStateException("Property loading failed");
    }

}
