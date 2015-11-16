/**
 * 
 */
package com.freway.ebike.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.telephony.TelephonyManager;

/**
 * APP通用工具类
 * 
 * @author Nicolls
 * 
 *         2015年5月14日
 */
public class CommonUtil {
	/**
	 * 得到imei号
	 * 
	 * @param context
	 *            上下文
	 * */
	public static String getPhoneImei(Context context) {
		TelephonyManager tm = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		return tm.getDeviceId();// String
	}

	/**
	 * 得到版本号
	 * 
	 * @param context
	 *            上下文
	 * */
	public static String getAppVersion(Context context) {
		try {
			return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			LogUtils.e(context.getClass().getName(), "找不到包名！");
			return "0.0.0";
		}
	}

	/** 格式化html数据，嵌入script代码 */
	public static String formatScriptHtml(Context context, String scriptCode, String htmlFileName) {
		StringBuffer sb = new StringBuffer();
		InputStream is = null;
		try {
			is = context.getAssets().open(htmlFileName);
			int len = 0;
			byte[] buf = new byte[512];
			while ((len = is.read(buf, 0, buf.length)) != -1) {
				sb.append(new String(buf, 0, len, "UTF-8"));
			}
		} catch (IOException e) {
			LogUtils.e("CommonUtil", "读取assets目录下的html文件出现错误：" + e.getMessage());
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return scriptCode + "\n" + sb.toString();
	}
	
	// 判断是否为手机号
    public static boolean isPhone(String inputText) {
        Pattern p = Pattern
                .compile("^((13[0-9])|(15[^4,\\D])|(18[0,5-9]))\\d{8}$");
        Matcher m = p.matcher(inputText);
        return m.matches();
    }

    // 判断格式是否为email
    public static boolean isEmail(String email) {
        String str = "^([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)$";
        Pattern p = Pattern.compile(str);
        Matcher m = p.matcher(email);
        return m.matches();
    }

}
