/*
 * $Id: LiteralFragmentSerializer.java,v 1.2.2.2 2008-02-19 10:51:24 venkatajetti Exp $
 */

/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */


package com.sun.xml.rpc.encoding.literal;

import javax.xml.namespace.QName;
import com.sun.xml.rpc.streaming.*;
import com.sun.xml.rpc.encoding.*;
import com.sun.xml.rpc.encoding.xsd.XSDConstants;
import com.sun.xml.rpc.soap.SOAPVersion;
import com.sun.xml.rpc.soap.SOAPConstantsFactory;
import javax.xml.soap.*;
import java.util.Iterator;

/**
 *
 * @author JAX-RPC RI Development Team
 */
public class LiteralFragmentSerializer extends LiteralObjectSerializerBase {

    protected SOAPFactory soapFactory;

    private com.sun.xml.rpc.soap.SOAPEncodingConstants soapEncodingConstants = null;

    private void init(SOAPVersion ver) {
        this.soapEncodingConstants = SOAPConstantsFactory.getSOAPEncodingConstants(ver);

        try {
            soapFactory = SOAPFactory.newInstance();
        } catch (SOAPException e) {
            // TODO - report this
        }
    }

    public LiteralFragmentSerializer(QName type, boolean isNullable, String encodingStyle) {
        this(type, isNullable, encodingStyle, SOAPVersion.SOAP_11);
    }
 
    public LiteralFragmentSerializer(QName type, boolean isNullable, String encodingStyle, SOAPVersion ver) {
        this(type, isNullable, encodingStyle, false, ver);
    }

    public LiteralFragmentSerializer(QName type, boolean isNullable, String encodingStyle, boolean encodeType, SOAPVersion ver) {
        super(type, isNullable, encodingStyle, encodeType);
        init(ver);
    }

    protected void writeAdditionalNamespaceDeclarations(Object obj, XMLWriter writer) throws Exception {
        // no-op
    }

    protected boolean hasDefaultNamespace(SOAPElement element) {
        for (Iterator iter = element.getAllAttributes(); iter.hasNext();) {
            Name aname = (Name) iter.next();
            if (aname.getLocalName().equals("xmlns"))
                return true;
        }
        return false;
    }
    
    protected void internalSerialize(Object obj, QName name, XMLWriter writer, SOAPSerializationContext context) throws Exception {
        boolean pushedEncodingStyle = false;
        if (obj == null) {
            if (!isNullable) {
                throw new SerializationException("literal.unexpectedNull");
            }

            writer.startElement((name != null) ? name : type);
            writer.writeAttributeUnquoted(XSDConstants.QNAME_XSI_NIL, "1");
            writer.endElement();
        }
        else {
            SOAPElement element = (SOAPElement) obj;
            
            Name elementName = element.getElementName();
            if (hasDefaultNamespace(element))
                writer.startElement(elementName.getLocalName(), elementName.getURI());
            else
                writer.startElement(elementName.getLocalName(), elementName.getURI(), elementName.getPrefix());

            for (Iterator iter = element.getNamespacePrefixes(); iter.hasNext();) {
                String prefix = (String) iter.next();
                String uri = element.getNamespaceURI(prefix);
                String existingURI = writer.getURI(prefix);
                if (existingURI == null || !existingURI.equals(uri)) {
                    writer.writeNamespaceDeclaration(prefix, uri);
                }
            }

            if (encodingStyle != null)
                pushedEncodingStyle = context.pushEncodingStyle(encodingStyle, writer);

            for (Iterator iter = element.getAllAttributes(); iter.hasNext();) {
                Name aname = (Name) iter.next();
                String value = element.getAttributeValue(aname);
//                boolean isNamespaceDeclaration = aname.getPrefix().equals("xmlns");
                // Fix for bug: 4700103
                // This if was added because the encodingStyle attribute was being added
                // twice in the case of an echoDocmement(Element)
                // 2003-04-23 removed the above mentioned bug fix as it eliminates
                // encodingStyle attributes that are needed.
//               if (!isNamespaceDeclaration && !(aname.getLocalName().equals(soapEncodingConstants.getQNameEnvelopeEncodingStyle().getLocalPart()) &&
//                      aname.getURI().equals(soapEncodingConstants.getQNameEnvelopeEncodingStyle().getNamespaceURI()))) {
//                    writer.writeAttribute(aname.getLocalName(), aname.getURI(), value);
//                }
                writer.writeAttribute(aname.getLocalName(), aname.getURI(), value);
            }

            for (Iterator iter = element.getChildElements(); iter.hasNext();) {
                Node node = (Node) iter.next();
                if (node instanceof Text) {
                    Text text = (Text) node;
                    if (text.isComment()) {
                        // skip comments (for now)
                    }
					else if(text.getValue() != null) { //Nagesh(01/10/2006): verify the text node value for null
                        writer.writeChars(text.getValue());
                    }
                }
                else if (node instanceof SOAPElement) {
                    serialize(node, null, null, writer, context);
                }
                else {
                    // skip other nodes (for now)
                }
            }

            writer.endElement();
            if (pushedEncodingStyle) {
                context.popEncodingStyle();
            }
        }
    }

