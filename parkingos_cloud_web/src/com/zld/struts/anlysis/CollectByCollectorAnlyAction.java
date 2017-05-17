package com.zld.struts.anlysis;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import com.zld.facade.StatsAccountFacade;
import com.zld.impl.CommonMethods;
import com.zld.pojo.StatsAccountClass;
import com.zld.pojo.StatsFacadeResp;
import com.zld.pojo.StatsReq;
import com.zld.service.PgOnlyReadService;
import com.zld.utils.ExportExcelUtil;
import com.zld.utils.JsonUtil;
import com.zld.utils.RequestUtil;
import com.zld.utils.SqlInfo;
import com.zld.utils.StringUtils;
import com.zld.utils.TimeTools;

public class CollectByCollectorAnlyAction extends Action {
	@Autowired
	private PgOnlyReadService pgOnlyReadService;
	@Autowired
	private CommonMethods commonMethods;
	@Autowired
	private StatsAccountFacade accountFacade;
	
	private Logger logger = Logger.getLogger(CollectByCollectorAnlyAction.class);
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String action = RequestUtil.processParams(request, "action");
		Long uin = (Long)request.getSession().getAttribute("loginuin");//登录的用户id
		Long cityid = (Long)request.getSession().getAttribute("cityid");
		Long groupid = (Long)request.getSession().getAttribute("groupid");
		request.setAttribute("authid", request.getParameter("authid"));
		if(uin == null){
			response.sendRedirect("login.do");
			return null;
		}
		if(cityid == null && groupid == null){
			return null;
		}
		if(cityid == null) cityid = -1L;
		if(groupid == null) groupid = -1L;
		if(action.equals("")){
			return mapping.findForward("list");
		}if(action.equals("query")){
			String fieldsstr = RequestUtil.processParams(request, "fieldsstr");
			Integer pageNum = RequestUtil.getInteger(request, "page", 1);
			Integer pageSize = RequestUtil.getInteger(request, "rp", 20);
			Long b = TimeTools.getToDayBeginTime();
			Long e = b + 24 * 60 * 60;
			if(groupid == -1){//城市商户要求可按照运营集团筛查
				groupid = RequestUtil.getLong(request, "groupid", -1L);
			}
			SqlInfo sqlInfo = RequestUtil.customSearch(request, "user_info");
			List<Object> params = new ArrayList<Object>();
			String sql = "select id,nickname,resume from user_info_tb where ";
			String countSql = "select count(id) from user_info_tb where ";
			List<Object> collectors = null;
			if(cityid > 0){
				collectors = commonMethods.getcollctors(cityid);
			}else if(groupid > 0){
				collectors = commonMethods.getCollctors(groupid);
			}
			if(collectors != null && !collectors.isEmpty()){
				String preParams = "";
				for(Object object : collectors){
					if(preParams.equals(""))
						preParams ="?";
					else
						preParams += ",?";
				}
				sql += " id in ("+preParams+") ";
				countSql += " id in ("+preParams+") ";
				params.addAll(collectors);
			}
			if(sqlInfo != null) {
				countSql += " and "+ sqlInfo.getSql();
				sql += " and "+sqlInfo.getSql();
				params.addAll(sqlInfo.getParams());
			}
			List<Map<String, Object>> list = null;
			Long count = pgOnlyReadService.getCount(countSql, params);
			if(count > 0){
				list = pgOnlyReadService.getAll(sql, params, pageNum, pageSize);
				setList(list, b, e);
			}
			String json = JsonUtil.Map2Json(list, pageNum, count, fieldsstr, "id");
			AjaxUtil.ajaxOutput(response, json);
		}else if(action.equals("total")){
			String fieldsstr = RequestUtil.processParams(request, "fieldsstr");
			Long b = TimeTools.getToDayBeginTime();
			Long e = b + 24 * 60 * 60;
			if(groupid == -1){//城市商户要求可按照运营集团筛查
				groupid = RequestUtil.getLong(request, "groupid", -1L);
			}
			SqlInfo sqlInfo = RequestUtil.customSearch(request, "user_info");
			List<Object> params = new ArrayList<Object>();
			String sql = "select id,nickname,resume from user_info_tb where ";
			String countSql = "select count(id) from user_info_tb where ";
			List<Object> collectors = null;
			if(cityid > 0){
				collectors = commonMethods.getcollctors(cityid);
			}else if(groupid > 0){
				collectors = commonMethods.getCollctors(groupid);
			}
			if(collectors != null && !collectors.isEmpty()){
				String preParams = "";
				for(Object object : collectors){
					if(preParams.equals(""))
						preParams ="?";
					else
						preParams += ",?";
				}
				sql += " id in ("+preParams+") ";
				countSql += " id in ("+preParams+") ";
				params.addAll(collectors);
			}
			if(sqlInfo != null) {
				countSql += " and "+ sqlInfo.getSql();
				sql += " and "+sqlInfo.getSql();
				params.addAll(sqlInfo.getParams());
			}
			List<Map<String, Object>> list = null;
			Long count = pgOnlyReadService.getCount(countSql, params);
			if(count > 0){
				list = pgOnlyReadService.getAll(sql, params.toArray());
				setList(list, b, e);
			}
			String json = JsonUtil.Map2Json(list, 1, list.size(), fieldsstr, "id");
			AjaxUtil.ajaxOutput(response, json);
		}else if(action.equals("export")){
			String fieldsstr = RequestUtil.processParams(request, "fieldsstr");
			Long b = TimeTools.getToDayBeginTime();
			Long e = b + 24 * 60 * 60;
			if(groupid == -1){//城市商户要求可按照运营集团筛查
				groupid = RequestUtil.getLong(request, "groupid", -1L);
			}
			SqlInfo sqlInfo = RequestUtil.customSearch(request, "user_info");
			List<Object> params = new ArrayList<Object>();
			String sql = "select id,nickname,resume from user_info_tb where ";
			String countSql = "select count(id) from user_info_tb where ";
			List<Object> collectors = null;
			if(cityid > 0){
				collectors = commonMethods.getcollctors(cityid);
			}else if(groupid > 0){
				collectors = commonMethods.getCollctors(groupid);
			}
			if(collectors != null && !collectors.isEmpty()){
				String preParams = "";
				for(Object object : collectors){
					if(preParams.equals(""))
						preParams ="?";
					else
						preParams += ",?";
				}
				sql += " id in ("+preParams+") ";
				countSql += " id in ("+preParams+") ";
				params.addAll(collectors);
			}
			if(sqlInfo != null) {
				countSql += " and "+ sqlInfo.getSql();
				sql += " and "+sqlInfo.getSql();
				params.addAll(sqlInfo.getParams());
			}
			List<Map<String, Object>> list = null;
			Long count = pgOnlyReadService.getCount(countSql, params);
			if(count > 0){
				list = pgOnlyReadService.getAll(sql, params.toArray());
				setList(list, b, e);
			}
			Date date = new Date();
			SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd 00:00:00");
			SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
			export(response, list,sdf1.format(date), sdf2.format(date));
		}
		return null;
	}
	
	private void export(HttpServletResponse response, List<Map<String, Object>> list, String btime, String etime){
		try {
			if(list != null && !list.isEmpty()){
				String heards[] = new String[]{"账号","姓名","备注","普通订单","追缴订单","合计","普通订单","追缴订单","合计",
						"普通订单","追缴订单","合计","实收停车费","未缴停车费","应收停车费","售卡总面值","卡片充值","注销总面值"};
				List<List<String>> bodyList = new ArrayList<List<String>>();
				double total_cashCustomFee = 0d;
				double total_cashPursueFee = 0d;
				double total_cashTotalFee = 0d;
				
				double total_ePayCustomFee = 0d;
				double total_ePayPursueFee = 0d;
				double total_ePayTotalFee = 0d;
				
				double total_cardCustomFee = 0d;
				double total_cardPursueFee = 0d;
				double total_cardTotalFee = 0d;
				
			
				double total_totalFee = 0d;
				double total_escapeFee = 0d;
				double total_allTotalFee = 0d;
				
				double total_cardActFee = 0d;
				double total_cardChargeCashFee = 0d;
				double total_cardReturnFee = 0d;
				
				for(Map<String, Object> map : list){
					List<String> valueList = new ArrayList<String>();
					
					valueList.add(map.get("id") + "");
					valueList.add(map.get("nickname") + "");
					valueList.add(map.get("resume") + "");
					double cashCustomFee = (double)map.get("cashCustomFee");
					total_cashCustomFee += cashCustomFee;
					valueList.add(cashCustomFee + "");
					double cashPursueFee = (double)map.get("cashPursueFee");
					total_cashPursueFee += cashPursueFee;
					valueList.add(cashPursueFee + "");
					double cashTotalFee = (double)map.get("cashTotalFee");
					total_cashTotalFee += cashTotalFee;
					valueList.add(cashTotalFee + "");
					
					double ePayCustomFee = (double)map.get("ePayCustomFee");
					total_ePayCustomFee += ePayCustomFee;
					valueList.add(ePayCustomFee + "");
					double ePayPursueFee = (double)map.get("ePayPursueFee");
					total_ePayPursueFee += ePayPursueFee;
					valueList.add(ePayPursueFee + "");
					double ePayTotalFee = (double)map.get("ePayTotalFee");
					total_ePayTotalFee += ePayTotalFee;
					valueList.add(ePayTotalFee + "");
					
					double cardCustomFee = (double)map.get("cardCustomFee");
					total_cardCustomFee += cardCustomFee;
					valueList.add(cardCustomFee + "");
					double cardPursueFee = (double)map.get("cardPursueFee");
					total_cardPursueFee += cardPursueFee;
					valueList.add(cardPursueFee + "");
					double cardTotalFee = (double)map.get("cardTotalFee");
					total_cardTotalFee += cardTotalFee;
					valueList.add(cardTotalFee + "");
					
					double totalFee = (double)map.get("totalFee");
					total_totalFee += totalFee;
					valueList.add(totalFee + "");
					double escapeFee = (double)map.get("escapeFee");
					total_escapeFee += escapeFee;
					valueList.add(escapeFee + "");
					double allTotalFee = (double)map.get("allTotalFee");
					total_allTotalFee += allTotalFee;
					valueList.add(allTotalFee + "");
					
					double cardActFee = (double)map.get("cardActFee");
					total_cardActFee += cardActFee;
					valueList.add(cardActFee + "");
					double cardChargeCashFee = (double)map.get("cardChargeCashFee");
					total_cardChargeCashFee += cardChargeCashFee;
					valueList.add(cardChargeCashFee + "");
					double cardReturnFee = (double)map.get("cardReturnFee");
					total_cardReturnFee += cardReturnFee;
					valueList.add(cardReturnFee + "");
					bodyList.add(valueList);
				}
				List<String> valueList = new ArrayList<String>();
				valueList.add("");
				valueList.add("合计");
				valueList.add("合计");
				valueList.add(total_cashCustomFee + "");
				valueList.add(total_cashPursueFee + "");
				valueList.add(total_cashTotalFee + "");
				
				valueList.add(total_ePayCustomFee + "");
				valueList.add(total_ePayPursueFee + "");
				valueList.add(total_ePayTotalFee + "");
				
				valueList.add(total_cardCustomFee + "");
				valueList.add(total_cardPursueFee + "");
				valueList.add(total_cardTotalFee + "");
				
				valueList.add(total_totalFee + "");
				valueList.add(total_escapeFee + "");
				valueList.add(total_allTotalFee + "");
				
				valueList.add(total_cardActFee + "");
				valueList.add(total_cardChargeCashFee + "");
				valueList.add(total_cardReturnFee + "");
				bodyList.add(valueList);
				
				
				String fname = "收费员收费报表" + btime + "至" + etime;
				java.io.OutputStream os = response.getOutputStream();
				response.reset();
				response.setHeader("Content-disposition", "attachment; filename="
						+ StringUtils.encodingFileName(fname) + ".xls");
				ExportExcelUtil importExcel = new ExportExcelUtil("收费员收费报表",
						heards, bodyList);
				List<Map<String,String>> mulitHeadList = new ArrayList<Map<String,String>>();
				Map<String, String> mhead0 = new HashMap<String, String>();
				mhead0.put("length", "0");
				mhead0.put("content", "");
				mulitHeadList.add(mhead0);
				Map<String, String> mhead1 = new HashMap<String, String>();
				mhead1.put("length", "0");
				mhead1.put("content", "");
				mulitHeadList.add(mhead1);
				Map<String, String> mhead7 = new HashMap<String, String>();
				mhead7.put("length", "0");
				mhead7.put("content", "");
				mulitHeadList.add(mhead7);
				Map<String, String> mhead2 = new HashMap<String, String>();
				mhead2.put("length", "2");
				mhead2.put("content", "停车费-现金支付");
				mulitHeadList.add(mhead2);
				Map<String, String> mhead3 = new HashMap<String, String>();
				mhead3.put("length", "2");
				mhead3.put("content", "停车费-电子支付");
				mulitHeadList.add(mhead3);
				Map<String, String> mhead4 = new HashMap<String, String>();
				mhead4.put("length", "2");
				mhead4.put("content", "停车费-刷卡支付");
				mulitHeadList.add(mhead4);
				Map<String, String> mhead5 = new HashMap<String, String>();
				mhead5.put("length", "2");
				mhead5.put("content", "停车费-合计");
				mulitHeadList.add(mhead5);
				Map<String, String> mhead6 = new HashMap<String, String>();
				mhead6.put("length", "2");
				mhead6.put("content", "卡片");
				mulitHeadList.add(mhead6);
				importExcel.mulitHeadList = mulitHeadList;
				Map<String, String> headInfoMap=new HashMap<String, String>();
				headInfoMap.put("length", heards.length - 1 + "");
				headInfoMap.put("content", fname);
				importExcel.headInfo = headInfoMap;
				importExcel.createExcelFile(os);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setList(List<Map<String, Object>> userList, Long startTime, Long endTime){
		try {
			if(userList != null && !userList.isEmpty()){
				List<Object> idList = new ArrayList<Object>();
				for(Map<String, Object> map : userList){
					idList.add(map.get("id"));
				}
				StatsReq req = new StatsReq();
				req.setIdList(idList);
				req.setStartTime(startTime);
				req.setEndTime(endTime);
				StatsFacadeResp resp = accountFacade.statsParkUserAccount(req);
				if(resp.getResult() == 1){
					List<StatsAccountClass> classes = resp.getClasses();
					for(StatsAccountClass accountClass : classes){
						long id = accountClass.getId();
						double cashParkingFee = accountClass.getCashParkingFee();
						double cashPrepayFee = accountClass.getCashPrepayFee();
						double cashRefundFee = accountClass.getCashRefundFee();
						double cashAddFee = accountClass.getCashAddFee();
						double cashPursueFee = accountClass.getCashPursueFee();
						
						double ePayParkingFee = accountClass.getePayParkingFee();
						double ePayPrepayFee = accountClass.getePayPrepayFee();
						double ePayRefundFee = accountClass.getePayRefundFee();
						double ePayAddFee = accountClass.getePayAddFee();
						double ePayPursueFee = accountClass.getePayPursueFee();
						
						double cardParkingFee = accountClass.getCardParkingFee();
						double cardPrepayFee = accountClass.getCardPrepayFee();
						double cardRefundFee = accountClass.getCardRefundFee();
						double cardAddFee = accountClass.getCardAddFee();
						double cardPursueFee = accountClass.getCardPursueFee();
						
						double escapeFee = accountClass.getEscapeFee();
						double sensorOrderFee = accountClass.getSensorOrderFee();
						
						//卡片统计
						double cardChargeCashFee = accountClass.getCardChargeCashFee();//卡片充值金额
						double cardReturnFee = accountClass.getCardReturnFee();//退卡退还金额
						double cardActFee = accountClass.getCardActFee();//卖卡金额
						
						double cashCustomFee = StringUtils.formatDouble(cashParkingFee + cashPrepayFee + cashAddFee - cashRefundFee);
						double epayCustomFee = StringUtils.formatDouble(ePayParkingFee + ePayPrepayFee + ePayAddFee - ePayRefundFee);
						double cardCustomFee = StringUtils.formatDouble(cardParkingFee + cardPrepayFee + cardAddFee - cardRefundFee);
						double cashTotalFee = StringUtils.formatDouble(cashPursueFee + cashCustomFee);
						double ePayTotalFee = StringUtils.formatDouble(ePayPursueFee + epayCustomFee);
						double cardTotalFee = StringUtils.formatDouble(cardPursueFee + cardCustomFee);
						double totalFee = StringUtils.formatDouble(cashTotalFee + ePayTotalFee + cardTotalFee);
						double allTotalFee = StringUtils.formatDouble(totalFee + escapeFee);
						double totalPursueFee = StringUtils.formatDouble(cashPursueFee + ePayPursueFee + cardPursueFee);
						
						for(Map<String, Object> infoMap : userList){
							Long userId = (Long)infoMap.get("id");
							if(id == userId.intValue()){
								infoMap.put("cashPursueFee", cashPursueFee);
								infoMap.put("cashCustomFee", cashCustomFee);
								infoMap.put("cashTotalFee", cashTotalFee);
								infoMap.put("ePayPursueFee", ePayPursueFee);
								infoMap.put("ePayCustomFee", epayCustomFee);
								infoMap.put("ePayTotalFee", ePayTotalFee);
								infoMap.put("cardPursueFee", cardPursueFee);
								infoMap.put("cardCustomFee", cardCustomFee);
								infoMap.put("cardTotalFee", cardTotalFee);
								infoMap.put("totalFee", totalFee);
								infoMap.put("escapeFee", escapeFee);
								infoMap.put("allTotalFee", allTotalFee);
								infoMap.put("cardChargeCashFee", cardChargeCashFee);
								infoMap.put("cardReturnFee", cardReturnFee);
								infoMap.put("cardActFee", cardActFee);
								infoMap.put("totalPursueFee", totalPursueFee);
								break;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
