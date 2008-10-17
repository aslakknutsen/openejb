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
package org.apache.openejb.tomcat.installer;

import org.codehaus.swizzle.stream.DelimitedTokenReplacementInputStream;
import org.codehaus.swizzle.stream.StringTokenHandler;
import org.apache.openejb.loader.SystemInstance;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.jar.JarFile;

public class Installer {
    public enum Status {
        NONE, INSTALLED, REBOOT_REQUIRED
    }

    private static final boolean listenerInstalled;
    private static final boolean agentInstalled;

    static {
        // is the OpenEJB listener installed
        listenerInstalled = "OpenEJBListener".equals(SystemInstance.get().getProperty("openejb.embedder.source"));

        // is the OpenEJB javaagent installed
        agentInstalled = invokeStaticNoArgMethod("org.apache.openejb.javaagent.Agent", "getInstrumentation") != null;
    }

    public static boolean isListenerInstalled() {
        return listenerInstalled;
    }

    public static boolean isAgentInstalled() {
        return agentInstalled;
    }

    private final Paths paths;
    private Status status = Status.NONE;

    // Thi may need to be redesigned but the goal is to provide some feedback on what happened
    private final List<String> errors = new ArrayList<String>();
    private final List<String> warnings = new ArrayList<String>();
    private final List<String> infos = new ArrayList<String>();

    public Installer(Paths paths) {
        this.paths = paths;

        if (listenerInstalled && agentInstalled) {
            status = Status.INSTALLED;
        }
    }

    public Status getStatus() {
        return status;
    }

    public void installAll() {
        installListener();

        installJavaagent();

        installConfigFiles();
        
        if (!hasErrors()) {
            status = Status.REBOOT_REQUIRED;
        }
    }

    public void installListener() {
        if (listenerInstalled) {
//            addInfo("OpenEJB Listener already installed");
            return;
        }

        boolean copyOpenEJBLoader = true;

        // copy loader jar to lib
        File destination = new File(paths.getCatalinaLibDir(), paths.getOpenEJBTomcatLoaderJar().getName());
        if (destination.exists()) {
            if (paths.getOpenEJBTomcatLoaderJar().length() != destination.length()) {
                // md5 diff the files
            } else {
//                addInfo("OpenEJB loader jar already installed in Tomcat lib directory.");
                copyOpenEJBLoader = false;
            }
        }

        if (copyOpenEJBLoader) {
            try {
                copyFile(paths.getOpenEJBTomcatLoaderJar(), destination);
                addInfo("Copy " + paths.getOpenEJBTomcatLoaderJar().getName() + " to lib");
            } catch (IOException e) {
                addError("Unable to copy OpenEJB Tomcat loader jar to Tomcat lib directory.  This will need to be performed manually.", e);
            }
        }

        // read server.xml
        String serverXmlOriginal = readAll(paths.getServerXmlFile());

        // server xml will be null if we couldn't read the file
        if (serverXmlOriginal == null) {
            return;
        }

        // does the server.xml contain our listener name... it is possible that they commented out our listener, but that would be a PITA to detect
        if (serverXmlOriginal.contains("org.apache.openejb.tomcat.loader.OpenEJBListener")) {
            addWarning("OpenEJB Listener already declared in Tomcat server.xml file.");
            return;
        }

        // if we can't backup the file, do not modify it
        if (!backup(paths.getServerXmlFile())) {
            return;
        }

        // add our listener
        String newServerXml = null;
        try {
            newServerXml = replace(serverXmlOriginal,
                    "<Server",
                    "<Server",
                    ">",
                    ">\r\n" +
                            "  <!-- OpenEJB plugin for Tomcat -->\r\n" +
                            "  <Listener className=\"org.apache.openejb.tomcat.loader.OpenEJBListener\" />");
        } catch (IOException e) {
            addError("Error while adding listener to server.xml file", e);
        }

        // overwrite server.xml
        if (writeAll(paths.getServerXmlFile(), newServerXml)) {
            addInfo("Add OpenEJB listener to server.xml");
        }
    }

