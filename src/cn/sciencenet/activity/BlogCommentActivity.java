package cn.sciencenet.activity;

import java.io.InputStream;
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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import cn.sciencenet.R;
import cn.sciencenet.dialog.CommentDialog;
import cn.sciencenet.dialog.LoginDialog;
import cn.sciencenet.httpclient.XmlBlogCommentHandler;
import cn.sciencenet.httpclient.XmlItemBlogComment;
import cn.sciencenet.util.DataUrlKeys;
import cn.sciencenet.util.DateUtil;
import cn.sciencenet.util.EncryptBySHA1;
import cn.sciencenet.util.NetWorkState;
import cn.sciencenet.view.MyListView;
import cn.sciencenet.view.MyListView.OnRefreshListener;

public class BlogCommentActivity extends Activity {

	private String tag;
	private String id;
	private String canComment;

	private MyListView commentListView;

	private List<XmlItemBlogComment> list;
	private XmlBlogCommentHandler xmlHandler;

	private CommentAdapter adapter;

	private ViewSwitcher viewSwitcher;
	private Button goBackButton;
	private Button commentButton;
	private TextView tv_title;

	myBroadcastReceiver receiver;

	@Override
	public void onCreate(Bundle savedInstaceState) {
		super.onCreate(savedInstaceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.comment_content);

		setTheme(android.R.style.Theme_Translucent_NoTitleBar);

		initData();
		initView();

		xmlHandler = new XmlBlogCommentHandler();
		requestBlogComment();

		// 注册刷新评论列表的监听器
		IntentFilter filter = new IntentFilter(ShowCommentActivity.REFERSH_COMMENT_LIST);
		receiver = new myBroadcastReceiver();
		registerReceiver(receiver, filter);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
	}

