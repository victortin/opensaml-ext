/*
 * The opensaml-ext project is an open-source package that extends OpenSAML
 * with useful extensions and utilities.
 *
 * More details on <https://github.com/litsec/opensaml-ext>
 * Copyright (C) 2017 Litsec AB
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package se.litsec.opensaml.utils;

import javax.xml.namespace.QName;

import org.junit.Assert;
import org.junit.Test;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Status;
import org.opensaml.saml2.core.StatusCode;
import org.opensaml.saml2.core.StatusMessage;
import org.opensaml.xml.XMLRuntimeException;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.schema.XSBase64Binary;
import org.opensaml.xml.schema.XSString;
import org.opensaml.xml.util.XMLConstants;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Element;

import se.litsec.opensaml.OpenSAMLTestBase;

/**
 * Test cases for {@link ObjectUtils}.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class ObjectUtilsTest extends OpenSAMLTestBase {

  @Test
  public void testCreateSamlObject() throws Exception {
    
    // Create an Assertion using default element name.
    //
    Assertion assertion = ObjectUtils.createSamlObject(Assertion.class);
    Assert.assertNotNull(assertion);
    Assert.assertEquals(Assertion.DEFAULT_ELEMENT_NAME, assertion.getElementQName());
    
    // Create an Assertion with another localname and prefix.
    //
    QName qname = new QName(SAMLConstants.SAML20_NS, "OtherAssertion", "xyz");
    
    assertion = ObjectUtils.createSamlObject(Assertion.class, qname);
    Assert.assertNotNull(assertion);
    Assert.assertEquals(qname, assertion.getElementQName());    
  }

  @Test
  public void testCreateSamlObjectErrors() throws Exception {
    
    // If we try to create an Assertion using another, already registered, element name
    // we should get a cast error.
    //    
    try {
      ObjectUtils.createSamlObject(Assertion.class, AuthnRequest.DEFAULT_ELEMENT_NAME);
      Assert.fail("Expected class cast exception");
    }
    catch (ClassCastException e) {      
    }
    
    // We need a SAMLObject that has a registered builder.
    //
    try {
      ObjectUtils.createSamlObject(NonRegistered.class);
      Assert.fail("It should not be possible to create objects that have to registered builder");
    }
    catch (XMLRuntimeException e) {      
    }
    
    // When using createSamlObject(Class) we need the DEFAULT_ELEMENT_NAME to be defined.
    try {
      ObjectUtils.createSamlObject(NonRegistered2.class);
      Assert.fail("It should not be possible to create objects that does not have a DEFAULT_ELEMENT_NAME");
    }
    catch (XMLRuntimeException e) {      
    }
  }
  
  @Test
  public void testCreateXMLObject() throws Exception {
    
    // Create a XSString.
    //
    XSString string = ObjectUtils.createXMLObject(XSString.class, XSString.TYPE_NAME);
    Assert.assertNotNull(string);
    Assert.assertEquals(XSString.TYPE_NAME, string.getElementQName());
    
    // A SAMLObject is a XMLObject.
    //
    Assertion assertion = ObjectUtils.createXMLObject(Assertion.class, Assertion.DEFAULT_ELEMENT_NAME);
    Assert.assertNotNull(assertion);
    Assert.assertEquals(Assertion.DEFAULT_ELEMENT_NAME, assertion.getElementQName());
    
    // Assign another element name to the created XML object.
    //
    QName typeName = new QName(XMLConstants.XSD_NS, "ssttrriinngg", XMLConstants.XSD_PREFIX);
    string = ObjectUtils.createXMLObject(XSString.class, XSString.TYPE_NAME, typeName);
    Assert.assertNotNull(string);
    Assert.assertEquals(typeName, string.getElementQName());
  }
  
  @Test
  public void testCreateXMLObjectErrors() throws Exception {
    
    // Using the wrong type name for another type should give a casting error.
    //
    XSString s = null;
    try {
      s = ObjectUtils.createXMLObject(XSString.class, XSBase64Binary.TYPE_NAME);
      Assert.fail("Expected class cast exception");
    }
    catch (ClassCastException e) {      
    }
    Assert.assertNull(s);
    
    // We need a SAMLObject that has a registered builder.
    //
    try {
      ObjectUtils.createSamlObject(NonRegistered.class);
      Assert.fail("It should not be possible to create objects that have to registered builder");
    }
    catch (XMLRuntimeException e) {      
    }        
  }
  
  @Test
  public void testMarshall() throws Exception {
    
    Status status = getSampleStatusObject();    
    Assert.assertNull(status.getDOM());
    
    Element element = ObjectUtils.marshall(status);
    
    Assert.assertNotNull(status.getDOM());        
    Assert.assertEquals(Status.DEFAULT_ELEMENT_NAME.getLocalPart(), element.getLocalName());
    Assert.assertEquals(Status.DEFAULT_ELEMENT_NAME.getNamespaceURI(), element.getNamespaceURI());
    Assert.assertEquals(Status.DEFAULT_ELEMENT_NAME.getPrefix(), element.getPrefix());
    Assert.assertTrue(element.getChildNodes().getLength() == 2);    
  }
  
  @Test
  public void testUnmarshall() throws Exception {
    
    Status status = getSampleStatusObject();
    Element element = ObjectUtils.marshall(status);
    
    Status status2 = ObjectUtils.unmarshall(element, Status.class);
    Assert.assertTrue(equals(status, status2));
  }  
  
  private static Status getSampleStatusObject() {
    Status status = ObjectUtils.createSamlObject(Status.class);
    status.setStatusMessage(ObjectUtils.createSamlObject(StatusMessage.class));
    status.getStatusMessage().setMessage("An error");
    status.setStatusCode(ObjectUtils.createSamlObject(StatusCode.class));
    status.getStatusCode().setValue(StatusCode.REQUESTER_URI);
    status.getStatusCode().setStatusCode(ObjectUtils.createSamlObject(StatusCode.class));
    status.getStatusCode().getStatusCode().setValue(StatusCode.AUTHN_FAILED_URI);
    return status;
  }    
  
  private static boolean equals(Status s1, Status s2) throws MarshallingException {
    if (s1 == s2) return true;
    if (s1 == null && s2 != null || s1 != null && s2 == null) return false;
    String xml1 = XMLHelper.prettyPrintXML(ObjectUtils.marshall(s1));
    String xml2 = XMLHelper.prettyPrintXML(ObjectUtils.marshall(s2));
    return xml1.equals(xml2);
  }
  

  // Dummy SAMLObject for tests.
  //
  public interface NonRegistered extends SAMLObject {
    
    public static final String DEFAULT_ELEMENT_LOCAL_NAME = "NonRegistered";
    public static final QName DEFAULT_ELEMENT_NAME = new QName(SAMLConstants.SAML20_NS, DEFAULT_ELEMENT_LOCAL_NAME, SAMLConstants.SAML20_PREFIX);
    public static final String TYPE_LOCAL_NAME = "NonRegisteredType";
    public static final QName TYPE_NAME = new QName(SAMLConstants.SAML20_NS, TYPE_LOCAL_NAME, SAMLConstants.SAML20_PREFIX);
  }
  
  // Dummy SAMLObject for tests.
  //
  public interface NonRegistered2 extends Assertion {    
  }

  
}
