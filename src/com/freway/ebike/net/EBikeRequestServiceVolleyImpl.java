/**
 * 
 */
package com.freway.ebike.net;

import java.io.File;
import java.lang.reflect.Type;

import android.content.Context;

import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.freway.ebike.model.EBRequest;
import com.freway.ebike.model.EBResponse;
import com.freway.ebike.model.RspLogin;
import com.freway.ebike.model.RspRegister;
import com.freway.ebike.model.RspUpLoadTravel;
import com.freway.ebike.model.RspUpdatePhoto;
import com.freway.ebike.model.RspUpdateUserInfo;
import com.freway.ebike.model.RspUserInfo;
import com.freway.ebike.model.User;
import com.freway.ebike.utils.LogUtils;
import com.freway.ebike.utils.MD5Tool;

/**
 * 通过volley框架来实现的与服务器交互接口的请求类
 * 
 * @author Nicolls
 * 
 */
public class EBikeRequestServiceVolleyImpl implements EBikeRequestService {
	private static final String TAG=EBikeRequestServiceVolleyImpl.class.getSimpleName();
	protected DataUpdateListener dataUpdateListener;
	private Context context;

	public EBikeRequestServiceVolleyImpl(Context context) {
		this.context = context;
	}

	@Override
	public void setUptateListener(DataUpdateListener dataUpdateListener) {
		this.dataUpdateListener = dataUpdateListener;
	}

	private void dataNotify(int what, Object obj) {
		if (dataUpdateListener != null) {
			dataUpdateListener.update(what, obj);
		}
	}

	/** 集成发送方法 */
	private <T extends EBResponse> void sendRequest(EBRequest ebRequest, final int methodId, Type type) {
		// 在这里设置参数，和私钥参数
		// 请求参数
		// String
		// data="{\"email\":\"pwj1230@126.com\",\"username\":\"pwj1230\",\"password\":\"123456\"}";
		// String s="d6c0cc040d";
		String data = ebRequest.getDataParam();
		ebRequest.setReqeustParam("data", data);
		String s = MD5Tool.md5(MD5Tool.md5(data + EBRequest.requestKey));
		if (s.length() >= 10) {
			s = s.substring(s.length() - 10, s.length());
		}
		ebRequest.setReqeustParam("s", s);
		VolleyGsonRequest<T> request = new VolleyGsonRequest<T>(context, ebRequest, type, new Response.Listener<T>() {

			@Override
			public void onResponse(T response) {
				dataNotify(methodId, response);
			}

		}, new Response.ErrorListener() {

			@Override
			public void onErrorResponse(VolleyError error) {
				LogUtils.e(EBikeRequestServiceVolleyImpl.class.getSimpleName(), "请求服务器异常:" + error.getMessage());
				dataNotify(ID_REQUEST_ERROR, null);
			}

		});
		if (methodId == EBikeRequestService.ID_LOGIN) {// 登录的请求要把所有值还原，比如cookie
			request.clearCookie();
		}
		VolleyRequestQueue.getInstance(context).addToRequestQueue(request);
	}

	@Override
	public void login(String username, String password) {
		EBRequest ebReq = new EBRequest(EBikeRequestService.METHOD_LOGIN);
		ebReq.setDataParam("username", username);
		ebReq.setDataParam("password", password);
		sendRequest(ebReq, EBikeRequestService.ID_LOGIN, RspLogin.class);
	}

	@Override
	public void register(String email, String username, String password) {
		EBRequest ebReq = new EBRequest(EBikeRequestService.METHOD_REGISTER);
		ebReq.setDataParam("email", email);
		ebReq.setDataParam("username", username);
		ebReq.setDataParam("password", password);
		sendRequest(ebReq, EBikeRequestService.ID_REGISTER, RspRegister.class);
	}

	@Override
	public void loginFaceBook(String userid, String username, String gender, String birthday, String photo,
			String email) {
		EBRequest ebReq = new EBRequest(EBikeRequestService.METHOD_LOGINFACEBOOK);
		ebReq.setDataParam("userid", userid);
		ebReq.setDataParam("username", username);
		ebReq.setDataParam("gender", gender);
		ebReq.setDataParam("birthday", birthday);
		ebReq.setDataParam("photo", photo);
		ebReq.setDataParam("email", email);
		sendRequest(ebReq, EBikeRequestService.ID_LOGINFACEBOOK, RspLogin.class);
	}

