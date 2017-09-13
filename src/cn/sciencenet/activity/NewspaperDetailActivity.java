package cn.sciencenet.activity;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import cn.sciencenet.R;
import cn.sciencenet.httpclient.XmlNewspaperDetail;
import cn.sciencenet.httpclient.XmlNewspaperDetailHandler;
import cn.sciencenet.util.AppUtil;
import cn.sciencenet.util.AsyncImageLoader;
import cn.sciencenet.util.AsyncImageLoader.ImageCallback;
import cn.sciencenet.util.DataUrlKeys;
import cn.sciencenet.util.DateUtil;
import cn.sciencenet.util.EncryptBySHA1;
import cn.sciencenet.util.NetWorkState;

public class NewspaperDetailActivity extends Activity {

	private String newspaperId;
	private ListView listView;

	private List<XmlNewspaperDetail> list;
	private XmlNewspaperDetailHandler xmlHandler;

	private NewspaperAdapter adapter;

	private ViewSwitcher viewSwitcher;
	private Button goBackButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 去掉顶部灰条
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.layout_newspaper_list);
		((TextView) findViewById(R.id.newspaper_title)).setText("新闻列表");
		setTheme(android.R.style.Theme_Translucent_NoTitleBar);

		initViews();

		newspaperId = getIntent().getStringExtra("newspaper_id");

		xmlHandler = new XmlNewspaperDetailHandler();
		requestNewspaper();
	}

	private void initViews() {
		viewSwitcher = (ViewSwitcher) findViewById(R.id.viewswitcher_newspaper);
		listView = new ListView(this);
		listView.setDivider(getResources().getDrawable(
				R.drawable.list_divider_line));
		listView.setDividerHeight(1);
		listView.setSelector(R.drawable.list_item_selector);
		listView.setCacheColorHint(Color.argb(0, 0, 0, 0));

		viewSwitcher.addView(listView);
		viewSwitcher.addView(getLayoutInflater().inflate(
				R.layout.layout_progress_page, null));
		viewSwitcher.showNext();

		goBackButton = (Button) findViewById(R.id.go_back);
		goBackButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				NewspaperDetailActivity.this.finish();
			}
		});
	}

	private void requestNewspaper() {
		Thread thread = new Thread() {
			@Override
			public void run() {
				getNewspaperList();
			}
		};
		thread.start();
	}

	public void getNewspaperList() {
		if (!NetWorkState.isNetworkAvailable(this)) {
			handler.sendEmptyMessage(0);
			return;
		}
		try {
			String pass = EncryptBySHA1.Encrypt(DateUtil.getCurrentDate());
			String myUrl = DataUrlKeys.NEWSPAPER_DETAIL_BY_ID_URL.replace(
					"$id", newspaperId).replace("$pass", pass);
			URL url = new URL(myUrl);

			URLConnection connection = url.openConnection();
			connection.connect();
			InputStream inputStream = connection.getInputStream();
			list = xmlHandler.getNewspaperDetails(inputStream);

			if (list.size() == 0) {
				handler.sendEmptyMessage(-1);
			} else {
				handler.sendEmptyMessage(1);
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 1) {
				adapter = new NewspaperAdapter();
				listView.setOnItemClickListener(listener);
				listView.setAdapter(adapter);
				viewSwitcher.setDisplayedChild(0);
			} else if (msg.what == -1) {
				Toast.makeText(NewspaperDetailActivity.this, "服务出现异常，请稍后使用",
						Toast.LENGTH_SHORT).show();
			} else if (msg.what == 0) {
				Toast.makeText(NewspaperDetailActivity.this,
						"网络连接不可用，请检查你的网络连接", Toast.LENGTH_SHORT).show();
			}
		}
	};

	private OnItemClickListener listener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			redirectDetailActivity(position);
		}

	};

	private void redirectDetailActivity(int listIndex) {
		Intent intent = new Intent();
		try {
			intent.setClass(NewspaperDetailActivity.this,
					NewspaperContentActivity.class);
			Bundle bundle = new Bundle();

			String tmpId = list.get(listIndex).getId();
			String tmpComment = list.get(listIndex).getComment();
			String tmpCurrentNewspaperDescription = list.get(listIndex)
					.getDescription();
			ArrayList<String> tmpNewspaperContentIdList = new ArrayList<String>();
			ArrayList<String> tmpNewpaperCommentList = new ArrayList<String>();
			ArrayList<String> tmpNewspaperDescriptionList = new ArrayList<String>();

			for (XmlNewspaperDetail xnd : list) {
				tmpNewspaperContentIdList.add(xnd.getId());
				tmpNewpaperCommentList.add(xnd.getComment());
				tmpNewspaperDescriptionList.add(xnd.getDescription());
			}
			int tmpIndex = listIndex;

			bundle.putString("current_newspaper_list_id", tmpId);
			bundle.putStringArrayList("newspaper_id_list",
					tmpNewspaperContentIdList);
			bundle.putInt("current_newspaper_index", tmpIndex);
			bundle.putString("current_newspaper_comment", tmpComment);
			bundle.putStringArrayList("newspaper_comment_list",
					tmpNewpaperCommentList);
			bundle.putStringArrayList("newspaper_description_list",
					tmpNewspaperDescriptionList);
			bundle.putString("current_newspaper_description",
					tmpCurrentNewspaperDescription);

			intent.putExtras(bundle);

			startActivityForResult(intent, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public class NewspaperAdapter extends BaseAdapter {
		Context mContext = NewspaperDetailActivity.this;
		LayoutInflater inflater = LayoutInflater.from(mContext);
		AsyncImageLoader asyncImageLoader = new AsyncImageLoader(
				DataUrlKeys.NEWSPAPER_LIST_IMG_CACHE_FOLDER,
				AppUtil.ITEM_IMG_WIDTH, AppUtil.ITEM_IMG_HEIGHT);
		final int VIEW_TYPE = 2;
		final int TYPE_WITHOUT_IMG = 0;
		final int TYPE_WITH_IMG = 1;

		@Override
		public int getCount() {
			return list.size();
		}

		// 每个convertview都会调用此方法，获得当前所需要的view样式
		@Override
		public int getItemViewType(int position) {
			if ("http://news.sciencenet.cn".equals(list.get(position) // 没有图片的新闻
					.getImgs())) {
				return TYPE_WITHOUT_IMG;
			} else {
				return TYPE_WITH_IMG;
			}
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public Object getItem(int position) {
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			NewspaperViewHolder holder = null;
			NewspaperViewsHolderWithImg holderWithImg = null;
			int type = getItemViewType(position);

			if (convertView == null) {
				switch (type) {
				case TYPE_WITHOUT_IMG:
					holder = new NewspaperViewHolder();
					convertView = getLayoutInflater().inflate(
							R.layout.news_list_item, null);
					holder.tv_title = (TextView) convertView
							.findViewById(R.id.news_item_title);
					holder.tv_description = (TextView) convertView
							.findViewById(R.id.news_item_description);
					convertView.setTag(holder);
					break;
				case TYPE_WITH_IMG:
					holderWithImg = new NewspaperViewsHolderWithImg();
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
				default:
					break;
				}
			} else {
				switch (type) {
				case TYPE_WITHOUT_IMG:
					holder = (NewspaperViewHolder) convertView.getTag();
					break;
				case TYPE_WITH_IMG:
					holderWithImg = (NewspaperViewsHolderWithImg) convertView
							.getTag();
					break;
				default:
					break;
				}
			}
			switch (type) {
			case TYPE_WITHOUT_IMG:
				holder.tv_title.setText(list.get(position).getTitle());
				holder.tv_description.setText(list.get(position)
						.getDescription());
				break;
			case TYPE_WITH_IMG:
				holderWithImg.tv_title.setText(list.get(position).getTitle());
				holderWithImg.tv_description.setText(list.get(position)
						.getDescription());
				// TODO 在此处异步加载图片
				holderWithImg.iv_imgs.setTag(list.get(position).getImgs());
				Drawable cachedImage = asyncImageLoader.loadDrawable(
						list.get(position).getImgs(), new ImageCallback() {
							@Override
							public void imageLoaded(Drawable imageDrawable,
									String imageUrl) {
								ImageView imageViewByTag = (ImageView) listView
										.findViewWithTag(imageUrl);
								if (imageViewByTag != null
										&& imageDrawable != null) {
									imageViewByTag
											.setImageDrawable(imageDrawable);
								} else {
									try {
										imageViewByTag
												.setImageResource(R.drawable.sync);
									} catch (Exception e) {
									}
								}
							}
						});
				holderWithImg.iv_imgs.setImageResource(R.drawable.sync);
				if (cachedImage != null) {
					holderWithImg.iv_imgs.setImageDrawable(cachedImage);
				}
				break;
			default:
				break;
			}
			System.gc();
			return convertView;
		}

		public class NewspaperViewHolder {
			TextView tv_title;
			TextView tv_description;
		}

		public class NewspaperViewsHolderWithImg {
			TextView tv_title;
			TextView tv_description;
			ImageView iv_imgs;
		}
	}
}
