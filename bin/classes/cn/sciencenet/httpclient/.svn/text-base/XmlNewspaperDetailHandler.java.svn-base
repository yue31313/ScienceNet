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

public class XmlNewspaperDetailHandler extends DefaultHandler{

	private List<XmlNewspaperDetail> list = null;
	private XmlNewspaperDetail itemNewspaper = null;
	private String preTag = null;
	
	private StringBuffer descriptionBuffer = null;
	private StringBuffer titleBuffer = null;
	private StringBuffer linkBuffer = null;
	private StringBuffer copyrightBuffer = null;
	private StringBuffer pubDateBuffer = null;
	private StringBuffer commentBuffer = null;
	private StringBuffer imgsBuffer = null;
	
	public List<XmlNewspaperDetail> getNewspaperDetails(InputStream xmlStream) throws Exception{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		XmlNewspaperDetailHandler handler = new XmlNewspaperDetailHandler();
		parser.parse(xmlStream, handler);
		return handler.getNewspaperDetails();
	}
	
	public List<XmlNewspaperDetail> getNewspaperDetails(){
		return list;
	}
	
	@Override
	public void startDocument() throws SAXException{
		this.list = new ArrayList<XmlNewspaperDetail>();
	}
	
	@Override
	public void startElement(String uri,String localName,String qName,Attributes attributes) throws SAXException{
		if("item".equals(qName)){
			itemNewspaper = new XmlNewspaperDetail();
			descriptionBuffer = new StringBuffer();
			titleBuffer = new StringBuffer();
			linkBuffer = new StringBuffer();
			copyrightBuffer = new StringBuffer();
			pubDateBuffer = new StringBuffer();
			commentBuffer = new StringBuffer();
			imgsBuffer = new StringBuffer();
		}
		preTag = qName;
	}
	
	@Override
	public void characters(char[] ch,int start,int length) throws SAXException{
		if(preTag != null){
			String content = new String(ch,start,length);
			if("title".equals(preTag)){
//				itemNewspaper.setTitle(content);
				titleBuffer.append(Html.fromHtml(content));
			}else if("link".equals(preTag)){
//				itemNewspaper.setLink(content);
				linkBuffer.append(Html.fromHtml(content));
			}else if("imgs".equals(preTag)){
				imgsBuffer.append(Html.fromHtml(content));
			}
			else if("description".equals(preTag)){
				descriptionBuffer.append(Html.fromHtml(content));
			}
			else if("copyright".equals(preTag)){
//				itemNewspaper.setCopyright(content);
				copyrightBuffer.append(Html.fromHtml(content));
			}else if("pubDate".equals(preTag)){
//				itemNewspaper.setPubDate(content);
				pubDateBuffer.append(Html.fromHtml(content));
			}else if("comment".equals(preTag)){
//				itemNewspaper.setComment(content);
				commentBuffer.append(Html.fromHtml(content));
			}
		}
	}
	
	@Override
	public void endElement(String uri,String localName,String qName) throws SAXException{
		if("item".equals(qName)){
			itemNewspaper.setDescription(descriptionBuffer.toString());
			itemNewspaper.setTitle(titleBuffer.toString());
			itemNewspaper.setLink(linkBuffer.toString());
			itemNewspaper.setId();
			itemNewspaper.setImgs(imgsBuffer.toString());
			itemNewspaper.setCopyright(copyrightBuffer.toString());
			itemNewspaper.setComment(commentBuffer.toString());
			itemNewspaper.setPubDate(pubDateBuffer.toString());
			
			list.add(itemNewspaper);
			
			itemNewspaper = null;
			descriptionBuffer = null;
			titleBuffer = null;
			linkBuffer = null;
			imgsBuffer = null;
			copyrightBuffer = null;
			pubDateBuffer = null;
			commentBuffer = null;
		}
		preTag = null;
	}
}
