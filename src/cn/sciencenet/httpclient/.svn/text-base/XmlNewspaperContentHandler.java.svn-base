package cn.sciencenet.httpclient;

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.os.Bundle;

public class XmlNewspaperContentHandler extends DefaultHandler{

	private Bundle bundle;
	
	private String preTag;
	
	private StringBuffer descriptionBuffer;
	private StringBuffer titleBuffer;
	private StringBuffer linkbBuffer;
	private StringBuffer imgsBuffer;
	private StringBuffer copyrightBuffer;
	private StringBuffer soursebBuffer;
	private StringBuffer pubdateBuffer;
	private StringBuffer commentsBuffer;
	
	public Bundle getNewspaperContent(InputStream xmlStream) throws Exception{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		XmlNewspaperContentHandler handler = new XmlNewspaperContentHandler();
		parser.parse(xmlStream, handler);
		return handler.getNewspaperContent();
	}
	
	public Bundle getNewspaperContent(){
		return bundle;
	}
	
	@Override 
	public void startDocument() throws SAXException{
		this.bundle = new Bundle();
	}
	
	@Override
	public void startElement (String uri,String localName,String qName,Attributes attributes) throws SAXException{
		if("item".equals(qName)){
			descriptionBuffer = new StringBuffer();
			titleBuffer = new StringBuffer();
			linkbBuffer = new StringBuffer();
			imgsBuffer = new StringBuffer();
			copyrightBuffer = new StringBuffer();
			soursebBuffer = new StringBuffer();
			pubdateBuffer = new StringBuffer();
			commentsBuffer = new StringBuffer();
		}
		preTag = qName;
		}
	
	@Override
	public void characters(char[] ch,int start,int length) throws SAXException{
		if(preTag!=null){
			String content = new String(ch,start,length);
			if("title".equals(preTag)){
//				this.bundle.putString("newspaper_content_title", content);
				titleBuffer.append(content);
			}else if("link".equals(preTag)){
//				this.bundle.putString("newspaper_content_link", content);
				linkbBuffer.append(content);
			}else if("imgs".equals(preTag)){
//				this.bundle.putString("newspaper_content_imgs", content);
				imgsBuffer.append(content);
			}else if("description".equals(preTag)){
				descriptionBuffer.append(content);
			}else if("copyright".equals(preTag)){
//				this.bundle.putString("newspaper_content_copyright", content);
				copyrightBuffer.append(content);
			}else if("sourse".equals(preTag)){
//				this.bundle.putString("newspaper_content_sourse", content);
				soursebBuffer.append(content);
			}else if("pubDate".equals(preTag)){
//				this.bundle.putString("newspaper_content_pubDate", content);
				pubdateBuffer.append(content);
			}else if("comments".equals(preTag)){
//				this.bundle.putString("newspaper_content_comments", content);
				commentsBuffer.append(content);
			}
		}
	}
	
	@Override
	public void endElement(String uri,String localName,String qName)throws SAXException{
		if("item".equals(qName)){
			this.bundle.putString("newspaper_content_description", descriptionBuffer.toString());
			this.bundle.putString("newspaper_content_title", titleBuffer.toString());
			this.bundle.putString("newspaper_content_link", linkbBuffer.toString());
			this.bundle.putString("newspaper_content_imgs", imgsBuffer.toString());
			this.bundle.putString("newspaper_content_copyright", copyrightBuffer.toString());
			this.bundle.putString("newspaper_content_sourse", soursebBuffer.toString());
			this.bundle.putString("newspaper_content_pubDate", pubdateBuffer.toString());
			this.bundle.putString("newspaper_content_comments", commentsBuffer.toString());
			
			descriptionBuffer = null;
			titleBuffer = null;
			linkbBuffer = null;
			imgsBuffer = null;
			copyrightBuffer = null;
			soursebBuffer = null;
			pubdateBuffer = null;
			commentsBuffer = null;
		}
		preTag = null;
	}
}
