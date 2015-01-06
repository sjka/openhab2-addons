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
 * <p>Java class for LdCtrlLoadCompleted complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="LdCtrlLoadCompleted">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="LsmIdx" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="Name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="LdCtrlLoadCompletedValue" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="Address" type="{http://www.w3.org/2001/XMLSchema}short" />
 *       &lt;attribute name="Size" type="{http://www.w3.org/2001/XMLSchema}short" />
 *       &lt;attribute name="Verify" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="InlineData" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="MergeId" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="Mode" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="Fill" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="ObjIdx" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="PropId" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="Obj" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="Occurrence" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *       &lt;attribute name="OriginalError" type="{http://www.w3.org/2001/XMLSchema}long" />
 *       &lt;attribute name="MappedError" type="{http://www.w3.org/2001/XMLSchema}long" />
 *       &lt;attribute name="Count" type="{http://www.w3.org/2001/XMLSchema}byte" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LdCtrlLoadCompleted", propOrder = {
    "value"
})
public class LdCtrlLoadCompleted {

    @XmlValue
    protected java.lang.String value;
    @XmlAttribute(name = "LsmIdx")
    protected Byte lsmIdx;
    @XmlAttribute(name = "Name")
    protected java.lang.String name;
    @XmlAttribute(name = "LdCtrlLoadCompletedValue")
    protected java.lang.String ldCtrlLoadCompletedValue;
    @XmlAttribute(name = "Address")
    protected Short address;
    @XmlAttribute(name = "Size")
    protected Short size;
    @XmlAttribute(name = "Verify")
    protected java.lang.String verify;
    @XmlAttribute(name = "InlineData")
    protected java.lang.String inlineData;
    @XmlAttribute(name = "MergeId")
    protected Byte mergeId;
    @XmlAttribute(name = "Mode")
    protected Byte mode;
    @XmlAttribute(name = "Fill")
    protected Byte fill;
    @XmlAttribute(name = "ObjIdx")
    protected Byte objIdx;
    @XmlAttribute(name = "PropId")
    protected Byte propId;
    @XmlAttribute(name = "Obj")
    protected Byte obj;
    @XmlAttribute(name = "Occurrence")
    protected Byte occurrence;
    @XmlAttribute(name = "OriginalError")
    protected Long originalError;
    @XmlAttribute(name = "MappedError")
    protected Long mappedError;
    @XmlAttribute(name = "Count")
    protected Byte count;

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
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String }
     *     
     */
    public java.lang.String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String }
     *     
     */
    public void setName(java.lang.String value) {
        this.name = value;
    }

    /**
     * Gets the value of the ldCtrlLoadCompletedValue property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String }
     *     
     */
    public java.lang.String getLdCtrlLoadCompletedValue() {
        return ldCtrlLoadCompletedValue;
    }

    /**
     * Sets the value of the ldCtrlLoadCompletedValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String }
     *     
     */
    public void setLdCtrlLoadCompletedValue(java.lang.String value) {
        this.ldCtrlLoadCompletedValue = value;
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
     * Gets the value of the size property.
     * 
     * @return
     *     possible object is
     *     {@link Short }
     *     
     */
    public Short getSize() {
        return size;
    }

    /**
     * Sets the value of the size property.
     * 
     * @param value
     *     allowed object is
     *     {@link Short }
     *     
     */
    public void setSize(Short value) {
        this.size = value;
    }

    /**
     * Gets the value of the verify property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String }
     *     
     */
    public java.lang.String getVerify() {
        return verify;
    }

    /**
     * Sets the value of the verify property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String }
     *     
     */
    public void setVerify(java.lang.String value) {
        this.verify = value;
    }

    /**
     * Gets the value of the inlineData property.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String }
     *     
     */
    public java.lang.String getInlineData() {
        return inlineData;
    }

    /**
     * Sets the value of the inlineData property.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String }
     *     
     */
    public void setInlineData(java.lang.String value) {
        this.inlineData = value;
    }

    /**
     * Gets the value of the mergeId property.
     * 
     * @return
     *     possible object is
     *     {@link Byte }
     *     
     */
    public Byte getMergeId() {
        return mergeId;
    }

    /**
     * Sets the value of the mergeId property.
     * 
     * @param value
     *     allowed object is
     *     {@link Byte }
     *     
     */
    public void setMergeId(Byte value) {
        this.mergeId = value;
    }

    /**
     * Gets the value of the mode property.
     * 
     * @return
     *     possible object is
     *     {@link Byte }
     *     
     */
    public Byte getMode() {
        return mode;
    }

    /**
     * Sets the value of the mode property.
     * 
     * @param value
     *     allowed object is
     *     {@link Byte }
     *     
     */
    public void setMode(Byte value) {
        this.mode = value;
    }

    /**
     * Gets the value of the fill property.
     * 
     * @return
     *     possible object is
     *     {@link Byte }
     *     
     */
    public Byte getFill() {
        return fill;
    }

    /**
     * Sets the value of the fill property.
     * 
     * @param value
     *     allowed object is
     *     {@link Byte }
     *     
     */
    public void setFill(Byte value) {
        this.fill = value;
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

    /**
     * Gets the value of the originalError property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getOriginalError() {
        return originalError;
    }

    /**
     * Sets the value of the originalError property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setOriginalError(Long value) {
        this.originalError = value;
    }

    /**
     * Gets the value of the mappedError property.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getMappedError() {
        return mappedError;
    }

    /**
     * Sets the value of the mappedError property.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setMappedError(Long value) {
        this.mappedError = value;
    }

    /**
     * Gets the value of the count property.
     * 
     * @return
     *     possible object is
     *     {@link Byte }
     *     
     */
    public Byte getCount() {
        return count;
    }

    /**
     * Sets the value of the count property.
     * 
     * @param value
     *     allowed object is
     *     {@link Byte }
     *     
     */
    public void setCount(Byte value) {
        this.count = value;
    }

}
