package cn.sciencenet.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import cn.sciencenet.R;
import cn.sciencenet.dialog.LoginDialog;
import cn.sciencenet.util.AppUtil;
import cn.sciencenet.util.DataUrlKeys;
import cn.sciencenet.util.FileAccess;

public class ConfigurationActivity extends Activity {

	private Button goBackButton;
	private ListView listView;

	public static final String CHANGE_ITEM_TEXT = "cn.sciencenet.checkloginstate";

	private List<String> list;
	private List<String> info_list;

	private ConfigurationAdapter adapter;

	myReceiver receiver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.layout_configruation);

		initViews();

		// 注册是否登录成功的监听器
		IntentFilter filter = new IntentFilter(CHANGE_ITEM_TEXT);
		receiver = new myReceiver();
		registerReceiver(receiver, filter);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		info_list.set(2, "当前字号：" + AppUtil.getCurrentFontString());
		refreshAdapter();
	}
	
	private class myReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			changeList();
		}
	}

	private void changeList() {
		if (DataUrlKeys.uid > 0) {
			info_list.set(0, "已登录,点击退出");
			refreshAdapter();
		}
	}

	public void initViews() {
		listView = (ListView) findViewById(R.id.configuration_list);
		listView.setDivider(getResources().getDrawable(
				R.drawable.list_divider_line));
		listView.setDividerHeight(3);
		listView.setCacheColorHint(Color.argb(0, 0, 0, 0));

		list = new ArrayList<String>();
		list.add("登录");
		list.add("清除缓存");
		list.add("字号设置");
		list.add("关于");
		list.add("");

		info_list = new ArrayList<String>();
		if (DataUrlKeys.isLogined) {
			info_list.add("已登录,点击退出");
		} else {
			info_list.add("未登录");
		}
		info_list.add("当前缓存为" + calcCache());
		info_list.add("当前字号：" + AppUtil.getCurrentFontString());
		info_list.add("关于");
		info_list.add("");

		goBackButton = (Button) findViewById(R.id.go_back_button);
		goBackButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				ConfigurationActivity.this.finish();
			}
		});

		refreshAdapter();
	}

	/**
	 * 计算所有的缓存图片的大小
	 * 
	 * @return
	 */
	private String calcCache() {
		long sum = FileAccess
				.getFolderSize(DataUrlKeys.NEWS_LIST_IMG_CACHE_FOLDER.substring(
						0, DataUrlKeys.NEWS_LIST_IMG_CACHE_FOLDER.length() - 1));
		sum += FileAccess
				.getFolderSize(DataUrlKeys.NEWSPAPER_LIST_IMG_CACHE_FOLDER
						.substring(0,
								DataUrlKeys.NEWSPAPER_LIST_IMG_CACHE_FOLDER
										.length() - 1));
		sum += FileAccess
				.getFolderSize(DataUrlKeys.SCIENCENEWSPAPER_CACHE_FOLDER
						.substring(0, DataUrlKeys.SCIENCENEWSPAPER_CACHE_FOLDER
								.length() - 1));
		return FileAccess.FormetFileSize(sum);
	}

	/**
	 * List的item的click监听器
	 */
	private OnItemClickListener listener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Log.i("click", "click" + position);
			if (position == 0) { // 登录登出
				DataUrlKeys.isComment = false;
				if (DataUrlKeys.isLogined == false) {
					new LoginDialog(ConfigurationActivity.this).setDisplay();
					Log.i("config_id", "login_id" + DataUrlKeys.uid);
				} else {
					DataUrlKeys.isLogined = false;
					DataUrlKeys.uid = 0;
					Toast.makeText(ConfigurationActivity.this, "成功退出登录",
							Toast.LENGTH_SHORT).show();
					info_list.set(0, "未登录");
					refreshAdapter();
				}
			} else if (position == 1) { // 清理缓存
				new AlertDialog.Builder(ConfigurationActivity.this)
						.setTitle("清理缓存")
						.setMessage("确定要清理缓存吗？")
						.setPositiveButton("确定",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										new removeCacheTask().execute("begin");
									}
								})
						.setNegativeButton("取消",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
									}
								}).create().show();
			} else if (position == 2) { // 改变字号
				int toSetFlag = (++DataUrlKeys.currentFontSizeFlag) % 3;
				switch (toSetFlag) {
				case 0:
					info_list.set(2, "当前字号：小");
					refreshAdapter();
					break;
				case 1:
					info_list.set(2, "当前字号：中");
					refreshAdapter();
					break;
				case 2:
					info_list.set(2, "当前字号：大");
					refreshAdapter();
					break;
				default:
					break;
				}
				DataUrlKeys.currentFontSizeFlag = toSetFlag;
			} else if (position == 3) { // 关于
				Builder builder = new AlertDialog.Builder(
						ConfigurationActivity.this)
						.setTitle("关于")
						.setIcon(R.drawable.ic_launcher)
						.setMessage(
								"　　科学网是由中国科学院、中国工程院和国家自然科学基金委员会主管，中国科学报社主办的综合性科学网站。"
										+ "作为全球最大的中文科学社区，科学网致力于全方位服务华人科学与高等教育界，以网络社区为基础构"
										+ "建起面向全球华人科学家的网络新媒体，促进科技创新和学术交流。")
						.setPositiveButton("确定",
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.cancel();
									}
								});
				builder.create().show();
			}

		}
	};

	/**
	 * 清理缓存的异步任务
	 * 
	 * @author liushuai
	 */
	private class removeCacheTask extends AsyncTask<String, Integer, String> {

		ProgressDialog dlg;

		@Override
		protected void onPreExecute() {
			dlg = new ProgressDialog(ConfigurationActivity.this);
			dlg.setTitle("清理缓存");
			dlg.setMessage("正在清理，请稍后...");
			dlg.setCancelable(false);
			dlg.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dlg.setButton("取消", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dlg.dismiss();
					cancel(true);

				}
			});
			dlg.show();
		}

		@Override
		protected String doInBackground(String... params) {
			FileAccess
					.deleteAllFiles(DataUrlKeys.NEWS_LIST_IMG_CACHE_FOLDER
							.substring(0,
									DataUrlKeys.NEWS_LIST_IMG_CACHE_FOLDER
											.length() - 1));
			FileAccess
					.deleteAllFiles(DataUrlKeys.NEWSPAPER_LIST_IMG_CACHE_FOLDER
							.substring(0,
									DataUrlKeys.NEWSPAPER_LIST_IMG_CACHE_FOLDER
											.length() - 1));
			FileAccess
					.deleteAllFiles(DataUrlKeys.SCIENCENEWSPAPER_CACHE_FOLDER
							.substring(0,
									DataUrlKeys.SCIENCENEWSPAPER_CACHE_FOLDER
											.length() - 1));
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			dlg.dismiss();
			showMsg("缓存清理完毕！");
			info_list.set(1, "当前缓存为0M");
			refreshAdapter();
		}

		@Override
		protected void onCancelled() {
			String tmp = calcCache();
			showMsg("已取消清理缓存，当前缓存大小：" + tmp);
			info_list.set(1, "当前缓存为" + tmp);
			refreshAdapter();
		}

		/**
		 * 显示对话框
		 * 
		 * @param message
		 */
		private void showMsg(String message) {
			new AlertDialog.Builder(ConfigurationActivity.this)
					.setTitle("消息")
					.setMessage(message)
					.setNegativeButton("关闭",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.dismiss();
								}
							}).show();
		}
	}

	/**
	 * 刷新列表
	 */
	private void refreshAdapter() {
		adapter = new ConfigurationAdapter();
		listView.setOnItemClickListener(listener);
		listView.setAdapter(adapter);
	}

	public class ConfigurationAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return list.size();
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
			ConfigurationViewHolder holder = new ConfigurationViewHolder();
			if (convertView == null) {
				convertView = getLayoutInflater().inflate(
						R.layout.configuration_item, null);
				holder.tv_title = (TextView) convertView
						.findViewById(R.id.configuration_item_title);
				holder.tv_description = (TextView) convertView
						.findViewById(R.id.configuration_item_description);
				convertView.setTag(holder);
			} else {
				holder = (ConfigurationViewHolder) convertView.getTag();
			}

			holder.tv_title.setText(list.get(position).toString());
			holder.tv_description.setText(info_list.get(position).toString());
			return convertView;
		}

		public class ConfigurationViewHolder {
			TextView tv_title;
			TextView tv_description;
		}
	}
}
