package com.zld.pcloud_xunlang.codec;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.zld.pcloud_xunlang.pojo.XunLangBase;
import com.zld.pcloud_xunlang.pojo.XunLangConfirm;
import com.zld.pcloud_xunlang.pojo.XunLangTimeCommand;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

public class XunLangPOJOEncoder extends MessageToMessageEncoder<XunLangBase>{

	@Override
	protected void encode(ChannelHandlerContext paramChannelHandlerContext, XunLangBase data, List<Object> list)
			throws Exception {
		// TODO Auto-generated method stub
		List<Byte> next = new ArrayList<Byte>();
		byte[] nids = hexStringToBytes(null, 0, data.getNid());
		for(int i = 0 ; i < nids.length; ++i){
			next.add(nids[i]);
		}
		//数据类型数量
		next.add((byte)0x01);
		switch(data.getCommand()){
		case XunLangConfirm.COMMAND:
			//确认应答
			XunLangConfirm c = (XunLangConfirm)data;
			next.add(XunLangConfirm.COMMAND);
			next.add((byte)0x01);
			next.add((byte)c.getData());
			break;
		case XunLangTimeCommand.COMMAND:
			//后台控制命令
			XunLangTimeCommand command = (XunLangTimeCommand)data;
			next.add(XunLangTimeCommand.COMMAND);
			next.add((byte)0x0B); //数据包长度
			next.add((byte)0x01); //后台命令数量
			next.add((byte)0x09); //命令长度
			next.add((byte)0xF5); //命令字符
			next.add((byte)0x00); //命令对象
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(command.getDate());
			int year = calendar.get(Calendar.YEAR);
			next.add((byte)(( year >> 8) &0xFF));
			next.add((byte)(year & 0xFF));
			int month = calendar.get(Calendar.MONTH);
			next.add((byte)(month + 1));
			int day = calendar.get(Calendar.DAY_OF_MONTH);
			next.add((byte)day);
			int hour = calendar.get(Calendar.HOUR_OF_DAY);
			next.add((byte)hour);
			int minite = calendar.get(Calendar.MINUTE);
			next.add((byte)minite);
			int second = calendar.get(Calendar.SECOND);
			next.add((byte)second);
		}
		byte[] result = new byte[next.size()];
		for(int i = 0 ; i < next.size() ; ++i){
			result[i] = next.get(i);
		}
		list.add(result);
	}
	
	public byte[] hexStringToBytes(byte[] d, int start, String hexString) {  
	    if (hexString == null || hexString.equals("")) {  
	        return null;  
	    }  
	    hexString = hexString.toUpperCase();  
	    int length = hexString.length() / 2;  
	    char[] hexChars = hexString.toCharArray();  
	    if(d == null){
	        d = new byte[length];
	        start = 0;
	    }
	    for (int i = 0; i < length; i++) {  
	        int pos = start + i * 2;  
	        d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));  
	    }  
	    return d;  
	}  
	 private byte charToByte(char c) {  
		    return (byte) "0123456789ABCDEF".indexOf(c);  
		}  
}
