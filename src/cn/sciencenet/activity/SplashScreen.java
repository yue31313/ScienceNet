package cn.sciencenet.activity;

import cn.sciencenet.R;
import cn.sciencenet.util.AppUtil;
import cn.sciencenet.util.DataUrlKeys;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

public class SplashScreen extends Activity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);//宽高全屏显示
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.splash);

		int screenWidth = getResources().getDisplayMetrics().widthPixels;
		int screenHeight = getResources().getDisplayMetrics().heightPixels;

		AppUtil.GALLERY_HEIGHT = (int) (0.65f * (float) screenHeight);
		AppUtil.GALLERY_WIDTH = (int) (0.9f * (float) screenWidth);
		AppUtil.ITEM_IMG_HEIGHT = dip2px(65.0f);
		AppUtil.ITEM_IMG_WIDTH = dip2px(80.0f);
		Log.i("splashScreen", "AppUtil.GALLERY_WIDTH:" + AppUtil.GALLERY_WIDTH
				+ "    AppUtil.GALLERY_HEIGHT:" + AppUtil.GALLERY_HEIGHT);
		Log.i("splashScreen", "AppUtil.ITEM_IMG_WIDTH" + AppUtil.ITEM_IMG_WIDTH
				+ "    AppUtil.ITEM_IMG_HEIGHT" + AppUtil.ITEM_IMG_HEIGHT);
		
		SharedPreferences sharedata = getSharedPreferences("data", 0);  
		DataUrlKeys.currentFontSizeFlag = sharedata.getInt("font_size", 1);  
		
		// splash screen 3 seconds
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				Intent intent = new Intent(SplashScreen.this,
						MainTabActivity.class);
				SplashScreen.this.startActivity(intent);
				SplashScreen.this.finish();
			}
		}, 3000);
	}

	private int dip2px(float dip) {
		final float scale = getResources().getDisplayMetrics().density;
		return (int) (dip * scale + 0.5f);
	}
}
