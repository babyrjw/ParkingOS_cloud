package com.zld.struts.parkadmin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.beans.factory.annotation.Autowired;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.zld.AjaxUtil;
import com.zld.impl.MongoClientFactory;
import com.zld.impl.MongoDbUtils;
import com.zld.impl.PublicMethods;
import com.zld.service.DataBaseService;
import com.zld.service.PgOnlyReadService;
import com.zld.utils.Check;
import com.zld.utils.ExportExcelUtil;
import com.zld.utils.JsonUtil;
import com.zld.utils.RequestUtil;
import com.zld.utils.SqlInfo;
import com.zld.utils.StringUtils;
import com.zld.utils.TimeTools;
import com.zld.utils.ZLDType;
/**
 * ͣ������̨����Ա��¼�󣬲鿴�����������޸ĺ�ɾ��
 * @author Administrator
 *
 */
public class OrderManageAction extends Action{
	
	@Autowired
	private DataBaseService daService;
	@Autowired
	private PgOnlyReadService pgOnlyReadService;
	@Autowired
	private PublicMethods PublicMethods;
	@Autowired
	private MongoDbUtils mongoDbUtils;
	private Logger logger = Logger.getLogger(OrderManageAction.class);

	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String action = RequestUtil.processParams(request, "action");
		Long comid = (Long)request.getSession().getAttribute("comid");
		Integer role = RequestUtil.getInteger(request, "role",-1);
		Integer authId = RequestUtil.getInteger(request, "authid",-1);
		request.setAttribute("authid", authId);
		request.setAttribute("role", role);
		Integer otype = RequestUtil.getInteger(request, "otype", -1);
		Integer isHd = (Integer)request.getSession().getAttribute("ishdorder");
		Long groupid = (Long)request.getSession().getAttribute("groupid");
		if(comid == null){
			response.sendRedirect("login.do");
			return null;
		}
		if(comid==0)
			comid = RequestUtil.getLong(request, "comid", 0L);
		if(groupid != null && groupid > 0){
			request.setAttribute("groupid", groupid);
			if(comid == null || comid <= 0){
				Map map = pgOnlyReadService.getMap("select id,company_name from com_info_tb where groupid=? order by id limit ? ", 
						new Object[]{groupid, 1});
				if(map != null){
					comid = (Long)map.get("id");
				}else{
					comid = -999L;
				}
			}
		}
		if(action.equals("")){
			request.setAttribute("comid", comid);
			initQuery(request,comid,isHd);
			return mapping.findForward("list");
		}else if(action.equals("query")){
			List arrayList = query(request,comid,isHd,otype);
			List<Map<String, Object>> list = (List<Map<String, Object>>) arrayList.get(0);
			Integer pageNum = (Integer) arrayList.get(1);
			long count = Long.valueOf(arrayList.get(2)+"");
			String fieldsstr = arrayList.get(3)+"";
			String json = JsonUtil.Map2Json(list,pageNum,count, fieldsstr,"id");
			AjaxUtil.ajaxOutput(response, json);
			return null;
			
		}else if(action.equals("exportExcel")){
			Map uin = (Map)request.getSession().getAttribute("userinfo");
			if(uin!=null&&uin.get("auth_flag")!=null){
				if(Integer.valueOf(uin.get("auth_flag")+"")==ZLDType.ZLD_ACCOUNTANT_ROLE||Integer.valueOf(uin.get("auth_flag")+"")==ZLDType.ZLD_CARDOPERATOR){
					String ret = "û��Ȩ�޵�����������";
					logger.error(">>>>"+ret);
					AjaxUtil.ajaxOutput(response,ret);
					return null;
				}
			}
			List arrayList = query(request,comid,isHd,otype);
			List<Map<String, Object>> list = (List<Map<String, Object>>) arrayList.get(0);
			List<List<String>> bodyList = new ArrayList<List<String>>();
			String [] heards = null;
			if(list!=null&&list.size()>0){
				mongoDbUtils.saveLogs( request,0, 5, "��������������"+list.size()+"��");
				//setComName(list);
				String [] f = new String[]{"id","c_type","car_number","create_time","end_time","duration","pay_type","total","uid","state","isclick","in_passid","out_passid"};
				heards = new String[]{"���","������ʽ","���ƺ�","����ʱ��","����ʱ��","ʱ��","֧����ʽ","���","�տ���","״̬","���㷽ʽ","����ͨ��","����ͨ��"};
				Map<Long, String> uinNameMap = new HashMap<Long, String>();
				Map<Integer, String> passNameMap = new HashMap<Integer, String>();
				for(Map<String, Object> map : list){
					List<String> values = new ArrayList<String>();
					for(String field : f){
						Object v =map.get(field); 
						if(v==null)
							v="";
						if("uid".equals(field)){
							Long uid = -1L;
							if(Check.isLong(v+""))
								uid = Long.valueOf(v+"");
							if(uinNameMap.containsKey(uid))
								values.add(uinNameMap.get(uid));
							else{
								String name = getUinName(Long.valueOf(map.get(field)+""));
								values.add(name);
								uinNameMap.put(uid, name);
							}
						}else if("c_type".equals(field)){
							switch(Integer.valueOf(v+"")){//0:NFC,1:IBeacon,2:����   3ͨ������ 4ֱ�� 5�¿��û�
							case 0:values.add("NFCˢ��");break;
							case 1:values.add("Ibeacon");break;
							case 2:values.add("�ֻ�ɨ��");break;
							case 3:values.add("ͨ��ɨ��");break;
							case 4:values.add("ֱ��");break;
							case 5:values.add("�¿�");break;
							default:values.add("");
							}
						}else if("duration".equals(field)){
							Long start = (Long)map.get("create_time");
							Long end = (Long)map.get("end_time");
							if(start!=null&&end!=null){
								values.add(StringUtils.getTimeString(start, end));
							}else{
								values.add("");
							}
						}else if("pay_type".equals(field)){
							switch(Integer.valueOf(v+"")){//0:NFC,1:IBeacon,2:����   3ͨ������ 4ֱ�� 5�¿��û�
							case 0:values.add("�˻�֧��");break;
							case 1:values.add("�ֽ�֧��");break;
							case 2:values.add("�ֻ�֧��");break;
							case 3:values.add("����");break;
							case 4:values.add("����Ԥ֧���ֽ�");break;
							case 5:values.add("����Ԥ֧��������");break;
							case 6:values.add("����Ԥ֧���̼ҿ�");break;
							case 8:values.add("���");break;
							default:values.add("");
							}
						}else if("state".equals(field)){
							switch(Integer.valueOf(v+"")){//0:NFC,1:IBeacon,2:����   3ͨ������ 4ֱ�� 5�¿��û�
							case 0:values.add("δ֧��");break;
							case 1:values.add("��֧��");break;
							case 2:values.add("�ӵ�");break;
							default:values.add("");
							}
						}else if("isclick".equals(field)){
							switch(Integer.valueOf(map.get(field)+"")){//0:NFC,1:IBeacon,2:����   3ͨ������ 4ֱ�� 5�¿��û�
							case 0:values.add("ϵͳ����");break;
							case 1:values.add("�ֶ�����");break;
							default:values.add("");
							}
						}else if("in_passid".equals(field)||"out_passid".equals(field)){
							if(!"".equals(v.toString())&&Check.isNumber(v.toString())){
								Integer passId = Integer.valueOf(v.toString());
								if(passNameMap.containsKey(passId))
									values.add(passNameMap.get(passId));
								else {
									String passName = getPassName(comid, passId);
									values.add(passName);
									passNameMap.put(passId, passName);
								}
							}else{
								values.add("");
							}
						}else{
							if("create_time".equals(field)||"end_time".equals(field)){
								if(!"".equals(v.toString())){
									values.add(TimeTools.getTime_yyyyMMdd_HHmmss(Long.valueOf((v+""))*1000));
								}else{
									values.add("null");
								}
							}else{
								values.add(v+"");
							}
						}
					}
					bodyList.add(values);
				}
			}
			String fname = "��������" + com.zld.utils.TimeTools.getDate_YY_MM_DD();
			fname = StringUtils.encodingFileName(fname);
			java.io.OutputStream os;
			try {
				response.reset();
				response.setHeader("Content-disposition", "attachment; filename="
						+ fname + ".xls");
				response.setContentType("application/x-download");
				os = response.getOutputStream();
				ExportExcelUtil importExcel = new ExportExcelUtil("��������",
						heards, bodyList);
				importExcel.createExcelFile(os);
			} catch (IOException e) {
				e.printStackTrace();
			}
//			String json = "";
//			AjaxUtil.ajaxOutput(response, json);
			return null;
		}else if(action.equals("completezeroorder")){
			String ids =RequestUtil.processParams(request, "ids");
			int ret = 0;
			if(StringUtils.isNotNull(ids)){
				String[] idsarr = ids.split(",");
				long etime = System.currentTimeMillis()/1000;
				for (int i = 0; i < idsarr.length; i++) {
					long id = Long.valueOf(idsarr[i]);
					if(PublicMethods.isEtcPark(comid)){
						etime+=1;
						//���¿�
						ret=daService.update("update order_tb set total=?,pay_type=?,end_time=?,state=?,need_sync=? where id=? and state=? and c_type<>?", new Object[]{0.0,1,etime,1,4,id,0,5});
						if(ret>0){
							mongoDbUtils.saveLogs( request,0, 6, "�����ط������ĺ�̨0Ԫ������¿�������"+id );
							logger.error("�����ط������ĺ�̨0Ԫ������¿�������"+id +",���㷽ʽpay_type��1");
						}else {
							//�¿�
							ret=daService.update("update order_tb set total=?,pay_type=?,end_time=?,state=?,need_sync=? where id=? and state=? and c_type=?", new Object[]{0.0,3,etime,1,4,id,0,5});
							if(ret>0){
								mongoDbUtils.saveLogs( request,0, 6, "�����ط������ĺ�̨0Ԫ�����¿�������"+id);
								logger.error("�����ط������ĺ�̨��̨0Ԫ�����¿�������"+id +",���㷽ʽpay_type��3");
							}
						}
					}else{
						//���¿�
						ret=daService.update("update order_tb set total=?,pay_type=?,end_time=?,state=? where id=? and state=? and c_type<>?", new Object[]{0.0,1,etime,1,id,0,5});
						if(ret>0){
							mongoDbUtils.saveLogs( request,0, 6, "��̨0Ԫ������¿�������"+id );
							logger.error("��̨0Ԫ������¿�������"+id +",���㷽ʽpay_type��1");
						}else {
							//�¿�
							ret=daService.update("update order_tb set total=?,pay_type=?,end_time=?,state=? where id=? and state=? and c_type=?", new Object[]{0.0,3,etime,1,id,0,5});
							if(ret>0){
								mongoDbUtils.saveLogs( request,0, 6, "��̨0Ԫ�����¿�������"+id);
								logger.error("��̨0Ԫ�����¿�������"+id +",���㷽ʽpay_type��3");
							}
						}
					}
					
				}
			}
			AjaxUtil.ajaxOutput(response, ret+"");
		}else if(action.equals("edit")){
			String nickname =AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "nickname"));
			String strid =AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "strid"));
			String phone =RequestUtil.processParams(request, "phone");
			String mobile =RequestUtil.processParams(request, "mobile");
			String id =RequestUtil.processParams(request, "id");
			String sql = "update order_tb set nickname=?,strid=?,phone=?,mobile=? where uin=?";
			Object [] values = new Object[]{nickname,strid,phone,mobile,Long.valueOf(id)};
			int result = daService.update(sql, values);
			AjaxUtil.ajaxOutput(response, result+"");
		}else if(action.equals("delete")){
			String id =RequestUtil.processParams(request, "selids");
			String sql = "delete from user_info where id =?";
			Object [] values = new Object[]{Long.valueOf(id)};
			int result = daService.update(sql, values);
			AjaxUtil.ajaxOutput(response, result+"");
		}else if(action.equals("carpics")){
			Long orderid = RequestUtil.getLong(request, "orderid", -1L);
			DB db = MongoClientFactory.getInstance().getMongoDBBuilder("zld");
			DBCollection collection = db.getCollection("car_inout_pics");
			BasicDBObject document = new BasicDBObject();
			document.put("orderid", orderid);
			document.put("gate", 0);
//			DBCursor objsin = collection.find(document);
//			int insize = objsin.size();
//			objsin.close();
			Long insize  = collection.count(document);
			document.put("gate", 1);
//			DBCursor objsout = collection.find(document);
//			int outsize = objsout.size();
//			objsout.close();
			Long outsize =collection.count(document);
			if(insize==0&&outsize==0){//�鲻��ʱ������һ�ű�
				collection = db.getCollection("car_hd_pics");
				outsize =collection.count(document);
				document.put("gate", 0);
				insize  = collection.count(document);
				logger.error("mongodb>>>>>>>>>>>car_inout_pics����û�У���car_hd_pics���в�ѯ"+insize+","+outsize);
			}
			String inhtml = "<img src='carpicsup.do?action=downloadpic&comid=0&type=0&orderid="+orderid+"' id='p1' width='600px' height='600px'></img>";
			String outhtml = "<img src='carpicsup.do?action=downloadpic&comid=0&type=1&orderid="+orderid+"' id='p1' width='600px' height='600px'></img>";
			if(insize>1){
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i <insize ; i++) {
					sb.append("<img src='carpicsup.do?action=downloadpic&comid=0&type=0&currentnum="+i+"&orderid="+orderid+"' id='p1' width='600px' height='600px'></img>").append("<br/><br/>");
				}
				inhtml = sb.toString();
			}
			if(outsize>1){
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i <outsize ; i++) {
					sb.append("<img src='carpicsup.do?action=downloadpic&comid=0&type=1&currentnum="+i+"&orderid="+orderid+"' id='p1' width='600px' height='600px'></img>").append("<br/><br/>");
				}
				outhtml = sb.toString();
			}
		
			request.setAttribute("inhtml", inhtml);
			request.setAttribute("outhtml", outhtml);
