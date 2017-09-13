package cn.sciencenet.activity;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import cn.sciencenet.R;
import cn.sciencenet.httpclient.XmlAdHandler;
import cn.sciencenet.httpclient.XmlBlogHandler;
import cn.sciencenet.httpclient.XmlItemAd;
import cn.sciencenet.httpclient.XmlItemBlog;
import cn.sciencenet.util.AppUtil;
import cn.sciencenet.util.AsyncImageLoader;
import cn.sciencenet.util.AsyncImageLoader.ImageCallback;
import cn.sciencenet.util.DataUrlKeys;
import cn.sciencenet.util.DateUtil;
import cn.sciencenet.util.EncryptBySHA1;
import cn.sciencenet.util.NetWorkState;
import cn.sciencenet.view.MyListView;
import cn.sciencenet.view.MyListView.OnRefreshListener;

public class ScienceNetBlogActivity extends Activity {

	private static final String TAG = "ScienceNetBlogActivity";
	public static final String REFRESH_NOW = "cn.sciencenet.ScienceNetBlogActivity.RefreshNow";
	public static final String DO_SEARCH = "cn.sciencenet.ScienceNewBlogActivity.DoSearch";

	private MyListView blogList;

	private List<XmlItemBlog> list;
	private List<XmlItemBlog> lastList;
	private List<XmlItemAd> adList;
	private XmlBlogHandler xmlHandler;
	private XmlAdHandler xmlAdHandler;

	private BlogAdapter adapter;

	private ViewSwitcher viewSwitcher;

	private Button bt;
	private ProgressBar pg;
	private View moreView;

