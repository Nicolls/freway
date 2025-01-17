/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freway.ebike.map;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.IBinder;
import android.os.Parcelable;

import com.freway.ebike.bluetooth.BlueToothConstants;
import com.freway.ebike.bluetooth.BlueToothService;
import com.freway.ebike.bluetooth.EBikeTravelData;
import com.freway.ebike.common.BaseApplication;
import com.freway.ebike.common.EBConstant;
import com.freway.ebike.db.DBHelper;
import com.freway.ebike.model.TravelLocation;
import com.freway.ebike.utils.LogUtils;

import java.util.List;
import java.util.Random;

public class MapService extends Service /*implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener*/ {
	private final static String TAG = MapService.class.getSimpleName();
	/** 国内定位纬度偏差 */
	private static final double LAT_OFFSET = 0.0012893886;
	/** 国内定位精度偏差 */
	private static final double LNG_OFFSET = 0.0061154366;
	/*private static final LocationRequest REQUEST = LocationRequest.create().setInterval(2000) // 5
																								// seconds
			.setFastestInterval(100) // 16ms = 60fps
			.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	private GoogleApiClient mGoogleApiClient;*/
	private static final float RECORD_MIN_DISTANCE = 2;// 达到记录的最短距离2米，但是实时
														// 是不管多少都要画在地图上
	private TravelLocation fromLocation;// 开始画位置
	private TravelLocation toLocation;// 结束画位置
	private boolean isRecord = false;
	private TravelLocation currentLocation;// 当前的位置

	// 计算
	private NodifyUIThread nodifyUIThread;
	private boolean isNodify = false;
	private static final int NODIFY_TIME=300;//刷新UI间隔ms
	@Override
	public void onCreate() {
		super.onCreate();
		LogUtils.i(TAG, TAG + "onCreate");
		/*mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this).build();*/
		// 注册广播
		IntentFilter filter = new IntentFilter(TravelConstant.ACTION_UI_SERICE_TRAVEL_STATE_CHANGE);
		registerReceiver(mStateReceiver, filter);
		filter = new IntentFilter(TravelConstant.ACTION_UI_SERICE_QUIT_APP);// 退出app
		registerReceiver(mBleStateReceiver, filter);
		filter = new IntentFilter(BlueToothConstants.BLE_SERVER_STATE_CHANAGE);
		registerReceiver(mBleStateReceiver, filter);
		// ToastUtils.toast(this, "onCreate");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		LogUtils.i(TAG, "onStartCommand");
		// ToastUtils.toast(this, "onStartCommand");
/*
		mGoogleApiClient.connect();
*/
		initData();
		return super.onStartCommand(intent, flags, startId);
	}

	/** 初始化数据,初始化完成，要把状态返回到UI， */
	private void initData() {
		if (BaseApplication.travelId > 0) {
			if (BaseApplication.travelState == TravelConstant.TRAVEL_STATE_START
					|| BaseApplication.travelState == TravelConstant.TRAVEL_STATE_RESUME
					|| BaseApplication.travelState == TravelConstant.TRAVEL_STATE_FAKE_PAUSE) {
				isRecord = true;
			} else {
				isRecord = false;
			}
			List<TravelLocation> routes = DBHelper.getInstance(this).listTravelLocation(BaseApplication.travelId);
			for (int i = 0; i < routes.size(); i++) {
				TravelLocation from = routes.get(i);
				TravelLocation to = null;
				if ((i + 1) < routes.size()) {
					to = routes.get(i + 1);
					// to.setDescription("这是我初始化从数据库拿的");
					broadCastLocation(TravelConstant.ACTION_MAP_SERVICE_LOCATION_CHANGE, toLocation, from, to);
				}
			}
		}
	}

