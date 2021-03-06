package cn.sciencenet.activity;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import cn.sciencenet.R;
import cn.sciencenet.httpclient.XmlItemNewspaper;
import cn.sciencenet.httpclient.XmlNewspaperHandler;
import cn.sciencenet.util.AppUtil;
import cn.sciencenet.util.DataUrlKeys;
import cn.sciencenet.util.DateUtil;
import cn.sciencenet.util.EncryptBySHA1;
import cn.sciencenet.util.NetWorkState;
import edu.mit.mobile.android.imagecache.ImageCache;
import edu.mit.mobile.android.imagecache.ImageLoaderAdapter;
import edu.mit.mobile.android.imagecache.SimpleThumbnailAdapter;

public class TabScienceNewspaperActivity extends Activity implements
		OnItemClickListener {

	private TextView tv;
	private EditText time;

	private int mYear;
	private int mMonth;
	private int mDay;

	public static int ScreenWidth;
	public static int ScreenHeight;

	private DisplayMetrics dm;
	private Gallery images_ga;

	private int num = 0;  //第num版科学报

	private List<XmlItemNewspaper> list;
	private XmlNewspaperHandler xmlHandler = new XmlNewspaperHandler();;

	private String[] ids;
	private String[] titles;
	private String selectedTime;
	private String setTime;
	private static String currentTime;

	List<String> urls = new ArrayList<String>();// 所有图片地址
	List<String> url = new ArrayList<String>();// 需要下载图片地址

	private ImageCache mCache;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.layout_newspaper);

		Calendar c = Calendar.getInstance();
		mYear = c.get(Calendar.YEAR);
		mMonth = c.get(Calendar.MONTH);
		mDay = c.get(Calendar.DAY_OF_MONTH);

		dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		ScreenWidth = dm.widthPixels;
		ScreenHeight = dm.heightPixels;

		// 加上这两句可以防止出现netOnMainThread异常，但是是极不稳定的
		// StrictMode.ThreadPolicy policy = new
		// StrictMode.ThreadPolicy.Builder().permitAll().build();
		// StrictMode.setThreadPolicy(policy);

		initView();
		initData();
		getUpdateThreadInstace().start();
	}

	private void initView() {
		time = (EditText) findViewById(R.id.et);
		images_ga = (Gallery) findViewById(R.id.gallery);
		tv = (TextView) findViewById(R.id.tv);
		
		setTime = String.valueOf(mYear) + "年" + (mMonth + 1) + "月" + mDay + "日";
		selectedTime = String.valueOf(mYear) + "-" + (mMonth + 1) + "-" + mDay;
		currentTime = selectedTime;
		time.setText(setTime);
		time.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new DatePickerDialog(TabScienceNewspaperActivity.this,
						mDateSetListener, mYear, mMonth, mDay).show();
			}
		});
		time.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus == true) {
					hideIM(v);
					new DatePickerDialog(TabScienceNewspaperActivity.this,
							mDateSetListener, mYear, mMonth, mDay).show();
				}
			}
		});
	}
	
	private void initData() {
		mCache = ImageCache.getInstance(TabScienceNewspaperActivity.this);
	}
	
	private HashMap<String, String> addItem(String title, String image) {
		final HashMap<String, String> m = new HashMap<String, String>();

		m.put("title", title);
		m.put("thumb", image);

		return m;
	}

	public void refresh() {
		final ListAdapter smallAdapter = new SimpleThumbnailAdapter(
				this, data, R.layout.small_thumbnail_item,
				new String[] { "thumb" }, new int[] { R.id.thumb },
				new int[] { R.id.thumb });
		
		images_ga.setAdapter(new ImageLoaderAdapter(this, smallAdapter,
				mCache, new int[] { R.id.thumb }, AppUtil.GALLERY_WIDTH, AppUtil.GALLERY_HEIGHT,
				ImageLoaderAdapter.UNIT_PX));
		
		images_ga.setOnItemClickListener(this);
		images_ga.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				num = arg2;
				tv.setText(titles[num]);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		redirectActivity(position);
	}
	
	List<HashMap<String, String>> data;
	
	/**
	 * 获得更新报纸的线程的实例
	 * 
	 * @return
	 * @author liushuai
	 */
	private Thread getUpdateThreadInstace() {
		return new Thread() {
			@Override
			public void run() {
				getNewspaperList();
				data = new ArrayList<HashMap<String, String>>();
				for (int i = 0; i < urls.size(); i++) {
					data.add(addItem("" + i, urls.get(i).replace("sm", "")));
				}
				mHandler.sendEmptyMessage(4);
			}
		};
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			try {
				switch (msg.what) {
				case -1: { // 无该日的科学报
					Toast.makeText(TabScienceNewspaperActivity.this,
							"无该日的科学报，请选择其它日期", Toast.LENGTH_LONG).show();
					tv.setText("");
					break;
				}
				case 2: {
					Toast.makeText(TabScienceNewspaperActivity.this,
							"服务出现异常，请稍后使用", Toast.LENGTH_LONG).show();
					break;
				}
				case 3: {
					Toast.makeText(TabScienceNewspaperActivity.this,
							"网络连接不可用，请检查你的网络连接", Toast.LENGTH_LONG).show();
					break;
				}
				case 4: { // 更新UI
				 refresh();
					break;
				}
				}
				super.handleMessage(msg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			System.exit(0);
		}
		return super.onKeyDown(keyCode, event);
	}

	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			mYear = year;
			mMonth = monthOfYear;
			mDay = dayOfMonth;

			selectedTime = String.valueOf(mYear) + "-" + (mMonth + 1) + "-"
					+ mDay;
			setTime = String.valueOf(mYear) + "年" + (mMonth + 1) + "月" + mDay
					+ "日";
			time.setText(setTime);

			if (!DateUtil.isDateBefore(selectedTime, currentTime)) {
				urls = new ArrayList<String>();
				getUpdateThreadInstace().start();
			} else {
				Toast.makeText(TabScienceNewspaperActivity.this,
						"请选择历史日期，不要选择晚于今天的日期", Toast.LENGTH_LONG).show();
			}
		}
	};

	protected void hideIM(View edt) {
		try {
			InputMethodManager im = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
			IBinder windowToken = edt.getWindowToken();
			if (windowToken != null) {
				im.hideSoftInputFromInputMethod(windowToken, 0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void getNewspaperList() {
		if (!NetWorkState.isNetworkAvailable(this)) {
			mHandler.sendEmptyMessage(3);
			return;
		}
		try {
			String pass = EncryptBySHA1.Encrypt(DateUtil.getCurrentDate());
			String myUrl = (DataUrlKeys.NEWSPAPER_LIST_URL).replace(
					"$mydatetime", selectedTime) + pass;
			URL url = new URL(myUrl);
			URLConnection con = url.openConnection();
			con.connect();
			int state = ((HttpURLConnection) con).getResponseCode();
			if (state == 404) {
				mHandler.sendEmptyMessage(-1);
			} else {
				InputStream input = con.getInputStream();
				list = xmlHandler.getNewspaperItems(input);
				if (list != null) {
					String tmp;
					int i = 0;
					ids = new String[16];
					titles = new String[16];
					for (XmlItemNewspaper xin : list) {
						if (xin == list.get(0)) {
							tmp = xin.getLogo().replace("sm", "");
						} else {
							tmp = xin.getLogo();
						}
						urls.add(tmp);
						ids[i] = xin.getId();
						titles[i] = xin.getTitle();
						i++;
					}
				}
				if (list.size() == 0) {
					mHandler.sendEmptyMessage(2);
				}
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
	 * 当用户选择不同的日日期时，刷新新的报纸
	 */
	public void redirectActivity(int index) {
		Intent intent = new Intent();
		try {
			intent.setClass(TabScienceNewspaperActivity.this,
					NewspaperDetailActivity.class);
			Bundle bundle = new Bundle();
			String tmpId = ids[index];

			Log.e("putid", ids[index]);
			bundle.putString("newspaper_id", tmpId);
			intent.putExtras(bundle);
			startActivityForResult(intent, 0);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