	private RefreshReceiver refreReceiver;
	private DoSearchReceiver doSearchReceiver;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 去掉顶部灰条
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.layout_news);
		setTheme(android.R.style.Theme_Translucent_NoTitleBar);

		initViews();

		xmlHandler = new XmlBlogHandler();
		xmlAdHandler = new XmlAdHandler();
		requestBlog();

		IntentFilter filter = new IntentFilter(REFRESH_NOW);
		refreReceiver = new RefreshReceiver();
		registerReceiver(refreReceiver, filter);

		// 注册用来搜索的BroadcastReceiver
		IntentFilter dosearchFilter = new IntentFilter(DO_SEARCH);
		doSearchReceiver = new DoSearchReceiver();
		registerReceiver(doSearchReceiver, dosearchFilter);
	}

	private class RefreshReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			refreshPage();
		}
	}

	private class DoSearchReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			doSearch(TabScienceNetActivity.searchString);
		}

	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(refreReceiver);
		unregisterReceiver(doSearchReceiver);
		super.onDestroy();
	}

	/**
	 * 初始化页面
	 */
	private void initViews() {
		viewSwitcher = (ViewSwitcher) findViewById(R.id.viewswitcher_news);
		blogList = new MyListView(this);
		blogList.setCacheColorHint(Color.argb(0, 0, 0, 0));
		blogList.setDivider(getResources().getDrawable(
				R.drawable.list_divider_line));
		blogList.setDividerHeight(1);
		blogList.setSelector(R.drawable.list_item_selector);
		blogList.setonRefreshListener(refreshListener);
		blogList.setOnItemClickListener(listener);

		viewSwitcher.addView(blogList);
		viewSwitcher.addView(getLayoutInflater().inflate(
				R.layout.layout_progress_page, null));
		viewSwitcher.showNext();

		// 实例化底部布局
		moreView = getLayoutInflater().inflate(R.layout.moredata, null);

		bt = (Button) moreView.findViewById(R.id.bt_load);
		pg = (ProgressBar) moreView.findViewById(R.id.pg);

		// 加上底部view
		bt.setOnClickListener(bt_listener);
	}

	private OnRefreshListener refreshListener = new OnRefreshListener() {

		@Override
		public void onRefresh() {
			TabScienceNetActivity.isSearch = false;
			refreshPage();
		}
	};

	/**
	 * 刷新当前页面
	 */
	private void refreshPage() {
		new AsyncTask<String, Integer, String>() {
			@Override
			protected String doInBackground(String... params) {
				getBlogList();
				return null;
			}

			protected void onPostExecute(String result) {
				if (adapter != null) {
					adapter.notifyDataSetChanged();
					blogList.onRefreshComplete();
				}
			}
		}.execute("begin");
	}

	/**
	 * 请求博客列表
	 */
	private void requestBlog() {
		Thread t = new Thread() {
			@Override
			public void run() {
				getBlogList();
			}
		};
		t.start();
	}

	/**
	 * 获取更多博客列表
	 */
	private void getMoreBlogList(URL url) {
		if (!NetWorkState.isNetworkAvailable(this)) {
			handler.sendEmptyMessage(0);
			return;
		}
		try {
			// 获取博客列表，存到临时的list里边
			
			URLConnection con = url.openConnection();
			con.connect();
			InputStream input = con.getInputStream();
			lastList = xmlHandler.getBlogItems(input);

			if (lastList.size() == 0) {
				handler.sendEmptyMessage(-1);
			} else {
				for (XmlItemBlog xib : lastList) {
					list.add(xib);
				}
				handler.sendEmptyMessage(2);
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取搜索博客的更多
	 * @param url
	 */
	private void getSearchMoreList(URL url){
		if (!NetWorkState.isNetworkAvailable(this)) {
			handler.sendEmptyMessage(0);
			return;
		}
		try {
			// 获取博客列表，存到临时的list里边
			URLConnection con = url.openConnection();
			con.connect();
			InputStream input = con.getInputStream();
			InputStream nInputStream = AppUtil.transferInputStream(input);
			
			lastList = xmlHandler.getBlogItems(nInputStream);

			if (lastList.size() == 0) {
				handler.sendEmptyMessage(-1);
			} else {
				for (XmlItemBlog xib : lastList) {
					list.add(xib);
				}
				handler.sendEmptyMessage(2);
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 获取博客列表
	 */
	private void getBlogList() {
		if (!NetWorkState.isNetworkAvailable(this)) {
			handler.sendEmptyMessage(0);
			return;
		}
		try {
			String pass = EncryptBySHA1.Encrypt(DateUtil.getCurrentDate());
			URL url = new URL(DataUrlKeys.BLOG_LIST_URL + pass);
			Log.i("BlogListUrl", DataUrlKeys.NEWS_LIST_URL + pass);
			URLConnection con = url.openConnection();
			con.connect();
			InputStream input = con.getInputStream();
			
			list = xmlHandler.getBlogItems(input);

			if (list.size() == 0) {
				handler.sendEmptyMessage(-1);
			} else {
				handler.sendEmptyMessage(1);
			}

			// 获取博客的广告
			url = new URL(DataUrlKeys.AD_BLOG);
			con = url.openConnection();
			con.connect();
			input = con.getInputStream();
			adList = xmlAdHandler.getAdItem(input);
			if (adList.size() != 0) {
				handler.sendEmptyMessage(5);
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	private Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 1) {
				Log.i(TAG + " handler", "--handler msg received--");
				adapter = new BlogAdapter();
				blogList.setAdapter(adapter);
				if (blogList.getFooterViewsCount() == 0) {
					blogList.addFooterView(moreView);
				}
				viewSwitcher.setDisplayedChild(0);
				blogList.onRefreshComplete();
			} else if (msg.what == -1) {
				Toast.makeText(ScienceNetBlogActivity.this, "服务器出现异常，请稍后使用",
						Toast.LENGTH_SHORT).show();
			} else if (msg.what == 0) {
				Toast.makeText(ScienceNetBlogActivity.this,
						"网络连接不可用，请检查你的网络连接", Toast.LENGTH_SHORT).show();
			} else if (msg.what == 2) {
				adapter.notifyDataSetChanged();
				pg.setVisibility(View.GONE);
				bt.setVisibility(View.VISIBLE);
			} else if (msg.what == 3) {
				viewSwitcher.setDisplayedChild(1);
			} else if (msg.what == 4) {
				Toast.makeText(ScienceNetBlogActivity.this, "无搜索结果",
						Toast.LENGTH_LONG).show();
				refreshPage();
			} else if (msg.what == 5) { // 广告请求完毕，可以刷新
				if (adapter != null) {
					adapter.notifyDataSetChanged();
					blogList.onRefreshComplete();
				}
			} else if (msg.what == 6) {
				adapter = new BlogAdapter();
				blogList.setAdapter(adapter);
				viewSwitcher.setDisplayedChild(0);
				blogList.onRefreshComplete();
//				blogList.removeFooterView(moreView);
			}
		}
	};

	/**
	 * 执行搜索的线程
	 * 
	 * @param searchContent
	 */
	private void doSearch(String searchContent) {
		final String tmpSearchContent = searchContent;
		Thread t = new Thread() {
			@Override
			public void run() {
				getSearchList(tmpSearchContent);
			}
		};
		t.start();
	}

	String tmptmpURL;

	/**
	 * 通过给定的关键字获取搜索结果列表
	 * 
	 * @param searchContent
	 *            搜索的内容
	 */
	private void getSearchList(String searchContent) {
		if (!NetWorkState.isNetworkAvailable(this)) { // 判断网络连接情况
			handler.sendEmptyMessage(0);
			return;
		}
		try {
			handler.sendEmptyMessage(3); // 显示进度条

			URL url = new URL(DataUrlKeys.SEARCH_BLOG_URL.replace(
					"$searchContent", searchContent));
			tmptmpURL = url.toString();
			Log.e("tmpUrl", tmptmpURL);

			URLConnection con = url.openConnection();
			con.connect();
			InputStream input = con.getInputStream();

			InputStream nInputStream = AppUtil.transferInputStream(input);

			list = xmlHandler.getBlogItems(nInputStream);

			if (list.size() == 0) {
				handler.sendEmptyMessage(4); // 没有搜索结果
			} else {
				handler.sendEmptyMessage(6);
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private OnClickListener bt_listener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			pg.setVisibility(View.VISIBLE);
			bt.setVisibility(View.GONE);

			if(TabScienceNetActivity.isSearch)
				getSearchMoreThread().start();
			else
				getMoreDataThread().start();
		}
	};

	private OnItemClickListener listener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (position == 0) {
				return;
			} else if (position == 1 && adList != null && adList.size() != 0) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(adList
						.get(0).getLink())));
			} else {
				redirectDetailActivity(position - 2);
			}
		}
	};

	/**
	 * 获取更多博客的线程
	 * 
	 * @return
	 */
	private Thread getMoreDataThread() {
		return new Thread() {
			@Override
			public void run() {
				String lastId = list.get(list.size() - 1).getBlogid();
				try {
					URL url = new URL(DataUrlKeys.MORE_BLOG_LIST_URL.replace(
							"$since_id", lastId));
					getMoreBlogList(url);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		};
	}
	
	/**
	 * get search more blog list thread
	 * @return
	 */
	private Thread getSearchMoreThread() {
		return new Thread() {
			@Override
			public void run() {
				String lastId = list.get(list.size() - 1).getBlogid();
				try {
					URL url = new URL(DataUrlKeys.SEARCH_MORE_BLOG_URL.replace(
							"$since_id", lastId).replace("$searchContent", TabScienceNetActivity.searchString));
					getSearchMoreList(url);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		};
	}

	/**
	 * 打开显示具体博客内容的activity
	 * 
	 * @param listIndex
	 *            触发点击事件的那条博客在博客list里面的索引
	 */
	private void redirectDetailActivity(int listIndex) {
		Intent intent = new Intent();
		try {
			intent.setClass(ScienceNetBlogActivity.this,
					BlogDetailActivity.class);
			Bundle bundle = new Bundle();
			String tmpId = list.get(listIndex).getBlogid(); // current_blog_id
			String tmpCopyRight = list.get(listIndex).getCopyright(); // current_blog_copyright
			String tmpLink = list.get(listIndex).getLink();//current_blog_link
			int tmpIndex = listIndex; // current_blog_index
			ArrayList<String> tmpBlogIdList = new ArrayList<String>(); // blog_id_list
			ArrayList<String> tmpBlogCopyrightList = new ArrayList<String>(); // blog_copyright_list
			ArrayList<String> tmpBlogDescriptionList = new ArrayList<String>(); // blog_description_list
			ArrayList<String> tmpBlogLinkList = new ArrayList<String>();//blog_link_list 
			String tmpDescription = list.get(listIndex).getDescription(); // current_blog_description

			for (XmlItemBlog item : list) {
				tmpBlogIdList.add(item.getBlogid());
				// Log.e("item.getBlogid()", "--" + item.getBlogid() + "--");
				tmpBlogCopyrightList.add(item.getCopyright());
				// Log.e("item.getCopyright()", "--" + item.getCopyright() +
				// "--");
				tmpBlogDescriptionList.add(item.getDescription());
				tmpBlogLinkList.add(item.getLink());
			}

			bundle.putString("current_blog_id", tmpId);
			bundle.putString("current_blog_copyright", tmpCopyRight);
			bundle.putInt("current_blog_index", tmpIndex);
			bundle.putString("current_blog_description", tmpDescription);
			bundle.putString("current_blog_link", tmpLink);
			
			bundle.putStringArrayList("blog_id_list", tmpBlogIdList);
			bundle.putStringArrayList("blog_copyright_list",
					tmpBlogCopyrightList);
			bundle.putStringArrayList("blog_description_list",
					tmpBlogDescriptionList);
			bundle.putStringArrayList("blog_link_list", tmpBlogLinkList);

			intent.putExtras(bundle);

			startActivityForResult(intent, 0);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public class BlogAdapter extends BaseAdapter {

		final int VIEW_TYPE = 2;
		final int TYPE_AD = 1;
		final int TYPE_NORMAL = 0;

		AsyncImageLoader asyncImageLoader = new AsyncImageLoader(
				AppUtil.ITEM_IMG_WIDTH, AppUtil.ITEM_IMG_HEIGHT);

		@Override
		public int getItemViewType(int positon) {
			if (positon == 0) {
				return TYPE_AD;
			} else {
				return TYPE_NORMAL;
			}
		}

		@Override
		public int getViewTypeCount() {
			return VIEW_TYPE;
		}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			return list.get(position - 1);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			BlogViewHolder holder = null;
			AdBarHolder holderAd = null;

			int type = getItemViewType(position);

			if (convertView == null) {
				switch (type) {
				case TYPE_NORMAL:
					holder = new BlogViewHolder();
					convertView = getLayoutInflater().inflate(
							R.layout.news_list_item, null);
					holder.tv_title = (TextView) convertView
							.findViewById(R.id.news_item_title);
					holder.tv_descirption = (TextView) convertView
							.findViewById(R.id.news_item_description);
					convertView.setTag(holder);
					break;
				case TYPE_AD:
					holderAd = new AdBarHolder();
					convertView = getLayoutInflater().inflate(R.layout.ad_item,
							null);
					holderAd.iv_ad = (ImageView) convertView
							.findViewById(R.id.ad);
					convertView.setTag(holderAd);
					break;
				default:
					break;
				}

			} else {
				switch (type) {
				case TYPE_NORMAL:
					holder = (BlogViewHolder) convertView.getTag();
					break;
				case TYPE_AD:
					holderAd = (AdBarHolder) convertView.getTag();
					break;
				default:
					break;
				}

			}

			switch (type) {
			case TYPE_NORMAL:
				holder.tv_title.setText(list.get(position - 1).getTitle());
				holder.tv_descirption.setText(list.get(position - 1)
						.getDescription());
				break;
			case TYPE_AD:
				if (adList == null || adList.size() == 0) {
					break;
				}
				holderAd.iv_ad.setTag(adList.get(0).getImg());
				Drawable cachedImageAd = asyncImageLoader.loadDrawable(adList
						.get(0).getImg(), new ImageCallback() {
					@Override
					public void imageLoaded(Drawable imageDrawable,
							String imageUrl) {
						ImageView imageViewByTag = (ImageView) blogList
								.findViewWithTag(imageUrl);
						if (imageViewByTag != null && imageDrawable != null) {
							imageViewByTag.setImageDrawable(imageDrawable);
						} 
					}
				});
				if (cachedImageAd != null) {
					holderAd.iv_ad.setImageDrawable(cachedImageAd);
				}
				break;
			default:
				break;
			}

			return convertView;
		}

		public class BlogViewHolder {
			TextView tv_title;
			TextView tv_descirption;
		}

		public class AdBarHolder {
			ImageView iv_ad;
		}
	}
}
