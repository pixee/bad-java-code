package com.acme.xxe;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/** Holds various XXE vulns for different APIs. */
public class XXEVulnFixed {

  public static void main(String[] args)
      throws TransformerException,
          ParserConfigurationException,
          IOException,
          SAXException {
    docToString(null);
    saxTransformer(args[0]);
    withDom(args[1]);
    withDomButDisabled(args[2]);
    withReaderFactory(args[3]);
  }

  public static String docToString(final Document poDocument) throws TransformerException {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    Transformer transformer = transformerFactory.newTransformer();
    DOMSource domSrc = new DOMSource(poDocument);
    StringWriter sw = new StringWriter();
    StreamResult result = new StreamResult(sw);
    transformer.transform(domSrc, result);
    return sw.toString();
  }

  public static void saxTransformer(String xml)
      throws ParserConfigurationException, SAXException, IOException {
    SAXParserFactory spf = SAXParserFactory.newInstance();
    spf.setFeature(HTTP_XML_ORG_SAX_FEATURES_EXTERNAL_GENERAL_ENTITIES, false);
    spf.setFeature(HTTP_XML_ORG_SAX_FEATURES_EXTERNAL_PARAMETER_ENTITIES, false);
    spf.setValidating(true);

    SAXParser saxParser = spf.newSAXParser();
    XMLReader xmlReader = saxParser.getXMLReader();
    xmlReader.parse(new InputSource(new StringReader(xml)));
  }

  public static Document withDom(String xml)
      throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setFeature(HTTP_XML_ORG_SAX_FEATURES_EXTERNAL_GENERAL_ENTITIES, false);
    dbf.setFeature(HTTP_XML_ORG_SAX_FEATURES_EXTERNAL_PARAMETER_ENTITIES, false);
    DocumentBuilder db = dbf.newDocumentBuilder();
    return db.parse(new InputSource(new StringReader(xml)));
  }

  public static Document withDomButDisabled(String xml)
      throws ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setFeature(HTTP_XML_ORG_SAX_FEATURES_EXTERNAL_GENERAL_ENTITIES, false);
    dbf.setFeature(HTTP_XML_ORG_SAX_FEATURES_EXTERNAL_PARAMETER_ENTITIES, false);
    dbf.setExpandEntityReferences(true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    return db.parse(new InputSource(new StringReader(xml)));
  }

  public static void withReaderFactory(String xml)
      throws IOException, SAXException {
    XMLReader reader = XMLReaderFactory.createXMLReader();
    reader.setFeature(HTTP_XML_ORG_SAX_FEATURES_EXTERNAL_GENERAL_ENTITIES, false);
    reader.setFeature(HTTP_XML_ORG_SAX_FEATURES_EXTERNAL_PARAMETER_ENTITIES, false);
    reader.parse(new InputSource(new StringReader(xml)));
  }
  
  private static final String HTTP_XML_ORG_SAX_FEATURES_EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
  
  private static final String HTTP_XML_ORG_SAX_FEATURES_EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";
}