    protected Object doDeserialize(XMLReader reader, SOAPDeserializationContext context) throws Exception {
        SOAPElement element;
        String elementURI = reader.getURI();
        //Nagesh (12/23/2005): a hack for fixing the QAI# 93421. 
        //JAXRPC RI is assigning WSDL namespace for child element in certain cases. 
        //So ignoring such namespace while creating child elements 
        if (elementURI == null || elementURI.equals("") || elementURI.equals("http://schemas.xmlsoap.org/wsdl/")) {
            element = soapFactory.createElement(reader.getLocalName());
        }
        else {
            element = soapFactory.createElement(reader.getLocalName(), FIRST_PREFIX, reader.getURI());
        }

        String defaultURI = reader.getURI(""
        
        
        );
        if (defaultURI != null) {
            element.addNamespaceDeclaration("", defaultURI);
        }

        for (Iterator iter = reader.getPrefixes(); iter.hasNext();) {
            String prefix = (String) iter.next();
            String uri = reader.getURI(prefix);
            element.addNamespaceDeclaration(prefix, uri);
        }

        Attributes attributes = reader.getAttributes();
        for (int i = 0; i < attributes.getLength(); ++i) {
            if (attributes.isNamespaceDeclaration(i)) {
                continue;
            }

            Name name;
            String uri = attributes.getURI(i);
            if (uri == null) {
                // non-qualified attribute
                name = soapFactory.createName(attributes.getLocalName(i));
            }
            else {
                // qualified attribute
                String prefix = attributes.getPrefix(i);
                name = soapFactory.createName(attributes.getLocalName(i), prefix, uri);
            }
            element.addAttribute(name, attributes.getValue(i));
        }

        reader.next();
        while (reader.getState() != XMLReader.END) {
            int state = reader.getState();
            if (state == XMLReader.START) {
                SOAPElement child = (SOAPElement) deserialize(null, reader, context);
                //element.addChildElement(child);
                /* Nagesh(06-24-2005): Added  conditional based branching.
                 * When xsi:nil="true" found in soap response, deserializer is returning null. 
                 * When deserializer returns null then create new soapelement and add it to its parent element. 
                 */
                try {
                if(child != null) {
                	element.addChildElement(child);
                } else {
                    // CR-6660363, Merge from JavaCAPS RTS for backward compatibility

                        //child = soapFactory.createElement(reader.getLocalName(), FIRST_PREFIX, reader.getURI());

                        /* 
                         * HotFix 104035.  When xsi:nil="true", we first
                         * create a new child element and add all namespaces and 
                         * attributes to the new child element.  We then add the
                         * child element to the parent element.
                         */

                        elementURI = reader.getURI();
                        if (elementURI == null || elementURI.equals("") || elementURI.equals("http://schemas.xmlsoap.org/wsdl/")) {
                            child = soapFactory.createElement(reader.getLocalName());
                        } else {
                            child = soapFactory.createElement(reader.getLocalName(), FIRST_PREFIX, reader.getURI());
                        }

                        defaultURI = reader.getURI("");
                        
                        if (defaultURI != null) {
                            child.addNamespaceDeclaration("", defaultURI);
                        }

                        for (Iterator iter = reader.getPrefixes(); iter.hasNext();) {
                            String prefix = (String) iter.next();
                            String uri = reader.getURI(prefix);
                            child.addNamespaceDeclaration(prefix, uri);
                        }

                        attributes = reader.getAttributes();
                        for (int i = 0; i < attributes.getLength(); ++i) {
                            if (attributes.isNamespaceDeclaration(i)) {
                                continue;
                            }
                            
                            Name name;
                            String uri = attributes.getURI(i);
                            if (uri == null) {
                                // non-qualified attribute
                                name = soapFactory.createName(attributes.getLocalName(i));
                            }
                            else {
                                // qualified attribute
                                String prefix = attributes.getPrefix(i);
                                name = soapFactory.createName(attributes.getLocalName(i), prefix, uri);
                            }
                            child.addAttribute(name, attributes.getValue(i));
                        }
                	element.addChildElement(child);
                 }
                } catch (Exception e) {
                    throw e;
                }
            }
            else if (state == XMLReader.CHARS) {
                element.addTextNode(reader.getValue());
            }

            reader.next();
        }

        return element;
    }

    protected void doSerialize(Object obj, XMLWriter writer, SOAPSerializationContext context) throws Exception {
        // unused
    }

    protected void doSerializeAttributes(Object obj, XMLWriter writer, SOAPSerializationContext context) throws Exception {
        // unused
    }

    protected boolean isAcceptableType(QName actualType) {
        // avoid checking literal types too strictly, since we do not have all
        // the information needed to do so (for instance, we don't know the precise
        // derivation hierarchy)
        return true;
    }

    private static final String FIRST_PREFIX = "ns";
}
