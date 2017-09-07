package com.zld.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.zld.AjaxUtil;

public class ZldTest {
	public static String anlysisPhoneLocal(){
		String filenname="c:\\mobile.txt";
		Reader reader = null;
		BufferedReader bufferedReader = null;
		List<String > reList=new ArrayList<String>();
		String lineString = "";
		String mobiles = "";
		int k = 0;
		try {
			reader = new InputStreamReader(new FileInputStream(filenname));
			bufferedReader = new BufferedReader(reader);
			while ((lineString = bufferedReader.readLine()) != null ) {
				mobiles +=","+lineString;
			}
		}catch (Exception e) {
			
		}finally{
			try {
				reader.close();
				bufferedReader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return mobiles;
		//http://vip.showji.com/locating/?m=1878952&outfmt=json&callback=phone.callBack
	}
	
	
	private  static String weixin(){
		BufferedReader reader = null;
		String lineString=null;
		String result = "";
		BufferedWriter writer;
		try {
			reader = new BufferedReader(new FileReader(new File("d:/weixin0727_0930.csv")));
			writer = new BufferedWriter(new FileWriter(new File("d:/weixin0727_0930_final.csv"),true));
			int i=0;
			while ((lineString = reader.readLine()) != null) {
				if(i==0){
					i++;
					continue;
				}
				String []info = lineString.split(",");
				Integer t1 =0;
				Integer t2 =0; 
				if(Check.isNumber(info[6])&&Check.isNumber(info[5])){
					t1=Integer.valueOf(info[5]);
					t2=Integer.valueOf(info[6]);
				}
				int index = lineString.indexOf("oRo");
				if(t2<t1){
					result += lineString.substring(0,index)+"����ȯ,"+lineString.substring(index);
				}else {
					result += lineString.substring(0,index)+"��ͨȯ,"+lineString.substring(index);
				}
				result+="\n";
				if(i%100==0){
					writer.write(result);
					//writer.flush();
					result ="";
					System.err.println(i);
				}
				i++;
			}
			reader.close();
			writer.write(result);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
				}
			}
		}
	
		//System.err.println(result);
		return result;
	}

