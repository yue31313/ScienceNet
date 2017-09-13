package cn.sciencenet.httpclient;

import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.text.Html;


public class XmlLoginResponseHandler extends DefaultHandler{

	private XmlLoginResponse loginResponse;
	private String preTag;
	
	private StringBuffer uidBuffer = null;
	private StringBuffer usernameBuffer = null;
	private StringBuffer emailBuffer = null;
	
	public XmlLoginResponse getLoginResponse(InputStream xmlStream)throws Exception{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		XmlLoginResponseHandler handler = new XmlLoginResponseHandler();
		parser.parse(xmlStream, handler);
		return handler.getLoginResponse();
	}
	
	public XmlLoginResponse getLoginResponse(){
		return loginResponse;
	}
	
	@Override
	public void startDocument() throws SAXException{
		this.loginResponse = new XmlLoginResponse();
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if("t".equals(qName)){
			loginResponse = new XmlLoginResponse();
			
			uidBuffer = new StringBuffer();
			usernameBuffer = new StringBuffer();
			emailBuffer = new StringBuffer();
			
		}
		preTag = qName;
	}
	
	@Override
	public void characters(char[] ch,int start,int length)throws SAXException{
		if(preTag!=null){
			String content = new String(ch,start,length);
			if("uid".equals(preTag)){
				uidBuffer.append(Html.fromHtml(content));
			}else if("username".equals(preTag)){
				usernameBuffer.append(Html.fromHtml(content));
			}else if("email".equals(preTag)){
				emailBuffer.append(Html.fromHtml(content));
			}
		}
	}
	
	@Override
	public void endElement(String uri,String localName,String qName)throws SAXException{
		if("t".equals(qName)){
			loginResponse.setUid(uidBuffer.toString());
			loginResponse.setUsername(usernameBuffer.toString());
			loginResponse.setEmail(emailBuffer.toString());
			
			uidBuffer = null;
			usernameBuffer = null;
			emailBuffer = null;
			
		}
		preTag = null;
	}
}