//			request.setAttribute("orderid", orderid);
			return mapping.findForward("carpics");
		}else if(action.equals("escarpics")){
			Long orderid = RequestUtil.getLong(request, "orderid", -1L);
			DB db = MongoClientFactory.getInstance().getMongoDBBuilder("zld");
			DBCollection collection = db.getCollection("car_inout_pics");
			BasicDBObject document = new BasicDBObject();
			document.put("orderid", orderid);
			document.put("gate", 0);
//			DBCursor objsin = collection.find(document);
//			int insize = objsin.size();
//			objsin.close();
			
			Long insize  = collection.count(document);
			document.put("gate", 2);
			
//			DBCursor objseesc = collection.find(document);
//			int escsize = objseesc.size();
//			objseesc.close();
			
			Long escsize  = collection.count(document);
			if(insize==0&&escsize==0){//�鲻��ʱ������һ�ű�
				collection = db.getCollection("car_hd_pics");
				escsize =collection.count(document);
				document.put("gate", 0);
				insize  = collection.count(document);
				logger.error("mongodb>>>>>>>>>>>car_inout_pics����û�У���car_hd_pics���в�ѯ"+insize+","+escsize);
			}
			
			String inhtml = "<img src='carpicsup.do?action=downloadpic&comid=0&type=0&orderid="+orderid+"' id='p1' width='600px' height='600px'></img>";
			String eschtml = "<img src='carpicsup.do?action=downloadpic&comid=0&type=2&orderid="+orderid+"' id='p1' width='600px' height='600px'></img>";
			if(insize>1){
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i <insize ; i++) {
					sb.append("<img src='carpicsup.do?action=downloadpic&comid=0&type=0&currentnum="+i+"&orderid="+orderid+"' id='p1' width='600px' height='600px'></img>").append("<br/><br/>");
				}
				inhtml = sb.toString();
			}
			
			if(escsize>1){
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i <escsize ; i++) {
					sb.append("<img src='carpicsup.do?action=downloadpic&comid=0&type=2&currentnum="+i+"&orderid="+orderid+"' id='p1' width='600px' height='600px'></img>").append("<br/><br/>");
				}
				eschtml = sb.toString();
			}
			request.setAttribute("inhtml", inhtml);
			request.setAttribute("eschtml", eschtml);
