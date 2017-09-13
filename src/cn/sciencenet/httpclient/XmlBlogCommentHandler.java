package cn.sciencenet.httpclient;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.text.Html;

public class XmlBlogCommentHandler extends DefaultHandler {
	private List<XmlItemBlogComment> list = null;
	private XmlItemBlogComment itemBlogComment = null;
	private String preTag = null;

	// 无评论回复
	private StringBuffer messageBuffer = null;
	private StringBuffer usernameBuffer = null;
	private StringBuffer uidBuffer = null;
	private StringBuffer datelineBuffer = null;
	// 有回复
	private StringBuffer messageBuffer1 = null;
	private StringBuffer usernameBuffer1 = null;
	private StringBuffer uidBuffer1 = null;
	private StringBuffer datelineBuffer1 = null;

	private XmlItemCommentMessage messageItem = null;
	private XmlItemCommentMessage replayItem = null;

	private boolean ItemFlag = false;// true表示“item”
	private boolean ReplayFlag = false;// true表示“replay”

	public List<XmlItemBlogComment> getBlogComments(InputStream xmlStream)
			throws Exception {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		XmlBlogCommentHandler handler = new XmlBlogCommentHandler();
		InputStreamReader reader = new InputStreamReader(xmlStream, "GBK");
		InputSource is = new InputSource(reader);
		parser.parse(is, handler);
		return handler.getBlogComments();
	}

	public List<XmlItemBlogComment> getBlogComments() {
		return list;
	}

	@Override
	public void startDocument() throws SAXException {
		this.list = new ArrayList<XmlItemBlogComment>();
	}

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if ("item".equals(qName)) {
			itemBlogComment = new XmlItemBlogComment();
			messageItem = new XmlItemCommentMessage();

			messageBuffer = new StringBuffer();
			usernameBuffer = new StringBuffer();
			uidBuffer = new StringBuffer();
			datelineBuffer = new StringBuffer();

			ItemFlag = true;
		} else if ("replay".equals(qName)) {
			replayItem = new XmlItemCommentMessage();

			messageBuffer1 = new StringBuffer();
			usernameBuffer1 = new StringBuffer();
			uidBuffer1 = new StringBuffer();
			datelineBuffer1 = new StringBuffer();

			ItemFlag = false;
			ReplayFlag = true;
		}
		preTag = qName;
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		if (preTag != null) {
			String content = new String(ch, start, length);
			if (ItemFlag) {
				if ("message".equals(preTag)) {
					messageBuffer.append(Html.fromHtml(content));
				} else if ("username".equals(preTag)) {
					usernameBuffer.append(Html.fromHtml(content));
				} else if ("uid".equals(preTag)) {
					uidBuffer.append(Html.fromHtml(content));
				} else if ("dateline".equals(preTag)) {
					datelineBuffer.append(Html.fromHtml(content));
				}
			}
			if(ReplayFlag & ItemFlag == false){
				if ("message".equals(preTag)) {
					messageBuffer1.append(Html.fromHtml(content));
				} else if ("username".equals(preTag)) {
					usernameBuffer1.append(Html.fromHtml(content));
				} else if ("uid".equals(preTag)) {
					uidBuffer1.append(Html.fromHtml(content));
				} else if ("dateline".equals(preTag)) {
					datelineBuffer1.append(Html.fromHtml(content));
				}
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if ("item".equals(qName)) {
			messageItem.setMessage(messageBuffer.toString());
			messageItem.setUsername(usernameBuffer.toString());
			messageItem.setUid(uidBuffer.toString());
			messageItem.setDateline(datelineBuffer.toString());

			itemBlogComment.setMessageItem(messageItem);

			list.add(itemBlogComment);

			itemBlogComment = null;
			messageItem = null;
			replayItem = null;
			messageBuffer = null;
			usernameBuffer = null;
			uidBuffer = null;
			datelineBuffer = null;
			ItemFlag = false;
			
		} else if ("replay".equals(qName)) {
			replayItem.setMessage(messageBuffer1.toString());
			replayItem.setUsername(usernameBuffer1.toString());
			replayItem.setUid(uidBuffer1.toString());
			replayItem.setDateline(datelineBuffer1.toString());

			itemBlogComment.setReplayItem(replayItem);
			
			messageBuffer1 = null;
			usernameBuffer1 = null;
			uidBuffer1 = null;
			datelineBuffer1 = null;
			ReplayFlag = false;
			ItemFlag = true;
			return;
		}
		preTag = null;
	}
}
