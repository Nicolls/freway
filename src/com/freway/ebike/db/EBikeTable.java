package com.freway.ebike.db;

import android.provider.BaseColumns;

public final class EBikeTable {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public EBikeTable() {}

   	/**位置表*/
    public static abstract class TravelLocationEntry implements BaseColumns {
        public static final String TABLE_NAME = "travel_location";
        public static final String COLUMN_TRAVEL_ID = "travelId";
        public static final String COLUMN_ISPAUSE = "isPause";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_LOCATION = "location";
    }
    
 	/**蓝牙数据表*/
    public static abstract class TravelBluetoothEntry implements BaseColumns {
        public static final String TABLE_NAME = "travel_bluetooth";
        public static final String COLUMN_TRAVEL_ID = "travelId";
    }
    
    /**行程表*/
    public static abstract class TravelEntry implements BaseColumns {
        public static final String TABLE_NAME = "travel";
        public static final String COLUMN_STARTTIME = "startTime";
        public static final String COLUMN_ENDTIME = "endTime";
        public static final String COLUMN_SPENDTIME = "spendTime";
        public static final String COLUMN_DISTANCE= "distance";
    }
}