//			request.setAttribute("orderid", orderid);
			return mapping.findForward("escarpics");
		}else if(action.equals("getalluser")){
			List<Map<String, Object>> tradsList = pgOnlyReadService.getAll("select id,nickname from user_info_tb where (comid=? or groupid=?) and state=? and auth_flag in(?,?)",
					new Object[]{comid,groupid,0,1,2});
			String result = "[{\"value_no\":\"-1\",\"value_name\":\"ȫ��\"}";
			if(tradsList!=null&&tradsList.size()>0){
				for(Map map : tradsList){
					result+=",{\"value_no\":\""+map.get("id")+"\",\"value_name\":\""+map.get("nickname")+"\"}";
				}
			}
			result+="]";
			AjaxUtil.ajaxOutput(response, result);
		}else if(action.equals("getfreereasons")){
			List<Map<String, Object>> tradsList = pgOnlyReadService.getAll("select id,name from free_reasons_tb where comid=? ",
					new Object[]{comid});
			String result = "[{\"value_no\":\"-1\",\"value_name\":\" \"}";
			if(tradsList!=null&&tradsList.size()>0){
				for(Map map : tradsList){
					result+=",{\"value_no\":\""+map.get("id")+"\",\"value_name\":\""+map.get("name")+"\"}";
				}
			}
			result+="]";
			AjaxUtil.ajaxOutput(response, result);
		}else if(action.equals("carpicsnew")){
			//�޸Ĳ鿴ͼƬ�ӿ�,���´�mongodb�л�ȡͼƬ����ʵ��ͼƬ������չʾ
			/*String orderid = RequestUtil.getString(request, "orderid");
			logger.error(">>>>>>>>>>>>>>>>>>>>>>������Ϣ��ʾ��ѯͼƬ�ӿڣ�carpicsnew");
			String inhtml = "<img src='carpicsup.do?action=getpicture&comid="+comid+"&typeNew=in&orderid="+orderid+"' id='p1' width='600px' height='600px'></img>";
			String outhtml = "<img src='carpicsup.do?action=getpicture&comid="+comid+"&typeNew=out&orderid="+orderid+"' id='p1' width='600px' height='600px'></img>";
			request.setAttribute("inhtml", inhtml);
			request.setAttribute("outhtml", outhtml);
			return mapping.findForward("carpics");*/
			String orderid = RequestUtil.getString(request, "orderid");
			DB db = MongoClientFactory.getInstance().getMongoDBBuilder("zld");
			//���ݶ�����Ų�ѯ��mongodb�д���Ķ�Ӧ������
			Map map = daService.getMap("select * from order_tb where order_id_local=? and comid=?", new Object[]{orderid,comid});
			String collectionName = "";
			if(map !=null && !map.isEmpty()){
				collectionName = (String) map.get("carpic_table_name");
			}
			DBCollection collection = db.getCollection("collectionName");
			BasicDBObject document = new BasicDBObject();
			document.put("parkid", String.valueOf(comid));
			document.put("orderid", orderid);
			document.put("gate", "in");
			Long insize  = collection.count(document);
			document.put("gate", "out");
			Long outsize =collection.count(document);
			String inhtml = "<img src='carpicsup.do?action=getpicture&comid="+comid+"&typeNew=in&orderid="+orderid+"' id='p1' width='600px' height='600px'></img>";
			String outhtml = "<img src='carpicsup.do?action=getpicture&comid="+comid+"&typeNew=out&orderid="+orderid+"' id='p1' width='600px' height='600px'></img>";
			if(insize>1){
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i <insize ; i++) {
					sb.append("<img src='carpicsup.do?action=getpicture&comid="+comid+"&typeNew=in&currentnum="+i+"&orderid="+orderid+"' id='p1' width='600px' height='600px'></img>").append("<br/><br/>");
				}
				inhtml = sb.toString();
			}
			if(outsize>1){
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i <outsize ; i++) {
					sb.append("<img src='carpicsup.do?action=getpicture&comid="+comid+"&typeNew=out&currentnum="+i+"&orderid="+orderid+"' id='p1' width='600px' height='600px'></img>").append("<br/><br/>");
				}
				outhtml = sb.toString();
			}
		
			request.setAttribute("inhtml", inhtml);
			request.setAttribute("outhtml", outhtml);
			return mapping.findForward("carpics");
		}
		
		return null;
	}
	
	private void initQuery(HttpServletRequest request,Long comid,Integer isHd){
		int total = 0;
		int month = 0;
		int parktotal = 0;
		int blank = 0;
		String allSql = "select count(*)total from order_tb where comid = ? and state=?";
		Object [] allparm =  new Object[]{comid,0};
		String monthSql = "select count(*)total from order_tb where comid = ? and state=?  ";
		Object [] monthparm =  new Object[]{comid,0};
		Map allmap = pgOnlyReadService.getMap(allSql,allparm);
		Map monthmap = pgOnlyReadService.getMap(monthSql,monthparm);
		Map cominfo = pgOnlyReadService.getMap("select share_number,parking_total from com_info_tb where id = ? ", new Object[]{comid});
		if(allmap!=null&&allmap.get("total")!=null)
			total = Integer.valueOf(allmap.get("total")+"");
		if(monthmap!=null&&monthmap.get("total")!=null)
			month = Integer.valueOf(monthmap.get("total")+"");
		if(cominfo!=null){
			Integer parking_total = 0;
			if(cominfo.get("parking_total") != null){
				parking_total=(Integer)cominfo.get("parking_total");//������λ��
			}
			Integer shareNumber = 0;
			if(cominfo.get("share_number") != null){
				shareNumber=(Integer)cominfo.get("share_number");//������λ������
			}
			if(shareNumber > 0){
				parktotal = shareNumber;
			}else{
				parktotal = parking_total;
			}
		}
		blank = parktotal-total;
		if(blank<=0)
			blank=0;
		request.setAttribute("parkinfo",  AjaxUtil.decodeUTF8("��λͳ��:����ͣ��"+total+"��,�����¿���"+month+"��,��ͣ��"+(total-month)+"��,�ճ�λ"+blank+"��"));
		
	}
	
	private String getUinName(Long uin) {
		Map list = pgOnlyReadService.getPojo("select * from user_info_tb where id =?  ",new Object[]{uin});
		String uinName = "";
		if(list!=null&&list.get("nickname")!=null){
			uinName = list.get("nickname")+"";
		}
		return uinName;
	}
	
	private String getPassName(Long comId,Integer passId) {
		String sql = "select passname from com_pass_tb where comid=? and id = ?";
		Map m = pgOnlyReadService.getPojo(sql, new Object[]{comId,passId});
		if(m!=null){
			return m.get("passname")+"";
		}
		return "";
	}
	
	private List query(HttpServletRequest request,long comid,Integer isHd,Integer otype){
		ArrayList arrayList = new ArrayList();
		String orderfield = RequestUtil.processParams(request, "orderfield");
		String orderby = RequestUtil.processParams(request, "orderby");
		String sql = "select * from order_tb where comid=?  ";
		if(orderfield.equals("")){
			orderfield = " end_time ";
		}else if(orderfield.equals("duration")){
			sql = "select *,(end_time-create_time) as duration from order_tb where comid=?  ";
		}
		if(orderby.equals("")){
			orderby = " desc nulls last ";
		}else {
			orderby +=" nulls last";
		}
		String countSql = "select count(*) from order_tb where  comid=? " ;
		Integer pageNum = RequestUtil.getInteger(request, "page", 1);
		Integer pageSize = RequestUtil.getInteger(request, "rp", 20);
		String fieldsstr = RequestUtil.processParams(request, "fieldsstr");
		SqlInfo base = new SqlInfo("1=1", new Object[]{comid});
		
		if(isHd==1){
			if(otype>-1){
				countSql +=" and ishd=? and state=? and isclick=? ";
				sql      +=" and ishd=? and state=? and isclick=? ";
				base = new SqlInfo("1=1", new Object[]{comid,0,1,otype});
			}else {
				countSql +=" and ishd=? ";
				sql      +=" and ishd=? ";
				base = new SqlInfo("1=1", new Object[]{comid,0});
			}
		}else {
			if(otype>-1){
				countSql +=" and state=? and isclick=? ";
				sql      +=" and state=? and isclick=? ";
				base = new SqlInfo("1=1", new Object[]{comid,1,otype});
			}
		}
		
		SqlInfo sqlInfo = RequestUtil.customSearch(request,"order_tb");
		List<Object> params =new ArrayList<Object>();
		
		boolean isSelectCreateTime = false;
		
		if(sqlInfo!=null){
			sqlInfo = SqlInfo.joinSqlInfo(base,sqlInfo, 2);
			countSql+=" and "+ sqlInfo.getSql();
			sql +=" and "+sqlInfo.getSql();
			if(sqlInfo.getSql().indexOf("create_time")!=-1){
				isSelectCreateTime = true;
			}
			params = sqlInfo.getParams();
		}else {
			params= base.getParams();
		}
		
		if(!isSelectCreateTime){
			countSql +=" and create_time > ? ";
			sql +=" and create_time > ? ";
			params.add(System.currentTimeMillis()/1000-30*86400);
		}
		
		sql += " order by " + orderfield + " " + orderby+" ,id desc";
		//System.out.println(sqlInfo);
		Long count= pgOnlyReadService.getCount(countSql, params);
		List<Map<String, Object>> list = null;//daService.getPage(sql, null, 1, 20);
		if(count>0){
			list = pgOnlyReadService.getAll(sql, params, pageNum, pageSize);
			List<Object> orderidList = new ArrayList<Object>();
			for(Map<String, Object> map : list){
				orderidList.add(map.get("id"));
				Integer isClick =(Integer)map.get("isclick");
				Integer state = (Integer)map.get("state");
				if(state==null||state==0){
					if(isClick==0)
						map.put("isclick", -1);
				}
			}
			List<Map<String, Object>> shopTicketList = queryShopTicket(orderidList);
			if(shopTicketList != null && !shopTicketList.isEmpty()){
				for(Map<String, Object> map : list){
					Long id = (Long)map.get("id");
					
					Double total = 0d;
					if(map.get("total") != null){
						total = Double.valueOf(map.get("total") + "");
					}
					for(Map<String, Object> map2 : shopTicketList){
						Long orderid = (Long)map2.get("orderid");
						Double shopmon = 0d;
						if(map2.get("shopmon") != null){
							shopmon = Double.valueOf(map2.get("shopmon") + "");
						}
						if(id.intValue() == orderid.intValue()){
							map.put("total", StringUtils.formatDouble(total + shopmon));
							break;
						}
					}
				}
			}
		}
		arrayList.add(list);
		arrayList.add(pageNum);
		arrayList.add(count);
		arrayList.add(fieldsstr);
		return arrayList;
	}
	
	private List<Map<String, Object>> queryShopTicket(List<Object> orderidList){
		if(orderidList != null && !orderidList.isEmpty()){
			String preParams  ="";
			for(Object orderid : orderidList){
				if(preParams.equals(""))
					preParams ="?";
				else
					preParams += ",?";
			}
			
			List<Map<String, Object>> list = pgOnlyReadService.getAllMap("select orderid,sum(umoney) shopmon from ticket_tb where orderid in ("
							+ preParams + ") group by orderid ", orderidList);
			return list;
			
		}
		return null;
	}
}