package cn.sciencenet.httpclient;

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.os.Bundle;

public class XmlGroupDetailHandler extends DefaultHandler {
	private Bundle bundle;

	private String preTag;

	private StringBuffer descriptionBuffer;
	private StringBuffer titleBuffer;
	private StringBuffer tidBuffer;
	private StringBuffer linkBuffer;
	private StringBuffer copyrightBuffer;
	private StringBuffer pubDateBuffer;

	public Bundle getGroupDetails(InputStream xmlStream) throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		XmlGroupDetailHandler handler = new XmlGroupDetailHandler();
		parser.parse(xmlStream, handler);
		return handler.getGroupDetails();
	}

	public Bundle getGroupDetails() {
		return bundle;
	}

	@Override
	public void startDocument() throws SAXException {
		this.bundle = new Bundle();
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if ("item".equals(qName)) {
			descriptionBuffer = new StringBuffer();
			titleBuffer = new StringBuffer();
			tidBuffer = new StringBuffer();
			linkBuffer = new StringBuffer();
			copyrightBuffer = new StringBuffer();
			pubDateBuffer = new StringBuffer();
		}
		preTag = qName;
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (preTag != null) {
			String content = new String(ch, start, length);
			if ("tid".equals(preTag)) {
//				bundle.putString("group_id", content);
				tidBuffer.append(content);
			} else if ("title".equals(preTag)) {
				titleBuffer.append(content);
			} else if ("link".equals(preTag)) {
//				bundle.putString("group_link", content);
				linkBuffer.append(content);
			} else if ("description".equals(preTag)) {
				descriptionBuffer.append(content);
			} else if ("copyright".equals(preTag)) {
//				bundle.putString("group_copyright", content);
				copyrightBuffer.append(content);
			} else if ("pubDate".equals(preTag)) {
//				bundle.putString("group_pubdate", content);
				pubDateBuffer.append(content);
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if ("item".equals(qName)) {
			bundle.putString("group_title", titleBuffer.toString());
			bundle.putString("group_description", descriptionBuffer.toString());
			bundle.putString("group_id", tidBuffer.toString());
			bundle.putString("group_link", linkBuffer.toString());
			bundle.putString("group_copyright", copyrightBuffer.toString());
			bundle.putString("group_pubdate", pubDateBuffer.toString());
			
			titleBuffer = null;
			descriptionBuffer = null;
			tidBuffer = null;
			linkBuffer= null;
			copyrightBuffer = null;
			pubDateBuffer = null;
		}
		preTag = null;
	}
}
