package cn.sciencenet.httpclient;


public class XmlItemNews {
	private String id;
	private String title;
	private String link;
	private String imgs; // 该新闻有没有图片,如果有的话将链接存上
	private String description;
	private String copyright;
	private String pubDate;
	private String comment;
	
	public String getId() {
		return id;
	}
	public void setId() {
		if (link != null && !link.equals("") && link.contains("id=")) {
			int tmpIndex = link.indexOf("id=");
			tmpIndex += 3;
			this.id = link.substring(tmpIndex);
		} else {
			this.id = String.valueOf(-1);
		}
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getImgs() {
		return imgs;
	}
	public void setImgs(String imgs) {
		this.imgs = imgs;
	}
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getCopyright() {
		return copyright;
	}
	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}
	public String getPubDate() {
		return pubDate;
	}
	public void setPubDate(String pubDate) {
		this.pubDate = pubDate;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
}
