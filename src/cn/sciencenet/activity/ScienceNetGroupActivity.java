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
import cn.sciencenet.httpclient.XmlGroupHandler;
import cn.sciencenet.httpclient.XmlItemAd;
import cn.sciencenet.httpclient.XmlItemGroup;
import cn.sciencenet.util.AppUtil;
import cn.sciencenet.util.AsyncImageLoader;
import cn.sciencenet.util.AsyncImageLoader.ImageCallback;
import cn.sciencenet.util.DataUrlKeys;
import cn.sciencenet.util.DateUtil;
import cn.sciencenet.util.EncryptBySHA1;
import cn.sciencenet.util.NetWorkState;
import cn.sciencenet.view.MyListView;
import cn.sciencenet.view.MyListView.OnRefreshListener;

public class ScienceNetGroupActivity extends Activity {

	private static final String TAG = "ScienceNetGroupActivity";
	public static final String REFRESH_NOW = "cn.sciencenet.ScienceNetGroupActivity.RefreshNow";
	public static final String DO_SEARCH = "cn.sciencenet.ScienceNetGroupActivity.DoSearch";

	private MyListView groupList;

	private List<XmlItemGroup> list;
	private List<XmlItemGroup> lastList;
	private List<XmlItemAd> adList;
	private XmlGroupHandler xmlHandler;
	private XmlAdHandler xmlAdHandler;

	private GroupAdapter adapter;

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
		setTheme(android.R.style.Theme_Black_NoTitleBar);

		initViews();

		xmlHandler = new XmlGroupHandler();
		xmlAdHandler = new XmlAdHandler();
		requestGroup();

