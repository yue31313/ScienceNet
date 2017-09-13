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
import android.view.LayoutInflater;
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
import cn.sciencenet.httpclient.XmlItemAd;
import cn.sciencenet.httpclient.XmlItemNews;
import cn.sciencenet.httpclient.XmlNewsHandler;
import cn.sciencenet.util.AppUtil;
import cn.sciencenet.util.AsyncImageLoader;
import cn.sciencenet.util.AsyncImageLoader.ImageCallback;
import cn.sciencenet.util.DataUrlKeys;
import cn.sciencenet.util.DateUtil;
import cn.sciencenet.util.EncryptBySHA1;
import cn.sciencenet.util.NetWorkState;
import cn.sciencenet.view.MyListView;
import cn.sciencenet.view.MyListView.OnRefreshListener;

public class ScienceNetNewsActivity extends Activity {

	private static final String TAG = "ScienceNetNewsActivity";
	public static final String REFRESH_NOW = "cn.sciencenet.ScienceNetNewsActivity.RefreshNow";
	public static final String DO_SEARCH = "cn.sciencenet.ScienceNetNewsActivity.DoSearch";

	private MyListView listView;

	private List<XmlItemNews> list;
	private List<XmlItemNews> lastList;
	private List<XmlItemAd> adList;
	private XmlNewsHandler xmlHandler;
	private XmlAdHandler xmlAdHandler;

	private NewsAdapter adapter;

	private ViewSwitcher viewSwitcher;

	private Button bt;
	private ProgressBar pg;
	private View moreView;

