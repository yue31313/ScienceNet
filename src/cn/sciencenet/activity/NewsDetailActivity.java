package cn.sciencenet.activity;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import cn.sciencenet.R;
import cn.sciencenet.datastorage.CollectionItem;
import cn.sciencenet.datastorage.DBManager;
import cn.sciencenet.httpclient.XmlNewsDetailHandler;
import cn.sciencenet.util.AppUtil;
import cn.sciencenet.util.DataUrlKeys;
import cn.sciencenet.util.DateUtil;
import cn.sciencenet.util.EncryptBySHA1;
import cn.sciencenet.util.FileAccess;
import cn.sciencenet.util.HttpUtil;
import cn.sciencenet.util.NetWorkState;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

public class NewsDetailActivity extends Activity {
	// 以下的数据是从新闻列表Activity传过来的bundle获取到的
	private int currentListIndex;
	private String currentNewsID; // 当前的新闻Id
	private ArrayList<String> newsIdList;
	private ArrayList<String> newsDescriptionList;
	private String currentNewsDescription;

	// 以下数据的获取是解析某一篇具体新闻的XML获取到的
	private String title = ""; // 新闻ID
	private String copyright = ""; // 新闻的作者
	private String pubDate = ""; // 新闻的发布日期
	private String link = ""; // 新闻的原URL
	private String imgs = ""; // 新闻的图片URL
	private String description = ""; // 新闻的具体内容
	private String sourse = ""; // 新闻的来源
	private String canComment = ""; // 1表示新闻允许评论，0表示不允许评论

	private XmlNewsDetailHandler xmlHandler = new XmlNewsDetailHandler();

	final String mimeType = "text/html";
	final String encoding = "utf-8";

	private Button goBackButton; // 返回按钮
	private ImageView previous; // 显示上一篇文章的按钮
	private ImageView next; // 显示下一篇文章的按钮
	private WebView contentWebView;
	private ViewSwitcher viewSwitcher;

