package com.coderrect.conflib;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class Version {
    public static String VERSION = "0.0.1";

    public static void main(String[] args) throws Exception {
        System.out.println("conflib v0.0.1");
/*
        for (String s : args) {
            System.out.println("args: " + s);
        }

        System.out.println("cwd " + System.getProperty("user.dir"));

        ConfLib.initialize(args);
        int param2 = ConfLib.get("reporter.param1", 2);
        System.out.println(param2);

        double param3 = ConfLib.get("reporter.param3", 3.14);
        System.out.println(param3);
 */
    }
}
