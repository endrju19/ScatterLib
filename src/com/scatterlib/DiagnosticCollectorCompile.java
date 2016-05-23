package com.scatterlib;

/**
 * Created by przemek on 23.05.16.
 */

import com.scatterlib.mdkt.compiler.InMemoryJavaCompiler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Scanner;

public class DiagnosticCollectorCompile {

    public static void main(String args[]) throws IOException {
        StringBuffer sourceCode = new StringBuffer();

        sourceCode.append("package org.mdkt;\n");
        sourceCode.append("public class HelloClass {\n");
        sourceCode.append("   public static String hello() { return \"hello\"; }");
        sourceCode.append("}");

        Class<?> helloClass = null;


        Scanner scanner = new Scanner(new File("src/" + Work.class.getName().replace(".", "/") + ".java"));
        String text = scanner.useDelimiter("\\A").next();
        scanner.close();

        try {
            helloClass = InMemoryJavaCompiler.compile(Work.class.getName(), Work.class.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(helloClass.getDeclaredMethods().length);

        try {
            System.out.println(helloClass.getDeclaredMethods()[0].invoke(null));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}