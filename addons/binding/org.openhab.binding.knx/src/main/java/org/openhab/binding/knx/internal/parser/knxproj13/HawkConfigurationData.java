//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.01.14 at 05:23:12 PM CET 
//


package org.openhab.binding.knx.internal.parser.knxproj13;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for HawkConfigurationData complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="HawkConfigurationData">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Features" type="{http://knx.org/xml/project/13}Features" minOccurs="0"/>
 *         &lt;element name="Resources" type="{http://knx.org/xml/project/13}Resources"/>
 *         &lt;element name="Procedures" type="{http://knx.org/xml/project/13}Procedures" minOccurs="0"/>
 *         &lt;element name="MemorySegments" type="{http://knx.org/xml/project/13}MemorySegments" minOccurs="0"/>
 *         &lt;element name="InterfaceObjects" type="{http://knx.org/xml/project/13}InterfaceObjects" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Ets3SystemPlugin" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="LegacyVersion" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HawkConfigurationData", propOrder = {
    "features",
    "resources",
    "procedures",
    "memorySegments",
    "interfaceObjects"
})
public class HawkConfigurationData {

    @XmlElement(name = "Features")
    protected Features features;
    @XmlElement(name = "Resources", required = true)
    protected Resources resources;
    @XmlElement(name = "Procedures")
    protected Procedures procedures;
    @XmlElement(name = "MemorySegments")
    protected MemorySegments memorySegments;
    @XmlElement(name = "InterfaceObjects")
    protected InterfaceObjects interfaceObjects;
    @XmlAttribute(name = "Ets3SystemPlugin")
    protected java.lang.String ets3SystemPlugin;
    @XmlAttribute(name = "LegacyVersion")
    protected Byte legacyVersion;

    /**
     * Gets the value of the features property.
     * 
     * @return
     *     possible object is
     *     {@link Features }
     *     
     */
    public Features getFeatures() {
        return features;
    }

    /**
     * Sets the value of the features property.
     * 
     * @param value
     *     allowed object is
     *     {@link Features }
     *     
     */
    public void setFeatures(Features value) {
        this.features = value;
    }

    /**
     * Gets the value of the resources property.
     * 
     * @return
     *     possible object is
     *     {@link Resources }
     *     
     */
    public Resources getResources() {
        return resources;
    }

    /**
     * Sets the value of the resources property.
     * 
     * @param value
     *     allowed object is
     *     {@link Resources }
     *     
     */
    public void setResources(Resources value) {
        this.resources = value;
    }

    /**
     * Gets the value of the procedures property.
     * 
     * @return
     *     possible object is
     *     {@link Procedures }
     *     
     */
    public Procedures getProcedures() {
        return procedures;
    }

    /**
     * Sets the value of the procedures property.
     * 
     * @param value
     *     allowed object is
     *     {@link Procedures }
     *     
     */
    public void setProcedures(Procedures value) {
        this.procedures = value;
    }

    /**
     * Gets the value of the memorySegments property.
     * 
     * @return
     *     possible object is
     *     {@link MemorySegments }
     *     
     */
    public MemorySegments getMemorySegments() {
        return memorySegments;
    }

    /**
     * Sets the value of the memorySegments property.
     * 
     * @param value
     *     allowed object is
     *     {@link MemorySegments }
     *     
     */
    public void setMemorySegments(MemorySegments value) {
        this.memorySegments = value;
    }

    /**
     * Gets the value of the interfaceObjects property.
     * 
     * @return
     *     possible object is
     *     {@link InterfaceObjects }
     *     
     */
    public InterfaceObjects getInterfaceObjects() {
        return interfaceObjects;
    }

    /**
     * Sets the value of the interfaceObjects property.
     * 
     * @param value
     *     allowed object is
     *     {@link InterfaceObjects }
     *     
     */
    public void setInterfaceObjects(InterfaceObjects value) {
        this.interfaceObjects = value;
    }

    /**
     * Gets the value of the ets3SystemPlugin property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String }
     *     
     */
    public java.lang.String getEts3SystemPlugin() {
        return ets3SystemPlugin;
    }

    /**
     * Sets the value of the ets3SystemPlugin property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String }
     *     
     */
    public void setEts3SystemPlugin(java.lang.String value) {
        this.ets3SystemPlugin = value;
    }

    /**
     * Gets the value of the legacyVersion property.
     * 
     * @return
     *     possible object is
     *     {@link Byte }
     *     
     */
    public Byte getLegacyVersion() {
        return legacyVersion;
    }

    /**
     * Sets the value of the legacyVersion property.
     * 
     * @param value
     *     allowed object is
     *     {@link Byte }
     *     
     */
    public void setLegacyVersion(Byte value) {
        this.legacyVersion = value;
    }

}
