package com.coderrect.conflib;


import com.google.common.base.Splitter;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * ConfLib combines configurations from three places:
 *   - <installation-folder>/conf/configuration.json
 *   - a custom json configuration file
 *   - command-line configurations
 *
 * The configurations from the command line will precede same item in the custom configuration
 * file whose configurations will precede the same item in the default configuration file.
 *
 * How does conflib look for the default configuration file?
 * It searches "conf/coderrect.json" of the installation folder. The code relies on the environment
 * variable CODERRECT_HOME to determine the installation folder.
 *
 * How does conflib look for the custom configuration file?
 * If the user specifies a custom file via the command line (-conf), the file will be used.
 * Otherwise, it checks if ~/.coderrect.json exists.
 *
 * Command line example 1
 *   java -jar foo -param1 -param2=hello -p.a.ram3=45
 *
 * The example above specifies three parameters via the command line. It doesn't specify
 * the custom configuration file so that the conflib will check if there is a file ~/.codderct.json
 * as the custom configuration file.
 *
 * Command line example 2
 *   java -jar bar -param1 -param2=33 -conf=myproj/coderrect.json
 *
 * The example above specifies 2 parameters via the command line. It also specifies a custom
 * configuration file "myproj/coderrect.json" (so, ~/.coderrect.json won't be used).
 *
 * How does your code use conflib?
 *
 * <code>
 *   ConfLib.initialize(argv);  // call this from the beginning of your code
 *   int threshold = ConfLib.get("racedetector.threshold", 2);
 * </code>
 */
public class ConfLib {
    public static final String DEFAULT_CONFIGURATION_NAME = "coderrect.json";
    public static final String DEFAULT_CUSTOM_CONFIGURATION_PATH = "~/.coderrect.json";
    public static final String ENV_INSTALLATION_DIRECTORY = "CODERRECT_HOME";

    private static DocumentContext _defaultConf = null;
    private static DocumentContext _customConf = null;
    private static DocumentContext _cmdlineConf = null;

    public static void initialize(String[] args) {
        try {
            String confPath = null;

            // load the default configuration file
            String homeDir = System.getenv(ENV_INSTALLATION_DIRECTORY);
            if (homeDir != null) {
                confPath = homeDir + File.separator + "conf" + File.separator + DEFAULT_CONFIGURATION_NAME;
            }
            else {
                // we assume the base dir of the application is the installation dir
                // todo
            }
            if (confPath != null) {
                _defaultConf = JsonPath.parse(new File(confPath));
            }

            // load the custom configuration file
            confPath = DEFAULT_CUSTOM_CONFIGURATION_PATH;
            for (String s : args) {
                if (s.startsWith("-conf=")) {
                    confPath = _getConfValue(s);
                }
            }
            File customFile = new File(confPath);
            if (customFile.exists())
                _customConf = JsonPath.parse(customFile);

            // converts all command-line parameters (only param prefixed with "-")
            _cmdlineConf = _paramsToDocumentContext(args);
        }
        catch (IOException e) {
            throw new ConfLibException(e);
        }
    }

    public static <T> T get(String key, T defaultValue) {
        String jpath = key.startsWith("$.") ? key : "$." + key;

        if (_jsonPathExists(_cmdlineConf, jpath)) {
            return _cmdlineConf.read(jpath);
        }

        if (_jsonPathExists(_customConf, jpath)) {
            return _customConf.read(jpath);
        }

        if (_jsonPathExists(_defaultConf, jpath)) {
            return _defaultConf.read(jpath);
        }

        return defaultValue;
    }


    /**
     * the param "s" has the format "-key=value". _getConfValue() returns
     * the part after "="
     */
    private static String _getConfValue(String s) {
        int pos = s.indexOf('=');
        if (pos > 0)
            return s.substring(pos+1);
        throw new ConfLibException("Invalid parameter string " + s);
    }


    private static DocumentContext _paramsToDocumentContext(String[] args) {
        DocumentContext ctx = JsonPath.parse("{}");

        for (String arg : args) {
            if (!arg.startsWith("-") || arg.startsWith("-conf=") || arg.startsWith("="))
                continue;

            int pos = arg.indexOf('=');
            if (pos < 0) {
                // sth like -racedetector.enableFunction
                arg += "=true";
            }

            Object[] kv = _parseProperty(arg.substring(1));
            List<String> stages = Splitter.on('.').splitToList(kv[0].toString());
            if (stages.size() == 1) {
                ctx.put(JsonPath.compile("$"), stages.get(0), kv[1]);
            }
            else {
                String path = "$";
                for (int i = 0; i < stages.size()-1; i++) {
                    ctx.put(JsonPath.compile(path), stages.get(i), new HashMap<String, Object>());
                    path = path + "." + stages.get(i);
                }
                ctx.put(JsonPath.compile(path), stages.get(stages.size()-1), kv[1]);
            }
        }

        System.out.println(ctx.jsonString());
        return ctx;
    }


    private static boolean _jsonPathExists(DocumentContext ctx, String jpath) {
        if (ctx == null)
            return false;

        try {
            ctx.read(jpath);
            return true;
        }
        catch (PathNotFoundException e) {
            return false;
        }
    }


    private static Object[] _parseProperty(String prop) {
        int pos = prop.indexOf('=');
        Object[] kv = new Object[2];
        kv[0] = prop.substring(0, pos);

        String s = prop.substring(pos+1);
        if (s.equalsIgnoreCase("true")) {
            kv[1] = Boolean.TRUE;
        }
        else if (s.equalsIgnoreCase("false")) {
            kv[1] = Boolean.FALSE;
        }
        else if (_isInteger(s)) {
            kv[1] = Integer.parseInt(s);
        }
        else if (_isDouble(s)) {
            kv[1] = Double.parseDouble(s);
        }
        else {
            kv[1] = s;
        }

        return kv;
    }


    private static boolean _isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }


    private static boolean _isDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }
}
