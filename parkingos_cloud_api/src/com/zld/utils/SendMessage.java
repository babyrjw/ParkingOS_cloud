package com.zld.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SendMessage {

	public  Integer sendMessageToCarOwer(String mobile){
		Integer code = new Random(System.currentTimeMillis()).nextInt(10000);
		//String time ="";// TimeTools.gettime().substring(11,16);
		if(code<99)
			code=code*100;
		if(code<999)
			code =code*10;
		String message = "�����ε���֤��:"+code;
		String result ="";
		try {
			//result = MessageUtils.sendMsg(mobile, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//String result = sendMessage(mobile, "�����ε���֤��:"+code+" "+time+"��ͣ������");//sendMsg_ManDao_Http(mobile,code);
		//System.out.println("======�ֻ��ţ�"+mobile+"=========���ŷ��ͽ��"+result);
		
		//if(result!=null&&result.indexOf("return=\"0\"")!=-1)
		return code;
		//return null;
	}
	
	/**
	 * ���Ϳ�Ƭ���Ѷ���֪ͨ
	 * @param mobile
	 * @param message
	 * @return
	 */
	public static String sendCardMessageToCarOwer(String mobile,String message){
		String result ="";
		try {
			result = MessageUtils.sendMsg(mobile, message);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(result!=null&&result.indexOf("return=\"0\"")!=-1)
			return "1";
		return "0";
	}
	
	public  Integer getCode(){
		Integer code = new Random(System.currentTimeMillis()).nextInt(10000);
		if(code<99)
			code=code*100;
		if(code<999)
			code =code*10;
		return code;
	}
	
	public static  String sendMessage(String mobile,String code){
		//http://sdk.entinfo.cn:8060/webservice.asmx/SendSMS?sn=WEB-XJG-010-00051&pwd=791445&mobile=15801482643&content=���ԡ�ͣ������
		//String url ="http://211.147.224.154/cgi-bin/sendsms";//
		//D4D714306327249005D7C3DE885A1051
		String url ="http://sdk.entinfo.cn:8060/webservice.asmx/SendSMS";//
		//?username=tq&password=123456&msgtype=1&to="+mobile+"&text="+code;
		String result = null;
		url +="?sn=WEB-XJG-010-00051&pwd=791445&mobile="+mobile;
		//url +="?username=tq&password=123456&msgtype=1&to="+mobile;
		try {
			url +="&content="+URLEncoder.encode(code,"gb2312");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
		result = "";//new HttpProxy().doGet(url);
		/*Map<String, String> params = new HashMap<String, String>();
		params.put("username", "tq");
		params.put("password", "123456");
		params.put("msgtype", "1");
		params.put("to", mobile);
		try {
			params.put("text",URLEncoder.encode(code+"","gb2312"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		result = new HttpProxy().doPost(url, params);*/
		//return doGet(url);
		System.err.println("���Ͷ��ţ����ݣ�"+code+",�����ˣ�"+mobile+",���:"+result);
		if(result!=null&&result.indexOf("�ɹ�")!=-1)
			return "0";
		return "-1";
	}
	//Ⱥ������
	public  static String sendMultiMessage(String mobiles,String message){
		//http://sdk.entinfo.cn:8060/webservice.asmx/SendSMS?sn=WEB-XJG-010-00051&pwd=791445&mobile=15801482643&content=���ԡ�ͣ������
		//String url ="http://211.147.224.154:18013/cgi-bin/sendsms";//
		//String url ="http://211.147.239.62:9088/cgi-bin/sendsms";//
		//String url="http://www.oa-sms.com/sendSms.action";
		//String url ="http://sdk.entinfo.cn:8060/webservice.asmx/mt";//
		String url ="http://sdk2.entinfo.cn:8061/mdsmssend.ashx";//
		
		String sn = "WEB-CSL-010-00199";
		String ps = "285776";
		String result = null;
		String content = message;
		String password = "";
		try {
			//content =URLEncoder.encode(message,"gb2312");
			content =URLEncoder.encode(content,"utf-8");
			password = StringUtils.MD5(sn+ps).toUpperCase();
		} catch (Exception e) {
			// TODO: handle exception
		}
		Map<String, String> params = new HashMap<String, String>();
		params.put("sn", sn);
		params.put("pwd", password);
		params.put("mobile", mobiles);
		params.put("Content", content);
		params.put("Ext", "");
		params.put("stime", "");
		params.put("Rrid", "");
		try {
			result = "";//new HttpProxy().doPost(url, params);
			//result = send(param, url, "utf-8",  "utf-8");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		result = new HttpProxy().doPost(url, params);
		//System.out.println(">>>>>>>>>>>>>>>Ⱥ�������ˣ�"+mobiles);
		//System.out.println(">>>>>>>>>>>>>>>Ⱥ�����Ž����"+result);
		System.err.println(">>>>>>>>>>>>>>>���Ͷ��ţ����ݣ�"+message+",�����ˣ�"+mobiles+",���:"+result+",����:"+url);
		//if(result.equals("0"))
		return "1";
		//return "-1";
	}
	
	public  String send(String params, String strurl,String encode,String decode) throws Exception {
		// TODO Auto-generated method stub
		HttpURLConnection url_con = null;
		String responseContent = null;
		URL url = new URL(strurl);
		url_con = (HttpURLConnection) url.openConnection();
		url_con.setRequestMethod("POST");
		url_con.setConnectTimeout(10000);//
		url_con.setReadTimeout(10000);//
		url_con.setDoOutput(true);
		byte[] b = params.toString().getBytes(encode);
		url_con.getOutputStream().write(b, 0, b.length);
		url_con.getOutputStream().flush();
		url_con.getOutputStream().close();
		int rescode = url_con.getResponseCode();
		responseContent = rescode+"";
		InputStream in = null;
		try {
			in = url_con.getInputStream();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			in=url_con.getErrorStream();
		}
		BufferedReader rd = new BufferedReader(new InputStreamReader(in,decode));
		String tempLine = rd.readLine();
		StringBuffer tempStr = new StringBuffer();
		String crlf=System.getProperty("line.separator");
		while (tempLine != null)
		{
			tempStr.append(tempLine);
			tempStr.append(crlf);
			tempLine = rd.readLine();
		}
		responseContent = tempStr.toString();
		rd.close();
		in.close();
		// Thread.sleep(5000);
		return responseContent;
	}
	public static void main(String[] args) {
		String mesg = "������ͣ����֧��ͣ���ѣ�ʡ��ʡ������ʡ��Ǯ�������ڡ��ҵ��˻����鿴����ͣ��ȯ��ͣ���������������һվ�����ص�ַ��www.tingchebao.com ��ͣ������ ";
		sendMessage("15801482643", mesg);
		//		Integer code = new Random(System.currentTimeMillis()).nextInt(10000);
//		String time = TimeTools.gettime().substring(11,16);
//		if(code<99)
//			code=code*100;
//		if(code<999)
//			code =code*10;
//		System.out.println(code);
//		String mesg ="�����ֻ��ţ�1364 121 2226���ѱ���ص������˲�������Ϣ�����ǽ�������أ�������в�����Ϣ�����ǽ�����ͣ�������ƽ�˾�����Ž���ȡ֤���飬" +
//				"�ɴ��������ĺ�������������е������й���Ϣ��ء�";
//		String result =sendMessage("13641212226",mesg);
//		System.out.println(result);

//

//		String message ="�𾴵ĺ���������ã�����(�ֻ���18518132466)��ͨ��ͣ��������󳵳����·���10���£�����3990.00Ԫ���������ں�̨�鿴��Ӧ��Ϣ��"+
//						"������ƾ����ǰ����ȡ�¿����뱸����Ӧ�¿���лл���ͷ���01053618108 ��ͣ������";
		//System.err.println(sendMessage("13718451896",message));
//		System.out.println(sendMessage("15801482643",message));
//		String message ="��ͣ����������һ��12��22�գ��𣬶��ڲ�֧���ֻ�֧����ͣ�������������ۼƻ��ַ��Ž�Ʒ��������֧��֧����ÿ�ʽ��׽�����Ԫ����ӭ��ϵ010-56450585Ǣ̸֧����";
//		String mobiles = ZldTest.anlysisPhoneLocal();
//		System.out.println(sendMultiMessage(mobiles,message));
	}
	
/*	public static String sendMsg_ManDao_Http (String mobile,Integer code){
		String result="";
		String strurl="http://sdk.entinfo.cn:8060/z_mdsmssend.aspx";
		String user= "SDK-BBX-010-14147";
		String pass = "F475168E9BC3D3CB275BF8E684CD680A";
//		String strurl="http://sdk2.entinfo.cn/z_send.aspx";
//		String user= "SDK-BBX-010-14147";
//		String pass = "332100";
//		String user= "SDK-BBX-010-14146";
//		String pass = "371571";
//		String user= "SDK-BBX-010-19035";
//		String pass = "c(32adb(";
		
		String params = "sn="+user+"&pwd="+pass+"&mobile="+mobile+"&content=��֤�룺"+code+","+TimeTools.gettime()+"�������硿";
		try {
			result = submit(params,strurl, "GBK","GBK");
			result=result.trim();
		} catch (Exception e) {
			e.printStackTrace();
			result = "-7";//�쳣
		}
		return result;
	}
	private static String submit(String params, String strurl,String encode,String decode) throws Exception {
		// TODO Auto-generated method stub
		HttpURLConnection url_con = null;
		String responseContent = null;
		URL url = new URL(strurl);
		url_con = (HttpURLConnection) url.openConnection();
		url_con.setRequestMethod("POST");
		url_con.setConnectTimeout(10000);//
		url_con.setReadTimeout(10000);//
		url_con.setDoOutput(true);
		byte[] b = params.toString().getBytes(encode);
		url_con.getOutputStream().write(b, 0, b.length);
		url_con.getOutputStream().flush();
		url_con.getOutputStream().close();
		int rescode = url_con.getResponseCode();
		responseContent = rescode+"";
		InputStream in = null;
		try {
			in = url_con.getInputStream();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			in=url_con.getErrorStream();
		}
		BufferedReader rd = new BufferedReader(new InputStreamReader(in,decode));
		String tempLine = rd.readLine();
		StringBuffer tempStr = new StringBuffer();
		String crlf=System.getProperty("line.separator");
		while (tempLine != null)
		{
			tempStr.append(tempLine);
			tempStr.append(crlf);
			tempLine = rd.readLine();
		}
		responseContent = tempStr.toString();
		rd.close();
		in.close();
		// Thread.sleep(5000);
		return responseContent;
	}
	
	*/
}