	private class myBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			refreshCommentList();
		}

	}

	private void refreshCommentList(){
		refreshPage();
	}
	
	protected void initView() {
		tv_title = (TextView) findViewById(R.id.comment_title);
		viewSwitcher = (ViewSwitcher) findViewById(R.id.viewswitcher_comment_content);
		commentListView = new MyListView(this);
		commentListView.setCacheColorHint(Color.argb(0, 0, 0, 0));
		commentListView.setDivider(getResources().getDrawable(
				R.drawable.list_divider_line));
		commentListView.setDividerHeight(3);
		commentListView.setonRefreshListener(refreshListener);

		viewSwitcher.addView(commentListView);
		viewSwitcher.addView(getLayoutInflater().inflate(
				R.layout.layout_progress_page, null));
		viewSwitcher.showNext();

		goBackButton = (Button) findViewById(R.id.go_back_btn);
		goBackButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				BlogCommentActivity.this.finish();
			}
		});

		commentButton = (Button) findViewById(R.id.comment);
		commentButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (tag.equals("blog_comment")) {
					switch (Integer.parseInt(canComment)) {
					case 0:
						break;
					case 2:
						break;
					case 1:
						Toast.makeText(BlogCommentActivity.this, "本博客不允许评论",
								Toast.LENGTH_SHORT).show();
						return;
					default:
						Toast.makeText(BlogCommentActivity.this,
								"本客户端不支持您评论，请到网站上发表您的评论", Toast.LENGTH_SHORT)
								.show();
						return;
					}
				} else if (tag.equals("group_comment")) {
//					Toast.makeText(BlogCommentActivity.this,
//							"本客户端不支持您评论，请到网站上发表您的评论", Toast.LENGTH_SHORT)
//							.show();
				}
				// 执行到这里说明本篇文章允许评论
				if (DataUrlKeys.isLogined == false) {
					DataUrlKeys.isComment = true;
					new LoginDialog(BlogCommentActivity.this).setDisplay();
				} else {
					new CommentDialog(id, BlogCommentActivity.this)
							.setDisplay();
				}
			}
		});
	}

	protected void initData() {
		this.tag = getIntent().getStringExtra("tag");
		this.id = getIntent().getStringExtra("id");
		this.canComment = getIntent().getStringExtra("can_comment");
		DataUrlKeys.getId = id;
		DataUrlKeys.type = tag;
		Log.i("getId", id);
		Log.e("getTag", tag);
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
				getBlogCommentList();
				return null;
			}

			protected void onPostExecute(String result) {
				if (adapter != null) {
					adapter.notifyDataSetChanged();
					commentListView.onRefreshComplete();
				}
			}
		}.execute("begin");
	}

	private void requestBlogComment() {
		Thread t = new Thread() {
			@Override
			public void run() {
				getBlogCommentList();
			}
		};
		t.start();
	}

	private void getBlogCommentList() {
		if (!NetWorkState.isNetworkAvailable(this)) {
			handler.sendEmptyMessage(0);
			return;
		}
		try {
			String pass = EncryptBySHA1.Encrypt(DateUtil.getCurrentDate());
			URL url = null;
			if (tag.equals("blog_comment")) {
				handler.sendEmptyMessage(2);
				url = new URL(DataUrlKeys.BLOG_COMMENT_URL.replace("$blogid",
						id) + pass);
				Log.i("urlurl", "获取的url" + url);
			} else if (tag.equals("group_comment")) {
				handler.sendEmptyMessage(3);
				url = new URL(DataUrlKeys.GROUP_COMMENT_URL.replace("$tid", id)
						+ pass);
			}
			URLConnection connection = url.openConnection();
			connection.setReadTimeout(60000);
			connection.connect();
			InputStream inputStream = connection.getInputStream();
			list = xmlHandler.getBlogComments(inputStream);

			if (list.size() == 0) {
				handler.sendEmptyMessage(-1);
			} else {
				handler.sendEmptyMessage(1);
			}

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
				Toast.makeText(BlogCommentActivity.this, "该文章无评论",
						Toast.LENGTH_SHORT).show();
				break;
			case 0:
				Toast.makeText(BlogCommentActivity.this, "网络连接不可用，请检查你的网络连接",
						Toast.LENGTH_SHORT).show();
				break;
			case 1:
				adapter = new CommentAdapter();
				commentListView.setAdapter(adapter);
				viewSwitcher.setDisplayedChild(0);
				commentListView.onRefreshComplete();
				break;
			case 2:
				tv_title.setText("博客评论");
				break;
			case 3:
				tv_title.setText("群组评论");
				break;
			default:
				break;
			}
		}
	};

	private class CommentAdapter extends BaseAdapter {

		final int TYPE_WITHOUT_REPLAY = 0;
		final int TYPE_WITH_REPLAY = 1;

		@Override
		public int getCount() {
			return list.size();
		}

		// 获取当前所需的view
		@Override
		public int getItemViewType(int position) {
			if (list.get(position).getReplayItem() == null) { // 无评论
				return TYPE_WITHOUT_REPLAY;
			} else {
				return TYPE_WITH_REPLAY;
			}
		}

		@Override
		public int getViewTypeCount() {
			return 2;
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
			CommentViewHolder holder = null;
			CommentViewHolderWithReplay holderWithReplay = null;
			int type = getItemViewType(position);

			if (convertView == null) {
				switch (type) {
				case TYPE_WITHOUT_REPLAY:
					holder = new CommentViewHolder();
					convertView = getLayoutInflater().inflate(
							R.layout.comment_list_item, null);
					holder.tv_message = (TextView) convertView
							.findViewById(R.id.comment_detail);
					holder.tv_username = (TextView) convertView
							.findViewById(R.id.comment_username);
					holder.tv_dateline = (TextView) convertView
							.findViewById(R.id.comment_time);

					convertView.setTag(holder);
					break;

				case TYPE_WITH_REPLAY:
					holderWithReplay = new CommentViewHolderWithReplay();
					convertView = getLayoutInflater().inflate(
							R.layout.comment_list_item_with_replay, null);
					holderWithReplay.tv_message = (TextView) convertView
							.findViewById(R.id.comment_detail);
					holderWithReplay.tv_username = (TextView) convertView
							.findViewById(R.id.comment_username);
					holderWithReplay.tv_dateline = (TextView) convertView
							.findViewById(R.id.comment_time);
					holderWithReplay.tv_replay = (TextView) convertView
							.findViewById(R.id.comment_replay);
					holderWithReplay.tv_replayTime = (TextView) convertView
							.findViewById(R.id.replay_time);

					convertView.setTag(holderWithReplay);
					break;
				default:
					break;
				}

			} else {
				switch (type) {
				case TYPE_WITHOUT_REPLAY:
					holder = (CommentViewHolder) convertView.getTag();
					break;
				case TYPE_WITH_REPLAY:
					holderWithReplay = (CommentViewHolderWithReplay) convertView
							.getTag();
					break;
				default:
					break;
				}
			}

			switch (type) {
			case TYPE_WITHOUT_REPLAY:
				holder.tv_username.setText(list.get(position).getMessageItem()
						.getUsername());
				holder.tv_dateline.setText(list.get(position).getMessageItem()
						.getDateline());
				holder.tv_message.setText(list.get(position).getMessageItem()
						.getMessage());
				break;
			case TYPE_WITH_REPLAY:
				holderWithReplay.tv_username.setText(list.get(position)
						.getMessageItem().getUsername());
				holderWithReplay.tv_dateline.setText(list.get(position)
						.getMessageItem().getDateline());
				holderWithReplay.tv_message.setText(list.get(position)
						.getMessageItem().getMessage());
				holderWithReplay.tv_replay.setText(list.get(position)
						.getReplayItem().getMessage());
				holderWithReplay.tv_replayTime.setText(list.get(position)
						.getReplayItem().getDateline());
				break;
			default:
				break;
			}

			return convertView;
		}

		public class CommentViewHolder {
			TextView tv_message;
			TextView tv_username;
			TextView tv_dateline;
		}

		public class CommentViewHolderWithReplay {
			TextView tv_message;
			TextView tv_username;
			TextView tv_dateline;
			TextView tv_replay;
			TextView tv_replayTime;
		}
	}
}
