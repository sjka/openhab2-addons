//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.01.14 at 06:54:48 PM CET 
//


package org.openhab.binding.knx.internal.parser.knxproj13;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Installations complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Installations">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Installation" type="{http://knx.org/xml/project/13}Installation"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Installations", propOrder = {
    "installation"
})
public class Installations {

    @XmlElement(name = "Installation", required = true)
    protected Installation installation;

    /**
     * Gets the value of the installation property.
     * 
     * @return
     *     possible object is
     *     {@link Installation }
     *     
     */
    public Installation getInstallation() {
        return installation;
    }

    /**
     * Sets the value of the installation property.
     * 
     * @param value
     *     allowed object is
     *     {@link Installation }
     *     
     */
    public void setInstallation(Installation value) {
        this.installation = value;
    }

}
