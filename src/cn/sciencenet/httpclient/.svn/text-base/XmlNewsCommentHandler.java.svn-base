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

public class XmlNewsCommentHandler extends DefaultHandler{
	private List<XmlItemNewsComment> list = null;
	private XmlItemNewsComment itemComment = null;
	
	private String preTag = null;
	
	private StringBuffer usernameBuffer = null;
	private StringBuffer descriptionBuffer = null;
	private StringBuffer posttimebBuffer = null;
	
	public List<XmlItemNewsComment> getNewsComments(InputStream xmlStream) throws Exception{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		XmlNewsCommentHandler handler = new XmlNewsCommentHandler();
		parser.parse(xmlStream, handler);
		return handler.getNewsComments();
	}
	
	public List<XmlItemNewsComment> getNewsComments(){
		return list;
	}
	
	@Override
	public void startDocument() throws SAXException{
		this.list = new ArrayList<XmlItemNewsComment>();
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException{
		if("item".equals(qName)){
			itemComment = new XmlItemNewsComment();
			
			usernameBuffer = new StringBuffer();
			descriptionBuffer = new StringBuffer();
			posttimebBuffer = new StringBuffer();
		}
		preTag = qName;
	}
	
	@Override
	public void characters(char[] ch,int start,int length) throws SAXException{
		if(preTag != null){
			String content = new String(ch,start,length);
			if("username".equals(preTag)){
				usernameBuffer.append(Html.fromHtml(content));
			}else if("description".equals(preTag)){
				descriptionBuffer.append(Html.fromHtml(content));
			}else if("posttime".equals(preTag)){
				posttimebBuffer.append(Html.fromHtml(content));
			}
		}
	}
		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException{
			if("item".equals(qName)){
				itemComment.setUsername(usernameBuffer.toString());
				itemComment.setDescription(descriptionBuffer.toString());
				itemComment.setPosttime(posttimebBuffer.toString());
				
				list.add(itemComment);
				
				itemComment = null;
				usernameBuffer = null;
				descriptionBuffer = null;
				posttimebBuffer = null;
			}
			preTag = null;
	}
}
