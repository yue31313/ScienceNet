package cn.sciencenet.activity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

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
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import cn.sciencenet.R;
import cn.sciencenet.datastorage.CollectionItem;
import cn.sciencenet.datastorage.DBManager;
import cn.sciencenet.httpclient.XmlNewspaperContentHandler;
import cn.sciencenet.util.AppUtil;
import cn.sciencenet.util.DataUrlKeys;
import cn.sciencenet.util.DateUtil;
import cn.sciencenet.util.EncryptBySHA1;
import cn.sciencenet.util.FileAccess;
import cn.sciencenet.util.NetWorkState;

public class NewspaperContentActivity extends Activity {

	// 以下三个字段为从报刊新闻list的activity传过来的bundle获取的
	private int currentListIndex;
	private String currentNewspaperNewsId;// 获取的具体新闻ID
	private ArrayList<String> newspaperIdList;
	private String canComment;// 该篇报刊是否可以被评论，1表示可以被评论，0不可被评论
	private ArrayList<String> newspaperCommentList;
	private ArrayList<String> newspaperDescriptionList;
	private String currentNewspaperDescription;

	// 以下数据是解析具体报刊新闻的XML而得到的
	private String title = "";
	private String copyright = "";
	private String link = "";
	private String imgs = "";
	private String description = "";
	private String sourse = "";
	private String pubDate = "";
	private String comment = "";

	// private static int currentFontSizeFlag = 1;//
	// 字体大小标志位，0代表normal，1代表larger，2代表largest

	private XmlNewspaperContentHandler xmlHandler;

	final String mimeType = "text/html";
	final String encoding = "utf-8";

	private Button goBackButton; // 返回按钮
	private ImageView previous; // 显示上一篇文章的按钮
	private ImageView next; // 显示下一篇文章的按钮
	private WebView contentWebView;
	private ViewSwitcher viewSwitcher;

