/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.xml.rpc.streaming;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Stack;

import com.sun.xml.rpc.sp.NamespaceSupport;
import com.sun.xml.rpc.util.exception.LocalizableExceptionAdapter;
import com.sun.xml.rpc.util.xml.CDATA;
import com.sun.xml.rpc.util.xml.XmlWriter;

/**
 * <p> A concrete XMLWriter implementation class. </p>
 *
 * @author JAX-RPC Development Team
 */
public class XMLWriterImpl extends XMLWriterBase {

    public XMLWriterImpl(OutputStream out, String enc, boolean declare) {
        try {
            _writer = new XmlWriter(out, enc, declare);
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public void startElement(String localName, String uri) {
        try {
            _nsSupport.pushContext();

            if (!uri.equals("")) {
                String aPrefix = null;
                boolean mustDeclarePrefix = false;

                String defaultNamespaceURI = _nsSupport.getPrefix("");
                if (defaultNamespaceURI != null) {
                    if (uri.equals(defaultNamespaceURI)) {
                        aPrefix = "";
                    }
                }

                aPrefix = _nsSupport.getPrefix(uri);

                if (aPrefix == null) {
                    mustDeclarePrefix = true;

                    if (_prefixFactory != null) {
                        aPrefix = _prefixFactory.getPrefix(uri);
                    }

                    if (aPrefix == null) {
                        throw new XMLWriterException(
                            "xmlwriter.noPrefixForURI",
                            uri);
                    }
                }

                String rawName =
                    aPrefix.equals("")
                        ? localName
                        : (aPrefix + ":" + localName);

                _writer.start(rawName);
                _elemStack.push(rawName);

                if (mustDeclarePrefix) {
                    writeNamespaceDeclaration(aPrefix, uri);
                }
            } else {
                _writer.start(localName);
                _elemStack.push(localName);
            }
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public void startElement(String localName, String uri, String prefix) {
        try {
            _nsSupport.pushContext();

            if (!uri.equals("")) {
                String aPrefix = null;
                boolean mustDeclarePrefix = false;

                String defaultNamespaceURI = _nsSupport.getPrefix("");
                if (defaultNamespaceURI != null) {
                    if (uri.equals(defaultNamespaceURI)) {
                        aPrefix = "";
                    }
                }

                aPrefix = _nsSupport.getPrefix(uri);

                if (aPrefix == null) {
                    mustDeclarePrefix = true;

                    aPrefix = prefix;

                    if (aPrefix == null) {
                        throw new XMLWriterException(
                            "xmlwriter.noPrefixForURI",
                            uri);
                    }
                }

                String rawName =
                    aPrefix.equals("")
                        ? localName
                        : (aPrefix + ":" + localName);

                _writer.start(rawName);
                _elemStack.push(rawName);

                if (mustDeclarePrefix) {
                    writeNamespaceDeclaration(aPrefix, uri);
                }
            } else {
                _writer.start(localName);
                _elemStack.push(localName);
            }
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public void writeNamespaceDeclaration(String prefix, String uri) {
        try {
            _nsSupport.declarePrefix(prefix, uri);

            if ((prefix != null) && !prefix.equals("")) {
                // it's not a default namespace declaration
                _writer.attribute("xmlns", prefix, uri);
            } else {
                _writer.attribute("xmlns", uri);
            }

        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public void writeNamespaceDeclaration(String uri) {

        if (_prefixFactory == null) {
            throw new XMLWriterException("xmlwriter.noPrefixForURI", uri);
        }

        String aPrefix = _prefixFactory.getPrefix(uri);
        writeNamespaceDeclaration(aPrefix, uri);
    }

    public void writeAttribute(String localName, String uri, String value) {
        try {
            if (!uri.equals("")) {

                String aPrefix = null;
                boolean mustDeclarePrefix = false;

                String defaultNamespaceURI = _nsSupport.getPrefix("");
                if (defaultNamespaceURI != null) {
                    if (uri.equals(defaultNamespaceURI)) {
                        aPrefix = "";
                    }
                }

                aPrefix = _nsSupport.getPrefix(uri);

                if (aPrefix == null) {
                    mustDeclarePrefix = true;

                    if (_prefixFactory != null) {
                        aPrefix = _prefixFactory.getPrefix(uri);
                    }

                    if (aPrefix == null) {
                        throw new XMLWriterException(
                            "xmlwriter.noPrefixForURI",
                            uri);
                    }
                }

                _writer.attribute(aPrefix, localName, value);

                if (mustDeclarePrefix) {
                    writeNamespaceDeclaration(aPrefix, uri);
                }

            } else {
                _writer.attribute(localName, value);
            }
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public void writeAttributeUnquoted(
        String localName,
        String uri,
        String value) {
        try {
            if (!uri.equals("")) {

                String aPrefix = null;
                boolean mustDeclarePrefix = false;

                String defaultNamespaceURI = _nsSupport.getPrefix("");
                if (defaultNamespaceURI != null) {
                    if (uri.equals(defaultNamespaceURI)) {
                        aPrefix = "";
                    }
                }

                aPrefix = _nsSupport.getPrefix(uri);

                if (aPrefix == null) {
                    mustDeclarePrefix = true;

                    if (_prefixFactory != null) {
                        aPrefix = _prefixFactory.getPrefix(uri);
                    }

                    if (aPrefix == null) {
                        throw new XMLWriterException(
                            "xmlwriter.noPrefixForURI",
                            uri);
                    }
                }

                _writer.attributeUnquoted(aPrefix, localName, value);

                if (mustDeclarePrefix) {
                    writeNamespaceDeclaration(aPrefix, uri);
                }

            } else {
                _writer.attributeUnquoted(localName, value);
            }
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public void writeChars(CDATA chars) {
        try {
            _writer.chars(chars);
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public void writeChars(String chars) {
        try {
            _writer.chars(chars);
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public void writeCharsUnquoted(String chars) {
        try {
            _writer.charsUnquoted(chars);
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public void writeCharsUnquoted(char[] buf, int offset, int len) {
        try {
            _writer.charsUnquoted(buf, offset, len);
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public void endElement() {
        try {
            // write the end tag
            String rawName = (String) _elemStack.pop();
            _writer.end(rawName);

            _nsSupport.popContext();
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public PrefixFactory getPrefixFactory() {
        return _prefixFactory;
    }

    public void setPrefixFactory(PrefixFactory factory) {
        _prefixFactory = factory;
    }

    public String getURI(String prefix) {
        return _nsSupport.getURI(prefix);
    }

    public String getPrefix(String uri) {
        return _nsSupport.getPrefix(uri);
    }

    public void flush() {
        try {
            _writer.flush();
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    public void close() {
        try {
            _writer.close();
        } catch (IOException e) {
            throw wrapException(e);
        }
    }

    private XMLWriterException wrapException(IOException e) {
        return new XMLWriterException(
            "xmlwriter.ioException",
            new LocalizableExceptionAdapter(e));
    }

    private XmlWriter _writer;
    private NamespaceSupport _nsSupport = new NamespaceSupport();
    private Stack _elemStack = new Stack();
    private PrefixFactory _prefixFactory;
}
