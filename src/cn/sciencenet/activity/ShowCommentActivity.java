package cn.sciencenet.activity;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import cn.sciencenet.R;
import cn.sciencenet.dialog.CommentDialog;
import cn.sciencenet.dialog.LoginDialog;
import cn.sciencenet.httpclient.XmlItemNewsComment;
import cn.sciencenet.httpclient.XmlNewsCommentHandler;
import cn.sciencenet.util.DataUrlKeys;
import cn.sciencenet.util.DateUtil;
import cn.sciencenet.util.EncryptBySHA1;
import cn.sciencenet.util.NetWorkState;
import cn.sciencenet.view.MyListView;
import cn.sciencenet.view.MyListView.OnRefreshListener;

public class ShowCommentActivity extends Activity {

	public static final String REFERSH_COMMENT_LIST = "cn.sciencenet.refreshComment";
	
	private String tags;
	private String id;
	private String canComment;

	private MyListView commentList;
	private List<XmlItemNewsComment> list;
	private XmlNewsCommentHandler xmlHandler;

	private CommentAdapter adapter;

	private ViewSwitcher viewSwitcher;

	private Button goBackButton;
	private Button commentButton;
	private TextView tv_title;
	
	myBroadcastReceiver receiver;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.comment_content);

		setTheme(android.R.style.Theme_Translucent_NoTitleBar);

		initData();
		initView();

		xmlHandler = new XmlNewsCommentHandler();
		requestComment();
		
		//注册刷新评论列表的监听器
		IntentFilter filter = new IntentFilter(REFERSH_COMMENT_LIST);
		receiver = new myBroadcastReceiver();
		registerReceiver(receiver, filter);
	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
		unregisterReceiver(receiver);
	}
	
	private class myBroadcastReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			refreshCommentList();
		}
		
	}
	
	private void refreshCommentList(){
		refreshPage();
	}
	
	protected void initView() {
		tv_title = ((TextView) findViewById(R.id.comment_title));
		viewSwitcher = (ViewSwitcher) findViewById(R.id.viewswitcher_comment_content);
		commentList = new MyListView(this);
		commentList.setCacheColorHint(Color.argb(0, 0, 0, 0));
		commentList.setDivider(getResources().getDrawable(
				R.drawable.list_divider_line));
		commentList.setDividerHeight(3);
		commentList.setonRefreshListener(refreshListener);

		viewSwitcher.addView(commentList);
		viewSwitcher.addView(getLayoutInflater().inflate(
				R.layout.layout_progress_page, null));
		viewSwitcher.showNext();

		goBackButton = (Button) findViewById(R.id.go_back_btn);
		goBackButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ShowCommentActivity.this.finish();
			}
		});
		commentButton = (Button) findViewById(R.id.comment);
		commentButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (tags.equals("news_comment")) {
					switch (Integer.parseInt(canComment)) {
					case 0:
						Toast.makeText(ShowCommentActivity.this, "本新闻不允许评论", Toast.LENGTH_SHORT).show();
						return ;
					case 1:
						break;
					default:
						Toast.makeText(ShowCommentActivity.this, "本新闻不允许评论", Toast.LENGTH_SHORT).show();
						return ;
					}
				} else if (tags.equals("newspaper_comment")) {
					switch (Integer.parseInt(canComment)) {
					case 0:
						Toast.makeText(ShowCommentActivity.this, "本新闻不允许评论", Toast.LENGTH_SHORT).show();
						return ;
					case 1:
						break;
					default:
						Toast.makeText(ShowCommentActivity.this, "本新闻不允许评论", Toast.LENGTH_SHORT).show();
						return ;
					}
				}
				if (DataUrlKeys.isLogined == false) {
					new LoginDialog(ShowCommentActivity.this).setDisplay();
					DataUrlKeys.isComment = true;
				} else {
					new CommentDialog(id, ShowCommentActivity.this)
							.setDisplay();
				}
			}
		});
	}
	
	protected void initData() {
		this.tags = getIntent().getStringExtra("tag");
		this.id = getIntent().getStringExtra("id");
		this.canComment = getIntent().getStringExtra("can_reply");
		DataUrlKeys.getId = id;
		DataUrlKeys.type = tags;

		Log.i("get-tag", tags);
		Log.e("get-id", id);
	}

	private OnRefreshListener refreshListener = new OnRefreshListener() {

		@Override
		public void onRefresh() {
			refreshPage();
		}
	};

	private void refreshPage() {
		new AsyncTask<String, Integer, String>() {

			@Override
			protected String doInBackground(String... params) {
				getCommentList();
				return null;
			}

			protected void onPostExecute(String result) {
				if (adapter != null) {
					adapter.notifyDataSetChanged();
					commentList.onRefreshComplete();
				}
			}
		}.execute("begin");
	}

	private void requestComment() {
		Thread thread = new Thread() {
			@Override
			public void run() {
				getCommentList();
			}
		};
		thread.start();
	}

	private void getCommentList() {
		if (!NetWorkState.isNetworkAvailable(this)) {
			handler.sendEmptyMessage(0);
			return;
		}
		try {
			String pass = EncryptBySHA1.Encrypt(DateUtil.getCurrentDate());
			URL url = null;
			if (tags.equals("news_comment")) {
				handler.sendEmptyMessage(2);
				url = new URL(DataUrlKeys.NEWS_COMMENT_URL.replace("$pass",
						pass) + id);
				// Log.e("url", "获取的url "+url);
			} else if (tags.equals("newspaper_comment")) {
				handler.sendEmptyMessage(3);
				url = new URL(
						DataUrlKeys.NEWSPAPER_CONTENT_COMMENT_URL.replace(
								"$pass", pass) + id);
			}
			URLConnection con = url.openConnection();
			con.connect();
			InputStream inputStream = con.getInputStream();
			list = xmlHandler.getNewsComments(inputStream);

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

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case -1:
//				viewSwitcher.removeAllViews();
				Toast.makeText(ShowCommentActivity.this, "该文章无评论",
						Toast.LENGTH_SHORT).show();
				break;
			case 0:
				Toast.makeText(ShowCommentActivity.this, "网络连接不可用，请检查你的网络连接",
						Toast.LENGTH_SHORT).show();
				break;
			case 1:
				adapter = new CommentAdapter();
				commentList.setOnItemClickListener(listener);
				commentList.setAdapter(adapter);
				viewSwitcher.setDisplayedChild(0);
				commentList.onRefreshComplete();
				break;
			case 2:
				tv_title.setText("新闻评论");
				break;
			case 3:
				tv_title.setText("报刊评论");
				break;
			default:
				break;
			}

		}
	};

	private OnItemClickListener listener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (position == 0) {
				return;
			}
		}

	};

	public class CommentAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			CommentViewHolder holder;
			if (convertView == null) {
				holder = new CommentViewHolder();
				convertView = getLayoutInflater().inflate(
						R.layout.comment_list_item, null);
				holder.tv_username = (TextView) convertView
						.findViewById(R.id.comment_username);
				holder.tv_description = (TextView) convertView
						.findViewById(R.id.comment_detail);
				holder.tv_posttimeTextView = (TextView) convertView
						.findViewById(R.id.comment_time);
				convertView.setTag(holder);
			} else {
				holder = (CommentViewHolder) convertView.getTag();
			}

			holder.tv_username.setText(list.get(position).getUsername());
			holder.tv_description.setText(list.get(position).getDescription());
			holder.tv_posttimeTextView
					.setText(list.get(position).getPosttime());

			return convertView;
		}

		public class CommentViewHolder {
			TextView tv_username;
			TextView tv_description;
			TextView tv_posttimeTextView;
		}
	}

}
