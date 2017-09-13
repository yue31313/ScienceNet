package cn.sciencenet.activity;

import android.app.ActivityGroup;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;
import cn.sciencenet.R;
import cn.sciencenet.util.AppUtil;
import cn.sciencenet.util.MoveBg;

public class TabScienceNetActivity extends ActivityGroup {

	private RelativeLayout layout;
	private RelativeLayout layout_news_mainLayout;
	private LayoutInflater inflater;
	private Intent intent;
	private View Page;

	private TextView tv_front;

	private TextView tv_bar_news;
	private TextView tv_bar_blog;
	private TextView tv_bar_group;
	private ImageView iv_bar_refresh;
	private ImageView iv_bar_config;
	private ImageView btn_search;
	private ImageView btn_cancle;
	private EditText et_search_bar;

	private int avg_width = 0;

	public static String searchString = "北京";
	public static boolean isSearch = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 去掉顶部灰条
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.layout_science_net);

		initView();
	}

	private void initView() {
		layout = (RelativeLayout) findViewById(R.id.layout_title_bar);
		layout_news_mainLayout = (RelativeLayout) findViewById(R.id.layout_science_net_main);
		inflater = getLayoutInflater();

		tv_bar_news = (TextView) findViewById(R.id.tv_title_bar_news);
		tv_bar_blog = (TextView) findViewById(R.id.tv_title_bar_blog);
		tv_bar_group = (TextView) findViewById(R.id.tv_title_bar_group);
		iv_bar_refresh = (ImageView) findViewById(R.id.iv_title_bar_refresh);
		iv_bar_config = (ImageView) findViewById(R.id.iv_title_bar_config);
		btn_search = (ImageView) findViewById(R.id.search_btn);
		btn_cancle = (ImageView) findViewById(R.id.cancel_btn);
		et_search_bar = (EditText) findViewById(R.id.search_bar);

		// 设置View的监听器
		tv_bar_news.setOnClickListener(onClickListener);
		tv_bar_blog.setOnClickListener(onClickListener);
		tv_bar_group.setOnClickListener(onClickListener);
		iv_bar_refresh.setOnClickListener(onClickListener);
		iv_bar_config.setOnClickListener(onClickListener);
		btn_search.setOnClickListener(onClickListener);
		btn_cancle.setOnClickListener(onClickListener);

		tv_front = new TextView(this);
		tv_front.setBackgroundResource(R.drawable.slidebar);
		tv_front.setTextColor(Color.WHITE);
		tv_front.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		tv_front.setText("新闻");
		tv_front.setGravity(Gravity.CENTER);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
		layout.addView(tv_front, params);

		android.view.ViewGroup.LayoutParams tmp_params = new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

		intent = new Intent(TabScienceNetActivity.this,
				ScienceNetNewsActivity.class);
		Page = getLocalActivityManager().startActivity("activity_1", intent)
				.getDecorView();
		layout_news_mainLayout.addView(Page, tmp_params);
	}

	@Override
	protected void onPause() {
		AppUtil.hideIM(TabScienceNetActivity.this, et_search_bar); // 在Activity停止时隐藏
		super.onPause();
	}

	private OnClickListener onClickListener = new OnClickListener() {
		int startX;
		LayoutParams params = new LayoutParams(LayoutParams.FILL_PARENT,
				android.view.ViewGroup.LayoutParams.FILL_PARENT);

		@Override
		public void onClick(View v) {
			avg_width = findViewById(R.id.layout).getWidth();
			switch (v.getId()) {
			case R.id.tv_title_bar_news:
				AppUtil.hideIM(TabScienceNetActivity.this, et_search_bar);
				MoveBg.moveFrontBg(tv_front, startX, 0, 0, 0);
				startX = 0;
				tv_front.setText("新闻");
				// TODO:在此处加载新闻的页面
				Page = inflater.inflate(R.layout.layout_news, null);
				intent.setClass(TabScienceNetActivity.this,
						ScienceNetNewsActivity.class);
				Page = getLocalActivityManager().startActivity("activity_1",
						intent).getDecorView();
				break;
			case R.id.tv_title_bar_blog:
				AppUtil.hideIM(TabScienceNetActivity.this, et_search_bar);
				MoveBg.moveFrontBg(tv_front, startX, avg_width, 0, 0);
				startX = avg_width;
				tv_front.setText("博客");
				// TODO:在此处加载博客的页面
				intent.setClass(TabScienceNetActivity.this,
						ScienceNetBlogActivity.class);
				Page = getLocalActivityManager().startActivity("activity_2",
						intent).getDecorView();
				break;
			case R.id.tv_title_bar_group:
				AppUtil.hideIM(TabScienceNetActivity.this, et_search_bar);
				MoveBg.moveFrontBg(tv_front, startX, avg_width * 2, 0, 0);
				startX = avg_width * 2;
				tv_front.setText("群组");
				// TODO:在此处加载群组的页面
				intent.setClass(TabScienceNetActivity.this,
						ScienceNetGroupActivity.class);
				Page = getLocalActivityManager().startActivity("activity_3",
						intent).getDecorView();
				break;
			case R.id.iv_title_bar_refresh:
				isSearch = false;
				AppUtil.hideIM(TabScienceNetActivity.this, et_search_bar);
				if ("activity_1".equals(getLocalActivityManager()
						.getCurrentId())) {
					TabScienceNetActivity.this.sendBroadcast(new Intent(
							ScienceNetNewsActivity.REFRESH_NOW));
					Toast.makeText(TabScienceNetActivity.this, "正在刷新新闻",
							Toast.LENGTH_SHORT).show();
				} else if ("activity_2".equals(getLocalActivityManager()
						.getCurrentId())) {
					TabScienceNetActivity.this.sendBroadcast(new Intent(
							ScienceNetBlogActivity.REFRESH_NOW));
					Toast.makeText(TabScienceNetActivity.this, "正在刷新博客",
							Toast.LENGTH_SHORT).show();
				} else if ("activity_3".equals(getLocalActivityManager()
						.getCurrentId())) {
					TabScienceNetActivity.this.sendBroadcast(new Intent(
							ScienceNetGroupActivity.REFRESH_NOW));
					Toast.makeText(TabScienceNetActivity.this, "正在刷新群组",
							Toast.LENGTH_SHORT).show();
				}
				break;
			case R.id.iv_title_bar_config:
				intent.setClass(TabScienceNetActivity.this,
						ConfigurationActivity.class);
				startActivityForResult(intent, 0);
				break;
			case R.id.search_btn:
				// TODO 在这个地方发送搜索广播更新UI列表
				isSearch = true;
				AppUtil.hideIM(TabScienceNetActivity.this, et_search_bar);
				searchString = AppUtil.encode(et_search_bar.getText()
						.toString());
				if ("activity_1".equals(getLocalActivityManager()
						.getCurrentId())) {
					TabScienceNetActivity.this.sendBroadcast(new Intent(
							ScienceNetNewsActivity.DO_SEARCH));
				} else if ("activity_2".equals(getLocalActivityManager()
						.getCurrentId())) {
					TabScienceNetActivity.this.sendBroadcast(new Intent(
							ScienceNetBlogActivity.DO_SEARCH));
				} else if ("activity_3".equals(getLocalActivityManager()
						.getCurrentId())) {
					TabScienceNetActivity.this.sendBroadcast(new Intent(
							ScienceNetGroupActivity.DO_SEARCH));
				}
				break;
			case R.id.cancel_btn: // 返回
				isSearch = false;
				AppUtil.hideIM(TabScienceNetActivity.this, et_search_bar);
				Toast.makeText(TabScienceNetActivity.this, "正在返回",
						Toast.LENGTH_SHORT).show();
				searchString = "";
				et_search_bar.setText(searchString);
				if ("activity_1".equals(getLocalActivityManager()
						.getCurrentId())) {
					TabScienceNetActivity.this.sendBroadcast(new Intent(
							ScienceNetNewsActivity.REFRESH_NOW));
				} else if ("activity_2".equals(getLocalActivityManager()
						.getCurrentId())) {
					TabScienceNetActivity.this.sendBroadcast(new Intent(
							ScienceNetBlogActivity.REFRESH_NOW));
				} else if ("activity_3".equals(getLocalActivityManager()
						.getCurrentId())) {
					TabScienceNetActivity.this.sendBroadcast(new Intent(
							ScienceNetGroupActivity.REFRESH_NOW));
				}
				break;
			default:
				break;
			}
			layout_news_mainLayout.removeAllViews();
			layout_news_mainLayout.addView(Page, params);
		}
	};
}
