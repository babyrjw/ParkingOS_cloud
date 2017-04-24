package com.zld.pcloud_xunlang;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class Constants {
	
	public static final byte UPLOAD_DICI = (byte)0xA8;
	public static final byte UPLOAD_TIME = (byte)0xF5;
	public static final byte UPLOAD_ATTACH = (byte)0xA9;
	
	public static final byte ATTACH_TYPE_DICI = (byte)0x6;
	public static final byte ATTACH_TYPE_TRANS = (byte)0xA2;
	
	public static String SNESOR_SIGN = "PCLOUD-SENSOR-SERVER";//xmemcache加一个头部标识，防止重复
	
	public static String TB_CLIENT = "TB_CLIENT";//天泊通信通道标识
	
	public static String TB_ADDR = "139.196.229.89";//天泊服务地址
	
	public static int TB_PORT = 6001;//天泊服务端口
	
	public static int SENSOR_PORT = 12300;//车检器中间件服务地址
	
	//public static String DOMAIN = "180.150.188.224:8080";
	
	public static String DOMAIN = "123.56.195.194:4001";
	
	public static String SITE_HEART_URL = "http://" + DOMAIN + "/zld/api/hdinfo/InsertTransmitter";//基站心跳接口
	
	public static String SENSOR_HEART_URL = "http://" + DOMAIN + "/zld/api/hdinfo/InsertSensor";//车检器心跳接口
	
	public static String SENSOR_IN_CAR = "http://" + DOMAIN + "/zld/api/hdinfo/InsertCarAdmission";//车检器监测到有车接口
	
	public static String SENSOR_OUT_CAR = "http://" + DOMAIN + "/zld/api/hdinfo/InsertCarEntrance";//车检器监测无车接口
	
	public static void main(String[] args){
		try {
			//车检器复位数据
			//String str = "ffff500f0012a90104010129ff";
			String str = "ffff500f0012a90104010181ff";
			//传输器时间请求
			//String str = "ffff500f0012f500b8ff";
			//String str = "FFFF5475C56AF5007BFF";
			//车检器电压数据
			//String str = "FFFFA0080021A90106160425258925258625258824219325251425259726248325208524248725268B25258E25258C26259126269025259226269626269826269A25259B25258F252699252521FF";
			//传输器电压数据
			//String str = "FFFFA0080021A901A20105AA2DFF";
			//离车数据
			//String str = "FFFFA0080021A8100B011160A7DA00000000535201D200E5FF";
			//心跳数据
			//String str = "FFFFA0080021A8100B00178D9B99048688948A838E0C909296989A139597878B918924FF";
			//心跳数据
			//String str = "FFFFA0080021A8100B00159B99048688948A8E8C909296989A139517878B918928FF";
			byte[] bytes = Hex.decodeHex(str.toCharArray());
			Socket s = new Socket("127.0.0.1", SENSOR_PORT);
			OutputStream os = s.getOutputStream();
			while(true){
				os.write(bytes);
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DecoderException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
	}
}
