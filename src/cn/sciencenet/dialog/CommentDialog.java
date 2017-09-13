package cn.sciencenet.dialog;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import cn.sciencenet.R;
import cn.sciencenet.activity.ShowCommentActivity;
import cn.sciencenet.util.DataUrlKeys;
import cn.sciencenet.util.DateUtil;
import cn.sciencenet.util.EncryptBySHA1;

public class CommentDialog extends Dialog implements OnClickListener {

	private Button yesButton;
	private Button cancelButton;
	private TextView showInfo;
	private EditText commentContent;

	private String id = null;

	private Context mContext;

	public CommentDialog(Context context) {
		super(context);
		mContext = context;
	}

	public CommentDialog(String id, Context context) {
		super(context);
		this.id = id;
		mContext = context;
	}

	public void setDisplay() {
		setContentView(R.layout.layout_comment);
		showInfo = (TextView) findViewById(R.id.tv_showInfo);
		commentContent = (EditText) findViewById(R.id.comment_content_et);
		yesButton = (Button) findViewById(R.id.btn_yes);
		cancelButton = (Button) findViewById(R.id.btn_no);
		yesButton.setOnClickListener(yesClickListener);
		cancelButton.setOnClickListener(cancelClickListener);
		setTitle("请输入你的评论：　　　　");
		show();
	}

	View.OnClickListener yesClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			new postCommentTask().execute("begin");
		}
	};

	/**
	 * 发布评论的任务
	 * 
	 * @author liushuai
	 * 
	 */
	private class postCommentTask extends AsyncTask<String, Integer, String> {

		ProgressDialog dlg;

		HttpPost httpRequest = null;
		String pass = null;
		List<NameValuePair> params = null;
		HttpResponse httpResponse = null;

		@Override
		protected void onPreExecute() {

			dlg = new ProgressDialog(mContext);
			dlg.setTitle("发布评论");
			dlg.setMessage("正在发表您的评论，请稍后...");
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

			pass = EncryptBySHA1.Encrypt(DateUtil.getCurrentDate());
			if (DataUrlKeys.type.equals("news_comment")) {
				String newsCommentUrl = DataUrlKeys.RELEASE_NEWS_COMMENT_URL
						.replace("$pass", pass) + id;

				Log.i("comment_url", newsCommentUrl);
				httpRequest = new HttpPost(newsCommentUrl);

				params = new ArrayList<NameValuePair>();

				Log.i("username", DataUrlKeys.username);
				Log.i("comment", commentContent.getText().toString());
				params.add(new BasicNameValuePair("username",
						DataUrlKeys.username));
				params.add(new BasicNameValuePair("commcontent", commentContent
						.getText().toString()));
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params,
							HTTP.UTF_8));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			} else if (DataUrlKeys.type.equals("newspaper_comment")) {
				String newspaperCommmentUrl = DataUrlKeys.RELEASE_NEWSPAPER_COMMENT_URL
						.replace("$pass", pass) + id;

				httpRequest = new HttpPost(newspaperCommmentUrl);

				params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("username",
						DataUrlKeys.username));
				params.add(new BasicNameValuePair("commcontent", commentContent
						.getText().toString()));
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params,
							HTTP.UTF_8));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			} else if (DataUrlKeys.type.equals("blog_comment")) {
				String blogCommentUrl = DataUrlKeys.RELEASE_BLOG_COMMENT_URL
						.replace("$blogid", id) + pass;
				Log.i("comment_url", blogCommentUrl);
				httpRequest = new HttpPost(blogCommentUrl);

				params = new ArrayList<NameValuePair>();
				Log.e("username", DataUrlKeys.username);
				Log.e("comment", commentContent.getText().toString());
				params.add(new BasicNameValuePair("username",
						DataUrlKeys.username));
				params.add(new BasicNameValuePair("message", commentContent
						.getText().toString()));
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params,
							"GBK"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			} else if (DataUrlKeys.type.equals("group_comment")) {
				String groupCommentUrl = DataUrlKeys.RELEASE_GROUP_COMMENT_URL
						.replace("$tid", id) + pass;
				httpRequest = new HttpPost(groupCommentUrl);

				params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("username",
						DataUrlKeys.username));
				params.add(new BasicNameValuePair("message", commentContent
						.getText().toString()));
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params,
							"GBK"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		protected String doInBackground(String... params) {
			try {
				httpResponse = new DefaultHttpClient()
						.execute(httpRequest);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			dlg.dismiss();
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
//				Toast.makeText(getContext(), "评论成功,请刷新查看", Toast.LENGTH_SHORT)
//						.show();
				mContext.sendBroadcast(new Intent(ShowCommentActivity.REFERSH_COMMENT_LIST));
				dismiss();
			} else {
				showInfo.setText("Error Response"
						+ httpResponse.getStatusLine().toString());
			}
		}
	}

	View.OnClickListener cancelClickListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			dismiss();
		}
	};

	@Override
	public void onClick(View v) {
	}

}
