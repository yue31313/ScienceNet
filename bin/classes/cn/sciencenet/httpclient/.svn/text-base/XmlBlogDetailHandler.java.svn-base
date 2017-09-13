package cn.sciencenet.httpclient;

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.os.Bundle;

public class XmlBlogDetailHandler extends DefaultHandler {
	private Bundle bundle;

	private String preTag;

	private StringBuffer contentBuffer;
	private StringBuffer titleBuffer;
	private StringBuffer idBuffer;
	private StringBuffer noreplyBuffer;
	private StringBuffer datelineBuffer;

	public Bundle getBlogDetails(InputStream xmlStream) throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		XmlBlogDetailHandler handler = new XmlBlogDetailHandler();
		parser.parse(xmlStream, handler);
		return handler.getBlogDetails();
	}

	public Bundle getBlogDetails() {
		return bundle;
	}

	@Override
	public void startDocument() throws SAXException {
		this.bundle = new Bundle();
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if ("NewDataSet".equals(qName)) {
			contentBuffer = new StringBuffer();
			titleBuffer = new StringBuffer();
			idBuffer = new StringBuffer();
			noreplyBuffer = new StringBuffer();
			datelineBuffer = new StringBuffer();
		}
		preTag = qName;
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (preTag != null) {
			String content = new String(ch, start, length);
			if ("title".equals(preTag)) {
				titleBuffer.append(content);
			} else if ("content".equals(preTag)) {
				contentBuffer.append(content);
			} else if ("id".equals(preTag)) {
//				this.bundle.putString("blog_id", content);
				idBuffer.append(content);
			} else if ("noreply".equals(preTag)) {
//				this.bundle.putString("blog_noreply", content);
				noreplyBuffer.append(content);
			} else if ("dateline".equals(preTag)) {
//				this.bundle.putString("blog_dateline", content);
				datelineBuffer.append(content);
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if ("NewDataSet".equals(qName)) {
			this.bundle.putString("blog_title", titleBuffer.toString());
			this.bundle.putString("blog_content", contentBuffer.toString());
			this.bundle.putString("blog_id", idBuffer.toString());
			this.bundle.putString("blog_noreply", noreplyBuffer.toString());
			this.bundle.putString("blog_dateline", datelineBuffer.toString());
			
			titleBuffer = null;
			contentBuffer = null;
			idBuffer = null;
			noreplyBuffer = null;
			datelineBuffer = null;
		}
		preTag = null;
	}
}
