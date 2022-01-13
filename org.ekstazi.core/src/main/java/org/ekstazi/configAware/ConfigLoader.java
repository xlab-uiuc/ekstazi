package org.ekstazi.configAware;

import org.ekstazi.Config;
import org.ekstazi.log.Log;
import org.ekstazi.util.FileUtil;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.*;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class ConfigLoader {
    private static final Map<String, String > sExercisedConfigMap = new HashMap<String, String>();
    private static final String configFileSeparator = ",";

    public static Map<String, String> getUserConfigMap() {
        if (sExercisedConfigMap.isEmpty() || sExercisedConfigMap == null){
            loadConfigFromFile();
        }
        return sExercisedConfigMap;
    }

    public static Boolean hasConfigFile(String configFileName) {
        //String configFileName = Config.CONFIG_FILE_PATH_V;
        if (configFileName.isEmpty() || configFileName == null) {
            return false;
        }
        File configFile = new File(configFileName);
        return configFile.exists();
    }

    /**
     *  Load user's configuration file
     *  TODO: Need to consider different types of configuration files
     */
    public static void loadConfigFromFile() {
        String configFileName = Config.CONFIG_FILE_PATH_V;
        load0(configFileName);
    }

    private static void load0(String filenameList) {
        InputStream is = null;

        //Handle several configuration files, separated by ","
        String files [] = filenameList.split(configFileSeparator);
        for (String filename : files) {
            filename = filename.trim();
            if (filename == null || filename.isEmpty()) {
                Log.d2f("Load configuration: Continue next file, current filename: " + filename);
                continue;
            }
            if (!hasConfigFile(filename)) {
                Log.d2f("Can't find user's configuration file: " + filename);
                return;
            }
            try {
                is = new FileInputStream(filename);
                parseConfigurationFile(filename, is);
                //Log.d2f("Configuration file is loaded.");
                //Log.printConfig(sExercisedConfigMap, "-loadMethod");
            } catch (IOException e) {
                Log.e("Loading configuration is not successful", e);
                Log.d2f("Loading configuration is not successful" + e);
                sExercisedConfigMap.clear();
            } finally {
                FileUtil.closeAndIgnoreExceptions(is);
            }
        }
    }

    private static String getFileSuffix(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private static void parseConfigurationFile(String filename, InputStream is) throws IOException {
        String fileSuffix = getFileSuffix(filename).toLowerCase();
        switch (fileSuffix) {
            case "xml":
                loadFromXML(is);
                break;
            case "properties":
                loadFromProperties();
                break;
            case "cfg":
                loadFromCFG();
                break;
            default:
                Log.e("Can't load configuration from ." + fileSuffix + " file");
                throw new IOException();
        }
    }

    private static void loadFromXML(InputStream is) {
        parseXML(is, "property", "name", "value");
    }

    private static void loadFromProperties() {

    }

    private static void loadFromCFG() {

    }


    public static void parseXML(InputStream is, String tagName, String tagConfigName, String tagConfigValue) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            NodeList nl = doc.getElementsByTagName(tagName);
            for (int i = 0; i < nl.getLength(); i++) {
                NodeList nl2 = nl.item(i).getChildNodes();
                String configName = "";
                String configValue = "";
                for (int j = 0; j < nl2.getLength(); j++) {
                    Node n = nl2.item(j);
                    if (n.getNodeName().equals(tagConfigName)) configName = n.getTextContent();
                    if (n.getNodeName().equals(tagConfigValue)) configValue = n.getTextContent();
                }

                // Multiple configuration files may have duplicated settings. We choose the last one as the final value (Overwrite)
                // This is the same idea as some real-world software like Hadoop.
                if (!Objects.equals(configName, "") && !Objects.equals(configValue, "")) {
                    sExercisedConfigMap.put(replaceBlank(configName), replaceBlank(configValue));
                }
                //System.out.println(configName + " , " + configValue);
            }
        } catch (Exception e) {
            Log.d2f("Loading configuration is not successful: " + e.getStackTrace());
            Log.e("Loading configuration is not successful", e);
            sExercisedConfigMap.clear();
        }
    }

    // internal method

    /**
     * Remove blank space, \r, \n, \t in a given string
     */
    public static String replaceBlank(String str) {
        String dest = "";
        if (str != null) {
            Pattern p = Pattern.compile("\\s*|\t|\r|\n");
            Matcher m = p.matcher(str);
            dest = m.replaceAll("");
        }
        return dest;
    }

    // For simply test
    public static void main(String args[]) {
        //load0("/Users/alenwang/Documents/xlab/hadoop/hadoop-common-project/hadoop-kms/src/main/conf/kms-site.xml");
        load0("/Users/alenwang/Documents/xlab/hadoop/hadoop-common-project/hadoop-common/src/main/resources/core-default-ekstazi.xml");
        int count = 0;
        for(Map.Entry<String, String> entry : sExercisedConfigMap.entrySet()) {
            count += 1;
            System.out.println(entry.getKey() + " , " + entry.getValue());
        }
        System.out.println(count);
    }
}
