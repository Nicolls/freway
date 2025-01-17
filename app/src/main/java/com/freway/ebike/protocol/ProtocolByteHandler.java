package com.freway.ebike.protocol;
import java.util.HashMap;

import android.content.Context;

import com.freway.ebike.bluetooth.EBikeTravelData;
import com.freway.ebike.utils.LogUtils;

/**
 * @author Nicolls
 * @Description 协议数据操作类，包括装包，解包
 * @date 2015年10月25日
 */
public class ProtocolByteHandler {
	
	private static final String TAG="CreateByteCommand";
	
	
	public static byte[]packData;//mark 加这个是为了给蓝牙工程师调试用的，显示数据，发布版本的时候，应该把它去掉
	/**
	 * @Fields EXTRA_CODE 解析数据包时，装入HashMap中的结果码
	 */
	public static final String EXTRA_CODE="EXTRA_CODE";
	/**
	 * @Fields EXTRA_CMD 解析数据包时，装入HashMap中的命令Key
	 */
	public static final String EXTRA_CMD="EXTRA_CMD";
	
	/**
	 * @Fields EXTRA_DATA 解析数据包时，装入HashMap中的数据值byte[]
	 */
	public static final String EXTRA_DATA="EXTRA_DATA";
	/**
	 * @param cmdCode 操作码 @see com.freway.protocol.CommandCode
	 * @param data 命令发出的数据 ，如果不需要传递参数，则设置为空
	 * @return byte[]
	 * @Description 通过命令枚举，和参数，构造要发送的数据
	 */
	public static byte[] command(int cmdCode,byte[] data) {
		String sendstr = "";
		if(data!=null){
			sendstr=new String(data);
		}
		switch(cmdCode)
		{
		case CommandCode.SURVEY:
			
			break;
		}
		LogUtils.d(TAG,"发送的数据: " + sendstr);
		Protocol mProtocol = new Protocol();
		return mProtocol.pack(cmdCode,data);
	}
	
	/** 对接收到的数据进行解析
	 * @param receiveData 接收到的数据
	 * @return HashMap<String,Object> 
	 * @Description 返回的key有:
	 * 操作码ProtocolByteHandler.EXTRA_CMD
	 * 对应的值ProtocolByteHandler.EXTRA_DATA 数据块的值@see com.freway.ebike.bluetooth.EBikeData
	 * 操作码如果为CommandCode.ERROR则表示出错，出错信息在对应的值中
	 * 
	 * */
	public static HashMap<String,Object> parseData(Context context,byte[]receiveData) {
		packData=receiveData;
		HashMap<String,Object> map=null;
		Protocol mProtocol = new Protocol();
		if(mProtocol.parseBytes(receiveData)){//如果这个数据包是正确的才去操作，不正确就丢掉了
			map=new HashMap<String, Object>();
			int cmd=ProtocolTool.byteArrayToInt(mProtocol.getCommandCode());
			map.put(EXTRA_CMD, cmd);
			map.put(EXTRA_CODE, mProtocol.getResultCode());
			if(mProtocol.getResultCode()!=Protocol.RESULT_OK){//有错误
				map.put(EXTRA_DATA, mProtocol.getResultMessage());
			}else{
				byte[]data=mProtocol.getParamData();
				if(CommandCode.SURVEY_RESULT==cmd){//当前行程数据
					EBikeTravelData.getInstance(context).parseBikeData(data);
					map.put(EXTRA_DATA, null);
				}else if(CommandCode.HISTORY==cmd){//历史数据
					EBikeTravelData.getInstance(context).parseHistoryData(data);
				}
			}
		}
		return map;
	}
}
