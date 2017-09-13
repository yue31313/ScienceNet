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

public class XmlAdHandler extends DefaultHandler{

	private List<XmlItemAd> list = null;
	private XmlItemAd itemAd = null;
	
	private StringBuffer idbBuffer = null;
	private StringBuffer linkBuffer = null;
	private StringBuffer imgBuffer = null;
	
	private String preTag = null;
	
	public List<XmlItemAd> getAdItem(InputStream xmlStream) throws Exception{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		XmlAdHandler handler = new XmlAdHandler();
		parser.parse(xmlStream, handler);
		return handler.getAdItem();
	}
	
	public List<XmlItemAd> getAdItem(){
		return list;
	}
	
	@Override
	public void startDocument() throws SAXException {
		this.list = new ArrayList<XmlItemAd>();
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes)throws SAXException{
		if ("item".equals(qName)) {
			itemAd = new XmlItemAd();
			
			idbBuffer = new StringBuffer();
			linkBuffer = new StringBuffer();
			imgBuffer = new StringBuffer();
		}
		preTag = qName;
	}
	
	@Override
	public void characters(char[] ch,int start,int length)throws SAXException{
		if(preTag!=null){
			String content = new String(ch,start,length);
			if ("id".equals(preTag)) {
				idbBuffer.append(Html.fromHtml(content));
			}else if("link".equals(preTag)){
				linkBuffer.append(Html.fromHtml(content));
			}else if("img".equals(preTag)){
				imgBuffer.append(Html.fromHtml(content));
			}
		}
	}
	
	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException{
		if("item".equals(qName)){
			itemAd.setId(idbBuffer.toString());
			itemAd.setLink(linkBuffer.toString());
			itemAd.setImg(imgBuffer.toString());
			
			list.add(itemAd);
			
			itemAd = null;
			idbBuffer = null;
			linkBuffer = null;
			imgBuffer = null;
		}
		preTag = null;
	}
}
