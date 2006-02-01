/* ====================================================================
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce this list of
 *    conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The name "OpenEJB" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of The OpenEJB Group.  For written permission,
 *    please contact openejb-group@openejb.sf.net.
 *
 * 4. Products derived from this Software may not be called "OpenEJB"
 *    nor may "OpenEJB" appear in their names without prior written
 *    permission of The OpenEJB Group. OpenEJB is a registered
 *    trademark of The OpenEJB Group.
 *
 * 5. Due credit should be given to the OpenEJB Project
 *    (http://openejb.org/).
 *
 * THIS SOFTWARE IS PROVIDED BY THE OPENEJB GROUP AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * THE OPENEJB GROUP OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the OpenEJB Project.  For more information
 * please see <http://openejb.org/>.
 *
 * ====================================================================
 */
package org.openejb.deployment;

import java.net.URI;
import java.util.Set;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import javax.naming.Reference;
import javax.management.ObjectName;

import org.apache.geronimo.j2ee.deployment.EJBReferenceBuilder;
import org.apache.geronimo.j2ee.deployment.NamingContext;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.common.UnresolvedEJBRefException;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.kernel.GBeanNotFoundException;
import org.apache.geronimo.kernel.jmx.JMXUtil;
import org.openejb.proxy.EJBProxyReference;
import org.openejb.proxy.ProxyInfo;
import org.openejb.corba.proxy.CORBAProxyReference;

/**
 * @version $Revision$ $Date$
 */
public class OpenEjbReferenceBuilder implements EJBReferenceBuilder {
    private final static ObjectName STATELESS = JMXUtil.getObjectName("*:j2eeType=StatelessSessionBean,*");
    private final static ObjectName STATEFUL = JMXUtil.getObjectName("*:j2eeType=StatefulSessionBean,*");
    private final static ObjectName ENTITY = JMXUtil.getObjectName("*:j2eeType=EntityBean,*");

    public Reference createEJBLocalReference(String objectName, GBeanData gbeanData, boolean session, String localHome, String local) throws DeploymentException {
        if (gbeanData != null) {
            // exception will be thrown if interfaces don't match
            matchesProxyInfo(gbeanData, false, localHome, local, true);
        }
        return buildLocalReference(objectName, session, localHome, local);
    }

    public Reference createEJBRemoteReference(String objectName, GBeanData gbeanData, boolean session, String home, String remote) throws DeploymentException {
        if (gbeanData != null) {
            // exception will be thrown if interfaces don't match
            matchesProxyInfo(gbeanData, true, home, remote, true);
        }
        return buildRemoteReference(objectName, session, home, remote);
    }

    public Reference createCORBAReference(URI corbaURL, String objectName, ObjectName containerName, String home) throws DeploymentException {
        return new CORBAProxyReference(corbaURL, objectName, containerName, home);
    }

    public Reference getImplicitEJBRemoteRef(URI module, String refName, boolean isSession, String home, String remote, NamingContext context) throws DeploymentException {
        boolean isRemote = true;
        ObjectName match = getImplicitMatch(isSession, context, isRemote, home, remote, refName, module);
        return buildRemoteReference(match.getCanonicalName(), isSession, home, remote);
    }

    public Reference getImplicitEJBLocalRef(URI module, String refName, boolean isSession, String localHome, String local, NamingContext context) throws DeploymentException {
        boolean isRemote = false;
        ObjectName match = getImplicitMatch(isSession, context, isRemote, localHome, local, refName, module);
        return buildLocalReference(match.getCanonicalName(), isSession, localHome, local);
    }

    protected Reference buildLocalReference(String objectName, boolean session, String localHome, String local) {
        return EJBProxyReference.createLocal(objectName, session, localHome, local);
    }

    protected Reference buildRemoteReference(String objectName, boolean session, String home, String remote) {
        return EJBProxyReference.createRemote(objectName, session, home, remote);
    }

