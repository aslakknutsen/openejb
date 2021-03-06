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
package org.apache.openejb.tomcat.catalina;

import org.apache.catalina.Container;
import org.apache.catalina.Engine;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Service;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.deploy.ContextEnvironment;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.deploy.ContextResourceLink;
import org.apache.catalina.deploy.NamingResources;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.HostConfig;
import org.apache.naming.ContextAccessController;
import org.apache.naming.ContextBindings;
import org.apache.openejb.Injection;
import org.apache.openejb.OpenEJBException;
import org.apache.openejb.assembler.classic.AppInfo;
import org.apache.openejb.assembler.classic.Assembler;
import org.apache.openejb.assembler.classic.ConnectorInfo;
import org.apache.openejb.assembler.classic.EjbJarInfo;
import org.apache.openejb.assembler.classic.InjectionBuilder;
import org.apache.openejb.assembler.classic.WebAppBuilder;
import org.apache.openejb.assembler.classic.WebAppInfo;
import org.apache.openejb.config.AppModule;
import org.apache.openejb.config.ConfigurationFactory;
import org.apache.openejb.config.DeploymentLoader;
import org.apache.openejb.config.WebModule;
import org.apache.openejb.core.CoreContainerSystem;
import org.apache.openejb.core.WebContext;
import org.apache.openejb.core.ivm.naming.SystemComponentReference;
import org.apache.openejb.jee.EnvEntry;
import org.apache.openejb.jee.WebApp;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.server.webservices.WsService;
import org.apache.openejb.tomcat.common.LegacyAnnotationProcessor;
import org.apache.openejb.tomcat.common.TomcatVersion;
import org.apache.openejb.tomcat.loader.TomcatHelper;
import org.apache.openejb.util.LinkResolver;
import org.apache.openejb.util.LogCategory;
import org.apache.openejb.util.Logger;
import org.omg.CORBA.ORB;

import javax.ejb.spi.HandleDelegate;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletContext;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.apache.openejb.tomcat.catalina.BackportUtil.getNamingContextListener;

/**
 * Web application builder.
 *
 * @version $Rev$ $Date$
 */
public class TomcatWebAppBuilder implements WebAppBuilder, ContextListener {

    /**
     * Flag for ignore context
     */
    public static final String IGNORE_CONTEXT = TomcatWebAppBuilder.class.getName() + ".IGNORE";
    /**
     * Logger instance
     */
    private static final Logger logger = Logger.getInstance(LogCategory.OPENEJB.createChild("tomcat"), "org.apache.openejb.util.resources");
    /**
     * Context information for web applications
     */
    private final TreeMap<String, ContextInfo> infos = new TreeMap<String, ContextInfo>();
    /**
     * Global listener for Tomcat fired events.
     */
    private final GlobalListenerSupport globalListenerSupport;
    /**
     * OpenEJB configuration factory instance
     */
    private final ConfigurationFactory configurationFactory;
    /**
     * Tomcat host config elements
     */
    //Key is the host name
    private final Map<String, HostConfig> deployers = new TreeMap<String, HostConfig>();
    /**
     * Deployed web applications
     */
    // todo merge this map witth the infos map above
    private final Map<String, DeployedApplication> deployedApps = new TreeMap<String, DeployedApplication>();
    /**
     * OpenEJB deployment loader instance
     */
    private final DeploymentLoader deploymentLoader;
    /**
     * OpenEJB assembler instance
     * TODO can we use the SPI interface instead?
     */
    private Assembler assembler;
    /**
     * OpenEJB container system
     * TODO can we use the SPI interface instead?
     */
    private CoreContainerSystem containerSystem;

    /**
     * WsService
     */
    private WsService wsService;