    public void installJavaagent() {
        if (agentInstalled) {
//            addInfo("OpenEJB Agent already installed");
            return;
        }

        //
        // Copy openejb-javaagent.jar to lib
        //
        boolean copyJavaagentJar = true;
        File javaagentJar = new File(paths.getCatalinaLibDir(), "openejb-javaagent.jar");
        if (javaagentJar.exists()) {
            if (paths.getOpenEJBJavaagentJar().length() != javaagentJar.length()) {
                // md5 diff the files
            } else {
//                addInfo("OpenEJB javaagent jar already installed in Tomcat lib directory.");
                copyJavaagentJar = false;
            }
        }

        if (copyJavaagentJar) {
            try {
                copyFile(paths.getOpenEJBJavaagentJar(), javaagentJar);
                addInfo("Copy " + paths.getOpenEJBJavaagentJar().getName() + " to lib");
            } catch (IOException e) {
                addError("Unable to copy OpenEJB javaagent jar to Tomcat lib directory.  This will need to be performed manually.", e);
            }
        }


        //
        // bin/catalina.sh
        //

        // read the catalina sh file
        String catalinaShOriginal = readAll(paths.getCatalinaShFile());

        // catalina sh will be null if we couldn't read the file
        if (catalinaShOriginal == null) {
            return;
        }

        // does the catalina sh contain our comment... it is possible that they commented out the magic script code, but there is no way to detect that
        if (catalinaShOriginal.contains("Add OpenEJB javaagent")) {
            addWarning("OpenEJB javaagent already declared in Tomcat catalina.sh file.");
            return;
        }

        // if we can't backup the file, do not modify it
        if (!backup(paths.getCatalinaShFile())) {
            return;
        }

        // add our magic bits to the catalina sh file
        String openejbJavaagentPath = paths.getCatalinaBaseDir().toURI().relativize(javaagentJar.toURI()).getPath();
        String newCatalinaSh = catalinaShOriginal.replace("# ----- Execute The Requested Command",
                "# Add OpenEJB javaagent\n" +
                "if [ -r \"$CATALINA_BASE\"/" + openejbJavaagentPath + " ]; then\n" +
                "  JAVA_OPTS=\"\"-javaagent:$CATALINA_BASE/" + openejbJavaagentPath + "\" $JAVA_OPTS\"\n" +
                "fi\n" +
                "\n" +
                "# ----- Execute The Requested Command");

        // overwrite the catalina.sh file
        if (writeAll(paths.getCatalinaShFile(), newCatalinaSh)) {
            addInfo("Add OpenEJB JavaAgent to catalina.sh");
        }

        //
        // bin/catalina.bat
        //

        // read the catalina bat file
        String catalinaBatOriginal = readAll(paths.getCatalinaBatFile());

        // catalina bat will be null if we couldn't read the file
        if (catalinaBatOriginal == null) {
            return;
        }

        // does the catalina bat contain our comment... it is possible that they commented out the magic script code, but there is no way to detect that
        if (catalinaBatOriginal.contains("Add OpenEJB javaagent")) {
            addWarning("OpenEJB javaagent already declared in Tomcat catalina.bat file.");
            return;
        }

        // if we can't backup the file, do not modify it
        if (!backup(paths.getCatalinaBatFile())) {
            return;
        }

        // add our magic bits to the catalina bat file
        openejbJavaagentPath = openejbJavaagentPath.replace('/', '\\');
        String newCatalinaBat = catalinaBatOriginal.replace("rem ----- Execute The Requested Command",
                "rem Add OpenEJB javaagent\r\n" +
                "if not exist \"%CATALINA_BASE%\\" + openejbJavaagentPath + "\" goto noOpenEJBJavaagent\r\n" +
                "set JAVA_OPTS=\"-javaagent:%CATALINA_BASE%\\" + openejbJavaagentPath + "\" %JAVA_OPTS%\r\n" +
                ":noOpenEJBJavaagent\r\n" +
                "\r\n" +
                "rem ----- Execute The Requested Command");

        // overwrite the catalina.bat file
        if (writeAll(paths.getCatalinaBatFile(), newCatalinaBat)) {
            addInfo("Add OpenEJB JavaAgent to catalina.bat");
        }
    }
    /**
     * Installs conf/openejb.xml and conf/logging.properties files.
     * This method retrieves the openejb.xml and logging.properties files
     * from openejb core jar file and installs them under the conf directory
     * of tomcat. if there is already a conf/logging.properties file available
     * then this method appends the contents of openejb logging.properties file
     * to the exisiting properties file. 
     * NOTE:- If the existing conf/logging.properties file already has some openejb specific
     * configuration, then this method will just leave the logging.properties file alone
     */
    public void installConfigFiles() {
        if (paths.getOpenEJBCoreJar() == null) {
            // the core jar contains the config files
            return;
        }
        JarFile coreJar;
        try {
            coreJar = new JarFile(paths.getOpenEJBCoreJar());
        } catch (IOException e) {
            return;
        }

        //
        // conf/openejb.xml
        //
        File openEjbXmlFile = new File(paths.getCatalinaConfDir(), "openejb.xml");
        if (!openEjbXmlFile.exists()) {
            // read in the openejb.xml file from the openejb core jar
            String openEjbXml = readEntry(coreJar, "default.openejb.conf");
            if (openEjbXml != null) {
                if (writeAll(openEjbXmlFile, openEjbXml)) {
                    addInfo("Copy openejb.xml to conf");
                }
            }
        }


        //
        // conf/logging.properties
        //
        String openejbLoggingProps = readEntry(coreJar, "logging.properties");
        if (openejbLoggingProps != null) {
            File loggingPropsFile = new File(paths.getCatalinaConfDir(), "logging.properties");
            String newLoggingProps = null;
            if (!loggingPropsFile.exists()) {
                newLoggingProps = openejbLoggingProps;
            } else {
                String loggingPropsOriginal = readAll(loggingPropsFile);
                if (!loggingPropsOriginal.toLowerCase().contains("openejb")) {
                    // strip off license header
                    String[] strings = openejbLoggingProps.split("## --*", 3);
                    if (strings.length == 3) {
                        openejbLoggingProps = strings[2];
                    }
                    // append our properties
                    newLoggingProps = loggingPropsOriginal +
                            "\r\n" +
                            "############################################################\r\n" +
                            "# OpenEJB Logging Configuration.\r\n" +
                            "############################################################\r\n" +
                            openejbLoggingProps + "\r\n";
                }
            }
            if (newLoggingProps != null) {
                if (writeAll(loggingPropsFile, newLoggingProps)) {
                    addInfo("Append OpenEJB config to logging.properties");
                }
            }
        }
    }

