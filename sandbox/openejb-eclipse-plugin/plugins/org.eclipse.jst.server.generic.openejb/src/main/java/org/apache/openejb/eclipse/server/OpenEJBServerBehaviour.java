/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.openejb.eclipse.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleFile;
import org.eclipse.wst.server.core.model.IModuleFolder;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.ProjectModule;

public class OpenEJBServerBehaviour extends ServerBehaviourDelegate {

	@SuppressWarnings("serial")
	public class ServerStoppedException extends Exception {
	}

	private class ServerMonitor extends Thread {

		private static final int ONE_SECOND = 1000;
		private boolean running = false;

		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(ONE_SECOND);
				} catch (InterruptedException e) {
					stopServer();
				}

				try {
					check();
				} catch (ServerStoppedException e) {
					// break out the loop and stop monitoring
					// a restart will start a new monitor
					break;
				}
			}
		}

		private void check() throws ServerStoppedException {
			// connect to admin interface
			try {
				Socket socket = new Socket("localhost", 4200);
				socket.close();
				
				// update the server status if this is first time we've connected
				if (! running) {
					running = true;
					setState(IServer.STATE_STARTED);
				}
			} catch (IOException e) {
				if (running) {
					// looks like server has started successfully, but has died
					setServerState(IServer.STATE_STOPPED);
					running = false;
				}
				// server might not be started yet
			}
			// if success, server is running
		}

		/*
		 * @see org.apache.openejb.server.admin.AdminDaemon.service(Socket socket)
		 */
		private void stopServer() {
			// connect to admin interface, and send 'Q' to stop the server
			try {
				Socket socket = new Socket("localhost", 4200);
				socket.getOutputStream().write('Q');
				socket.close();
				
				setState(IServer.STATE_STOPPING);
			} catch (IOException e) {
				// we're really stuck
			}
		}
		
		public void terminate() {
			this.interrupt();
		}

		public ServerMonitor() {
			super();
		}
	}
	
	private ServerMonitor monitor;
	
	@Override
	public void stop(boolean force) {
		if (monitor == null) {
			return;
		}
		
		monitor.terminate();
	}

	@Override
	public void setupLaunchConfiguration(ILaunchConfigurationWorkingCopy workingCopy, IProgressMonitor monitor) throws CoreException {
		workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, "org.apache.openejb.cli.Bootstrap");

		OpenEJBRuntimeDelegate runtime = getRuntimeDelegate();

		IVMInstall vmInstall = runtime.getVMInstall();
		if (vmInstall != null)
			workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, JavaRuntime.newJREContainerPath(vmInstall).toPortableString());

		workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, "start");
		workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "-Dopenejb.home=" +  getServer().getRuntime().getLocation().toString() /*+ " -javaagent:" + runtime.getJavaAgent()*/);
		
		List<IRuntimeClasspathEntry> cp = new ArrayList<IRuntimeClasspathEntry>();
		IPath serverJar = new Path(runtime.getCore());
		cp.add(JavaRuntime.newArchiveRuntimeClasspathEntry(serverJar));
		
		List<String> classPath = new ArrayList<String>();
		for (IRuntimeClasspathEntry entry : cp) {
			classPath.add(entry.getMemento());
		}
		
		workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, classPath);
		workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, false);
	}

	private OpenEJBRuntimeDelegate getRuntimeDelegate() {
		OpenEJBRuntimeDelegate rd = (OpenEJBRuntimeDelegate) getServer().getRuntime().getAdapter(OpenEJBRuntimeDelegate.class);
		if (rd == null)
			rd = (OpenEJBRuntimeDelegate) getServer().getRuntime().loadAdapter(OpenEJBRuntimeDelegate.class, new NullProgressMonitor());
		return rd;
	}

	public void setState(int state) {
		setServerState(state);
	}
	
	public void start(ILaunch launch) {
		monitor = new ServerMonitor();
		monitor.start();
	}

	@Override
	protected IStatus publishModule(int kind, IModule[] modules, int deltaKind, IProgressMonitor monitor) {
		IPath appsDir = getRuntimeDelegate().getRuntime().getLocation().append("/apps");
		
		if (deltaKind == REMOVED) {
			StringBuffer removedMsg = new StringBuffer();
			
			for (IModule module : modules) {
				File target = new File(appsDir.toFile(), module.getName() + ".jar");
				if (target.exists() && target.isFile()) {
					boolean deleted = target.delete();
					
					if (! deleted) {
						removedMsg.append("Unable to delete module: " + target.getAbsolutePath() + " from server\n");
					}
				}
			}
			
			if (removedMsg.length() == 0) {
				return new Status(IStatus.OK, "org.eclipse.jst.server.generic.openejb", "");
			} else {
				return new Status(IStatus.WARNING, "org.eclipse.jst.server.generic.openejb", removedMsg.toString());
			}
		}
		
		try {
			for (IModule module : modules) {
				ProjectModule projectModule = (ProjectModule) module.loadAdapter(ProjectModule.class, null);
				if (projectModule == null) {
					continue;
				}
				
				IModuleResource[] members = projectModule.members();

				try {
					File tempJarFile = File.createTempFile("oejb", ".jar");
					ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempJarFile));
			
					
					for (IModuleResource resource : members) {
						IPath projectLocation = module.getProject().getLocation();
						
						writeResourceToZipStream(zos, resource, projectLocation);
						 
					}
					
					zos.close();
					
					File target = new File(appsDir.toFile(), module.getName() + ".jar");
					
					tempJarFile.renameTo(target);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return super.publishModule(kind, modules, deltaKind, monitor);
	}

	private void writeResourceToZipStream(ZipOutputStream zipStream, IModuleResource resource, IPath projectLocation) throws IOException, FileNotFoundException {
		byte[] buffer = new byte[8192];
		if (resource instanceof IModuleFile) {
			IPath relativePath = resource.getModuleRelativePath().append("/" + resource.getName());

			try {
				Field fileField = resource.getClass().getDeclaredField("file");
				fileField.setAccessible(true);
				IFile obj = (IFile) fileField.get(resource);
				InputStream is = obj.getContents();

				ZipEntry zipEntry = new ZipEntry(relativePath.toString());
				zipStream.putNextEntry(zipEntry);

				int bytesRead = -1;
				while ((bytesRead = is.read(buffer)) > 0) {
					zipStream.write(buffer, 0, bytesRead);
				}

				is.close();
				zipStream.closeEntry();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (resource instanceof IModuleFolder) {
			IModuleResource[] resources = ((IModuleFolder) resource).members();
			for (IModuleResource childResource : resources) {
				writeResourceToZipStream(zipStream, childResource, projectLocation);
			}
		}
	}
}