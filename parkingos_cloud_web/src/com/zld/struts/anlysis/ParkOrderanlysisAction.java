package com.zld.struts.anlysis;

import java.text.SimpleDateFormat;
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

import com.zld.AjaxUtil;
import com.zld.impl.CommonMethods;
import com.zld.service.DataBaseService;
import com.zld.service.PgOnlyReadService;
import com.zld.utils.JsonUtil;
import com.zld.utils.RequestUtil;
import com.zld.utils.SqlInfo;
import com.zld.utils.StringUtils;
import com.zld.utils.TimeTools;
import com.zld.utils.ZLDType;

/**
 * ��������ͳ��
 * @author Administrator
 *
 */
public class ParkOrderanlysisAction extends Action {

	@Autowired
	private DataBaseService daService;
	@Autowired
	private PgOnlyReadService pgOnlyReadService;
	@Autowired
	private CommonMethods commonMethods;
	
	private Logger logger = Logger.getLogger(ParkOrderanlysisAction.class);
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String action = RequestUtil.processParams(request, "action");
		Long comid = (Long)request.getSession().getAttribute("comid");
		Integer role = RequestUtil.getInteger(request, "role",-1);
		Long uin = (Long)request.getSession().getAttribute("loginuin");//��¼���û�id
		request.setAttribute("authid", request.getParameter("authid"));
		Integer isHd = (Integer)request.getSession().getAttribute("ishdorder");
		Long groupid = (Long)request.getSession().getAttribute("groupid");
		Long cityid = (Long)request.getSession().getAttribute("cityid");
		if(ZLDType.ZLD_ACCOUNTANT_ROLE==role||ZLDType.ZLD_CARDOPERATOR==role)
			request.setAttribute("role", role);
		if(uin == null){
			response.sendRedirect("login.do");
			return null;
		}
		
