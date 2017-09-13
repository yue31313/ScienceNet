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

public class XmlBlogHandler extends DefaultHandler {
	private List<XmlItemBlog> list = null;
	private XmlItemBlog itemBlog = null;
	private String preTag = null;

	private StringBuffer descriptionBuffer = null;
	private StringBuffer titleBuffer = null;
	private StringBuffer blogidBuffer = null;
	private StringBuffer linkBuffer = null;
	private StringBuffer copyrightBuffer = null;
	private StringBuffer pubDateBuffer = null;

	public List<XmlItemBlog> getBlogItems(InputStream xmlStream)
			throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		XmlBlogHandler handler = new XmlBlogHandler();
		parser.parse(xmlStream, handler);
		return handler.getBlogItems();
	}
	
	public List<XmlItemBlog> getBlogItems() {
		return list;
	}
	
	@Override
	public void startDocument() throws SAXException {
		this.list = new ArrayList<XmlItemBlog>();
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
	Attributes attributes) throws SAXException {
		if ("item".equals(qName)) {
			itemBlog = new XmlItemBlog();
			descriptionBuffer = new StringBuffer();
			titleBuffer = new StringBuffer();
			blogidBuffer = new StringBuffer();
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
			if ("blogid".equals(preTag)) {
//				itemBlog.setBlogid(content);
				blogidBuffer.append(Html.fromHtml(content));
			} else if ("title".equals(preTag)) {
				titleBuffer.append(Html.fromHtml(content));
			} else if ("link".equals(preTag)) {
//				itemBlog.setLink(content);
				linkBuffer.append(Html.fromHtml(content));
			} else if ("description".equals(preTag)) {
				descriptionBuffer.append(Html.fromHtml(content));
			} else if ("copyright".equals(preTag)) {
//				itemBlog.setCopyright(content);
				copyrightBuffer.append(Html.fromHtml(content));
			}  else if ("pubDate".equals(preTag)) {
//				itemBlog.setPubDate(content);
				pubDateBuffer.append(Html.fromHtml(content));
			}
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if ("item".equals(qName)) {
			itemBlog.setDescription(descriptionBuffer.toString());
			itemBlog.setTitle(titleBuffer.toString());
			itemBlog.setBlogid(blogidBuffer.toString());
			itemBlog.setLink(linkBuffer.toString());
			itemBlog.setCopyright(copyrightBuffer.toString());
			itemBlog.setPubDate(pubDateBuffer.toString());
			
			list.add(itemBlog);
			
			itemBlog = null;
			descriptionBuffer = null;
			titleBuffer = null;
			blogidBuffer = null;
			linkBuffer = null;
			copyrightBuffer = null;
			pubDateBuffer = null;
		}
		preTag = null;
	}
}











