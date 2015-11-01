package com.freway.ebike.db;

import java.util.ArrayList;
import java.util.List;

import com.freway.ebike.db.EBikeTable.TravelBluetoothEntry;
import com.freway.ebike.db.EBikeTable.TravelEntry;
import com.freway.ebike.db.EBikeTable.TravelLocationEntry;
import com.freway.ebike.utils.LogUtils;
import com.google.gson.Gson;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
/**数据库帮助类*/
public class DBHelper extends SQLiteOpenHelper {
	private final static String TAG = DBHelper.class.getSimpleName();
	private static DBHelper _instance;
	private static final String DB_NAME = "freway.db";
	private static final int DB_VERSION = 1;
	private static Context mContext;
	private static Gson gson=new Gson();
	public static DBHelper getInstance(Context context) {
		mContext = context;
		if (_instance == null)
			_instance = new DBHelper(context, DB_NAME, null, DB_VERSION);
		return _instance;
	}

	public DBHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
		mContext = context;
	}

	@Override
	public void onCreate(SQLiteDatabase sqliteDatabase) {
		//建立表
		//位置表
		sqliteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + TravelLocationEntry.TABLE_NAME + " (" 
				+ TravelLocationEntry._ID+ " INTEGER PRIMARY KEY AUTOINCREMENT," 
				+ TravelLocationEntry.COLUMN_TRAVEL_ID + " INTEGER," 
				+ TravelLocationEntry.COLUMN_ISPAUSE + " INTEGER," 
				+ TravelLocationEntry.COLUMN_DESCRIPTION + " TEXT,"
				+ TravelLocationEntry.COLUMN_LOCATION + " TEXT"
				+");");
		
		//蓝牙数据表
		sqliteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + TravelBluetoothEntry.TABLE_NAME + " (" 
				+ TravelBluetoothEntry._ID+ " INTEGER PRIMARY KEY AUTOINCREMENT," 
				+ TravelBluetoothEntry.COLUMN_TRAVEL_ID + " INTEGER" 
				+");");
		
		//行程表
		sqliteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + TravelEntry.TABLE_NAME + " (" 
				+ TravelEntry._ID+ " INTEGER PRIMARY KEY AUTOINCREMENT," 
				+ TravelEntry.COLUMN_STARTTIME + " INTEGER," 
				+ TravelEntry.COLUMN_ENDTIME + " INTEGER," 
				+ TravelEntry.COLUMN_SPENDTIME + " TEXT,"
				+ TravelEntry.COLUMN_DISTANCE + " TEXT"
				+");");

