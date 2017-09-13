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

public class XmlNewsHandler extends DefaultHandler {
	private List<XmlItemNews> list = null;
	private XmlItemNews itemNews = null;
	private String preTag = null;

	private StringBuffer descriptionBuffer = null;
	private StringBuffer titleBuffer = null;
	private StringBuffer linkBuffer = null;
	private StringBuffer imgsBuffer = null;
	private StringBuffer copyrightBuffer = null;
	private StringBuffer pubDateBuffer = null;
	private StringBuffer commentsBuffer = null;

	public List<XmlItemNews> getNewsItems(InputStream xmlStream)
			throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		XmlNewsHandler handler = new XmlNewsHandler();
		parser.parse(xmlStream, handler);
		return handler.getNewsItems();
	}

	public List<XmlItemNews> getNewsItems() {
		return list;
	}

	@Override
	public void startDocument() throws SAXException {
		this.list = new ArrayList<XmlItemNews>();
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if ("item".equals(qName)) {
			itemNews = new XmlItemNews();

			descriptionBuffer = new StringBuffer();
			titleBuffer = new StringBuffer();
			linkBuffer = new StringBuffer();
			imgsBuffer = new StringBuffer();
			copyrightBuffer = new StringBuffer();
			pubDateBuffer = new StringBuffer();
			commentsBuffer = new StringBuffer();
		}
		preTag = qName;
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (preTag != null) {
			String content = new String(ch, start, length);
			if ("title".equals(preTag)) {
				titleBuffer.append(Html.fromHtml(content));
			} else if ("link".equals(preTag)) {
				// itemNews.setLink(content);
				// itemNews.setId();
				linkBuffer.append(Html.fromHtml(content));
			} else if ("imgs".equals(preTag)) {
				imgsBuffer.append(Html.fromHtml(content)); // 图片的URL
			} else if ("description".equals(preTag)) {
				descriptionBuffer.append(Html.fromHtml(content));
			} else if ("copyright".equals(preTag)) {
				// itemNews.setCopyright(content);
				copyrightBuffer.append(Html.fromHtml(content));
			} else if ("pubDate".equals(preTag)) {
				// itemNews.setPubDate(content);
				pubDateBuffer.append(Html.fromHtml(content));
			} else if ("comments".equals(preTag)) {
				// itemNews.setComment(content);
				commentsBuffer.append(Html.fromHtml(content));
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if ("item".equals(qName)) {
			itemNews.setDescription(descriptionBuffer.toString());
			itemNews.setTitle(titleBuffer.toString());
			itemNews.setLink(linkBuffer.toString());
			itemNews.setId();
			itemNews.setImgs(imgsBuffer.toString());
			itemNews.setCopyright(copyrightBuffer.toString());
			itemNews.setPubDate(pubDateBuffer.toString());
			itemNews.setComment(commentsBuffer.toString());

			list.add(itemNews);

			itemNews = null;
			descriptionBuffer = null;
			titleBuffer = null;
			linkBuffer = null;
			imgsBuffer = null;
			copyrightBuffer = null;
			pubDateBuffer = null;
			commentsBuffer = null;
		}
		preTag = null;
	}
}