    /**
     * Creates a new web application builder
     * instance.
     */
    public TomcatWebAppBuilder() {
    	
    	// TODO: re-write this bit, so this becomes part of the listener, and we register this with the mbean server.
    	
        StandardServer standardServer = TomcatHelper.getServer();
        globalListenerSupport = new GlobalListenerSupport(standardServer, this);

        // could search mbeans
        
        //Getting host config listeners
        for (Service service : standardServer.findServices()) {
            if (service.getContainer() instanceof Engine) {
                Engine engine = (Engine) service.getContainer();
                for (Container engineChild : engine.findChildren()) {
                    if (engineChild instanceof StandardHost) {
                        StandardHost host = (StandardHost) engineChild;
                        for (LifecycleListener listener : host.findLifecycleListeners()) {
                            if (listener instanceof HostConfig) {
                                HostConfig hostConfig = (HostConfig) listener;
                                deployers.put(host.getName(), hostConfig);
                            }
                        }
                    }
                }
            }
        }

        configurationFactory = new ConfigurationFactory();
        deploymentLoader = new DeploymentLoader();
    }

    /**
     * Start operation.
     */
    public void start() {
        globalListenerSupport.start();

    }

    /**
     * Stop operation.
     */
    public void stop() {
        globalListenerSupport.stop();
    }