	/** 监听UI发送的广播 */
	private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (TravelConstant.ACTION_UI_SERICE_TRAVEL_STATE_CHANGE.equals(action)
					&& BaseApplication.workModel == EBConstant.WORK_MAP) {
				int state = intent.getIntExtra(TravelConstant.EXTRA_STATE, 0);
				if (state == TravelConstant.TRAVEL_STATE_START) {// 开始
					start();
				} else if (state == TravelConstant.TRAVEL_STATE_PAUSE) {// 暂停
					pause();
				} else if (state == TravelConstant.TRAVEL_STATE_FAKE_PAUSE) {// 伪暂停
					fakePause();
				} else if (state == TravelConstant.TRAVEL_STATE_RESUME) {// 恢复
					resume();
				} else if (state == TravelConstant.TRAVEL_STATE_COMPLETED) {// 完成
					completed();
				} else if (state == TravelConstant.TRAVEL_STATE_STOP) {// 停止
					stop();
				}
			} else if (TravelConstant.ACTION_UI_SERICE_QUIT_APP.equals(action)) {// 退出应用
				exit();
			}
		}
	};

	/**
	 * The BroadcastReceiver that listens for discovered devices and changes the
	 * title when discovery is finished
	 */
	private final BroadcastReceiver mBleStateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BlueToothConstants.BLE_SERVER_STATE_CHANAGE.equals(action)) {
				int bleState = intent.getIntExtra(BlueToothConstants.EXTRA_STATE, 0);
				switch (bleState) {
				case BlueToothConstants.BLE_STATE_NONE:

					break;
				case BlueToothConstants.BLE_STATE_CONNECTED:
					LogUtils.i(TAG, "map service BLE_STATE_CONNECTED " + BaseApplication.travelState);
					// mark 原先是，当ble断开时，自动暂停，那现在做成断开时不用自动暂停。
					// if (BaseApplication.travelState ==
					// TravelConstant.TRAVEL_STATE_PAUSE) {
					// BaseApplication.sendStateChangeBroadCast(context,
					// TravelConstant.TRAVEL_STATE_RESUME);
					// }
					break;
				case BlueToothConstants.BLE_STATE_CONNECTTING:

					break;
				case BlueToothConstants.BLE_STATE_DISCONNECTED:
					// mark 原先是，当ble断开时，自动暂停，那现在做成断开时不用自动暂停,但是这个线要画成别的颜色，用来区分。
					// if (BaseApplication.travelState ==
					// TravelConstant.TRAVEL_STATE_START
					// || BaseApplication.travelState ==
					// TravelConstant.TRAVEL_STATE_RESUME) {
					// BaseApplication.sendStateChangeBroadCast(context,
					// TravelConstant.TRAVEL_STATE_PAUSE);
					// }
					break;
				default:
					break;
				}
			}
		}
	};