	private Thread getNewspaperDetailThread;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 防止休眠
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// 去掉顶部灰条
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.content);
		((TextView) findViewById(R.id.content_title)).setText("报刊");

		xmlHandler = new XmlNewspaperContentHandler();

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
		webSetting.setCacheMode(WebSettings.LOAD_DEFAULT
				| WebSettings.LOAD_CACHE_ELSE_NETWORK);

		// 返回按钮
		goBackButton = (Button) findViewById(R.id.go_back);
		goBackButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				NewspaperContentActivity.this.finish();
			}
		});

		// 显示上一篇报刊新闻
		previous = (ImageView) findViewById(R.id.previous);
		previous.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (viewSwitcher.getDisplayedChild() == 1) { // 当前的报刊新闻还没刷出来
					Toast.makeText(NewspaperContentActivity.this,
							"请等待当前报刊新闻刷新完毕", Toast.LENGTH_SHORT).show();
					return;
				}
				if (--currentListIndex < 0) {
					Toast.makeText(NewspaperContentActivity.this, "已经是第一篇文章",
							Toast.LENGTH_SHORT).show();
					currentListIndex++;
					return;
				}
				currentNewspaperNewsId = newspaperIdList.get(currentListIndex);
				canComment = newspaperCommentList.get(currentListIndex);
				currentNewspaperDescription = newspaperDescriptionList
						.get(currentListIndex);
				getNewspaperDetailThread = getThreadInstace();
				getNewspaperDetailThread.start();
			}
		});

		// 显示下一篇新闻
		next = (ImageView) findViewById(R.id.next);
		next.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (viewSwitcher.getDisplayedChild() == 1) { // 当前的报刊新闻还没刷出来
					Toast.makeText(NewspaperContentActivity.this,
							"请等待当前报刊新闻刷新完毕", Toast.LENGTH_SHORT).show();
					return;
				}
				if (++currentListIndex > newspaperIdList.size() - 1) {
					Toast.makeText(NewspaperContentActivity.this, "已经是最后一篇文章",
							Toast.LENGTH_SHORT).show();
					currentListIndex--;
					return;
				}
				currentNewspaperNewsId = newspaperIdList.get(currentListIndex);
				canComment = newspaperCommentList.get(currentListIndex);
				currentNewspaperDescription = newspaperDescriptionList
						.get(currentListIndex);
				getNewspaperDetailThread = getThreadInstace();
				getNewspaperDetailThread.start();
			}
		});

		viewSwitcher.addView(contentWebView);
		viewSwitcher.addView(getLayoutInflater().inflate(
				R.layout.layout_progress_page, null));
		viewSwitcher.showNext();
	}

	/**
	 * 为请求详细内容的线程新建一个实例
	 */
	private Thread getThreadInstace() {
		return new Thread() {
			@Override
			public void run() {
				// http进行xml解析获取某篇报刊新闻的数据
				handler.sendEmptyMessage(-1); // 显示转圈的进度条
				getNewsDetails(currentNewspaperNewsId);
				handler.sendEmptyMessage(1); // 显示webView
			}
		};
	}

	/**
	 * 初始化数据
	 */
	private void initData() {
		// 报刊列表传过来的数据
		this.currentNewspaperNewsId = getIntent().getStringExtra(
				"current_newspaper_list_id");
		this.newspaperIdList = getIntent().getStringArrayListExtra(
				"newspaper_id_list");
		this.currentListIndex = getIntent().getIntExtra(
				"current_newspaper_index", 0);
		this.canComment = getIntent().getStringExtra(
				"current_newspaper_comment");
		this.newspaperCommentList = getIntent().getStringArrayListExtra(
				"newspaper_comment_list");
		this.newspaperDescriptionList = getIntent().getStringArrayListExtra(
				"newspaper_description_list");
		this.currentNewspaperDescription = getIntent().getStringExtra(
				"current_newspaper_description");

		getNewspaperDetailThread = getThreadInstace();
		getNewspaperDetailThread.start();
	}

	/**
	 * 获取报刊新闻的具体内容
	 */
	private void getNewsDetails(String newspaper_Id) {
		if (!NetWorkState.isNetworkAvailable(this)) { // 判断网络连接情况
			handler.sendEmptyMessage(0);
			return;
		}
		try {
			String pass = EncryptBySHA1.Encrypt(DateUtil.getCurrentDate());
			String tmpUrl = DataUrlKeys.NEWPAPER_CONTENT_URL.replace("$pass",
					pass);
			URL url = new URL(tmpUrl.replace("$id", newspaper_Id));
			Log.i("NewsContentUrl", tmpUrl.replace("$id", newspaper_Id));
			URLConnection con = url.openConnection();
			con.connect();
			InputStream input = con.getInputStream();
			Bundle bundle = xmlHandler.getNewspaperContent(input);

			title = bundle.getString("newspaper_content_title");
			link = bundle.getString("newspaper_content_link");
			copyright = bundle.getString("newspaper_content_copyright");
			pubDate = bundle.getString("newspaper_content_pubDate");
			imgs = bundle.getString("newspaper_content_imgs");
			description = bundle.getString("newspaper_content_description");
			sourse = bundle.getString("newspaper_content_sourse");
			comment = bundle.getString("newspaper_content_comments");

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
			byte[] tmp = readInputStream(in);
			htmlContent = new String(tmp);
			checkParams(); // 防止异常标签引发的异常
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
			e.printStackTrace();
		}
	}

	/**
	 * 做最后的校验，校验要被显示到UI上的某篇报刊新闻具体内容的各属性是否为空
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
	 * 读取输入流
	 */
	public static byte[] readInputStream(InputStream inStream) throws Exception {
		ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int len = 0;
		while ((len = inStream.read(buffer)) != -1) {
			outSteam.write(buffer, 0, len);
		}
		outSteam.close();
		inStream.close();
		return outSteam.toByteArray();
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
				Toast.makeText(NewspaperContentActivity.this,
						"网络连接不可用，请检查你的网络连接", Toast.LENGTH_LONG).show();
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
			// TODO 在这里打开该条报刊新闻评论的页面
			redirectNewspaperCommentActivity();
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
			// TODO 在这里将该报刊新闻加入收藏
			addToCollection();
			break;
		}
		return super.onOptionsItemSelected(mi);
	}

	/**
	 * 将当前的报刊加入收藏
	 */
	private void addToCollection() {
		DBManager manager = new DBManager(NewspaperContentActivity.this);
		try {
			String imgName;
			if (!"http://news.sciencenet.cn".equals(imgs)) {
				imgName = imgs.substring(imgs.lastIndexOf("/") + 1);
				// 将加入收藏的新闻的item的图片转存一下
				FileAccess.restoreImg(
						DataUrlKeys.NEWSPAPER_LIST_IMG_CACHE_FOLDER, imgName,
						DataUrlKeys.COLLECTION_NEWSPAPER_ITEM_IMG_FOLDER);
			} else {
				imgName = imgs;
			}
			manager.addOneCollection(CollectionItem.TYPE_NEWSPAPER,
					currentNewspaperNewsId, "[报刊]" + this.title,
					this.currentNewspaperDescription, imgName);
			manager.addNpcontentCancomment(currentNewspaperNewsId, canComment);
			manager.closeDB();
			Toast.makeText(NewspaperContentActivity.this, "已添加到收藏",
					Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Toast.makeText(NewspaperContentActivity.this, "该文章已经在收藏中",
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

	/**
	 * 重定向到评论
	 */
	private void redirectNewspaperCommentActivity() {
		Intent intent = new Intent();
		try {
			intent.setClass(NewspaperContentActivity.this,
					ShowCommentActivity.class);
			Bundle bundle = new Bundle();
			String tagString = "newspaper_comment";

			bundle.putString("can_reply", canComment);
			bundle.putString("tag", tagString);
			bundle.putString("id", currentNewspaperNewsId);

			Log.i("put-newspaper-id", currentNewspaperNewsId);
			Log.e("put-newspaper-tag", tagString);

			intent.putExtras(bundle);

			startActivityForResult(intent, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
