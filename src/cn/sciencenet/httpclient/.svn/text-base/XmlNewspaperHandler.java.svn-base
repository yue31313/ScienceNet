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

public class XmlNewspaperHandler extends DefaultHandler {

	private List<XmlItemNewspaper> list = null;
	private XmlItemNewspaper itemNewspaper = null;
	
	private StringBuffer descirptionBuffer = null;
	private StringBuffer titleBuffer = null;
	private StringBuffer idBuffer = null;
	private StringBuffer pubdateBuffer = null;
	
	private String preTag = null;

	public List<XmlItemNewspaper> getNewspaperItems(InputStream xmlStream)
			throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		XmlNewspaperHandler handler = new XmlNewspaperHandler();
		parser.parse(xmlStream, handler);
		return handler.getNewspaperItems();
	}

	public List<XmlItemNewspaper> getNewspaperItems() {
		return list;
	}

	@Override
	public void startDocument() throws SAXException {
		this.list = new ArrayList<XmlItemNewspaper>();
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if ("item".equals(qName)) {
			itemNewspaper = new XmlItemNewspaper();
			
			descirptionBuffer = new StringBuffer();
			titleBuffer = new StringBuffer();
			idBuffer = new StringBuffer();
			pubdateBuffer = new StringBuffer();
		}
		preTag = qName;
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (preTag != null) {
			String content = new String(ch, start, length);
			if ("title".equals(preTag)) {
//				itemNewspaper.setTitle(content);
				titleBuffer.append(Html.fromHtml(content));
			} else if ("id".equals(preTag)) {
//				itemNewspaper.setId(content);
				idBuffer.append(Html.fromHtml(content));
			} else if ("logo".equals(preTag)) {
				// itemNewspaper.setLogo(content);
//				Log.e("liushuai!!!", content);
				descirptionBuffer.append(Html.fromHtml(content));
			} else if ("pubDate".equals(preTag)) {
//				itemNewspaper.setPubDate(content);
				pubdateBuffer.append(Html.fromHtml(content));
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if ("item".equals(qName)) {
			itemNewspaper.setLogo(descirptionBuffer.toString());
			itemNewspaper.setTitle(titleBuffer.toString());
			itemNewspaper.setId(idBuffer.toString());
			itemNewspaper.setPubDate(pubdateBuffer.toString());
			
			list.add(itemNewspaper);
			
			itemNewspaper = null;
			descirptionBuffer = null;
			idBuffer = null;
			pubdateBuffer = null;
		}
		preTag = null;
	}
}
