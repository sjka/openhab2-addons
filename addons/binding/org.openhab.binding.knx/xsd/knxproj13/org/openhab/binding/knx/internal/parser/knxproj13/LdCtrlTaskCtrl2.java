//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.01.14 at 06:54:48 PM CET 
//


package org.openhab.binding.knx.internal.parser.knxproj13;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>Java class for LdCtrlTaskCtrl2 complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LdCtrlTaskCtrl2">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="LsmIdx" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="Callback" type="{http://www.w3.org/2001/XMLSchema}short" />
 *       &lt;attribute name="Address" type="{http://www.w3.org/2001/XMLSchema}short" />
 *       &lt;attribute name="Seg0" type="{http://www.w3.org/2001/XMLSchema}short" />
 *       &lt;attribute name="Seg1" type="{http://www.w3.org/2001/XMLSchema}short" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LdCtrlTaskCtrl2", propOrder = {
    "value"
})
public class LdCtrlTaskCtrl2 {

    @XmlValue
    protected java.lang.String value;
    @XmlAttribute(name = "LsmIdx")
    protected Byte lsmIdx;
    @XmlAttribute(name = "Callback")
    protected Short callback;
    @XmlAttribute(name = "Address")
    protected Short address;
    @XmlAttribute(name = "Seg0")
    protected Short seg0;
    @XmlAttribute(name = "Seg1")
    protected Short seg1;

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String }
     *     
     */
    public java.lang.String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String }
     *     
     */
    public void setValue(java.lang.String value) {
        this.value = value;
    }

    /**
     * Gets the value of the lsmIdx property.
     * 
     * @return
     *     possible object is
     *     {@link Byte }
     *     
     */
    public Byte getLsmIdx() {
        return lsmIdx;
    }

    /**
     * Sets the value of the lsmIdx property.
     * 
     * @param value
     *     allowed object is
     *     {@link Byte }
     *     
     */
    public void setLsmIdx(Byte value) {
        this.lsmIdx = value;
    }

    /**
     * Gets the value of the callback property.
     * 
     * @return
     *     possible object is
     *     {@link Short }
     *     
     */
    public Short getCallback() {
        return callback;
    }

    /**
     * Sets the value of the callback property.
     * 
     * @param value
     *     allowed object is
     *     {@link Short }
     *     
     */
    public void setCallback(Short value) {
        this.callback = value;
    }

    /**
     * Gets the value of the address property.
     * 
     * @return
     *     possible object is
     *     {@link Short }
     *     
     */
    public Short getAddress() {
        return address;
    }

    /**
     * Sets the value of the address property.
     * 
     * @param value
     *     allowed object is
     *     {@link Short }
     *     
     */
    public void setAddress(Short value) {
        this.address = value;
    }

    /**
     * Gets the value of the seg0 property.
     * 
     * @return
     *     possible object is
     *     {@link Short }
     *     
     */
    public Short getSeg0() {
        return seg0;
    }

    /**
     * Sets the value of the seg0 property.
     * 
     * @param value
     *     allowed object is
     *     {@link Short }
     *     
     */
    public void setSeg0(Short value) {
        this.seg0 = value;
    }

    /**
     * Gets the value of the seg1 property.
     * 
     * @return
     *     possible object is
     *     {@link Short }
     *     
     */
    public Short getSeg1() {
        return seg1;
    }

    /**
     * Sets the value of the seg1 property.
     * 
     * @param value
     *     allowed object is
     *     {@link Short }
     *     
     */
    public void setSeg1(Short value) {
        this.seg1 = value;
    }

}