		// 注册用来刷新的BroadcastReceiver
		IntentFilter filter = new IntentFilter(REFRESH_NOW);
		refreshReceiver = new RefreshReceiver();
		registerReceiver(refreshReceiver, filter);

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
	 * 初始化页面
	 */
	private void initViews() {
		viewSwitcher = (ViewSwitcher) findViewById(R.id.viewswitcher_news);
		groupList = new MyListView(this);
		groupList.setCacheColorHint(Color.argb(0, 0, 0, 0));
		groupList.setDivider(getResources().getDrawable(
				R.drawable.list_divider_line));
		groupList.setDividerHeight(1);
		groupList.setSelector(R.drawable.list_item_selector);
		groupList.setonRefreshListener(refreshListener);
		groupList.setOnItemClickListener(listener);

		viewSwitcher.addView(groupList);
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

	private OnRefreshListener refreshListener = new OnRefreshListener() {

		@Override
		public void onRefresh() {
			TabScienceNetActivity.isSearch = false;
			refreshPage();
		}
	};

	/**
	 * 刷新当前页
	 */
	private void refreshPage() {
		new AsyncTask<String, Integer, String>() {
			@Override
			protected String doInBackground(String... params) {
				getGroupList();
				return null;
			}

			@Override
			protected void onPostExecute(String result) {
				if (adapter != null) {
					adapter.notifyDataSetChanged();
					groupList.onRefreshComplete();
				}
			}
		}.execute("begin");
	}

	/**
	 * 请求群组列表
	 */
	private void requestGroup() {
		Thread t = new Thread() {
			@Override
			public void run() {
				getGroupList();
			}
		};
		t.start();
	}

	/**
	 * 获取更多群组的线程
	 * 
	 * @return
	 */
	private Thread getMoreDataThread() {
		return new Thread() {
			@Override
			public void run() {
				getMoreGroupList();
			}
		};
	}
	
	/**
	 * get search more group thread
	 * @return
	 */
	private Thread getSearchMoreThread() {
		return new Thread() {
			@Override
			public void run() {
				getSearchMoreList();
			}
		};
	}

	/**
	 * 获取更多群组列表
	 */
	private void getMoreGroupList() {
		if (!NetWorkState.isNetworkAvailable(this)) {
			handler.sendEmptyMessage(0);
			return;
		}
		try {
			// 获取新闻列表，存到临时的list里边
			String lastId = list.get(list.size() - 1).getTid();
			URL url = new URL(DataUrlKeys.MORE_GROUP_LIST_URL.replace(
					"$since_id", lastId));
			URLConnection con = url.openConnection();
			con.connect();
			InputStream input = con.getInputStream();
			lastList = xmlHandler.getGroupItems(input);

			if (lastList.size() == 0) {
				handler.sendEmptyMessage(-1);
			} else {
				for (XmlItemGroup xig : lastList) {
					list.add(xig);
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
	 * get search more list
	 */
	private void getSearchMoreList(){
		if (!NetWorkState.isNetworkAvailable(this)) {
			handler.sendEmptyMessage(0);
			return;
		}
		try {
			// 获取新闻列表，存到临时的list里边
			String lastId = list.get(list.size() - 1).getTid();
			URL url = new URL(DataUrlKeys.SEARCH_MORE_GROUP_URL.replace(
					"$since_id", lastId).replace("$searchContent", TabScienceNetActivity.searchString));
			URLConnection con = url.openConnection();
			con.connect();
			InputStream input = con.getInputStream();
			InputStream nInputStream = AppUtil.transferInputStream(input);
			
			lastList = xmlHandler.getGroupItems(nInputStream);

			if (lastList.size() == 0) {
				handler.sendEmptyMessage(-1);
			} else {
				for (XmlItemGroup xig : lastList) {
					list.add(xig);
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
	 * 获取群组列表，同时获取群组的广告
	 */
	private void getGroupList() {
		Log.i(TAG, "get group list!!!");
		if (!NetWorkState.isNetworkAvailable(this)) {
			handler.sendEmptyMessage(0);
			return;
		}
		try {
			String pass = EncryptBySHA1.Encrypt(DateUtil.getCurrentDate());
			URL url = new URL(DataUrlKeys.GROUP_LIST_URL + pass);
			Log.i("NewsGroupUrl", DataUrlKeys.NEWS_LIST_URL + pass);
			URLConnection con = url.openConnection();
			con.connect();
			InputStream input = con.getInputStream();
			list = xmlHandler.getGroupItems(input);

			if (list.size() == 0) {
				handler.sendEmptyMessage(-1);
			} else {
				handler.sendEmptyMessage(1);
			}

			// 获取群组的广告
			url = new URL(DataUrlKeys.AD_GROUP);
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
			// TODO 在这里加上广告不存在的处理
			handler.sendEmptyMessage(7);
			e.printStackTrace();
		}
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

			URL url = new URL(DataUrlKeys.SEARCH_GROUP_URL.replace(
					"$searchContent", searchContent));
			tmptmpURL = url.toString();

			URLConnection con = url.openConnection();
			con.connect();
			InputStream input = con.getInputStream();
			InputStream nInputStream = AppUtil.transferInputStream(input);
			// Log.e("afterString", nInputStream.toString());
			list = xmlHandler.getGroupItems(nInputStream);

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

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 1) {
				Log.i(TAG, "--handler msg received--");
				adapter = new GroupAdapter();
				groupList.setAdapter(adapter);
				if (groupList.getFooterViewsCount() == 0) {
					groupList.addFooterView(moreView);
				}
				viewSwitcher.setDisplayedChild(0);
				groupList.onRefreshComplete();
			} else if (msg.what == -1) {
				Toast.makeText(ScienceNetGroupActivity.this, "服务器出现异常，请稍后使用",
						Toast.LENGTH_SHORT).show();
			} else if (msg.what == 0) {
				Toast.makeText(ScienceNetGroupActivity.this,
						"网络连接不可用，请检查你的网络连接", Toast.LENGTH_SHORT).show();
			} else if (msg.what == 2) {
				adapter.notifyDataSetChanged();
				pg.setVisibility(View.GONE);
				bt.setVisibility(View.VISIBLE);
			} else if (msg.what == 3) {
				viewSwitcher.setDisplayedChild(1);
			} else if (msg.what == 4) {
				Toast.makeText(ScienceNetGroupActivity.this, "无搜索结果",
						Toast.LENGTH_LONG).show();
				refreshPage();
			} else if (msg.what == 5) { // 广告请求完毕，可以刷新
				if (adapter != null) {
					adapter.notifyDataSetChanged();
					groupList.onRefreshComplete();
				}
			} else if (msg.what == 6) {
				adapter = new GroupAdapter();
				groupList.setAdapter(adapter);
				viewSwitcher.setDisplayedChild(0);
				groupList.onRefreshComplete();
//				groupList.removeFooterView(moreView);
			} else if (msg.what == 7) {
//				Log.e("fuck!!!!!", "fuck!!!!!!!!!!!!!!!!!");
//				groupList.removeViewAt(1);
//				adapter.getItem(1);
			}
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
	 * 打开显示具体群组内容的activity
	 * 
	 * @param listIndex
	 *            触发点击事件的那条群组在群组list里面的索引
	 */
	private void redirectDetailActivity(int listIndex) {
		Intent intent = new Intent();
		try {
			intent.setClass(ScienceNetGroupActivity.this,
					GroupDetailActivity.class);
			Bundle bundle = new Bundle();
			String tmpId = list.get(listIndex).getTid(); // current_group_id
			int tmpIndex = listIndex; // current_group_index
			String tmpCurrentGroupDescription = list.get(listIndex)
					.getDescription();
			String tmpCurrentGroupLink = list.get(listIndex).getLink();
			ArrayList<String> tmpGroupIdList = new ArrayList<String>();
			ArrayList<String> tmpGroupDescriptionList = new ArrayList<String>();
			ArrayList<String> tmpGroupLinkList = new ArrayList<String>();
			for (XmlItemGroup xig : list) {
				tmpGroupIdList.add(xig.getTid());
				tmpGroupLinkList.add(xig.getLink());
				tmpGroupDescriptionList.add(xig.getDescription());
			}

			bundle.putString("current_group_id", tmpId);
			bundle.putInt("current_group_index", tmpIndex);
			bundle.putStringArrayList("group_id_list", tmpGroupIdList);
			bundle.putStringArrayList("group_description_list",
					tmpGroupDescriptionList);
			bundle.putString("current_group_description",
					tmpCurrentGroupDescription);
			bundle.putString("current_group_link", tmpCurrentGroupLink);
			bundle.putStringArrayList("group_link_list", tmpGroupLinkList);

			intent.putExtras(bundle);

			startActivityForResult(intent, 0);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public class GroupAdapter extends BaseAdapter {

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
			GroupViewHolder holder = null;
			AdBarHolder holderAd = null;

			int type = getItemViewType(position);

			if (convertView == null) {
				switch (type) {
				case TYPE_NORMAL:
					holder = new GroupViewHolder();
					convertView = getLayoutInflater().inflate(
							R.layout.news_list_item, null);
					holder.tv_title = (TextView) convertView
							.findViewById(R.id.news_item_title);
					holder.tv_description = (TextView) convertView
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
					holder = (GroupViewHolder) convertView.getTag();
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
				holder.tv_description.setText(list.get(position - 1)
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
						ImageView imageViewByTag = (ImageView) groupList
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

		public class GroupViewHolder {
			TextView tv_title;
			TextView tv_description;
		}

		public class AdBarHolder {
			ImageView iv_ad;
		}
	}
}