	private RefreshReceiver refreshReceiver;
	private DoSearchReceiver doSearchReceiver;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 去掉顶部灰条
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.layout_news);
		setTheme(android.R.style.Theme_Translucent_NoTitleBar);

		initViews();

		xmlHandler = new XmlNewsHandler();
		xmlAdHandler = new XmlAdHandler();
		requestNews(); // 刚启动的时候刷新一次新闻

		// 注册用来刷新的BroadcastReceiver
		IntentFilter refreshFilter = new IntentFilter(REFRESH_NOW);
		refreshReceiver = new RefreshReceiver();
		registerReceiver(refreshReceiver, refreshFilter);
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
		unregisterReceiver(refreshReceiver);
		unregisterReceiver(doSearchReceiver);
		super.onDestroy();
	}

	/**
	 * 初始化viewSwitcher
	 */
	private void initViews() {
		viewSwitcher = (ViewSwitcher) findViewById(R.id.viewswitcher_news);
		listView = new MyListView(this);
		listView.setCacheColorHint(Color.argb(0, 0, 0, 0));
		listView.setDivider(getResources().getDrawable(
				R.drawable.list_divider_line));
		listView.setDividerHeight(1);
		listView.setSelector(R.drawable.list_item_selector);
		listView.setonRefreshListener(refreshListener);
		listView.setOnItemClickListener(listener);

		viewSwitcher.addView(listView);
		viewSwitcher.addView(getLayoutInflater().inflate(
				R.layout.layout_progress_page, null));
		viewSwitcher.showNext();
		// listView.setOnItemClickListener(listener);

		// 实例化底部布局
		moreView = getLayoutInflater().inflate(R.layout.moredata, null);

		bt = (Button) moreView.findViewById(R.id.bt_load);
		pg = (ProgressBar) moreView.findViewById(R.id.pg);

		// 加上底部view
		bt.setOnClickListener(bt_listener);
	}

	/**
	 * 下拉刷新监听器
	 */
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
				getNewsList();
				return null;
			}

			@Override
			protected void onPostExecute(String result) {
				if (adapter != null) {
					adapter.notifyDataSetChanged();
					listView.onRefreshComplete();
				}
			}
		}.execute("begin");
	}

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

			URL url = new URL(DataUrlKeys.SEARCH_NEWS_URL.replace(
					"$searchContent", searchContent));
			tmptmpURL = url.toString();

			URLConnection con = url.openConnection();
			con.connect();
			InputStream input = con.getInputStream();
			list = xmlHandler.getNewsItems(input);

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

	/**
	 * 请求新闻列表的线程
	 */
	private void requestNews() {
		Thread t = new Thread() {
			@Override
			public void run() {
				getNewsList();
			}
		};
		t.start();
	}

	/**
	 * 通过http请求获取新闻列表,获取完列表之后再获取广告的URL
	 */
	private void getNewsList() {
		Log.i(TAG, "get news list!!!");
		if (!NetWorkState.isNetworkAvailable(this)) { // 判断网络连接情况
			handler.sendEmptyMessage(0);
			return;
		}
		try {
			// 获取新闻列表，存到list里边
			String pass = EncryptBySHA1.Encrypt(DateUtil.getCurrentDate());
			URL url = new URL(DataUrlKeys.NEWS_LIST_URL + pass);
			Log.i("NewsListUrl", DataUrlKeys.NEWS_LIST_URL + pass);
			URLConnection con = url.openConnection();
			con.connect();
			InputStream input = con.getInputStream();
			list = xmlHandler.getNewsItems(input);
			// for (XmlItemNews xin : list) {
			// Log.e("content","id:" + xin.getId());
			// Log.e("content","title:" + xin.getTitle());
			// Log.e("content","link:" + xin.getLink());
			// Log.e("content","description:" + xin.getDescription());
			// Log.e("content","copyright:" + xin.getCopyright());
			// Log.e("content","pubDate:" + xin.getPubDate());
			// Log.e("content","comment:" + xin.getComment());
			// }
			if (list.size() == 0) {
				handler.sendEmptyMessage(-1);
			} else {
				handler.sendEmptyMessage(1);
			}

			// 获取广告列表，再发一次http连接请求
			url = new URL(DataUrlKeys.AD_NEWS);
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

	/**
	 * 获取更多新闻的线程
	 * 
	 * @return
	 */
	private void getMoreDataThread() {
		Thread t = new Thread() {
			@Override
			public void run() {
				String lastId = list.get(list.size() - 1).getId();
				Log.i("listlastId", lastId);
				try {
					URL url = new URL(DataUrlKeys.MORE_NEWS_LIST_URL.replace(
							"$since_id", lastId));
					getMoreNewsList(url);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}

			}
		};
		t.start();
	}

	/**
	 * get more search data thread
	 * 
	 */
	private void getSearchMoreThread() {
		Thread thread = new Thread() {
			@Override
			public void run() {

				String lastId = list.get(list.size() - 1).getId();
				Log.i("listlastId", lastId);
				try {
					URL url = new URL(DataUrlKeys.SEARCH_MORE_NEWS_URL.replace(
							"$since_id", lastId).replace("$searchContent",
							TabScienceNetActivity.searchString));
					getMoreNewsList(url);
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		};
		thread.start();
	}

	/**
	 * 通过http请求获得更多的新闻
	 */
	private void getMoreNewsList(URL url) {
		if (!NetWorkState.isNetworkAvailable(this)) { // 判断网络连接情况
			handler.sendEmptyMessage(0);
			return;
		}
		try {
			// 获取新闻列表，存到临时的list里边
			// String lastId = list.get(list.size() - 1).getId();
			// Log.i("listlastId", lastId);
			// URL url = new URL(DataUrlKeys.MORE_NEWS_LIST_URL.replace(
			// "$since_id", lastId));
			URLConnection con = url.openConnection();
			con.connect();
			InputStream input = con.getInputStream();
			lastList = xmlHandler.getNewsItems(input);

			if (lastList.size() == 0) {
				handler.sendEmptyMessage(-1);
			} else {
				for (XmlItemNews xin : lastList) {
					list.add(xin);
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

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 1) {
				Log.i(TAG + " handler", "--handler msg received--");
				adapter = new NewsAdapter();
				listView.setAdapter(adapter);
				if (listView.getFooterViewsCount() == 0) {
					listView.addFooterView(moreView);
				}
				viewSwitcher.setDisplayedChild(0);
				listView.onRefreshComplete();
			} else if (msg.what == -1) {
				Toast.makeText(ScienceNetNewsActivity.this, "服务出现异常，请稍后使用",
						Toast.LENGTH_SHORT).show();
			} else if (msg.what == 0) {
				Toast.makeText(ScienceNetNewsActivity.this,
						"网络连接不可用，请检查你的网络连接", Toast.LENGTH_LONG).show();
			} else if (msg.what == 2) {
				adapter.notifyDataSetChanged();
				pg.setVisibility(View.GONE);
				bt.setVisibility(View.VISIBLE);
			} else if (msg.what == 3) { // 进行搜索，将显示列表的区域显示为旋转的进度条
				viewSwitcher.setDisplayedChild(1);
			} else if (msg.what == 4) {
				Toast.makeText(ScienceNetNewsActivity.this, "无搜索结果",
						Toast.LENGTH_LONG).show();
				refreshPage();
			} else if (msg.what == 5) { // 广告请求完毕，可以刷新
				if (adapter != null) {
					adapter.notifyDataSetChanged();
					listView.onRefreshComplete();
				}
			} else if (msg.what == 6) { // 搜索结束，有搜索结果
				adapter = new NewsAdapter();
				listView.setAdapter(adapter);
				viewSwitcher.setDisplayedChild(0);
				listView.onRefreshComplete();
//				listView.removeFooterView(moreView);
			}
		}
	};

	/**
	 * 底部加载更多按钮的监听器
	 */
	private OnClickListener bt_listener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			pg.setVisibility(View.VISIBLE);
			bt.setVisibility(View.GONE);

			if (TabScienceNetActivity.isSearch)
				getSearchMoreThread();
			else
				getMoreDataThread();
		}
	};

	/**
	 * ListView的每个item的点击事件
	 */
	private OnItemClickListener listener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (position == 0) {
				return;
			} else if (position == 1 && adList != null && adList.size() != 0) { // 打开广告链接
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(adList
						.get(0).getLink())));
			} else {
				redirectDetailActivity(position - 2); // 打开显示具体的新闻的页面
			}
		}
	};

	/**
	 * 打开显示具体的新闻内容的activity
	 * 
	 * @param listIndex
	 *            触发点击事件的那条新闻在新闻list里面的索引
	 */
	private void redirectDetailActivity(int listIndex) {
		Intent intent = new Intent();
		try {
			intent.setClass(ScienceNetNewsActivity.this,
					NewsDetailActivity.class);
			Bundle bundle = new Bundle();
			// 将要传递给显示具体新闻页面的数据放入Bundle
			String tmpId = list.get(listIndex).getId(); // 触发点击事件的那条新闻的ID
			ArrayList<String> tmpNewsIdList = new ArrayList<String>(); // 当前的新闻的ID列表
			ArrayList<String> tmpNewsDescriptionList = new ArrayList<String>();
			for (XmlItemNews item : list) {
				tmpNewsIdList.add(item.getId());
				tmpNewsDescriptionList.add(item.getDescription());
			}
			int tmpIndex = listIndex; // 触发点击事件的那条新闻所在新闻list里面的索引
			String tmpDescription = list.get(listIndex).getDescription(); // 触发点击事件的那条新闻的描述

			bundle.putString("current_news_id", tmpId);
			bundle.putStringArrayList("news_id_list", tmpNewsIdList);
			bundle.putStringArrayList("news_description_list",
					tmpNewsDescriptionList);
			bundle.putInt("current_news_index", tmpIndex);
			bundle.putString("current_news_description", tmpDescription);

			intent.putExtras(bundle);

			startActivityForResult(intent, 0);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * ListView的Adapter
	 * 
	 * @author liushuai
	 * 
	 */
	public class NewsAdapter extends BaseAdapter {

		Context mContext = ScienceNetNewsActivity.this;
		LayoutInflater inflater = LayoutInflater.from(mContext);
		AsyncImageLoader asyncImageLoader = new AsyncImageLoader(
				AppUtil.ITEM_IMG_WIDTH, AppUtil.ITEM_IMG_HEIGHT);
		final int VIEW_TYPE = 3;
		final int TYPE_AD = 2;
		final int TYPE_WITHOUT_IMG = 0;
		final int TYPE_WITH_IMG = 1;

		@Override
		public int getCount() {
			return list.size();
		}

		// 每个convertview都会调用此方法，获得当前所需要的view样式
		@Override
		public int getItemViewType(int position) {
			if (position == 0) { // 广告条
				return TYPE_AD;
			} else if ("http://news.sciencenet.cn".equals(list
					.get(position - 1) // 没有图片的新闻
					.getImgs())) {
				return TYPE_WITHOUT_IMG;
			} else {
				return TYPE_WITH_IMG;
			}
		}

		@Override
		public int getViewTypeCount() {
			return VIEW_TYPE;
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
			NewsViewHolder holder = null;
			NewsViewsHolderWithImg holderWithImg = null;
			AdBarHolder holderAd = null;

			int type = getItemViewType(position);

			if (convertView == null) {
				switch (type) {
				case TYPE_WITHOUT_IMG:
					holder = new NewsViewHolder();
					convertView = getLayoutInflater().inflate(
							R.layout.news_list_item, null);
					holder.tv_title = (TextView) convertView
							.findViewById(R.id.news_item_title);
					holder.tv_description = (TextView) convertView
							.findViewById(R.id.news_item_description);
					convertView.setTag(holder);
					break;
				case TYPE_WITH_IMG:
					holderWithImg = new NewsViewsHolderWithImg();
					convertView = getLayoutInflater().inflate(
							R.layout.news_list_item_with_img, null);
					holderWithImg.tv_title = (TextView) convertView
							.findViewById(R.id.news_item_title);
					holderWithImg.tv_description = (TextView) convertView
							.findViewById(R.id.news_item_description);
					holderWithImg.iv_imgs = (ImageView) convertView
							.findViewById(R.id.news_item_img);
					convertView.setTag(holderWithImg);
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
				case TYPE_WITHOUT_IMG:
					holder = (NewsViewHolder) convertView.getTag();
					break;
				case TYPE_WITH_IMG:
					holderWithImg = (NewsViewsHolderWithImg) convertView
							.getTag();
					break;
				case TYPE_AD:
					holderAd = (AdBarHolder) convertView.getTag();
					break;
				default:
					break;
				}
			}
			switch (type) {
			case TYPE_WITHOUT_IMG:
				holder.tv_title.setText(list.get(position - 1).getTitle());
				holder.tv_description.setText(list.get(position - 1)
						.getDescription());
				break;
			case TYPE_WITH_IMG:
				holderWithImg.tv_title.setText(list.get(position - 1)
						.getTitle());
				holderWithImg.tv_description.setText(list.get(position - 1)
						.getDescription());
				// 异步加载图片
				holderWithImg.iv_imgs.setTag(list.get(position - 1).getImgs());
				Drawable cachedImage = asyncImageLoader.loadDrawable(
						list.get(position - 1).getImgs(), new ImageCallback() {
							@Override
							public void imageLoaded(Drawable imageDrawable,
									String imageUrl) {
								ImageView imageViewByTag = (ImageView) listView
										.findViewWithTag(imageUrl);
								if (imageViewByTag != null
										&& imageDrawable != null) {
									imageViewByTag
											.setImageDrawable(imageDrawable);
									// Log.e("在回调里面设置好图片", "liushuai");
								} else {
									try {
										imageViewByTag
												.setImageResource(R.drawable.sync);
										// Log.e("在回调里面设置了默认的图片", "liushuai");
									} catch (Exception e) {
									}
								}
							}
						});
				holderWithImg.iv_imgs.setImageResource(R.drawable.sync);
				if (cachedImage != null) {
					// Log.e("没在回调里设置好图片", "liushuai");
					holderWithImg.iv_imgs.setImageDrawable(cachedImage);
				}
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
						ImageView imageViewByTag = (ImageView) listView
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
			// Log.e("getView", "" + asyncImageLoader.imageCache.size());
			return convertView;
		}

		public class NewsViewHolder {
			TextView tv_title;
			TextView tv_description;
		}

		public class NewsViewsHolderWithImg {
			TextView tv_title;
			TextView tv_description;
			ImageView iv_imgs;
		}

		public class AdBarHolder {
			ImageView iv_ad;
		}
	}
}