	@Override
	public void loginTwitter(String userid, String username, String gender, String birthday, String photo,
			String email) {
		EBRequest ebReq = new EBRequest(EBikeRequestService.METHOD_LOGINTWITTER);
		ebReq.setDataParam("userid", userid);
		ebReq.setDataParam("username", username);
		ebReq.setDataParam("gender", gender);
		ebReq.setDataParam("birthday", birthday);
		ebReq.setDataParam("photo", photo);
		ebReq.setDataParam("email", email);
		sendRequest(ebReq, EBikeRequestService.ID_LOGINTWITTER, RspLogin.class);
	}

	@Override
	public void userInfo(String token) {
		EBRequest ebReq = new EBRequest(EBikeRequestService.METHOD_USERINFO);
		ebReq.setDataParam("token", token);
		sendRequest(ebReq, EBikeRequestService.ID_USERINFO, RspUserInfo.class);
	}

	@Override
	public void updateUserInfo(String token, User user) {
		EBRequest ebReq = new EBRequest(EBikeRequestService.METHOD_UPDATEUSERINFO);
		ebReq.setDataParam("token", token);
		ebReq.setDataParam("username", user.getUsername());
		ebReq.setDataParam("gender", user.getGender());
		ebReq.setDataParam("birthday", user.getBirthday());
		ebReq.setDataParam("email", user.getEmail());
		sendRequest(ebReq, EBikeRequestService.ID_UPDATEUSERINFO, RspUpdateUserInfo.class);
	}

	@Override
	public void upLoadTravel(String token, String type, String stime, String etime, String distance, String time,
			String cadence, String calories, String speedList, String locationList, String topSpeed, String avgSpeed) {
		EBRequest ebReq = new EBRequest(EBikeRequestService.METHOD_UPLOADTRAVEL);
		ebReq.setDataParam("token", token);
		ebReq.setDataParam("type", type);
		ebReq.setDataParam("stime", stime);
		ebReq.setDataParam("etime", etime);
		ebReq.setDataParam("distance", distance);
		ebReq.setDataParam("distance", token);
		ebReq.setDataParam("time", time);
		ebReq.setDataParam("cadence", cadence);
		ebReq.setDataParam("calories", calories);
		ebReq.setDataParam("speedList", speedList);
		ebReq.setDataParam("locationList", locationList);
		ebReq.setDataParam("topSpeed", topSpeed);
		ebReq.setDataParam("topSpeed", avgSpeed);
		sendRequest(ebReq, EBikeRequestService.ID_UPLOADTRAVEL, RspUpLoadTravel.class);
	}

	@Override
	public void updatePhoto(final String token, final String photoPath) {
		File file=new File(photoPath);
		if(file.exists()){
			EBRequest ebReq = new EBRequest(EBikeRequestService.METHOD_PHOTO);
			ebReq.setDataParam("token", token);
			String data = ebReq.getDataParam();
			ebReq.setReqeustParam("data", data);
			String s = MD5Tool.md5(MD5Tool.md5(data + EBRequest.requestKey));
			if (s.length() >= 10) {
				s = s.substring(s.length() - 10, s.length());
			}
			ebReq.setReqeustParam("s", s);
			UploadFileRequest request = new UploadFileRequest(ebReq.getReqeustURL(), new ErrorListener() {

				@Override
				public void onErrorResponse(VolleyError error) {
					LogUtils.e(TAG, error.getMessage());
					LogUtils.i(TAG, "上传图片失败，"+error.getMessage());
					dataNotify(ID_REQUEST_ERROR, null);
				}
			}, new Listener<String>() {

				@Override
				public void onResponse(String response) {
					LogUtils.i(TAG, "上传图片返回:"+response);
					RspUpdatePhoto updatePhoto=new RspUpdatePhoto();
					updatePhoto.setText(response);
					dataNotify(EBikeRequestService.ID_PHOTO, updatePhoto);
				}
			},"login_bg", file, ebReq.getReqeustParam());
			VolleyRequestQueue.getInstance(context).addToRequestQueue(request);
		}else{
			LogUtils.e(TAG, "文件不存在");
		}
		
	}

}
