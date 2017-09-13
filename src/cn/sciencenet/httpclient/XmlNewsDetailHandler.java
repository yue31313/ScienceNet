package cn.sciencenet.httpclient;

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.os.Bundle;

public class XmlNewsDetailHandler extends DefaultHandler {
	private Bundle bundle;

	private String preTag;

	private StringBuffer descriptionBuffer;
	private StringBuffer titleBuffer;
	private StringBuffer linkBuffer;
	private StringBuffer imgsBuffer;
	private StringBuffer copyrightBuffer;
	private StringBuffer sourseBuffer;
	private StringBuffer pubDateBuffer;
	private StringBuffer commentBuffer;

	public Bundle getNewsDetails(InputStream xmlStream) throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		XmlNewsDetailHandler handler = new XmlNewsDetailHandler();
		parser.parse(xmlStream, handler);
		return handler.getNewsDetails();
	}

	public Bundle getNewsDetails() {
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
			linkBuffer = new StringBuffer();
			imgsBuffer = new StringBuffer();
			copyrightBuffer = new StringBuffer();
			sourseBuffer = new StringBuffer();
			pubDateBuffer = new StringBuffer();
			commentBuffer = new StringBuffer();
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
			} else if ("link".equals(preTag)) {
				// this.bundle.putString("news_link", content);
				linkBuffer.append(content);
			} else if ("imgs".equals(preTag)) {
				// this.bundle.putString("news_imgs", content);
				imgsBuffer.append(content); // 图片链接
			} else if ("description".equals(preTag)) {
				descriptionBuffer.append(content);
			} else if ("copyright".equals(preTag)) {
				// this.bundle.putString("news_copyright", content);
				copyrightBuffer.append(content);
			} else if ("sourse".equals(preTag)) {
				// this.bundle.putString("news_sourse", content);
				sourseBuffer.append(content);
			} else if ("pubDate".equals(preTag)) {
				// this.bundle.putString("news_pubDate", content);
				pubDateBuffer.append(content);
			} else if ("comment".equals(preTag)) {
				// this.bundle.putString("news_comment", content);
				commentBuffer.append(content);
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if ("item".equals(qName)) {
			this.bundle.putString("news_description",
					descriptionBuffer.toString());
			this.bundle.putString("news_title", titleBuffer.toString());
			this.bundle.putString("news_link", linkBuffer.toString());
			this.bundle.putString("news_imgs", imgsBuffer.toString());
			this.bundle.putString("news_copyright", copyrightBuffer.toString());
			this.bundle.putString("news_sourse", sourseBuffer.toString());
			this.bundle.putString("news_pubDate", pubDateBuffer.toString());
			this.bundle.putString("news_comment", commentBuffer.toString());

			descriptionBuffer = null;
			titleBuffer = null;
			linkBuffer = null;
			imgsBuffer = null;
			copyrightBuffer = null;
			sourseBuffer = null;
			pubDateBuffer = null;
			commentBuffer = null;
		}
		preTag = null;
	}
}
