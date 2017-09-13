package cn.sciencenet.dialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import cn.sciencenet.R;
import cn.sciencenet.activity.ConfigurationActivity;
import cn.sciencenet.util.DataUrlKeys;
import cn.sciencenet.util.HttpUtil;

public class LoginDialog extends Dialog {

	public String usernameString;
	public int userid;

	private Button yesButton;
	private Button cancelButton;
	private EditText et_username;
	private EditText et_password;
	private TextView tv_warning;
	
	private Context mContext;
	
	public LoginDialog(Context context) {
		super(context);
		mContext = context;
	}

	public void setDisplay() {
		setContentView(R.layout.layout_login_dialog);
		yesButton = (Button) findViewById(R.id.btn_login);
		cancelButton = (Button) findViewById(R.id.btn_cancel);
		yesButton.setOnClickListener(yesClickListener);
		cancelButton.setOnClickListener(cancelClickListener);
		setTitle("登录");
		setCancelable(false);
		show();
	}

	View.OnClickListener yesClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			new LoginTask().execute("begin");
		}
	};
	
	/**
	 * 登录的事件
	 * @author liushuai
	 *
	 */
	private class LoginTask extends AsyncTask<String, Integer, String> {
		
		ProgressDialog dlg;
		String localPassword;
		
		@Override
		protected void onPreExecute() {
			dlg = new ProgressDialog(mContext);
			dlg.setTitle("登录");
			dlg.setMessage("正在登录，请稍后...");
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
			
			et_username = (EditText) findViewById(R.id.username);
			et_password = (EditText) findViewById(R.id.password);
			tv_warning = (TextView) findViewById(R.id.warining);
			usernameString = et_username.getText().toString();
			localPassword = et_password.getText().toString();
			Log.i("username", usernameString);
			Log.i("password", localPassword);
		}
		
		@Override
		protected String doInBackground(String... params) {
			HttpUtil httpUtil = new HttpUtil();
			userid = httpUtil.Login(usernameString, localPassword);
			Log.i("logindialog_username", usernameString);
			Log.e("logindialog_uid", "uid" + userid);
			return null;
		}
		
		@Override
		protected void onPostExecute(String result) {
			dlg.dismiss();
			if (userid > 0) {
				DataUrlKeys.uid = userid;
				DataUrlKeys.username = usernameString;
				DataUrlKeys.isLogined = true;
//				result = true;
				if (DataUrlKeys.isComment) {
					new CommentDialog(DataUrlKeys.getId, getContext())
							.setDisplay();
				}
				dismiss();
				mContext.sendBroadcast(new Intent(ConfigurationActivity.CHANGE_ITEM_TEXT));
			} else if (userid == -1) {
				tv_warning.setText("用户名不存在，或者被删除");
			} else if (userid == -2) {
				tv_warning.setText("密码错误");
			} else if (userid == -3) {
				tv_warning.setText("安全问题错误");
			}
		}
	}
	
	View.OnClickListener cancelClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			dismiss();
		}
	};
}
