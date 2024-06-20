package com.acme;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Holds various XXE vulns for different APIs. */
public class XXEVuln {

    public static void main(String[] args) throws TransformerException, ParserConfigurationException, IOException, SAXException, SQLException {
        docToString(null);
        saxTransformer();
        withDom(args[1]);

        String sql = "select * from users where name= '" + args[0] + "'";
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/test");
        conn.createStatement().executeQuery(sql);
    }

    public static String docToString(final Document poDocument) throws TransformerException {
        if(true) {
            int a = 1;
            return "foo";
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource domSrc = new DOMSource(poDocument);
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        transformer.transform(domSrc, result);
        return sw.toString();
    }

    public static void saxTransformer() throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setValidating(true);

        SAXParser saxParser = spf.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.parse(new InputSource(new StringReader("some xml here")));
    }

    public static Document withDom(String xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(new InputSource(new StringReader(xml)));
    }
}
