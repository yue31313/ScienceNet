package cn.sciencenet.httpclient;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.text.Html;

public class XmlGroupHandler extends DefaultHandler {
	private List<XmlItemGroup> list = null;
	private XmlItemGroup itemGroup = null;
	private String preTag = null;

	private StringBuffer descriptionBuffer = null;
	private StringBuffer titleBuffer = null;
	private StringBuffer tidBuffer = null;
	private StringBuffer linkBuffer = null;
	private StringBuffer copyrightBuffer = null;
	private StringBuffer pubdateBuffer = null;

	public List<XmlItemGroup> getGroupItems(InputStream xmlStream)
			throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		XmlGroupHandler handler = new XmlGroupHandler();
		parser.parse(xmlStream, handler);
		return handler.getGroupItems();
	}
	
	public List<XmlItemGroup> getGroupItems() {
		return list;
	}
	
	@Override
	public void startDocument() throws SAXException {
		this.list = new ArrayList<XmlItemGroup>();
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if ("item".equals(qName)) {
			itemGroup = new XmlItemGroup();
			descriptionBuffer = new StringBuffer();
			titleBuffer = new StringBuffer();
			tidBuffer = new StringBuffer();
			linkBuffer = new StringBuffer();
			copyrightBuffer = new StringBuffer();
			pubdateBuffer = new StringBuffer();
		}
		preTag = qName;
	}
	
	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (preTag != null) {
			String content = new String(ch, start, length);
			if ("tid".equals(preTag)) {
				tidBuffer.append(Html.fromHtml(content));
			} else if ("title".equals(preTag)) {
				titleBuffer.append(Html.fromHtml(content));
			} else if ("link".equals(preTag)) {
				linkBuffer.append(Html.fromHtml(content));
			} else if ("description".equals(preTag)) {
				descriptionBuffer.append(Html.fromHtml(content));
			} else if ("copyright".equals(preTag)) {
				copyrightBuffer.append(Html.fromHtml(content));
			} else if ("pubDate".equals(preTag)) {
				pubdateBuffer.append(Html.fromHtml(content));
			}
 		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if ("item".equals(qName)) {
			itemGroup.setDescription(descriptionBuffer.toString());
			itemGroup.setTitle(titleBuffer.toString());
			itemGroup.setTid(tidBuffer.toString());
			itemGroup.setLink(linkBuffer.toString());
			itemGroup.setCopyright(copyrightBuffer.toString());
			itemGroup.setPubDate(pubdateBuffer.toString());
			
			list.add(itemGroup);
			
			itemGroup = null;
			descriptionBuffer = null;
			titleBuffer = null;
			tidBuffer = null;
			linkBuffer = null;
			copyrightBuffer = null;
			pubdateBuffer = null;
		}
		preTag = null;
	}
}




