    private ObjectName getImplicitMatch(boolean isSession, NamingContext context, boolean isRemote, String home, String remote, String refName, URI module) throws DeploymentException {
        Set gbeans;
        if (isSession) {
            gbeans = context.listGBeans(STATELESS);
            gbeans.addAll(context.listGBeans(STATEFUL));
        } else {
            gbeans = context.listGBeans(ENTITY);
        }
        Collection matches = new ArrayList();
        for (Iterator iterator = gbeans.iterator(); iterator.hasNext();) {
            ObjectName objectName = (ObjectName) iterator.next();
            GBeanData data = null;
            try {
                data = context.getGBeanInstance(objectName);
            } catch (GBeanNotFoundException e) {
                throw new DeploymentException("We just got this ejb name out of a query! It must be there!");
            }
            if (matchesProxyInfo(data, isRemote, home, remote, false)) {
                matches.add(objectName);
            }
        }
        if (matches.isEmpty()) {
            throw new UnresolvedEJBRefException(refName, false, isSession, home, remote, false);
        }
        ObjectName match;
        if (matches.size() == 1) {
            match = (ObjectName) matches.iterator().next();
        } else {
            for (Iterator iterator = matches.iterator(); iterator.hasNext();) {
                ObjectName objectName = (ObjectName) iterator.next();
                if (!(objectName.getKeyProperty(NameFactory.EJB_MODULE).equals(module.getPath()))) {
                    iterator.remove();
                }
            }
            if (matches.size() == 1) {
                match = (ObjectName) matches.iterator().next();
            } else {
                throw new UnresolvedEJBRefException(refName, false, isSession, home, remote, matches.size() > 0);
            }
        }
        return match;
    }

    private boolean matchesProxyInfo(GBeanData data, boolean isRemote, String home, String remote, boolean requiredMatch) throws DeploymentException {
        String[] proxyInfoData = getProxyInfoData(data, isRemote);
        if (matches(proxyInfoData[0], home) && matches(proxyInfoData[1], remote)) {
            return true;
        }
        if (!requiredMatch) return false;

        if (isRemote) {
            throw new DeploymentException("Reference interfaces do not match bean interfaces:\n" +
                    "reference home: " + home + "\n" +
                    "ejb home: " + proxyInfoData[0] + "\n" +
                    "reference remote: " + remote + "\n" +
                    "ejb remote: " + proxyInfoData[1]);

        } else {
            throw new DeploymentException("Reference interfaces do not match bean interfaces:\n" +
                    "reference localHome: " + home + "\n" +
                    "ejb localHome: " + proxyInfoData[0] + "\n" +
                    "reference local: " + remote + "\n" +
                    "ejb local: " + proxyInfoData[1]);
        }
    }

    private String[] getProxyInfoData(GBeanData data, boolean isRemote) {
        Object proxyInfoAttribute = data.getAttribute("proxyInfo");
        if (proxyInfoAttribute == null) {
            if (isRemote) {
                String homeInterface = (String) data.getAttribute("homeInterfaceName");
                String remoteInterface = (String) data.getAttribute("remoteInterfaceName");
                return new String[] { homeInterface, remoteInterface };
            } else {
                String localHomeInterface = (String) data.getAttribute("localHomeInterfaceName");
                String localInterface = (String) data.getAttribute("localInterfaceName");
                return new String[] { localHomeInterface, localInterface };
            }
        } else if (proxyInfoAttribute instanceof ProxyInfo) {
            ProxyInfo proxyInfo = (ProxyInfo) proxyInfoAttribute;
            if (isRemote) {
                return new String[] { getClassName(proxyInfo.getHomeInterface()), getClassName(proxyInfo.getRemoteInterface())};
            } else {
                return new String[] { getClassName(proxyInfo.getLocalHomeInterface()), getClassName(proxyInfo.getLocalInterface())};
            }
        } else {
            throw new IllegalStateException("BUG! unknown proxy info attribute type: " + proxyInfoAttribute.getClass());
        }
    }

    private static String getClassName(Class clazz) {
        if (clazz == null) return null;
        return clazz.getName();
    }

    private boolean matches(String clazz, String name) {
        return clazz != null && clazz.equals(name);
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoBuilder infoFactory = GBeanInfoBuilder.createStatic(OpenEjbReferenceBuilder.class, NameFactory.MODULE_BUILDER); //TODO decide what type this should be
        infoFactory.addInterface(EJBReferenceBuilder.class);

        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }

}
