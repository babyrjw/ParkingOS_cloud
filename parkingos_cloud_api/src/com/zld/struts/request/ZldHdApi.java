package com.zld.struts.request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;
import com.zld.AjaxUtil;
import com.zld.impl.CommonMethods;
import com.zld.impl.MemcacheUtils;
import com.zld.impl.MongoClientFactory;
import com.zld.impl.PublicMethods;
import com.zld.pojo.ParseJson;
import com.zld.service.DataBaseService;
import com.zld.service.PgOnlyReadService;
import com.zld.utils.Check;
import com.zld.utils.StringUtils;
import com.zld.utils.TimeTools;
import com.zld.utils.ZldUploadOperate;
import com.zld.utils.ZldUploadUtils;


/**
 * �豸��Ϣ  ���нӿ�
 * @author laoyao
 *
 */
@Path("hdinfo")
public class ZldHdApi {
	
	Logger logger = Logger.getLogger(ZldHdApi.class);
	
	/**
	 * �豸��Ϣ
	 * http://127.0.0.1/zld/api/hdinfo/uploadhd
	 */
	@POST
	@Path("/uploadhd")//�豸
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)       
	public void uploadhd(String params,
			@Context ServletContext context,
			@Context HttpServletResponse response)throws IOException {
		Map<String, String> paramMap = ZldUploadUtils.stringToMap(params);
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
		ZldUploadOperate zldUploadOperate = (ZldUploadOperate) ctx.getBean("zldUploadOperate");
		if(paramMap.get("park_uuid")!=null){
			String comid =zldUploadOperate.getComIdByParkUUID(paramMap.get("park_uuid"),context);
			paramMap.put("comid", comid);
			paramMap.remove("park_uuid");
		}
		//0��ǩ��д�豸,1һ��ͨ��д�豸,3���������,4�ظ�,5�ֳ��ն�,6����ʶ���豸,7��������豸,8:ͨ�Ż�վ��9�յ���,10��բ
		//��д���豸��
		Map<String, Object> returnMap = zldUploadOperate.handleData(context,paramMap,params,"com_hd_tb",-1);
		if(returnMap.get("status").equals("1")){
			System.out.println("�豸��д��....");
			
			String type = paramMap.get("type");
			if(Check.isNumber(type)){
				Integer t = Integer.valueOf(type);
				switch (t) {
				case 3:
					returnMap.clear();
					paramMap.remove("type");
					returnMap = zldUploadOperate.handleData(context,paramMap,params,"dici_tb",-1);
					break;
				case 6:
					returnMap.clear();
					paramMap.remove("type");
					paramMap.remove("plot_id");
					paramMap.put("upload_time", paramMap.get("operate_time"));
					paramMap.remove("operate_time");
					paramMap.put("passid", "-1");
					returnMap = zldUploadOperate.handleData(context,paramMap,params,"com_camera_tb",-1);
					break;
			   case 8://��վ
					returnMap.clear();
					paramMap.remove("type");
					paramMap.remove("plot_id");
					paramMap.put("upload_time", paramMap.get("operate_time"));
					paramMap.remove("operate_time");
					paramMap.put("passid", "-1");
					returnMap = zldUploadOperate.handleData(context,paramMap,params,"sites_tb",-1);
					break;
				case 9:
					returnMap.clear();
					paramMap.remove("type");
					paramMap.remove("plot_id");
					paramMap.put("upload_time", paramMap.get("operate_time"));
					paramMap.remove("operate_time");
					paramMap.put("passid", "-1");
					returnMap = zldUploadOperate.handleData(context,paramMap,params,"com_led_tb",-1);
					break;
				case 10:
					returnMap.clear();
					paramMap.remove("type");
					paramMap.remove("plot_id");
					paramMap.put("upload_time", paramMap.get("operate_time"));
					paramMap.remove("operate_time");
					paramMap.put("passid", "-1");
					returnMap = zldUploadOperate.handleData(context,paramMap,params,"com_brake_tb",-1);
					break;
				default:
					break;
				}
			}
		}
		logger.error(returnMap);
		AjaxUtil.ajaxOutput(response, StringUtils.createJson(returnMap));
	}
	/**
	 * 1.��ȡ�������豸����/��ѹ�������б�:
	 * http://127.0.0.1/zld/api/hdinfo/InsertSensor
	 */
	@POST
	@Path("/InsertSensor")//��ȡ�������豸����/��ѹ�������б�
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)       
	public void insertSensor(String params,
			@Context ServletContext context,
			@Context HttpServletResponse response
			,@Context HttpServletRequest request)throws IOException {
		Map<String, String> paramMap = ZldUploadUtils.stringToMap(params);
		//logger.error("paramMap:"+paramMap);
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
		DataBaseService daService = (DataBaseService) ctx.getBean("dataBaseService");
		PgOnlyReadService pService = (PgOnlyReadService) ctx.getBean("pgOnlyReadService");
		CommonMethods commonMethods = (CommonMethods)ctx.getBean("commonMethods");
		Long curTime = System.currentTimeMillis()/1000;
		String sensornumber = paramMap.get("sensornumber");
		logger.error(StringUtils.getIpAddr(request));
		logger.error("����,paramMap:"+paramMap);
		double magnetism = 0;
		double battery = 0;
		long siteId = -1L;
		if(paramMap.get("magnetism") != null){
			magnetism = Double.valueOf(paramMap.get("magnetism"));
		}
		if(paramMap.get("battery") != null){
			battery = Double.valueOf(paramMap.get("battery"));
		}
		if(paramMap.get("site_uuid") != null){
			String siteUUID = paramMap.get("site_uuid");
			Map<String, Object> map = pService.getMap("select id from sites_tb where uuid=? and is_delete=? limit ? ",
					new Object[]{siteUUID, 0, 1});
			if(map != null){
				siteId = (Long)map.get("id");
			}
		}
		//logger.error("paramMap:"+paramMap+",siteId:"+siteId);
		Long count = daService.getLong("select count(id) from dici_tb where did=? and is_delete=? ", 
				new Object[]{sensornumber, 0});
		//logger.error("count:"+count);
		if(count == 0){
			int ret = daService.update("insert into dici_tb (did,magnetism,battery,site_id," +
					"operate_time,beart_time) values(?,?,?,?,?,?)", 
					new Object[]{sensornumber, magnetism, battery, siteId, curTime, curTime});
			//logger.error("ret:"+ret);
		}else{
			if(battery > 0){//Ѷ�ʵĳ�����ÿ��ֻ�����ε�ѹ������ʱ���ѹֵ����0
				int ret = daService.update("update dici_tb set beart_time=?,magnetism=?," +
						"battery=?,site_id=? where did=? ", 
						new Object[]{curTime, magnetism, battery, siteId, sensornumber});
				//logger.error("ret:"+ret);
			}else{
				int ret = daService.update("update dici_tb set beart_time=?,magnetism=?," +
						"site_id=? where did=? ", 
						new Object[]{curTime, magnetism, siteId, sensornumber});
				//logger.error("ret:"+ret);
			}
		}
		AjaxUtil.ajaxOutput(response, StringUtils.createXML1(paramMap));
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		cal.setTimeInMillis(System.currentTimeMillis());
		if(cal.get(Calendar.HOUR_OF_DAY)!=18)//ÿ��18�㲻���´˱�
			commonMethods.deviceRecover(0, sensornumber, curTime);
		logger.error("����,paramMap:�������....");
//		Map<String, Object> diciMap = pService.getMap("select comid from dici_tb where did=? and is_delete=? ", 
//				new Object[]{sensornumber, 0});
//		Integer comid = -1;
//		if(diciMap != null && diciMap.get("comid") != null){
//			comid = (Integer)diciMap.get("comid");
//		}
		//paramMap.put("source", "InsertSensor");
		//writeToMongodb("zld_hdbeart_logs", paramMap, comid.longValue());
	}
	/**
	 * ��ȡ��վ����/��ѹ�б�
	 * http://127.0.0.1/zld/api/hdinfo/InsertTransmitter
	 */
	@POST
	@Path("/InsertTransmitter")//��ȡ��վ����/��ѹ�б�
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)       
	public void insertTransmitter(String params,
			@Context ServletContext context,
			@Context HttpServletResponse response)throws IOException {
		//TransmitterNumber
		//VoltageCaution ��ѹ
		Map<String, String> paramMap = ZldUploadUtils.stringToMap(params);
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
		DataBaseService daService = (DataBaseService) ctx.getBean("dataBaseService");
		CommonMethods commonMethods = (CommonMethods)ctx.getBean("commonMethods");
		PgOnlyReadService pService = (PgOnlyReadService) ctx.getBean("pgOnlyReadService");
		logger.error("InsertTransmitter ,paramsMap :"+paramMap);
		String sql = "update sites_tb set heartbeat=? where uuid=? ";
		Object []values = new Object[]{System.currentTimeMillis()/1000,paramMap.get("transmitternumber")};
		Double vol = StringUtils.formatDouble(paramMap.get("voltagecaution"));
		if(vol>0){
			sql ="update sites_tb set voltage=?,heartbeat=?,update_time=? where uuid=? ";
			values = new Object[]{Double.valueOf(paramMap.get("voltagecaution")),
					System.currentTimeMillis()/1000,System.currentTimeMillis()/1000,paramMap.get("transmitternumber")};
		}
		int ret = daService.update(sql, values);
		//logger.error("InsertTransmitter ret :"+ret+",paramsMap :"+paramMap);
		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		cal.setTimeInMillis(System.currentTimeMillis());
		if(cal.get(Calendar.HOUR_OF_DAY)!=18)//ÿ��18�㲻���´˱�
			commonMethods.deviceRecover(1, paramMap.get("transmitternumber"), System.currentTimeMillis()/1000);
		Integer comid=-1;
		if(ret>0){
			Map diciMap = pService.getMap("select comid from dici_tb where did = ? and is_delete=? ",
					new Object[]{paramMap.get("transmitternumber"), 0});
			if(diciMap!=null&&diciMap.get("comid")!=null){
				comid = (Integer)diciMap.get("comid");
			}
		}else{
			Long ntime = System.currentTimeMillis()/1000;
			ret = daService.update("insert into sites_tb (uuid,state,voltage,heartbeat,create_time) values(?,?,?,?,?)", 
					new Object[]{paramMap.get("transmitternumber"),1,Double.valueOf(paramMap.get("voltagecaution")),ntime,ntime});
			//logger.error("InsertTransmitter insert ret :"+ret);
		}
		AjaxUtil.ajaxOutput(response, StringUtils.createXML1(paramMap));
		paramMap.put("battery", paramMap.get("voltagecaution"));
		paramMap.remove("voltagecaution");
		paramMap.put("source", "InsertTransmitter");
		writeToMongodb("zld_hdbeart_logs", paramMap,comid.longValue());
		logger.error("InsertTransmitter ,������� .....");
		//logger.error("InsertTransmitter write to mongodb result :"+result+",paramsMap :"+paramMap);
		
	}
	
	/**
	 * ͨѶ���ݼ�¼
	 * http://127.0.0.1/zld/api/hdinfo/InsertSensorLog
	 */
	@POST
	@Path("/InsertSensorLog")//ͨѶ���ݼ�¼
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)       
	public void insertSensorLog(String params,
			@Context ServletContext context,
			@Context HttpServletResponse response)throws IOException {
		Map<String, String> paramMap = ZldUploadUtils.stringToMap(params);
		writeToMongodb("zld_sensordata_logs", paramMap,-1L);
		AjaxUtil.ajaxOutput(response, StringUtils.createXML1(paramMap));
		//Content ͨѶ����
		//Exception  �Ƿ����쳣
	}
	/**
	 *����������
	 * http://127.0.0.1/zld/api/hdinfo/InsertCarAdmission
	 */
	@POST
	@Path("/InsertCarAdmission")//���������� InsertCarAdmission
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)       
	public void insertCarAdmission(String params,
			@Context ServletContext context,
			@Context HttpServletResponse response)throws IOException {
		Map<String, String> paramMap = ZldUploadUtils.stringToMap(params);
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
		DataBaseService daService = (DataBaseService) ctx.getBean("dataBaseService");
		MemcacheUtils  memcacheUtils=(MemcacheUtils)ctx.getBean("memcacheUtils");
		PgOnlyReadService pService = (PgOnlyReadService) ctx.getBean("pgOnlyReadService");
		logger.error("InsertCarAdmission>>>>>>>��������>>>>>>>>>>InsertCarAdmission "+paramMap);
		//CarInTime ����ʱ��
		//Indicate ��Ա�ʾ
		//SensorNumber ���������
		//��Ҫд�شű��ڴŶ���������Ϣ���շ�Ա
		paramMap.put("type", "����������");
		//�ж��Ƿ��ѷ�����Ϣ
		Map<String, String> cMap = new HashMap<String, String>();
		cMap.put("sensornumber", paramMap.get("sensornumber"));
		cMap.put("carintime", AjaxUtil.decodeUTF8(paramMap.get("carintime")));
		Long pcount = isHave("zld_sensor_logs", cMap);
		if(pcount!=null&&pcount>0){
			paramMap.put("error", "�����Ѵ������");
			logger.equals("InsertCarAdmission >>>�����Ѵ����...."+paramMap);
			AjaxUtil.ajaxOutput(response, StringUtils.createXML1(paramMap));
			return;
		}
		//�ж��Ƿ��ѷ�����Ϣ����
		writeToMongodb("zld_sensor_logs",paramMap,-1L);
		String carIntime = AjaxUtil.decodeUTF8(paramMap.get("carintime"));
		Long intime = TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(carIntime);
		String sql = "update dici_tb set state = ? where did =? ";
		int ret =daService.update(sql, new Object[]{1,paramMap.get("sensornumber")});
		//logger.error("InsertCarAdmission>>>>>>>��������>>>>>>>>>>InsertCarAdmission "+paramMap+", update dici state:"+ret);
		if(ret<1){//�شŲ����� ��ֱ�ӷ���
			paramMap.put("error", "�شŲ�����");
			AjaxUtil.ajaxOutput(response, StringUtils.createXML1(paramMap));
			return;
		}
		Map diciMap = pService.getMap("select id,comid from dici_tb where did = ? and is_delete=? ",
				new Object[]{paramMap.get("sensornumber"), 0});
		Long comId = -1L;
		Long dici_id = -1L;
		if(diciMap!=null&&diciMap.get("comid")!=null){
			dici_id = (Long)diciMap.get("id");
		}
		//logger.error("did:"+paramMap.get("sensornumber")+",dici_id:"+dici_id);
		if(dici_id > 0){
			//���ݵشű�Ų����Ƿ���ע�ᵽ��λ
			Map<String, Object> comParkMap = pService.getMap("select id,berthsec_id,comid,state,order_id from com_park_tb " +
					"where is_delete=? and  dici_id =? ", new Object[]{0,dici_id});
			//logger.error("did:"+paramMap.get("sensornumber")+",comParkMap:"+comParkMap+",dici_id:"+dici_id);
			Long bid =-1L;
			if(comParkMap != null && !comParkMap.isEmpty()){//�ش��Ѱ󶨵���λ
				Long pid = (Long)comParkMap.get("id");
				comId = (Long)comParkMap.get("comid");
				Integer state = (Integer)comParkMap.get("state");
				Long uin = -1L;
				sql ="select uid from parkuser_work_record_tb where state=? and start_time>? and  berthsec_id =? ";
				Map userMap = pService.getMap(sql, new Object[]{0,0,comParkMap.get("berthsec_id")});
				//logger.error("InsertCarAdmission find user:"+userMap);
				if(userMap!=null&&userMap.get("uid")!=null){//�鵽���շ�Ա
					uin = (Long)userMap.get("uid");
				}
				//�Ƿ������ɹ�����
				Long count = pService.getLong("select count(ID) from berth_order_tb where berth_id=? and indicate=? and in_time=? ",
						new Object[]{paramMap.get("sensornumber"),paramMap.get("indicate"),intime});
				//logger.error("sensor come in>>>uin:"+uin+",paramMap:"+paramMap+",count:"+count);
				if(count==0){
					Integer bind_flag = 0;//Ĭ�ϲ����԰�POS������
					if(state == 0){//����λ״̬�ǿ��е�ʱ��ſ��԰�POS������
						bind_flag = 1;
					}
					bid = daService.getkey("seq_berth_order_tb");
					sql="insert into berth_order_tb (id,in_time,state,berth_id,indicate,comid,dici_id,in_uid,bind_flag) values(?,?,?,?,?,?,?,?,?)";
					ret = daService.update(sql, new Object[]{bid,intime,0,paramMap.get("sensornumber"),
							paramMap.get("indicate"),comId,comParkMap.get("id"),uin,bind_flag});
					logger.error("InsertCarAdmission insert berth_order_tb state:"+ret);
				}
				//logger.error("bid:"+bid+",ret:"+ret);
				//��ѯ�شŶ�Ӧ�Ĳ�λ��Ӧ�Ĳ�λ�ζ�Ӧ���ϸ��շ�Ա
//				"(select berthsec_id from com_park_tb where is_delete=? and dici_id ="+
//				"(select id from dici_tb where did= ?))";
				//logger.error("InsertCarAdmission find user:"+userMap);
				//��ѯ�شŶ�Ӧ�Ĳ�λ���
				
//			if(comParkMap!=null&&comParkMap.get("id")!=null){
//				pid = comParkMap.get("id")+"";
//			}
				if(userMap!=null&&userMap.get("uid")!=null){//�鵽���շ�Ա
					putMesgToCache("10",uin,pid+"","1",bid,memcacheUtils);
					paramMap.put("uid",uin+"");
				}else {//��Ϣ����λ��
//			sql ="select berthsec_id from com_park_tb where is_delete=? and dici_id ="+
//					"(select id from dici_tb where did= ?)";
//			Map berthMap = daService.getMap(sql, new Object[]{0,paramMap.get("sensornumber")});
					//logger.error("InsertCarAdmission no user write to berthsecid:"+comParkMap.get("berthsec_id"));
					//if(berthMap!=null&&berthMap.get("berthsec_id")!=null){//�ҵ��˲�λ��
					Long berthSegId = (Long)comParkMap.get("berthsec_id");
					if(berthSegId != null && berthSegId > 0){
						putMesgToCache("10",berthSegId,pid+"","1",bid,memcacheUtils);
						paramMap.put("berthsecid",berthSegId+"");
					}
					//}
				}
			}else {
				logger.error("InsertCarAdmission...�ش�û�а󶨳�λ..."+paramMap);
			}
		}
		
		paramMap.put("type", "in");
		writeToMongodb("zld_hdinout_logs", paramMap,comId);
		//logger.error("InsertCarAdmission write to mongodb result :"+result+",paramsMap :"+paramMap);
		logger.error("InsertCarAdmission>>>>>>>��������>>>>>>>>>>�������");
		AjaxUtil.ajaxOutput(response, StringUtils.createXML1(paramMap));
	}
	
	/**
	 * ����������
	 * http://127.0.0.1/zld/api/hdinfo/InsertCarEntrance
	 */
	@POST
	@Path("/InsertCarEntrance")//����������
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)       
	public void insertCarEntrance(String params,
			@Context ServletContext context,
			@Context HttpServletResponse response)throws IOException {
		Map<String, String> paramMap = ZldUploadUtils.stringToMap(params);
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
		DataBaseService daService = (DataBaseService) ctx.getBean("dataBaseService");
		MemcacheUtils  memcacheUtils=(MemcacheUtils)ctx.getBean("memcacheUtils");
		CommonMethods  commonMethods=(CommonMethods)ctx.getBean("commonMethods");
		PublicMethods  publicMethods=(PublicMethods)ctx.getBean("publicMethods");
		PgOnlyReadService pService = (PgOnlyReadService) ctx.getBean("pgOnlyReadService");
		logger.error(">>>>>>>��������>>>>>>>>>>InsertCarEntrance "+paramMap);
		//CarOutTime ����ʱ��
		//Indicate  ��Ա�ʾ
		//SensorNumber ���������
		paramMap.put("type", "����������");
		//�ж��Ƿ��ѷ�����Ϣ
		Map<String, String> cMap = new HashMap<String, String>();
		cMap.put("sensornumber", paramMap.get("sensornumber"));
		cMap.put("carouttime", AjaxUtil.decodeUTF8(paramMap.get("carouttime")));
		Long pcount = isHave("zld_sensor_logs", cMap);
		if(pcount!=null&&pcount>0){
			paramMap.put("error", "�����Ѵ������");
			logger.equals("InsertCarEntrance >>>�����Ѵ����...."+paramMap);
			AjaxUtil.ajaxOutput(response, StringUtils.createXML1(paramMap));
			return;
		}
		//�ж��Ƿ��ѷ�����Ϣ����
		writeToMongodb("zld_sensor_logs",paramMap,-1L);
		String carIntime = AjaxUtil.decodeUTF8(paramMap.get("carouttime"));
		Long outtime = TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(carIntime);
		String sql = "update dici_tb set state = ?  where did =? ";
		int ret =daService.update(sql, new Object[]{0,paramMap.get("sensornumber")});
		//logger.error(">>>>>>>��������>>>>>>>>>>InsertCarEntrance "+paramMap+",update dici state:"+ret);
		if(ret<1){//�شŲ����� ��ֱ�ӷ���
			paramMap.put("error", "�شŲ�����");
			AjaxUtil.ajaxOutput(response, StringUtils.createXML1(paramMap));
			return;
		}
		//���µشŶ���
		//?����۸�
		Double total = 0.0;
		Long intime = null;
		Integer comId = -1;
		Map diciMap = pService.getMap("select id,comid from dici_tb where did = ? and is_delete=? ",
				new Object[]{paramMap.get("sensornumber"), 0});
		if(diciMap!=null&&diciMap.get("comid")!=null){
			comId = (Integer)diciMap.get("comid");
		}
		Map botMap = pService.getMap("select id,in_time,orderid from berth_order_tb where berth_id=? and indicate=? and state=? order by in_time desc limit ? ", 
				new Object[]{paramMap.get("sensornumber"),paramMap.get("indicate"),0, 1});
		//logger.error("sensor come out>>>comId:"+comId+",botMap:"+botMap);
		Long bid = -1L;
		Long orderid = -1L;
		if(botMap!=null&&botMap.get("in_time")!=null){
			intime=(Long)botMap.get("in_time");
			bid = (Long)botMap.get("id");
			orderid = (Long)botMap.get("orderid");
			if(intime>0){
				if(comId>0){
					try {
						Map<String, Object> orderMap = null;
						if(orderid>0)
							orderMap=pService.getMap("select * from order_tb where id=? ", new Object[]{orderid});
						if(orderMap != null){
							Long uin = (Long)orderMap.get("uin");
							String carNumber =orderMap.get("car_number")+"";
							Integer pid = (Integer)orderMap.get("pid");
							Integer car_type = (Integer)orderMap.get("car_type");
							if(StringUtils.isNumber(carNumber)){
								carNumber = "���ƺ�δ֪";
							}
							if(carNumber.equals("null")||carNumber.equals("")){
								carNumber =publicMethods.getCarNumber(uin);
							}
							if("".equals(carNumber.trim())||"���ƺ�δ֪".equals(carNumber.trim()))
								carNumber ="null";
							if(pid>-1){
								total = Double.valueOf(publicMethods.getCustomPrice(intime, outtime, pid));
							}else {
								int isspecialcar = 0;
								Map map = pService.getMap("select typeid from car_number_type_tb where car_number = ? and comid=?", 
										new Object[]{carNumber, comId.longValue()});
								if(map!=null&&map.size()>0){
									isspecialcar = 1;
								}
								total = Double.valueOf(publicMethods.getPriceHY(intime, outtime, comId.longValue(), car_type, isspecialcar));
							}
						}else{
							Integer car_type = 0;
							List<Map<String, Object>> allCarTypes = commonMethods.getCarType(comId.longValue());
							if(allCarTypes != null && !allCarTypes.isEmpty()){
								car_type = Integer.valueOf(allCarTypes.get(0).get("value_no")+"");
							}
							total = Double.valueOf(publicMethods.getPriceHY(intime, outtime, comId.longValue(), car_type, 0));
						}
					} catch (Exception e) {
						logger.error(e.getMessage());
					}
				}
			}
			ret = daService.update("update berth_order_tb set out_time =?,state=?,total=? where id=? ", 
					new Object[]{outtime,1,total,bid});
			//logger.error("InsertCarEntrance update berth_order_tb state:"+ret);
		}
		//logger.error("bid:"+bid+",ret:"+ret);
		//���ݵشű�Ų����Ƿ���ע�ᵽ��λ
		Map<String, Object> comParkMap = pService.getMap("select id,berthsec_id,comid from com_park_tb " +
				"where is_delete=? and  dici_id =(select id from dici_tb where did=? and is_delete=? )  ", 
				new Object[]{0,paramMap.get("sensornumber"), 0});
		Long uin = -1L;
		if(comParkMap!=null&&!comParkMap.isEmpty()){
			//��ѯ�شŶ�Ӧ�Ĳ�λ��Ӧ�Ĳ�λ�ζ�Ӧ���ϸ��շ�Ա
			sql ="select uid from parkuser_work_record_tb where state=? and start_time>? and  berthsec_id =? ";
//					"(select berthsec_id from com_park_tb where is_delete=? and dici_id ="+
//					"(select id from dici_tb where did= ?))";
			Map userMap = pService.getMap(sql, new Object[]{0,0,comParkMap.get("berthsec_id")});
			//logger.error("InsertCarEntrance find user:"+userMap);
			String pid =comParkMap.get("id")+"";	
//			if(comParkMap!=null&&comParkMap.get("id")!=null){
//				pid = comParkMap.get("id")+"";
//			}
			//logger.error("userMap:"+userMap);
			if(userMap!=null&&userMap.get("uid")!=null){//�鵽���շ�Ա
				uin = (Long)userMap.get("uid");
				if(ret==1&&uin!=null&&uin>0){
					Long monthTime = TimeTools.getMonthStartSeconds();
					Long ntime = System.currentTimeMillis()/1000;
					if(ntime-monthTime<10*86400)
						monthTime = ntime-10*86400;
					ret = daService.update("update berth_order_tb set out_uid =? where in_time>? and berth_id=? and indicate=? ", 
							new Object[]{uin,monthTime,paramMap.get("sensornumber"),paramMap.get("indicate")});
					//logger.error("InsertCarEntrance >>>>���³����շ�Ա���شŶ�����"+ret);
					putMesgToCache("10",uin,pid,"0",bid,memcacheUtils);
					paramMap.put("uid",uin+"");
				}
			}else {//��Ϣ����λ��
//				sql ="select berthsec_id from com_park_tb where is_delete=?  and dici_id ="+
//						"(select id from dici_tb where did= ?)";
//				Map berthMap = daService.getMap(sql, new Object[]{0,paramMap.get("sensornumber")});
				//logger.error("InsertCarEntrance no user write to berthsecid:"+comParkMap.get("berthsec_id"));
				//if(berthMap!=null&&berthMap.get("berthsec_id")!=null){//�ҵ��˲�λ��
				Long berthId = (Long)comParkMap.get("berthsec_id");
				if(berthId!=null&&berthId>0){
					putMesgToCache("10",berthId,pid,"0",bid,memcacheUtils);
					paramMap.put("berthsecid",berthId+"");
				}
				//û���շ�Ա�ڸھͰѰ󶨵�POS��δ���㶩����Ϊ�ӵ�
				escape(bid, total, pService, commonMethods);
			}
		}else {
			logger.error("InsertCarEntrance...�ش�û�а󶨳�λ..."+paramMap);
		}
		paramMap.put("type", "out");
		writeToMongodb("zld_hdinout_logs", paramMap,comId.longValue());
		//logger.error("InsertCarEntrance write to mongodb result :"+result+",paramsMap :"+paramMap);
		logger.error(">>>>>>>��������>>>>>>>>>>�������.....");
		AjaxUtil.ajaxOutput(response, StringUtils.createXML1(paramMap));
	}
	/**
	 * ��������λ
	 * http://127.0.0.1/zld/api/hdinfo/SensorReset
	 */
	@POST
	@Path("/SensorReset")//��������λ
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)       
	public void sensorReset(String params,
			@Context ServletContext context,
			@Context HttpServletResponse response)throws IOException {
		Map<String, String> paramMap = ZldUploadUtils.stringToMap(params);
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
		DataBaseService daService = (DataBaseService) ctx.getBean("dataBaseService");
		//TransmitterNumber  ��վ���
		//SensorNumber ���������
		int ret = daService.update("update dici_tb set beart_time=? where did=? ", new Object[]{System.currentTimeMillis()/1000,paramMap.get("Sensornumber")});
		logger.error("SensorReset update dici ret:"+ret);
		ret =daService.update("update sites_tb set heartbeat=?,state=? where uuid=? ", new Object[]{
				System.currentTimeMillis()/1000,1,paramMap.get("transmitternumber")});
		logger.error("SensorReset update sites_tb ret:"+ret);
		Integer comid=-1;
		if(ret==1){
			Map diciMap = daService.getMap("select comid from dici_tb where did = ? and is_delete=? ",
					new Object[]{paramMap.get("transmitternumber"), 0});
			if(diciMap!=null&&diciMap.get("comid")!=null){
				comid = (Integer)diciMap.get("comid");
			}
		}
		writeToMongodb("zld_hdreset_logs", paramMap,comid.longValue());
		//logger.error("SensorReset write to mongodb result :"+result+",paramsMap :"+paramMap);
		AjaxUtil.ajaxOutput(response, StringUtils.createXML1(paramMap));
	}

	/**
	 * д��Ϣ������
	 * @param mesgtype ��Ϣ���� 10��λ��Ϣ���ɵشŷ���
	 * @param key ����Ϣ�˱�ţ����շ�Ա�ڸ�ʱ�����շ�Ա�������ڸ�ʱ������λ�Σ��շ�Ա�ϸں��յ�
	 * @param mesgKey ��Ϣkey
	 * @param putmesg ��Ϣ����
	 * @param memcacheUtils 
	 */
	private void putMesgToCache(String mesgtype,Long key,String mesgKey,String putmesg,Long bid,MemcacheUtils memcacheUtils){
		Map<Long, String>  messCacheMap = memcacheUtils.doMapLongStringCache("parkuser_messages", null, null);
		String mesg = null;
		if(messCacheMap!=null){
			mesg=messCacheMap.get(key);
			//messCacheMap.remove(-1L);
		}else {
			messCacheMap = new HashMap<Long, String>();
		}
		//System.err.println("curr cache:"+messCacheMap);
		boolean iscached = false;
		if(mesg!=null){
			Map<String, Object> messageMap = ParseJson.jsonToMap(mesg);
			if(messageMap!=null&&!messageMap.isEmpty()){
				String mtype = (String)messageMap.get("mtype");
				if(mtype!=null&&mtype.equals(mesgtype)){//��λ��Ϣ
					List<Map<String, Object>> messageList =(List<Map<String, Object>>) messageMap.get("mesgs");
					if(messageList!=null&&!messageList.isEmpty()){
						for(Map<String, Object> msgMap: messageList){
							String parkid = (String)msgMap.get("id");//��λ���
							if(parkid!=null&&parkid.equals(mesgKey)){//�Ѿ����������λ����Ϣ��ɾ��
								messageList.remove(msgMap);
								break;
							}
						}
						Map<String, Object> parkMesgMap = new HashMap<String, Object>();
						parkMesgMap.put("id", mesgKey);
						parkMesgMap.put("state", putmesg);
						parkMesgMap.put("berthorderid", bid);
						//���뵱ǰ��λ״̬��Ϣ
						messageList.add(parkMesgMap);
						String jsonMesg = "{\"mtype\":\""+mesgtype+"\",\"mesgs\": "+StringUtils.createJson(messageList)+"}";
						messCacheMap.put(key, jsonMesg);
						System.err.println("savr cache:"+key+"="+jsonMesg);
						memcacheUtils.doMapLongStringCache("parkuser_messages", messCacheMap, "update");
						iscached = true;
					}
				}
			}
		}
		if(!iscached){
			List<Map<String, Object>> messageList = new ArrayList<Map<String,Object>>();
			Map<String, Object> parkMesgMap = new HashMap<String, Object>();
			parkMesgMap.put("id", mesgKey);
			parkMesgMap.put("state", putmesg);
			parkMesgMap.put("berthorderid", bid);
			//���뵱ǰ��λ״̬��Ϣ
			messageList.add(parkMesgMap);
			String jsonMesg ="{\"mtype\":\""+mesgtype+"\",\"mesgs\": "+StringUtils.createJson(messageList)+"}";
			messCacheMap.put(key, jsonMesg);
			System.err.println("savr cache:"+key+"="+jsonMesg);
			memcacheUtils.doMapLongStringCache("parkuser_messages", messCacheMap, "update");
		}
	}
	
	private void writeToMongodb(String dbName,Map<String, String> paramMap,Long comId){
		WriteResult result =null;
		try {
			DB mydb = MongoClientFactory.getInstance().getMongoDBBuilder("zld");
			mydb.requestStart();
			DBCollection collection = mydb.getCollection(dbName);
			BasicDBObject object = new BasicDBObject();
			for(String key : paramMap.keySet()){
				object.put(key, paramMap.get(key));
			}
			object.put("comid", comId);
			object.put("ctime", System.currentTimeMillis()/1000);
			mydb.requestStart();
			result = collection.insert(object);
			  //��������
			mydb.requestDone();
		} catch (Exception e) {
			logger.error("sensor write to monbodb error...."+result);
			e.printStackTrace();
		}
	}
	
	private Long isHave(String dbName,Map<String, String> paramMap){
		Long count = 0L;
		try {
			DB mydb = MongoClientFactory.getInstance().getMongoDBBuilder("zld");
			mydb.requestStart();
			DBCollection collection = mydb.getCollection(dbName);
			BasicDBObject object = new BasicDBObject();
			for(String key : paramMap.keySet()){
				object.put(key, paramMap.get(key));
			}
			mydb.requestStart();
			count =  collection.count(object);
			  //��������
			mydb.requestDone();
		} catch (Exception e) {
			logger.error("sensor query  monbodb count error...."+paramMap);
			e.printStackTrace();
			return count;
		}
		return count;
	}
	
	private void escape(Long bid, Double total,PgOnlyReadService daService, CommonMethods commonMethods){
		logger.error("bid:"+bid+",total:"+total);
		try {
			if(bid != null && bid > 0){
				Map<String, Object> orderMap = daService.getMap("select o.id,o.uid from berth_order_tb b,order_tb o " +
						"where b.orderid=o.id and b.id=? and b.orderid>? and o.state=? ", new Object[]{bid, 0, 0});
				if(orderMap != null){
					Long orderid = (Long)orderMap.get("id");
					Long uid = (Long)orderMap.get("uid");
					boolean b = commonMethods.escape(orderid, uid, total, -1L);
					logger.error("handleEscape>>>orderid:"+orderid+",bid:"+bid+",total:"+total+",b:"+b);
				}
			}
		} catch (Exception e) {
			logger.error("handleEscape>>>bid:"+bid+",total:"+total,e);
		}
	}
}
