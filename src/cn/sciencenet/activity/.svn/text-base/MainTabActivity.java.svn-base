package cn.sciencenet.activity;

import cn.sciencenet.R;
import cn.sciencenet.util.AppUtil;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TabHost;

public class MainTabActivity extends TabActivity {
	private TabHost tabHost;
	private RadioGroup radioGroup;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 去掉顶部灰条
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		initTabHost();
		initRadioGroup();
	}

	/**
	 * 按下键盘上返回按钮
	 */
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			AppUtil.QuitHintDialog(MainTabActivity.this);
			return false;
		} else {
			return super.dispatchKeyEvent(event);
		}
	}

	private void initTabHost() {
		tabHost = getTabHost();
		tabHost.addTab(tabHost.newTabSpec("sciencenet")
				.setIndicator("sciencenet")
				.setContent(new Intent(this, TabScienceNetActivity.class)));
		tabHost.addTab(tabHost
				.newTabSpec("sciencenewspaper")
				.setIndicator("sciencenewspaper")
				.setContent(new Intent(this, TabScienceNewspaperActivity.class)));
		tabHost.addTab(tabHost.newTabSpec("collection")
				.setIndicator("collection")
				.setContent(new Intent(this, TabCollectionActivity.class)));
	}

	private void initRadioGroup() {
		radioGroup = (RadioGroup) findViewById(R.id.radiogroup);
		radioGroup.setOnCheckedChangeListener(checkedChangeListener);
	}

	private OnCheckedChangeListener checkedChangeListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			switch (checkedId) {
			case R.id.radio_sciencenet:
				tabHost.setCurrentTabByTag("sciencenet");
				break;
			case R.id.radio_sciencenewspaper:
				tabHost.setCurrentTabByTag("sciencenewspaper");
				break;
			case R.id.radio_collection:
				tabHost.setCurrentTabByTag("collection");
				break;
			default:
				break;
			}
		}
	};
}
