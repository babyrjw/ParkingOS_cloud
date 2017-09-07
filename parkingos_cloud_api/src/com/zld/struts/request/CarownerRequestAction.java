package com.zld.struts.request;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.beans.factory.annotation.Autowired;

import pay.Constants;
import pay.PayConfigDefind;

import com.zld.AjaxUtil;
import com.zld.CustomDefind;
import com.zld.impl.CommonMethods;
import com.zld.impl.MemcacheUtils;
import com.zld.impl.PublicMethods;
import com.zld.service.DataBaseService;
import com.zld.service.LogService;
import com.zld.service.PgOnlyReadService;
import com.zld.utils.Check;
import com.zld.utils.OrderSortCompare;
import com.zld.utils.RequestUtil;
import com.zld.utils.StringUtils;
import com.zld.utils.TimeTools;
import com.zld.utils.ZldMap;
import com.zld.wxpublic.util.CommonUtil;
/**
 * ����������
 * @author Administrator
 *
 */
public class CarownerRequestAction extends Action{
	
	@Autowired
	private DataBaseService daService;
	@Autowired
	private PgOnlyReadService pService;
	@Autowired
	private PublicMethods publicMethods;
	@Autowired
	private MemcacheUtils memcacheUtils;
	@Autowired
	private LogService logService;
	@Autowired
	private CommonMethods commonMethods;
	
