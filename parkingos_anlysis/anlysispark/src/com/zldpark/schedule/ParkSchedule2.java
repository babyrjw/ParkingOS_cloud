package com.zldpark.schedule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.log4j.Logger;

import com.zldpark.CustomDefind;
import com.zldpark.service.DataBaseService;
import com.zldpark.utils.ContinueFTP;
import com.zldpark.utils.StringUtils;
import com.zldpark.utils.TimeTools;
/**
 * 沈阳采集数据的
 * @author drh
 *
 */
public class ParkSchedule2 implements Runnable {
	
	DataBaseService dataBaseService;
	
	public ParkSchedule2(DataBaseService dataBaseService ){
		this.dataBaseService = dataBaseService;
	}
	ExecutorService executor2 =Executors.newSingleThreadExecutor();

	private static Logger log = Logger.getLogger(ParkSchedule2.class);
	public void run() {
		log.error("***********==========主线程开始定时任务"+TimeTools.gettime()+"=============*********************");
		try {
			long btime = System.currentTimeMillis();
			FutureTask<String> future =
			       new FutureTask<String>(new Callable<String>() {
			         public String call() {
			        	 log.error("***********==========子线程开始查询数据"+TimeTools.gettime()+"=============*********************");
			 			long comId = Long.parseLong(CustomDefind.TESTCOMID);
			 			Map<String,Object> infoMap  = new LinkedHashMap <String, Object>();
			 			long btime = TimeTools.getToDayBeginTime();
			 			long time2 = TimeTools.getBeginTime(System.currentTimeMillis()-2*24*60*60*1000);
			 			long time16 = TimeTools.getBeginTime(System.currentTimeMillis()-16*24*60*60*1000);
			 			int totalin = 0;
			 			int policein = 0;
			 			int monthin = 0;
			 			int totalout = 0;
			 			int policeout = 0;
			 			int monthout = 0;
			 			int che = 0;
			 			Integer shareNumber =0;// pgService.getLong("select share_number from com_info_tb where id=?", new Object[]{comId});
			 		try {
			 			String sql = "select count(ID) from order_tb where comid=? and create_time>? and state=? ";
			 			Long count = dataBaseService.getLong(sql, new Object[]{comId,time2,0});
			 			if(count==null){
			 				count=0L;
			 			}
			 			String sql1 = "select count(ID) from order_tb where comid=? and create_time>? and create_time<? and state=? ";
			 			Long count1 = dataBaseService.getLong(sql1, new Object[]{comId,time16,time2,0});//2天前往前推14天的未结算的订单
			 			if(count==null){
			 				count1=0L;
			 			}
			 			int invalid = (int) (count1*2/14);
			 			Map<String, Object> map = dataBaseService.getMap("select share_number,invalid_order,disablereason from com_info_tb where id=?", new Object[]{comId});
			 			shareNumber = (Integer)map.get("share_number");
//			 			Long invalid_order = (Long)map.get("invalid_order");
			 			Integer disablereason = (Integer)map.get("disablereason");
			 			//总的进场
			 			Map<String, Object> ordermap1 = dataBaseService.getMap("select count(ID)total from order_tb where comid=?  and create_time>?", new Object[]{comId,btime});
			 			if(ordermap1!=null&&ordermap1.get("total")!=null){
			 				totalin = Integer.parseInt(ordermap1.get("total")+"");
			 			}
			 			//军警进场
			 			Map<String, Object> ordermap2 = dataBaseService.getMap("select count(ID)total from order_tb where comid=?  and car_type=? and create_time>?", new Object[]{comId,9,btime});
			 			if(ordermap2!=null&&ordermap2.get("total")!=null){
			 				policein = Integer.parseInt(ordermap2.get("total")+"");
			 			}
			 			//包月场
			 			Map<String, Object> ordermap3 = dataBaseService.getMap("select count(ID)total from order_tb where comid=? and c_type= ? and create_time>? ", new Object[]{comId,5,btime});
			 			if(ordermap3!=null&&ordermap3.get("total")!=null){
			 				monthin = Integer.parseInt(ordermap3.get("total")+"");
			 			}
			 			//总的离场
			 			Map<String, Object> ordermap4 = dataBaseService.getMap("select count(ID)total from order_tb where comid=? and state=? and end_time>?", new Object[]{comId,1,btime});
			 			if(ordermap4!=null&&ordermap4.get("total")!=null){
			 				totalout = Integer.parseInt(ordermap4.get("total")+"");
			 			}
			 			//军警离场
			 			Map<String, Object> ordermap5 = dataBaseService.getMap("select count(ID)total from order_tb where comid=? and state=? and pay_type=? and end_time>?", new Object[]{comId,1,9,btime});
			 			if(ordermap5!=null&&ordermap5.get("total")!=null){
			 				policeout = Integer.parseInt(ordermap5.get("total")+"");
			 			}
			 			//包月离场
			 			Map<String, Object> ordermap6 = dataBaseService.getMap("select count(ID)total from order_tb where comid=? and state=? and c_type= ? and end_time>? ", new Object[]{comId,1,5,btime});
			 			if(ordermap6!=null&&ordermap6.get("total")!=null){
			 				monthout = Integer.parseInt(ordermap6.get("total")+"");
			 			}
			 			infoMap.put("WorksNo", CustomDefind.PARKID);//临停进场
			 			infoMap.put("InNum", totalin-policein-monthin);//临停进场
			 			infoMap.put("OutNum", totalout-policeout-monthout);//临停进场
			 			infoMap.put("MonthInNum", monthin);//月卡进场
			 			infoMap.put("MonthOutNum", monthout);//月卡出场
			 			infoMap.put("CarInNum", 0);
			 			infoMap.put("CarOutNum", 0);
			 			infoMap.put("PublicInNum", policein);//警车进场
			 			infoMap.put("PublicOutNum", policeout);//警车车场
//			 			infoMap.put("ParkinkNum", shareNumber-count+invalid_order>0?shareNumber-count+invalid_order:0);
			 			infoMap.put("ParkinkNum", shareNumber-count-invalid<0?1:shareNumber-count-invalid);
			 			infoMap.put("MonthNum", 0);
			 			infoMap.put("CarNum", 0);
			 			infoMap.put("PublicNum", 0);
			 			infoMap.put("Reason", disablereason);//停用原因
			 			String xml = StringUtils.createXML(infoMap);
			 			String parkid = CustomDefind.PARKID;
			 			String filename1 = parkid+"_"+TimeTools.getTime_yyyyMMddHHmmss()+"_"+getid(0)+"_1"+"_BERTH.xml";
			 			String filename2 = write(xml,"c:/test/parkinfo/"+filename1);
			 			log.error("准备上传ftp时间："+TimeTools.gettime());
			 			ContinueFTP.ftpUtil(filename2,filename1);
			 			log.error("***********==========一次定时任务结束"+TimeTools.gettime()+"=============*********************");
			 		} catch (Exception e) {
			 			e.printStackTrace();
			 		}
					return "success";
			       }});
			executor2.submit(future);
			try {
				Thread.sleep(9000);
				log.error("子线程执行10秒后是非完成："+future.isDone());
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}finally{
				future.cancel(true);
				log.error("10秒后关闭子线程结果："+future.isCancelled());
			}
//			log.error("子线程执行10秒后是非完成："+future.isDone());
//			while(!future.isDone()){
//				try {
//					String result = future.get(1, TimeUnit.MILLISECONDS);
//					if(result==null){
//						future.cancel(true);
//						log.error("关闭子线程结果："+future.isCancelled());
//					}
//				} catch (Exception e) {
//					future.cancel(true);
//					log.error("关闭子线程结果："+future.isCancelled());
//				} finally {
//				}
//			}
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
		
	}
	 public static String write(String xml , String filename) {
		 BufferedWriter bw = null;
		 FileWriter fw = null;
		  try {
			  File root = new File("C:\\test\\parkinfo\\");
			    File[] files = root.listFiles();
			    int i = 0;
			    for(File f:files){    
			     if(!f.isDirectory()){
			    	 f.delete();
			     }
			   }
		   File file = new File(filename);
		   if (!file.exists()) {
		    file.createNewFile();
		   }
		   fw = new FileWriter(file.getAbsoluteFile());
		   bw = new BufferedWriter(fw);
		   bw.write(xml);
		  } catch (IOException e) {
		   e.printStackTrace();
		  }finally{
			  if(bw!=null){
				  try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			  }
			  if(fw!=null){
				  try {
					fw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			  }
		  }
		  return filename;
		}
	 private String getid(long a){
//		 uploadftp_fileno_tb
		 int time = Integer.parseInt("2"+getTimeyyyyMMdd());
		 Map map = dataBaseService.getMap("select * from uploadftp_fileno_tb where id =? ", new Object[]{time});
		 if(map==null){
			 dataBaseService.update("insert into uploadftp_fileno_tb(id,no) values (?,?) ", new Object[]{time,1});
		 }else{
			 a = Integer.parseInt(map.get("no")+"");
			 dataBaseService.update("update uploadftp_fileno_tb set no=? where id=? ", new Object[]{a+1,time});
		 }
		 if(a>=0&&a<10)
			 return "00000"+a;
		 if(a>=10&&a<100)
			 return "0000"+a;
		 if(a>=100&&a<1000)
			 return "000"+a;
		 if(a>=1000&&a<100000)
			 return "00"+a;
		 if(a>=10000&&a<100000)
			 return "0"+a;
		 return "000000";
	 }
	 private static String getTimeyyyyMMdd(){
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			return dateFormat.format(new java.util.Date());
		}
}
