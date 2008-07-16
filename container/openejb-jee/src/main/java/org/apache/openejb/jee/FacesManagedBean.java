/**
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
//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-558 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2008.07.13 at 11:14:43 PM EDT 
//


package org.apache.openejb.jee;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 *                 
 *                 The "managed-bean" element represents a JavaBean, of a 
 *                 particular class, that will be dynamically instantiated 
 *                 at runtime (by the default VariableResolver implementation) 
 *                 if it is referenced as the first element of a value binding 
 *                 expression, and no corresponding bean can be identified in 
 *                 any scope.  In addition to the creation of the managed bean, 
 *                 and the optional storing of it into the specified scope, 
 *                 the nested managed-property elements can be used to 
 *                 initialize the contents of settable JavaBeans properties of 
 *                 the created instance.
 *                 
 *             
 * 
 * <p>Java class for faces-config-managed-beanType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="faces-config-managed-beanType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;group ref="{http://java.sun.com/xml/ns/javaee}descriptionGroup"/>
 *         &lt;element name="managed-bean-name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="managed-bean-class" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="managed-bean-scope" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;choice>
 *           &lt;element name="managed-property" type="{http://java.sun.com/xml/ns/javaee}faces-config-managed-propertyType" maxOccurs="unbounded" minOccurs="0"/>
 *           &lt;element name="map-entries" type="{http://java.sun.com/xml/ns/javaee}faces-config-map-entriesType"/>
 *           &lt;element name="list-entries" type="{http://java.sun.com/xml/ns/javaee}faces-config-list-entriesType"/>
 *         &lt;/choice>
 *         &lt;element name="managed-bean-extension" type="{http://java.sun.com/xml/ns/javaee}faces-config-managed-bean-extensionType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "faces-config-managed-beanType", propOrder = {
    "description",
    "displayName",
    "icon",
    "managedBeanName",
    "managedBeanClass",
    "managedBeanScope",
    "managedProperty",
    "mapEntries",
    "listEntries",
    "managedBeanExtension"
})
public class FacesManagedBean {

    protected List<DescriptionType> description;
    @XmlElement(name = "display-name")
    protected List<java.lang.String> displayName;
    protected List<Icon> icon;
    @XmlElement(name = "managed-bean-name", required = true)
    protected java.lang.String managedBeanName;
    @XmlElement(name = "managed-bean-class", required = true)
    protected java.lang.String managedBeanClass;
    @XmlElement(name = "managed-bean-scope", required = true)
    protected java.lang.String managedBeanScope;
    @XmlElement(name = "managed-property")
    protected List<FacesManagedProperty> managedProperty;
    @XmlElement(name = "map-entries")
    protected FacesMapEntries mapEntries;
    @XmlElement(name = "list-entries")
    protected FacesListEntries listEntries;
    @XmlElement(name = "managed-bean-extension")
    protected List<FacesManagedBeanExtension> managedBeanExtension;
    @XmlAttribute
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected java.lang.String id;

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
     * {@link DescriptionType }
     * 
     * 
     */
    public List<DescriptionType> getDescription() {
        if (description == null) {
            description = new ArrayList<DescriptionType>();
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
     * {@link java.lang.String }
     * 
     * 
     */
    public List<java.lang.String> getDisplayName() {
        if (displayName == null) {
            displayName = new ArrayList<java.lang.String>();
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
     * {@link Icon }
     * 
     * 
     */
    public List<Icon> getIcon() {
        if (icon == null) {
            icon = new ArrayList<Icon>();
        }
        return this.icon;
    }

    /**
     * Gets the value of the managedBeanName property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String }
     *     
     */
    public java.lang.String getManagedBeanName() {
        return managedBeanName;
    }

    /**
     * Sets the value of the managedBeanName property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String }
     *     
     */
    public void setManagedBeanName(java.lang.String value) {
        this.managedBeanName = value;
    }

    /**
     * Gets the value of the managedBeanClass property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String }
     *     
     */
    public java.lang.String getManagedBeanClass() {
        return managedBeanClass;
    }

    /**
     * Sets the value of the managedBeanClass property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String }
     *     
     */
    public void setManagedBeanClass(java.lang.String value) {
        this.managedBeanClass = value;
    }

    /**
     * Gets the value of the managedBeanScope property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String }
     *     
     */
    public java.lang.String getManagedBeanScope() {
        return managedBeanScope;
    }

    /**
     * Sets the value of the managedBeanScope property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String }
     *     
     */
    public void setManagedBeanScope(java.lang.String value) {
        this.managedBeanScope = value;
    }

    /**
     * Gets the value of the managedProperty property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the managedProperty property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getManagedProperty().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link FacesManagedProperty }
     * 
     * 
     */
    public List<FacesManagedProperty> getManagedProperty() {
        if (managedProperty == null) {
            managedProperty = new ArrayList<FacesManagedProperty>();
        }
        return this.managedProperty;
    }

    /**
     * Gets the value of the mapEntries property.
     * 
     * @return
     *     possible object is
     *     {@link FacesMapEntries }
     *     
     */
    public FacesMapEntries getMapEntries() {
        return mapEntries;
    }

    /**
     * Sets the value of the mapEntries property.
     * 
     * @param value
     *     allowed object is
     *     {@link FacesMapEntries }
     *     
     */
    public void setMapEntries(FacesMapEntries value) {
        this.mapEntries = value;
    }

    /**
     * Gets the value of the listEntries property.
     * 
     * @return
     *     possible object is
     *     {@link FacesListEntries }
     *     
     */
    public FacesListEntries getListEntries() {
        return listEntries;
    }

    /**
     * Sets the value of the listEntries property.
     * 
     * @param value
     *     allowed object is
     *     {@link FacesListEntries }
     *     
     */
    public void setListEntries(FacesListEntries value) {
        this.listEntries = value;
    }

    /**
     * Gets the value of the managedBeanExtension property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the managedBeanExtension property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getManagedBeanExtension().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link FacesManagedBeanExtension }
     * 
     * 
     */
    public List<FacesManagedBeanExtension> getManagedBeanExtension() {
        if (managedBeanExtension == null) {
            managedBeanExtension = new ArrayList<FacesManagedBeanExtension>();
        }
        return this.managedBeanExtension;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String }
     *     
     */
    public java.lang.String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String }
     *     
     */
    public void setId(java.lang.String value) {
        this.id = value;
    }

}