	private Logger logger = Logger.getLogger(CarownerRequestAction.class);
	
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		Long ntime = System.currentTimeMillis()/1000;
		String mobile =RequestUtil.processParams(request, "mobile");
		String openid =RequestUtil.processParams(request, "openid");
		String action =RequestUtil.processParams(request, "action");
		logger.info("-----------------------------action:"+action+",mobile="+mobile);
		Long comId= RequestUtil.getLong(request, "comid", -1L);
		Map<String,Object> infoMap = new HashMap<String, Object>();
		Long uin = null;
		Map userMap =null;
		Integer client_type=0;
		if(!"".equals(openid)){
			userMap = daService.getPojo("select * from user_info_tb where wxp_openid=? and auth_flag=? ", new Object[]{openid,4});
			if(userMap!=null){
				uin =(Long) userMap.get("id");
				if(userMap.get("client_type")!=null)
					client_type = (Integer)userMap.get("client_type");
//				Integer unionState = (Integer)userMap.get("union_state");
//				if(unionState==0){//û���ϴ�������
//					List carList= pService.getAll("select car_number from car_info_tb where uin = ? and state=? ", new Object[]{uin,1});
//					if(carList!=null&&!carList.isEmpty()){//�г�����Ҫͬ��������ƽ̨
//						publicMethods.syncUser2Bolink(carList,StringUtils.formatDouble(userMap.get("balance")));
//					}
//				}
			}else {
				infoMap.put("info", "openid is invalid");
				AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));
				return null;
			}
		}else {
			userMap = daService.getPojo("select * from user_info_tb where mobile=? and auth_flag=? ", new Object[]{mobile,4});
			if(userMap!=null)
				uin = (Long)userMap.get("id");
		}
		logger.error("-----------------------------action:"+action+",uin="+uin+",mobile="+mobile+",openid="+openid+",client_type:"+client_type);
		if(action.equals("orderdetail")){//orderdetail:�������飨��ʷ������==
			infoMap = orderDetail(request,uin);
			String info = StringUtils.createJson(infoMap);
			info = info.replace("null", "");
			AjaxUtil.ajaxOutput(response, info );
			return null;
			//�������飨��ʷ������http://127.0.0.1/zld/carowner.do?action=orderdetail&orderid=786121&mobile=15801482643
		}else if(action.equals("historyroder")){//historyroder:��ʷ������==
			List<Map<String, Object>> infoMapList = historyOrder(request,uin);
			//Long _total = daService.getLong("select count(*) from order_tb where state=? ", new Object[]{1});
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMapList));
			return null;
			//��ʷ������http://127.0.0.1/zld/carowner.do?action=historyroder&page=1&size=10&mobile=18101333937
		}else if(action.equals("detail")){//detail:������Ϣ��==
			infoMap = detail(mobile,userMap);
			//������Ϣ��http://127.0.0.1/zld/carowner.do?action=detail&mobile=1
		}else if(action.equals("bonusinfo")){
			List<Map<String, Object>> list = daService.getAll("select id,exptime,type,btime  from order_ticket_tb where uin = ? and money> ? order by id desc limit ?", new Object[]{uin,0,15});
			if(list!=null){
				setBonusType(list);
			}
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(list));
			//������Ϣ��http://127.0.0.1/zld/carowner.do?action=bonusinfo&mobile=13641309140
			return null;
		}else if(action.equals("parkdetail")){//parkdetail:ͣ������Ϣ��==
			infoMap = parkDetail(request,comId,uin);
			//ͣ������Ϣ��http://127.0.0.1/zld/carowner.do?action=parkdetail&comid=3
		}else if(action.equals("parkproduct")){//parkproduct:ͣ�����İ��²�Ʒ==
			List<Map<String, Object>> infoLists = parkProduct(comId,uin);
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoLists));
			//AjaxUtil.ajaxOutput(response, StringUtils.createXML(infoLists));
			return null;
			//ͣ�������²�Ʒ http://127.0.0.1/zld/carowner.do?action=parkproduct&comid=3&mobile=15801482643
		}else if(action.equals("buyproduct")){//buyproduct:������£�==
			int result  = buyProduct(request,uin);
			AjaxUtil.ajaxOutput(response, result+"");
			return null;
			//������£�http://127.0.0.1/zld/carowner.do?action=buyproduct&productid=1&mobile=15801482643
		}else if(action.equals("currorder")){//currorder����ǰ������ֻһ����û��,==
			infoMap = currOrder(request,uin);
			//��ǰ������ֻһ����û��,http://127.0.0.1/zld/carowner.do?action=currorder&mobile=15801482643
		}else if(action.equals("currentorder")){//currorder����ǰδ���㶩����ֻһ����û��,==
			String from = RequestUtil.getString(request, "from");
			infoMap = currentOrder(request,uin);
			String result = StringUtils.createJson(infoMap);
			if(from.equals("qr")){
				result="{\"type\":\"2\",\"info\":"+result+"}";
				logger.error(">>ɨ��ά��鶩����"+result);
			}else if(from.equals("qrpark")){
				result="{\"type\":\"5\",\"info\":"+result+"}";
				logger.error(">>ɨ��ά����㶩����"+result);
			}
			AjaxUtil.ajaxOutput(response,result);
			return null;
			//��ǰ������ֻһ����û��,http://127.0.0.1/zld/cparowner.do?action=currentorder&mobile=15801482643
		}else if(action.equals("buyticket")){
//			String result = buyTicket(request,uin);
//			AjaxUtil.ajaxOutput(response, result);
			AjaxUtil.ajaxOutput(response, "0");
			return null;
			//����ͣ��ȯ�� http://192.168.199.240/zld/carowner.do?action=buyticket&mbile=18101333937&value=10&number=2
		}else if(action.equals("products")){//products:������İ��²�Ʒ==
			//List<Map<String, Object >> infoList = products(request,uin);
			List<Map<String, Object>> infoList = getproducts(request, uin);
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoList));
			return null;
			//�ҵİ��²�Ʒ  http://127.0.0.1/zld/carowner.do?action=products&mobile=15375242041 ==
		}
		else if(action.equals("getprofile")){//���������
			infoMap = getProfile(uin);
			//��������� http://127.0.0.1/zld/carowner.do?action=getprofile&mobile=15801270154
		}else if(action.equals("payorder")){
			//version=2�����ش������json���� 
			String ret = payOrder(request,comId,uin);
			AjaxUtil.ajaxOutput(response, ret);
			return null;
			//���֧���� http://127.0.0.1/zld/carowner.do?action=payorder&mobile=15375242041&ptype=1֧�����²�Ʒ(&productid=number=&start=),
				//2֧������(&orderid=&total=)
		}else if(action.equals("prepay")){
			String ret = prepay(request,comId,uin);
			AjaxUtil.ajaxOutput(response, ret);
			return null;
			//http://wang151068941.oicp.net/zld/carowner.do?action=prepay&mobile=18201517240&comid=3251&orderid=829931&money=0.01&ticketid=
		}else if(action.equals("paying")){//����֧��,���շ�Ա����Ϣ����ʾ���������ֻ�֧��
			//�շ�Ա�ʺ�
			Long orderId = RequestUtil.getLong(request, "orderid", -1L);
			Map orderMap = daService.getMap("select * from order_tb where id=?", new Object[]{orderId});
			Long btime = (Long)orderMap.get("create_time");
			Long etime  = (Long)orderMap.get("end_time");
			String duration = StringUtils.getTimeString(btime, etime);
			String carNumber =  publicMethods.getCarNumber(uin);
			Long uid = (Long)orderMap.get("uid");
			//���շ�Ա��Ϣ����ʾ�����ֻ�֧��....
			logService.insertParkUserMessage(comId, 1, uid, carNumber, orderId,  Double.valueOf(orderMap.get("total")+""),
					duration, 0, btime, etime, 0, null);
			return null;
			//֧���� http://127.0.0.1/zld/carowner.do?action=paying&mobile=15801482643&orderid=1066
		}else if(action.equals("accountdetail")){//�ʻ���ϸ
			List<Map<String,Object>> allList =accountDetail(request,uin);
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(allList));
			return null;
			///�ʻ���ϸ http://127.0.0.1/zld/carowner.do?action=accountdetail&mobile=15375242041&type=2//2ȫ����0����ֵ��1������
		}
		else if(action.equals("profile")){//��������
			//table=user_profile_tb;
			//�������� http://127.0.0.1/zld/carowner.do?action=profile&mobile=15801482643&
			if(uin==null){
				AjaxUtil.ajaxOutput(response, "-1");
				return null;
			}
			int result = setProfile(request,uin);
			AjaxUtil.ajaxOutput(response, result+"");
			return null;
		}else if(action.equals("setprof")){//�ͻ���1.0.20���ϰ汾�ĸ������ýӿ�
			if(uin==null){
				AjaxUtil.ajaxOutput(response, "-1");
				return null;
			}
			int result = setPro(request, uin);
			AjaxUtil.ajaxOutput(response, result+"");
			//http://127.0.0.1/zld/carowner.do?mobile=15801270154&action=setprof&low_recharge=10&limit_money=0
			return null;
		}else if(action.equals("getprof")){//�ͻ���1.0.20���ϰ汾�ĸ������ýӿ�
			//Map<String, Object> infoMap = new HashMap<String, Object>();
			infoMap = getProf(request,uin);
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));
			return null;
			//http://127.0.0.1/zld/carowner.do?mobile=15801482643&action=getprof
		}else if(action.equals("editcarnumber")){//�༭���ƺ�
			int result = editCarNumber(request,uin);
			AjaxUtil.ajaxOutput(response, result+"");
			return null;
			//�༭���ƺ� http://127.0.0.1/zld/carowner.do?mobile=15801270154&action=editcarnumber&carnumber=%25E4%25BA%25ACg23667
		
		}else if(action.equals("eidtphone")){//�޸ĵ绰
			int result = editPhone(request, uin,mobile);
			AjaxUtil.ajaxOutput(response, result+"");
			return null;
			//�޸ĵ绰 http://127.0.0.1/zld/carowner.do?action=eidtphone&mobile=13332223333&newmobile=15822224452
		}else if(action.equals("logout")){//�˳�
			int result = 0;
			if(uin!=null)
				result = daService.update("delete from user_session_tb where uin = ?",	new Object[]{uin});
			AjaxUtil.ajaxOutput(response, result+"");
			return null;
			//�˳� http://127.0.0.1/zld/carowner.do?action=logout&mobile=13332223333
		}else if(action.equals("praise")){//����ͣ����
			int result = praise(request,comId,uin);
			AjaxUtil.ajaxOutput(response,result+"");
			return null;
			// ����ͣ����(�ᡢ��)http://127.0.0.1/zld/carowner.do?action=praise&comid=3&praise=0&mobile=
		}else if(action.equals("comment")){//д������
			int result = comment(request,comId,uin);
			AjaxUtil.ajaxOutput(response, result+"");
			return null;
			//����ͣ���� http://127.0.0.1/zld/carowner.do?action=comment&comid=3&mobile=15375242041&comment=iunfsakehrej3245324
		}else if(action.equals("getcomment")){//��ȡ����
			List<Map<String, Object>> resultMap = getComment(request,comId);
			//��ȡͣ�������� put  http://127.0.0.1/zld/carowner.do?action=getcomment&comid=3&mobile=15375242041
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(resultMap));
			return null;
		}else if(action.equals("gettickets")){
			String tickets = getTickets(request,uin,mobile);
			AjaxUtil.ajaxOutput(response, tickets);
			return null;
			//�����ȯ http://127.0.0.1/zld/carowner.do?action=gettickets&mobile=13641309140
		}else if(action.equals("usetickets")){
			List<Map<String, Object>> ticketMap = daService.getAll("select id,create_time beginday ,limit_day limitday,state,money  from ticket_tb where uin = ? and state=? and limit_day>?",
					new Object[]{uin,0,ntime});
			if(ticketMap==null||ticketMap.isEmpty())
				AjaxUtil.ajaxOutput(response, "[]");
			else {
				AjaxUtil.ajaxOutput(response, StringUtils.createJson(ticketMap));
			}
			return null;
			//�����ȯ http://127.0.0.1/zld/carowner.do?action=usetickets&mobile=15375242041 
		}else if(action.equals("getaccount")){
			//����������ͣ��ȯ
			//���������ȯ http://127.0.0.1/zld/carowner.do?action=getaccount&mobile=13439680500&total=2.8&ptype=1&uid=21694&utype=1
			Object balance = userMap.get("balance");
			String ret = getAccount(request,balance,uin,mobile);
			//����û�з��ص��˻��е�ͣ��ȯ
			commonMethods.checkBonus(mobile, uin);
			AjaxUtil.ajaxOutput(response, ret);
			//���������ȯ http://127.0.0.1/zld/carowner.do?action=getaccount&mobile=18101333937&total=3&ptype=4&uid=21694
			return null;
		}else if(action.equals("isvip")){//�Ƿ�����ͨ���û�
			Long count = daService.getLong("select count(*) from con_nfc_tb where uin =? ", new Object[]{uin});
			AjaxUtil.ajaxOutput(response, count+"");
			return null;
		}else if(action.equals("epay")){
			String ret = epay(request,uin);
			AjaxUtil.ajaxOutput(response, ret);
			//�����ȯ http://127.0.0.1/zld/carowner.do?action=epay&mobile=15375242041&uid=&total=&ticketid=
			return null;
		}else if(action.equals("getparkusers")){//��������������ѡ�
			Long comid = RequestUtil.getLong(request, "comid", -1L);
			List userliList = daService.getAll("select id,nickname as name,online_flag online from user_info_tb where " +
					"comid=? and  state=? and isview=? and auth_flag in(?,?) " +
					"order by online desc  nulls last, name desc  nulls last", 
					new Object[]{comid,0,1,1,2});
			//System.out.println(userliList);
			String result = StringUtils.createJson(userliList);
			AjaxUtil.ajaxOutput(response,result.replace("null", "") );
			return null;
			//�����ȯ http://127.0.0.1/zld/carowner.do?action=getparkusers&comid=1197&mobile=15375242041
		}else if(action.equals("getpkuser")){
			String result = getPKUser(request,comId,uin);
			AjaxUtil.ajaxOutput(response,result.replace("null", "") );
			return null;
			//�����ȯ http://127.0.0.1/zld/carowner.do?action=getpkuser&uid=10700&mobile=15375242041
		}else if(action.equals("regcarmsg")){//�����Ƽ�����
			String ret =regCarMsg(request,uin);
			AjaxUtil.ajaxOutput(response, ret);
			//http://127.0.0.1/zld/carowner.do?action=regcarmsg&mobile=15375242041
			return null;
		}else if(action.equals("hbonus")){//��ѯ�Ƿ��н��պ��
			String ret = hBonus(request,uin);
			AjaxUtil.ajaxOutput(response, ret);
			return null;
			//http://127.0.0.1/zld/carowner.do?action=hbonus&mobile=15801482643
		}else if(action.equals("obparms")){//���������ʱ���������
			String ret = oBparms(request);
			AjaxUtil.ajaxOutput(response, ret);
			return null;
			//http://127.0.0.1/zld/carowner.do?action=obparms&mobile=15801482463&bid=1
		}else if(action.equals("hbparms")){//������պ��ʱ���������
			String title = CustomDefind.getValue("TITLE");
			String description = CustomDefind.getValue("DESCRIPTION");
			AjaxUtil.ajaxOutput(response, "{\"imgurl\":\"images/bonus/weixilogo_300.png\",\"title\":\""+title+"\"," +
					"\"description\":\""+description+"\",\"url\":\"carowner.do?action=gethbonus\"}");
			return null;
			//http://127.0.0.1/zld/carowner.do?action=hbparms&mobile=15375242041
		}else if(action.equals("pusercomment")){//�������շ�Ա������
			String ret = commpuser(request);
			AjaxUtil.ajaxOutput(response, ret);
			return null;
			//�������շ�Ա������ http://192.168.199.240/zld/carowner.do?action=pusercomment&mobile=18101333937&orderid=&comment=
		}else if(action.equals("puserreward")){//�������շ�Ա�Ĵ���
			String ret = reward(request);
			AjaxUtil.ajaxOutput(response, ret);
			return null;
			//�������շ�Ա������  http://192.168.199.240/zld/carowner.do?action=puserreward&mobile=18101333937&orderid=786820&ticketid=38875&uid=11802&money=2
		}else if(action.equals("sweepticket")){
			String result= sweepTicket(request,uin,mobile);
			AjaxUtil.ajaxOutput(response,result);
			return null;
			//http://192.168.199.239/zld/carowner.do?action=sweepticket&codeid=100253&mobile=18201517240;
		}else if(action.equals("recominfo")){//ȡ�Ƽ���¼
			List list = daService.getAll("select nid uin,state from recommend_tb where pid=? and type=? ",new Object[]{uin,1});
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(list));
			return null;
			//http://127.0.0.1/zld/carowner.do?action=recominfo&mobile=15801482643
		}else if(action.equals("getmesg")){
			String ret = getMesg(request,uin);
			AjaxUtil.ajaxOutput(response, ret);
			//http://127.0.0.1/zld/carowner.do?action=getmesg&mobile=15801482643&page=1
			return null;
		}else if(action.equals("getrecomurl")){
			AjaxUtil.ajaxOutput(response, "http://t.cn/RZuFpVJ");
			return null;
			//http://127.0.0.1/zld/carowner.do?action=getrecomurl&mobile=15801482643
		}else if(action.equals("gethbonus")){//��ȡ���պ���������
			//http://127.0.0.1/zld/carowner.do?action=gethbonus&id=9
			Long id =RequestUtil.getLong(request, "id",3L);
			if(id>12&&id!=999){//�����ڵĺ��
				AjaxUtil.ajaxOutput(response, "�Բ�����Ҫ������������ ��������");
				return null;
			}
			Long uid =RequestUtil.getLong(request, "uid",-1L);//�Ƽ��ˣ���id=999ʱ���շ�Ա�Ƽ�������
			logger.error("�����������ȡҳ��....�շ�Աuid:"+uid+",���id:"+id);
			if(mobile.equals("")){
				request.setAttribute("type", "-2");//��ȡҳ�棬�ڷ��������д� 
				request.setAttribute("action",action);//��ȡҳ�棬�ڷ��������д� 
				request.setAttribute("id",id);//��ȡҳ�棬�ڷ��������д� 
				request.setAttribute("uid",uid);//��ȡҳ�棬�ڷ��������д� 
				return mapping.findForward("success");
			}
			if(!Check.checkMobile(mobile)){
				logger.error("��������ֻ����Ϸ�...."+mobile);
				request.setAttribute("type", "-4");//�ֻ��Ų��Ϸ�
				request.setAttribute("action",action);//��ȡҳ�棬�ڷ��������д� 
				request.setAttribute("id",id);//��ȡҳ�棬�ڷ��������д� 
				request.setAttribute("mobile",mobile);//�ֻ���
				return mapping.findForward("success");
			}
			//һ�����ֻ����ȡһ��
			String qsql = "select amount from bonus_record_tb where bid= ? and mobile=? ";
			Object []values =new Object[]{id,mobile};
			if(id==3){//���պ����һ�������һ��
				qsql += " and ctime>?";
				values =new Object[]{id,mobile,TimeTools.getToDayBeginTime()};
			}
			Map buMap  = daService.getMap(qsql,values);
			int t =0;
			if(buMap!=null)
				t = (Integer)buMap.get("amount");
			if(t>0){//�Ѿ���ȡ��
				request.setAttribute("type", "-3");//�Ѿ���ȡ��
				request.setAttribute("amount", t);//�Ѿ���ȡ��
				request.setAttribute("uphone", mobile);
				request.setAttribute("action",action);//��ȡҳ�棬�ڷ��������д� 
				request.setAttribute("id",id);//��ȡҳ�棬�ڷ��������д� 
				logger.error("�����������ȡ��....mobile="+mobile+",money:"+t);
				return mapping.findForward("bonusm");
			}
			String bsql = "insert into bonus_record_tb (bid,ctime,mobile,state,amount) values(?,?,?,?,?) ";
			String tsql = "insert into ticket_tb (create_time,limit_day,money,state,uin) values(?,?,?,?,?) ";
			if(uin!=null&&uin>0){//���û�
				if(id==12){//�������,���û�ֻҪ�ǵ�һ����ȡ��Ҳ��30Ԫ
					int ret = backTickets(uin, 3);
					request.setAttribute("amount", 30);
					request.setAttribute("uphone", mobile);
					request.setAttribute("type", 1);
					logger.error(">>>>�������,���û�д��ͣ��ȯ��"+ret);
					if(ret>0){
						daService.update(bsql, new Object[]{id,ntime,mobile,1,30});
					}
				}else {
					logger.error("����������û�("+uin+")....mobile:"+mobile);
					Integer [] amounts = new Integer[]{1,3,3,3,3,1,1,3,3,3,3,3,1,3,3,3,3,1,3,1,3,3,1,1,3,3,3,3,3,1,3,3,2,3,3,1,3,3,3,3,3,1,3,3,3,3,3,1,3,3,2,3,1,3,2,3,3,1,3,2,3,3,3,1,3,3,2,3,1,3,2,3,3,1,3,3,3,1,2,3,3,3,3,1,3,3,3,2,1,3,2,3,1,3,3,3,1,3,3,1};
					Integer index = memcacheUtils.readGetHBonusCache();
					Integer amount = amounts[0];
					if(index!=null){
						amount= amounts[index];
						index++;
						if(index==100)
							index=0;
					}else {
						index=0;
					}
					memcacheUtils.doIntegerCache("hbonus_index", index, "update");
					List<Map<String, Object>> sqls = new ArrayList<Map<String,Object>>();
					Map<String, Object>	bMap = new HashMap<String, Object>();
					bMap.put("sql", bsql);
					bMap.put("values",new Object[]{id,ntime,mobile,1,amount});
					sqls.add(bMap);
					
					Map<String, Object>	tMap = new HashMap<String, Object>();
					tMap.put("sql", tsql);
					tMap.put("values",new Object[]{ntime,ntime+6*24*60*60,amount,0,uin});
					sqls.add(tMap);
					boolean ret = daService.bathUpdate(sqls);
					if(ret){
						logService.insertUserMesg(1, uin, "��ϲ�����һ��"+amount+"Ԫͣ��ȯ!", "�������");
					}
					logger.error("getobonus:"+ret);	
					request.setAttribute("amount", amount);
					request.setAttribute("uphone", mobile);
					logger.error("����������û���"+amount+"...."+mobile);
				}
				
			}else if(!mobile.equals("")){//���û�
				Long uinLong = publicMethods.regUser(mobile, id,uid,false);
				Integer money = 10;
				if(id==12)
					money=30;
//				if(id==8||id==7)
//					money=100;
				Object [] _values = new Object[]{id,ntime,mobile,0,money};
//				if(regUser(mobile, id,uid)>0){
//					_values = new Object[]{id,ntime,mobile,1,money};
//				}
				int ret = daService.update(bsql,_values );
				logger.error("����������û���"+money+"....mobile:"+mobile+",�û��˻���"+uinLong);
				//int ret = daService.update(bsql, new Object[]{id,ntime,mobile,0,30});
				logger.error("getobonus:"+ret);	
				request.setAttribute("amount", money);
				request.setAttribute("uphone", mobile);
				request.setAttribute("type", 1);
			}
			request.setAttribute("action",action);//��ȡҳ�棬�ڷ��������д� 
			request.setAttribute("id",id);//��ȡҳ�棬�ڷ��������д� 
			return mapping.findForward("bonusm");
		}else if(action.equals("getobonus")){//��ȡ�������
			//http://127.0.0.1/zld/carowner.do?action=getobonus&id=1
			Long bid = RequestUtil.getLong(request, "id",-1L);
			String operate = RequestUtil.getString(request, "operate");
			if(operate.equals("")){
				//String ret = getOrderBonus(bid, request);
				//String location ="https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx08c66cac888faa2a&redirect_uri=http%3A%2F%2Fwww.tingchebao.com%2Fzld%2Fcarowner.do%3Faction%3Dgetobonus%26id%3D"+bid+"%26operate%3Dcaibonus&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
				int ret = doPreGetBonus(request, bid);
				if(ret==1){//�к����Ҳû�й���
					String location ="https://open.weixin.qq.com/connect/oauth2/authorize?appid="+Constants.WXPUBLIC_APPID+"&redirect_uri=http%3A%2F%2F"+Constants.WXPUBLIC_REDIRECTURL+"%2Fzld%2Fcarowner.do%3Faction%3Dgetobonus%26id%3D"+bid+"%26operate%3Dcaibonus&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
					//�û���Ȩ
					response.sendRedirect(location);
					return null;
				}else {
					request.setAttribute("isover", 0);
					return mapping.findForward("caibouns");
				}
//				return mapping.findForward("bounsret");
				//request.setAttribute("bid", bid);
				//return mapping.findForward(ret);
			}else if(operate.equals("caibonus")){//����
				String auth_range = RequestUtil.getString(request, "authrange");
				String []wxkeys = getOpenid(request);
				if(wxkeys == null){
					return mapping.findForward("error");
				}
				Map user  = getMobileByOpenid(wxkeys[0]);
				String wximgurl =null;
				if(user!=null)
					wximgurl =(String)user.get("wx_imgurl");
				
				//����Ȩ΢��
				if(auth_range.equals("")){
					//û��΢��ͷ��΢�����ƣ���Ҫ�û���Ȩ
					if(user==null||user.get("wx_name")==null||wximgurl==null || wximgurl.length()<1 ){
						//�û���Ȩ
						String authurl =   "https://open.weixin.qq.com/connect/oauth2/authorize?appid="+Constants.WXPUBLIC_APPID+"&redirect_uri=http%3A%2F%2F"+Constants.WXPUBLIC_REDIRECTURL+"%2Fzld%2Fcarowner.do%3Faction%3Dgetobonus%26id%3D"+bid+"%26operate%3Dcaibonus" +
								"%26authrange%3Dtingchebao&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
						//String authurl =   "https://open.weixin.qq.com/connect/oauth2/authorize?appid="+Constants.WXPUBLIC_APPID+"&redirect_uri=http%3A%2F%2Fwww.tingchebao.com%2Fzld%2Fcarowner.do%3Faction%3Dgetobonus%26id%3D"+bid+"%26operate%3Dcaibonus&response_type=code&scope=snsapi_userinfo&state=STATE#wechat_redirect";
						response.sendRedirect(authurl);
						return null;
					}
				}
				if(user!=null&&(wximgurl==null || wximgurl.length()<1)){//����΢��ͷ��
					setWxUser(wxkeys[0], wxkeys[1], mobile, (Long)user.get("id"),bid);
				}
				//���Ƿ������
				if(user!=null&&user.get("id")!=null){
					Map utmMap = daService.getMap("select amount from order_ticket_detail_tb where uin =? and otid=? ", new Object[]{user.get("id"),bid});
					if(utmMap!=null&&utmMap.get("amount")!=null){//�����
						request.setAttribute("money", utmMap.get("amount"));
						request.setAttribute("mobile", user.get("mobile"));
						String ret = caiBonusList(request, (Long)user.get("id"),bid);
						return mapping.findForward(ret);
					}
				}
				
				String target ="caibouns";
				if(user!=null){//��ע���û���ֱ������
					mobile = (String)user.get("mobile");
					target = caiBonusRet(request, bid, (Long)user.get("id"), mobile, wxkeys[0], wxkeys[1]);
				}else {//��ҳ�������ֻ���
					caiBonusList(request,null,bid);
					request.setAttribute("bid",bid);
					request.setAttribute("openid", wxkeys[0]);
					request.setAttribute("acctoken", wxkeys[1]);
				}
				logger.error(">>>>>>>>>>>>>>>>>>>>carhonbai target:"+target);
				return mapping.findForward(target);
			}else if(operate.equals("caibonusret")){//ȡ����������ȫ���������
				openid = RequestUtil.getString(request, "openid");
				String accToken = RequestUtil.getString(request, "acctoken");
				if(uin!=null&&uin!=-1){
					int isnewuser=setWxUser(openid, accToken, mobile, uin,bid);
					if(isnewuser==-1){
						request.setAttribute("message", "��������ֻ����Ѱ󶨣���ֱ�ӽ���΢�ź���ȡ��");
						return mapping.findForward("error");
					}else if(isnewuser==-2){
						request.setAttribute("message", "��ǰ΢���˻�����ȡ��ͣ��ȯ���º�������ٴ���ȡ��");
						return mapping.findForward("error");
					}
				}
				if(uin!=null&&uin!=-1){
					Map utmMap = daService.getMap("select amount from order_ticket_detail_tb where uin =? and otid=? ", new Object[]{uin,bid});
					if(utmMap!=null&&utmMap.get("amount")!=null){//�����
						request.setAttribute("money", utmMap.get("amount"));
						request.setAttribute("mobile", mobile);
						String ret = caiBonusList(request, uin,bid);
						return mapping.findForward(ret);
					}
				}
				String target = caiBonusRet(request,bid,uin,mobile,openid,accToken);
				return mapping.findForward(target);
			}
		}
		String reslut = StringUtils.createJson(infoMap);
		//logger.info(reslut);
		AjaxUtil.ajaxOutput(response,reslut);
		//AjaxUtil.ajaxOutput(response, StringUtils.createXML(infoMap));
		return null;
	}
	
	private String getMesg(HttpServletRequest request, Long uin) {
		Long maxid = RequestUtil.getLong(request, "maxid", -1L);
		Integer page = RequestUtil.getInteger(request, "page", 1);
		if(maxid>-1){
			Long count = daService.getLong("select count(ID) from user_message_tb where uin=? and id>?", new Object[]{uin,maxid});
			return count+"";
		}else{
			List<Object> params = new ArrayList<Object>();
			params.add(uin);
			List<Map<String, Object>> list = daService.getAll("select id,type,ctime,content,title from user_message_tb where uin=? order by id desc",
					params,page,10);
			return StringUtils.createJson(list);
		}
	}

	private String sweepTicket(HttpServletRequest request, Long uin,String mobile) {
		Long ntime = System.currentTimeMillis()/1000;
		Map<String,Object> infoMap = new HashMap<String, Object>();
		Long codeid = RequestUtil.getLong(request, "codeid", -1L);
		infoMap.put("type", 3);
		String info = null;
		if(codeid != -1){
			Map<String, Object> codeMap = daService
					.getMap("select * from qr_code_tb where id=? and type=? and state=? and ticketid is not null and comid is not null and uid is not null ",
							new Object[] { codeid, 6, 0 });
			String carnumber = publicMethods.getCarNumber(uin);
			if(codeMap != null){
				Long ticketid = (Long)codeMap.get("ticketid");
				Map<String, Object> ticketMap = daService.getMap("select * from ticket_tb where id=? and limit_day>? and type=? and uin is null ",
								new Object[] { ticketid, System.currentTimeMillis() / 1000, 1 });
				logger.error("������ɨ����ר��ȯ>>>mobile:"+mobile+",codeid:"+codeid+",uin:"+uin+",ticketid:"+ticketid);
				if(ticketMap != null){
					Double score = Double.valueOf(codeMap.get("score") + "");
					Long uid = (Long)codeMap.get("uid");
					Long comid = (Long)codeMap.get("comid");
					Map<String, Object> uidMap = daService.getMap("select nickname,reward_score from user_info_tb where id=? ", new Object[]{uid});
					Map<String, Object> comMap = daService.getMap("select company_name from com_info_tb where id=? ", new Object[]{comid});
					
					Double reward_score = Double.valueOf(uidMap.get("reward_score") +"");
					logger.error("������ɨ����ר��ȯ>>>mobile:"+mobile+",codeid:"+codeid+",uin:"+uin+",ticketid:"+ticketid+",score:"+reward_score);
					if(reward_score >= score){//type=0�����������
						List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
						//��ά��
						Map<String, Object> codeSqlMap = new HashMap<String, Object>();
						
						Map<String, Object> ticketSqlMap = new HashMap<String, Object>();
						
						Map<String, Object> scoreSqlMap = new HashMap<String, Object>();
						//������ϸ
						Map<String, Object> scoreAccountSqlMap = new HashMap<String, Object>();
						
						codeSqlMap.put("sql", "update qr_code_tb set state=? where id=? ");
						codeSqlMap.put("values", new Object[] { 1, codeid });
						bathSql.add(codeSqlMap);
						
						ticketSqlMap.put("sql", "update ticket_tb set uin=? where id=? ");
						ticketSqlMap.put("values", new Object[] { uin, ticketid});
						bathSql.add(ticketSqlMap);
						
						scoreAccountSqlMap.put("sql", "insert into reward_account_tb (uin,score,type,create_time,remark,target,ticket_id) values(?,?,?,?,?,?,?)");
						scoreAccountSqlMap.put("values", new Object[]{uid,score,1,ntime,"ͣ��ȯ ɨ��",2,ticketid});
						bathSql.add(scoreAccountSqlMap);
						
						scoreSqlMap.put("sql", "update user_info_tb set reward_score=reward_score-? where id=? ");
						scoreSqlMap.put("values", new Object[]{score, uid});
						bathSql.add(scoreSqlMap);
						boolean b = daService.bathUpdate(bathSql);
						logger.error("������ɨ����ר��ȯ>>>mobile:"+mobile+",codeid:"+codeid+",uin:"+uin+",uid:"+uid+",comid:"+comid+",b:"+b);
						if(b){
							info="{\"id\":\""+ticketid+"\",\"money\":\""+ticketMap.get("money")+"\",\"cname\":\""+comMap.get("company_name")+"\",\"type\":\""+ticketMap.get("type")+"\",\"fee\":{\"id\":\""+uid+"\",\"name\":\""+uidMap.get("nickname")+"\"}}";
							Map<String, Object> rMap = new HashMap<String, Object>();
							rMap.put("uin", uid);
							rMap.put("score", score);
							rMap.put("tmoney", ticketMap.get("money"));
							rMap.put("carnumber", carnumber);
							logService.insertParkUserMesg(7, rMap);
						}else{
							logger.error("������ɨ����ר��ȯ>>>mobile:"+mobile+",codeid:"+codeid+",uin:"+uin+",ticketid:"+ticketid+"b:"+b);
							info="{\"id\":\"-1\"}";
						}
					}else{
						logger.error("������ɨ����ר��ȯ>>>�շ�Ա���ֲ���mobile:"+mobile+",codeid:"+codeid+",uin:"+uin+",ticketid:"+ticketid+",�շ�Աʣ��score:"+reward_score+",�˴����Ļ��֣�score:"+score);
						info="{\"id\":\"-1\"}";
					}
				}else{
					logger.error("������ɨ����ר��ȯ>>>mobile:"+mobile+",codeid:"+codeid+",uin:"+uin+",��ά�����");
					info="{\"id\":\"-1\"}";//�ö�ά����ʧЧ
				}
			}else{
				logger.error("������ɨ����ר��ȯ>>>mobile:"+mobile+",codeid:"+codeid+",uin:"+uin+",��ά���ѱ���ȡ");
				info="{\"id\":\"-2\"}";//�ö�ά����ʧЧ
			}
		}else{
			info="{\"id\":\"-1\"}";
		}
		String result="{\"type\":\"3\",\"info\":"+info+"}";
		return  result;
	}

	private String oBparms(HttpServletRequest request) {
		Long bid = RequestUtil.getLong(request, "bid", -1L);//���ĺ�����
		Map<String, Object> map = daService.getMap("select * from order_ticket_tb where id = ?", new Object[]{bid});
		if(map!=null){
			Integer type = (Integer)map.get("type");
			String imgurl = "images/bonus/order_bonu.png";
			String title = CustomDefind.getValue("TITLE");
			if(type==3){
				title="ͨ����֤1246���";
				imgurl = "images/bonus/auth_ticket.png";
			}else if(type==4){
				title="ͣ������ֵͣ��ȯ�����";
				imgurl = "images/bonus/recharge.png";
			}else if(type==5){
				title="��ɻ�ͣ��ȯ�����";
				imgurl = "images/flygame/share_b.png";
			}
			return "{\"imgurl\":\""+imgurl+"\",\"title\":\""+title+"\"," +
					"\"description\":\""+map.get("bwords")+"\",\"url\":\"carowner.do?action=getobonus\",\"total\":\""+map.get("money")+"\",\"bnum\":\""+map.get("bnum")+"\"}";
		}
		return null;
	}

	private String hBonus(HttpServletRequest request, Long uin) {
		String key = memcacheUtils.readHBonusCache();
		String version = RequestUtil.getString(request, "version");
		if(!version.equals(""))
			logger.error(">>>>д��汾��:"+version+","+daService.update("update user_info_tb set version=? where id =? ", new Object[]{version,uin}));
		if(key!=null&&key.equals("1")){//���տ����ѿ�
			return "{\"imgurl\":\"hbonou_ltip.jpg\",\"sharable\":\"0\"}";
		}
		return "{}";
	}

	private String regCarMsg(HttpServletRequest request, Long uin) {
		Long ntime = System.currentTimeMillis()/1000;
		Long tid = daService.getkey("seq_transfer_url_tb");
		String url = "http://www.tingchebao.com/zld/turl?p="+tid;
		//String url = "http://192.168.10.240/zld/turl?p="+tid;
		int result = daService.update("insert into transfer_url_tb(id,url,ctime,state) values (?,?,?,?)",
				new Object[]{tid,"regparker.do?action=toregpage&recomcode="+uin,
						ntime,0});
		if(result==1)
			return url;
		else {
			return "�Ƽ�ʧ��!";
		}
	}

	private String getPKUser(HttpServletRequest request,Long comId,Long uin) {
		Long ntime = System.currentTimeMillis()/1000;
		Map<String,Object> infoMap = new HashMap<String, Object>();
		Long uid = RequestUtil.getLong(request, "uid", -1L);
		Map orderMap = daService.getMap("select * from order_tb o where o.uin=? and o.state=? and o.comid = " +
				"(select u.comid from user_info_Tb u where u.id =? ) ", new Object[]{uin,0,uid});
		String result ="{}";
		if(orderMap==null||orderMap.isEmpty()){
			 orderMap = daService.getMap("select * from order_tb o where o.uin=? and o.state=? and pay_type=?" +
			 		" and o.end_time >? and o.comid = " +
					"(select u.comid from user_info_Tb u where u.id =? ) order by end_time desc ", 
					new Object[]{uin,1,1,ntime-15*60,uid});
		}
		if(orderMap!=null&&!orderMap.isEmpty()){//�����Ѵ��ڵĶ���������ж�������δ��������ֽ����ʱ���������շ���Ϣ
			Integer state = (Integer)orderMap.get("state");
			String cname =""; 
			String address = "";
			Map comMap = daService.getMap("select company_name,address from com_info_tb where id=?",new Object[]{orderMap.get("comid")});
			if(comMap!=null){
				cname = (String)comMap.get("company_name");
				address = (String)comMap.get("address");
			}
			if(state==0){//δ���㣬�󶨶���
				Long btime = (Long)orderMap.get("create_time");
				Long end = ntime;
				Integer pid = (Integer)orderMap.get("pid");
				Integer car_type = (Integer)orderMap.get("car_type");//0��ͨ�ã�1��С����2����
				if(pid>-1){
					infoMap.put("total",publicMethods.getCustomPrice(btime, end, pid));
				}else {
					infoMap.put("total",publicMethods.getPrice(btime, end, comId, car_type));	
				}
				infoMap.put("btime", btime);
				infoMap.put("etime",end);
				infoMap.put("parkname", cname);
				infoMap.put("address",address);
				infoMap.put("orderid", orderMap.get("id"));
				infoMap.put("state",orderMap.get("state"));
				infoMap.put("parkid", orderMap.get("comid"));
				result= StringUtils.createJson(infoMap);
			}else if(state==1){//�ѽ���
				Map<String, Object> infomMap = new HashMap<String, Object>();
				comId = (Long)orderMap.get("comid");
				Long btime = (Long)orderMap.get("create_time");
				Long etime = (Long)orderMap.get("end_time");
				infomMap.put("parkname", cname);
				infomMap.put("address",address);
				infomMap.put("btime", btime);
				infomMap.put("etime", etime);
				infomMap.put("total", StringUtils.formatDouble(orderMap.get("total")));
				infomMap.put("state",orderMap.get("pay_type"));// -- 0:δ���㣬1����֧����2��֧�����
				infomMap.put("orderid",orderMap.get("id"));
				result = StringUtils.createJson(infomMap);
			}
		}
		if(result.equals("{}")){//û�ж����������շ���Ϣ
			Map uMap = daService.getMap("select u.id,u.nickname as name,c.company_name as parkname from " +
					"user_info_Tb u, com_info_tb  c where u.comid=c.id and u.id=?", new Object[]{uid});
			result = StringUtils.createJson(uMap);
		}
		logger.info(">>>>"+result);
		return null;
	}

	private String epay(HttpServletRequest request, Long uin) {
		Long ntime = System.currentTimeMillis()/1000;
		String version = RequestUtil.processParams(request, "version");
		Long uid = RequestUtil.getLong(request, "uid", -1L);
		Double money  = RequestUtil.getDouble(request, "total", 0d);
		Long ticketId = RequestUtil.getLong(request, "ticketid", -1L);
		Integer bind_flag = RequestUtil.getInteger(request, "bind_flag", 1);//0:δ���˻���1���Ѱ��˻�
		//String version = RequestUtil.processParams(request, "version");
		// comId, total, uin, uid, ticketId, comName, ptype
		Long _comId = daService.getLong("select comid from user_info_tb where id=? ", new Object[]{uid});
		String carNumber = publicMethods.getCarNumber(uin);
		Long orderId = daService.getkey("seq_order_tb");
		int	result = publicMethods.epay(_comId, money, uin, uid, ticketId,carNumber, 0, bind_flag,orderId,null);
		if(result==5){
			result = 1;
			//��֧���ɹ���Ϣ���շ�Ա
			logService.insertParkUserMessage(_comId, 2,uid,carNumber, -1L,money,"", 0, ntime, ntime+10,0, null);
		}
		logger.error(">>>>>>>>>>>����ֱ��֧���������أ�"+result);
		if(version.equals("2")){
			if(result==1){//֧���ɹ�����ѯ��û��ֱ�����
				Long count = null;
				Map bMap  =daService.getMap("select id from order_ticket_tb where uin=? and  order_id=? and ctime>? ",
						new Object[]{uin,orderId,ntime-5*60});//�����ǰ�ĺ��
				if(bMap!=null){
					Long btime = (Long)bMap.get("btime");
					if(btime!=null&&btime>10000){//�Ѿ�����������ٷ���
						bMap=null;
					}
				}
				if(bMap!=null&&bMap.get("id")!=null)
					count = (Long)bMap.get("id");
				logger.error(">>>>>>>>>>json>ֱ��������֧���������أ�"+"{\"result\":\"1\",\"tips\":\""+count+"\",\"errmsg\":\""+orderId+"\"}");
				if(count!=null&&count>0){
					return "{\"result\":\"1\",\"tips\":\""+count+"\",\"errmsg\":\""+orderId+"\"}";
				}else {
//					if(client_type==0)//android
//						AjaxUtil.ajaxOutput(response, "{\"result\":\"1\",\"tips\":\"on\"}");
//					else {//ios
					return "{\"result\":\"1\",\"tips\":\"\",\"errmsg\":\""+orderId+"\"}";
//					}
				}
				
//				Long count = null;
//				Map bMap  =daService.getMap("select id from bouns_tb where uin=? and  order_id=?",new Object[]{uin,998});
//				if(bMap!=null&&bMap.get("id")!=null)
//					count = (Long)bMap.get("id");
//				if(count!=null&&count>0){
//					AjaxUtil.ajaxOutput(response, "{\"result\":\"1\",\"tips\":\""+count+"\"}");
//				}else {
//					AjaxUtil.ajaxOutput(response, "{\"result\":\"1\",\"tips\":\"\"}");
//				}
//				logger.error(">>>>>>>>>>json>ֱ��������֧���������أ�"+"{\"result\":\"1\",\"tips\":\""+count+"\"}");
			}else {
				logger.error(">>>>>>>>>json>>ֱ��������֧���������أ�"+"{\"result\":\""+result+"\",\"tips\":\"\"}");
				return  "{\"result\":\""+result+"\",\"tips\":\"\",\"errmsg\":\"\"}";
			}
		}else {
			return result+"";
		}
	}

	private String getAccount(HttpServletRequest request, Object balance,Long uin,String mobile) {
		Double total = RequestUtil.getDouble(request, "total", 0d);
		Long orderId = RequestUtil.getLong(request, "orderid", -1L);//��ͨ���������������
		Long uid = RequestUtil.getLong(request, "uid", -1L);//ֱ��ʱ�����շ�Ա��� 
		Integer ptype = RequestUtil.getInteger(request, "ptype", -1);//0�˻���ֵ��1���²�Ʒ��2ͣ���ѽ��㣻3ֱ��;4���� 5����ͣ��ȯ
		Integer utype = RequestUtil.getInteger(request, "utype", 0);//0��ͨѡȯ��Ĭ�ϣ�1���ô���֧������ͣ��ȯ
		Integer source = RequestUtil.getInteger(request, "source", 0);//0�ͻ��� 1��΢�Ź��ں�
		
		Long parkId = null;
		Map<String, Object> ticketMap = null;
		if(orderId != -1){
			Map<String, Object> orderMap = daService.getMap("select comid,uid from order_tb where id=?", new Object[]{orderId});
			if(orderMap!=null){
				parkId = (Long)orderMap.get("comid");
				uid = (Long)orderMap.get("uid");
			}
		}else if(uid != -1){
			Map<String, Object> uidMap = daService.getMap("select comid from user_info_tb where id = ? ", new Object[]{uid});
			if(uidMap!=null){
				parkId = (Long)uidMap.get("comid");
			}
		}
		logger.error("chooseTicket>>>uin"+uin+",total:"+total+",orderId:"+orderId+",uid:"+uid+",parkId:"+parkId+",ptype:"+ptype);
		if(uid != -1 && parkId != null && total > 0){
			boolean parkuserblack = publicMethods.isBlackParkUser(uid, true);
			boolean parkblack = publicMethods.isBlackParkUser(parkId, false);
			boolean userblack = publicMethods.isBlackUser(uin);
			boolean isAuth = publicMethods.isAuthUser(uin);
			
			logger.error("chooseTicket:uin"+uin+",orderId:"+orderId+",parkuserblack:"+parkuserblack+",parkblack:"+parkblack+",userblack:"+userblack+",ptype:"+ptype+",isAuth:"+isAuth);
			if(parkId != null && !parkuserblack && !parkblack && !userblack){
				List<Map<String, Object>> list = commonMethods.chooseTicket(uin, total, utype, uid, isAuth, ptype, parkId, orderId, source);
				if(list != null && !list.isEmpty()){
					Map<String, Object> map = list.get(0);
					if(map.get("iscanuse") != null && (Integer)map.get("iscanuse") == 1){
						ticketMap = map;
					}
				}
			}
		}
		String ret = "{\"balance\":\""+balance+"\",\"tickets\":[]}";
		String tickets = "[";
		if(ticketMap!=null)
			tickets +=StringUtils.createJson(ticketMap);//"{\"id\":\""+ticketMap.get("id")+"\",\"money\":\""+ticketMap.get("money")+"\"}";
		tickets +="]";
		ret = ret.replace("[]", tickets);
		return ret;
	}

	private String getTickets(HttpServletRequest request, Long uin,String mobile) {
		Long ntime = System.currentTimeMillis()/1000;
		List<Map<String, Object>> ticketMap = daService.getAll("select t.create_time beginday ,limit_day limitday,resources," +
				"t.state,t.money,company_name cname,utime ,pmoney,t.type " +
				"from ticket_tb t left join com_info_tb c on t.comid=c.id where uin = ? and t.type<? order by limit_day", new Object[]{uin,2});
		
		//Integer ptype  = RequestUtil.getInteger(request, "ptype", -1);
		//����û�з��ص��˻��е�ͣ��ȯ
		boolean isback  = commonMethods.checkBonus(mobile, uin);
		//logger.error(">>>>");
		if(ticketMap!=null&&!ticketMap.isEmpty()){
			for(Map<String, Object> tMap : ticketMap){
				Long limitDay = (Long)tMap.get("limitday");
				Integer money = (Integer)tMap.get("money");
				Integer res = (Integer)tMap.get("resources");
				tMap.put("isbuy", res);
				tMap.remove("resources");
				if(ntime >limitDay)
					tMap.put("exp", 0);
				else {
					tMap.put("exp", 1);
				}
				Integer state = (Integer)tMap.get("state");
				Integer limit =Integer.valueOf(CustomDefind.getValue("TICKET_LIMIT"));
				Integer ttype = (Integer)tMap.get("type");
				Integer topMoney = CustomDefind.getUseMoney(money.doubleValue(), 1);
				tMap.put("desc", "��"+topMoney+"Ԫ���Եֿ�ȫ��");
				if(ttype==1||res==1){
					tMap.put("desc", "��"+(money+limit)+"Ԫ���Եֿ�ȫ��");
				}
				if(res==1&&state==0){
					tMap.put("desc", "��"+(money+1)+"Ԫ���Եֿ�ȫ��,���ں��˻�"+StringUtils.formatDouble(tMap.get("pmoney"))+"Ԫ�������˻�");
				}
			}
//			System.err.println(StringUtils.createJson(ticketMap).replace("null", ""));
			return StringUtils.createJson(ticketMap).replace("null", "");
		}
		return "[]";
	}

	private List<Map<String, Object>> getComment(HttpServletRequest request,
			Long comId) {
		List<Map<String, Object>> comList =daService.getAll("select * from com_comment_tb where comid=? order by id desc",
				new Object[]{comId});
		List<Map<String, Object>> resultMap = new ArrayList<Map<String,Object>>();
		if(comList!=null&&comList.size()>0){
			for(Map<String, Object> map : comList){
				Map<String, Object> iMap = new HashMap<String, Object>();
				Long createTime = (Long)map.get("create_time");
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(createTime*1000);
				String times = TimeTools.getTime_MMdd_HHmm(createTime*1000);
				Long uid = (Long)map.get("uin");
				iMap.put("parkId",comId);// ���۵ĳ���ID
				iMap.put("date", times.substring(0,5));// �������ڣ�7-24
				iMap.put("week", "����"+getWeek(calendar.get(Calendar.DAY_OF_WEEK)));//�������������ڼ���������
				iMap.put("time", times.substring(6));// ���۵ĳ���ID
				iMap.put("info",  map.get("comment"));//�������ݣ���������һ�󴮷ϻ�������
				iMap.put("user", getCarNumber(uid));// �����ߣ����������ƺţ���A***A111��
				resultMap.add(iMap);
			}
		}
		return resultMap;
	}

	private int comment(HttpServletRequest request, Long comId, Long uin) {
		Long ntime = System.currentTimeMillis()/1000;
		String comment = AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "comment"));
		if(comId==null||comId==-1){
			Long orderid =RequestUtil.getLong(request, "orderid", -1L);
			if(orderid>0){
				Map useMap = daService.getMap("select comid from order_tb where id =? ", new Object[]{orderid});
				if(useMap!=null&&useMap.get("comid")!=null)
					comId = (Long)useMap.get("comid");
			}
		}
		int result = 0;
		if(comId!=null&&comId!=-1&&uin!=-1&&!comment.equals("")){
			//20150612���---����ֻ�ܶԳ�������һ��
			Long count = daService.getLong("select count(ID) from com_comment_tb where uin=? and comid=?", new Object[]{uin,comId});
			if(count<1)
				result = daService.update("insert into com_comment_tb (comid,uin,comment,create_time)" +
					" values(?,?,?,?)", new Object[]{comId,uin,comment,ntime});
			else {
				result = daService.update("update com_comment_tb set comment=? where uin=? and comid=? ", new Object[]{comment,uin,comId});
			}
		}
		if(result>1)
			result=1;
		return result;
	}

	private int praise(HttpServletRequest request, Long comId, Long uin) {
		Long ntime = System.currentTimeMillis()/1000;
		Integer praise = RequestUtil.getInteger(request, "praise", -1);
		int result = 0;
		if(comId!=-1&&uin!=null&&uin!=-1){
			try {
				//�ڴ˳�������һ�����ϲſ������� 20150205
				Long ocount = daService.getLong("select count(id) from order_tb where comid=? and uin=? and state=?  ",
						new Object[]{comId,uin,1});
				if(ocount>0)
					result = daService.update("insert into com_praise_tb (comid,uin,praise,create_time)"
								+ "values (?,?,?,?)", new Object[] { comId,uin, praise ,ntime});
			} catch (Exception e) {
				e.printStackTrace();
			}
			/*if(result==1&&praise==0){//�����۷�
				List<Map> list = daService.getAll("select id from user_info_tb where comid=? and state=? ",
						new Object[]{comId,0});
				if(list!=null&&!list.isEmpty()){
					for(Map map : list){
						logService.updateScroe(3,(Long) map.get("id"),comId);
					}
				}
			}*/
		}
		return result;
	}

	private int editPhone(HttpServletRequest request, Long uin,String mobile) {
		String _mobile = RequestUtil.processParams(request, "newmobile");
		int result = 0;
		if(uin!=null&&!_mobile.equals(""))
			try {
				result = daService.update("update user_info_Tb set mobile = ? where mobile =?",
						new Object[]{_mobile,mobile});
			} catch (Exception e) {
				if(e.getMessage().indexOf("user_info_tb_mobile_key")!=-1){
					result= 2;
				}else {
					result =3;
				}
				logger.error("=eidtphone="+e.getMessage());
			}
		return result;
	}
	
	private int editCarNumber(HttpServletRequest request, Long uin) {
		String carNumber=AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "carnumber"));
		carNumber = carNumber.toUpperCase().trim();
		carNumber = carNumber.replace("I", "1").replace("O", "0");
		logger.error(">>>>>editcarnumber:"+carNumber+",uin:"+uin);
		int result = 0;
		if(uin!=null&&!carNumber.equals("")) {
			try {
				Map oldCarMap = daService.getMap("select car_number from car_info_tb where uin = ? ", new Object[]{uin});
				result = daService.update(
						"update car_info_tb set car_number=? where uin=?",
						new Object[] { carNumber, uin });
				if(result==1&&oldCarMap!=null){
					publicMethods.syncUserAddPlateNumber(uin, carNumber, (String)oldCarMap.get("car_number"));
				}
			} catch (Exception e) {
				if(e.getMessage().indexOf("car_info_tb_car_number_key")!=-1){
					result= 2;
				}else {
					result =3;
				}
				logger.error("=editcarnumber="+e.getMessage());
			}
		}
		return result;
	}

	private Map<String, Object> getProf(HttpServletRequest request, Long uin) {
		Map<String,Object> infoMap = new HashMap<String, Object>();
		Map profileMap = daService.getPojo("select low_recharge,limit_money,auto_cash from user_profile_tb where uin=?",new Object[]{uin});
		if(profileMap!=null){
			Integer autoCash = (Integer)profileMap.get("auto_cash");
			//@"10", @"25", @"50", @"100", @"0"
			Integer lre = (Integer)profileMap.get("low_recharge");
			if(lre!=10&&lre!=25&&lre!=50&&lre!=100)
				lre = 0;
			infoMap.put("low_recharge", lre);
			if(autoCash!=null&&autoCash==1){
				infoMap.put("limit_money", profileMap.get("limit_money"));
			}else {
				infoMap.put("limit_money", 0);
			}
			//infoMap.put("auto_cash", profileMap.get("auto_cash"));
		}else {
			infoMap.put("low_recharge", 0);
			infoMap.put("limit_money", 25);
		}
		logger.error(">>>>>>>>>>>>>>>getprofile:"+infoMap);
		return infoMap;
	}

	private int setPro(HttpServletRequest request, Long uin) {
		Long ntime = System.currentTimeMillis()/1000;
		Integer low_recharge=RequestUtil.getInteger(request, "low_recharge", 0);
		Integer limit_money=RequestUtil.getInteger(request, "limit_money", 0);
		Long count = daService.getLong("select count(*) from user_profile_tb where uin=?", new Object[]{uin});
		logger.error(">>>>>>>>>>>>>>>lowre:"+low_recharge+",limitmoney:"+limit_money);
		Long time = ntime;
		Integer auto_cash = 0;
		if(limit_money==-1||limit_money>0){
			auto_cash=1;
		}
		int result = 0;
		if(count>0){//update
			result = daService.update("update user_profile_tb set low_recharge=?," +
					" auto_cash=?,limit_money=?,update_time=? where uin=?",
					new Object[]{low_recharge,auto_cash,limit_money,time,uin});
			logger.error(">>>>>>>>>>>>>>>update profile uin:"+uin+",ret:"+result);
			if(result==1)
				publicMethods.syncUserToBolink(uin);//ͬ��������
			/*if(result==1){//֪ͨ����ƽ̨���޸��û��޶�
				//��ѯ�����Ƿ�ͬ����������ƽ̨������ϴ�ͬ�����޶�
				Map userMap = daService.getMap("select balance ,bolink_limit from user_info_tb u " +
						"left join user_profile_tb p on p.uin=u.id where  u.id =?   and u.union_state>? ", new Object[]{uin,0});
				logger.error("update profile,user set :"+userMap+",��ǰ�޸ģ�limitmoney :"+limit_money);
				if(userMap!=null){
					Double bolinkLimit = StringUtils.formatDouble(userMap.get("bolink_limit"));
					Double balance =StringUtils.formatDouble(userMap.get("balance"));
					boolean isSend = false;
					Double money = Double.valueOf(limit_money);
					
					if(auto_cash==0){
						if(bolinkLimit>0){//�������޶����0�����û����������˲��Զ�֧������Ҫ֪ͨ�����޸��޶�
							isSend=true;
							money=0.0;
						}
					}else {
						if(limit_money>0){
							if(limit_money<bolinkLimit){//�û��޸��޶�С���˲������޶��Ҫ֪ͨ�����޸��޶�,�����ܸ������
								isSend=true;
								if(limit_money>balance){
									money = balance;
								}
							}else if(bolinkLimit!=limit_money.doubleValue()){//�û��޸��޶�����˲������޶��Ҫ֪ͨ�����޸��޶�,�����ܸ������
								if(limit_money>balance){
									if(balance!=bolinkLimit){
										isSend=true;
										money = balance;
									}
								}else {
									isSend=true;
								}
							}
						}else {//�û����޶���
							if(bolinkLimit!=balance){
								isSend=true;
								money = balance;
							}
						}
					}
					if(isSend){
						publicMethods.syncUserLimit(uin, money);
					}
				}
			}*/
		}else {
			result = daService.update("insert into user_profile_tb (low_recharge,auto_cash," +
					"create_time,limit_money,update_time,uin) values (?,?,?,?,?,?)", 
					new Object[]{low_recharge,auto_cash,time,limit_money,time,uin});
		}
		logger.error(">>>>>>>>>>>>>>>sertprofile:limit_money:"+limit_money+",auto_cash:"+auto_cash+",result:"+result);
		return result;
	}

	private int setProfile(HttpServletRequest request, Long uin) {
		Long ntime = System.currentTimeMillis()/1000;
		Integer low_recharge=RequestUtil.getInteger(request, "low_recharge", 0);
		Integer voice_warn=RequestUtil.getInteger(request, "voice_warn", 0);
		Integer auto_cash=RequestUtil.getInteger(request, "auto_cash", 0);
		Integer enter_warn=RequestUtil.getInteger(request, "enter_warn", 0);
		Long count = daService.getLong("select count(*) from user_profile_tb where uin=?", new Object[]{uin});
		Long time = ntime;
		int result = 0;
		if(count>0){//update
			result = daService.update("update user_profile_tb set low_recharge=?," +
					" voice_warn=?,auto_cash=?,enter_warn=?,update_time=? where uin=?",
					new Object[]{low_recharge,voice_warn,auto_cash,enter_warn,time,uin});
		}else {
			result = daService.update("insert into user_profile_tb (low_recharge,voice_warn,auto_cash,enter_warn," +
					"create_time,update_time,uin) values (?,?,?,?,?,?,?)", 
					new Object[]{low_recharge,voice_warn,auto_cash,enter_warn,time,time,uin});
		}
		return result;
	}

	private List<Map<String, Object>> accountDetail(HttpServletRequest request,Long uin) {
		Integer type = RequestUtil.getInteger(request, "type", 2);//2ȫ����0����ֵ��1������
		Integer pageNum = RequestUtil.getInteger(request, "page", 1);//ȡ�ڼ�ҳ
		Long orderid = RequestUtil.getLong(request, "orderid", -1L);//�����ĸ��������˻���ϸ��Ĭ�ϺͶ����޹�
		String countSql = "select count(*) from user_account_tb where uin=?";
		String sql ="select create_time,amount,type,remark,pay_type from user_account_tb where uin=?"; 
		List<Object> params = new ArrayList<Object>();
		params.add(uin);
		if(type!=2){
			countSql +=" and type= ?";
			sql +=" and type=?";
			params.add(type);
		}
		if(orderid > 0){
			countSql +=" and orderid= ?";
			sql +=" and orderid=?";
			params.add(orderid);
		}
		Long count = daService.getCount(countSql, params);
		List<Map<String,Object>> allList =null;
		if(count>0){
			allList = daService.getAll(sql + " order by create_time desc ", params, pageNum, 15);
		}
		if(allList!=null){
			for(Map<String,Object> map : allList){
				Integer payType =(Integer)map.get("pay_type");
				// -- 0��1֧������2΢�ţ�3������4���+֧����,5���+΢��,6���+���� ,7ͣ������ֵ 
				// 8�����,9΢�Ź��ںţ�10΢�Ź��ں�+��11΢�Ŵ���ȯ,12Ԥ֧������ 13ͣ��ȯ�˿�
				String payName = "���";
				if(payType!=null){
					switch (payType) {
						case 1:
							payName = "֧����";				
							break;
						case 2:
							payName = "΢��";
							break;
						case 3:
							payName = "����";
							break;
						case 4:
							payName = "���+֧����";
							break;
						case 5:
							payName = "���+΢��";
							break;
						case 6:
							payName = "���+����";
							break;
						case 7:
							payName = "ͣ��ȯ";
							break;
						case 8:
							payName = "�����";
							break;
						case 9:
							payName = "΢�Ź��ں�";
							break;
						case 10:
							payName = "΢�Ź��ں�+���";
							break;
						case 11:
							payName = "΢�Ŵ���ȯ";
							break;
						case 12:
							payName = "Ԥ֧������";
							break;
						case 13:
							payName = "ͣ��ȯ�˿�";
							break;
						default:
							payName = "���";
					}
				}
				map.put("pay_name", payName);
			}
		}
		return allList;
	}
	
	private String prepay(HttpServletRequest request, Long comid, Long uin){
		Map<String, Object> infoMap = new HashMap<String, Object>();
		Long orderId = RequestUtil.getLong(request, "orderid", -1L);
		Long ticketId  =RequestUtil.getLong(request, "ticketid", -1L);
		Double money = RequestUtil.getDouble(request, "money", 0d);
		String wxp_orderid = RequestUtil.processParams(request, "wxp_orderid");
		logger.error("prepay>>>orderid:"+orderId+",uin:"+uin+",comid:"+comid+",money:"+money+",ticketid:"+ticketId);
		int result = 0;
		if(orderId > 0){
			Map<String, Object> orderMap = daService.getMap("select * from order_tb where id=? and state=? ", 
					new Object[]{orderId, 0});
			if(orderMap != null){
				Double prefee = 0d;
				if(orderMap.get("total") != null){
					prefee = Double.valueOf(orderMap.get("total") + "");
				}
				logger.error("prepay>>>orderid:"+orderId+",uin:"+uin+",prefee:"+prefee+",ticketid:"+ticketId);
				if(prefee == 0){
					int r = publicMethods.prepay(orderMap, money, uin, ticketId, 0, 1, wxp_orderid);
					result = r;
				}
			}
		}
		if(comid == 20130){
			String re = commonMethods.sendPrepay2Baohe(orderId, result, money, 0);
			logger.error("toprepay>>>orderid:"+orderId+",uin:"+uin+",re:"+re);
		}
		infoMap.put("result", result);
		infoMap.put("money", money);
		infoMap.put("orderid", orderId);
		infoMap.put("paytype", 0);
		return StringUtils.createJson(infoMap);
	}

	private String payOrder(HttpServletRequest request,Long comId,Long uin) {
		Long ntime = System.currentTimeMillis()/1000;
		String type = RequestUtil.processParams(request, "ptype");
		//version=2�����ش������json���� 
		String version = RequestUtil.processParams(request, "version");
		if(type.equals("1")){//������²�Ʒ
			int result  = buyProduct(request,uin);
			return result+"";
		}else if(type.equals("2")){//֧������
			Long orderId = RequestUtil.getLong(request, "orderid", -1L);
			Long ticketId  =RequestUtil.getLong(request, "ticketid", -1L);
			int result  =0;
			Double total = RequestUtil.getDouble(request, "total", 0d);
			logger.error("payorder>>>orderid:"+orderId+",type:"+type+",total:"+total+",version:"+version);
			Map orderMap =null;
			if(orderId!=-1){
			
				orderMap = daService.getMap("select * from order_tb where id=?", new Object[]{orderId});
				if(total==0)
					total = StringUtils.formatDouble(orderMap.get("total"));
				int re = publicMethods.payOrder(orderMap,total, uin, 2,0,ticketId,null,-1L, -1L);
				int state =1;//Ĭ��֧�����ɹ�
				logger.info(">>>>>>>>>>>>�����˻�֧�� ��"+re+",orderid:"+orderId+",uin:"+uin);
				comId = (Long)orderMap.get("comid");
				Long btime = (Long)orderMap.get("create_time");
				Long etime  = (Long)orderMap.get("end_time");
				if(etime==null)
					etime=ntime;
				String duration = StringUtils.getTimeString(btime, etime);
				String carNumber = (String)orderMap.get("car_number");
				if(carNumber==null||"".equals(carNumber)||"���ƺ�δ֪".equals(carNumber))
						carNumber = publicMethods.getCarNumber(uin);
				if(re==5){
					state  = 2;
					result = 1;
					if(comId==20130){//���͹�˾�Ĳ��Զ������������ǵĽӿڷ��Ͷ���֧��״̬
						String sr = commonMethods.sendOrderState2Baohe(orderId, result, total, 0);
						logger.error(">>>>>>>>>>>>>baohe sendresult:"+sr+",orderid:"+orderId+",state:"+result+",total:"+total);
					}
					//��֧���ɹ���Ϣ���շ�Ա
					logService.insertParkUserMessage(comId, state, (Long)orderMap.get("uid"),carNumber, orderId, total,duration, 0, btime, etime, 0, null);
					
				}else 
					result=re;
				//����Ϣ������-----���֧��ֻ���ؽ�������ٷ���Ϣ===20140826
				//logService.doMessage(comId, state, uin,carNumber, orderId, Double.valueOf(orderMap.get("total")+""),duration, 0, btime, etime,0);
			}
			//{result:1,errmsg:"",tips:"bonusid"}
			if(version.equals("2")){
				if(result==1){//֧���ɹ�����ѯ��û�к��
					Long count = null;
					Map bMap  =daService.getMap("select id from order_ticket_tb where order_id=?",new Object[]{orderId});
					//Map bMap  =daService.getMap("select id from bouns_tb where order_id=?",new Object[]{orderId});
					if(bMap!=null&&bMap.get("id")!=null)
						count = (Long)bMap.get("id");
					logger.error(">>>>>>>>>>json>֧���������أ�"+"{\"result\":\"1\",\"tips\":\""+count+"\",\"errmsg\":\""+orderId+"\"}");
					if(count!=null&&count>0){
						return  "{\"result\":\"1\",\"tips\":\""+count+"\",\"errmsg\":\""+orderId+"\"}";
					}else {
//						if(client_type==0)//android
//							AjaxUtil.ajaxOutput(response, "{\"result\":\"1\",\"tips\":\"on\"}");
//						else {//ios
						return "{\"result\":\"1\",\"tips\":\"\",\"errmsg\":\""+orderId+"\"}";
//						}
					}
				}else {
					logger.error(">>>>>>>>>json>>֧���������أ�"+"{\"result\":\""+result+"\",\"tips\":\"\",\"errmsg\":\""+orderId+"\"}");
					return "{\"result\":\""+result+"\",\"tips\":\"\",\"errmsg\":\"\"}";
				}
			}else{
				logger.error(">>>>>>>>>>>֧���������أ�"+result);
				return result+"";
			}
		}
		return "0";
	}

	private void setBonusType(List<Map<String, Object>> list) {
		Long ntime = System.currentTimeMillis()/1000;
		for(Map<String, Object> map : list){
			Long btime = (Long)map.get("btime");
			Long exptime = (Long)map.get("exptime");
			Integer type = (Integer)map.get("type");
			if(btime==null){//δ��ȡ
				if(ntime>exptime){
					map.put("state", 0);//�ѹ���
				}else {
					map.put("state", 1);//δ����
				}
			}else {
				map.put("state", 2);//����ȡ
			}
			if(type==0){
				map.put("title", "����ͣ��ȯ���");
			}else if(type==1){
				map.put("title", "΢�Ŵ���ȯ���");
			}else if(type==2){
				map.put("title", "����ר��ȯ���");
			}else if(type==3){
				map.put("title", "��֤ͨ�������");
			}else if(type==4){
				map.put("title", "��ֵͣ��ȯ���");
			}else if(type==5){
				map.put("title", "��Ϸͣ��ȯ���");
			}else {
				map.put("title", "����ͣ��ȯ���");
			}
			map.remove("type");
			map.remove("btime");
		}
	}

	/**
	 * ����ͣ��ȯ
	 * @param request
	 * @return
	 */
	private String buyTicket(HttpServletRequest request,Long uin) {
		Integer value = RequestUtil.getInteger(request, "value", 0);
		Integer number = RequestUtil.getInteger(request, "number", 0);
		int result = publicMethods.buyTickets(uin,value,number,0);
		return "{\"result\":\""+result+"\",\"errmesg\":\"\"}";
	}
	//�������շ�Ա�Ĵ���
	private String reward(HttpServletRequest request) {
		Long ntime = System.currentTimeMillis()/1000;
		//http://192.168.199.240/zld/carowner.do?action=puserreward&mobile=18101333937&orderid=&ticketid=&uid=&money=
		String ret = "{\"result\":\"0\",\"errmsg\":\"����ʧ��\"}";
		Long ticketId = RequestUtil.getLong(request, "ticketid", -1L);
		Long orderId = RequestUtil.getLong(request, "orderid", -1L);
		Double money = RequestUtil.getDouble(request, "total", 0.0);
		Long uid = RequestUtil.getLong(request, "uid", -1L);
		String mobile = RequestUtil.getString(request, "mobile");
		Long uin = getUinByMobile(mobile);
		int result = 0;
		Long count = daService.getLong("select count(id) from parkuser_reward_tb where uin=? and order_id=? ", new Object[]{uin,orderId});
		if(count>0){
			ret = "{\"result\":\"-1\",\"errmsg\":\"�Ѵ��͹�\"}";
		}else {
			Long btime = TimeTools.getToDayBeginTime();
			if(uin!=-1&&uid!=-1&&orderId!=-1&&money>0){
				result = publicMethods.doparkUserReward(uin, uid, orderId, ticketId, money,0,1);
			}
			if(result==1){
				String carNumber = publicMethods.getCarNumber(uin);
				Long recount = daService.getLong("select count(id) from parkuser_reward_tb where uid =? and ctime >? ",
						new Object[]{uid,TimeTools.getToDayBeginTime()});
				Map<String, Object> tscoreMap = daService.getMap("select sum(score) tscore from reward_account_tb where type=? and create_time>? and uin=? ",
						new Object[] { 0, btime, uid });
				Long comid = -1L;
				if(tscoreMap != null && tscoreMap.get("tscore") != null){
					Double tscore = Double.valueOf(tscoreMap.get("tscore") + "");
					if(tscore >= 5000){
						comid = -2L;
						logger.error("���մ����Ѵ�����uid:"+uid+",tscore:"+tscore+",uin:"+uin);
					}
				}
				logger.error("rewardmessage>>uid:"+uid+",uin:"+uin+",recount:"+recount+",carnumber:"+carNumber);
				logService.insertParkUserMessage(comid,2,uid,carNumber,uin,money, ""+recount, 0,ntime,ntime+10,5, null);
				ret = "{\"result\":\"1\",\"errmsg\":\"���ͳɹ�\"}";
			}else if(result==-1){
				ret = "{\"result\":\"-2\",\"errmsg\":\"����\"}";
			}
		}
		return ret;
	}
	/*�������շ�Ա������**/
	private String commpuser(HttpServletRequest request) {
		Long ntime = System.currentTimeMillis()/1000;
		String comment = AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "comment"));
		Long orderid =RequestUtil.getLong(request, "orderid", -1L);
		String mobile = RequestUtil.getString(request, "mobile");
		Long uin = getUinByMobile(mobile);
		String result = "{\"result\":\"0\",\"errmsg\":\"����ʧ��\"}";
		if(uin!=null&&uin!=-1){
			Long count = daService.getLong("select count(ID) from parkuser_comment_tb where uin=? and order_id=? ", new Object[]{uin,orderid});
			if(count<1){
				Long uid = -1L;//�շ�Ա���
				if(orderid>0){
					Map useMap = daService.getMap("select uid from order_tb where id =? ", new Object[]{orderid});
					if(useMap!=null&&useMap.get("uid")!=null)
						uid = (Long)useMap.get("uid");
					if(uid!=null&&uid!=-1&&uin!=-1&&!comment.equals("")){
						int ret = daService.update("insert into parkuser_comment_tb (uid,uin,comments,ctime,order_id)" +
								" values(?,?,?,?,?)", new Object[]{uid,uin,comment,ntime,orderid});
						if(ret==1){
							result = "{\"result\":\"1\",\"errmsg\":\"�������\"}";
						}
					}
				}else {
					result = "{\"result\":\"-2\",\"errmsg\":\"�շ�Ա��Ϣ������\"}";
				}
			}else {
				result = "{\"result\":\"-1\",\"errmsg\":\"�������۹�\"}";
			}
		}
		return  result;
	}
	/**�Ƿ���ں�����Ƿ�������*/
	private int doPreGetBonus(HttpServletRequest request,Long id) {
		Long ntime = System.currentTimeMillis()/1000;
		//�Ƿ���ں�����Ƿ�������
		String words = AjaxUtil.decodeUTF8(RequestUtil.getString(request, "words"));
		Map bMap = daService.getMap("select etime,bnum,exptime,bwords,money,type,uin from order_ticket_tb where id =? ", new Object[]{id});
		Long count= daService.getLong("select count(id) from order_ticket_detail_tb where otid =?", new Object[]{id});
		//Long havecount= daService.getLong("select count(id) from order_ticket_detail_tb where otid =? and uin is not null", new Object[]{id});
		int ret = 1;
		Integer bnum=0;
		Integer money = 0;
		Integer type = 0;//0��ͨͣ��ȯ 1΢�Ŵ�ȯ
		String tname = "ͣ��ȯ";
		String tpic = "ticket";
		String fontColor="e38165";
		String mname="Ԫ";
		if(bMap!=null){
			String bwords = (String)bMap.get("bwords");
			if(!words.equals("")){//����ף����
				daService.update("update order_ticket_tb set bwords=? where id=? ", new Object[]{words,id});
				bwords = words;
			}
			if(bwords!=null&&bwords.length()>14)
				bwords = bwords.substring(0,12)+"...";
			Long exptime = (Long)bMap.get("exptime");
			bnum = (Integer)bMap.get("bnum");
			money = (Integer)bMap.get("money");
			type = (Integer)bMap.get("type");
			if(type==1){
				tname="�ۿ�ȯ";
				tpic="ticket_wx";
				mname="��";
			}else if(type==5){
				tname="�һ�ȯ";
				fontColor="8bd3a3";
				request.setAttribute("fly", "_fly");
			}
			Map userMap = daService.getMap("select wx_imgurl from user_info_tb where id=? ", new Object[]{bMap.get("uin")});
			if(userMap!=null&&userMap.get("wx_imgurl")!=null)
				request.setAttribute("carowenurl",userMap.get("wx_imgurl"));
			request.setAttribute("bwords",bwords);
			request.setAttribute("bnum",bnum);
			if(type==0||type==5)
				request.setAttribute("btotal","(��"+ bMap.get("money")+"Ԫ)");
			if(ntime>exptime+15*24*60*60){//�ѹ���
				request.setAttribute("tpic", tpic);
				request.setAttribute("mname", mname);
				request.setAttribute("tipwords", tname+"�ѹ���");
				ret =-1;
			}else {
				if(count==0){//û���۹��������ʼ�ۺ��
					if(bnum>0&&money>0){
						if(type==3){//��֤���
							String insertSql = " insert into order_ticket_detail_tb (otid,amount) values(?,?)";
							List<Object[]> values = new ArrayList<Object[]>();
							Integer[] _bonus=new Integer[]{1,2,1,1,2,1,1,2,1,1,2,1,1,2,1,1,2,1,2,4,6};
							for(Integer d : _bonus){
								Object[] objects = new Object[]{id,d};
								values.add(objects);
							}
							int _ret = daService.bathInsert(insertSql, values, new int[]{4,4});
							logger.error("������֤�����д����....."+_ret);
						}else {
							String insertSql = " insert into order_ticket_detail_tb (otid,amount) values(?,?)";
							List<Object[]> values = new ArrayList<Object[]>();
							List<Integer> _bonus=StringUtils.getBonusIngteger(money, bnum,3);
							if(type==4)
								_bonus=StringUtils.getBonusIngteger(money, bnum,12);
							else if(type==2&&money==18)
								_bonus=StringUtils.getBonusIngteger(money, bnum,4);
							for(Integer d : _bonus){
								Object[] objects = new Object[]{id,d};
								values.add(objects);
							}
							int _ret = daService.bathInsert(insertSql, values, new int[]{4,4});
							logger.error("���������д����....."+_ret);
						}
					}
				}
			}
		}else {//������
			request.setAttribute("tpic", tpic);
			request.setAttribute("mname", mname);
			request.setAttribute("tipwords", tname+"�ѹ���");
			ret =-3;
		}
		
		/*if(havecount.intValue()==bnum){
			request.setAttribute("tipwords", tname+"������");
			caiBonusList(request, null, id);
			ret =-4;
		}*/
		request.setAttribute("btype", type);
		request.setAttribute("tname", tname);
		request.setAttribute("fontColor", fontColor);
		return ret;
	}
	
	private String caiBonusRet(HttpServletRequest request,Long bid,Long uin,String mobile,String openid,String acc_token) {
		Long ntime = System.currentTimeMillis()/1000;
		Map bMap = daService.getMap("select * from order_ticket_tb where id = ? ", new Object[]{bid});
		int bums =0;
		Long havecount= daService.getLong("select count(id) from order_ticket_detail_tb where otid =? and uin is not null", new Object[]{bid});
		Integer type = 0;//0��ͨͣ��ȯ 1΢�Ŵ�ȯ
		Long comid = -1L;
		String tname = "ͣ��ȯ";
		String tpic = "ticket";
		String mname="Ԫ";
		Integer btype =0;
		String fontColor="e38165";
		Long otuin = null;//�����������
		if(bMap!=null){
			Map userMap = daService.getMap("select wx_imgurl,comid from user_info_tb where id=? ", new Object[]{bMap.get("uin")});
			bums = (Integer)bMap.get("bnum");
			type = (Integer)bMap.get("type");
			otuin = (Long)bMap.get("uin");
			btype = type;
			if(type==1){
				type=2;
				tname="�ۿ�ȯ";
				tpic="ticket_wx";
				mname="��";
			}else if(type == 2){//ר��ȯ
				type = 1;
				tname = "ר��ȯ";
				tpic = "ticket_limit";
				if(userMap.get("comid") != null && ((Long)userMap.get("comid")) > 0){
					comid = (Long)userMap.get("comid");
					Map<String, Object> comMap = daService.getMap(
							"select company_name from com_info_tb where id=? ",
							new Object[] { comid });
					if(comMap == null){
						request.setAttribute("mname", mname);
						request.setAttribute("tipwords", tname+"�ѹ���");
						request.setAttribute("isover", 0);
						return "caibouns";
					}else{
						request.setAttribute("cname", comMap.get("company_name"));
					}
				}else{
					request.setAttribute("mname", mname);
					request.setAttribute("tipwords", tname+"�ѹ���");
					request.setAttribute("isover", 0);
					return "caibouns";
				}
				logger.error("��ͣ����ר��ȯ,uin"+uin+",�շ�Ա��ţ�"+bMap.get("uin"));
			}else if(type==3){//��֤���,д����ͨ�������
				type=0;
				tname="��֤���";
			}else if(type==4){
				type=0;
				tname="��ֵ���";
			}else if(type==5){
				type=0;
				tname="�һ�ȯ";
				fontColor="8bd3a3";
				request.setAttribute("fly", "_fly");
			}
			String bwords = (String)bMap.get("bwords");
			if(bwords!=null&&bwords.length()>14)
				bwords = bwords.substring(0,12)+"...";
			request.setAttribute("bwords",bwords);
			
			if(userMap!=null&&userMap.get("wx_imgurl")!=null)
				request.setAttribute("carowenurl",userMap.get("wx_imgurl"));
			request.setAttribute("bnum",bMap.get("bnum"));
			if(type==0||type==5)
				request.setAttribute("btotal","(��"+ bMap.get("money")+"Ԫ)");
		}else {
			//û�к����
			request.setAttribute("mname", mname);
			request.setAttribute("tipwords", tname+"�ѹ���");
			request.setAttribute("isover", 0);
			return "caibouns";
		}
		request.setAttribute("mname", mname);
		request.setAttribute("tpic", tpic);
		request.setAttribute("tname", tname);
		request.setAttribute("btype", btype);
		request.setAttribute("fontColor", fontColor);
		if(havecount==bums){
			request.setAttribute("tipwords", tname+"������");
			request.setAttribute("isover", 0);
			request.setAttribute("tpic", tpic+"_l");
			getBonusList(request, bid, uin);
			return "caibouns";
		}
		
		Integer utype = 0;//���û�
		if(uin==null){//���û���ע��
			String bsql = "insert into bonus_record_tb (bid,ctime,mobile,state,amount) values(?,?,?,?,?) ";
			logger.error(">>>>>>>>��ͣ��ȯ�����û����������,��ע��...");
			uin = publicMethods.regUser(mobile, 1000L,-1L,false);//ע�Ტ��Ԫͣ��ȯ
			Object [] values = new Object[]{bid,ntime,mobile,0,10};//�Ǽ�Ϊδ��ȡ�������¼ʱд��ͣ��ȯ���ж��Ƿ��Ǻ�������
			logger.error(">>>>>>>>��ͣ��ȯ�����û�������10Ԫȯ��"+daService.update(bsql,values));	
			utype=1;//���û�
			setWxUser(openid, acc_token, mobile, uin,bid);
		}
		
		if(uin==null||uin==-1){
			//û�к����
			request.setAttribute("tipwords", tname+"������");
			request.setAttribute("isover", 0);
			return "caibouns";
		}
		boolean isbig=false;//�Ƿ�ȡ����
		//ȡͣ��ȯ������ܺ������С��10���Ķ������û������û��Ӵ��쵽С�����û����
		if(utype==0){//���û�
			Long counts = daService.getLong("select count(id) from order_ticket_detail_tb where uin=? ", new Object[]{uin});
			if(counts<10)
				isbig=true;
		}else {
			isbig=true;
		}
		Map otdtMap = null;
		List<Map> otdList =null;
		if(btype==3){//��֤���˳����ȡ
			otdList=daService.getAll("select id,amount from order_ticket_detail_tb where  uin is null and otid=? order by id", new Object[]{bid});
		}else if(btype==4){//��ֵ���
			otdList=daService.getAll("select id,amount,uin from order_ticket_detail_tb where otid=? order by id", new Object[]{bid});
		}else {
			otdList=daService.getAll("select id,amount from order_ticket_detail_tb where  uin is null and otid=? order by amount desc", new Object[]{bid});
		}
		Long otdid = null;
		if(otdList!=null&&!otdList.isEmpty()){
			if(btype==4){//��ֵ����������������Ҫ��ȡ�ڶ�����
				Integer notget=0;//δ��ȡ�����
				List<Map> timeList = new ArrayList<Map>();
				for(Map oMap : otdList){
					if(oMap.get("uin")==null){
						timeList.add(oMap);
						notget ++;
					}
				}
				Map map = otdList.get(otdList.size()-2);//�ڶ�����
				Long ouin = (Long)map.get("uin");
				if(ouin==null){//�������������û�������
					if(otuin.equals(uin)){//������������������
						otdtMap =map;
					}else {//ȥ���ڶ���ĺ��
						if(notget>1)//�����������Ϻ��û����ȡʱ
							otdList.remove(map);
					}
				}
				if(otdtMap==null&&timeList.size()>0){
					if(isbig){//���û��Ӵ��쵽С����֤���˳����ȡ
						otdtMap = timeList.get(timeList.size()-1);
					}else {//���û����
						int rand = new Random().nextInt(timeList.size());
						otdtMap = timeList.get(rand);
					}
				}
				
			}else if(isbig||btype==3){//���û��Ӵ��쵽С����֤���˳����ȡ
				otdtMap = otdList.get(0);
			}else {//���û����
				int rand = new Random().nextInt(otdList.size());
				otdtMap = otdList.get(rand);
			}
		}
		Integer money = 0;//������
		if(otdtMap!=null){
			otdid = (Long)otdtMap.get("id");
			money = (Integer)otdtMap.get("amount");
		}else {
			//û�к����
			request.setAttribute("tipwords", tname+"������");
			request.setAttribute("isover", 0);
			return "caibouns";
		}
		request.setAttribute("mobile", mobile);
		request.setAttribute("money", StringUtils.formatDouble(money));
				
		
		//��ȯ������1�ɹ���-1�ѱ�����
		int ret = daService.update("update order_ticket_detail_tb set uin = ?,ttime=? ,type=?  where id = ? and uin is null", 
				new Object[]{uin,ntime,utype,otdid});
		logger.error(">>>>>>>>��ͣ��ȯ��������"+mobile+",uin:"+uin+",��"+money+",��� "+ret);
		
		if(ret == 1){//�����ȡ�ɹ�,д��ͣ��ȯ��
			Long ttime = TimeTools.getToDayBeginTime();
			Long etime = ttime+16*24*60*60-1;
			if(btype==5)//��һ�ȯ������Ч��
				etime = ttime + 3*24*60*60-1;
			else if(btype!=2){//������ר��ȯ�ⶼ��������Ч��
				etime = ttime + 4*24*60*60-1;
			}
			Long ticketId = daService.getkey("seq_ticket_tb");
			String tsql = "insert into ticket_tb (id,create_time,limit_day,money,state,uin,type,comid) values(?,?,?,?,?,?,?,?) ";
			int _ret = daService.update(tsql, new Object[]{ticketId,ttime,etime,money,0,uin,type,comid});
			
			if(_ret==1){//ͣ��ȯ���д���������
				daService.update("update order_ticket_detail_tb set ticketid=? where id =?", new Object[]{ticketId,otdid});
			}
			logger.error(">>>>��ͣ��ȯ��������"+mobile+",ticketid:"+ticketId+",uin:"+uin+",��"+money+",дȯ���û���"+_ret);
			
			//���º�������ȡʱ�����ȡ����ʱ�� 
			Long btime = (Long)bMap.get("btime");
			int result = 0;
			if(btime==null){
				result = daService.update("update order_ticket_tb set btime=? where id=?  ", new Object[]{ntime,bid});
				logger.error(">>>��ʼ��ȡ���...."+result);
			}else {
				Long count = daService.getLong("select count(id) from order_ticket_detail_tb where otid=? and uin is not null", new Object[]{bid});
				if(count==bums){
					result = daService.update("update order_ticket_tb set etime = ? where id =? ", new Object[]{ntime,bid});
					logger.error(">>>��������...."+result);
				}
			}
		}
		
		//���������ȡ�ĺ��
		getBonusList(request, bid, uin);
		return "bounsret";
	}
	public int setWxUser(String openid,String acc_token,String mobile,Long uin,Long otid){
		//д��΢������ͷ���ַ
		Map userMap = daService.getMap("select wxp_openid,wx_name,wx_imgurl from user_info_Tb where id =? ", new Object[]{uin});
		String userOpenId = (String)userMap.get("wxp_openid");
		if(userOpenId!=null&&userOpenId.length()>2){
			if(!userOpenId.equals(openid)){
				return -1;
			}
		}
		if(userMap!=null){
			String wxname = (String)userMap.get("wx_name");
			if(wxname!=null)
				wxname=wxname.replace("'", "").replace("\"", "");
			
			String wxurl =(String)userMap.get("wx_imgurl");
			if(wxname==null||wxurl==null ||wxurl.length()<2 ){
				String url = "https://api.weixin.qq.com/sns/userinfo?access_token="+acc_token+"&openid="+openid+"&lang=zh_CN";
				String result = CommonUtil.httpsRequest(url, "GET", null);
				JSONObject retmap =null;
				if(result!=null){
					retmap = JSONObject.fromObject(result);
				}
				logger.error(">>>>>>>>return wxuserinfo map :"+retmap);
				if(retmap != null && retmap.get("nickname") != null){
					wxname = retmap.getString("nickname");
					if(wxname!=null)
						wxname = wxname.replace("'", "").replace("\"", "");
					wxurl = retmap.getString("headimgurl");
					if(wxname!=null){
						//���浽���ݿ�
						int rets = daService.update("update user_info_tb set wx_imgurl=? ,wx_name=? where id = ? ", new Object[]{wxurl,wxname,uin});
						if(rets==1)
							ZldMap.removeUser(uin);
						logger.error(">>>uin save wxname("+wxname+") and wxurl("+wxurl+"):"+rets);
					}
				}
			}
		}
		//����openid
		if(openid!=null&&!"".equals(openid)){
			logger.error(">>����openid:mobile="+mobile+",ret="+daService.update("update user_info_Tb set wxp_openid=? where id =? ", new Object[]{openid,uin}));
			publicMethods.sharkbinduser(openid, uin, 0L);
		}
		
		return 1;
	}
	
	/**
	 * �����б� 
	 * @param request
	 * @param uid
	 * @param bid
	 * @return
	 */
	private String caiBonusList(HttpServletRequest request,Long uin,Long bid) {
		Map bMap = daService.getMap("select bwords,bnum,uin,money,type from order_ticket_tb where id = ? ", new Object[]{bid});
		Integer type = 0;//0��ͨͣ��ȯ 1΢�Ŵ�ȯ
		String tpic = "haveget";
		String fontColor="e38165";
		if(uin==null)
			tpic ="ticket";
		String mname="Ԫ";
		String tname="ͣ��ȯ";
				
		if(bMap!=null){
			type = (Integer)bMap.get("type");
			if(type==1){
				type=2;
				tpic="haveget_wx";
				if(uin==null)
					tpic ="ticket_wx";
				mname="��";
				tname="�ۿ�ȯ";
			}else if(type == 2){
				type = 1;
				tpic="haveget_limit";
				if(uin==null)
					tpic ="ticket_limit";
				tname="ר��ȯ";
			}else if(type==5){
				request.setAttribute("fly", "_fly");
				fontColor="8bd3a3";
				tname="�һ�ȯ";
			}
			String bwords = (String)bMap.get("bwords");
			if(bwords!=null&&bwords.length()>14)
				bwords = bwords.substring(0,12)+"...";
			request.setAttribute("bwords",bwords);
			request.setAttribute("bnum",bMap.get("bnum"));
			Map userMap = daService.getMap("select wx_imgurl from user_info_tb where id=? ", new Object[]{bMap.get("uin")});
			if(userMap!=null&&userMap.get("wx_imgurl")!=null)
				request.setAttribute("carowenurl",userMap.get("wx_imgurl"));
			if(type==0||type==5)
				request.setAttribute("btotal","(��"+ bMap.get("money")+"Ԫ)");
		}
		request.setAttribute("tname", tname);
		request.setAttribute("mname", mname);
		request.setAttribute("tpic", tpic);
		request.setAttribute("btype", type);
		request.setAttribute("fontColor", fontColor);
		getBonusList(request, bid, uin);
		return "bounslist";
	}

	/**�������*/
	private void getBonusList(HttpServletRequest request,Long bid,Long uin){
		//���������ȡ�ĺ��
		List<Map<String, Object>> blList = daService.getAll("select o.id,amount,ttime,wx_name,wx_imgurl,wxp_openid,o.uin,o.ticketid,u.mobile from order_ticket_detail_tb o left join user_info_tb u on o.uin=u.id where otid=? and ttime is not null ", new Object[]{bid});
		String data = "[]";
		if(blList!=null&&!blList.isEmpty()){
			data = "[";
			for(Map<String, Object> map : blList){
				Long time = (Long)map.get("ttime");
				String wxname = (String)map.get("wx_name");
				if(wxname!=null)
					wxname=wxname.replace("'", "").replace("\"", "");
				String wxurl = (String)map.get("wx_imgurl");
				if(wxname==null){
					wxname = map.get("mobile")+"";
					if(wxname.length()>10){
						wxname = wxname.substring(0,3)+"****"+wxname.substring(7);
					}else {
						wxname="����...";
					}
				}
				if(wxurl==null||wxurl.length()<2){
					wxurl ="images/bunusimg/defaulthead.png";
				}
				Long ouin = (Long)map.get("uin");
				if(uin!=null&&ouin.equals(uin)){//�鳵
					request.setAttribute("tid", map.get("ticketid"));
					request.setAttribute("uin",uin);
				}
				data +="{\"amount\":\""+StringUtils.formatDouble(map.get("amount"))+"\"," +
						"\"ttime\":\""+TimeTools.getTime_yyMMdd_HHmm(time*1000).substring(3)+"\"," +
						"\"wxname\":\""+wxname+"\",\"wxurl\":\""+wxurl+"\"},";
			}
			data = data.substring(0,data.length()-1);
			data +="]";
			request.setAttribute("haveget", blList.size());
		}else {
			request.setAttribute("haveget", 0);
		}
		request.setAttribute("havegetpic", "haveget");
		request.setAttribute("data", data);
	}
	
	private Map<String, Object> getProfile(Long uin){
		Map<String, Object> infoMap = new HashMap<String, Object>();
		Map profileMap = daService.getPojo("select * from user_profile_tb where uin=?",new Object[]{uin});
		if(profileMap!=null){
			infoMap.put("low_recharge", profileMap.get("low_recharge"));
			infoMap.put("voice_warn", profileMap.get("voice_warn"));
			infoMap.put("auto_cash", profileMap.get("auto_cash"));
			infoMap.put("enter_warn",  profileMap.get("enter_warn"));
		}else {
//			infoMap.put("info", "-1");
//			infoMap.put("message", "û������");
		}
		return infoMap;
	}
	
	/**
	 * �ҵİ��²�Ʒ
	 * @param request
	 * @param uin
	 * @return
	 * http://127.0.0.1/zld/carowner.do?action=products&mobile=15375242041 ==
	 *//* 
	private List<Map<String, Object >> products(HttpServletRequest request,Long uin){
		Long ntime = System.currentTimeMillis()/1000;
		List<Map<String, Object >> infoList = new ArrayList<Map<String,Object>>();
		Long curtime = ntime;
		List<Map> pList = daService.getAll("select p.id prodid,p.p_name,p.b_time pb ,p.e_time pe " +
				//",p.remain_number" +
				",p.price,p.bmin,p.emin,c.id cid,c.b_time cb,c.e_time ce,c.create_time ,t.company_name " +
				"from carower_product c ,product_package_tb p ,com_info_tb t where p.comid=t.id and c.pid=p.id and uin=?", 
				new Object[]{uin});
		if(pList!=null&&pList.size()>0){
			for(Map map : pList){
				Map<String,Object> infoMap = new HashMap<String, Object>();
				Long cbtime = (Long)map.get("cb");
				Long cetime = (Long)map.get("ce");
				Integer pbtime = (Integer)map.get("pb");
				Integer petime = (Integer)map.get("pe");
				Integer bmin = (Integer)map.get("bmin");
				Integer emin = (Integer)map.get("emin");
				String limtime = pbtime+":";
				if(bmin<10)
					limtime +="0";
				limtime +=bmin+" - "+petime+":";
				if(emin<10)
					limtime+="0";
				limtime +=emin;
				Long limitDay = cetime-curtime;
				if(limitDay<0)
					limitDay =0L;
				infoMap.put("name", map.get("company_name"));
				infoMap.put("parkname", map.get("p_name"));
				infoMap.put("price", map.get("price"));
				infoMap.put("limitdate",TimeTools.getTimeStr_yyyy_MM_dd(cbtime*1000)
						+" ��  "+TimeTools.getTimeStr_yyyy_MM_dd(cetime*1000));//��Ч����
				infoMap.put("limittime",limtime);//ʱЧʱ��
				//infoMap.put("number", map.get("remain_number"));//ʣ������
				infoMap.put("resume", map.get("resume"));//����
				infoMap.put("limitday", limitDay/(24*60*60));//ʣ������
				infoMap.put("prodid", map.get("prodid"));//���²�Ʒ���
				int state = 0;//�¿�״̬ 0��δ��ʼ 1:ʹ���� 2�ѹ���
				if(cbtime <= curtime){
					if(cetime > curtime){
						state = 1;//����ʹ����
					}else{
						state = 2;//�ѹ���
					}
				}
				infoMap.put("state", state);
				infoList.add(infoMap);
			}
		}
		return infoList;
	}*/
	
	/**
	 * �ҵİ���
	 * @param request
	 * @param uin
	 * @return
	 */
	private List<Map<String, Object >> getproducts(HttpServletRequest request,Long uin){
		//uin==>car_number==>prod
		logger.error("getproducts>>>�����ѯ�û��¿���action");
		Long ntime = System.currentTimeMillis()/1000;
		Long curtime = ntime;
		List<Map<String, Object >> infoList = new ArrayList<Map<String,Object>>();
		List<String> allCarnumbers = commonMethods.getAllCarnumbers(uin);
		ArrayList<Long> ulist = new ArrayList<Long>();
		for (String cnumber : allCarnumbers) {
			//���ݳ��ƺŲ���û�id���¿���¼
			/*List<Map> pList = daService.getAll("select c.card_id cardid,c.member_id mid,c.car_number carnumbers,p.id prodid,p.p_name,p.b_time pb ,p.e_time pe " +
					",p.price,p.bmin,p.emin,c.id cid,c.b_time cb,c.e_time ce,c.create_time ,t.company_name " +
					"from carower_product c ,product_package_tb p ,com_info_tb t where p.comid=t.id and c.pid=p.id and car_number like ? and uin = ?", 
					new Object[]{"%"+cnumber+"%",uin});*/
			List<Map> pList = daService.getAll("select c.pid,c.card_id cardid,c.member_id mid,c.car_number carnumbers,c.total," +
					"c.id cid,c.b_time cb,c.e_time ce,c.create_time ,t.company_name,t.id tid " +
					"from carower_product c ,com_info_tb t where c.com_id=t.id and car_number like ? and uin = ?", 
					new Object[]{"%"+cnumber+"%",uin});
			if(pList!=null&&pList.size()>0){
				for(Map map : pList){
					Map<String,Object> infoMap = new HashMap<String, Object>();
					Long cid = (Long)map.get("cid");
					Long comid = (Long)map.get("tid");
					if(ulist.contains(cid)){
						break;
					}
					ulist.add(cid);
					String cardid = (String) map.get("cardid");
					String mid = (String) map.get("mid");
					String carnumbers = (String) map.get("carnumbers");
					String[] carnums = carnumbers.split("\\|");
					String carnumber = "";
					for (String carnum : carnums) {
						carnumber += carnum+",";
					}
					carnumber = carnumber.substring(0, carnumber.length()-1);
					Long cbtime = (Long)map.get("cb");
					Long cetime = (Long)map.get("ce");
					Long pid = (Long) map.get("pid");
					logger.error("pid:"+pid);
					if(pid!=null&&pid.intValue()!=-1){
						Map pmap = daService.getMap("select p_name,b_time,e_time,bmin,emin,price,card_id from product_package_tb where id = ?", new Object[]{pid});
						//Integer pbtime = (Integer)pmap.get("b_time");
						//Integer petime = (Integer)pmap.get("e_time");
						//Integer bmin = (Integer)pmap.get("bmin");
						//Integer emin = (Integer)pmap.get("emin");
						//String limtime = pbtime+":";
						//if(bmin<10)
						//	limtime +="0";
						//limtime +=bmin+" - "+petime+":";
						//if(emin<10)
						//	limtime+="0";
						//limtime +=emin;
						//infoMap.put("limittime",limtime);//ʱЧʱ��
						if(pmap!=null){
							infoMap.put("prodid", pmap.get("card_id"));//���²�Ʒ���
							infoMap.put("parkname", pmap.get("p_name"));//�¿�����
						}else{
							infoMap.put("parkname", "�¿�");
							infoMap.put("prodid", -1);
						}
					}else{
						infoMap.put("parkname", "�¿�");
						infoMap.put("prodid", -1);
					}
					infoMap.put("price", map.get("total"));
					Long limitDay = cetime-curtime;
					if(limitDay<0)
						limitDay =0L;
					infoMap.put("cid", cid);
					infoMap.put("comid", comid);
					infoMap.put("cardid", cardid);//�¿��������ر��
					infoMap.put("mid", mid);//�û����ر��
					infoMap.put("carnumber", carnumber);//���¿���Ӧ���ƺ�
					infoMap.put("name", map.get("company_name"));
					infoMap.put("limitdate",TimeTools.getTimeStr_yyyy_MM_dd(cbtime*1000)
							+" ��  "+TimeTools.getTimeStr_yyyy_MM_dd(cetime*1000));//��Ч����
					//infoMap.put("number", map.get("remain_number"));//ʣ������
					infoMap.put("resume", map.get("resume"));//����
					infoMap.put("limitday", limitDay/(24*60*60));//ʣ������
					int state = 0;//�¿�״̬ 0��δ��ʼ 1:ʹ���� 2�ѹ���
					if(cbtime <= curtime){
						if(cetime > curtime){
							state = 1;//����ʹ����
						}else{
							state = 2;//�ѹ���
						}
					}
					infoMap.put("state", state);
					infoMap.put("isthirdpay", PayConfigDefind.getValue("IS_TO_THIRD_WXPAY"));
					infoList.add(infoMap);
				}
			}
		}
		
		return infoList;
	}
	
	/**
	 * �ҵİ��²�Ʒ
	 * @param request
	 * @param uin
	 * @return
	 * http://127.0.0.1/zld/carowner.do?action=products&mobile=15375242041 ==
	 */ 
	private List<Map<String, Object >> products(HttpServletRequest request,Long uin){
		Long ntime = System.currentTimeMillis()/1000;
		List<Map<String, Object >> infoList = new ArrayList<Map<String,Object>>();
		Long curtime = ntime;
		List<Map> pList = daService.getAll("select c.card_id cardid,c.member_id mid,c.car_number carnumbers,p.id prodid,p.p_name,p.b_time pb ,p.e_time pe " +
				//",p.remain_number" +
				",p.price,p.bmin,p.emin,c.id cid,c.b_time cb,c.e_time ce,c.create_time ,t.company_name " +
				"from carower_product c ,product_package_tb p ,com_info_tb t where p.comid=t.id and c.pid=p.id and uin=?", 
				new Object[]{uin});
		if(pList!=null&&pList.size()>0){
			for(Map map : pList){
				Map<String,Object> infoMap = new HashMap<String, Object>();
				String cardid = (String) map.get("cardid");
				String mid = (String) map.get("mid");
				String carnumbers = (String) map.get("carnumbers");
				String[] carnums = carnumbers.split("\\|");
				String carnumber = "";
				for (String carnum : carnums) {
					carnumber += carnum+",";
				}
				carnumber = carnumber.substring(0, carnumber.length()-1);
				Long cbtime = (Long)map.get("cb");
				Long cetime = (Long)map.get("ce");
				Integer pbtime = (Integer)map.get("pb");
				Integer petime = (Integer)map.get("pe");
				Integer bmin = (Integer)map.get("bmin");
				Integer emin = (Integer)map.get("emin");
				String limtime = pbtime+":";
				if(bmin<10)
					limtime +="0";
				limtime +=bmin+" - "+petime+":";
				if(emin<10)
					limtime+="0";
				limtime +=emin;
				Long limitDay = cetime-curtime;
				if(limitDay<0)
					limitDay =0L;
				infoMap.put("cardid", cardid);//�¿��������ر��
				infoMap.put("mid", mid);//�û����ر��
				infoMap.put("carnumber", carnumber);//���¿���Ӧ���ƺ�
				infoMap.put("name", map.get("company_name"));
				infoMap.put("parkname", map.get("p_name"));
				infoMap.put("price", map.get("price"));
				infoMap.put("limitdate",TimeTools.getTimeStr_yyyy_MM_dd(cbtime*1000)
						+" ��  "+TimeTools.getTimeStr_yyyy_MM_dd(cetime*1000));//��Ч����
				infoMap.put("limittime",limtime);//ʱЧʱ��
				//infoMap.put("number", map.get("remain_number"));//ʣ������
				infoMap.put("resume", map.get("resume"));//����
				infoMap.put("limitday", limitDay/(24*60*60));//ʣ������
				infoMap.put("prodid", map.get("prodid"));//���²�Ʒ���
				int state = 0;//�¿�״̬ 0��δ��ʼ 1:ʹ���� 2�ѹ���
				if(cbtime <= curtime){
					if(cetime > curtime){
						state = 1;//����ʹ����
					}else{
						state = 2;//�ѹ���
					}
				}
				infoMap.put("state", state);
				infoList.add(infoMap);
			}
		}
		return infoList;
	}
	
	
	/**
	 * ��ǰ����������ͣ���Ķ�����û�л�ֻ��һ��
	 * @param request
	 * @param uin
	 * @return
	 * http://127.0.0.1/zld/carowner.do?action=orderdetail&orderid=786119&mobile=15801482643
	 */
	private Map<String, Object> orderDetail(HttpServletRequest request,Long uin){
		Map<String, Object> infoMap = new HashMap<String, Object>();
		Long orderId=RequestUtil.getLong(request, "orderid", -1L);
		boolean isBolinkOrder = false;
		//�������Ķ�������
		Map orderMap = daService.getPojo("select o.create_time,o.end_time,o.id,o.comid," +
				"c.company_name,c.address,o.total,o.state,o.uid,o.c_type " +
				"from order_tb o,com_info_tb c where o.comid=c.id and  o.id=? and uin=? ",
				new Object[]{orderId,uin});
		if(orderMap==null||orderMap.isEmpty()){//��ƽ̨û�д˶�������ѯ����ƽ̨����
			String carNumber = publicMethods.getCarNumber(uin);
			orderMap = daService.getPojo("select start_time create_time,end_time,id," +
					"park_name company_name,union_name address,money total,state " +
					"from bolink_order_tb where  id=?  ",
					new Object[]{orderId});
			if(orderMap!=null&&!orderMap.isEmpty()){
				isBolinkOrder = true;
				orderMap.put("c_type", 3);
				orderMap.put("comid", -1);
			}
		}
		if(orderMap!=null){
			Long btime = (Long)orderMap.get("create_time");
			Long end = (Long)orderMap.get("end_time");
			//Long comId = (Long)orderMap.get("comid");
			infoMap.put("total", StringUtils.formatDouble(orderMap.get("total")));
			//infoMap.put("duration", "ͣ��"+(end-btime)/(60*60)+"ʱ"+((end-btime)/60)%60+"��");
			infoMap.put("btime",btime);// TimeTools.getTime_yyyyMMdd_HHmm(btime*1000).substring(10));
			infoMap.put("etime",end);// TimeTools.getTime_yyyyMMdd_HHmm(end*1000).substring(10));
			infoMap.put("ctype", orderMap.get("c_type"));
			infoMap.put("parkname", orderMap.get("company_name"));
			infoMap.put("address", orderMap.get("address"));
			infoMap.put("orderid", orderMap.get("id"));
			infoMap.put("state", orderMap.get("state"));
			infoMap.put("parkid",  orderMap.get("comid"));
			if(!isBolinkOrder){
				Long uid= (Long)orderMap.get("uid");
				if(uid!=null){
					Map userMap = daService.getMap("select mobile,nickname from user_info_Tb where id =? ",new Object[]{uid});
					if(userMap!=null){
						infoMap.put("payee","{\"id\":\""+uid+"\",\"name\":\""+userMap.get("nickname")+"\",\"mobile\":\""+userMap.get("mobile")+"\"}");
					}
				}
				Map bMap= daService.getMap("select id from order_ticket_tb where order_id=? and type=? and etime is null", new Object[]{orderMap.get("id"),0});
				if(bMap!=null&&bMap.get("id")!=null){
					infoMap.put("bonusid", bMap.get("id"));
				}
				infoMap.put("reward","0");
				infoMap.put("comment","0");
				Long count = daService.getLong("select count(id) from parkuser_reward_tb where uin=? and order_id=? ", new Object[]{uin,orderId});
				if(count>0){
					infoMap.put("reward","1");
				}else{
					count = daService.getLong("select count(*) from parkuser_reward_tb where uin=? and ctime>=? and uid=? ",
							new Object[] { uin, TimeTools.getToDayBeginTime(), uid });
					if(count > 0){
						infoMap.put("reward","1");
					}
				}
				count = daService.getLong("select count(ID) from parkuser_comment_tb where uin=? and order_id=? ", new Object[]{uin,orderId});
				if(count>0){
					infoMap.put("comment","1");
				}
			}else{
				infoMap.put("reward","1");
				infoMap.put("comment","1");
				//infoMap.put("bonusid",0);
				infoMap.put("uid","10000");
				infoMap.put("payee","{\"id\":\"10000\",\"name\":\"�������շ�Ա\",\"mobile\":\"\"}");
			}
			logger.error(infoMap);
			//infoMap.put("begintime", TimeTools.getTime_yyyyMMdd_HHmm(btime*1000));
			//infoMap.put("endtime", TimeTools.getTime_yyyyMMdd_HHmm(end*1000));
		}else {
//			infoMap.put("info", "-1");
//			infoMap.put("message", "û�ж���");
		}
		return infoMap;
	}
	
	/**
	 * ��ǰ����������ͣ���Ķ�����û�л�ֻ��һ��
	 * @param request
	 * @param uin
	 * @return
	 */
	private Map<String, Object> currOrder(HttpServletRequest request,Long uin){
		Long ntime = System.currentTimeMillis()/1000;
		Map<String, Object> infoMap = new HashMap<String, Object>();
		Map orderMap = daService.getPojo("select o.create_time,o.id,o.comid,o.car_type,c.company_name,c.address,o.state,o.pid from order_tb o,com_info_tb c where o.comid=c.id and o.uin=? and o.state=?",
				new Object[]{uin,0});
		if(orderMap!=null){
			Long btime = (Long)orderMap.get("create_time");
			Long end = ntime;
			Long comId = (Long)orderMap.get("comid");
			Integer pid = (Integer)orderMap.get("pid");
			Integer car_type = (Integer)orderMap.get("car_type");//0��ͨ�ã�1��С����2����
			if(pid>-1){
				infoMap.put("total",publicMethods.getCustomPrice(btime, end, pid));
			}else {
				infoMap.put("total",publicMethods.getPrice(btime, end, comId, car_type));	
			}
			
			//infoMap.put("duration", "��ͣ"+(end-btime)/(60*60)+"ʱ"+((end-btime)/60)%60+"��");
			infoMap.put("btime", btime);//TimeTools.getTime_yyyyMMdd_HHmm(btime*1000).substring(10));
			infoMap.put("etime",end);// TimeTools.getTime_yyyyMMdd_HHmm(end*1000).substring(10));
			infoMap.put("parkname", orderMap.get("company_name"));
			infoMap.put("address", orderMap.get("address"));
			infoMap.put("orderid", orderMap.get("id"));
			infoMap.put("state",orderMap.get("state"));
			infoMap.put("parkid", comId);
			//infoMap.put("begintime", TimeTools.getTimeStr_yyyy_MM_dd(btime*1000));
		}
		return infoMap;
	}
	
	/**
	 * ��ǰ�������½ӿڣ����ȷ������������ѽ���(state=1)��δ���ֻ�֧����(pay_type!=2)�Ķ�����û�з���δ����(state=0)��һ������
	 * @param request
	 * @param uin
	 * @return
	 */
	private Map<String, Object> currentOrder(HttpServletRequest request,Long uin){
		Long ntime = System.currentTimeMillis()/1000;
		Long comid = RequestUtil.getLong(request, "comid", -1L);//20141213������ѯ��ǰ����ʱ���복����ţ�����ɨ��֧������ʱ�ų����������Ķ���
		Long orderId = RequestUtil.getLong(request, "orderid", -1L);//��ɨ��ӿ���ת������ʱ���ж������ 
		Map<String, Object> infoMap = new HashMap<String, Object>();
		//��ѯ
		Map orderMap =null;
		String sql = null;
		Object[] values =null;
		boolean isBolinkOrder=false;
		if(orderId!=-1){
			orderMap = daService.getMap("select create_time,id,comid,state,total,pid,car_type " +
				"from order_tb  where id=?", new Object[]{orderId});
		}else {
			sql = "select create_time,end_time,id,comid,state,total,pid,car_type " +
					"from order_tb where uin=? and state=? and pay_type=? and end_time >? "; //order by o.end_time desc 
//			sql = "select o.create_time,o.end_time,o.id,o.comid,c.company_name,c.address,o.state,o.total,o.pid,o.car_type " +
//					"from order_tb o,com_info_tb c where o.comid=c.id and o.uin=? and o.state=? and pay_type=? " +
//					"and end_time >? "; //order by o.end_time desc 
			values = new Object[]{uin,1,1,ntime-15*60};
			if(comid!=-1){
				sql +=" and comid= ?";
				values = new Object[]{uin,1,1,ntime-15*60,comid};
			}
			orderMap= daService.getMap(sql+" order by end_time desc ",values);
			if(orderMap==null){
				orderMap = daService.getMap("select * " +
						" from bolink_order_tb where uin =? and state=?  order by  id desc ",new Object[]{uin,0});
				if(orderMap!=null&&!orderMap.isEmpty())//�ǲ�������
					isBolinkOrder = true;
			}
		}
		if(!isBolinkOrder){
			boolean isover = true;
			if(orderMap==null||orderMap.isEmpty()){
				sql = "select o.create_time,o.id,o.comid,o.state,o.pid,o.car_type " +
						"from order_tb o  where o.uin=? and o.state=?  ";//order by o.end_time desc
				values = new Object[]{uin,0};
				if(comid!=-1){
					sql +=" and o.comid= ?";
					values = new Object[]{uin,0,comid};
				}
				orderMap = daService.getMap(sql+ " order by o.end_time desc",values);
				isover=false;
			}
			if(orderMap!=null){
				Integer pid = (Integer)orderMap.get("pid");
				Long btime = (Long)orderMap.get("create_time");
				
				Long end = ntime;
				if(orderMap.get("end_time")!=null)
					end = (Long)orderMap.get("end_time");
				Long comId = (Long)orderMap.get("comid");
				
				if(comId!=null&&comId>0){//�鳵�����Ƽ���ַ
					Map cMap = daService.getMap("select company_name,address from com_info_tb where id=? ", new Object[]{comId});
					if(cMap!=null){
						infoMap.put("parkname", cMap.get("company_name"));
						infoMap.put("address", cMap.get("address"));
					}
				}
				
				Integer car_type = (Integer)orderMap.get("car_type");//0��ͨ�ã�1��С����2����
				Double total =StringUtils.formatDouble(orderMap.get("total"));
				Integer state = (Integer)orderMap.get("state");
				if(isover&&state!=null&&state==1)//�ѽ��㣬���ض����еĽ���۸�
					infoMap.put("total",total);
				else {//δ���㣬���㶩���۸�
					if(pid>-1){
						infoMap.put("total",publicMethods.getCustomPrice(btime, end, pid));
					}else {
						infoMap.put("total",publicMethods.getPrice(btime, end, comId, car_type));	
					}
					//infoMap.put("total", publicMethods.getPrice(btime, end, comId));
				}
				infoMap.put("btime", btime);
				infoMap.put("etime",end);
				
				infoMap.put("orderid", orderMap.get("id"));
				infoMap.put("state",orderMap.get("state"));
				infoMap.put("parkid", comId);
				//infoMap.put("begintime", TimeTools.getTimeStr_yyyy_MM_dd(btime*1000));
			}else {
//			infoMap.put("info", "-1");
//			infoMap.put("message", "û�ж���");
			}
		}else {
			//���ò�����ѯ�۸�
			Long updateTime = (Long)orderMap.get("update_time");
			double money =0.0;
			if(updateTime!=null&&ntime-updateTime<60){
				money = StringUtils.formatDouble(orderMap.get("money"));
			}
			if(money==0){//û��15�����ڵĻ���۸����²�ѯ
				Map<String,Object> oMap =  publicMethods.catBolinkOrder((Long)orderMap.get("id"), 
						(String)orderMap.get("order_id"), (String)orderMap.get("plate_number"),null,0,uin);
				if(oMap!=null)
					money =StringUtils.formatDouble(oMap.get("money"));
			}
			infoMap.put("parkname", orderMap.get("union_name")+"-"+orderMap.get("park_name"));
			infoMap.put("address",orderMap.get("park_name"));
			infoMap.put("btime", orderMap.get("start_time"));
			infoMap.put("etime",ntime);
			infoMap.put("total",money);
			infoMap.put("orderid", orderMap.get("id"));
			infoMap.put("state",orderMap.get("state"));
			infoMap.put("parkid",  orderMap.get("id"));
		}
		System.err.println(">>>>>>>>>>>>>>>current order:>"+infoMap);
		return infoMap;
	}
	
	//������£�http://127.0.0.1/zld/carowner.do?action=buyproduct&productid=2&mobile=15375242041&number=&start=&etc_id=
	private int buyProduct(HttpServletRequest request,Long uin){
		Long pid = RequestUtil.getLong(request, "productid", -1L);
		Integer number = RequestUtil.getInteger(request, "number", 0);
		String start = RequestUtil.processParams(request, "start");
		String etcId = RequestUtil.processParams(request, "etc_id");
		int result = 0;
		if(pid!=-1&&number>0&&!start.equals("")){
			Map productMap = daService.getMap("select * from product_package_tb where id=?", new Object[]{Long.valueOf(pid)});
			result=publicMethods.buyProducts(uin, productMap, number, start,etcId,0);
		}
		return result;
	}
	//http://127.0.0.1/zld/carowner.do?action=parkproduct&comid=3&mobile=15801482643
	private List<Map<String, Object>> parkProduct(Long comId,Long uin){
		List<Map<String, Object>> infoMList = new ArrayList<Map<String,Object>>();
		List<Map> productList = daService.getAll("select * from product_package_tb where comid=? and state =?", new Object[]{comId,0});
		List<Map> myProductId = daService.getAll("select pid from carower_product where uin=?  ", new Object[]{uin});
		if(productList!=null&&productList.size()>0){
			for(Map map :productList){
				Map<String, Object> infoMap = new HashMap<String, Object>();
				Long pid = (Long)map.get("id");
				String isBuy = "0";
				if(myProductId!=null&&myProductId.size()>0)
				for(Map map2: myProductId){
					Long _pid = (Long)map2.get("pid");
					if(pid.intValue()==_pid.intValue()){
						isBuy="1";
						break;
					}
				}
				String bmin = map.get("bmin")+"";
				String emin = map.get("emin")+"";
				if(bmin.equals("null")||bmin.equals("0"))
					bmin="00";
				if(emin.equals("null")||emin.equals("0"))
					emin = "00";
				infoMap.put("id", map.get("id"));
				infoMap.put("name", map.get("p_name"));
				infoMap.put("price",map.get("price"));
				infoMap.put("limittime", map.get("b_time")+":"+bmin+"-"+map.get("e_time")+":"+emin);
				infoMap.put("number",map.get("remain_number"));
				infoMap.put("isbuy", isBuy);
				infoMap.put("resume", map.get("resume"));
				infoMList.add(infoMap);
			}
		}
		return infoMList;
	}
	//  http://127.0.0.1/zld/carowner.do?action=parkdetail&comid=3
	private Map<String, Object> parkDetail(HttpServletRequest request,Long comId,Long uin){

		Map<String, Object> infoMap = new HashMap<String, Object>();
		//��λ��/ռ�ó�λ������λ������ͣ������Ϣ����ַ�����ͣ��绰������ 
		/*  currentPrice;// ��ǰ�۸񣬲��÷����� ��
		 *  praiseNum;// "��һ��"����
			disparageNum;// "��һ��"����
			hasPraise;// ��ǰ�û��Ƿ��޹���ͣ������1-->�޹���0-->û�޹�
			//description;// ��������
			//nfc;// �Ƿ�֧��nfc��1--֧�֣�0-->��֧�֣���ͬ
			//etc;// �Ƿ�֧��etc
			//book;// �Ƿ�֧��Ԥ��
			//navi;// �Ƿ�֧�����ڵ���
			//monthlyPay;// �Ƿ�֧���¿�
			//photoUrl;//ͣ������Ƭurl��ַ���ϣ�����ͼƬ
		 */
		Map comMap = daService.getPojo("select * from com_info_tb where id=?", new Object[]{comId});
		//��ͼƬ
		List<Map<String, Object>> picMap = daService.getAll("select picurl from com_picturs_tb where comid=? order by id desc limit ?",
				new Object[]{comId,1});
		String picUrls = "[";
		if(picMap!=null&&!picMap.isEmpty()){
			for(Map<String, Object> map : picMap){
				if(picUrls.equals("["))
					picUrls += "\""+map.get("picurl")+"\"";
				else {
					picUrls += ",\""+map.get("picurl")+"\"";
				}
			}
		}
		picUrls += "]";
		//������
		int praiseNum=0;
		int disparageNum=0;
		int hasPraise=-1;
		if(uin!=null&&uin>0){
			List<Map<String, Object>> praiseMap = daService.getAll("select * from com_praise_tb where comid=?",new Object[]{comId});
			logger.info(praiseMap);
			logger.info(uin);
			if(praiseMap!=null&&!praiseMap.isEmpty()){
				for(Map<String, Object> map :praiseMap){
					Long uid = (Long) map.get("uin");
					Integer praise = (Integer)map.get("praise");
					if(praise==0)
						disparageNum++;
					else
						praiseNum++;
					if(uin!=null&&uin.intValue()==uid.intValue()){
						hasPraise=praise;
					}
				}
			}
		}
		if(comMap!=null){
			Long used = pService.getLong("select count(*) from order_tb where comid=? and state=?", new Object[]{comId,0}) ;
			Integer shareNumbre =  (Integer)comMap.get("share_number");
			Long updateTime = (Long)comMap.get("update_time");
			
			infoMap.put("total", comMap.get("parking_total"));
			//infoMap.put("share", shareNumbre-used);
			infoMap.put("address", comMap.get("address"));
			//infoMap.put("parkname", comMap.get("company_name"));
			infoMap.put("mobile", comMap.get("mobile"));
			infoMap.put("updatetime", TimeTools.getTime_yyyyMMdd_HHmm(updateTime*1000 ));
			infoMap.put("parking_type", comMap.get("parking_type"));//���ͣ�0���棬1���£�2ռ��
			infoMap.put("stop_type", comMap.get("stop_type"));//���ࣺ0ƽ�棬1����
			//infoMap.put("used", used);//��ռ����
			infoMap.put("photoUrl", picUrls);//��ռ����
			infoMap.put("nfc", comMap.get("nfc"));//�Ƿ�֧��nfc��1--֧�֣�0-->��֧�֣���ͬ
			infoMap.put("etc", comMap.get("etc"));//�Ƿ�֧��etc
			infoMap.put("book", comMap.get("book"));//�Ƿ�֧�����ڵ���
			infoMap.put("navi", comMap.get("navi"));//��������
			infoMap.put("epay", comMap.get("epay"));//��������
			infoMap.put("monthlyPay", comMap.get("monthlypay"));//�Ƿ�֧���¿�
			infoMap.put("praiseNum", praiseNum);//"��һ��"����
			infoMap.put("disparageNum", disparageNum);//"��һ��"����
			infoMap.put("hasPraise", hasPraise);//��ǰ�û��Ƿ��޹���ͣ������1-->�޹���0-->û�޹�
			infoMap.put("description", comMap.get("resume")==null?"":comMap.get("resume"));//��������
			infoMap.put("freeSpace", shareNumbre-used);//// ���г�λ��
			infoMap.put("currentPrice",getPrice(comId));// ��ǰ��Сʱ�۸�
		}
		return infoMap;
	}
	private Map<String, Object> detail(String mobile,Map userMap){
		Long ntime = System.currentTimeMillis()/1000;
		Map<String, Object> infoMap = new HashMap<String, Object>();
		//�����ƣ��绰
		if(userMap!=null){
			Long uin  = (Long)userMap.get("id");
			List<Map<String, Object>> carList = daService.getAll("select car_number,is_auth from car_info_tb where uin = ? order by is_auth", new Object[]{uin});
			Integer state  = 0;
			String carNumber = "";
			if(carList!=null&&!carList.isEmpty()){
				List<Integer> sList = new ArrayList<Integer>();
				for(Map<String, Object> car : carList){
					sList.add((Integer)car.get("is_auth"));
					carNumber= (String)car.get("car_number");
				}
				if(sList.size()==1)
					state = sList.get(0);
				else {
					if(sList.contains(-1))
						state=-1;
					else if (sList.contains(2)){
						state=2;
					}else if(sList.contains(0)){
						state=0;
					}else if(sList.contains(1))
						state=1;
				}
			}
			Double balance = Double.valueOf(userMap.get("balance")+"");
			infoMap.put("balance", balance);
			infoMap.put("mobile", mobile);
			infoMap.put("carNumber",carNumber);
			//��������ֶ�,���ö�Ⱥ����״̬
			Integer uIsAuth = (Integer)userMap.get("is_auth");//�����Ƿ�����֤
			if(uIsAuth!=null&&uIsAuth==1){
				infoMap.put("limit_balan", userMap.get("credit_limit"));//�������ö��
				infoMap.put("limit", 30);//���ö��
				infoMap.put("limit_warn",10);//���ö�ȸ澯
			}else {
				infoMap.put("limit_balan", 0);//�������ö��
				infoMap.put("limit", 0);//���ö��
				infoMap.put("limit_warn",0);//���ö��
			}
			infoMap.put("state", state);//�������״̬ 0:δ��֤��1:����֤��2:��֤��
			//�����ѯ
			Long count  =daService.getLong("select count(ID) from order_ticket_tb where uin=? and money>? and  exptime>? and btime is null ",
					new Object[]{userMap.get("id"),0,ntime});
			if(count==0)
				infoMap.put("bonusid", "");//����0����ʾ�к��δ����
			else
				infoMap.put("bonusid", count);//����0����ʾ�к��δ����
			//			Long bid = null;
//			if(bMap!=null&&bMap.get("id")!=null)
//				bid = (Long)bMap.get("id");
//			if(bid!=null&&bid>0){
//				infoMap.put("bonusid", bid);
//			}
		}
		return infoMap;
	}
	
	private String getCarNumber(Long uin){
		Map carInfoMap = daService.getPojo("select car_number from car_info_tb where uin=? ",
				new Object[]{uin});
		if(carInfoMap!=null&&carInfoMap.get("car_number")!=null)
			return (String)carInfoMap.get("car_number");
		return "";
	}
	
	private List<Map<String, Object>> historyOrder(HttpServletRequest request,Long uin){
		//���ڣ�ͣ�������ƣ��ܼ�
		
		Integer pageNum = RequestUtil.getInteger(request, "page", 1);
		Integer pageSize = RequestUtil.getInteger(request, "size", 20);
		List<Object> params = new ArrayList<Object>();
		params.add(1);
		params.add(uin);
		params.add(1);
		//Long time = TimeTools.getToDayBeginTime();
		List<Map<String, Object>> infoMaps = new ArrayList<Map<String,Object>>();
		List<Map<String, Object>> list = daService.getAll("select t.id, t.create_time,t.total,c.company_name from order_tb t,com_info_tb c " +
				"where t.comid=c.id and  t.state=? and t.uin=? and t.pay_type>? order by t.end_time desc",// and create_time>?",
				params, pageNum, pageSize);
		params.clear();
		//String car_number = publicMethods.getCarNumber(uin);
		params.add(uin);
		params.add(1);
		//����ͣ������
		List<Map<String, Object>> carStopList = daService.getAll("select id,money as total,start_time as create_time,park_name as company_name from" +
				" bolink_order_tb where uin=? and state =?  ",params,pageNum,pageSize);
		if(list!=null){
			if(carStopList!=null)
				list.addAll(carStopList);
		}else {
			list = carStopList;
		}
		if(carStopList!=null){//���򣬰�create_time ����
			Collections.sort(list, new OrderSortCompare());
		}
		if(list!=null&&list.size()>0){
			for(Map map : list){
				Map<String, Object> info = new HashMap<String, Object>();
				Long ctime = (Long)map.get("create_time");
				info.put("date", TimeTools.getTimeStr_yyyy_MM_dd(ctime*1000));
				info.put("total", StringUtils.formatDouble(map.get("total")));
				info.put("parkname", map.get("company_name"));
				info.put("orderid", map.get("id"));
				infoMaps.add(info);
			}
		}else {
			Map<String, Object> info = new HashMap<String, Object>();
//			info.put("info", "û�м�¼");
//			infoMaps.add(info);
		}
		System.err.println(infoMaps);
		return infoMaps;
	}

	/**
	 * ȡ��Сʱ�۸�
	 * @param parkId
	 * @return
	 */
	private String getPrice(Long parkId){
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		//��ʼСʱ
		int bhour = calendar.get(Calendar.HOUR_OF_DAY);
		List<Map> priceList=daService.getAll("select * from price_tb where comid=? " +
				"and state=? and pay_type=? order by id desc", new Object[]{parkId,0,0});
		if(priceList==null||priceList.size()==0){//û�а�ʱ�β���
			//�鰴�β���
			priceList=daService.getAll("select * from price_tb where comid=? " +
					"and state=? and pay_type=? order by id desc", new Object[]{parkId,0,1});
			if(priceList==null||priceList.size()==0){//û�а��β��ԣ�������ʾ
				return "0Ԫ/��";
			}else {//�а��β��ԣ�ֱ�ӷ���һ�ε��շ�
				Map timeMap =priceList.get(0);
				Integer unit = (Integer)timeMap.get("unit");
				if(unit!=null&&unit>0){
					if(unit>60){
						String t = "";
						if(unit%60==0)
							t = unit/60+"Сʱ";
						else
							t = unit/60+"Сʱ "+unit%60+"����";
						return timeMap.get("price")+"Ԫ/"+t;
					}else {
						return timeMap.get("price")+"Ԫ/"+unit+"����";
					}
				}else {
					return timeMap.get("price")+"Ԫ/��";
				}
				//return timeMap.get("price")+"Ԫ/��";
			}
			//�����Ÿ�����Ա��ͨ�����úü۸�
		}else {//�Ӱ�ʱ�μ۸�����зּ���ռ��ҹ���շѲ���
			if(priceList.size()>0){
				logger.info(priceList);
				for(Map map : priceList){
					Integer btime = (Integer)map.get("b_time");
					Integer etime = (Integer)map.get("e_time");
					Double price = Double.valueOf(map.get("price")+"");
					Double fprice = Double.valueOf(map.get("fprice")+"");
					Integer ftime = (Integer)map.get("first_times");
					if(ftime!=null&&ftime>0){
						if(fprice>0)
							price = fprice;
					}
					if(btime<etime){//�ռ� 
						if(bhour>=btime&&bhour<etime){
							return price+"Ԫ/"+map.get("unit")+"����";
						}
					}else {
						if(bhour>=btime||bhour<etime){
							return price+"Ԫ/"+map.get("unit")+"����";
						}
					}
				}
			}
		}
		return "0.0Ԫ/Сʱ";
	}
	
	private String getWeek(int week){
		switch (week) {
		case 2:
			return "һ";
		case 3:
			return "��";
		case 4:
			return "��";
		case 5:
			return "��";
		case 6:
			return "��";
		case 7:
			return "��";
		case 1:
			return "��";
		}
		return "";
	}
	

	/**
	 * ����΢�Žӿڣ�ȡ�û���openid
	 * @param request
	 * @return [opedid,access_token]
	 */
	private String[] getOpenid(HttpServletRequest request){
		String code = RequestUtil.processParams(request, "code");
		logger.error(">>>>>>>>code:"+code+",comfig appid:");
		if(code==null||"".equals(code))
			return null;
		String appid = Constants.WXPUBLIC_APPID;
		String secret=Constants.WXPUBLIC_SECRET;
		String access_token_url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid="+appid+"&secret="+secret+"&code="+code+"&grant_type=authorization_code";
		logger.error(">>>>>>>>access_token_url:"+access_token_url);
		String result = CommonUtil.httpsRequest(access_token_url, "GET", null);
		JSONObject map =null;
		if(result!=null){
			map = JSONObject.fromObject(result);
		}
		if(map == null || map.get("errcode") != null){
			return null;
		}
		String openid = (String)map.get("openid");
		String accToken = (String)map.get("access_token");
		logger.error(">>>>>>>>return map :"+map);
		return new String[]{openid,accToken};
	}
	/**
	 * ����openid ���û��ֻ�
	 * @param openid
	 * @return
	 */
	private Map getMobileByOpenid(String openid){
		Map<String, Object> userMap= null;
		if(openid!=null&&!openid.equals("")){
			userMap = daService.getMap("select id, mobile,wx_name,wx_imgurl from user_info_tb where state=? and auth_flag=? and wxp_openid=? ",
							new Object[] { 0, 4, openid });
		}
		return userMap;
	}
	
	public Long getUinByMobile(String mobile){
		if(!"".equals(mobile)){
			Map userMap = daService.getPojo("select id from user_info_Tb where mobile=? and auth_flag=? ", new Object[]{mobile,4});
			if(userMap!=null){
				return (Long) userMap.get("id");
			}
		}
		return -1L;
	}
	
	
	public int backTickets(Long uin,Integer num){
		//2015-03-10������������ʱ������д��ͣ��ȯ����¼ʱ�жϺ����������ͣ��ȯ
		String tsql = "insert into ticket_tb (create_time,limit_day,money,state,uin) values(?,?,?,?,?) ";
		List<Object[]> values = new ArrayList<Object[]>();
		Long ntime =TimeTools.getToDayBeginTime();
		for(int i=0;i<num;i++){
			Object[] v1 = new Object[]{ntime,ntime+6*24*60*60-1,1,0,uin};
			Object[] v2 = new Object[]{ntime,ntime+6*24*60*60-1,2,0,uin};
			Object[] v3 = new Object[]{ntime,ntime+6*24*60*60-1,3,0,uin};
			Object[] v4 = new Object[]{ntime,ntime+6*24*60*60-1,4,0,uin};
			values.add(v1);values.add(v2);values.add(v3);values.add(v4);
		}
		int result= daService.bathInsert(tsql, values, new int[]{4,4,4,4,4});
		if(result>0){
			logService.insertUserMesg(5, uin, "��ϲ�����30Ԫͣ��ȯ!", "ͣ��ȯ����");
		}
		return result;
	}
}


