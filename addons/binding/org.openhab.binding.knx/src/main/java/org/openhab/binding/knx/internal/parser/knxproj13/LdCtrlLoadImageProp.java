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
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * <p>Java class for LdCtrlLoadImageProp complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LdCtrlLoadImageProp">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="ObjIdx" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="PropId" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="Obj" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="Occurrence" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LdCtrlLoadImageProp", propOrder = {
    "value"
})
public class LdCtrlLoadImageProp {

    @XmlValue
    protected java.lang.String value;
    @XmlAttribute(name = "ObjIdx")
    protected Byte objIdx;
    @XmlAttribute(name = "PropId")
    protected Byte propId;
    @XmlAttribute(name = "Obj")
    protected Byte obj;
    @XmlAttribute(name = "Occurrence")
    protected Byte occurrence;

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
     * Gets the value of the objIdx property.
     * 
     * @return
     *     possible object is
     *     {@link Byte }
     *     
     */
    public Byte getObjIdx() {
        return objIdx;
    }

    /**
     * Sets the value of the objIdx property.
     * 
     * @param value
     *     allowed object is
     *     {@link Byte }
     *     
     */
    public void setObjIdx(Byte value) {
        this.objIdx = value;
    }

    /**
     * Gets the value of the propId property.
     * 
     * @return
     *     possible object is
     *     {@link Byte }
     *     
     */
    public Byte getPropId() {
        return propId;
    }

    /**
     * Sets the value of the propId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Byte }
     *     
     */
    public void setPropId(Byte value) {
        this.propId = value;
    }

    /**
     * Gets the value of the obj property.
     * 
     * @return
     *     possible object is
     *     {@link Byte }
     *     
     */
    public Byte getObj() {
        return obj;
    }

    /**
     * Sets the value of the obj property.
     * 
     * @param value
     *     allowed object is
     *     {@link Byte }
     *     
     */
    public void setObj(Byte value) {
        this.obj = value;
    }

    /**
     * Gets the value of the occurrence property.
     * 
     * @return
     *     possible object is
     *     {@link Byte }
     *     
     */
    public Byte getOccurrence() {
        return occurrence;
    }

    /**
     * Sets the value of the occurrence property.
     * 
     * @param value
     *     allowed object is
     *     {@link Byte }
     *     
     */
    public void setOccurrence(Byte value) {
        this.occurrence = value;
    }

}