    private String readEntry(JarFile jarFile, String name) {
        ZipEntry entry = jarFile.getEntry(name);
        if (entry == null) return null;
        InputStream in = null;
        try {
            in = jarFile.getInputStream(entry);
            String text = readAll(in);
            return text;
        } catch (Exception e) {
            addError("Unable to read " + name + " from " + jarFile.getName());
            return null;
        } finally {
            close(in);
        }
    }

    private String replace(String inputText, String begin, String newBegin, String end, String newEnd) throws IOException {
        BeginEndTokenHandler tokenHandler = new BeginEndTokenHandler(newBegin, newEnd);

        ByteArrayInputStream in = new ByteArrayInputStream(inputText.getBytes());

        InputStream replacementStream = new DelimitedTokenReplacementInputStream(in, begin, end, tokenHandler, true);
        String newServerXml = readAll(replacementStream);
        close(replacementStream);
        return newServerXml;
    }

    private boolean backup(File source) {
        try {
            File backupFile = new File(source.getParent(), source.getName() + ".original");
            if (!backupFile.exists()) {
                copyFile(source, backupFile);
            }
            return true;
        } catch (IOException e) {
            addError("Unable to backup " + source.getAbsolutePath() + "; No changes will be made to this file");
            return false;
        }
    }

    private void copyFile(File source, File destination) throws IOException {
        File destinationDir = destination.getParentFile();
        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
            throw new java.io.IOException("Cannot create directory : " + destinationDir);
        }

        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(source);
            out = new FileOutputStream(destination);
            writeAll(in, out);
        } finally {
            close(in);
            close(out);
        }
    }

    private boolean writeAll(File file, String text) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            writeAll(new ByteArrayInputStream(text.getBytes()), fileOutputStream);
            return true;
        } catch (Exception e) {
            addError("Unable to write to " + file.getAbsolutePath(), e);
            return false;
        } finally {
            close(fileOutputStream);
        }
    }

    private void writeAll(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int count;
        while ((count = in.read(buffer)) > 0) {
            out.write(buffer, 0, count);
        }
        out.flush();
    }

    private String readAll(File file) {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            String text = readAll(in);
            return text;
        } catch (Exception e) {
            addError("Unable to read " + file.getAbsolutePath());
            return null;
        } finally {
            close(in);
        }
    }

    private String readAll(InputStream in) throws IOException {
        // SwizzleStream block read methods are broken so read byte at a time
        StringBuilder sb = new StringBuilder();
        int i = in.read();
        while (i != -1) {
            sb.append((char) i);
            i = in.read();
        }
        return sb.toString();
    }

    private static Object invokeStaticNoArgMethod(String className, String propertyName) {
        try {
            Class<?> clazz = loadClass(className, Installer.class.getClassLoader());
            Method method = clazz.getMethod(propertyName);
            Object result = method.invoke(null, (Object[]) null);
            return result;
        } catch (Throwable e) {
            return null;
        }
    }

    private static Class<?> loadClass(String className, ClassLoader classLoader) throws ClassNotFoundException {
        LinkedList<ClassLoader> loaders = new LinkedList<ClassLoader>();
        for (ClassLoader loader = classLoader; loader != null; loader = loader.getParent()) {
            loaders.addFirst(loader);
        }
        for (ClassLoader loader : loaders) {
            try {
                Class<?> clazz = Class.forName(className, true, loader);
                return clazz;
            } catch (ClassNotFoundException e) {
            }
        }
        return null;
    }

    private void close(Closeable thing) {
        if (thing != null) {
            try {
                thing.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static class BeginEndTokenHandler extends StringTokenHandler {
        private final String begin;
        private final String end;

        public BeginEndTokenHandler(String begin, String end) {
            this.begin = begin;
            this.end = end;
        }

        public String handleToken(String token) throws IOException {
            String result = begin + token + end;
            return result;
        }
    }

    public void reset() {
        errors.clear();
        warnings.clear();
        infos.clear();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }


    public List<String> getErrors() {
        return errors;
    }

    private void addError(String message) {
        errors.add(message);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void addError(String message, Exception e) {
        // todo add exception somehow
        System.out.println(message);
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }


    public List<String> getWarnings() {
        return warnings;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private void addWarning(String message) {
        System.out.println(message);
    }

    public boolean hasInfos() {
        return !infos.isEmpty();
    }


    public List<String> getInfos() {
        return infos;
    }

    private void addInfo(String message) {
        infos.add(message);
    }
}