		if(comid == 0){
			comid = RequestUtil.getLong(request, "comid", 0L);
		}
		request.setAttribute("groupid", groupid);
		request.setAttribute("cityid", cityid);
		if(comid == 0){
			comid = getComid(comid, cityid, groupid);
		}
		if(action.equals("")){
			SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
			request.setAttribute("btime", df2.format(System.currentTimeMillis()));
			request.setAttribute("etime",  df2.format(System.currentTimeMillis()));
			request.setAttribute("comid", comid);
			return mapping.findForward("list");
		}else if(action.equals("query")){
			/*ԭ��ͳ�Ʒ����в�ѯ���շ�Ա��uid����Ϊ��ѯ�����շ�Աout_uid����Ϣ by lqb 2017-05-27*/
			/*
			 * 
			 */
			
			SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
			String nowtime= df2.format(System.currentTimeMillis());
			String type = RequestUtil.processParams(request, "type");
			String sql = "select count(*) scount,sum(amount_receivable) amount_receivable, " +
					"sum(total) total , sum(cash_pay) cash_pay, sum(electronic_pay+electronic_prepay) electronic_pay, " +
					"sum(reduce_amount) reduce_pay, out_uid from order_tb  ";
			String free_sql = "select count(*) scount,sum(amount_receivable-electronic_prepay-cash_prepay-reduce_amount) free_pay,out_uid from order_tb";
			String fieldsstr = RequestUtil.processParams(request, "fieldsstr");
			String btime = RequestUtil.processParams(request, "btime");
			String etime = RequestUtil.processParams(request, "etime");
			if(btime.equals(""))
				btime = nowtime;
			if(etime.equals(""))
				etime = nowtime;
			SqlInfo sqlInfo =null;
			List<Object> params = null;
			Long b = TimeTools.getToDayBeginTime();
			Long e = System.currentTimeMillis()/1000;
			String dstr = btime+"-"+etime;
			if(type.equals("today")){
				sqlInfo =new SqlInfo(" end_time between ? and ? ",
						new Object[]{b,e});
				dstr = "����";
			}else if(type.equals("toweek")){
				b = TimeTools.getWeekStartSeconds();
				sqlInfo =new SqlInfo(" end_time between ? and ? ",
						new Object[]{b,e});
				dstr = "����";
			}else if(type.equals("lastweek")){
				e = TimeTools.getWeekStartSeconds();
				b= e-7*24*60*60;
				e = e-1;
				sqlInfo =new SqlInfo(" end_time between ? and ? ",
						new Object[]{b,e});
				dstr = "����";
			}else if(type.equals("tomonth")){
				b=TimeTools.getMonthStartSeconds();
				sqlInfo =new SqlInfo(" end_time between ? and ? ",
						new Object[]{b,e});
				dstr="����";
			}else if(!btime.equals("")&&!etime.equals("")){
				b = TimeTools.getLongMilliSecondFrom_HHMMDD(btime)/1000;
				e =  TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(etime+" 23:59:59");
				sqlInfo =new SqlInfo(" end_time between ? and ? ",
						new Object[]{b,e});
			}
			sql +=" where "+sqlInfo.getSql()+" and comid=?  and state= ? and out_uid> ? and ishd=? ";
			free_sql +=" where "+sqlInfo.getSql()+" and comid=?  and state= ? and out_uid> ? and ishd=? ";
			List<Object> subParams =new ArrayList<Object>();
			params= sqlInfo.getParams();
			for(Object object :params){
				subParams.add(object);
			}
			params.add(comid);
			params.add(1);
			params.add(0);
			params.add(0);
			//�ܶ�������
			List<Map<String, Object>> totalList = pgOnlyReadService.getAllMap(sql +" group by out_uid order by scount desc ",params);
			//�¿���������
			List<Map<String, Object>> monthList = pgOnlyReadService.getAllMap(sql +" and pay_type=3 group by out_uid order by scount desc ",params);
			//��Ѷ�������
			List<Map<String, Object>> freeList = pgOnlyReadService.getAllMap(free_sql +" and pay_type=8 group by out_uid order by scount desc ",params);
			int totalCount = 0;//�ܶ�����
			int monthCount = 0;
			double totalMoney = 0.0;//�������
			double cashMoney = 0.0;//�ֽ�֧�����
			double elecMoney = 0.0;//����֧�����
			double freeMoney = 0.0;//��ѽ��
			double reduce_amount = 0.0;//����֧��
			List<Map<String, Object>> backList = new ArrayList<Map<String, Object>>();
			if(totalList != null && totalList.size() > 0){
				for(Map<String, Object> totalOrder : totalList){
					totalCount += Integer.parseInt(totalOrder.get("scount")+"");
					totalMoney += Double.parseDouble(totalOrder.get("amount_receivable")+"");
					//�趨Ĭ��ֵ
					String sql_worker = "select nickname from user_info_tb where id = ?";
					Object []val_worker = new Object[]{Long.parseLong(totalOrder.get("out_uid")+"")};
					Map worker = daService.getMap(sql_worker ,val_worker);
					if(worker!=null && worker.containsKey("nickname")){
						//�����շ�ԱId
						totalOrder.put("id",totalOrder.get("out_uid"));
						//�շ�Ա����
						totalOrder.put("name",worker.get("nickname"));
					}
					//ʱ���
					totalOrder.put("sdate",dstr);
					//�¿�������
					totalOrder.put("monthcount",0);
					//�����¿�����
					if(monthList != null && monthList.size() > 0){
						for(Map<String, Object> monthOrder : monthList){
							if(totalOrder.get("out_uid").equals(monthOrder.get("out_uid"))){
								monthCount += Integer.parseInt(monthOrder.get("scount")+"");
								totalOrder.put("monthcount", monthOrder.get("scount"));
							}
						}
					}
					cashMoney += Double.parseDouble((totalOrder.get("cash_pay")== null ? "0" : totalOrder.get("cash_pay")+""));
					//����֧��
					totalOrder.put("electronic_pay", StringUtils.formatDouble(Double.parseDouble((totalOrder.get("electronic_pay")== null ? "0" : totalOrder.get("electronic_pay")+""))));
					elecMoney += Double.parseDouble((totalOrder.get("electronic_pay")== null ? "0" : totalOrder.get("electronic_pay")+""));
					//���֧��
					totalOrder.put("free_pay",0.0);
					//������Ѽ���
					if(freeList != null && freeList.size() > 0){
						for(Map<String, Object> freeOrder : freeList){
							if(totalOrder.get("out_uid").equals(freeOrder.get("out_uid"))){
								freeMoney += Double.parseDouble((freeOrder.get("free_pay")== null ? "0" : freeOrder.get("free_pay")+""));
								totalOrder.put("free_pay", StringUtils.formatDouble(Double.parseDouble((freeOrder.get("free_pay")== null ? "0" : freeOrder.get("free_pay")+""))));
							}
						}
					}
					reduce_amount += Double.parseDouble((totalOrder.get("reduce_pay")== null ? "0" : totalOrder.get("reduce_pay")+""));
					backList.add(totalOrder);
				}
			}
			
			
			String money = "�ܶ�������"+totalCount+",�¿�������:"+monthCount+",�������:"+StringUtils.formatDouble(totalMoney)+"Ԫ," +
							"�ֽ�֧��:"+StringUtils.formatDouble(cashMoney)+"Ԫ,����֧�� :"+StringUtils.formatDouble(elecMoney)+"Ԫ," +
							"��ѽ��:"+StringUtils.formatDouble(freeMoney)+"Ԫ,����֧��:"+StringUtils.formatDouble(reduce_amount)+"Ԫ";
			String json = JsonUtil.anlysisMap2Json(backList,1,backList.size(), fieldsstr,"id",money);
			System.out.println(json);
			AjaxUtil.ajaxOutput(response, json);
			return null;
		}else if(action.equals("detail")){
			requestUtil(request);
			return mapping.findForward("detail");
		}else if(action.equals("work")){
			requestUtil(request);
			return mapping.findForward("work");
		}else if(action.equals("workdetail")){
			String bt = AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "btime"));
			String et = RequestUtil.processParams(request, "etime");
			String fieldsstr = RequestUtil.processParams(request, "fieldsstr");
			String pay_type = RequestUtil.processParams(request, "pay_type");
			String type = RequestUtil.processParams(request, "otype");
			Long btime = TimeTools.getToDayBeginTime();
			Long etime = System.currentTimeMillis()/1000;
			List list = null;//daService.getPage(sql, null, 1, 20);
			List freeorder = null;
			if(type.equals("today")){
			}else if(type.equals("toweek")){
				btime = TimeTools.getWeekStartSeconds();
			}else if(type.equals("lastweek")){
				etime = TimeTools.getWeekStartSeconds();
				btime= etime-7*24*60*60;
				etime = etime-1;
			}else if(type.equals("tomonth")){
				btime=TimeTools.getMonthStartSeconds();
			}else if(type.equals("custom")){
				btime = TimeTools.getLongMilliSecondFrom_HHMMDD(bt)/1000;
				etime =  TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(et+" 23:59:59");
			}else if(!bt.equals("")&&!et.equals("")){
				btime = TimeTools.getLongMilliSecondFrom_HHMMDD(bt)/1000;
				etime =  TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(et+" 23:59:59");
			}
			long uid = RequestUtil.getLong(request, "uid", -1L);
			String sql = "select a.id,a.start_time,a.end_time,a.uid,b.worksite_name worksite_id from parkuser_work_record_tb a ,com_worksite_tb b where (a.end_time " +
					"between ? and ? or a.start_time between ? and ? or (a.start_time between ? and ? and (a.end_time>? or a.end_time is null))) and a.uid = ? and b.id=a.worksite_id ";// order by a.end_time desc";//��ѯ�ϰ���Ϣ
			List<Object> params = new ArrayList();
			params.add(btime);
			params.add(etime);
			params.add(btime);
			params.add(etime);
			params.add(btime);
			params.add(etime);
			params.add(etime);
			params.add(uid);
			sql +=" order by a.end_time desc";
			list = daService.getAllMap(sql,params);
			
			double amountmoney = 0.0;//�ܽ��
			double cash_money = 0.0;//�ֽ�֧�����
			double elec_money = 0.0;//����֧�����
			double reduce_money = 0.0;//����֧�����
			double free_money = 0.0;//����֧�����
			int count =0;
			int monthcount =0;
			for (int i = 0; i < list.size(); i++) {//ѭ����֯ÿ�����ͳ��
				List<Object> p = new ArrayList(); 
				Map work = (Map)list.get(i);
				long start_time = (Long)work.get("start_time");
				long end_time = Long.MAX_VALUE;
				try {
					end_time = (Long)work.get("end_time");
				} catch (Exception e) {
				}
				p.add(start_time);
				p.add(end_time);
				p.add(1);
				p.add(uid);
				p.add(0);
				p.add(comid);
				List list2 = new ArrayList();//�ܵĶ���
				List list3 = new ArrayList();//���
				List list4 = new ArrayList();//�ֽ�

					//�ܵĶ��������ܵĽ��
				String sql2 = "select count(*) ordertotal,sum(amount_receivable) amount_receivable, " +
						"sum(total) total , sum(cash_pay) cash_pay, sum(electronic_pay+electronic_prepay) electronic_pay, " +
						"sum(reduce_amount) reduce_pay from order_tb where end_time between ? and ? " +
						" and state= ? and out_uid = ? and ishd=? and comid=?";
				list2 = daService.getAllMap(sql2 ,p);
				//�¿�������
				String sql5 = "select count(*) ordertotal from order_tb where end_time between ? and ? " +
						" and state= ? and out_uid = ? and pay_type =? and ishd=? and comid=?";
				Object []v5 = new Object[]{start_time,end_time,1,uid,3,0,comid};
				Map list5 = daService.getMap(sql5 ,v5);
				work.put("monthcount", list5.get("ordertotal"));
				monthcount+=Integer.parseInt(list5.get("ordertotal")+"");
				count+=Integer.parseInt((((Map)list2.get(0)).get("ordertotal"))+"");
				if(list2!=null&&list2.size()==1){
					int ordertotal = 0;
					double totalMOney = 0 ;
					try{
						//amount_receivable = Double.parseDouble((((Map)list2.get(0)).get("amount_receivable"))+"");
						ordertotal = Integer.parseInt((((Map)list2.get(0)).get("ordertotal"))+"");
						totalMOney = Double.parseDouble((((Map)list2.get(0)).get("amount_receivable"))+"");
						
					}catch (Exception e) {
						totalMOney=0.0;
					}
					//work.put("amount_receivable",StringUtils.formatDouble(amount_receivable));
					work.put("ordertotal",ordertotal);
					work.put("total",StringUtils.formatDouble(totalMOney));
					amountmoney+=totalMOney;
					//�ֽ�֧��
					work.put("cash_pay",StringUtils.formatDouble(Double.parseDouble((((Map)list2.get(0)).get("cash_pay")== null ? "0" : ((Map)list2.get(0)).get("cash_pay")+""))));
					cash_money += Double.parseDouble((((Map)list2.get(0)).get("cash_pay")== null ? "0" : ((Map)list2.get(0)).get("cash_pay")+""));
					//����֧��
					elec_money += Double.parseDouble((((Map)list2.get(0)).get("electronic_pay")== null ? "0" : ((Map)list2.get(0)).get("electronic_pay")+""));
					work.put("electronic_pay", StringUtils.formatDouble(Double.parseDouble((((Map)list2.get(0)).get("electronic_pay")== null ? "0" : ((Map)list2.get(0)).get("electronic_pay")+""))));
					//���℻֧��
					reduce_money += Double.parseDouble((((Map)list2.get(0)).get("reduce_pay")== null ? "0" : ((Map)list2.get(0)).get("reduce_pay")+""));
					work.put("reduce_pay", StringUtils.formatDouble(Double.parseDouble((((Map)list2.get(0)).get("reduce_pay")== null ? "0" : ((Map)list2.get(0)).get("reduce_pay")+""))));
				}
				//��Ѷ�������
				String sql6 = "select sum(amount_receivable-electronic_prepay-cash_prepay-reduce_amount) free_pay from order_tb where end_time between ? and ? " +
						" and state= ? and out_uid = ? and pay_type =? and ishd=? and comid=?";
				Object []v6 = new Object[]{start_time,end_time,1,uid,8, 0, comid};
				Map list6 = daService.getMap(sql6 ,v6);
				//���֧��
				free_money += Double.parseDouble((list6.get("free_pay")== null ? "0" : list6.get("free_pay")+""));
				work.put("free_pay", StringUtils.formatDouble(Double.parseDouble(list6.get("free_pay")== null ? "0" : (list6.get("free_pay")+""))));
			}
			String title = "�ܶ�������"+count+"���¿���������"+monthcount+"���ܽ����"+StringUtils.formatDouble(amountmoney)+"Ԫ�������ֽ�֧����"+StringUtils.formatDouble(cash_money)+"Ԫ������֧�� ��"+StringUtils.formatDouble(elec_money)+"Ԫ��" +
							"��ѽ�"+StringUtils.formatDouble(free_money)+"Ԫ,����֧����"+StringUtils.formatDouble(reduce_money)+"Ԫ";
			String ret = JsonUtil.anlysisMap2Json(list,1,list.size(), fieldsstr,"id",title);
			logger.error(ret);
			AjaxUtil.ajaxOutput(response, ret);
			return null;			
		}else if(action.equals("orderdetail")){
			String sql = "select *,(amount_receivable-electronic_prepay-cash_prepay-reduce_amount) free_pay from order_tb  ";
			//ͳ���ܶ������������Ӻ��ֽ�֧��
			String sql2 = "select count(*) ordertotal,sum(amount_receivable) amount_receivable, " +
					"sum(total) total , sum(cash_pay) cash_pay, sum(electronic_pay) electronic_pay, " +
					"sum(electronic_prepay) electronic_prepay,sum(reduce_amount) reduce_pay from order_tb";
			Long uid = RequestUtil.getLong(request, "uid", -2L);
			List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
			String fieldsstr = RequestUtil.processParams(request, "fieldsstr");
			String btime = AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "btime"));
			String etime = RequestUtil.processParams(request, "etime");
			String type = RequestUtil.processParams(request, "otype");
			SqlInfo sqlInfo =null;
			List<Object> params = null;
			Long b = TimeTools.getToDayBeginTime();
			Long e = System.currentTimeMillis()/1000;
			if(type.equals("today")){
				sqlInfo =new SqlInfo(" end_time between ? and ? ",
						new Object[]{b,e});
			}else if(type.equals("toweek")){
				b = TimeTools.getWeekStartSeconds();
				sqlInfo =new SqlInfo(" end_time between ? and ? ",
						new Object[]{b,e});
			}else if(type.equals("lastweek")){
				e = TimeTools.getWeekStartSeconds();
				b= e-7*24*60*60;
				e = e-1;
				sqlInfo =new SqlInfo(" end_time between ? and ? ",
						new Object[]{b,e});
			}else if(type.equals("tomonth")){
				b=TimeTools.getMonthStartSeconds();
				sqlInfo =new SqlInfo(" end_time between ? and ? ",
						new Object[]{b,e});
			}else if(type.equals("workcustom")){
				b = TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(btime);
				e =  TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(etime);
				sqlInfo =new SqlInfo(" end_time between ? and ? ",
						new Object[]{b,e});
			}else if(!btime.equals("")&&!etime.equals("")){
				b = TimeTools.getLongMilliSecondFrom_HHMMDD(btime)/1000;
				e =  TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(etime+" 23:59:59");
				sqlInfo =new SqlInfo(" end_time between ? and ? ",
						new Object[]{b,e});
			}
			
			sql +=" where "+sqlInfo.getSql()+" and out_uid=?  and state= ? and comid=? and ishd=?  ";
			sql2 +=" where "+sqlInfo.getSql()+" and out_uid=?  and state= ? and comid=? and ishd=?  ";
			params= sqlInfo.getParams();
			params.add(uid);
			params.add(1);
			params.add(comid);
			params.add(0);
			
			
			double amountmoney = 0.0;//�ܽ��
			double cash_money = 0.0;//�ֽ�֧�����
			double elec_money = 0.0;//����֧�����
			int count =0;
			
			if(uid!=-2){
				List<Map<String, Object>> orders = daService.getAllMap(sql+"order by end_time desc",params);
				for(Map<String, Object> order : orders){
					Map<String, Object> work = new HashMap<String, Object>();
					//��� 
					work.put("id", order.get("id"));
					//ͣ������ 
					work.put("create_time", order.get("create_time"));
					//�������� 
					work.put("end_time", order.get("end_time"));
					//�������  	
					work.put("total", order.get("amount_receivable"));
					//�ֽ�֧��  order.get("amount_receivable")
					work.put("cashMoney", order.get("cash_pay"));
					//����֧��  
					work.put("elecMoney", Double.parseDouble((order.get("electronic_pay")== null ? "0" : order.get("electronic_pay")+""))
								+ Double.parseDouble((order.get("electronic_prepay")== null ? "0" : order.get("electronic_prepay")+"")));
					//�¿�          
					//work.put("monthCard", order.get(""));
					//���֧��  
					work.put("freeMoney",0.0);
					if(order.get("pay_type")!=null && Integer.parseInt(order.get("pay_type")+"")==8){
						work.put("freeMoney", StringUtils.formatDouble(Double.parseDouble(order.get("free_pay")== null ? "0" : (order.get("free_pay")+""))));
					}
					//����֧��
					work.put("reduceMoney", order.get("reduce_amount"));
					//ͣ��ʱ��  
					work.put("duration", order.get("duration"));
					//֧����ʽ  
					work.put("pay_type", order.get("pay_type"));
					//NFC����  
					work.put("nfc_uuid", order.get(""));
					//���ƺ�  
					work.put("car_number", order.get("car_number"));
					//�鿴����ͼƬ  
					work.put("order_id_local", order.get("order_id_local"));
					list.add(work);
				}
				List<Map<String, Object>> orderList = daService.getAllMap(sql2,params);
				if(orderList!=null && orderList.size()>0){
					Map<String, Object> map = orderList.get(0);
					amountmoney = Double.parseDouble((map.get("amount_receivable"))+"");
					cash_money = Double.parseDouble((map.get("cash_pay"))+"");
					elec_money = Double.parseDouble((map.get("electronic_pay"))+"");
					count+=Integer.parseInt((map.get("ordertotal"))+"");
				}
				String title = StringUtils.formatDouble(amountmoney)+"Ԫ�������ֽ�֧����"+StringUtils.formatDouble(cash_money)+"Ԫ��" +
							   "����֧�� ��"+StringUtils.formatDouble(elec_money)+"Ԫ����"+count+"��";
				String json = JsonUtil.anlysisMap2Json(list,1,list.size(), fieldsstr,"id",title);
				//String json = JsonUtil.Map2Json(list,1,count, fieldsstr,"id");
				AjaxUtil.ajaxOutput(response, json);
				return null;
			}else {
				AjaxUtil.ajaxOutput(response, "{\"page\":1,\"total\":0,\"rows\":[]}");
			}
		}
		return null;
	}

	private String setName(List list,String dstr){
		List<Object> uins = new ArrayList<Object>();
		String total_count="";
		Double total = 0d;
		Long count = 0l;
		if(list!=null&&list.size()>0){
			for(int i=0;i<list.size();i++){
				Map map = (Map)list.get(i);
				uins.add(map.get("out_uid"));
				Double t = Double.valueOf(map.get("total")+"");
				Long c = (Long)map.get("scount");
				map.put("sdate", dstr);
				map.put("total", StringUtils.formatDouble(t));
				total+=t;
				count+=c;
			}
		}
		if(!uins.isEmpty()){
			String preParams  ="";
			for(Object uin : uins){
				if(preParams.equals(""))
					preParams ="?";
				else
					preParams += ",?";
			}
			List<Map<String, Object>> resultList = daService.getAllMap("select id,nickname  " +
					"from user_info_tb " +
					" where id in ("+preParams+") ", uins);
			if(resultList!=null&&!resultList.isEmpty()){
				for(int i=0;i<list.size();i++){
					Map map1 = (Map)list.get(i);
					for(Map<String,Object> map: resultList){
						Long uin = (Long)map.get("id");
						if(map1.get("out_uid").equals(uin)){
							map1.put("name", map.get("nickname"));
							break;
						}
					}
				}
			}
		}
		return total+"_"+count;
	}
	
	private void setList(List<Map<String, Object>> lists,String dstr){
		List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();
		List<Long> comidList = new ArrayList<Long>();
		for(Map<String, Object> map :lists){
			//Long comId = (Long)map.get("comid");
			//Integer state = (Integer)map.get("state");
			map.put("sdate", dstr);
//			if(state==1){
//				comidList.add(comId);
//				result.add(map);
//			}else {
//				if(comidList.contains(comId)){
//					for(Map<String, Object> dMap : result){
//						Long cid = (Long)dMap.get("comid");
//						if(cid.intValue()==comId.intValue()){
//							dMap.put("corder",map.get("scount"));
//							break;
//						}
//					}
//				}else {
//					map.put("corder", map.get("scount"));
//					map.put("scount", null);
//					result.add(map);
//				}
//			}
		}
	}
	private void requestUtil(HttpServletRequest request){
		request.setAttribute("uid", RequestUtil.processParams(request, "uid"));
		request.setAttribute("btime", RequestUtil.processParams(request, "btime"));
		request.setAttribute("etime", RequestUtil.processParams(request, "etime"));
		request.setAttribute("otype", RequestUtil.processParams(request, "otype"));
		System.out.println(RequestUtil.processParams(request, "otype"));
		Integer paytype = RequestUtil.getInteger(request, "pay_type", 0);
		request.setAttribute("pay_type",paytype);
		if(paytype==8){
			request.setAttribute("total", RequestUtil.getDouble(request, "free", 0d));
		}else{
			request.setAttribute("total", RequestUtil.getDouble(request, "total", 0d));
		}
		request.setAttribute("free", RequestUtil.getDouble(request, "free", 0d));
		request.setAttribute("pmoney", RequestUtil.getDouble(request, "pmoney", 0d));
		request.setAttribute("pmobile", RequestUtil.getDouble(request, "pmobile", 0d));
		request.setAttribute("count", RequestUtil.getInteger(request, "count", 0));
		request.setAttribute("comid", RequestUtil.getInteger(request, "comid", 0));
		request.setAttribute("pay_type", RequestUtil.getInteger(request, "pay_type", 0));
	}
	
	private Double getPayMoney2 (Long uid,Long comid,SqlInfo sqlInfo,List<Object> params){
		String sql ="select sum(total) money from order_tb where comid=? and pay_type=? and uid=? and "+sqlInfo.getSql();
//		params.add(0,uid);
//		params.add(0,1);
//		params.add(0,comid);
		Object[] values = new Object[]{comid,1,uid,params.get(0),params.get(1)};
		Map map = pgOnlyReadService.getMap(sql, values);
		if(map!=null&&map.get("money")!=null)
			return Double.valueOf(map.get("money")+"");
		return 0d;
	}
	//�ֽ�֧��
	private Double getPayMoney (Long uid,List<Object> params){
		String sql ="select sum(amount) money from parkuser_cash_tb where uin=? and type=? and create_time between ? and ? ";
		Object[] values = new Object[]{uid,0,params.get(0),params.get(1)};
		Map map = daService.getMap(sql, values);
		if(map!=null&&map.get("money")!=null)
			return Double.valueOf(map.get("money")+"");
		return 0d;
	}
	
	private Long getComid(Long comid, Long cityid, Long groupid){
		List<Object> parks = null;
		if(groupid != null && groupid > 0){
			parks = commonMethods.getParks(groupid);
			if(parks != null && !parks.isEmpty()){
				comid = (Long)parks.get(0);
			}else{
				comid = -999L;
			}
		}else if(cityid != null && cityid > 0){
			parks = commonMethods.getparks(cityid);
			if(parks != null && !parks.isEmpty()){
				comid = (Long)parks.get(0);
			}else{
				comid = -999L;
			}
		}
		
		return comid;
	}
}