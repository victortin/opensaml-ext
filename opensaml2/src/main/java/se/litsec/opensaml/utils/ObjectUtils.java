/*
 * Copyright 2016-2018 Litsec AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.litsec.opensaml.utils;

import java.io.InputStream;

import javax.xml.namespace.QName;

import org.opensaml.common.SAMLObject;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.XMLRuntimeException;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.opensaml.xml.util.XMLObjectHelper;
import org.w3c.dom.Element;

/**
 * Utility methods for creating OpenSAML objects within directly having to make use of the builders for each object you
 * are creating and methods for marshalling and unmarshalling.
 * 
 * @author Martin Lindström (martin.lindstrom@litsec.se)
 */
public class ObjectUtils {

  /** The builder factory for XML objects. */
  private static XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();

  /** Parser pool to use when unmarshalling. */
  private static BasicParserPool parserPool = new BasicParserPool();

  static {
    parserPool.setNamespaceAware(true);
  }

  /**
   * Utility method for creating an OpenSAML {@code SAMLObject} using the default element name of the class.
   * <p>
   * Note: The field DEFAULT_ELEMENT_NAME of the given class will be used as the object's element name.
   * </p>
   * 
   * @param clazz
   *          the class to create
   * @param <T>
   *          the type of the class to create
   * @return the SAML object
   * @see #createSamlObject(Class, QName)
   */
  public static <T extends SAMLObject> T createSamlObject(Class<T> clazz) {
    return createSamlObject(clazz, getDefaultElementName(clazz));
  }

  /**
   * Utility method for creating an OpenSAML {@code SAMLObject} given its element name.
   * 
   * @param clazz
   *          the class to create
   * @param elementName
   *          the element name to assign the object that is created.
   * @param <T>
   *          the type
   * @return the SAML object
   */
  public static <T extends SAMLObject> T createSamlObject(Class<T> clazz, QName elementName) {

    XMLObjectBuilder<?> builder = builderFactory.getBuilder(elementName);
    if (builder == null) {
      // No builder registered for the given element name. Try creating a builder for the default element name.
      builder = builderFactory.getBuilder(getDefaultElementName(clazz));
    }
    if (builder == null) {
      // Still no builder? Time to fail.
      throw new XMLRuntimeException("No builder registered for " + clazz.getName());
    }
    XMLObject object = builder.buildObject(elementName);
    return clazz.cast(object);
  }

  /**
   * Utility method for creating an {@code XMLObject} given its element name.
   * 
   * @param clazz
   *          the class to create
   * @param elementName
   *          the element name for the XML object to create
   * @param <T>
   *          the type
   * @return the XML object
   */
  public static <T extends XMLObject> T createXMLObject(Class<T> clazz, QName elementName) {

    XMLObjectBuilder<?> builder = builderFactory.getBuilder(elementName);
    if (builder == null) {
      // No builder registered for the given element name.
      throw new XMLRuntimeException("No builder registered for " + clazz.getName());
    }
    Object object = builder.buildObject(elementName);
    return clazz.cast(object);
  }

  /**
   * Utility method for creating an {@code XMLObject} given its registered element name but where the
   * {@code elementNameToAssign} is assigned to the object created.
   * 
   * @param clazz
   *          the class to create
   * @param registeredElementName
   *          the element name that the object is registered under
   * @param elementNameToAssign
   *          the element to assign to the object that is created
   * @param <T>
   *          the type
   * @return the XML object
   */
  public static <T extends XMLObject> T createXMLObject(Class<T> clazz, QName registeredElementName, QName elementNameToAssign) {
    XMLObjectBuilder<?> builder = builderFactory.getBuilder(registeredElementName);
    if (builder == null) {
      // No builder registered for the given element name.
      throw new XMLRuntimeException("No builder registered for " + clazz.getName() + " using elementName " + registeredElementName
        .toString());
    }
    Object object = builder.buildObject(elementNameToAssign);
    return clazz.cast(object);
  }

  /**
   * Returns the default element name for the supplied class
   * 
   * @param clazz
   *          class to check
   * @param <T>
   *          the type
   * @return the default QName
   */
  public static <T extends SAMLObject> QName getDefaultElementName(Class<T> clazz) {
    try {
      return (QName) clazz.getDeclaredField("DEFAULT_ELEMENT_NAME").get(null);
    }
    catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException | SecurityException e) {
      throw new XMLRuntimeException(e);
    }
  }

  /**
   * Returns the builder object that can be used to create objects of the supplied class type.
   * <p>
   * Note: The field DEFAULT_ELEMENT_NAME of the given class will be used as the object's element name.
   * </p>
   * 
   * @param clazz
   *          the class which we want a builder for
   * @param <T>
   *          the type
   * @return a builder object
   * @see #getBuilder(QName)
   */
  public static <T extends SAMLObject> XMLObjectBuilder<T> getBuilder(Class<T> clazz) {
    return getBuilder(getDefaultElementName(clazz));
  }

  /**
   * Returns the builder object that can be used to build object for the given element name.
   * 
   * @param elementName
   *          the element name for the XML object that the builder should return
   * @param <T>
   *          the type
   * @return a builder object
   */
  @SuppressWarnings("unchecked")
  public static <T extends XMLObject> XMLObjectBuilder<T> getBuilder(QName elementName) {
    return builderFactory.getBuilder(elementName);
  }

  /**
   * Marshalls the supplied {@code XMLObject} into an {@code Element}.
   * 
   * @param object
   *          the object to marshall
   * @param <T>
   *          the type
   * @return an XML element
   * @throws MarshallingException
   *           for marshalling errors
   */
  public static <T extends XMLObject> Element marshall(T object) throws MarshallingException {
    Marshaller marshaller = Configuration.getMarshallerFactory().getMarshaller(object);
    if (marshaller == null) {
      throw new MarshallingException("No marshaller found for " + object.getClass().getSimpleName());
    }
    return marshaller.marshall(object);
  }

  /**
   * Unmarshalls the supplied element into the given type.
   * 
   * @param xml
   *          the DOM (XML) to unmarshall
   * @param targetClass
   *          the required class
   * @param <T>
   *          the type
   * @return an {@code XMLObject} of the given type
   * @throws UnmarshallingException
   *           for unmarshalling errors
   */
  public static <T extends XMLObject> T unmarshall(Element xml, Class<T> targetClass) throws UnmarshallingException {
    Unmarshaller unmarshaller = Configuration.getUnmarshallerFactory().getUnmarshaller(xml);
    if (unmarshaller == null) {
      throw new UnmarshallingException("No unmarshaller found for " + xml.getNodeName());
    }
    XMLObject xmlObject = unmarshaller.unmarshall(xml);
    return targetClass.cast(xmlObject);
  }

  /**
   * Unmarshalls the supplied input stream into the given type.
   * 
   * @param inputStream
   *          the input stream of the XML resource
   * @param targetClass
   *          the required class
   * @param <T>
   *          the type
   * @return an {@code XMLObject} of the given type
   * @throws XMLParserException
   *           for XML parsing errors
   * @throws UnmarshallingException
   *           for unmarshalling errors
   */
  public static <T extends XMLObject> T unmarshall(InputStream inputStream, Class<T> targetClass) throws XMLParserException,
      UnmarshallingException {

    XMLObject object = XMLObjectHelper.unmarshallFromInputStream(parserPool, inputStream);
    return targetClass.cast(object);
  }

  // Hidden constructor.
  private ObjectUtils() {
  }

}
