package cn.sciencenet.activity;

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
import android.view.View.OnClickListener;
import android.view.Window;
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
import cn.sciencenet.httpclient.XmlGroupDetailHandler;
import cn.sciencenet.util.AppUtil;
import cn.sciencenet.util.DataUrlKeys;
import cn.sciencenet.util.DateUtil;
import cn.sciencenet.util.EncryptBySHA1;
import cn.sciencenet.util.HttpUtil;
import cn.sciencenet.util.NetWorkState;

public class GroupDetailActivity extends Activity {
	private static final String TAG = "GroupDetailActivity";

	// 以下的数据是从群组列表Activity传过来的bundle获取到的
	private int currentListIndex;
	private String currentGroupID; // 当前的群组Id
	private String currentGroupLink;//当前群组的link
	private ArrayList<String> groupIdList;
	private ArrayList<String> groupDescriptionList;
	private ArrayList<String> groupLinkList;
	private String currentGroupDescription;

	// 以下数据的获取是解析某一篇具体群组的XML获取到的
	private String tid = ""; // 群组的id
	private String title = ""; // 群组的题目
	private String link = ""; // 群组的原链接，注意使用时需要加上URL的前缀
	private String description = ""; // 群组的具体内容
	private String copyright = ""; // 群组的作者
	private String pubDate = ""; // 群组的发布日期

//	private static int currentFontSizeFlag = 1; // 字体大小标志位，0代表normal，1代表larger，2代表largest

	private XmlGroupDetailHandler xmlHandler = new XmlGroupDetailHandler();

	final String mimeType = "text/html";
	final String encoding = "utf-8";

	private Button goBackButton; // 返回按钮
	private ImageView previous; // 显示上一篇文章的按钮
	private ImageView next; // 显示下一篇文章的按钮
	private WebView contentWebView;
	private ViewSwitcher viewSwitcher;