/*
	@Override
*/
	public void onLocationChanged(Location location) {
//		LogUtils.i(TAG, "onLocationChanged" + location.getLatitude() + "--" + location.getLongitude());
		TravelLocation travelLocation = new TravelLocation(location);
		// travelLocation=formatLocationWithChina(travelLocation);//纠正经纬度。发布的时候要去掉
		travelLocation.setTravelId(BaseApplication.travelId);
		travelLocation.setSpeed(EBikeTravelData.getInstance(this).insSpeed);
		travelLocation.setAltitude(location.getAltitude());
		float pointDistance = 0;
		if (fromLocation != null) {
			pointDistance = fromLocation.getLocation().distanceTo(location);
		}
		broadCastLocation(TravelConstant.ACTION_MAP_SERVICE_LOCATION_CHANGE, travelLocation, null, null);// 当前位置
		if (fromLocation != null && pointDistance > RECORD_MIN_DISTANCE) {// 当两点距离值大于最小记录距离值，才算是发生距离改变
			fromLocation = toLocation;
			toLocation = travelLocation;
		} else {
			fromLocation = travelLocation;
			toLocation = travelLocation;
		}
		if (isRecord) {// 正在记录
			EBikeTravelData.getInstance(this).altitude = travelLocation.getLocation().getAltitude() / 1000;// 海拔行程存储的是km,所以要除以1000
			// 判断是开始的位置
			if (currentLocation == null) {// 如果是开始，则通知行程开始
				broadCastLocation(TravelConstant.ACTION_MAP_SERVICE_LOCATION_START, travelLocation, null, null);// 当前位置
			}
			currentLocation = travelLocation;

			// 当为控制器模式时如果ble断开，则把这些点都设置为停止状态，
			if (BaseApplication.workModel == EBConstant.WORK_BLUETOOTH
					&& BlueToothService.ble_state != BlueToothConstants.BLE_STATE_CONNECTED) {
				currentLocation.setPause(true);
				toLocation.setPause(true);
			}

			//发送地图数据格式化
			EBikeTravelData.getInstance(this).parseMapData(fromLocation, toLocation);
			// 向UI发送位置改变，画数据
			broadCastLocation(TravelConstant.ACTION_MAP_SERVICE_LOCATION_CHANGE, travelLocation, fromLocation,
					toLocation);// 当前位置
			// 记录数据
			if (pointDistance > RECORD_MIN_DISTANCE) {// 在过最小记录才去记录这个数据
				travelLocation.setTravelId(BaseApplication.travelId);
				DBHelper.getInstance(this).insertTravelLocation(currentLocation);
			}
		}
	}

	// test
	int m = 0;
	Random r = new Random();

	/** 重新格式化位置为国内坐标 */
	private TravelLocation formatLocationWithChina(TravelLocation location) {
		m++;
		if (location != null) {
			location.getLocation().setLatitude(location.getLocation().getLatitude() + LAT_OFFSET);
			location.getLocation().setLongitude(location.getLocation().getLongitude() + LNG_OFFSET);
			// test
			// location.getLocation().setLatitude(location.getLocation().getLatitude()
			// + LAT_OFFSET+0.001f*m);
			// location.getLocation().setLongitude(location.getLocation().getLongitude()
			// + LNG_OFFSET+0.001*r.nextInt(10));
		}
		return location;
	}

	/** 广播地址变化 */
	private void broadCastLocation(String action, TravelLocation current, TravelLocation from, TravelLocation to) {
		Intent intent = new Intent(action);
		intent.putExtra(TravelConstant.EXTRA_LOCATION_CURRENT, current);
		intent.putExtra(TravelConstant.EXTRA_LOCATION_FROM, from);
		intent.putExtra(TravelConstant.EXTRA_LOCATION_TO, to);
		sendBroadcast(intent);
	}

	/** 开始 */
	public void start() {
		LogUtils.i(TAG, "mapservice travel start ");
		fromLocation = null;
		toLocation = null;
		currentLocation = null;
		isRecord = true;
		LogUtils.i(TAG, "行程ID－－" + BaseApplication.travelId);
		isNodify = true;
		if (nodifyUIThread == null) {
			nodifyUIThread = new NodifyUIThread();
			nodifyUIThread.start();
		}
	}

	/** 暂停 */
	public void pause() {
		LogUtils.i(TAG, "mapservice travel pause ");
		isRecord = false;
		if (currentLocation != null) {
			currentLocation.setPause(true);
			DBHelper.getInstance(this).updateTravelLocation(currentLocation);
			broadCastLocation(TravelConstant.ACTION_MAP_SERVICE_LOCATION_CHANGE, currentLocation, currentLocation,
					currentLocation);
		}
	}

	/** 伪暂停 */
	public void fakePause() {
		LogUtils.i(TAG, "mapservice travel fakePause ");
		isRecord = true;
	}

	/** 恢复 */
	public void resume() {
		LogUtils.i(TAG, "mapservice travel resume ");
		isRecord = true;
	}

	/** 完成 */
	public void completed() {
		LogUtils.i(TAG, "mapservice travel completed " + nodifyUIThread);
		isRecord = false;
		if (nodifyUIThread != null) {
			nodifyUIThread.cancel();
		}
	}

	/** 停止 */
	public void stop() {
		LogUtils.i(TAG, "mapservice travel stop" + nodifyUIThread);
		isRecord = false;
		fromLocation = null;
		toLocation = null;
		if (nodifyUIThread != null) {
			nodifyUIThread.cancel();
		}
	}

	/** 退出应用 */
	public void exit() {
		if (BaseApplication.travelState == TravelConstant.TRAVEL_STATE_NONE
				|| BaseApplication.travelState == TravelConstant.TRAVEL_STATE_STOP
				|| BaseApplication.travelState == TravelConstant.TRAVEL_STATE_COMPLETED) {// 这些情况将不需要记录并且开启定位功能了
			/*mGoogleApiClient.disconnect();*/
			unregisterReceiver(mStateReceiver);
			unregisterReceiver(mBleStateReceiver);
		}
	}

	/*@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		LogUtils.i(TAG, "onConnectionFailed");
	}

	@Override
	public void onConnected(Bundle arg0) {
		// ToastUtils.toast(this, "onConnected");
		LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, REQUEST, this); // 位置变化监听
	}
*/
	@Override
	public void onDestroy() {
		super.onDestroy();
/*
		mGoogleApiClient.disconnect();
*/
		unregisterReceiver(mStateReceiver);
		unregisterReceiver(mBleStateReceiver);
		LogUtils.i(TAG, "onDestroy");
		// ToastUtils.toast(this, "onDestroy");
	}

	/*@Override
	public void onConnectionSuspended(int arg0) {
		LogUtils.i(TAG, "onConnectionSuspended");
	}
*/
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * This thread runs while connect is interrupt attempting to reconnect
	 */
	private class NodifyUIThread extends Thread {
		public void run() {
			LogUtils.i(TAG, "BEGIN NodifyUIThread:");
			setName("NodifyUIThread");
			while (isNodify) {
				try {
					Thread.sleep(NODIFY_TIME);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (isRecord&&currentLocation!=null) {// 正在记录数据
					//计算数据
					EBikeTravelData.getInstance(MapService.this).parseMapSpeed(currentLocation);
					// 通知UI
					broadCastData2UI(BlueToothConstants.BLUETOOTH_ACTION_HANDLE_SERVER_RESULT_SEND_DATA,
							BlueToothConstants.RESULT_SUCCESS, null);// 提示UI更新
				}
			}
			synchronized (MapService.this) {
				nodifyUIThread = null;
			}
		}

		public void cancel() {
			isNodify = false;
		}
	}

	/** 通知UI对应操作的结果 */
	private void broadCastData2UI(String action, int status, Parcelable data) {
		Intent intent = new Intent(action);
		intent.putExtra(BlueToothConstants.EXTRA_STATUS, status);
		intent.putExtra(BlueToothConstants.EXTRA_DATA, data);
		sendBroadcast(intent);
	}
}
