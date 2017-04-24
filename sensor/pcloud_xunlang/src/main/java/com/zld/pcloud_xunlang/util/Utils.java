package com.zld.pcloud_xunlang.util;

public class Utils {
	public static String bytesToHexString(byte[] src, int start, int end){  
	    StringBuilder stringBuilder = new StringBuilder("");  
	    if (src == null || src.length <= 0 || start + end > src.length) {  
	        return null;  
	    }  
	    for (int i = start; i < start + end; i++) {  
	        int v = src[i] & 0xFF;  
	        String hv = Integer.toHexString(v);  
	        if (hv.length() < 2) {  
	            stringBuilder.append(0);  
	        }  
	        stringBuilder.append(hv);  
	    }  
	    return stringBuilder.toString();  
	}  
	public static int unsignByteToInt(byte b){
		return ((int)b)&0xFF;
	}
}
