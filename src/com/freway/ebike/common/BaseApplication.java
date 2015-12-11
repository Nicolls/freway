package com.freway.ebike.common;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
import android.support.multidex.MultiDexApplication;
import android.text.TextUtils;

import com.facebook.FacebookSdk;
import com.freway.ebike.bluetooth.BLEScanConnectActivity;
import com.freway.ebike.bluetooth.BlueToothConstants;
import com.freway.ebike.bluetooth.BlueToothService;
import com.freway.ebike.bluetooth.BlueToothUtil;
import com.freway.ebike.bluetooth.EBikeTravelData;
import com.freway.ebike.db.DBHelper;
import com.freway.ebike.db.Travel;
import com.freway.ebike.map.TravelConstant;
import com.freway.ebike.utils.SPUtils;

/**
 * Application基类
 * */
public class BaseApplication extends MultiDexApplication{

	public static long travelId=-1;
	public static int travelState=TravelConstant.TRAVEL_STATE_NONE;
	@Override
	public void onCreate() {
		super.onCreate();
		initSdk();
	}
	/**初始化第三方sdk*/
	private void initSdk(){
		FacebookSdk.sdkInitialize(this.getApplicationContext());
	}
	
	/**
	 * @param context 上下文，只能是发送者如activity或者service
	 * @param state 改变的状态
	 * @param isSelfReceiver 自己是否要接收自己发送的此次状态改变的广播
	 * @param receiver 状态改变的广播 如果isSelfReceiver为false就是不需要接收，那么receiver一定不能为空
	 * @return void
	 * @Description 发送状态改变广播
	 */
	public static void sendStateChangeBroadCast(final Context context,int state){
		if(state==TravelConstant.TRAVEL_STATE_START||state==TravelConstant.TRAVEL_STATE_RESUME){//要去检查蓝牙有没有断线
			if(BlueToothService.ble_state!=BlueToothConstants.BLE_STATE_CONNECTED){//未链接，则要提示用户去链接
				if(TextUtils.isEmpty(SPUtils.getEBkieAddress(context))){
					BlueToothUtil.toBindBleActivity(context,BLEScanConnectActivity.HANDLE_SCAN);
				}else{
					BlueToothUtil.toBindBleActivity(context,BLEScanConnectActivity.HANDLE_CONNECT);
				}
				return;
			}
		}
		travelState=state;
		Intent intent = new Intent(TravelConstant.ACTION_UI_SERICE_TRAVEL_STATE_CHANGE);
		intent.putExtra(TravelConstant.EXTRA_STATE, travelState);
		if (state == TravelConstant.TRAVEL_STATE_START && checkGpsEnable(context)) {// 在这里判断定位有没有打开
			Travel travel=new Travel();
			DBHelper.getInstance(context).insertTravel(travel);
			travelId=travel.getId();
			EBikeTravelData.getInstance(context).start(travel.getId(),TravelConstant.TRAVEL_TYPE_IM);
			intent.putExtra(TravelConstant.EXTRA_TRAVEL_ID, travelId);
		}else if(state == TravelConstant.TRAVEL_STATE_PAUSE){
			EBikeTravelData.getInstance(context).pause();
		}else if(state == TravelConstant.TRAVEL_STATE_RESUME){
			EBikeTravelData.getInstance(context).resume();
		}else if(state==TravelConstant.TRAVEL_STATE_COMPLETED){//存储
			EBikeTravelData.getInstance(context).completed();
		}else if(state==TravelConstant.TRAVEL_STATE_STOP){
			EBikeTravelData.getInstance(context).stop();
		}else if(state==TravelConstant.TRAVEL_STATE_FAKE_PAUSE){//伪暂停
			travelState=state;
		}else{
			travelState=TravelConstant.TRAVEL_STATE_NONE;
		}
		context.sendBroadcast(intent);
	}
	/**发送退出App的广播*/
	public static void sendQuitAppBroadCast(Context context){
		Intent intent = new Intent(TravelConstant.ACTION_UI_SERICE_QUIT_APP);
		context.sendBroadcast(intent);
	}
	
	/** 判断定位是否打开 */
	public static boolean checkGpsEnable(Context context) {
		final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			buildAlertMessageNoGps(context);
			return false;
		} else {
			return true;
		}
	}
	/** 弹出确认打开Gps对话框 */
	private static void buildAlertMessageNoGps(final Context context) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage("Your GPS seems to be disabled, do you want to enable it?").setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(@SuppressWarnings("unused") final DialogInterface dialog,
							@SuppressWarnings("unused") final int id) {
						context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
					}
				}).setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
						dialog.cancel();
					}
				});
		final AlertDialog alert = builder.create();
		alert.show();
	}
}