    //
    // OpenEJB WebAppBuilder
    //
    /**
     * {@inheritDoc}
     */
    @Override
    public void deployWebApps(AppInfo appInfo, ClassLoader classLoader) throws Exception {
        for (WebAppInfo webApp : appInfo.webApps) {
            if (getContextInfo(webApp) == null) {
                StandardContext standardContext = new StandardContext();
                String s = File.pathSeparator;
                File contextXmlFile = new File(webApp.path + s + "META-INF" + s + "context.xml");
                if (contextXmlFile.exists()) {
                    BackportUtil.getAPI().setConfigFile(standardContext, contextXmlFile);
                    standardContext.setOverride(true);
                }
                ContextConfig contextConfig = new ContextConfig();
                standardContext.addLifecycleListener(contextConfig);

                standardContext.setPath("/" + webApp.contextRoot);
                standardContext.setDocBase(webApp.path);
                standardContext.setParentClassLoader(classLoader);
                standardContext.setDelegate(true);

                String host = webApp.host;
                if (host == null) {
                    host = "localhost";
                }
                
                // TODO: instead of storing deployers, we could just lookup the right hostconfig for the server.
                HostConfig deployer = deployers.get(host);
                if (deployer != null) {
                    // host isn't set until we call deployer.manageApp, so pass it
                    ContextInfo contextInfo = addContextInfo(host, standardContext);
                    contextInfo.appInfo = appInfo;
                    contextInfo.deployer = deployer;
                    contextInfo.standardContext = standardContext;
                    deployer.manageApp(standardContext);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undeployWebApps(AppInfo appInfo) throws Exception {
        for (WebAppInfo webApp : appInfo.webApps) {
            ContextInfo contextInfo = getContextInfo(webApp);
            if (contextInfo != null && contextInfo.deployer != null) {
                StandardContext standardContext = contextInfo.standardContext;
                HostConfig deployer = contextInfo.deployer;
                deployer.unmanageApp(standardContext.getPath());
                deleteDir(new File(standardContext.getServletContext().getRealPath("")));
                removeContextInfo(standardContext);
            }
        }
    }

    /**
     * Deletes given directory.
     *
     * @param dir directory
     */
    private void deleteDir(File dir) {
        if (dir == null) {
            return;
        }
        if (dir.isFile()) {
            return;
        }
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                deleteDir(file);
            } else {
                file.delete();
            }
        }
        dir.delete();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(StandardContext standardContext) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeStart(StandardContext standardContext) {
    }

    /**
     * {@inheritDoc}
     */
    // context class loader is now defined, but no classes should have been loaded
    @SuppressWarnings("unchecked")
    @Override
    public void start(StandardContext standardContext) {
        if (isIgnored(standardContext)) return;

        Assembler a = getAssembler();
        if (a == null) {
            logger.warning("OpenEJB has not been initialized so war will not be scanned for nested modules " + standardContext.getPath());
            return;
        }

        //Look for context info, maybe context is already scanned
        ContextInfo contextInfo = getContextInfo(standardContext);
        if (contextInfo == null) {
            AppModule appModule = loadApplication(standardContext);
            if (appModule != null) {
                try {
                    contextInfo = addContextInfo(standardContext.getHostname(), standardContext);
                    AppInfo appInfo = configurationFactory.configureApplication(appModule);
                    contextInfo.appInfo = appInfo;

                    a.createApplication(contextInfo.appInfo, standardContext.getLoader().getClassLoader());
                    // todo add watched resources to context
                } catch (Exception e) {
                    logger.error("Unable to deploy collapsed ear in war " + standardContext.getPath() + ": Exception: " + e.getMessage(), e);
                }
            }
        }

        contextInfo.standardContext = standardContext;

        WebAppInfo webAppInfo = null;
        // appInfo is null when deployment fails
        if (contextInfo.appInfo != null) {
            for (WebAppInfo w : contextInfo.appInfo.webApps) {
                if (("/" + w.contextRoot).equals(standardContext.getPath()) || isRootApplication(standardContext)) {
                    webAppInfo = w;
                    break;
                }
            }
        }

        if (webAppInfo != null) {
            try {
                // determind the injections
                InjectionBuilder injectionBuilder = new InjectionBuilder(standardContext.getLoader().getClassLoader());
                List<Injection> injections = injectionBuilder.buildInjections(webAppInfo.jndiEnc);

                // merge OpenEJB jndi into Tomcat jndi
                TomcatJndiBuilder jndiBuilder = new TomcatJndiBuilder(standardContext, webAppInfo, injections);
                jndiBuilder.mergeJndi();

                // add WebDeploymentInfo to ContainerSystem
                WebContext webContext = new WebContext();
                webContext.setId(webAppInfo.moduleId);
                webContext.setClassLoader(standardContext.getLoader().getClassLoader());
                webContext.getInjections().addAll(injections);
                getContainerSystem().addWebDeployment(webContext);
            } catch (Exception e) {
                logger.error("Error merging OpenEJB JNDI entries in to war " + standardContext.getPath() + ": Exception: " + e.getMessage(), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterStart(StandardContext standardContext) {
        if (isIgnored(standardContext)) return;

        // if appInfo is null this is a failed deployment... just ignore
        ContextInfo contextInfo = getContextInfo(standardContext);
        if (contextInfo != null && contextInfo.appInfo == null) {
            return;
        }

        // required for Pojo Web Services because when Assembler creates the application
        // the CoreContainerSystem does not contain the WebContext
        // see also the start method getContainerSystem().addWebDeployment(webContext);
        WsService wsService = getWsService();
        if (wsService != null) {
            List<WebAppInfo> webApps = contextInfo.appInfo.webApps;
            for (WebAppInfo webApp : webApps) {
                wsService.afterApplicationCreated(webApp);
            }
        }

        // bind extra stuff at the java:comp level which can only be
        // bound after the context is created
        String listenerName = getNamingContextListener(standardContext).getName();
        ContextAccessController.setWritable(listenerName, standardContext);
        try {

            Context openejbContext = getContainerSystem().getJNDIContext();
            openejbContext = (Context) openejbContext.lookup("openejb");

            Context root = (Context) ContextBindings.getClassLoader().lookup("");
            safeBind(root, "openejb", openejbContext);

            Context comp = (Context) ContextBindings.getClassLoader().lookup("comp");

            // add context to WebDeploymentInfo
            for (WebAppInfo webAppInfo : contextInfo.appInfo.webApps) {
                // Bean Validation
                standardContext.getServletContext().setAttribute("javax.faces.validator.beanValidator.ValidatorFactory", openejbContext.lookup(Assembler.VALIDATOR_FACTORY_NAMING_CONTEXT + webAppInfo.moduleId));

                if (("/" + webAppInfo.contextRoot).equals(standardContext.getPath()) || isRootApplication(standardContext)) {
                    WebContext webContext = getContainerSystem().getWebContext(webAppInfo.moduleId);
                    if (webContext != null) {
                        webContext.setJndiEnc(comp);
                    }
                    break;
                }
            }

            // bind TransactionManager
            TransactionManager transactionManager = SystemInstance.get().getComponent(TransactionManager.class);
            safeBind(comp, "TransactionManager", transactionManager);

            // bind TransactionSynchronizationRegistry
            TransactionSynchronizationRegistry synchronizationRegistry = SystemInstance.get().getComponent(TransactionSynchronizationRegistry.class);
            safeBind(comp, "TransactionSynchronizationRegistry", synchronizationRegistry);

            safeBind(comp, "ORB", new SystemComponentReference(ORB.class));
            safeBind(comp, "HandleDelegate", new SystemComponentReference(HandleDelegate.class));
        } catch (NamingException e) {
        }
        ContextAccessController.setReadOnly(listenerName);

        if (!TomcatVersion.hasAnnotationProcessingSupport()) {
            try {
                Context compEnv = (Context) ContextBindings.getClassLoader().lookup("comp/env");

                LegacyAnnotationProcessor annotationProcessor = new LegacyAnnotationProcessor(compEnv);

                standardContext.addContainerListener(new ProcessAnnotatedListenersListener(annotationProcessor));

                for (Container container : standardContext.findChildren()) {
                    if (container instanceof Wrapper) {
                        Wrapper wrapper = (Wrapper) container;
                        wrapper.addInstanceListener(new ProcessAnnotatedServletsListener(annotationProcessor));
                    }
                }
            } catch (NamingException e) {
            }
        }
        OpenEJBValve openejbValve = new OpenEJBValve();
        standardContext.getPipeline().addValve(openejbValve);
    }

    private static boolean isIgnored(StandardContext standardContext) {
        // useful to disable web applications deployment
        // it can be placed in the context.xml file, server.xml, ...
        // see http://tomcat.apache.org/tomcat-5.5-doc/config/context.html#Context_Parameters
        if (standardContext.getServletContext().getAttribute(IGNORE_CONTEXT) != null) return true;
        if (standardContext.getServletContext().getInitParameter(IGNORE_CONTEXT) != null) return true;

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeStop(StandardContext standardContext) {
        //No operation
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(StandardContext standardContext) {
        //No operation
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterStop(StandardContext standardContext) {
        if (isIgnored(standardContext)) return;

        ContextInfo contextInfo = getContextInfo(standardContext);
        if (contextInfo != null && contextInfo.appInfo != null && contextInfo.deployer == null) {
            try {
                getAssembler().destroyApplication(contextInfo.appInfo.path);
            } catch (Exception e) {
                logger.error("Unable to stop web application " + standardContext.getPath() + ": Exception: " + e.getMessage(), e);
            }
        }
        removeContextInfo(standardContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy(StandardContext standardContext) {
        //No operation
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterStop(StandardServer standardServer) {
        // clean ear based webapps after shutdown
        for (ContextInfo contextInfo : infos.values()) {
            if (contextInfo != null && contextInfo.deployer != null) {
                StandardContext standardContext = contextInfo.standardContext;
                HostConfig deployer = contextInfo.deployer;
                deployer.unmanageApp(standardContext.getPath());
                String realPath = standardContext.getServletContext().getRealPath("");
                if (realPath != null) {
                    deleteDir(new File(realPath));
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkHost(StandardHost standardHost) {
        if (standardHost.getAutoDeploy()) {
            // Undeploy any modified application
            for (Iterator<Map.Entry<String, DeployedApplication>> iterator = deployedApps.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<String, DeployedApplication> entry = iterator.next();
                DeployedApplication deployedApplication = entry.getValue();
                if (deployedApplication.isModified()) {
                    try {
                        getAssembler().destroyApplication(deployedApplication.appInfo.path);
                    } catch (Exception e) {
                        logger.error("Unable to application " + deployedApplication.appInfo.path + ": Exception: " + e.getMessage(), e);
                    }
                    iterator.remove();
                }
            }

            // Deploy new applications
            File appBase = appBase(standardHost);
            File[] files = appBase.listFiles();
            for (File file : files) {
                String name = file.getName();
                // ignore war files
                if (name.toLowerCase().endsWith(".war") || name.equals("ROOT") || name.equalsIgnoreCase("META-INF") || name.equalsIgnoreCase("WEB-INF")) {
                    continue;
                }
                // ignore unpacked web apps
                if (file.isDirectory() && new File(file, "WEB-INF").exists()) {
                    continue;
                }
                // ignore unpacked apps where packed version is present (packed version is owner)
                if (file.isDirectory() && (new File(file.getParent(), file.getName() + ".ear").exists()
                        || new File(file.getParent(), file.getName() + ".war").exists()
                        || new File(file.getParent(), file.getName() + ".rar").exists())) {
                    continue;
                }
                // ignore already deployed apps
                if (isDeployed(file, standardHost)) {
                    continue;
                }

                AppInfo appInfo = null;
                try {
                    file = file.getCanonicalFile().getAbsoluteFile();

                    AppModule appModule = deploymentLoader.load(file);

                    // Ignore any standalone web modules - this happens when the app is unpaked and doesn't have a WEB-INF dir
                    if (appModule.getDeploymentModule().size() == 1 && appModule.getWebModules().size() == 1) {
                        WebModule webModule = appModule.getWebModules().iterator().next();
                        if (file.getAbsolutePath().equals(webModule.getJarLocation())) {
                            continue;
                        }
                    }

                    // if this is an unpacked dir, tomcat will pick it up as a webapp so undeploy it first
                    if (file.isDirectory()) {
                        ContainerBase context = (ContainerBase) standardHost.findChild("/" + name);
                        if (context != null) {
                            try {
                                standardHost.removeChild(context);
                            } catch (Throwable t) {
                                logger.warning("Error undeploying wep application from Tomcat  " + name, t);
                            }
                            try {
                                context.destroy();
                            } catch (Throwable t) {
                                logger.warning("Error destroying Tomcat web context " + name, t);
                            }
                        }
                    }

                    // tell web modules to deploy using this host
                    for (WebModule webModule : appModule.getWebModules()) {
                        webModule.setHost(standardHost.getName());
                    }

                    appInfo = configurationFactory.configureApplication(appModule);
                    getAssembler().createApplication(appInfo);
                } catch (Throwable e) {
                    logger.warning("Error deploying application " + file.getAbsolutePath(), e);
                }
                deployedApps.put(file.getAbsolutePath(), new DeployedApplication(file, appInfo));
            }
        }
    }

    /**
     * Returns true if given application is deployed
     * false otherwise.
     *
     * @param file         web application file
     * @param standardHost host
     * @return true if given application is deployed
     */
    private boolean isDeployed(File file, StandardHost standardHost) {
        if (deployedApps.containsKey(file.getAbsolutePath())) {
            return true;
        }

        // check if this is a deployed web application
        String name = "/" + file.getName();

        // ROOT context is a special case
        if (name.equals("/ROOT")) {
            name = "";
        }

        return file.isFile() && standardHost.findChild(name) != null;
    }

    /**
     * Returns true if given context is root web appliction
     * false otherwise.
     *
     * @param standardContext tomcat context
     * @return true if given context is root web appliction
     */
    private boolean isRootApplication(StandardContext standardContext) {
        return "".equals(standardContext.getPath());
    }

    /**
     * Returns application base of the given host.
     *
     * @param standardHost tomcat host
     * @return application base of the given host
     */
    protected File appBase(StandardHost standardHost) {
        File file = new File(standardHost.getAppBase());
        if (!file.isAbsolute()) {
            file = new File(System.getProperty("catalina.base"), standardHost.getAppBase());
        }
        try {
            file = file.getCanonicalFile();
        } catch (IOException e) {
        }
        return file;
    }

    /**
     * Creates an openejb {@link AppModule} instance
     * from given tomcat context.
     *
     * @param standardContext tomcat context instance
     * @return a openejb application module
     */
    private AppModule loadApplication(StandardContext standardContext) {
        ServletContext servletContext = standardContext.getServletContext();

        TomcatDeploymentLoader tomcatDeploymentLoader = new TomcatDeploymentLoader(standardContext, getId(standardContext));
        AppModule appModule = null;
        try {
            appModule = tomcatDeploymentLoader.load(new File(servletContext.getRealPath(".")).getParentFile());
        } catch (OpenEJBException e) {
            throw new RuntimeException(e);
        }

        // create the web module
        loadWebModule(appModule, standardContext);

        return appModule;
    }

    /**
     * Creates a new {@link WebModule} instance from given
     * tomcat context instance.
     *
     * @param standardContext tomcat context instance
     * @return a openejb web module
     */
    private void loadWebModule(AppModule appModule, StandardContext standardContext) {
        WebModule webModule = appModule.getWebModules().get(0);
        WebApp webApp = webModule.getWebApp();

        // create the web module
        String path = standardContext.getPath();
        System.out.println("context path = " + path);
        webModule.setHost(standardContext.getHostname());
        // Add all Tomcat env entries to context so they can be overriden by the env.properties file
        NamingResources naming = standardContext.getNamingResources();
        for (ContextEnvironment environment : naming.findEnvironments()) {
            EnvEntry envEntry = webApp.getEnvEntryMap().get(environment.getName());
            if (envEntry == null) {
                envEntry = new EnvEntry();
                envEntry.setName(environment.getName());
                webApp.getEnvEntry().add(envEntry);
            }

            envEntry.setEnvEntryValue(environment.getValue());
            envEntry.setEnvEntryType(environment.getType());
        }

        // remove all jndi entries where there is a configured Tomcat resource or resource-link
        for (ContextResource resource : naming.findResources()) {
            String name = resource.getName();
            removeRef(webApp, name);
        }
        for (ContextResourceLink resourceLink : naming.findResourceLinks()) {
            String name = resourceLink.getName();
            removeRef(webApp, name);
        }

        // remove all env entries from the web xml that are not overridable
        for (ContextEnvironment environment : naming.findEnvironments()) {
            if (!environment.getOverride()) {
                // overrides are not allowed
                webApp.getEnvEntryMap().remove(environment.getName());
            }
        }

    }

    /**
     * Remove jndi references from related info map.
     *
     * @param webApp web application instance
     * @param name   jndi reference name
     */
    private void removeRef(WebApp webApp, String name) {
        webApp.getEnvEntryMap().remove(name);
        webApp.getEjbRefMap().remove(name);
        webApp.getEjbLocalRefMap().remove(name);
        webApp.getMessageDestinationRefMap().remove(name);
        webApp.getPersistenceContextRefMap().remove(name);
        webApp.getPersistenceUnitRefMap().remove(name);
        webApp.getResourceRefMap().remove(name);
        webApp.getResourceEnvRefMap().remove(name);
    }

    /**
     * Binds given object into given component context.
     *
     * @param comp  context
     * @param name  name of the binding
     * @param value binded object
     */
    private void safeBind(Context comp, String name, Object value) {
        try {
        	Object lookup = null;
        	
        	try {
				lookup = comp.lookup(name);
			} catch (Exception e) {
			}
			
			if (lookup != null) {
				logger.info(name + " already bound, ignoring");
				return;
			}
			
            comp.bind(name, value);
        } catch (NamingException e) {
            logger.error("Error in safeBind method", e);
        }
    }

    /**
     * Gets openejb assembler instance.
     *
     * @return assembler
     */
    private Assembler getAssembler() {
        if (assembler == null) {
            assembler = (Assembler) SystemInstance.get().getComponent(org.apache.openejb.spi.Assembler.class);
        }
        return assembler;
    }

    /**
     * Gets container system for openejb.
     *
     * @return openejb container system
     */
    private CoreContainerSystem getContainerSystem() {
        if (containerSystem == null) {
            containerSystem = (CoreContainerSystem) SystemInstance.get().getComponent(org.apache.openejb.spi.ContainerSystem.class);
        }
        return containerSystem;
    }

    /**
     * Gets WsService implementation.
     *
     * @return wsService
     */
    private WsService getWsService() {
        if (wsService == null) {
            wsService = SystemInstance.get().getComponent(WsService.class);
        }
        return wsService;
    }

    /**
     * Gets id of the context. Context id
     * is host name + context root name.
     *
     * @param standardContext context instance
     * @return id of the context
     */
    private String getId(StandardContext standardContext) {
        String contextRoot = standardContext.getName();
        if (!contextRoot.startsWith("/")) {
            contextRoot = "/" + contextRoot;
        }
        return standardContext.getHostname() + contextRoot;
    }

    /**
     * Gets context info for given context.
     *
     * @param standardContext context
     * @return context info
     */
    private ContextInfo getContextInfo(StandardContext standardContext) {
        String id = getId(standardContext);
        ContextInfo contextInfo = infos.get(id);
        return contextInfo;
    }

    /**
     * Gets context info for given web app info.
     *
     * @param webAppInfo web application info
     * @return context info
     */
    private ContextInfo getContextInfo(WebAppInfo webAppInfo) {
        String host = webAppInfo.host;
        if (host == null) {
            host = "localhost";
        }
        String contextRoot = webAppInfo.contextRoot;
        String id = host + "/" + contextRoot;
        ContextInfo contextInfo = infos.get(id);
        return contextInfo;
    }

    /**
     * Add new context info.
     *
     * @param host            host name
     * @param standardContext context
     * @return context info
     */
    private ContextInfo addContextInfo(String host, StandardContext standardContext) {
        String contextRoot = standardContext.getName();
        if (!contextRoot.startsWith("/")) {
            contextRoot = "/" + contextRoot;
        }
        String id = host + contextRoot;
        ContextInfo contextInfo = infos.get(id);
        if (contextInfo == null) {
            contextInfo = new ContextInfo();
            infos.put(id, contextInfo);
        }
        return contextInfo;
    }

    /**
     * Removes context info from map.
     *
     * @param standardContext context
     */
    private void removeContextInfo(StandardContext standardContext) {
        String id = getId(standardContext);
        infos.remove(id);
    }

    private static class ContextInfo {

        public AppInfo appInfo;
        public StandardContext standardContext;
        public HostConfig deployer;
        public LinkResolver<EntityManagerFactory> emfLinkResolver;
    }

    private static class DeployedApplication {

        private AppInfo appInfo;
        private final Map<File, Long> watchedResource = new HashMap<File, Long>();

        public DeployedApplication(File base, AppInfo appInfo) {
            this.appInfo = appInfo;
            watchedResource.put(base, base.lastModified());
            if (appInfo != null) {
                for (String resource : appInfo.watchedResources) {
                    File file = new File(resource);
                    watchedResource.put(file, file.lastModified());
                }
                for (EjbJarInfo info : appInfo.ejbJars) {
                    for (String resource : info.watchedResources) {
                        File file = new File(resource);
                        watchedResource.put(file, file.lastModified());
                    }
                }
                for (WebAppInfo info : appInfo.webApps) {
                    for (String resource : info.watchedResources) {
                        File file = new File(resource);
                        watchedResource.put(file, file.lastModified());
                    }
                }
                for (ConnectorInfo info : appInfo.connectors) {
                    for (String resource : info.watchedResources) {
                        File file = new File(resource);
                        watchedResource.put(file, file.lastModified());
                    }
                }
            }
        }

        public boolean isModified() {
            for (Map.Entry<File, Long> entry : watchedResource.entrySet()) {
                File file = entry.getKey();
                long lastModified = entry.getValue();
                if ((!file.exists() && lastModified != 0L)
                        || (file.lastModified() != lastModified)) {
                    return true;
                }
            }
            return false;
        }
    }
}