	private Thread getNewsDetailThread;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 防止休眠
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// 去掉顶部灰条
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.content);

		initViews();
		initData();
	}

	/**
	 * 初始化View
	 */
	private void initViews() {
		viewSwitcher = (ViewSwitcher) findViewById(R.id.viewswitcher_content);

		contentWebView = new WebView(this);
		contentWebView.addJavascriptInterface(this, "javatojs");
		contentWebView.setScrollBarStyle(0);
		WebSettings webSetting = contentWebView.getSettings();
		webSetting.setDefaultTextEncodingName("utf-8");
		webSetting.setJavaScriptEnabled(true);
//		webSetting.setPluginsEnabled(true);
		webSetting.setNeedInitialFocus(false);
		webSetting.setSupportZoom(true);
		// webSetting.setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
		// webSetting.setTextSize(WebSettings.TextSize.LARGER);
		AppUtil.setFont(contentWebView);
		// webSetting.setTextSize(WebSettings.TextSize.LARGER);
		webSetting.setCacheMode(WebSettings.LOAD_DEFAULT
				| WebSettings.LOAD_CACHE_ELSE_NETWORK);

		// 返回按钮
		goBackButton = (Button) findViewById(R.id.go_back);
		goBackButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				NewsDetailActivity.this.finish();
			}
		});

		// 显示上一篇新闻
		previous = (ImageView) findViewById(R.id.previous);
		previous.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (viewSwitcher.getDisplayedChild() == 1) { // 当前的新闻还没刷出来
					Toast.makeText(NewsDetailActivity.this, "请等待当前新闻刷新完毕",
							Toast.LENGTH_SHORT).show();
					return;
				}
				if (--currentListIndex < 0) {
					Toast.makeText(NewsDetailActivity.this, "已经是第一篇新闻",
							Toast.LENGTH_SHORT).show();
					currentListIndex++;
					return;
				}
				currentNewsID = newsIdList.get(currentListIndex);
				currentNewsDescription = newsDescriptionList
						.get(currentListIndex);
				getNewsDetailThread = getThreadInstace();
				getNewsDetailThread.start();
			}
		});

		// 显示下一篇新闻
		next = (ImageView) findViewById(R.id.next);
		next.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (viewSwitcher.getDisplayedChild() == 1) { // 当前的新闻还没刷出来
					Toast.makeText(NewsDetailActivity.this, "请等待当前新闻刷新完毕",
							Toast.LENGTH_SHORT).show();
					return;
				}
				if (++currentListIndex > newsIdList.size() - 1) {
					Toast.makeText(NewsDetailActivity.this, "已经是最后一篇新闻",
							Toast.LENGTH_SHORT).show();
					currentListIndex--;
					return;
				}
				currentNewsID = newsIdList.get(currentListIndex);
				currentNewsDescription = newsDescriptionList
						.get(currentListIndex);
				getNewsDetailThread = getThreadInstace();
				getNewsDetailThread.start();
			}
		});

		viewSwitcher.addView(contentWebView);
		viewSwitcher.addView(getLayoutInflater().inflate(
				R.layout.layout_progress_page, null));
		viewSwitcher.showNext();
	}

	/**
	 * 初始化数据
	 */
	private void initData() {
		// 新闻列表传过来的数据
		this.currentNewsID = getIntent().getStringExtra("current_news_id");
		this.newsIdList = getIntent().getStringArrayListExtra("news_id_list");
		this.currentListIndex = getIntent()
				.getIntExtra("current_news_index", 0);
		this.newsDescriptionList = getIntent().getStringArrayListExtra(
				"news_description_list");
		this.currentNewsDescription = getIntent().getStringExtra(
				"current_news_description");

		// 启动新线程解析XML获取数据
		getNewsDetailThread = getThreadInstace();
		getNewsDetailThread.start();
	}

	/**
	 * 为请求详细内容的线程新建一个实例
	 */
	private Thread getThreadInstace() {
		return new Thread() {
			@Override
			public void run() {
				// http进行xml解析获取某篇新闻的数据
				handler.sendEmptyMessage(-1); // 显示转圈的进度条
				getNewsDetails(currentNewsID);
				handler.sendEmptyMessage(1); // 显示webView
			}
		};
	}

	/**
	 * 获取新闻的具体内容，此方法在子线程中被调用
	 */
	private void getNewsDetails(String newsId) {
		if (!NetWorkState.isNetworkAvailable(this)) { // 判断网络连接情况
			handler.sendEmptyMessage(0);
			return;
		}
		try {
			String pass = EncryptBySHA1.Encrypt(DateUtil.getCurrentDate());
			URL url = new URL(
					DataUrlKeys.NEWS_DETAIL_URL.replace("$pass", pass) + newsId);
			Log.i("NewsContentUrl",
					DataUrlKeys.NEWS_DETAIL_URL.replace("$pass", pass) + newsId);
			URLConnection con = url.openConnection();
			con.connect();
			InputStream input = con.getInputStream();
			Bundle bundle = xmlHandler.getNewsDetails(input);

			title = bundle.getString("news_title");
			link = bundle.getString("news_link");
			copyright = bundle.getString("news_copyright");
			pubDate = bundle.getString("news_pubDate");
			imgs = bundle.getString("news_imgs");
			description = bundle.getString("news_description");
			sourse = bundle.getString("news_sourse");
			canComment = bundle.getString("news_comment");

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 将信息显示到WebView上
	 */
	private void setWebView() {
		String htmlContent = "";
		try {
			InputStream in = getAssets().open("context.html");
			byte[] tmp = HttpUtil.readInputStream(in);
			htmlContent = new String(tmp);
			checkParams(); // 最后校验属性，防止异常标签引发的异常
			AppUtil.setWebViewLayout(contentWebView, description); // 如果具体内容包含表格，则改变webview的布局逻辑
			contentWebView.loadDataWithBaseURL(
					"http://news.sciencenet.cn",
					htmlContent.replace("@title", title)
							.replace("@copyright", copyright)
							.replace("@source", sourse)
							.replace("@pubdate", pubDate)
							.replace("@content", description), mimeType,
					encoding, null);
		} catch (Exception e) {
			handler.sendEmptyMessage(2); // 出现异常
			e.printStackTrace();
		}
	}

	/**
	 * 做最后的校验，校验要被显示到UI上的某篇新闻具体内容的各属性是否为空
	 */
	private void checkParams() {
		this.title = (title == null || title.equals("")) ? "未知" : title;
		this.copyright = (copyright == null || copyright.equals("")) ? "未知"
				: copyright;
		this.sourse = (sourse == null || sourse.equals("")) ? "未知" : sourse;
		this.pubDate = (pubDate == null || pubDate.equals("")) ? "未知" : pubDate;
		this.description = (description == null || description.equals("")) ? "未知"
				: description;
	}

	/**
	 * UI线程的handler
	 */
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 1) {
				setWebView();
				viewSwitcher.setDisplayedChild(0); // 显示设置好的webview
			} else if (msg.what == -1) {
				viewSwitcher.setDisplayedChild(1); // 显示转圈的进度条
			} else if (msg.what == 0) {
				Toast.makeText(NewsDetailActivity.this, "网络连接不可用，请检查你的网络连接",
						Toast.LENGTH_LONG).show();
			} else if (msg.what == 2) {
				Toast.makeText(NewsDetailActivity.this, "抱歉，出现未知异常",
						Toast.LENGTH_LONG).show();
			}
		}
	};

	/**
	 * 菜单
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.news_detail_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem mi) {
		switch (mi.getItemId()) {
		case R.id.menu_news_pinglun:
			// TODO 在这里打开该条新闻评论的页面
			redirectCommentActivity();
			break;
		case R.id.menu_news_zihao:
			// TODO 在这里改变WebView的字体大小，需先判断WebView是不是为空
			if (contentWebView != null) {
				changeFont();
			}
			break;
		case R.id.menu_news_fenxiang:
			// TODO 在这里实现分享的功能
			shareContent("《" + title + "》,原文链接：" + link + " 分享自：科学网Android客户端。");
			break;
		case R.id.menu_news_shoucang:
			// TODO 在这里将该新闻加入收藏
			addToCollection();
			break;
		}
		return super.onOptionsItemSelected(mi);
	}

	/**
	 * 重定向到新闻评论的Activity
	 */
	private void redirectCommentActivity() {
		Intent intent = new Intent();
		try {
			intent.setClass(NewsDetailActivity.this, ShowCommentActivity.class);
			Bundle bundle = new Bundle();
			String tagString = "news_comment";

			bundle.putString("can_reply", canComment);
			bundle.putString("tag", tagString);
			bundle.putString("id", currentNewsID);

			intent.putExtras(bundle);
			startActivityForResult(intent, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 将当前的文章加入收藏
	 */
	private void addToCollection() {
		DBManager manager = new DBManager(NewsDetailActivity.this);
		try {
			String imgName;
			if (!"http://news.sciencenet.cn".equals(imgs)) {
				// 将加入收藏的新闻的item的图片转存一下
				imgName = imgs.substring(imgs.lastIndexOf("/") + 1);
				FileAccess.restoreImg(DataUrlKeys.NEWS_LIST_IMG_CACHE_FOLDER,
						imgName, DataUrlKeys.COLLECTION_ITEM_IMG_FOLDER);
			} else {
				imgName = imgs;
			}
			manager.addOneCollection(CollectionItem.TYPE_NEWS, currentNewsID,
					"[新闻]" + this.title, currentNewsDescription, imgName);
			Toast.makeText(NewsDetailActivity.this, "已添加到收藏",
					Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Toast.makeText(NewsDetailActivity.this, "该文章已经在收藏中",
					Toast.LENGTH_LONG).show();
		} finally {
			manager.closeDB();
		}
	}

	/**
	 * 修改WebView的字体
	 */
	private void changeFont() {
		int toSetFlag = (++DataUrlKeys.currentFontSizeFlag) % 3;
		switch (toSetFlag) {
		case 0:
			contentWebView.getSettings().setTextSize(
					WebSettings.TextSize.NORMAL);
			break;
		case 1:
			contentWebView.getSettings().setTextSize(
					WebSettings.TextSize.LARGER);
			break;
		case 2:
			contentWebView.getSettings().setTextSize(
					WebSettings.TextSize.LARGEST);
			break;
		default:
			break;
		}
		DataUrlKeys.currentFontSizeFlag = toSetFlag;
	}

	/**
	 * 打开分享界面
	 * 
	 * @param 要分享的内容
	 */
	private void shareContent(String shareContent) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_SUBJECT, title);
		intent.putExtra(Intent.EXTRA_TEXT, shareContent);
		startActivity(Intent.createChooser(intent, title));
	}
}
