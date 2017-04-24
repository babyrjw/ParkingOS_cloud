package com.zld.pcloud_xunlang.codec;

import java.util.ArrayList;
import java.util.List;
import java.util.TooManyListenersException;

import org.apache.commons.codec.binary.Hex;

import com.zld.pcloud_xunlang.Constants;
import com.zld.pcloud_xunlang.pojo.SensorCar;
import com.zld.pcloud_xunlang.pojo.XunLangAttach;
import com.zld.pcloud_xunlang.pojo.XunLangAttach.DiciAttach;
import com.zld.pcloud_xunlang.pojo.XunLangAttach.TranAttach;
import com.zld.pcloud_xunlang.pojo.XunLangAttaches;
import com.zld.pcloud_xunlang.pojo.SensorCar.Dici;
import com.zld.pcloud_xunlang.pojo.SensorCar.HeartBeat;
import com.zld.pcloud_xunlang.pojo.XunLangBase;
import com.zld.pcloud_xunlang.pojo.XunLangConfirm;
import com.zld.pcloud_xunlang.util.Utils;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

public class XunLangPOJODecoder extends MessageToMessageDecoder<byte[]>{

	@Override
	protected void decode(ChannelHandlerContext ctx, byte[] data, List<Object> list)
			throws Exception {
		// TODO Auto-generated method stub
		String nid = bytesToHexString(data, 0, 4);
		byte command = data[4];
		if(command == Constants.UPLOAD_DICI){
			SensorCar sc = new SensorCar();
			sc.setNid(nid);
			sc.setCommand(command);
			//停车数据
			//年份，1字节
			int year = 2000 + Utils.unsignByteToInt(data[5]);
			//月份，1字节
			int month = Utils.unsignByteToInt(data[6]);
			//车检器个数，1字节
			int count = Utils.unsignByteToInt(data[7]);
			sc.setYear(year);
			sc.setMonth(month);
			List<Dici> diciList = new ArrayList<Dici>();
			sc.setCars(diciList);
			for(int i = 0 ; i < count ; ++i){
				Dici dici = new Dici();
				diciList.add(dici);
				byte status = (byte)data[8 + i*12];
				if( (status & 0x80) != 0){
					//车辆到达
					dici.setStatus(1);
				}else{
					//车辆离开
					dici.setStatus(0);
				}
				
				int flag = 0;
				if( (status & 0x40) != 0){
					//十分钟一次传输磁场数据
					flag = 1;
				}else{
					//车辆进出场或者心跳
					flag = 0;
				}
				dici.setFlag(flag);
				if(0 == flag){
					//车检器节点ID
					int number = status & (byte)0x3F;
					
					int day = (data[9+i*12] >> 2) & 0x3F;
					int hour = ((data[9+i*12] << 4) & 0x30) |
							((data[10+i*12] >> 4) & 0x0F);
					int minite = ((data[10+i*12] << 2) & 0x3C) |
							((data[11 + i*12] >> 6) & 0x03);
					int second = (data[11 + i*12] & 0x3F);
					
					String label = bytesToHexString(data,12 + i*12,4);
					
					String serial = bytesToHexString(data, 16+i*12, 1);
					String lastSerial = bytesToHexString(data, 17+i*12, 1);
					String retry = bytesToHexString(data, 18+i*12, 1);
					String matchId = bytesToHexString(data, 19+i*12, 1);
					
					dici.setDiciId(number);
					dici.setDay(day);
					dici.setHour(hour);
					dici.setMinite(minite);
					dici.setSecond(second);
					dici.setLabel(label);
					dici.setSerial(serial);
					dici.setLastSerial(lastSerial);
					dici.setRetry(retry);
					dici.setMatchId(matchId);
				}else{
					int number = status & (byte)0x3F;
					//int mx = (data[9+i*12] )
					dici.setDiciId(number);
				}
			}
			//心跳数据长度位置
			int index = 8 + count*12;
			int countHeart = data[index];
			index ++;
			List<HeartBeat> heartBeatList = new ArrayList<HeartBeat>();
			sc.setHeartBeats(heartBeatList);
			for(int i = 0 ; i < countHeart ; ++i){
				HeartBeat hb = new HeartBeat();
				int berth = (data[index + i] >> 7) & 0x01;
				int number = (data[index + i] & 0x3F);
				hb.setDiciId(number);
				hb.setStatus(berth);
				heartBeatList.add(hb);
			}
			list.add(sc);
			System.out.print("传输器地磁数据:"+sc);
			ctx.writeAndFlush(XunLangConfirm.getOkConfirm(sc));
		}else if(command == (byte)0xA0){
			//传输器启动
			String mac = bytesToHexString(data, 5, 4);
			System.out.print("传输器启动数据:"+mac);
			
			XunLangBase base = new XunLangBase();
			base.setNid(nid);
			base.setCommand((byte)0xA0);
			ctx.writeAndFlush(XunLangConfirm.getOkConfirm(base));
			
		}else if(command == Constants.UPLOAD_TIME){
			//时间请求数据
			XunLangBase base = new XunLangBase();
			base.setNid(nid);
			base.setCommand(Constants.UPLOAD_TIME);
			list.add(base);
			System.out.print("传输器时间请求数据:"+base);
		}else if(command == (byte)0xA1){
			//传输器确认数据
			boolean isConfirm = false;
			if(data[5] == (byte)0x00){
				isConfirm = true;
			}else if(data[5] == (byte)0x01){
				isConfirm = false;
			}
			System.out.print("传输器确认数据："+isConfirm);
		}else if(command == Constants.UPLOAD_ATTACH){
			int count = Utils.unsignByteToInt(data[5]);
			List<XunLangAttach> attached = new ArrayList<XunLangAttach>();
			for(int i = 0 ; i < count ; ++i){
				int type = data[6 + i*count];
				XunLangAttach attach = new XunLangAttach();
				if(type == Constants.ATTACH_TYPE_DICI){
					int size = Utils.unsignByteToInt(data[7 + i*count]);
					List<DiciAttach> diciAttachList = new ArrayList<DiciAttach>();
					for(int j = 0 ; j  < size;  ++j){
						DiciAttach diciAttach = new DiciAttach();
						byte byteDiciId = data[7 + i*count + j * 3 + 1];
						int diciId = byteDiciId & 0x3F;
						int status = byteDiciId >> 7 & 0x01;
						//int diciId = Utils.unsignByteToInt(data[7 + i*count + j * 3 + 1]);
						int voltageBattery = Utils.unsignByteToInt(data[7 + i*count + j * 3 + 2]);
						int voltageCapacity = Utils.unsignByteToInt(data[7 + i*count + j * 3 + 3]);
						diciAttach.setDiciId(diciId);
						diciAttach.setStatus(status);
						diciAttach.setType(Constants.ATTACH_TYPE_DICI);
						diciAttach.setVoltageBattery(voltageBattery);
						diciAttach.setVoltageCapacity(voltageCapacity);
						diciAttachList.add(diciAttach);
					}
					attach.setAttaches(diciAttachList);
				}else if(type == Constants.ATTACH_TYPE_TRANS){
					int size = data[7 + i*count];
					List<TranAttach> diciAttachList = new ArrayList<TranAttach>();
					for(int j = 0 ; j  < size;  ++j){
						TranAttach tranAttach = new TranAttach();
						int voltage = Utils.unsignByteToInt(data[7 + i*count + j * 2 + 1]) * 0x100
								+ Utils.unsignByteToInt(data[7 + i*count + j * 2 + 2]);
						tranAttach.setVoltage(voltage);
						tranAttach.setType(Constants.ATTACH_TYPE_TRANS);
						diciAttachList.add(tranAttach);
					}
					attach.setAttaches(diciAttachList);
				}
				attached.add(attach);
			}
			XunLangAttaches attaches = new XunLangAttaches();
			attaches.setCommand(Constants.UPLOAD_ATTACH);
			attaches.setNid(nid);
			attaches.setAttaches(attached);
			list.add(attaches);
			System.out.print("传输器附加数据："+attached);
			ctx.writeAndFlush(XunLangConfirm.getOkConfirm(attaches));
		}else{
			XunLangBase base = new XunLangBase();
			base.setNid(nid);
			base.setCommand((byte)0xA0);
			ctx.writeAndFlush(XunLangConfirm.getOkConfirm(base));
		}
	}
	
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
}