	private Thread getGroupDetailThread;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 防止休眠
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// 去掉顶部灰条
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.content);
		((TextView) findViewById(R.id.content_title)).setText("群组");

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
//		webSetting.setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
//		webSetting.setTextSize(WebSettings.TextSize.LARGER);
		AppUtil.setFont(contentWebView);
		webSetting.setCacheMode(WebSettings.LOAD_DEFAULT
				| WebSettings.LOAD_CACHE_ELSE_NETWORK);

		// 返回按钮
		goBackButton = (Button) findViewById(R.id.go_back);
		goBackButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				GroupDetailActivity.this.finish();
			}
		});

		// 显示上一篇群组
		previous = (ImageView) findViewById(R.id.previous);
		previous.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (viewSwitcher.getDisplayedChild() == 1) { // 当前的群组还没刷出来
					Toast.makeText(GroupDetailActivity.this, "请等待当前群组刷新完毕",
							Toast.LENGTH_SHORT).show();
					return;
				}
				if (--currentListIndex < 0) {
					Toast.makeText(GroupDetailActivity.this, "已经是第一篇群组",
							Toast.LENGTH_SHORT).show();
					currentListIndex++;
					return;
				}
				currentGroupID = groupIdList.get(currentListIndex);
				currentGroupDescription = groupDescriptionList
						.get(currentListIndex);
				currentGroupLink = groupLinkList.get(currentListIndex);
				getGroupDetailThread = getThreadInstace();
				getGroupDetailThread.start();
			}
		});

		// 显示下一篇群组
		next = (ImageView) findViewById(R.id.next);
		next.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (viewSwitcher.getDisplayedChild() == 1) { // 当前的群组还没刷出来
					Toast.makeText(GroupDetailActivity.this, "请等待当前群组刷新完毕",
							Toast.LENGTH_SHORT).show();
					return;
				}
				if (++currentListIndex > groupIdList.size() - 1) {
					Toast.makeText(GroupDetailActivity.this, "已经是最后一篇群组",
							Toast.LENGTH_SHORT).show();
					currentListIndex--;
					return;
				}
				currentGroupID = groupIdList.get(currentListIndex);
				currentGroupDescription = groupDescriptionList
						.get(currentListIndex);
				currentGroupLink = groupLinkList.get(currentListIndex);
				getGroupDetailThread = getThreadInstace();
				getGroupDetailThread.start();
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
		// 群组列表传过来的数据
		currentListIndex = getIntent().getIntExtra("current_group_index", 0);
		currentGroupID = getIntent().getStringExtra("current_group_id");
		currentGroupLink = getIntent().getStringExtra("current_group_link");
		groupIdList = getIntent().getStringArrayListExtra("group_id_list");
		groupDescriptionList = getIntent().getStringArrayListExtra(
				"group_description_list");
		currentGroupDescription = getIntent().getStringExtra(
				"current_group_description");
		groupLinkList = getIntent().getStringArrayListExtra("group_link_list");

		// 启动新线程获取数据
		getGroupDetailThread = getThreadInstace();
		getGroupDetailThread.start();
	}

	/**
	 * 为请求详细内容的线程新建一个实例
	 */
	private Thread getThreadInstace() {
		return new Thread() {
			@Override
			public void run() {
				// http进行xml解析获取某篇群组的数据
				handler.sendEmptyMessage(-1); // 显示转圈的进度条
				getGroupDetails(currentGroupID);
				handler.sendEmptyMessage(1); // 显示webView
			}
		};
	}

	/**
	 * 获取群组的具体内容，此方法在子线程中被调用
	 */
	private void getGroupDetails(String groupId) {
		if (!NetWorkState.isNetworkAvailable(this)) { // 判断网络连接情况
			handler.sendEmptyMessage(0);
			return;
		}
		try {
			String pass = EncryptBySHA1.Encrypt(DateUtil.getCurrentDate());
			URL url = new URL(DataUrlKeys.GROUP_DETAIL_URL.replace("$id",
					currentGroupID) + pass);
			Log.e(TAG,
					DataUrlKeys.GROUP_DETAIL_URL.replace("$id", currentGroupID)
							+ pass);
			URLConnection con = url.openConnection();
			con.connect();
			InputStream input = con.getInputStream();
			Bundle bundle = xmlHandler.getGroupDetails(input);

			tid = bundle.getString("group_id");
			title = bundle.getString("group_title");
			link = bundle.getString("group_link");
			description = bundle.getString("group_description");
			copyright = bundle.getString("group_copyright");
			pubDate = bundle.getString("group_pubdate");
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
			InputStream in = getAssets().open("context_blog.html"); // 群组的和博客的具体内容版面是一样的，可以直接用blog的
			byte[] tmp = HttpUtil.readInputStream(in);
			htmlContent = new String(tmp);
			checkParams(); // 最后校验属性，防止异常标签引发的异常
			AppUtil.setWebViewLayout(contentWebView, description); // 如果具体内容包含表格，则改变webview的布局逻辑
			contentWebView.loadDataWithBaseURL(
					"http://news.sciencenet.cn",
					htmlContent.replace("@title", title)
							.replace("@copyright", copyright)
							.replace("@dateLine", pubDate)
							.replace("@content", description), mimeType,
					encoding, null);
		} catch (Exception e) {
			handler.sendEmptyMessage(2); // 出现异常
			e.printStackTrace();
		}
	}

	/**
	 * 做最后的校验，校验要被显示到UI上的某篇群组具体内容的各属性是否为空
	 */
	private void checkParams() {
		this.title = (title == null || title.equals("")) ? "未知" : title;
		this.copyright = (copyright == null || copyright.equals("")) ? "未知"
				: copyright;
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
				Toast.makeText(GroupDetailActivity.this, "网络连接不可用，请检查你的网络连接",
						Toast.LENGTH_LONG).show();
			} else if (msg.what == 2) {
				Toast.makeText(GroupDetailActivity.this, "抱歉，出现未知异常",
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
			// TODO 在这里打开该条群组评论的页面
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
			Log.e("groupsharelink","http://bbs.sciencenet.cn/" + currentGroupLink);
			shareContent("《" + title + "》,原文链接：" +  "http://bbs.sciencenet.cn/" + currentGroupLink + " 分享自：科学网Android客户端。");
			break;
		case R.id.menu_news_shoucang:
			// TODO 在这里将该群组加入收藏
			addToCollection();
			break;
		}
		return super.onOptionsItemSelected(mi);
	}

	/**
	 * 将当前的群组加入收藏
	 */
	private void addToCollection() {
		DBManager manager = new DBManager(GroupDetailActivity.this);
		try {
			manager.addOneCollection(CollectionItem.TYPE_GROUP, currentGroupID, "[群组]" + this.title,
					this.currentGroupDescription, "http://news.sciencenet.cn");
			Toast.makeText(GroupDetailActivity.this, "已添加到收藏",
					Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Toast.makeText(GroupDetailActivity.this, "该文章已经在收藏中", Toast.LENGTH_LONG).show();
		} finally {
			manager.closeDB();
		}
	}

	/**
	 * 重定向到评论activity
	 */
	private void redirectCommentActivity() {
		Intent intent = new Intent();
		try {
			intent.setClass(GroupDetailActivity.this, BlogCommentActivity.class);
			Bundle bundle = new Bundle();
			String tagString = "group_comment";
			
			bundle.putString("can_comment", "-1");
			bundle.putString("tag", tagString);
			bundle.putString("id", currentGroupID);

			intent.putExtras(bundle);
			startActivityForResult(intent, 0);
		} catch (Exception e) {
			e.printStackTrace();
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
