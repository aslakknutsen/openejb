//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.1-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2006.07.08 at 06:01:06 PM PDT 
//


package org.openejb.jee2;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 * 
 * 	The ejb-jarType defines the root element of the EJB
 * 	deployment descriptor. It contains
 * 
 * 	    - an optional description of the ejb-jar file
 * 	    - an optional display name
 * 	    - an optional icon that contains a small and a large
 * 	      icon file name
 * 	    - structural information about all included
 * 	      enterprise beans that is not specified through
 *               annotations
 *             - structural information about interceptor classes
 * 	    - a descriptor for container managed relationships,
 * 	      if any.
 * 	    - an optional application-assembly descriptor
 * 	    - an optional name of an ejb-client-jar file for the
 * 	      ejb-jar.
 * 
 *       
 * 
 * <p>Java class for ejb-jarType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ejb-jarType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;group ref="{http://java.sun.com/xml/ns/javaee}descriptionGroup"/>
 *         &lt;element name="enterprise-beans" type="{http://java.sun.com/xml/ns/javaee}enterprise-beansType" minOccurs="0"/>
 *         &lt;element name="interceptors" type="{http://java.sun.com/xml/ns/javaee}interceptorsType" minOccurs="0"/>
 *         &lt;element name="relationships" type="{http://java.sun.com/xml/ns/javaee}relationshipsType" minOccurs="0"/>
 *         &lt;element name="assembly-descriptor" type="{http://java.sun.com/xml/ns/javaee}assembly-descriptorType" minOccurs="0"/>
 *         &lt;element name="ejb-client-jar" type="{http://java.sun.com/xml/ns/javaee}pathType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;attribute name="metadata-complete" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="version" use="required" type="{http://java.sun.com/xml/ns/javaee}dewey-versionType" fixed="3.0" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ejb-jarType", propOrder = {
    "description",
    "displayName",
    "icon",
    "enterpriseBeans",
    "interceptors",
    "relationships",
    "assemblyDescriptor",
    "ejbClientJar"
})
public class EjbJarType {

    @XmlElement(required = true)
    protected List<Text> description;
    @XmlElement(name = "display-name", required = true)
    protected List<Text> displayName;
    @XmlElement(required = true)
    protected List<IconType> icon;
    @XmlElement(name = "enterprise-beans")
    protected EnterpriseBeansType enterpriseBeans;
    protected InterceptorsType interceptors;
    protected RelationshipsType relationships;
    @XmlElement(name = "assembly-descriptor")
    protected AssemblyDescriptorType assemblyDescriptor;
    @XmlElement(name = "ejb-client-jar")
    protected String ejbClientJar;
    @XmlAttribute
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    protected String id;
    @XmlAttribute(name = "metadata-complete")
    protected Boolean metadataComplete;
    @XmlAttribute(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String version;

    /**
     * Gets the value of the description property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the description property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDescription().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Text }
     * 
     * 
     */
    public List<Text> getDescription() {
        if (description == null) {
            description = new ArrayList<Text>();
        }
        return this.description;
    }

    /**
     * Gets the value of the displayName property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the displayName property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDisplayName().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Text }
     * 
     * 
     */
    public List<Text> getDisplayName() {
        if (displayName == null) {
            displayName = new ArrayList<Text>();
        }
        return this.displayName;
    }

    /**
     * Gets the value of the icon property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the icon property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getIcon().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link IconType }
     * 
     * 
     */
    public List<IconType> getIcon() {
        if (icon == null) {
            icon = new ArrayList<IconType>();
        }
        return this.icon;
    }

    public EnterpriseBeansType getEnterpriseBeans() {
        return enterpriseBeans;
    }

    public void setEnterpriseBeans(EnterpriseBeansType value) {
        this.enterpriseBeans = value;
    }

    public InterceptorsType getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(InterceptorsType value) {
        this.interceptors = value;
    }

    public RelationshipsType getRelationships() {
        return relationships;
    }

    public void setRelationships(RelationshipsType value) {
        this.relationships = value;
    }

    public AssemblyDescriptorType getAssemblyDescriptor() {
        return assemblyDescriptor;
    }

    public void setAssemblyDescriptor(AssemblyDescriptorType value) {
        this.assemblyDescriptor = value;
    }

    public String getEjbClientJar() {
        return ejbClientJar;
    }

    public void setEjbClientJar(String value) {
        this.ejbClientJar = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String value) {
        this.id = value;
    }

    public Boolean isMetadataComplete() {
        return metadataComplete;
    }

    public void setMetadataComplete(Boolean value) {
        this.metadataComplete = value;
    }

    public String getVersion() {
        if (version == null) {
            return "3.0";
        } else {
            return version;
        }
    }

    public void setVersion(String value) {
        this.version = value;
    }

}