	private  static String getMobile(){
		BufferedReader reader = null;
		String lineString=null;
		String result = "";
		BufferedWriter writer;
		try {
			reader = new BufferedReader(new FileReader(new File("d:/ctime.txt")));
			writer = new BufferedWriter(new FileWriter(new File("d:/ctime1.txt")));
			int i=0;
			while ((lineString = reader.readLine()) != null) {
				if(lineString.length()>4)
					result +=TimeTools.getTime_yyyyMMdd_HHmmss(Long.valueOf(lineString)*1000)+"\n";
				else {
					result +="\n";
				}
				i++;
				if(i%1000==0){
					System.out.println(lineString+"-->>>>"+i);
					writer.write(result);
					result="";
				}
			}
			reader.close();
			writer.write(result);
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e1) {
				}
			}
		}
	
		//System.err.println(result);
		return result;
	}
	

	/*����ʱ���*/
	private static void testPrice(Long start,Long end,Integer bHour,
			Integer bMinute,Integer eHour,Integer eMinute) {
		/*����ʱ���*/
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));//ȡ��ǰʱ�䣻
		
		calendar.setTimeInMillis(start*1000);
		Long pes =0L;
		
		if(eHour==24){
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			pes = calendar.getTimeInMillis()/1000+24*3600;//�볡������ײͽ���ʱ�䣨NTC��
		}else {
			calendar.set(Calendar.HOUR_OF_DAY, eHour);
			calendar.set(Calendar.MINUTE, eMinute);
			calendar.set(Calendar.SECOND, 0);
			pes = calendar.getTimeInMillis()/1000;//�볡������ײͽ���ʱ�䣨NTC��
		}
		
		calendar.set(Calendar.HOUR_OF_DAY, bHour);
		calendar.set(Calendar.MINUTE, bMinute);
		calendar.set(Calendar.SECOND, 0);
		Long pbs = calendar.getTimeInMillis()/1000;//�볡������ײͿ�ʼʱ�䣨NTC��
		
		List<List<Long>> out = new ArrayList<List<Long>>();//�ײ���ʱ���
		List<List<Long>> in = new ArrayList<List<Long>>(); //�ײ���ʱ���
		
		while( start<end){
			if(start<pbs){//������ʼʱ��С���ײͿ�ʼʱ��
				if(end<pbs){//��������ʱ��С���ײͿ�ʼʱ��
					List<Long> outList = new ArrayList<Long>();
					outList.add(start);
					outList.add(end);
					out.add(outList);//�ײ���ʱ������һ�Σ�������ʼʱ�䵽������ʱ�䣬����������������ѭ��
					break;
				}else {//��������ʱ����ڻ�����ײͿ�ʼʱ��
					List<Long> outList = new ArrayList<Long>();
					outList.add(start);
					outList.add(pbs);
					out.add(outList);//�ײ���ʱ������һ�Σ�������ʼʱ�䵽�ײͿ�ʼʱ��
					if(end>pes){//��������ʱ������ײͽ���ʱ��
						List<Long> inList = new ArrayList<Long>();
						inList.add(pbs);
						inList.add(pes);
						in.add(inList);//�ײ���ʱ������һ�Σ��ײͿ�ʼʱ�䵽�ײͽ���ʱ��
						start = pes;//������ʼʱ���ƶ����ײͽ���ʱ�䣬����һ��ѭ������Ҫ��������

					}else {//��������ʱ�����ײͽ���ʱ����
						List<Long> inList = new ArrayList<Long>();
						inList.add(pbs);
						inList.add(end);
						in.add(inList);//�ײ���ʱ������һ�Σ��ײͿ�ʼʱ�䵽��������ʱ�䣬 ����������������ѭ��
						break;
					}
				}
			}else {//������ʼʱ����ڻ�����ײͿ�ʼʱ��
				if(start<pes){//������ʼʱ��С���ײͽ���ʱ��
					if(end>pes){//��������ʱ��С���ײͽ���ʱ��
						List<Long> inList = new ArrayList<Long>();
						inList.add(start);
						inList.add(pes);
						in.add(inList);//�ײ���ʱ������һ�Σ�������ʼʱ�䵽�ײͽ���ʱ��
						start = pes;//������ʼʱ���ƶ����ײͽ���ʱ�䣬����һ��ѭ������Ҫ��������
					}else {
						List<Long> inList = new ArrayList<Long>();
						inList.add(start);
						inList.add(end);
						in.add(inList);//�ײ���ʱ������һ�Σ��ײͿ�ʼʱ�䵽��������ʱ�䣬 ����������������ѭ��
						break;
					}
				}else{//����ʱ����ڻ�����ײͽ���ʱ��
					pbs = pbs + 24*3600;//�ײ͵��ڶ���
					pes = pes + 24*3600;//�ײ͵��ڶ���
					if(end<pbs){//����ʱ��С�ڵڶ�����ײͿ�ʼʱ��
						List<Long> outList = new ArrayList<Long>();
						outList.add(start);
						outList.add(end);
						out.add(outList);//�ײ���ʱ������һ�Σ�������ʼʱ�䵽������ʱ��
						break;
					}else {//����ʱ����ڻ���ڵڶ�����ײͿ�ʼʱ��
						List<Long> outList = new ArrayList<Long>();
						outList.add(start);
						outList.add(pbs);
						out.add(outList);//�ײ���ʱ������һ�Σ�������ʼʱ�䵽�ڶ�����ײͿ�ʼʱ��
						start = pbs;//������ʼʱ���ƶ������ڶ�����ײͿ�ʼʱ��,����һ��ѭ������Ҫ��������
					}
				}
			}
		}
		if(!in.isEmpty()){
			System.out.println("�ײ��ڣ�");
			for(List<Long> l: in){
				System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(l.get(0)*1000)+"��"+TimeTools.getTime_yyyyMMdd_HHmmss(l.get(1)*1000));
			}
		}
		if(!out.isEmpty()){
			System.out.println("�ײ��⣺");
			for(List<Long> l: out){
				System.out.println(TimeTools.getTime_yyyyMMdd_HHmmss(l.get(0)*1000)+"-"+TimeTools.getTime_yyyyMMdd_HHmmss(l.get(1)*1000));
			}
		}
		/*����ʱ���*/
	}
	
	private static void testGetJson(){
		String str = "[{\"IsNewData\":\"0\",\"RouteID\":110022,\"RouteName\":\"1002·�ڻ�\",\"RouteType\":\"2\",\"SegmentList\":[{\"SegmentID\":100210,\"SegmentName\":\"������Է\",\"FirstTime\":\"2016-01-16 06:30:00\",\"LastTime\":\"2016-01-16 18:30:00\",\"RoutePrice\":\"0\",\"NormalTimeSpan\":0,\"PeakTimeSpan\":0,\"StationList\":[{\"StationID\":\"107552\",\"StationName\":\"������Է\",\"StationNO\":\"107552\",\"StationPostion\":{\"Longitude\":119.38729,\"Latitude\":32.40852},\"Stationmemo\":\"�ϱ�վ̨\"},{\"StationID\":\"111101\",\"StationName\":\"÷Է˫��ѧУ\",\"StationNO\":\"111101\",\"StationPostion\":{\"Longitude\":119.3854,\"Latitude\":32.41248},\"Stationmemo\":\"����վ̨\"},{\"StationID\":\"116592\",\"StationName\":\"���ù������������㻨԰����\",\"StationNO\":\"116592\",\"StationPostion\":{\"Longitude\":119.37957,\"Latitude\":32.41899},\"Stationmemo\":\"\"},{\"StationID\":\"116582\",\"StationName\":\"�����Է��\",\"StationNO\":\"116582\",\"StationPostion\":{\"Longitude\":119.38044,\"Latitude\":32.4225},\"Stationmemo\":\"\"},{\"StationID\":\"116572\",\"StationName\":\"ά����ѧ\",\"StationNO\":\"116572\",\"StationPostion\":{\"Longitude\":119.38095,\"Latitude\":32.42489},\"Stationmemo\":\"\"},{\"StationID\":\"104292\",\"StationName\":\"����ӡˢ��\",\"StationNO\":\"104292\",\"StationPostion\":{\"Longitude\":119.38587,\"Latitude\":32.42519},\"Stationmemo\":\"�ϱ�վ̨\"},{\"StationID\":\"102142\",\"StationName\":\"��ׯ��������Ժ��\",\"StationNO\":\"102142\",\"StationPostion\":{\"Longitude\":119.39063,\"Latitude\":32.42449},\"Stationmemo\":\"�ϱ�վ̨\"},{\"StationID\":\"116563\",\"StationName\":\"ɽˮ���Ƕ���\",\"StationNO\":\"116563\",\"StationPostion\":{\"Longitude\":119.39228,\"Latitude\":32.4235},\"Stationmemo\":\"\"},{\"StationID\":\"116552\",\"StationName\":\"����ӡ�󻪸�\",\"StationNO\":\"116552\",\"StationPostion\":{\"Longitude\":119.39298,\"Latitude\":32.42217},\"Stationmemo\":\"\"},{\"StationID\":\"107572\",\"StationName\":\"����������\",\"StationNO\":\"107572\",\"StationPostion\":{\"Longitude\":119.39717,\"Latitude\":32.42488},\"Stationmemo\":\"�ϱ�վ̨\"},{\"StationID\":\"107862\",\"StationName\":\"�¶�����ѧ\",\"StationNO\":\"107862\",\"StationPostion\":{\"Longitude\":119.40234,\"Latitude\":32.42593},\"Stationmemo\":\"�ϱ�վ̨\"},{\"StationID\":\"107563\",\"StationName\":\"������\",\"StationNO\":\"107563\",\"StationPostion\":{\"Longitude\":119.4051,\"Latitude\":32.42496},\"Stationmemo\":\"����վ̨\"},{\"StationID\":\"103593\",\"StationName\":\"����·\",\"StationNO\":\"103593\",\"StationPostion\":{\"Longitude\":119.40678,\"Latitude\":32.42186},\"Stationmemo\":\"����վ̨\"},{\"StationID\":\"106333\",\"StationName\":\"�������\",\"StationNO\":\"106333\",\"StationPostion\":{\"Longitude\":119.41055,\"Latitude\":32.41498},\"Stationmemo\":\"����վ̨\"},{\"StationID\":\"115284\",\"StationName\":\"����������\",\"StationNO\":\"115284\",\"StationPostion\":{\"Longitude\":119.41067,\"Latitude\":32.41152},\"Stationmemo\":\"\"},{\"StationID\":\"115274\",\"StationName\":\"����Է����\",\"StationNO\":\"115274\",\"StationPostion\":{\"Longitude\":119.40255,\"Latitude\":32.40852},\"Stationmemo\":\"\"},{\"StationID\":\"116524\",\"StationName\":\"������Է\",\"StationNO\":\"116524\",\"StationPostion\":{\"Longitude\":119.39905,\"Latitude\":32.40699},\"Stationmemo\":\"\"},{\"StationID\":\"115264\",\"StationName\":\"��ܰ��԰����\",\"StationNO\":\"115264\",\"StationPostion\":{\"Longitude\":119.39688,\"Latitude\":32.40609},\"Stationmemo\":\"\"},{\"StationID\":\"104891\",\"StationName\":\"ɽ��԰����ܰ��԰��\",\"StationNO\":\"104891\",\"StationPostion\":{\"Longitude\":119.39473,\"Latitude\":32.40603},\"Stationmemo\":\"����վ̨\"},{\"StationID\":\"107544\",\"StationName\":\"������Է\",\"StationNO\":\"107544\",\"StationPostion\":{\"Longitude\":119.39167,\"Latitude\":32.40926},\"Stationmemo\":\"����վ̨\"},{\"StationID\":\"107554\",\"StationName\":\"������Է\",\"StationNO\":\"107554\",\"StationPostion\":{\"Longitude\":119.38687,\"Latitude\":32.40858},\"Stationmemo\":\"����վ̨\"}],\"FirtLastShiftInfo\":\"��ĩ�ࣺ06:30--18:30\",\"FirtLastShiftInfo2\":null,\"Memos\":null}],\"TimeStamp\":\"2016-03-12 00:12:39\",\"RouteMemo\":null}]";
		if(str.startsWith("[")){
			JSONArray array=null;
			List<Map<String, Object>> busList = new ArrayList<Map<String,Object>>();
			try {
				array = new JSONArray(str);
				for(int i=0;i<array.length();i++){
					Map<String, Object>	 retMap = jsonObj2Map(array.getJSONObject(i));
					busList.add(retMap);
				}
				//List<Object> list = CreatePojo.createObjectList(RouteStation.class, busList);
				//System.err.println(list);
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println(busList);
		}else {
			try {
				JSONObject jobj = new JSONObject(str);
				Map<String, Object>	 retMap = jsonObj2Map(jobj);
				System.err.println(retMap);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static Map<String,Object> jsonObj2Map(JSONObject jobj){
		Map<String,Object> map = new HashMap<String, Object>();
		for (Iterator<String> iter = jobj.keys(); iter.hasNext();) { 
		       String key = (String)iter.next();
		        try {
		        	Object value = jobj.get(key);
		        	key = key.substring(0,1).toLowerCase()+key.substring(1);
					if (value instanceof JSONObject) {
						Map<String,Object> map2 = jsonObj2Map((JSONObject)value);
						map.put(key, map2);
					}else if(value instanceof JSONArray){
						JSONArray value2 = (JSONArray)value;
						List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
						for(int i=0;i<value2.length();i++){
							Map<String,Object> map3 = jsonObj2Map(value2.getJSONObject(i));
							list.add(map3);
						}
						map.put(key, list);
					}else {
						map.put(key, value);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			 }
		return map;
	}
	
	//���Ͷ���
	public static void main(String[] args) {
		String parkId = "aaaa_213132a_ee";
		String pid = parkId;
		String localId  = null;
		if(parkId.indexOf("_")!=-1){
			pid = parkId.split("_")[0];
			localId = parkId.substring(pid.length()+1);
		}
		System.err.println(localId);
	//	System.out.println(AjaxUtil.encodeUTF8("https://s.bolink.club:8443/zld/wxpfast.do?faction=sweepcom&from=&codeid=254219"));
		//System.err.println(AjaxUtil.decodeUTF8("http%3a%2f%2fyxiudongyeahnet.vicp.cc:50803%2fzld%2fwxpfast.do%3faction%3dsweepcom%26from%3dbolink%26codeid%3d33"));
//		  String inXml = "<?xml version=\"1.0\" encoding=\"GBK\"?>" +
//	        		"<ROOT><STNO1>1</STNO1><STNO2>2000</STNO2><CURRPAGE>0</CURRPAGE><PAGENO>0</PAGENO></ROOT>" ;
//	        String msg = 
//	        		"<ROOT>" +
//	        		"<ActNo>6222621410000535967</ActNo>" +
//	        		"<TeLNum>13579370660</TeLNum>" +
//	        		"<CertNo>65290119880321044X</CertNo>"+
//	        		"<CurrPage>0</CurrPage>"+
//	        		"<PageNo>20</PageNo>"+
//	        		"<BeginTime>20160301100000</BeginTime>" +
//	        		"<EndTime>20160330100000</EndTime>" +
//	        		"</ROOT>" ;
//	        	//	"<PageNo></PageNo>";
//	        String detailXmlString="<?xml version=\"1.0\" encoding=\"GBK\"?>"+
//	"<ROOT><ACTNO>6222621410000535967</ACTNO><CERTNO>13579370660</CERTNO><TELNUM>65290119880312044X</TELNUM><BEGINTIME>20160304000000</BEGINTIME><ENDTIME>20160330235959</ENDTIME><CURRPAGE>1</CURRPAGE><PAGENO>25</PAGENO></ROOT>";
//	     //   System.out.println( service.queryLeaseRecXML("<?xml version=\"1.0\" encoding=\"GBK\"?>"+msg.toUpperCase()));
//	        IYZBikeInterFaceserviceClient client = new IYZBikeInterFaceserviceClient();
//	        
//			//create a default service endpoint
//	        IYZBikeInterFace service = client.getIYZBikeInterFacePort();
//	        System.out.println( service.queryLeaseRecXML(detailXmlString));
	       // System.err.println(service.queryAllStationBaseInfoXML(inXml));
	        //System.err.println(service.queryStationBaseInfoXML("<?xml version=\"1.0\" encoding=\"GBK\"?><ROOT><STNO>118</STNO></ROOT>"));
	      
//		String str = "[{\"IsNewData\":\"0\",\"RouteID\":110022,\"RouteName\":\"1002·�ڻ�\",\"RouteType\":\"2\",\"SegmentList\":[{\"SegmentID\":100210,\"SegmentName\":\"������Է\",\"FirstTime\":\"2016-01-16 06:30:00\",\"LastTime\":\"2016-01-16 18:30:00\",\"RoutePrice\":\"0\",\"NormalTimeSpan\":0,\"PeakTimeSpan\":0,\"StationList\":[{\"StationID\":\"107552\",\"StationName\":\"������Է\",\"StationNO\":\"107552\",\"StationPostion\":{\"Longitude\":119.38729,\"Latitude\":32.40852},\"Stationmemo\":\"�ϱ�վ̨\"},{\"StationID\":\"111101\",\"StationName\":\"÷Է˫��ѧУ\",\"StationNO\":\"111101\",\"StationPostion\":{\"Longitude\":119.3854,\"Latitude\":32.41248},\"Stationmemo\":\"����վ̨\"},{\"StationID\":\"116592\",\"StationName\":\"���ù������������㻨԰����\",\"StationNO\":\"116592\",\"StationPostion\":{\"Longitude\":119.37957,\"Latitude\":32.41899},\"Stationmemo\":\"\"},{\"StationID\":\"116582\",\"StationName\":\"�����Է��\",\"StationNO\":\"116582\",\"StationPostion\":{\"Longitude\":119.38044,\"Latitude\":32.4225},\"Stationmemo\":\"\"},{\"StationID\":\"116572\",\"StationName\":\"ά����ѧ\",\"StationNO\":\"116572\",\"StationPostion\":{\"Longitude\":119.38095,\"Latitude\":32.42489},\"Stationmemo\":\"\"},{\"StationID\":\"104292\",\"StationName\":\"����ӡˢ��\",\"StationNO\":\"104292\",\"StationPostion\":{\"Longitude\":119.38587,\"Latitude\":32.42519},\"Stationmemo\":\"�ϱ�վ̨\"},{\"StationID\":\"102142\",\"StationName\":\"��ׯ��������Ժ��\",\"StationNO\":\"102142\",\"StationPostion\":{\"Longitude\":119.39063,\"Latitude\":32.42449},\"Stationmemo\":\"�ϱ�վ̨\"},{\"StationID\":\"116563\",\"StationName\":\"ɽˮ���Ƕ���\",\"StationNO\":\"116563\",\"StationPostion\":{\"Longitude\":119.39228,\"Latitude\":32.4235},\"Stationmemo\":\"\"},{\"StationID\":\"116552\",\"StationName\":\"����ӡ�󻪸�\",\"StationNO\":\"116552\",\"StationPostion\":{\"Longitude\":119.39298,\"Latitude\":32.42217},\"Stationmemo\":\"\"},{\"StationID\":\"107572\",\"StationName\":\"����������\",\"StationNO\":\"107572\",\"StationPostion\":{\"Longitude\":119.39717,\"Latitude\":32.42488},\"Stationmemo\":\"�ϱ�վ̨\"},{\"StationID\":\"107862\",\"StationName\":\"�¶�����ѧ\",\"StationNO\":\"107862\",\"StationPostion\":{\"Longitude\":119.40234,\"Latitude\":32.42593},\"Stationmemo\":\"�ϱ�վ̨\"},{\"StationID\":\"107563\",\"StationName\":\"������\",\"StationNO\":\"107563\",\"StationPostion\":{\"Longitude\":119.4051,\"Latitude\":32.42496},\"Stationmemo\":\"����վ̨\"},{\"StationID\":\"103593\",\"StationName\":\"����·\",\"StationNO\":\"103593\",\"StationPostion\":{\"Longitude\":119.40678,\"Latitude\":32.42186},\"Stationmemo\":\"����վ̨\"},{\"StationID\":\"106333\",\"StationName\":\"�������\",\"StationNO\":\"106333\",\"StationPostion\":{\"Longitude\":119.41055,\"Latitude\":32.41498},\"Stationmemo\":\"����վ̨\"},{\"StationID\":\"115284\",\"StationName\":\"����������\",\"StationNO\":\"115284\",\"StationPostion\":{\"Longitude\":119.41067,\"Latitude\":32.41152},\"Stationmemo\":\"\"},{\"StationID\":\"115274\",\"StationName\":\"����Է����\",\"StationNO\":\"115274\",\"StationPostion\":{\"Longitude\":119.40255,\"Latitude\":32.40852},\"Stationmemo\":\"\"},{\"StationID\":\"116524\",\"StationName\":\"������Է\",\"StationNO\":\"116524\",\"StationPostion\":{\"Longitude\":119.39905,\"Latitude\":32.40699},\"Stationmemo\":\"\"},{\"StationID\":\"115264\",\"StationName\":\"��ܰ��԰����\",\"StationNO\":\"115264\",\"StationPostion\":{\"Longitude\":119.39688,\"Latitude\":32.40609},\"Stationmemo\":\"\"},{\"StationID\":\"104891\",\"StationName\":\"ɽ��԰����ܰ��԰��\",\"StationNO\":\"104891\",\"StationPostion\":{\"Longitude\":119.39473,\"Latitude\":32.40603},\"Stationmemo\":\"����վ̨\"},{\"StationID\":\"107544\",\"StationName\":\"������Է\",\"StationNO\":\"107544\",\"StationPostion\":{\"Longitude\":119.39167,\"Latitude\":32.40926},\"Stationmemo\":\"����վ̨\"},{\"StationID\":\"107554\",\"StationName\":\"������Է\",\"StationNO\":\"107554\",\"StationPostion\":{\"Longitude\":119.38687,\"Latitude\":32.40858},\"Stationmemo\":\"����վ̨\"}],\"FirtLastShiftInfo\":\"��ĩ�ࣺ06:30--18:30\",\"FirtLastShiftInfo2\":null,\"Memos\":null}],\"TimeStamp\":\"2016-03-12 00:12:39\",\"RouteMemo\":null}]";
//		Object object = CreatePojo.getObjFromJson(RouteStation.class, str);
		//testGetJson();
//		Long start =1447541731L;//2015-11-15 6:55:31
//		Long end =  1448009731L;//2015-11-18 16:55:31
//		Integer bHour= 0;
//		Integer bMiunte = 0;
//		Integer eHour= 24;
//		Integer eMiunte = 0;
//		testPrice(start,end,bHour,bMiunte,eHour,eMiunte);
		//System.out.println(TimeTools.getTime_MMdd_HHmm(1447567045L*1000));
	//	weixin();//
	//	getMobile();
//		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
//		Map<String ,Object> m1 = new HashMap<String, Object>();
//		Map<String ,Object> m2 = new HashMap<String, Object>();
//		Map<String ,Object> m3 = new HashMap<String, Object>();
//		Map<String ,Object> m4 = new HashMap<String, Object>();
//		Map<String ,Object> m5 = new HashMap<String, Object>();
//		m1.put("create_time", 1L);
//		m2.put("create_time",2L);
//		m3.put("create_time", 3L);
//		m4.put("create_time", 4L);
//		m5.put("create_time", 5L);
//		m1.put("name", "1");
//		m2.put("name", "name1");
//		m3.put("name", "555");
//		list.add(m5);
//		list.add(m2);
//		list.add(m1);
//		list.add(m4);
//		System.out.println(list);
//		Collections.sort(list,new OrderSortCompare());
//		
		/*String carNumber =  "��A12334";
		if(carNumber.equals("���ƺ�δ֪"))
			carNumber = null;
		if(carNumber!=null&&!carNumber.equals("")&&carNumber.length()>5)
			carNumber = carNumber.substring(0,2)+"***"+carNumber.substring(5);
		else {
			carNumber="d";
		}
		System.out.println(carNumber);*/
//		System.out.println(list);
		/*for(int i =0;i<100;i++){
			int k =  RandomUtils.nextInt(20);
			//if(k==8)
			System.err.println(k);
		}*/
		//String mobiles = getMobile();
		//ȫ�峵����
		//String mesg = "���Ӹ����ѣ������Ѱ��ꡣΪ��л֧�֣����˳���ÿ�˻�3��4Ԫȯ��5�²���010-56450585��Ѱ���ͨ����˳����ʡ��˶���T ��ͣ������";
		//��ͨ��Ա������
		//String mesg = "�Ƽ����賵�����һԪ����֧���ŵ��ˣ�������һ���Ƚ���ͣ�����������������ֵ�����΢�Ź�עtingchebao2014�ء�������ѯ";
		
		//new SendMessage().sendMultiMessage("13860132164,15801482643,13041096867,18101333937"+mobiles, mesg);
/*//		
	   
	        String[] ss = getUUID(1); 
	        for(int i=0;i<ss.length;i++){ 
	        	String vInteger = "10258";
	        	String c = ss[i];
	        	StringBuffer nc = new StringBuffer();
	        	Integer charIndex=RandomUtils.nextInt(2);
	        	String stuf = "zd";
	        	c = stuf.charAt(charIndex)+c;
	        	System.out.println(c);
	        	for(int j=0;j<c.length();j++){
	        		Character chara = c.charAt(j);
	        		if(j>1&&j<9)
	        			chara = chara.toUpperCase(chara);
	        		nc.append(chara);
	        		if(j==1)
	        			nc.append(vInteger.charAt(0));
	        		else if(j==3){
	        			nc.append(vInteger.charAt(1));
	        		}else if(j==5){
	        			nc.append(vInteger.charAt(2));
	        		}else if(j==6){
	        			nc.append(vInteger.charAt(3));
	        		}else if(j==9){
	        			nc.append(vInteger.charAt(4));
	        		}
	        		
	        	}
	        	String result = nc.toString();
	            System.out.println(result); 
	            System.out.println(result.length());
	            System.out.println(result.charAt(2)+""+result.charAt(5)+result.charAt(8)+""+result.charAt(10)+""+result.charAt(14));
	        } */
		/*
		String url = "https://api.mch.weixin.qq.com/pay/downloadbill";
		String paramsm = "<xml>" +
				"<appid>wx2421b1c4370ec43b</appid>" +
				"<bill_date>20141110</bill_date>"+
				" <bill_type>ALL</bill_type>" +
				"<mch_id>10000100</mch_id> " +
				"<nonce_str>21df7dc9cd8616b56919f20d9f679233</nonce_str>"+
				"<sign>332F17B766FC787203EBE9D6E40457A1</sign>" +
				"</xml>";
		String dString = "20150610";
		SortedMap<Object,Object> params = new TreeMap<Object,Object>();
		String timestamp = Sha1Util.getTimeStamp();
		params.put("appid", Constants.WXPUBLIC_APPID);
        params.put("bill_date", dString);
        params.put("bill_type","SUCCESS");
        params.put("mch_id", Constants.WXPUBLIC_MCH_ID);
        params.put("nonce_str", timestamp);
        String paySign = PayCommonUtil.createSign("UTF-8", params);
        
        Map<String, Object> parMap = new HashMap<String, Object>();
        parMap.put("appid", Constants.WXPUBLIC_APPID);
        parMap.put("bill_date", dString);
        parMap.put("bill_type","SUCCESS");
        parMap.put("mch_id", Constants.WXPUBLIC_MCH_ID);
        parMap.put("nonce_str", timestamp);
        parMap.put("sign", paySign);
        
        StringBuffer xml = new StringBuffer();
		xml.append("<xml>");
		for(String key : parMap.keySet()){
			xml.append("<"+key+">"+parMap.get(key)+"</"+key+">");
		}
		xml.append("</xml>");
        
		 
        String result = CommonUtil.httpsRequest(url, "POST",xml.toString());*/
		/*BufferedReader reader =null;
		BufferedWriter writer=null;
		try {
			reader = new BufferedReader(new FileReader(new File("d:/pay.txt")));
			String lineString = "";
			Map<String, String> uidoidMap = new HashMap<String, String>();
			while ((lineString = reader.readLine()) != null) {
				String []temp = lineString.split(",");
				if(temp.length>6){
					String key = temp[6];
					if(key!=null&&key.length()==10)
						uidoidMap.put(key, temp[5]);
				}
			}
			reader.close();
			reader = new BufferedReader(new FileReader(new File("d:/weixin3.csv")));
			writer = new BufferedWriter(new FileWriter(new File("d:/weixin3new.csv"),true));
			while ((lineString = reader.readLine()) != null) {
				String []temp = lineString.split(",");
				if(temp.length==11){
					String uid = temp[10];
					if(uid.length()==9)
						uid ="0"+uid;
					String oid = uidoidMap.get(uid);
					if(oid!=null){
						lineString +=",'"+oid;
					}
					writer.write("\n"+lineString+" ");
				}
			}
			writer.flush();
			writer.close();
			reader.close();
			reader = new BufferedReader(new FileReader(new File("d:/weixin5.csv")));
			writer = new BufferedWriter(new FileWriter(new File("d:/weixin5new.csv"),true));
			while ((lineString = reader.readLine()) != null) {
				String []temp = lineString.split(",");
				if(temp.length==11){
					String uid = temp[10];
					if(uid.length()==9)
						uid ="0"+uid;
					String oid = uidoidMap.get(uid);
					if(oid!=null){
						lineString +=",'"+oid;
					}
					writer.write("\n"+lineString+" ");
				}
			}
			writer.flush();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			try {
				writer.close();
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}*/
	}
	
	 /** 
     * ���һ��UUID 
     * @return String UUID 
     */ 
    public static String getUUID(){ 
        String s = UUID.randomUUID().toString(); 
        //ȥ����-������ 
        return s.substring(s.lastIndexOf("-")+1);//.substring(0,8)+s.substring(9,13)+s.substring(14,18)+s.substring(19,23)+s.substring(24); 
    } 
    /** 
     * ���ָ����Ŀ��UUID 
     * @param number int ��Ҫ��õ�UUID���� 
     * @return String[] UUID���� 
     */ 
    public static String[] getUUID(int number){ 
        if(number < 1){ 
            return null; 
        } 
        String[] ss = new String[number]; 
        for(int i=0;i<number;i++){ 
            ss[i] = getUUID(); 
        } 
        return ss; 
    } 
}