//				+ " TEXT DEFAULT 'false'," + DevicesColumns.ONLINE + " TEXT DEFAULT 'true');");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TravelLocationEntry.TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + TravelBluetoothEntry.TABLE_NAME);
		db.execSQL("DROP TABLE IF EXISTS " + TravelEntry.TABLE_NAME);
		onCreate(db);
	}
	//行程
	public long insertTravel(Travel travel) {
		SQLiteDatabase sqliteDatabase = getWritableDatabase();
		ContentValues contentValues = new ContentValues();
		contentValues.put(TravelEntry.COLUMN_STARTTIME, travel.formatTime(travel.getStartTime()));
		contentValues.put(TravelEntry.COLUMN_ENDTIME, travel.formatTime(travel.getEndTime()));
		contentValues.put(TravelEntry.COLUMN_SPENDTIME, travel.getSpendTime());
		contentValues.put(TravelEntry.COLUMN_DISTANCE, travel.getDistance());
		long id=sqliteDatabase.insert(TravelEntry.TABLE_NAME, null, contentValues);
		travel.setId(id);
		LogUtils.i(TAG, "insertTravel"+id);
		return id;
	}

	public void updateTravel(Travel travel) {
		SQLiteDatabase sqliteDatabase = getWritableDatabase();
		ContentValues contentValues = new ContentValues();
		contentValues.put(TravelEntry.COLUMN_STARTTIME, travel.formatTime(travel.getStartTime()));
		contentValues.put(TravelEntry.COLUMN_ENDTIME, travel.formatTime(travel.getEndTime()));
		contentValues.put(TravelEntry.COLUMN_SPENDTIME, travel.getSpendTime());
		contentValues.put(TravelEntry.COLUMN_DISTANCE, travel.getDistance());
		int row=sqliteDatabase.update(TravelEntry.TABLE_NAME, contentValues, TravelEntry._ID + "=?",new String[]{travel.getId()+""});
		LogUtils.i(TAG, "updateTravel"+row);
	}


	public List<Travel> listTravel() {
		SQLiteDatabase sqliteDatabase = getReadableDatabase();
		Cursor result = sqliteDatabase.rawQuery("SELECT * FROM " + TravelEntry.TABLE_NAME , null);
		List<Travel> list = new ArrayList<Travel>();
		while(result.moveToNext()){
			Travel travel = new Travel();
			travel.setId(result.getInt(result.getColumnIndex(TravelEntry._ID)));
			travel.setStartTime(travel.parseTime(result.getString(result.getColumnIndex(TravelEntry.COLUMN_STARTTIME))));
			travel.setEndTime(travel.parseTime(result.getString(result.getColumnIndex(TravelEntry.COLUMN_ENDTIME))));
			travel.setSpendTime(result.getLong(result.getColumnIndex(TravelEntry.COLUMN_SPENDTIME)));
			travel.setDistance(result.getFloat(result.getColumnIndex(TravelEntry.COLUMN_DISTANCE)));
			list.add(travel);
		}
		LogUtils.i(TAG, "listTravel"+list.size());
		result.close();
		return list;
	}
	
	public Travel findTravelById(long travelId) {
		SQLiteDatabase sqliteDatabase = getReadableDatabase();
		Cursor result = sqliteDatabase.rawQuery("SELECT * FROM " + TravelEntry.TABLE_NAME +" WHERE "+TravelEntry._ID+"=?", new String[]{travelId+""});
		Travel travel=new Travel();
		while(result.moveToNext()){
			travel.setId(result.getInt(result.getColumnIndex(TravelEntry._ID)));
			travel.setStartTime(travel.parseTime(result.getString(result.getColumnIndex(TravelEntry.COLUMN_STARTTIME))));
			travel.setEndTime(travel.parseTime(result.getString(result.getColumnIndex(TravelEntry.COLUMN_ENDTIME))));
			travel.setSpendTime(result.getLong(result.getColumnIndex(TravelEntry.COLUMN_SPENDTIME)));
			travel.setDistance(result.getFloat(result.getColumnIndex(TravelEntry.COLUMN_DISTANCE)));
			break;
		}
		LogUtils.i(TAG, "findTravelById"+travelId);
		result.close();
		return travel;
	}
	
	public void deleteTravel(long travelId) {
		SQLiteDatabase sqlDb = getWritableDatabase();
		deleteTravelLocationByTravelId(travelId);
		int row=sqlDb.delete(TravelEntry.TABLE_NAME, TravelEntry._ID + " = '" + travelId + "'", null);
		LogUtils.i(TAG, "deleteTravel"+row);
	}
	
	//位置
	public long insertTravelLocation(TravelLocation travelLocation) {
		SQLiteDatabase sqliteDatabase = getWritableDatabase();
		ContentValues contentValues = new ContentValues();
		contentValues.put(TravelLocationEntry.COLUMN_TRAVEL_ID, travelLocation.getTravelId());
		contentValues.put(TravelLocationEntry.COLUMN_ISPAUSE, travelLocation.isPause()?1:0);
		contentValues.put(TravelLocationEntry.COLUMN_DESCRIPTION,travelLocation.getDescription());
		contentValues.put(TravelLocationEntry.COLUMN_LOCATION, travelLocation.getLocation()==null?"":gson.toJson(travelLocation.getLocation()));
		long id=sqliteDatabase.insert(TravelLocationEntry.TABLE_NAME, null, contentValues);
		travelLocation.setId(id);
		LogUtils.i(TAG, "insertTravelLocation"+id);
		return id;
	}

	public void updateTravelLocation(TravelLocation travelLocationEntry) {
		SQLiteDatabase sqliteDatabase = getWritableDatabase();
		ContentValues contentValues = new ContentValues();
		contentValues.put(TravelLocationEntry.COLUMN_TRAVEL_ID, travelLocationEntry.getTravelId());
		contentValues.put(TravelLocationEntry.COLUMN_ISPAUSE, travelLocationEntry.isPause()?1:0);
		contentValues.put(TravelLocationEntry.COLUMN_DESCRIPTION,travelLocationEntry.getDescription());
		contentValues.put(TravelLocationEntry.COLUMN_LOCATION, travelLocationEntry.getLocation()==null?"":gson.toJson(travelLocationEntry.getLocation()));
		int row=sqliteDatabase.update(TravelLocationEntry.TABLE_NAME, contentValues, TravelLocationEntry._ID + "=?",new String[]{travelLocationEntry.getId()+""});
		LogUtils.i(TAG, "updateTravelLocation"+row);
	}
	public List<TravelLocation> listTravelLocation (long travelId) {
		SQLiteDatabase sqliteDatabase = getReadableDatabase();
		Cursor result = sqliteDatabase.rawQuery("SELECT * FROM " + TravelLocationEntry.TABLE_NAME +" WHERE "+TravelLocationEntry.COLUMN_TRAVEL_ID+"=?", new String[]{travelId+""});
		List<TravelLocation> list = new ArrayList<TravelLocation>();
		while(result.moveToNext()){
			String locationJson=result.getString(result.getColumnIndex(TravelLocationEntry.COLUMN_LOCATION));
			Location l=gson.fromJson(locationJson, Location.class);
			TravelLocation travel = new TravelLocation(l);
			travel.setId(result.getInt(result.getColumnIndex(TravelLocationEntry._ID)));
			travel.setTravelId(result.getInt(result.getColumnIndex(TravelLocationEntry.COLUMN_TRAVEL_ID)));
			travel.setPause(result.getInt(result.getColumnIndex(TravelLocationEntry.COLUMN_ISPAUSE))==1?true:false);
			travel.setDescription(result.getString(result.getColumnIndex(TravelLocationEntry.COLUMN_DESCRIPTION)));
			list.add(travel);
		}
		LogUtils.i(TAG, "listTravelLocation"+list.size());
		result.close();
		return list;
	}
	
	public void deleteTravelLocationByTravelId (long travelId) {
		SQLiteDatabase sqlDb = getWritableDatabase();
		int row=sqlDb.delete(TravelLocationEntry.TABLE_NAME, TravelLocationEntry.COLUMN_TRAVEL_ID + " = '" + travelId + "'", null);
		LogUtils.i(TAG, "deleteTravelLocationByTravelId"+row);
	}
	
	public void deleteTravelLocationById (long travelLocationId) {
		SQLiteDatabase sqlDb = getWritableDatabase();
		int row=sqlDb.delete(TravelLocationEntry.TABLE_NAME, TravelLocationEntry._ID + " = '" + travelLocationId + "'", null);
		LogUtils.i(TAG, "deleteTravelLocationById"+row);
	}

}
