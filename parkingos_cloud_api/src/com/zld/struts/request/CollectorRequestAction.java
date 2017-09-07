package com.zld.struts.request;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.beans.factory.annotation.Autowired;

import pay.Constants;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.zld.AjaxUtil;
import com.zld.CustomDefind;
import com.zld.facade.GenPosOrderFacade;
import com.zld.facade.PayPosOrderFacade;
import com.zld.facade.StatsAccountFacade;
import com.zld.impl.CommonMethods;
import com.zld.impl.MemcacheUtils;
import com.zld.impl.MongoClientFactory;
import com.zld.impl.MongoDbUtils;
import com.zld.impl.PublicMethods;
import com.zld.pojo.ActCardReq;
import com.zld.pojo.AutoPayPosOrderFacadeReq;
import com.zld.pojo.AutoPayPosOrderResp;
import com.zld.pojo.BaseResp;
import com.zld.pojo.BerthSeg;
import com.zld.pojo.BindCardReq;
import com.zld.pojo.CardChargeReq;
import com.zld.pojo.CardInfoReq;
import com.zld.pojo.CardInfoResp;
import com.zld.pojo.CollectorSetting;
import com.zld.pojo.GenPosOrderFacadeReq;
import com.zld.pojo.GenPosOrderFacadeResp;
import com.zld.pojo.Group;
import com.zld.pojo.ManuPayPosOrderFacadeReq;
import com.zld.pojo.ManuPayPosOrderResp;
import com.zld.pojo.Order;
import com.zld.pojo.ParseJson;
import com.zld.pojo.PayEscapePosOrderFacadeReq;
import com.zld.pojo.PayEscapePosOrderResp;
import com.zld.pojo.StatsAccountClass;
import com.zld.pojo.StatsFacadeResp;
import com.zld.pojo.StatsReq;
import com.zld.pojo.StatsWorkRecordInfo;
import com.zld.pojo.StatsWorkRecordResp;
import com.zld.pojo.UnbindCardReq;
import com.zld.pojo.WorkRecord;
import com.zld.service.CardService;
import com.zld.service.DataBaseService;
import com.zld.service.LogService;
import com.zld.service.PgOnlyReadService;
import com.zld.utils.ExecutorsUtil;
import com.zld.utils.OrderSortCompare;
import com.zld.utils.ParkingMap;
import com.zld.utils.RequestUtil;
import com.zld.utils.SendMessage;
import com.zld.utils.StringUtils;
import com.zld.utils.TimeTools;
import com.zld.utils.ToAlipayQrTradePay;
/**
 * ͣ�����շ�Ա����������λ�����۴����
 * @author Administrator
 *
 */
public class CollectorRequestAction extends Action{
	
	@Autowired
	private DataBaseService daService;
	@Autowired
	private LogService logService;
	@Autowired
	private PublicMethods publicMethods;
	@Autowired
	private PgOnlyReadService pService;
	@Autowired 
	private CommonMethods commonMethods;
	@Autowired 
	private MongoDbUtils mongoDbUtils;
	@Autowired
	private PayPosOrderFacade payOrderFacade;
	@Autowired
	private GenPosOrderFacade genOrderFacade;
	@Autowired
	private CardService cardService;
	@Autowired
	private MemcacheUtils memcacheUtils;
	@Autowired
	private StatsAccountFacade accountFacade;
	@Autowired
	private ToAlipayQrTradePay toAlipayQrTradePay;
	
	private Logger logger = Logger.getLogger(CollectorRequestAction.class);
	
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		response.setContentType("application/json");
		String token =RequestUtil.processParams(request, "token");
		String action =RequestUtil.processParams(request, "action");
		String out= RequestUtil.processParams(request, "out");
		Map<String,Object> infoMap = new HashMap<String, Object>();
		Long comId = null;//�շ�Ա���ڳ���
		Long uin = null;//�շ�Ա�˺�
		Long groupId = null;//�շ�Ա������Ӫ���ű��
		Long authFlag = 0L;
		if(token.equals("")){
			infoMap.put("info", "no token");
		}else {
			if(token.equals("notoken")){
				comId = RequestUtil.getLong(request, "comid",-1L);
				uin = RequestUtil.getLong(request, "uin",-1L);
			}else{
				Map<String, Object> comMap = daService.getPojo("select * from user_session_tb" +
						" where token=?", new Object[]{token});
				if(comMap != null && comMap.get("comid") != null){
					comId = (Long)comMap.get("comid");
					uin = (Long) comMap.get("uin");
					groupId = (Long)comMap.get("groupid");
				}else {
					infoMap.put("info", "token is invalid");
				}
			}
		}
		//logger.error("token="+token+",comid="+comId+",action="+action+",uin="+uin);
		/*tokenΪ�ջ�token��Чʱ�����ش���		 */
		if(token.equals("") || comId == null || uin == null){
			if(out.equals("json"))
				AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));
			else
				AjaxUtil.ajaxOutput(response, StringUtils.createXML(infoMap));
			return null;
		}
		String result ="";
		if(action.equals("myinfo")){
			result= myInfo(uin);
			//test:http://127.0.0.1/zld/collectorrequest.do?action=myinfo&token=0dc591f7ddda2d6fb73cd8c2b4e4a372
		}else if(action.equals("comparks")){
			//http://127.0.0.1/zld/collectorrequest.do?action=comparks&out=josn&token=5f0c0edb1cc891ac9c3fa248a28c14d5
			result =getComParks(comId);
			result = result.replace("null", "");
		}else if(action.equals("autoup")){//�Զ�̧��
			result=  autoUp(request,comId,uin);
			//http://127.0.0.1/zld/collectorrequest.do?action=autoup&price=&carnumber=&token=0dc591f7ddda2d6fb73cd8c2b4e4a372
		}else if(action.equals("toshare")){//����λ
			Integer number = RequestUtil.getInteger(request, "s_number", -1);
			boolean isCanLalaRecord = ParkingMap.isCanRecordLaLa(uin);
			if(number!=-1){
				doShare(comId, uin,number,infoMap,isCanLalaRecord);
			}else {
				infoMap.put("info", "fail");
				infoMap.put("message", "�����������Ϸ�!");
			}
			if(out.equals("json")){
				result= StringUtils.createJson(infoMap);
			}else
				result=StringUtils.createXML(infoMap);
			//test:http://127.0.0.1/zld/collectorrequest.do?action=toshare&token=d450ea04d67bf0b428ea1204675d5b53&s_number=800

		}else if(action.equals("tosale")){//���۴���
			Long orderId = RequestUtil.getLong(request, "orderid", -1L);
			Integer houer = RequestUtil.getInteger(request, "hour", 0);
			if(orderId!=-1&&houer>0){
				doSale(comId, uin,houer, orderId,infoMap);
			}else {
				infoMap.put("info", "����û�ж�����Ż��Ż�Сʱ!");
			}
			if(out.equals("json")){
				result= StringUtils.createJson(infoMap);
			}else
				result=StringUtils.createXML(infoMap);
			//test:http://127.0.0.1/zld/collectorrequest.do?action=tosale&token=d450ea04d67bf0b428ea1204675d5b53&orderid=1&hour=1
		}else if(action.equals("orderdetail")){//��������
			orderDetail(request,comId,uin,infoMap);
			if(out.equals("json")){
				result= StringUtils.createJson(infoMap);
			}else
				result= StringUtils.createXML(infoMap);
			result = result.replace("null", "");
			//http://127.0.0.1:8080/zld/collectorrequest.do?action=orderdetail&token=9e47c76a147e18ae9c60584de673ed9e&orderid=18245855&out=json
		}else if(action.equals("currorders")){//��ǰ����
			result = currOrders(request,uin,comId,out,infoMap);
			System.out.println(result);
			//test:http://127.0.0.1/zld/collectorrequest.do?action=currorders&token=4bad81d8d7993446265a155318182dee&page=1&size=10&out=json
		}else if(action.equals("orderhistory")){//��ʷ����
			result = orderHistory(request, comId, out, groupId);
			//http://127.0.0.1/zld/collectorrequest.do?action=orderhistory&day=last&uid=10828&ptype=0&token=ec5c8185dae6f48c03c43785fe17be22&uin=10824&page=1&size=10&out=json
		}else if(action.equals("ordercash")){//�ֽ��շ�
			result = orderCash(request, out, uin, groupId);
			//test:http://127.0.0.1/zld/collectorrequest.do?action=ordercash&token=5286f078c6d2ecde9b30929f77771149&orderid=787824
		}else if(action.equals("ordercard")){//ˢ���շ�
			result = manuPayPosOrder(request, uin, 1, groupId);
		}else if(action.equals("freeorder")){//HD�棬��ѷ���
			result = freeOrder(request,uin,comId);
			//http://192.168.199.239/zld/collectorrequest.do?action=freeorder&token=7d4860ef99bd70d5c91af535bb2c5065&orderid=1
		}else if(action.equals("cominfo")){//��˾��Ϣ
			comInfo(request,comId,infoMap);
			//System.err.println(infoMap);
			//test:http://127.0.0.1/zld/collectorrequest.do?action=cominfo&token=761afb1ecc204a2d73223c2e96ae6b80&out=json
			if(out.equals("json")){
				result= StringUtils.createJson(infoMap);
			}else
				result=StringUtils.createXML(infoMap);
		}else if(action.equals("corder")){//һ����ѯ
			result = corder(request,comId);
		}
		else if(action.equals("score")){//�������
			result= "[]";
		}else if(action.equals("getparkaccount")){
			Map parkAccountMap  = daService.getMap("select sum(amount) amount from park_account_tb where create_time>? and uid =? and type=? ", 
					new Object[]{TimeTools.getToDayBeginTime(),uin,0});
			Double total = 0d;
			if(parkAccountMap!=null){
				total =StringUtils.formatDouble(parkAccountMap.get("amount"));
			}
			result=""+total;
			//http://127.0.0.1/zld/collectorrequest.do?action=getparkaccount&token=4182e6ad895208c3d4829d447e0c61b7
		}else if(action.equals("getpadetail")){//�˻���ϸ
			result = getpaDetail(request,comId);
			//http://127.0.0.1/zld/collectorrequest.do?action=getpadetail&token=c5ea6e5fd0acdf97a262f7f86c31f3ae
		}else if(action.equals("withdraw")){//������������
			//http://192.168.199.240/zld/collectorrequest.do?action=withdraw&uid=10343&comid=858&money=20
			result = withdraw(request,uin,comId);
		}else if(action.equals("getpaccount")){//��ѯͣ�����˻��ܶ�
			Map comMap = daService.getMap("select money from com_info_tb where id=?", new Object[]{comId});
			Double total = 0d;
			if(comMap!=null){
				total = StringUtils.formatDouble(comMap.get("money"));
			}
			result= total+"";
			//http://127.0.0.1/zld/collectorrequest.do?action=getpaccount&token=17ad4f0a3cbdce40c56595f00d7666bc
		}else if(action.equals("getparkbank")){
			Map comaMap  = daService.getMap("select id,card_number,name,mobile,bank_name,area,bank_pint,user_id from com_account_tb where comid=? and type=? order by id desc ",new Object[]{comId,0});
			result=StringUtils.createJson(comaMap);
			result =  result.replace("null", "");
			//http://127.0.0.1/zld/collectorrequest.do?action=getparkbank&token=17ad4f0a3cbdce40c56595f00d7666bc
		}else if(action.equals("addparkbank")){
			result = addParkBank(request,uin,comId);
		}else if(action.equals("editpbank")){
			result = editpBank(request,uin,comId);
		}else if(action.equals("uploadll")){//upload lat and lon, �շ�Ա�ϴ���γ��
			result = uploadll(request,uin,comId,authFlag);
		}else if(action.equals("reguser")){//�շ�Ա�Ƽ�����
			result = reguser(request,uin,comId);
		}else if(action.equals("regcolmsg")){//�����Ƽ�����
			result = regcolmsg(request,uin,comId,out);
		}else if(action.equals("recominfo")){//�����շ�Ա�Ƽ���¼
			result = recominfo(request,uin);
		}else if(action.equals("getmesg")){
			result = getMesg(request,uin);
		}else if(action.equals("getincome")){//�����շ�Աһ��ʱ�����ֽ���������ֻ�������
			result = getIncome(request,uin);
			//http://127.0.0.1/zld/collectorrequest.do?action=getincome&token=15d1bb15b8dcb99aa7dbe0adc9797162&btime=2012-12-28
		}else if(action.equals("getnewincome")){//�����շ�Աһ��ʱ�����ֽ���������ֻ�������
			result = getNewIncome(request,uin);
			//http://127.0.0.1/zld/collectorrequest.do?action=getincome&token=15d1bb15b8dcb99aa7dbe0adc9797162&btime=2012-12-28
		}else if(action.equals("querycarpics")){//����һ���µĳ��ƻ���
			result = queryCarPics(request,uin,comId);
			//http://192.168.10.239/zld/collectorrequest.do?action=querycarpics&token=
		}else if(action.equals("incomanly")){//����ͳ��
			result = incomAnly(request,uin,comId);
			//http://192.168.199.240/zld/collectorrequest.do?action=incomanly&acctype=1&incom=2&datetype=2&page=1&token=6d5d6a1bd45b5dafd2294e99cf9c91c9
		}else if(action.equals("invalidorders")){
			Long invalid_order = RequestUtil.getLong(request, "invalid_order", 0L);
			int ret = daService.update("update com_info_tb set invalid_order=invalid_order+? where id=?", new Object[]{invalid_order, comId});
			result = result + "";
			//http://192.168.199.239/zld/collectorrequest.do?action=invalidorders&invalid_order=-1&token=198f697eb27de5515e91a70d1f64cec7
		}else if(action.equals("bindworksite")){//�շ�Ա�󶨹���վ
			result = bindWorkSite(request,uin);
			//collectorrequest.do?action=bindworksite&wid=&token=198f697eb27de5515e91a70d1f64cec7
		}else if(action.equals("gooffwork")){//�շ�Ա�°�
			result = goOffWork(request,uin);
		}else if(action.equals("akeycheckaccount")){
			result = akeyCheckAccount(request,uin,comId);
		}else if(action.equals("getparkdetail")){//����Ա�鿴����������ͣ������ϸ
			result = getParkDetail(request,uin,comId,authFlag);
		}else if(action.equals("countprice")){
			Long btime = RequestUtil.getLong(request, "btime", -1L);
			Long etime = RequestUtil.getLong(request, "etime", -1L);
			Map<String,Object> info = new HashMap<String,Object>();
			String ret = publicMethods.getPrice(btime, etime, comId, 0);
			info.put("total", ret);
			result = StringUtils.createJson(info);
		}else if(action.equals("rewardscore")){
			infoMap =rewardscore(request,uin,infoMap);
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));
			result = StringUtils.createJson(infoMap);
			//http://127.0.0.1/zld/collectorrequest.do?action=rewardscore&token=5286f078c6d2ecde9b30929f77771149
		}else if(action.equals("rscorerank")){//�������а�
			result = rscoreRank(request,uin);
			//http://127.0.0.1/zld/collectorrequest.do?action=rscorerank&token=5286f078c6d2ecde9b30929f77771149
		}else if(action.equals("rewardrank")){
			result = rewardRank(request,uin);
			//http://127.0.0.1/zld/collectorrequest.do?action=rewardrank&token=5286f078c6d2ecde9b30929f77771149
		}else if(action.equals("bonusinfo")){
			result = bonusInfo(request,uin);
			//http://127.0.0.1/zld/collectorrequest.do?action=bonusinfo&token=67579fd93b96ad32ced2584b54d8454f
		}else if(action.equals("sendticket")){
			result = sendTicket(request,uin,comId);
			//http://127.0.0.1/zld/collectorrequest.do?action=sendticket&token=5286f078c6d2ecde9b30929f77771149&bmoney=3&score=1&uins=21616,21577,21554
		}else if(action.equals("sendbonus")){
			result = sendBonus(request,uin,comId);
			//http://127.0.0.1/zld/collectorrequest.do?action=sendbonus&token=5286f078c6d2ecde9b30929f77771149&bmoney=12&bnum=8&score=1
		}else if(action.equals("sendsuccess")){
			result = sendSuccess(request,uin);
		}else if(action.equals("rewardlist")){
			result = rewardList(request,uin);
			//http://127.0.0.1/zld/collectorrequest.do?action=rewardlist&token=116a87809926db5c477a9a1a58488ec1
		}else if(action.equals("parkinglist")){
			result = parkingList(request,uin);
			//http://127.0.0.1/zld/collectorrequest.do?action=parkinglist&token=116a87809926db5c477a9a1a58488ec1
		}else if(action.equals("sweepticket")){
			result = sweepTicket(request,uin,comId);
			//http://127.0.0.1/zld/collectorrequest.do?action=sweepticket&bmoney=3&score=1&token=5286f078c6d2ecde9b30929f77771149
		}else if(action.equals("deductscore")){//��΢���﷢�ͣ��û������ȡ
			result = deductScore(request,uin,comId,infoMap);
			//http://127.0.0.1/zld/collectorrequest.do?action=deductscore&score=1&ticketid=&token=
		}else if(action.equals("todayaccount")){//�����˻������֡����Ͳ�ѯ
			result = todayAccount(request,uin,comId,infoMap);
			//http://127.0.0.1/zld/collectorrequest.do?action=todayaccount&token=5286f078c6d2ecde9b30929f77771149
		}else if(action.equals("remainscore")){
			Double todayscore = 0d;//ʣ�����
			Map score = daService.getMap("select reward_score from user_info_tb where id=? ", new Object[] { uin });
			if(score != null && score.get("reward_score") != null){
				todayscore = Double.valueOf(score.get("reward_score") + "");
			}
			infoMap.put("score", todayscore);
			result =StringUtils.createJson(infoMap);
		}else if(action.equals("queryaccount")){//���ݳ��Ʋ�ѯ�ڸó������˻���ϸ
			result = queryAccount(request,uin);
			//http://127.0.0.1/zld/collectorrequest.do?action=queryaccount&token=2dd4b1b320225dfd4fc44ad6b53fa734&carnumber=��QLL122
		}else if(action.equals("posincome")){//pos�����ɶ���
			result = posIncome(request, comId, groupId, uin);
			//http://127.0.0.1/zld/collectorrequest.do?action=posincome&token=2dd4b1b320225dfd4fc44ad6b53fa734&carnumber=��QLL122
		}else if(action.equals("getfreeparks")){//��ѯ���п��г�λ
			result = getFreeParks(request,comId);
			//http://127.0.0.1/zld/collectorrequest.do?action=getfreeparks&token=5ebac7b26ce782ebffaadf76b1519a64
		}else if(action.equals("bondcarpark")){//�󶨳�λ������
			result = bondCarPark(request,comId);
			//http://127.0.0.1/zld/collectorrequest.do?action=bondcarpark&token=a0b952263fbb0a264194a1443c71174d&orderid=11111&id=134
		}else if(action.equals("liftrodrecord")){//̧�˼�¼
			result = liftRod(request,uin,comId);
			//http://127.0.0.1/zld/collectorrequest.do?action=liftrodrecord&token=d481a6fb58e758c3f0ef9aa7c4bdff29&passid=13
		}else if(action.equals("liftrodreason")){//̧�˼�¼������ԭ��
			result = liftRodReason(request);
			//http://127.0.0.1/zld/collectorrequest.do?action=liftrodreason&token=d481a6fb58e758c3f0ef9aa7c4bdff29&lrid=3&reason=1
		}else if(action.equals("liftroduppic")){//̧�˼�¼���ϴ�ͼƬ
			result = liftRodPic(request);
			//http://127.0.0.1/zld/collectorrequest.do?action=liftroduppic&token=a0b952263fbb0a264194a1443c71174d&lrid=3
		}else if(action.equals("getberths")){//��ѯ��λ��Ϣ��ǩ��
			//http://127.0.0.1/zld/collectorrequest.do?action=getberths&token=522b6bc2abd903eacf6b9a4ae3359815&berthid=15&devicecode=357143047019192
			result =getBerths(request,uin,comId,token, groupId);
			result= result==null?"{}":result.replace("null", "");
			//logger.error("ǩ����"+result);
		}else if(action.equals("workout")){//ǩ�˲���
			//http://127.0.0.1/zld/collectorrequest.do?action=workout&token=f0f8f63ebd720c34077ee6b302b15cff&berthid=-1&workid=44068&from=1
			result = workOut(request,uin);
		}else if(action.equals("uporderpic")){//�ϴ�����ͼƬ 
			//http://127.0.0.1/zld/collectorrequest.do?action=uporderpic&token=ca67649c7a6c023e08b0357658c08c3d&orderid=&type=
			result = uporderpic(request);
		}else if(action.equals("getecsorder")){//���ݳ��Ʋ��ұ����ų������ӵ�
			//http://127.0.0.1/zld/collectorrequest.do?action=getecsorder&token=149032374fcb20d719a08acb237cfc4d&comid=8690&car_number=��095455
			result = queryEscOrder(request, uin, comId, groupId);
		}else if(action.equals("payescorder")){//֧���ӵ���֧�������ӵ�����
			//http://127.0.0.1/zld/collectorrequest.do?action=payescorder&token=53aa954da7de01e1e7439fb386c41234&orderlist=
			result = payEscOrder(request, uin, groupId, comId);
		}else if(action.equals("getorderpic")){//ȡ����ͼƬorderid ������� type:0�볡��Ƭ��1������Ƭ 2׷��
			getOrderPic(request,response);
		  //http://127.0.0.1/zld/collectorrequest.do?action=getorderpic&token=a3a0dafbe61d9b491b6094b6f64a0693&orderid=842138&type=0
		}else if(action.equals("regpossequence")){//ע��POS������
			result = regPosSequecce(request,comId,uin);
			 //http://127.0.0.1/zld/collectorrequest.do?action=regpossequence&token=a3a0dafbe61d9b491b6094b6f64a0693&device_code=
		}else if(action.equals("paydetail")){//��ѯ����ǩ���ڵ��շ�
			result = getPayDetail(request);
			//http://127.0.0.1/zld/collectorrequest.do?action=paydetail&token=149032374fcb20d719a08acb237cfc4d&workid=768
		}else if(action.equals("paymonthorder")){//֧���¿�
			result = payMonthOrder(request,uin);
			//http://127.0.0.1/zld/collectorrequest.do?action=paymonthorder&token=149032374fcb20d719a08acb237cfc4d&orderid=
		}else if(action.equals("payposorder")){//pos�����ý��㣬�����¿������֧��
			result = payPosOrder(request, uin, groupId);
			logger.error("payposorder result :"+result);
			/*http://127.0.0.1/zld/collectorrequest.do?action=payposorder&
			token=2d0f797087b3c43b27700a1ef0b14a10&orderid=842690&version=1341&total=0*/
		}else if(action.equals("actcard")){//���Ƭ
			result = actCard(request, uin, groupId, comId);
		}else if(action.equals("bindcard")){//��Ƭ���û�
			result = bindCard(request, uin, groupId, comId);
		}else if(action.equals("cardinfo")){//��Ƭ����
			result = cardInfo(request, uin, groupId);
			/*http://127.0.0.1/zld/collectorrequest.do?action=cardinfo&
			token=&version=&uuid=*/
		}else if(action.equals("chargecard")){//��ֵ��Ƭ
			result = chargeCard(request, uin, groupId, comId);
			/*http://127.0.0.1/zld/collectorrequest.do?action=chargecard&
			token=&version=&uuid=&money=*/
		}else if(action.equals("closecard")){//ע����Ƭ
			result = closeCard(request, uin, groupId);
			/*http://127.0.0.1/zld/collectorrequest.do?action=closecard&
			token=&version=&uuid=*/
		}else if(action.equals("zfbpayqr")){//����֧����֧����ά��
			//http://127.0.0.1/zld/collectorrequest.do?action=zfbpayqr&orderid=&total=
			result = zfbPayQr(request, uin);
		}else if(action.equals("getservertime")){
			result = getCurrentTime() + "";
		}
		
		AjaxUtil.ajaxOutput(response, result);
		return null;
	}
	
	private long getCurrentTime(){
		try {
			long currentTime = System.currentTimeMillis();
			return currentTime;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	private String zfbPayQr(HttpServletRequest request, Long uid) {
		Long orderId = RequestUtil.getLong(request, "orderid", -1L);
		Double total = StringUtils.formatDouble(RequestUtil.getString(request, "total"));
		String ret = "{\"state\":\"0\",\"errmsg\":\"���Ժ�����\"}";
		String qr = "";
		if(orderId>0&&total>0){
			Map orderMap = pService.getMap("select uin,car_number,comid,create_time from order_tb where id =? ", new Object[]{orderId});
			Long uin = -1L;
			String car_number ="";
			if(orderMap!=null){
				//��ѯ��˾��Ϣ
				Long ctime = (Long)orderMap.get("create_time");
				String time = TimeTools.getTime_MMdd_HHmm(ctime*1000)+"-"+TimeTools.getTime_MMdd_HHmm(System.currentTimeMillis());
				uin = (Long)orderMap.get("uin");
				car_number=(String)orderMap.get("car_number");
				qr = toAlipayQrTradePay.qrPay(orderId+"", total+"", "ͣ����-�շ�Ա("+uid+")����("+car_number+")ʱ��("+time+")", uin+"_7_"+uid, uid);
				ret= "{\"state\":\"1\",\"errmsg\":\"\",\"qrcode\":\""+qr+"\"}";
			}else {
				ret= "{\"state\":\"0\",\"errmsg\":\"����������\"}";
			}
		}
		logger.error("zfbPayQr-->"+ret);
		return ret;
	}

	private String closeCard(HttpServletRequest request, Long uid, Long groupId){
		try {
			String nfc_uuid = RequestUtil.processParams(request, "uuid");
			Integer version = RequestUtil.getInteger(request, "version", -1);
			logger.error("nfc_uuid:"+nfc_uuid+",version:"+version);
			UnbindCardReq req = new UnbindCardReq();
			req.setGroupId(groupId);
			req.setNfc_uuid(nfc_uuid);
			req.setUnBinder(uid);
			BaseResp resp = cardService.returnCard(req);
			if(resp.getResult() == 1){
				return "{\"result\":\"1\",\"errmsg\":\""+resp.getErrmsg()+"\"}";
			}else{
				return "{\"result\":\"-1\",\"errmsg\":\""+resp.getErrmsg()+"\"}";
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return "{\"result\":\"-1\",\"errmsg\":\"ϵͳ����\"}";
	}
	
	private String chargeCard(HttpServletRequest request, Long uid, Long groupId, Long comid){
		try {
			String nfc_uuid = RequestUtil.processParams(request, "uuid");
			Integer version = RequestUtil.getInteger(request, "version", -1);
			Double money = RequestUtil.getDouble(request, "money", 0d);
			logger.error("nfc_uuid:"+nfc_uuid+",version:"+version+",money:"+money);
			CardChargeReq req = new CardChargeReq();
			req.setCashierId(uid);
			req.setChargeType(0);
			req.setGroupId(groupId);
			req.setMoney(money);
			req.setNfc_uuid(nfc_uuid);
			req.setParkId(comid);
			
			BaseResp resp = cardService.cardCharge(req);
			if(resp.getResult() == 1){
				return "{\"result\":\"1\",\"errmsg\":\""+resp.getErrmsg()+"\"}";
			}else{
				return "{\"result\":\"-1\",\"errmsg\":\""+resp.getErrmsg()+"\"}";
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return "{\"result\":\"-1\",\"errmsg\":\"ϵͳ����\"}";
	}
	
	private String cardInfo(HttpServletRequest request, Long uid, Long groupId){
		try {
			String nfc_uuid = RequestUtil.processParams(request, "uuid");
			Integer version = RequestUtil.getInteger(request, "version", -1);
			logger.error("nfc_uuid:"+nfc_uuid+",version:"+version);
			CardInfoReq req = new CardInfoReq();
			req.setGroupId(groupId);
			req.setNfc_uuid(nfc_uuid);
			CardInfoResp resp = cardService.getCardInfo(req);
			JSONObject object = JSONObject.fromObject(resp);
			logger.error(object.toString());
			return object.toString();
		} catch (Exception e) {
			logger.error(e);
		}
		return "{\"result\":\"-1\",\"errmsg\":\"ϵͳ����\"}";
	}
	
	private String actCard(HttpServletRequest request, Long uid, Long groupId, Long comid){
		try {
			String nfc_uuid = RequestUtil.processParams(request, "uuid");
			ActCardReq req = new ActCardReq();
			req.setUid(uid);
			req.setNfc_uuid(nfc_uuid);
			req.setGroupId(groupId);
			req.setParkId(comid);
			BaseResp resp = cardService.actCard(req);
			if(resp.getResult() == 1){
				return "{\"result\":\"1\",\"errmsg\":\""+resp.getErrmsg()+"\"}";
			}else{
				return "{\"result\":\"0\",\"errmsg\":\""+resp.getErrmsg()+"\"}";
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return "{\"result\":\"0\",\"errmsg\":\"ϵͳ����\"}";
	}
	
	private String bindCard(HttpServletRequest request, Long uid, Long groupId, Long comid){
		try {
			String nfc_uuid = RequestUtil.processParams(request, "uuid");
			String mobile = RequestUtil.processParams(request, "mobile");
			String carNumber = AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "carnumber")).toUpperCase();
			BindCardReq req = new BindCardReq();
			req.setBinder(uid);
			req.setNfc_uuid(nfc_uuid);
			req.setMobile(mobile);
			req.setCarNumber(carNumber);
			req.setGroupId(groupId);
			req.setParkId(comid);
			BaseResp resp = null;
			if(mobile == null || "".equals(mobile)){
				resp = cardService.bindPlateCard(req);
			}else{
				resp = cardService.bindUserCard(req);
			}
			if(resp.getResult() == 1){
				return "{\"result\":\"1\",\"errmsg\":\""+resp.getErrmsg()+"\"}";
			}else{
				return "{\"result\":\"0\",\"errmsg\":\""+resp.getErrmsg()+"\"}";
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return "{\"result\":\"0\",\"errmsg\":\"ϵͳ����\"}";
	}
	
	/**
	 * ǩ��������֤
	 * @param request
	 * @param uin
	 * @return
	 */
	private int signOutValid(HttpServletRequest request, Long uin){
		try {
			String collpwd = RequestUtil.processParams(request, "collpwd");
			Integer version = RequestUtil.getInteger(request, "version", -1);//�汾��
			logger.error("validate>>>uid:"+uin+",collpwd:"+collpwd+",version:"+version);
			if(version > 1340){//�ͻ���v1.3.40�Ժ��дι���
				Map<String, Object> setMap = pService.getMap("select s.signout_valid," +
						"s.signout_password from collector_set_tb s,user_info_tb u " +
						"where s.role_id=u.role_id and u.id=? ", new Object[]{uin});
				logger.error("validate>>>uid:"+uin+",setMap:"+setMap);
				if(setMap != null){
					Integer signout_valid = (Integer)setMap.get("signout_valid");
					logger.error("signout_valid:"+signout_valid);
					if(signout_valid == 1 && setMap.get("signout_password") != null){
						String password = (String)setMap.get("signout_password");
						logger.error("validate>>>uid:"+uin+",signout_password:"+password);
						if(!password.equals("")){
							if(!collpwd.equals(password)){//У������
								return -1;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("validate>>>uin:"+uin, e);
		}
		return 1;
	}
	
	/**
	 * �鿴�շѻ���������֤
	 * @param request
	 * @param uin
	 * @return
	 */
	private int validate(HttpServletRequest request, Long uin){
		try {
			String collpwd = RequestUtil.processParams(request, "collpwd");
			logger.error("validate>>>uid:"+uin+",collpwd:"+collpwd);
			Map<String, Object> setMap = pService.getMap("select hidedetail,s.password from collector_set_tb s,user_info_tb u " +
					" where s.role_id=u.role_id and u.id=? ", new Object[]{uin});
			logger.error("validate>>>uid:"+uin+",setMap:"+setMap);
			if(setMap != null){
				Integer hidedetail = (Integer)setMap.get("hidedetail");
				logger.error("hidedetail:"+hidedetail);
				if(hidedetail == 1 && setMap.get("password") != null){//�����շѻ���
					String password = (String)setMap.get("password");
					logger.error("validate>>>uid:"+uin+",password:"+password);
					if(!password.equals("")){
						if(!collpwd.equals(password)){//У������
							return -1;
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("validate>>>uin:"+uin, e);
		}
		return 1;
	}
	
	private String autoPayPosOrder(HttpServletRequest request, Long uid, Long groupId){
		try {
			//----------------------------����--------------------------------//
			Long orderId = RequestUtil.getLong(request, "orderid", -1L);//������� 
			Double money = RequestUtil.getDouble(request, "total", 0d);//�ܽ��
			String imei  =  RequestUtil.getString(request, "imei");//�ֻ�����
			Integer version = RequestUtil.getInteger(request, "version", -1);//�汾��
			Long endtime = RequestUtil.getLong(request, "endtime", -1L);
			logger.error("orderid:"+orderId+",money:"+money+",imei:"+imei+
					",version:"+version+",endtime:"+endtime);
			//----------------------------��������--------------------------------//
			//Integer isMonthUser = RequestUtil.getInteger(request, "ismonthuser",0);
			//�Ƿ����¿��û�,5�����¿��û�����collectorrequest.do?action=getberths��ȡ�ĸ�ֵ
			//----------------------------У�����--------------------------------//
			AutoPayPosOrderFacadeReq req = new AutoPayPosOrderFacadeReq();
			req.setOrderId(orderId);
			req.setMoney(money);
			req.setImei(imei);
			req.setUid(uid);
			req.setVersion(version);
			req.setGroupId(groupId);
			req.setEndTime(endtime);
			AutoPayPosOrderResp resp = payOrderFacade.autoPayPosOrder(req);
			logger.error(resp.toString());
			if(resp != null){
				JSONObject object = JSONObject.fromObject(resp);
				logger.error(object.toString());
				return object.toString();
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return "{\"result\":\"0\",\"errmsg\":\"����ʧ��\"}";
	}
	
	/*pos��֧�����������¿�����*************/
	private String payPosOrder(HttpServletRequest request, Long uin, Long groupId) {
		Integer version = RequestUtil.getInteger(request, "version", -1);
		logger.error("version:"+version);
		if(version > 1340){
			return autoPayPosOrder(request, uin, groupId);
		}
		//���ж��¿��û�
		Integer isMonthUser = RequestUtil.getInteger(request, "ismonthuser",0);//�Ƿ����¿��û�
		if(isMonthUser==5){
			return payMonthOrder(request, uin);
		}
		Long orderId = RequestUtil.getLong(request, "orderid", -1L);//������� 
		//Long brethOrderId = RequestUtil.getLong(request, "brethorderid", -1L);//�شŶ������
		Double money = RequestUtil.getDouble(request, "total", 0d);//�ܽ��
		if(money==0){//Ԥ�ս�����֧��ʱ���������ֽ�֧������
			return "{\"result\":\"3\",\"errmsg\":\"���0Ԫ����Ҫ֧��\"}";
		}
		Integer workid = RequestUtil.getInteger(request, "workid",0);//�������
		String imei  =  RequestUtil.getString(request, "imei");//�ֻ�����
		Map orderMap = daService.getMap("select * from order_tb where id =? ", 
				new Object[]{orderId});
		Long ntime = System.currentTimeMillis()/1000;
		Long brethOrderId = commonMethods.getBerthOrderId(orderId);//�شŶ������
		Long end_time = commonMethods.getOrderEndTime(brethOrderId, uin, ntime);
		if(orderMap!=null&&!orderMap.isEmpty()){
			Double prePay = StringUtils.formatDouble(orderMap.get("prepaid"));//Ԥ�ս��
			if(prePay>=money&&money>0){//Ԥ�ս�����֧��ʱ���������ֽ�֧������
				return "{\"result\":\"3\",\"errmsg\":\"Ԥ�ս�����֧��\"}";
			}else if(prePay>0){
				money = money-prePay;//ֻ�ղ���Ĳ��ֽ�֧���ɹ����ٰѶ�������޸�Ϊʵ�ʽ��
			}
			Long btime = (Long)orderMap.get("create_time");
			String duration = StringUtils.getTimeString(btime, end_time);
			Long user = (Long)orderMap.get("uin");
			Long comId = (Long)orderMap.get("comid");
			Integer is_auth =0;
			String carNumber = (String)orderMap.get("car_number");
			if(user==null||user<1){
				if(carNumber!=null&&!"".equals(carNumber)){
					Map carMap = daService.getMap("select uin,is_auth from car_info_tb where car_number=? and state=? ",
							new Object[]{carNumber,1});
					if(carMap!=null&&carMap.get("uin")!=null){
						user = (Long)carMap.get("uin");
						is_auth=(Integer)carMap.get("is_auth");
					}
				}
			}
			if(user>0){//�ҵ��˳���
				//���û����
				Double balance =0d;
				Map userMap = daService.getMap("select balance,wxp_openid,is_auth,credit_limit from user_info_tb where id=?",new Object[]{user});
				if(userMap!=null){
					balance=StringUtils.formatDouble(userMap.get("balance"));
					if(balance==0)
						return "{\"result\":\"-2\",\"errmsg\":\"��������\"}";
				}else {
					return "{\"result\":\"-3\",\"errmsg\":\"����δע��\"}";
				}
				//�鳵�����ã��Ƿ��������Զ�֧����û������ʱ��Ĭ��25Ԫ�����Զ�֧�� 
				Integer autoCash=1;
				Map upMap = daService.getPojo("select auto_cash,limit_money from user_profile_tb where uin =?", new Object[]{user});
				Integer limitMoney =25;
				if(upMap!=null&&upMap.get("auto_cash")!=null){
					autoCash= (Integer)upMap.get("auto_cash");
					limitMoney = (Integer)upMap.get("limit_money");
				}
				if(autoCash!=null&&autoCash==1){//�������Զ�֧��
					boolean isupmoney=true;//�Ƿ�ɳ����Զ�֧���޶�
					if(limitMoney!=null){
						if(limitMoney==-1||limitMoney>=money)//����ǲ��޻����֧�������Զ�֧�� 
							isupmoney=false;
					}
					if(isupmoney){//�����Զ�֧���޶�
						return "{\"result\":\"-1\",\"errmsg\":\"�����Զ�֧���޶"+limitMoney+"Ԫ\"}";
					}
					//�����Ƿ���֤ͨ�������ö���Ƿ����꣬����ʱ�����ö�ȵֿ�
					Double creditLimit=0.0;//�������ö�ȳ�ֵ��֧��ʧ��ʱ��Ҫ����ֵ
					if((balance)<money){//����ʱ���鳵���Ƿ���֤ͨ��
						creditLimit = money-(balance);
						if(is_auth==1){//����֤�ĳ��ƣ��鳵���Ƿ��п������ö��
							is_auth = (Integer)userMap.get("is_auth");
							Double climit = StringUtils.formatDouble(userMap.get("credit_limit"));
							if(is_auth==1&&climit>=creditLimit){
								int ret = daService.update("update user_info_tb set balance=balance+?,credit_limit=credit_limit-? where id =? ", 
										new Object[]{creditLimit,creditLimit,user});
								logger.error(">>>>>>>auto pay ,�������ö�ȵֿۣ�"+creditLimit+",ret��"+ret);
								if(ret==1){//���ö�ȳ�ֵ�ɹ�
									balance = money;
								}else {
									creditLimit=0.0;
								}
							}
						}
					}
					if((balance)>=money){//������֧��
						//int re = publicMethods.payOrder(orderMap, money, user, 2,0,-1L,null);//����֧������
						int re = publicMethods.payOrder(orderMap, money, user, 2,0,-1L,null, brethOrderId, uin);//����֧������
						if(re==5){//�ɹ�֧��
							if(prePay>0){
								money = (money+prePay);
								int ret = daService.update("update order_tb set total=? where id =? ", new Object[]{money,orderId});
								logger.error("payposorder>>>��Ԥ�ս�ʵ��֧����"+(money-prePay)+",Ԥ�ս�"+prePay+",������"+money+",ret:"+ret);
							}
							//�޸Ĳ�λ״̬
							Long berthnumber=(Long)orderMap.get("berthnumber");
							if(berthnumber!=null&&berthnumber>0){//���ݲ�λ�Ÿ��²�λ״̬
								int ret =daService.update("update com_park_tb set state=?,order_id=? where id =? and order_id=?",  new Object[]{0,null,berthnumber,orderId});
								logger.error("payposorder �����ӵ����²�λ��ret :"+ret+",berthnumber��"+berthnumber+",orderid:"+orderId);
							}
							//�޸ĵشŶ���
							if(brethOrderId>0){
								int r = daService.update("update berth_order_tb set out_uid=?,order_total=?  where id=? ", new Object[]{uin,money,brethOrderId});
								logger.error("payposorder �޸ĵشŶ�����ret :"+r);
							}
							//������¼
							if(workid>0){//�й������ʱ����һ�´˶����ǲ������ϰ��ڼ�����ģ�������ǣ�Ҫ�Ѵ�ǰԤ�������빤����α�ǩ��ʱ�۳��ⲿ�֣����Ѷ������д�빤����α�
								Long count = daService.getLong("select count(ID) from work_detail_tb where workid=? and orderid=? ",
										new Object[]{workid,orderId});
								if(count<1){//���������ϰ��ڼ������
									int ret = 0;
									if(prePay>0){
										ret = daService.update("update parkuser_work_record_tb set history_money = history_money+? where id =? ",
												new Object[]{prePay,workid});
										logger.error("payposorder pos���Զ����㣬�����ڱ���β����Ķ�����Ԥ�ս�"+prePay+"д���α�"+ret);
									}
									if(money>0){
										ret = daService.update("insert into work_detail_tb (uid,orderid,bid,workid,berthsec_id) values(?,?,?,?,?)", 
												new Object[]{uin,orderId,berthnumber,workid,orderMap.get("berthsec_id")});
										logger.error("payposorder pos���Զ����㣬�����ڱ���β����Ķ�����������ţ�"+orderId+"����κţ�"+workid+",д���������"+ret);
									}
								}
							}
							logger.error("payposorder : success,����Ϣ�������������շ�Ա....");
							logService.insertMessage(comId, 2, user,carNumber, orderId, (money-prePay), duration,0, btime, end_time, 0);
							//logService.insertParkUserMessage(comId, 2, uid, carNumber, orderId, money,duration,0, btime, etime,0);
							return "{\"result\":\"2\",\"errmsg\":\"Ԥ�ս�"+prePay+"Ԫ�����֧����"+(money-prePay)+"Ԫ\",\"duration\":\""+duration+"\"}";
						}else {
							if(re!=5&&creditLimit>0){//֧��ʧ��ʱ�� ���ö��Ҫ����ֵ
								int ret = daService.update("update user_info_tb set balance=balance-?,credit_limit=credit_limit+? where id =? ", 
										new Object[]{creditLimit,creditLimit,user});
								logger.error("payposorder>>>>>>>auto pay ,�������ö�ȵֿ�֧��ʧ�ܣ� ���ö�ȷ���ֵ����"+creditLimit+",ret��"+ret);
							}
							return "{\"result\":\"-4\",\"errmsg\":\"�������֧��ʧ��\"}";
						}
					}else {
						return "{\"result\":\"-2\",\"errmsg\":\"��������\"}";
					}
				}
			}else {
				return "{\"result\":\"0\",\"errmsg\":\"����ʧ��\"}";
			}
		}
		return "{\"result\":\"0\",\"errmsg\":\"����ʧ�ܣ����������� \"}";
	}
	//�¿�֧��
	private String payMonthOrder(HttpServletRequest request,Long uin) {
		Long ntime = System.currentTimeMillis()/1000;
		Long orderId = RequestUtil.getLong(request, "orderid", -1L);
		//Long brethOrderId = RequestUtil.getLong(request, "brethorderid", -1L);//�شŶ������
		Long brethOrderId = commonMethods.getBerthOrderId(orderId);//�شŶ������
		String imei  =  RequestUtil.getString(request, "imei");
		String result = "";
		Map<String, Object> orderMap = daService.getPojo("select * from order_tb where id=?", 
				new Object[]{ orderId });
		Integer state = (Integer)orderMap.get("state");
		if(state != null && state == 1){//�ѽ��㣬����
			return "{\"result\":\"-2\",\"errmsg\":\"�����ѽ���!\"}";
		}
		Long create_time = (Long)orderMap.get("create_time");
		Long end_time = commonMethods.getOrderEndTime(brethOrderId, uin, ntime);
		List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
		//���¶���״̬
		Map<String, Object> orderSqlMap = new HashMap<String, Object>();
		orderSqlMap.put("sql", "update order_tb set state=?,total=?,end_time=?,pay_type=?,imei=?,out_uid=? where id=?");
		orderSqlMap.put("values", new Object[]{1, 0.0, end_time, 3, imei, uin, orderId});
		bathSql.add(orderSqlMap);
		Long berthId = -1L;
		if(orderMap.get("berthnumber") != null){
			berthId = (Long)orderMap.get("berthnumber");
		}
		if(berthId != null && berthId > 0){
			//���²�λ״̬
			Map<String, Object> berthSqlMap = new HashMap<String, Object>();
			berthSqlMap.put("sql", "update com_park_tb set state=?,order_id=? where id =? and order_id=?");
			berthSqlMap.put("values", new Object[]{0, null, berthId, orderId});
			bathSql.add(berthSqlMap);
		}
		if(brethOrderId != null && brethOrderId > 0){
			//���³���������״̬
			Map<String, Object> berthOrderSqlMap = new HashMap<String, Object>();
			berthOrderSqlMap.put("sql", "update berth_order_tb set out_uid=?,order_total=?  where id=? ");
			berthOrderSqlMap.put("values", new Object[]{uin, 0.0, brethOrderId});
			bathSql.add(berthOrderSqlMap);
		}
		boolean b = daService.bathUpdate2(bathSql);
		logger.error("payMonthOrder ���µشŶ�����b :" + b + ",(update com_park_tb orderid):" + orderId + ",berthid:" + berthId + ",brethOrderid:" + brethOrderId);
		if(b){
			String duration = StringUtils.getTimeString(create_time, end_time);
			result ="{\"result\":\"1\",\"errmsg\":\"�¿�����ɹ�\",\"duration\":\""+duration+"\"}";
		}else {
			result ="{\"result\":\"0\",\"errmsg\":\"����ʧ�ܣ����������� \"}";
		}
		logger.error("payMonthOrder result:"+result);
		return result;
	}
	private String getPayDetail(HttpServletRequest request) {
		Long workId = RequestUtil.getLong(request, "workid", -1L);
		Map workMap  = daService.getMap("select history_money,start_time from parkuser_work_record_tb where id=? ", new Object[]{workId});
		//��һ��λ��Ԥ�ս��
		Double historyPrePay = 0.0;
		if(workMap!=null&&!workMap.isEmpty()){
			historyPrePay = StringUtils.formatDouble(workMap.get("history_money"));
			logger.error("paydetail,�渶��"+historyPrePay);
		}
			
		List<Map<String, Object>> orderList = daService.getAll("select total,state,prepaid,pay_type from order_tb" +
				" where id in(select orderid from work_detail_tb where workid=? )", new Object[]{workId});
		Double cash =0.0;
		Double card =0.0;
		Double etc =0.0;
		//Double prepay =0.0;
		logger.error("paydetail,�շ���ϸ��"+orderList);
		if(orderList!=null&&!orderList.isEmpty()){
			for(Map<String, Object> map : orderList){
				Integer state  = (Integer)map.get("state");
				if(state!=null&&state==1){
					Integer payType =(Integer)map.get("pay_type");
					if(payType==1){
						cash +=StringUtils.formatDouble(map.get("total"));
					}else if(payType==2){
						etc +=StringUtils.formatDouble(map.get("total"))-StringUtils.formatDouble(map.get("prepaid"));
						cash +=StringUtils.formatDouble(map.get("prepaid"));
					}else {
						card +=StringUtils.formatDouble(map.get("total"));
					}
				}else {
					cash +=StringUtils.formatDouble(map.get("prepaid"));
				}
			}
		}
		Map<String, Object> resultMap = new HashMap<String, Object>();
		resultMap.put("cashpay",StringUtils.formatDouble(cash-historyPrePay));
		resultMap.put("onlinepay", StringUtils.formatDouble(etc));
		resultMap.put("cardpay", StringUtils.formatDouble(card));
		logger.error("paydetail,���أ�"+resultMap);
		return StringUtils.createJson(resultMap);
	}
	/**
	 * ע��POS��
	 * @param request
	 * @param comid
	 * @param uid
	 * @return
	 */
	private String regPosSequecce(HttpServletRequest request,Long comid,Long uid) {
		Long ntime = System.currentTimeMillis()/1000;
		String device_code = RequestUtil.getString(request, "device_code");
		Long count = daService.getLong("select count(Id) from mobile_tb where device_code=?  ", new Object[]{device_code});
		String result = "";
		if(count>0){//�豸�ѵ�¼
			result = "{\"result\":\"-1\",\"errmst\":\"�豸��ע���\"}";
		}else {
			Long time = ntime;
			String sql = "insert into  mobile_tb (comid,uid,mode,create_time,device_code) values" +
					"(?,?,?,?,?)";
			Object [] values = new Object[]{comid,uid,"POS��",time,device_code};
			int ret = daService.update(sql, values);
			if(ret==1)
				result = "{\"result\":\"1\",\"errmst\":\"�豸ע��ɹ�\"}";
			else
				result = "{\"result\":\"0\",\"errmst\":\"�豸ע��ʧ��\"}";
		}
		return result;
	}

	private void getOrderPic(HttpServletRequest request,HttpServletResponse response) {
		Long orderId = RequestUtil.getLong(request, "orderid", -1L);
		Integer type = RequestUtil.getInteger(request, "type", 0);
		BasicDBObject conditions = new BasicDBObject();
		conditions.put("orderid", orderId);
		conditions.put("type", type);
		byte[] pic = mongoDbUtils.getPictures("carstop_pics", conditions);
		if(pic!=null){
			response.setDateHeader("Expires", System.currentTimeMillis()+4*60*60*1000);
			//response.setStatus(httpc);
			Calendar c = Calendar.getInstance();
			c.set(1970, 1, 1, 1, 1, 1);
			response.setHeader("Last-Modified", c.getTime().toString());
			response.setContentLength(pic.length);
			response.setContentType("image/jpeg");
			System.err.println(pic.length);
			try {
				OutputStream o = response.getOutputStream();
				o.write(pic);
				o.flush();
				o.close();
				response.flushBuffer();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else {
			try {
				response.sendRedirect("http://sysimages.tq.cn/images/webchat_101001/common/kefu.png");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private String payEscapeOrder(HttpServletRequest request, Long uid, Long groupId, Long comid){
		try {
			String ids = AjaxUtil.decodeUTF8(RequestUtil.getString(request, "orderlist"));
			Integer version = RequestUtil.getInteger(request, "version", -1);//�汾��
			String nfc_uuid = RequestUtil.processParams(request, "uuid");//ˢ��֧���Ŀ�Ƭ���
			Integer bindcard = RequestUtil.getInteger(request, "bindcard", 0);//0:�ͻ��˵����󶨳����ֻ��ŵ���ʾ�� 1����������ʾ��ֱ��ˢ��Ԥ��
			Integer payType = RequestUtil.getInteger(request, "paytype", 0);//֧����ʽ 0���ֽ�֧���� 1��ˢ��֧��
			Long bid = RequestUtil.getLong(request, "bid", -1L);//��λ���(2016-10-14��ӣ�Ϊ�˼�¼���ĸ���λ��׷�ɵĶ���)
			//---------------------------�����Ĳ���----------------------------//
			Integer workid = RequestUtil.getInteger(request, "workid",0);//�������
			logger.error("ids:"+ids+",workid:"+workid+",version:"+version+
					",nfc_uuid:"+nfc_uuid+",bindcard:"+bindcard+",bid:"+bid);
			
			List<Map<String, Object>> list =ParseJson.jsonToList(ids);
			if(list != null && !list.isEmpty()){
				for(Map<String, Object> map : list){
					Long orderId = Long.valueOf(map.get("orderid") + "");
					Double total = StringUtils.formatDouble(map.get("total"));
					if(orderId != null && orderId > 0){
						PayEscapePosOrderFacadeReq req = new PayEscapePosOrderFacadeReq();
						req.setOrderId(orderId);
						req.setBindcard(bindcard);
						req.setMoney(total);
						req.setNfc_uuid(nfc_uuid);
						req.setPayType(payType);
						req.setUid(uid);
						req.setVersion(version);
						req.setGroupId(groupId);
						req.setParkId(comid);
						req.setBerthId(bid);
						PayEscapePosOrderResp resp = payOrderFacade.payEscapePosOrder(req);
						if(resp.getResult() != 1){
							return "{\"result\":\""+resp.getResult()+"\",\"errmsg\":\""+resp.getErrmsg()+"\"}";
						}
					}
				}
				return "{\"result\":\"1\",\"errmsg\":\"����ɹ�\"}";
			}
			
		} catch (Exception e) {
			logger.error(e);
		}
		return "{\"result\":\"0\",\"errmsg\":\"ϵͳ����\"}";
	}

	private String payEscOrder(HttpServletRequest request, Long uin, Long groupId, Long comid) {
		String result = "";
		String ids = AjaxUtil.decodeUTF8(RequestUtil.getString(request, "orderlist"));
		Integer workid = RequestUtil.getInteger(request, "workid",0);//�������
		Integer version = RequestUtil.getInteger(request, "version", -1);//�汾��
		logger.error("payOrder:"+ids+",version:"+version);
		if(version > 1340){//
			return payEscapeOrder(request, uin, groupId, comid);
		}
		// [{"car_number":"��F55555","duartion":"2����","end":"1461206247","total":"1.00","orderid":"841808","prepay":"8.0","start":"1461206101","ischeck":true}]
		List<Map<String, Object>> array =ParseJson.jsonToList(ids);
		boolean re = true;
		String errmsg="";
		if(array!=null&&!array.isEmpty()){
			for(Map<String, Object> map : array){
				Long orderId =Long.valueOf(map.get("orderid")+"");
				Double total = StringUtils.formatDouble(map.get("total"));
				if(orderId!=null&&orderId>0){
					String ret = payOrder(orderId, total, uin, workid, "json", "", 0,1,-1L,-1L);
					logger.error("payOrder  payEscOrder  ���㶩��:"+map+",ret:"+ret);
					if(!"1".equals(ret)){
						re = false;
						if(errmsg.length()>0)
							errmsg +=";";
						Long start = Long.valueOf(map.get("start")+"");
						Long end = Long.valueOf(map.get("end")+"");
						errmsg+=TimeTools.getTime_yyyyMMdd_HHmm(start*1000)+"-"+TimeTools.getTime_yyyyMMdd_HHmm(end*1000)+"�Ķ�������ʧ��";
					}
				}
			}
		}
		if(re){
			result= "{\"result\":\"1\",\"errmsg\":\"����ɹ�\"}";
		}else {
			result= "{\"result\":\"0\",\"errmsg\":\""+errmsg+"\"}";
		}
		return result;
	}
	
	
	private String queryEscOrder(HttpServletRequest request, Long uin, Long comId, Long groupId) {
		try {
			Long berthSgeId = RequestUtil.getLong(request, "berthid",-1L);
			String carNumber = AjaxUtil.decodeUTF8(RequestUtil.getString(request, "car_number")).toUpperCase().trim();
			Integer version = RequestUtil.getInteger(request, "version", -1);//�汾��
			
			logger.error("berthSgeId:"+berthSgeId+",carNumber:"+carNumber+",version:"+version+",groupid:"+groupId);
			String ret = "1";
			String errmsg = "";
			String orders = "[]";
			if(groupId != null && groupId > 0){
				List<Object> params = new ArrayList<Object>();
				boolean isPursue = commonMethods.pursueInCity(groupId);
				logger.error("isPursue:"+isPursue);
				List<Object> comList = null;
				if(isPursue){
					Group group = pService.getPOJO("select cityid from org_group_tb where id=? and cityid>? ",
							new Object[]{groupId, 0}, Group.class);
					comList = commonMethods.getparks(group.getCityid());
				} else {
					comList = commonMethods.getParks(groupId);
				}
				if(comList != null && !comList.isEmpty()){
					String preParams = "";
					for(Object parkid : comList){
						if(preParams.equals(""))
							preParams ="?";
						else
							preParams += ",?";
					}
					params.addAll(comList);
					params.add(0);
					params.add(carNumber);
					List<Map<String, Object>> escList = pService.getAllMap("select order_id as orderid,create_time as start," +
							"end_time as end,car_number,total,prepay,berthseg_id as berthsec_id from no_payment_tb where comid " +
							" in ("+preParams+") and state=? and car_number=? ", params);
					if(escList != null && !escList.isEmpty()){
						for(Map<String, Object>map : escList){
							Long in_time = (Long)map.get("start");
							Long out_time = (Long)map.get("end");
							map.put("prepay", StringUtils.formatDouble(map.get("prepay")));
							map.put("duartion", StringUtils.getTimeStringSenconds(in_time, out_time));
							map.put("in_time", TimeTools.getTime_yyyyMMdd_HHmmss(in_time));
							map.put("out_time", TimeTools.getTime_yyyyMMdd_HHmmss(out_time));
							map.put("total", map.get("total"));
						}
						getBerthSegInfo(escList);
						putPicUrls(escList);
						orders = StringUtils.createJson(escList);
						ret="0";
						errmsg="";
					}
					
					if(version >= 1390){//2016-12-14�����
						boolean isInpark = commonMethods.isInparkInCity(groupId);
						logger.error("isInpark�Ƿ�ͬһ���ƿɷ��ڳ������ظ��볡:"+isInpark);
						if(!isInpark){//�������볡��ʱ��Ҫ����������г���
							Group group = pService.getPOJO("select cityid from org_group_tb where id=? and cityid>? ",
									new Object[]{groupId, 0}, Group.class);
							comList = commonMethods.getparks(group.getCityid());
						} else {
							comList = commonMethods.getParks(groupId);
						}
						preParams  = "";
						for(Object parkid : comList){
							if(preParams.equals(""))
								preParams ="?";
							else
								preParams += ",?";
						}
						params.clear();
						params.add(carNumber);
						params.add(0);
						params.add(0);
						params.addAll(comList);
						params.add(comId);
						params.add(1);
						Order order = daService.getPOJO("select comid,berthsec_id from order_tb where car_number=? and state=? and ishd=? " +
								" and comid in ("+preParams+") and comid <>? limit ? ", params, Order.class);
						if(order != null){
							errmsg = "�ó�����";
							Long parkId = order.getComid();
							Long berthSegId = order.getBerthsec_id();
							Map<String, Object> map = pService.getMap("select company_name from com_info_tb where id=? ",
									new Object[]{parkId});
							if(map != null && map.get("company_name") != null){
								errmsg += "ͣ������" + map.get("company_name");
							}
							if(berthSegId > 0){
								BerthSeg berthSeg = pService.getPOJO("select berthsec_name from com_berthsecs_tb where id=? ",
										new Object[]{berthSegId}, BerthSeg.class);
								if(berthSeg != null){
									errmsg += "����λ�Σ�"+berthSeg.getBerthsec_name();
								}
							}
							errmsg += "��δ���㶩����";
							Long count = pService.getLong("select count(s.id) from collector_set_tb s,user_info_tb u where " +
									" s.role_id=u.role_id and s.is_duplicate_order=? and u.id=? ", new Object[]{0, uin});
							logger.error("count:"+count);
							if(count > 0||!isInpark){
								ret = "-3";//ͬһ�����ڲ�ͬ���������ظ������ڳ�����
							} else {
								ret = "-2";
							}
						}
					}
					Long count = daService.getLong("select count(id) from order_tb where car_number=? and state=? and ishd=? " +
							" and comid=? ", new Object[]{carNumber, 0, 0, comId});
					if(count > 0){
						ret = "-1";
						errmsg = "�������볡��";
					}
				}
				int ismonth = 0;
				if(commonMethods.isMonthUser(carNumber, comId))
					ismonth = 1;
				Long orderId = -1L;
				if(!ret.equals("-1")){
					orderId = daService.getkey("seq_order_tb");
				}
				String result = "{\"result\":\""+ret+"\",\"errmsg\":\""+errmsg+"\",\"orderid\":\""+orderId+"\",\"ismonthuser\":\""+ismonth+"\",\"orders\":"+orders+"}"; 
			//	logger.error("queryEscOrder result :"+result);
				return result;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "{\"result\":\"-1\",\"errmsg\":\"ϵͳ����!\"}";
	}
	
	private void getBerthSegInfo(List<Map<String, Object>> list){
		if(list != null && !list.isEmpty()){
			for(Map<String, Object> map : list){
				Long berthsec_id = (Long)map.get("berthsec_id");
				if(berthsec_id != null && berthsec_id > 0){
					BerthSeg berthSeg = pService.getPOJO("select berthsec_name from com_berthsecs_tb where id =?",
							new Object[]{berthsec_id}, BerthSeg.class);
					if(berthSeg != null && berthSeg.getBerthsec_name() != null){
						map.put("berthsec_name", berthSeg.getBerthsec_name());
					}
				}
			}
		}
	}
	
	private void putPicUrls(List<Map<String, Object>> list){
		try {
			if(list != null && !list.isEmpty()){
				for(Map<String, Object> map : list){
					if(map.get("orderid") != null){
						Long orderId = (Long)map.get("orderid");
						BasicDBObject conditions = new BasicDBObject();
						conditions.put("orderid", orderId);
						conditions.put("type", 2);
						List<String> urls = mongoDbUtils.getPicUrls("car_inout_pics", conditions);
						if(urls != null && !urls.isEmpty()){
							String urlStr = "";
							for(String url : urls){
								url = "carpicsup.do?action=getpicbyname&filename="+url;
								if("".equals(urlStr)){
									urlStr += url;
								}else{
									urlStr += "," + url;
								}
							}
							map.put("picurls", urlStr);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error(e);
		}
	}

	private String uporderpic(HttpServletRequest request)throws Exception {
		Long id = RequestUtil.getLong(request, "orderid", -1L);
		Integer type = RequestUtil.getInteger(request, "type", 0);//0�볡��Ƭ��1������Ƭ 2׷��
		logger.error(">>>>�ϴ�������������Ƭ....orderid:"+id+",type:"+type);
		Map<String, Object> paramsMap = new HashMap<String, Object>();
		paramsMap.put("orderid", id);
		paramsMap.put("type", type);
		paramsMap.put("ctime", TimeTools.gettime1());
		String picurl  =publicMethods.uploadPic(request, paramsMap, "car_inout_pics");
		logger.error(">>>>�ϴ�������������Ƭ....ͼƬ���ƣ�"+picurl+",mongodb table:car_inout_pics");
		int ret =0;
		if(!"-1".equals(picurl)){
			ret=1;
		}
		return "{\"result\":\""+ret+"\"}";
	}
	private String workOut(HttpServletRequest request,Long uid) {
		try {
			Long ntime = System.currentTimeMillis()/1000;
			Long bid = RequestUtil.getLong(request, "berthid", -1L);
			Long workId = RequestUtil.getLong(request, "workid", -1L);
			Integer from = RequestUtil.getInteger(request, "from", 0);//��Դ 0��ǩ�� 1���鿴�շѻ���
			Integer version = RequestUtil.getInteger(request, "version", -1);//�汾��
			logger.error("workout �˳���λ��:"+bid+",workid:"+workId+",from:"+from+",version:"+version);
			if(from == 0){
				int result = signOutValid(request, uid);
				if(result < 0){
					return  "{\"result\":\"-2\",\"errmsg\":\"�������\"}";
				}
				List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
				//���¹������е�ָ���Ĳ�λ����ǩ��
				Map<String, Object> workBerthSegSqlMap = new HashMap<String, Object>();
				workBerthSegSqlMap.put("sql", "update work_berthsec_tb set state=? where berthsec_id=? and is_delete =? ");
				workBerthSegSqlMap.put("values", new Object[]{0, bid, 0});
				bathSql.add(workBerthSegSqlMap);
				//ǩ�˲���
				
				boolean off = commonMethods.checkWorkTime(uid, ntime);
				int logoff_state = 0;
				if(!off){
					logoff_state = 1;
				}
				logger.error("logoff_state:"+logoff_state);
				Map<String, Object> workRecordSqlMap = new HashMap<String, Object>();
				workRecordSqlMap.put("sql", "update parkuser_work_record_tb set end_time=?,state=?,logoff_state=? where id =? ");
				workRecordSqlMap.put("values", new Object[]{ntime, 1, logoff_state, workId});
				bathSql.add(workRecordSqlMap);
				//��Ϊ����
				Map<String, Object> onlineSqlMap = new HashMap<String, Object>();
				onlineSqlMap.put("sql", "update user_info_tb set online_flag=? where id=? ");
				onlineSqlMap.put("values", new Object[]{21, uid});
				bathSql.add(onlineSqlMap);
				boolean b = daService.bathUpdate2(bathSql);
				logger.error("workout �˳���λ��:"+bid+",workid:"+workId+",from:"+from+",b:"+b);
			}else if(from == 1){
				int result = validate(request, uid);
				if(result < 0){
					return  "{\"result\":\"-2\",\"errmsg\":\"�������\"}";
				}
			}
			String result = "";
			if(version >= 1370){
				StatsWorkRecordResp resp = getIncome(uid, workId, ntime, from);
				result = JSONObject.fromObject(resp).toString();
			}else{//�ϰ汾
				Map<String, Object> infoMap = getIncomeOld(uid, workId, ntime);
				result = StringUtils.createJson(infoMap);
			}
			logger.error("work out result:"+result);
			return result;
		} catch (Exception e) {
			logger.error(e);
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> getIncomeOld(Long uid, Long workId, Long curTime){
		Map<String, Object> infoMap = new HashMap<String, Object>();
		try {
			infoMap.put("result",1);
			//��ӡǩ��СƱ
			//�ϸ�ʱ���ڳ�������Ԥ�ս��
			Map<String, Object> workMap = daService.getMap("select history_money,start_time from" +
					" parkuser_work_record_tb where id=? ", new Object[]{workId});
			Long outcar = 0L;
			Long incar = 0L;
			Map<String, Object> inMap = daService.getMap("select count(id) ocount from order_tb" +
					" where id in (select orderid from work_detail_tb where workid=? ) and uid=? ", new Object[]{workId, uid});
			if(inMap != null && inMap.get("ocount") != null){
				incar = (Long)inMap.get("ocount");
			}
			Map<String, Object> outMap = daService.getMap("select count(id) ocount from order_tb" +
					" where id in (select orderid from work_detail_tb where workid=? ) and out_uid=? ", new Object[]{workId, uid});
			if(outMap != null && outMap.get("ocount") != null){
				outcar = (Long)outMap.get("ocount");
			}
			Long startTime = (Long)workMap.get("start_time");
			Long endTime = curTime;
			infoMap.put("onwork_time", TimeTools.getTime_yyMMdd_HHmm(startTime*1000));
			infoMap.put("outwork_time", TimeTools.getTime_yyMMdd_HHmm(endTime*1000));
			infoMap.put("incar",incar);//��������
			infoMap.put("outcar", outcar);//��������
			StatsReq req = new StatsReq();
			List<Object> idList = new ArrayList<Object>();
			idList.add(uid);
			req.setIdList(idList);
			req.setStartTime(startTime);
			req.setEndTime(endTime);
			StatsFacadeResp resp = accountFacade.statsParkUserAccount(req);
			if(resp.getResult() == 1){
				List<StatsAccountClass> classes = resp.getClasses();
				StatsAccountClass accountClass = classes.get(0);
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
				
				//��Ƭͳ��
				double cardChargeCashFee = accountClass.getCardChargeCashFee();//��Ƭ��ֵ���
				double cardReturnFee = accountClass.getCardReturnFee();//�˿��˻����
				double cardActFee = accountClass.getCardActFee();//�������
				long cardActCount = accountClass.getCardActCount();//���Ƭ����
				
				double cash = StringUtils.formatDouble(cashParkingFee + cashPursueFee + cashPrepayFee + cashAddFee);
				double epay = StringUtils.formatDouble(ePayParkingFee + ePayPursueFee + ePayPrepayFee + ePayAddFee);
				double card = StringUtils.formatDouble(cardParkingFee + cardPursueFee + cardPrepayFee + cardAddFee);
				double refund = StringUtils.formatDouble(cashRefundFee + ePayRefundFee + cardRefundFee);
				double pursue = StringUtils.formatDouble(cashPursueFee + ePayPursueFee + cardPursueFee);
				double prepay = StringUtils.formatDouble(cashPrepayFee + ePayPrepayFee + cardPrepayFee);
				double totalCash = StringUtils.formatDouble(cash - cashRefundFee);
				double receTotal = StringUtils.formatDouble(cash + epay + card - refund + escapeFee);
				infoMap.put("total_fee", cash);//�ֽ����շ�
				infoMap.put("epay", epay);//�������շ�
				infoMap.put("card_pay", card);//ˢ��֧��
				infoMap.put("history_prepay", refund);//�渶
				infoMap.put("upmoney", totalCash);//�Ͻ�
				infoMap.put("prepay_cash", cashPrepayFee);//�ֽ�Ԥ֧��
				infoMap.put("prepay_epay", ePayPrepayFee);//����Ԥ֧��
				infoMap.put("prepay", prepay);//��Ԥ֧��
				infoMap.put("pursue_cash", cashPursueFee);//�ֽ�׷��
				infoMap.put("pursue_epay", ePayPursueFee);//����׷��
				infoMap.put("pursue", pursue);//��׷��
				infoMap.put("rece_fee", receTotal);//Ӧ�ս�ʵ��+δ�ɣ�
				infoMap.put("act_card_count", cardActCount);
				infoMap.put("act_card_fee", cardActFee);
				infoMap.put("charge_card", cardChargeCashFee);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return infoMap;
	}
	
	private StatsWorkRecordResp getIncome(Long uid, Long workId, Long curTime, 
			Integer from){
		StatsWorkRecordResp statsWorkRecordResp = new StatsWorkRecordResp();
		try {
			int is_show_card = 0;//�Ƿ����շѻ��ܺʹ�ӡСƱ����ʾ������Ƭ�����ݣ���Щ��Ӫ����û�п�Ƭ�� 0����ʾ 1������ʾ
			CollectorSetting setting = pService.getPOJO("select s.is_show_card from collector_set_tb s,user_info_tb u " +
					" where s.role_id=u.role_id and u.id=? order by s.id desc limit ? ",
					new Object[]{uid, 1}, CollectorSetting.class);
			if(setting != null){
				is_show_card = setting.getIs_show_card();
			}
			//��ӡǩ��СƱ
			WorkRecord workRecord = pService.getPOJO("select start_time from parkuser_work_record_tb " +
					" where id=? ", new Object[]{workId}, WorkRecord.class);
			if(workRecord != null){
				List<StatsWorkRecordInfo> infos = new ArrayList<StatsWorkRecordInfo>();
				statsWorkRecordResp.setResult(1);
				StatsWorkRecordInfo info1 = new StatsWorkRecordInfo();
				Long startTime = workRecord.getStart_time();//ǩ��ʱ��
				info1.setName("ǩ��ʱ�䣺");
				info1.setValue(TimeTools.getTime_yyMMdd_HHmm(startTime * 1000));
				infos.add(info1);
				String curName = "����ʱ�䣺";
				if(from == 0){
					curName = "ǩ��ʱ�䣺";
				}
				StatsWorkRecordInfo info2 = new StatsWorkRecordInfo();
				info2.setName(curName);
				info2.setValue(TimeTools.getTime_yyMMdd_HHmm(curTime * 1000));
				infos.add(info2);
				//-------------------------------����������-------------------------//
				int inCar = commonMethods.getInOutCar(workId, 0);
				int outCar = commonMethods.getInOutCar(workId, 1);
				StatsWorkRecordInfo info3 = new StatsWorkRecordInfo();
				info3.setName("����������");
				info3.setValue(inCar);
				infos.add(info3);
				StatsWorkRecordInfo info4 = new StatsWorkRecordInfo();
				info4.setName("����������");
				info4.setValue(outCar);
				infos.add(info4);
				//-------------------------------ͳ�ƽ��-------------------------//
				StatsReq req = new StatsReq();
				List<Object> idList = new ArrayList<Object>();
				idList.add(uid);
				req.setIdList(idList);
				req.setStartTime(startTime);
				req.setEndTime(curTime);
				StatsFacadeResp resp = accountFacade.statsParkUserAccount(req);
				if(resp.getResult() == 1){
					List<StatsAccountClass> classes = resp.getClasses();
					StatsAccountClass accountClass = classes.get(0);
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
					
					//��Ƭͳ��
					double cardChargeCashFee = accountClass.getCardChargeCashFee();//��Ƭ��ֵ���
					double cardActFee = accountClass.getCardActFee();//�������
					long cardActCount = accountClass.getCardActCount();//���Ƭ����
					
					double cashCustomFee = StringUtils.formatDouble(cashParkingFee + cashPrepayFee + 
							cashAddFee - cashRefundFee);
					double epayCustomFee = StringUtils.formatDouble(ePayParkingFee + ePayPrepayFee + 
							ePayAddFee - ePayRefundFee);
					double cardCustomFee = StringUtils.formatDouble(cardParkingFee + cardPrepayFee + 
							cardAddFee - cardRefundFee);
					
					double cashTotalFee = StringUtils.formatDouble(cashPursueFee + cashCustomFee);
					double cashTotal = StringUtils.formatDouble(cashTotalFee + cardChargeCashFee + cardActFee);//��Ҫ�Ͻɵ��ֽ���
					
					List<StatsWorkRecordInfo> cashInfos = new ArrayList<StatsWorkRecordInfo>();
					StatsWorkRecordInfo cashInfo1 = new StatsWorkRecordInfo();
					cashInfo1.setName("��ͨ������");
					cashInfo1.setValue(cashCustomFee + " Ԫ");
					cashInfos.add(cashInfo1);
					StatsWorkRecordInfo cashInfo2 = new StatsWorkRecordInfo();
					cashInfo2.setName("׷�ɶ�����");
					cashInfo2.setValue(cashPursueFee + " Ԫ");
					cashInfos.add(cashInfo2);
					StatsWorkRecordInfo info5 = new StatsWorkRecordInfo();
					info5.setName("ͣ����-�ֽ�֧��");
					info5.setValue(cashInfos);
					infos.add(info5);
					
					List<StatsWorkRecordInfo> epayInfos = new ArrayList<StatsWorkRecordInfo>();
					StatsWorkRecordInfo epayInfo1 = new StatsWorkRecordInfo();
					epayInfo1.setName("��ͨ������");
					epayInfo1.setValue(epayCustomFee + " Ԫ");
					epayInfos.add(epayInfo1);
					StatsWorkRecordInfo epayInfo2 = new StatsWorkRecordInfo();
					epayInfo2.setName("׷�ɶ�����");
					epayInfo2.setValue(ePayPursueFee + " Ԫ");
					epayInfos.add(epayInfo2);
					StatsWorkRecordInfo info6 = new StatsWorkRecordInfo();
					info6.setName("ͣ����-����֧��");
					info6.setValue(epayInfos);
					infos.add(info6);
					if(is_show_card == 0){
						List<StatsWorkRecordInfo> cardInfos = new ArrayList<StatsWorkRecordInfo>();
						StatsWorkRecordInfo cardInfo1 = new StatsWorkRecordInfo();
						cardInfo1.setName("��ͨ������");
						cardInfo1.setValue(cardCustomFee + " Ԫ");
						cardInfos.add(cardInfo1);
						StatsWorkRecordInfo cardInfo2 = new StatsWorkRecordInfo();
						cardInfo2.setName("׷�ɶ�����");
						cardInfo2.setValue(cardPursueFee + " Ԫ");
						cardInfos.add(cardInfo2);
						StatsWorkRecordInfo info7 = new StatsWorkRecordInfo();
						info7.setName("ͣ����-��Ƭ֧��");
						info7.setValue(cardInfos);
						infos.add(info7);
						
						List<StatsWorkRecordInfo> cards = new ArrayList<StatsWorkRecordInfo>();
						StatsWorkRecordInfo card1 = new StatsWorkRecordInfo();
						card1.setName("�ֽ��ֵ��");
						card1.setValue(cardChargeCashFee + " Ԫ");
						cards.add(card1);
						StatsWorkRecordInfo card2 = new StatsWorkRecordInfo();
						card2.setName("�ۿ�������");
						card2.setValue(cardActCount + " ��");
						cards.add(card2);
						StatsWorkRecordInfo card3 = new StatsWorkRecordInfo();
						card3.setName("�ۿ�����ֵ��");
						card3.setValue(cardActFee + " Ԫ");
						cards.add(card3);
						StatsWorkRecordInfo info8 = new StatsWorkRecordInfo();
						info8.setName("��Ƭ");
						info8.setValue(cards);
						infos.add(info8);
					}
					StatsWorkRecordInfo info10 = new StatsWorkRecordInfo();
					info10.setName("�ӵ���");
					info10.setValue(escapeFee + " Ԫ");
					info10.setFontColor("#FF0000");
					info10.setFontSize(16);
					infos.add(info10);
					StatsWorkRecordInfo info9 = new StatsWorkRecordInfo();
					info9.setName("�Ͻɽ�");
					info9.setValue(cashTotal + " Ԫ");
					info9.setFontColor("#FF0000");
					info9.setFontSize(16);
					infos.add(info9);
				}
				statsWorkRecordResp.setInfos(infos);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return statsWorkRecordResp;
	}

	private String getBerths(HttpServletRequest request, Long uid, Long oldParkId, String token, long groupId) {
		//http://127.0.0.1/zld/collectorrequest.do?action=getberths&token=522b6bc2abd903eacf6b9a4ae3359815&berthid=15&devicecode=357143047019192
		try {
			Long ntime = System.currentTimeMillis()/1000;
			Long berthSegId = RequestUtil.getLong(request, "berthid", -1L);
			String device_code = RequestUtil.getString(request, "devicecode");
			//logger.error(berthSegId+","+device_code);
			if(berthSegId > 0){
				Map<String, Object> deviceMap = pService.getMap("select device_auth from mobile_tb where device_code=? ",
						new Object[]{device_code});
				//logger.error("deviceMap:"+deviceMap);
				if(deviceMap != null&&!deviceMap.isEmpty()){
					int device_auth = (Integer)deviceMap.get("device_auth");
					//logger.error("device_auth:"+device_auth);
					if(device_auth == 1){
						BerthSeg berthSeg = pService.getPOJO("select comid from com_berthsecs_tb where id =? ",
								new Object[]{berthSegId}, BerthSeg.class);
						long parkId = berthSeg.getComid();
						logger.error("berthsecs_id:"+berthSegId+",parkId:"+parkId+",old comid:"+oldParkId);
						updateSession(parkId, oldParkId, token);
						//�鲴λ�ϵĶ���������
						List<Map<String, Object>> berths = pService.getAll("select id,cid as ber_name,order_id,state,dici_id from com_park_tb " +
								" where is_delete=? and berthsec_id=? order by cid ", new Object[]{0, berthSegId});
						//------------------------���̲߳��в�ѯ------------------------//
						ExecutorService pool = ExecutorsUtil.getExecutorService();
						ExeCallable callable0 = new ExeCallable(berths, groupId, 0);//��������
						ExeCallable callable1 = new ExeCallable(berths, groupId, 1);//������״̬
						ExeCallable callable2 = new ExeCallable(parkId, 2);//��������
						ExeCallable callable3 = new ExeCallable(parkId, 3);//��������
						ExeCallable callable4 = new ExeCallable(berthSegId, uid, ntime, device_code, 4);//ǩ��
						
						Future<Object> future0 = pool.submit(callable0);
						Future<Object> future1 = pool.submit(callable1);
						Future<Object> future2 = pool.submit(callable2);
						Future<Object> future3 = pool.submit(callable3);
						Future<Object> future4 = pool.submit(callable4);
						
						future0.get();
						future1.get();
						String parkName = (String)future2.get();
						String carType = (String)future3.get();
						Map<String, Object> signMap = (Map<String, Object>)future4.get();
						if(signMap != null){
							Long workId = (Long)signMap.get("workId");
							if(workId != null && workId > 0){
								String errmsg = (String)signMap.get("errmsg");
								logger.error("ǩ���ɹ�");
								return "{\"state\":\"1\",\"workid\":\"" + workId + "\",\"comid\":\"" + parkId + "\",\"cname\":\"" + parkName + "\",\"errmsg\":\""+errmsg+"\",\"data\":"+StringUtils.createJson(berths)+",\"car_type\":"+carType+"}";
							}
						}
						logger.error("ǩ��ʧ��");
						return "{\"state\":\"-1\",\"workid\":\"-1\",\"comid\":\"" + parkId + "\",\"cname\":\"" + parkName + "\",\"errmsg\":\"ǩ��ʧ�ܣ����Ժ����ԣ�\",\"data\":[],\"car_type\":"+carType+"}";
					}
					logger.error("ǩ��ʧ��");
					return "{\"state\":\"0\",\"workid\":\"-2\",\"comid\":\"-1\",\"cname\":\"\",\"errmsg\":\"�豸δ��ˣ�����ϵ����Ա\",\"data\":[],\"car_type\":[]}";
				}
				logger.error("ǩ��ʧ��");
				return "{\"state\":\"0\",\"workid\":\"-1\",\"comid\":\"-1\",\"cname\":\"\",\"errmsg\":\"�豸δע�ᣬ����ϵ����Ա\",\"data\":[],\"car_type\":[]}";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "{}";
	}
	
	/**
	 * ǩ��
	 * @param berthSegId
	 * @param ntime
	 * @param uid
	 * @param device_code
	 * @return
	 */
	private Map<String, Object> signIn(long berthSegId, long ntime, long uid, String device_code){
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			WorkRecord workRecord = daService.getPOJO("select id,start_time from parkuser_work_record_tb where uid=? " +
					" and berthsec_id=? and state=? order by id desc limit ? ", 
					new Object[]{uid, berthSegId, 0, 1}, WorkRecord.class);
			if(workRecord == null){
				boolean logon = commonMethods.checkWorkTime(uid, ntime);
				int logon_state = 0;
				if(!logon){
					logon_state = 1;
				}
				logger.error("logon_state:"+logon_state);
				List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
				Map<String, Object> workRecordSqlMap = new HashMap<String, Object>();
				Long workId  = daService.getkey("seq_parkuser_work_record_tb");
				workRecordSqlMap.put("sql", "insert into parkuser_work_record_tb (id,berthsec_id,start_time,uid,state,device_code,logon_state) " +
						" values(?,?,?,?,?,?,?)");
				workRecordSqlMap.put("values", new Object[]{workId, berthSegId, ntime, uid, 0, device_code, logon_state});
				bathSql.add(workRecordSqlMap);
				
				Map<String, Object> workBerthSegSqlMap = new HashMap<String, Object>();
				workBerthSegSqlMap.put("sql", "update work_berthsec_tb set state=? where berthsec_id=? and is_delete=? ");
				workBerthSegSqlMap.put("values", new Object[]{1, berthSegId, 0});
				bathSql.add(workBerthSegSqlMap);
				
				Map<String, Object> onlineSqlMap = new HashMap<String, Object>();
				onlineSqlMap.put("sql", "update user_info_tb set online_flag=? where id=? ");
				onlineSqlMap.put("values", new Object[]{22, uid});
				bathSql.add(onlineSqlMap);
				
				boolean b = daService.bathUpdate2(bathSql);
				logger.error("b:"+b);
				if(b){
					map.put("workId", workId);
					map.put("errmsg", "");
					return map;
				}
				map.put("errmsg", "ǩ��ʧ�ܣ����Ժ����ԣ�");
				return map;
			}else {
				Long startTime = workRecord.getStart_time();
				map.put("workId", workRecord.getId());
				map.put("errmsg", "��ǩ����ʱ�䣺" + TimeTools.getTime_MMdd_HHmm(startTime * 1000));
				return map;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}
	
	/**
	 * ���µĳ�����Ÿ��½�session��
	 * @param newParkId
	 * @param oldParkId
	 * @param token
	 */
	private void updateSession(long newParkId, long oldParkId, String token){
		//��comid���µ�user_session_tb �ͻ����С�����
		try {
			if(newParkId != oldParkId){
				int r = daService.update("update user_session_tb set comid=? where token=? ",
						new Object[]{newParkId, token});
				logger.error("r:"+r);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * ��ȡ�����ĳ�������
	 * @param parkId
	 * @return
	 */
	private String getCarTypeInfo(long parkId){
		try {
			if(parkId > 0){
				long count = pService.getLong("select count(id) from com_info_tb where id=? and car_type=? ",
						new Object[]{parkId, 1});
				if(count > 0){
					List<Map<String, Object>> carTypeList = pService.getAll("select id,name from car_type_tb " +
							" where comid=? order by sort ", new Object[]{parkId});
					if(carTypeList != null && !carTypeList.isEmpty()){
						return StringUtils.createJson(carTypeList);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "[]";
	}
	
	/**
	 * ��ȡ��λ�ϵĶ�����Ϣ
	 * @param list
	 * @param groupId
	 */
	private void getOrderInfo(List<Map<String, Object>> list, long groupId){
		try {
			if(list != null && !list.isEmpty()){
				List<Object> params = new ArrayList<Object>();
				String preParam = "";
				for(Map<String, Object> berth : list){
					berth.put("car_number", "");//�ͻ���Ҫ��ʹû�ж�����ҲҪ����car_numnber�ֶ�
					Integer state = (Integer)berth.get("state");
					Long orderId = (Long)berth.get("order_id");
					if(orderId != null && orderId > 0 && state == 1){
						params.add(orderId);
						if("".equals(preParam)){
							preParam = "?";
						}else{
							preParam += ",?";
						}
					}
				}
				if(!params.isEmpty()){
					params.add(0);
					List<Map<String, Object>> orders = pService.getAllMap("select id,car_number,prepaid,c_type,nfc_uuid from order_tb " +
							" where id in ("+preParam+") and state=? ", params);
					if(orders != null && !orders.isEmpty()){
						boolean showCardUser = false;
						String group_card_user = CustomDefind.GROUP_CARD_USER;
						if(group_card_user != null){
							String[] groupIds = group_card_user.split("\\|");
							for(int i = 0; i<groupIds.length; i++){
								long gId = Long.valueOf(groupIds[i]);
								if(gId == groupId){
									showCardUser = true;
									break;
								}
							}
						}
						logger.error("showCardUser:"+showCardUser);
						for(Map<String, Object> map : orders){
							Long id = (Long)map.get("id");
							for(Map<String, Object> map2 : list){
								Long orderId = (Long)map2.get("order_id");
								if(orderId != null){
									if(id.intValue() == orderId.intValue()){
										String car_number = (String)map.get("car_number");
										map2.put("orderid", id);
										map2.put("car_number", car_number);
										map2.put("prepay", map.get("prepaid"));
										map2.put("ismonthuser", map.get("c_type"));
										
										if(showCardUser){
											boolean is_card = commonMethods.cardUser(car_number, groupId);
											if(is_card){
												map2.put("is_card", 1);
											}else{
												map2.put("is_card", 0);
											}
										}
										break;
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * ��ȡ��λ�ϵĳ���������״̬
	 * @param list
	 */
	private void getSensorInfo(List<Map<String, Object>> list){
		try {
			if(list != null && !list.isEmpty()){
				List<Object> params = new ArrayList<Object>();
				String preParams = "";
				for(Map<String, Object> map : list){
					if(map.get("dici_id") != null){
						params.add(map.get("dici_id"));
						if("".equals(preParams)){
							preParams = "?";
						}else{
							preParams += ",?";
						}
					}
				}
				if(params != null && !params.isEmpty()){
					params.add(0);
					List<Map<String, Object>> sensorList = pService.getAllMap("select id,state from dici_tb where " +
							" id in ("+preParams+") and is_delete=? ", params);
					if(sensorList != null && !sensorList.isEmpty()){
						for(Map<String, Object> map : sensorList){
							Long id = (Long)map.get("id");
							for(Map<String, Object> map2 : list){
								if(map2.get("dici_id") != null){
									Integer sensorId = (Integer)map2.get("dici_id");
									if(id.intValue() == sensorId.intValue()){
										map2.put("sensor_state", map.get("state"));
										break;
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("getSensorInfo>>>",e);
		}
	}

	private String liftRodReason(HttpServletRequest request) {
		Integer reason = RequestUtil.getInteger(request, "reason", -1);
		Long lrid = RequestUtil.getLong(request, "lrid", -1L);
		if(lrid==-1){
			return  "{result:-1,errmsg:\"�������Ϊ�գ�\"}";
		}
		String sql = "update lift_rod_tb set reason=? where id=?";
		int ret = daService.update(sql, new Object[]{reason,lrid});
		logger.error(">>>>>>>>>>lrid:"+lrid+",reason:"+reason+",update lift_rod_tb,ret:"+ret);
		return  "{result:\""+ret+"\",errmsg:\"�����ɹ���\"}";
	}

	private String liftRod(HttpServletRequest request, Long uin, Long comId) {
		Long ntime = System.currentTimeMillis()/1000;
		Long key = daService.getkey("seq_lift_rod_tb");
		Long pass_id = RequestUtil.getLong(request, "passid", -1L);//���ͨ��id
		Integer reason = RequestUtil.getInteger(request, "reason", -2);//ԭ��δѡ 
		String sql = "insert into lift_rod_tb (id,comid,uin,ctime,pass_id,reason) values(?,?,?,?,?,?)";
		int ret = daService.update(sql, new Object[]{key,comId,uin,ntime,pass_id,reason});
		logger.error(">>>>>>>>>>"+comId+","+uin+",upload lift rod,insert into db ret:"+ret);
		if(ret==1)
			return  "{\"result\":\""+ret+"\",\"errmsg\":\"�����ɹ���\",lrid:\""+key+"\"}";
		else {
			return  "{\"result\":\""+ret+"\",\"errmsg\":\"����ʧ�ܣ�\"}";
		}
	}

	//�ϴ�̧�˼�¼
	private String liftRodPic(HttpServletRequest request) throws Exception{
		Long ntime = System.currentTimeMillis()/1000;
		Long lrid = RequestUtil.getLong(request, "lrid", -1L);
		logger.error("begin upload lift rod picture....lrid:"+lrid);
		Map<String, String> extMap = new HashMap<String, String>();
	    extMap.put(".jpg", "image/jpeg");
	    extMap.put(".jpeg", "image/jpeg");
	    extMap.put(".png", "image/png");
	    extMap.put(".gif", "image/gif");
	    extMap.put(".webp", "image/webp");
		if(lrid==-1){
			return  "{\"result\":\"-1\",\"errmsg\":\"�������Ϊ�գ�\"}";
		}
		request.setCharacterEncoding("UTF-8"); // ���ô�����������ı����ʽ
		DiskFileItemFactory  factory = new DiskFileItemFactory(); // ����FileItemFactory����
		factory.setSizeThreshold(16*4096*1024);
		ServletFileUpload upload = new ServletFileUpload(factory);
		// �������󣬲��õ��ϴ��ļ���FileItem����
		upload.setSizeMax(16*4096*1024);
		List<FileItem> items = null;
		try {
			items =upload.parseRequest(request);
		} catch (FileUploadException e) {
			e.printStackTrace();
			return "{\"result\":\"1\",\"errmsg\":\"���Ʊ���ɹ���\"}";
		}
		String filename = ""; // �ϴ��ļ����浽���������ļ���
		InputStream is = null; // ��ǰ�ϴ��ļ���InputStream����
		// ѭ�������ϴ��ļ�
		for (FileItem item : items){
			// ������ͨ�ı���
			if (!item.isFormField()){
				// �ӿͻ��˷��͹������ϴ��ļ�·���н�ȡ�ļ���
				filename = item.getName().substring(item.getName().lastIndexOf("\\")+1);
				is = item.getInputStream(); // �õ��ϴ��ļ���InputStream����
				logger.error("filename:"+item.getName()+",stream:"+is);
			}else{
				continue;
			}
			String file_ext =filename.substring(filename.lastIndexOf(".")).toLowerCase();// ��չ��
			String picurl = lrid + "_"+ ntime + file_ext;
			BufferedInputStream in = null;  
			ByteArrayOutputStream byteout =null;
			try {
				in = new BufferedInputStream(is);   
				byteout = new ByteArrayOutputStream(1024);        	       
				
				byte[] temp = new byte[1024];        
				int bytesize = 0;        
				while ((bytesize = in.read(temp)) != -1) {        
					byteout.write(temp, 0, bytesize);        
				}        
				
				byte[] content = byteout.toByteArray(); 
				DB mydb = MongoClientFactory.getInstance().getMongoDBBuilder("zld");
				mydb.requestStart();
				
				DBCollection collection = mydb.getCollection("lift_rod_pics");
				//  DBCollection collection = mydb.getCollection("records_test");
				
				BasicDBObject document = new BasicDBObject();
				document.put("lrid", lrid);
				document.put("ctime", ntime);
				document.put("type", extMap.get(file_ext));
				document.put("content", content);
				document.put("filename", picurl);
				//��ʼ����
				//��������
				mydb.requestStart();
				collection.insert(document);
				//��������
				mydb.requestDone();
				in.close();        
				is.close();
				byteout.close();
				String sql = "update lift_rod_tb set img=? where id =?";
				int ret = daService.update(sql, new Object[]{picurl,lrid});
				logger.error(">>>>>>>>>>orderId:"+lrid+",filename:"+picurl+", update lift_rod_tb, ret:"+ret);
			} catch (Exception e) {
				e.printStackTrace();
				return "{\"result\":\"0\",\"errmsg\":\"ͼƬ�ϴ�ʧ�ܣ�\"}";
			}finally{
				if(in!=null)
					in.close();
				if(byteout!=null)
					byteout.close();
				if(is!=null)
					is.close();
			}
		}
		return "{\"result\":\"1\",\"errmsg\":\"�ϴ��ɹ���\"}";
	}
	//�󶨳�λ������
	private String bondCarPark(HttpServletRequest request, Long comId) {
		Long ntime = System.currentTimeMillis()/1000;
		Long id = RequestUtil.getLong(request, "id", -1L);
		Long oid = RequestUtil.getLong(request, "orderid", -1L);
		long count = daService.getLong("select count(*) from order_tb where id = ? and state=?",
				new Object[]{oid, 1});
		int ret = 0;
		if(count==0){
			count = daService.getLong("select count(id) from com_park_tb where order_id>? " +
					" and state =? and id=? ", new Object[]{0, 1, id});
			if(count == 0){
				ret = daService.update("update com_park_tb set order_id =?,state =?,enter_time=? where id =?",
						new Object[]{oid,1,ntime,id});
				logger.error("(update com_park_tb orderid):"+oid+",id:"+id+",result:"+ret);
			}
		}
		return "{\"result\":\""+ret+"\"}";
	}
	//��ѯ���п��г�λ
	private String getFreeParks(HttpServletRequest request,Long comid) {
		//���³�λ���Ѿ�����Ķ�������Ϊδռ��״̬
		//commonMethods.updateParkInfo(comid);
		String result ="{\"result\":\"0\",\"errmsg\":\"δ���ó�λ��Ϣ\"}";
		List list = daService.getAll("select id,cid as name from com_park_tb where comid =? and state = ? ", new Object[]{comid,0});
		if(list!=null&&!list.isEmpty()){
			
			result ="{\"result\":\"1\",\"errmsg\":\"\",\"info\":"+StringUtils.createJson(list)+"}";
		}
		return result;
	}

	private String queryAccount(HttpServletRequest request, Long uin) {
		Long ntime = System.currentTimeMillis()/1000;
		String carnumber = AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "carnumber"));
		Integer pageNum = RequestUtil.getInteger(request, "page", 1);
		Integer pageSize = RequestUtil.getInteger(request, "size", 20);
		if(carnumber.equals("")){
			//AjaxUtil.ajaxOutput(response, "-1");
			return "-1";
		}
		Map<String, Object> carMap = pService.getMap(
				"select uin from car_info_tb where car_number=? ",
				new Object[] { carnumber });
		if(carMap == null || carMap.get("uin") == null){
			//AjaxUtil.ajaxOutput(response, "-1");
			return "-1";
		}
		List<Map<String, Object>> carList = pService
				.getAll("select car_number from car_info_tb where uin=? and state=? ",
						new Object[] { carMap.get("uin"), 1 });
		String cnum = "�ó�����"+carList.size()+"������:/n";
		for(int i = 0; i<carList.size(); i++){
			Map<String, Object> map = carList.get(i);
			if(i == 0){
				cnum += map.get("car_number");
			}else{
				cnum += "," + map.get("car_number");
			}
		}
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		List<Object> params = new ArrayList<Object>();
		String sql = "select a.*,o.car_number carnumber from parkuser_account_tb a,order_tb o where a.orderid=o.id and o.uid=? and o.uin=? and a.type=? and a.create_time between ? and ? order by a.create_time desc";
		String sqlcount = "select count(a.*) from parkuser_account_tb a,order_tb o where a.orderid=o.id and o.uid=? and o.uin=? and a.type=? and a.create_time between ? and ? ";
		params.add(uin);
		params.add(carMap.get("uin"));
		params.add(0);
		params.add(ntime - 30*24*60*60);
		params.add(ntime);
		Long count = pService.getCount(sqlcount, params);
		if(count > 0){
			list = pService.getAll(sql, params, pageNum, pageSize);
			setRemark(list);
		}
		String reslut =  "{\"count\":"+count+",\"carinfo\":\""+cnum+"\",\"info\":"+StringUtils.createJson(list)+"}";
		return reslut;
	}

	private String todayAccount(HttpServletRequest request, Long uin,
			Long comId, Map<String, Object> infoMap) {
		Long ntime = System.currentTimeMillis()/1000;
		Long b = TimeTools.getToDayBeginTime();
		Long e = ntime+60;
		Double parkmoney = 0d;
		Double parkusermoney = 0d;
//		Double cashmoney = 0d;
		Double rewardmoney = 0d;
		Double todayscore = 0d;//ʣ�����
		Long todayin = 0L;//�����볡����
		Long todayout = 0L;//���ճ�������
		//һ������   1:��������Ա    2�շ�Ա
		Map park = daService.getMap( "select sum(amount) total from park_account_tb where create_time between ? and ? " +
				" and type <> ? and uid=? and comid=? ",new Object[]{b,e,1,uin,comId});
		if(park!=null&&park.get("total")!=null){
			parkmoney = Double.valueOf(park.get("total")+"");//�����˻����루������Դ��
		}
		
		Map parkuser = daService.getMap( "select sum(amount) total from parkuser_account_tb where create_time between ? and ? " +
				" and type= ? and uin = ? ",new Object[]{b,e,0,uin});
		if(parkuser!=null&&parkuser.get("total")!=null){
			parkusermoney = Double.valueOf(parkuser.get("total")+"");//�շ�Ա�˻����루������Դ��
		}
		
		/*Map cash = daService.getMap( "select sum(amount) total from parkuser_cash_tb where create_time between ? and ? " +
				" and uin=? ",new Object[]{b,e,uin});
		if(cash!=null&&cash.get("total")!=null){
			cashmoney = Double.valueOf(cash.get("total")+"");//�շ�Ա�ֽ�����
		}*/
		
		Map reward = daService.getMap("select sum(money) total from parkuser_reward_tb where ctime between ? and ? and uid=? ",
						new Object[] { b, e, uin });
		if(reward != null && reward.get("total") != null){
			rewardmoney = Double.valueOf(reward.get("total") + "");
		}
		
		Map score = pService.getMap("select reward_score from user_info_tb where id=? ", new Object[] { uin });
		if(score != null && score.get("reward_score") != null){
			todayscore = Double.valueOf(score.get("reward_score") + "");
		}
		
		todayin = pService.getLong("select count(1) from order_tb where comid=? " +
				"and create_time between ? and ?", new Object[]{comId,b,e});
		
		todayout = pService.getLong("select count(1) from order_tb where comid=? and state=? " +
				"and end_time between ? and ?", new Object[]{comId,1,b,e});
		
		infoMap.put("mobilemoney", StringUtils.formatDouble(parkmoney + parkusermoney));
		infoMap.put("rewardmoney", StringUtils.formatDouble(rewardmoney));
		infoMap.put("todayscore", StringUtils.formatDouble(todayscore));
		infoMap.put("todayin", todayin);
		infoMap.put("todayout", todayout);
		//AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));
		return  StringUtils.createJson(infoMap);
	}

	private String deductScore(HttpServletRequest request, Long uin,
			Long comId, Map<String, Object> infoMap) {
		Long ntime = System.currentTimeMillis()/1000;
		Double score = RequestUtil.getDouble(request, "score", 0d);//���Ļ���
		Long ticketid = RequestUtil.getLong(request, "ticketid", -1L);
		logger.error("ticketid:"+ticketid+",score:"+ticketid+",uin:"+uin);
		if(score == 0 || ticketid == -1){
			infoMap.put("result", -1);
		}
		Map<String, Object> userMap = daService.getMap(
				"select id,nickname,reward_score from user_info_tb where id=? ",
				new Object[] { uin });
		Map<String, Object> comMap = daService.getMap("select company_name from com_info_tb where id=? ", new Object[]{comId});
		Double reward_score = Double.valueOf(userMap.get("reward_score") + "");
		if(reward_score < score){
			infoMap.put("result", -3);
		//	AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));//���ֲ���
			logger.error("deductscore>>>���ͻ��ֲ��㣬�շ�Ա:"+uin+",score:"+score+",reward_score:"+reward_score);
			return  StringUtils.createJson(infoMap);
		}
		List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
		
		Map<String, Object> scoreSqlMap = new HashMap<String, Object>();
		//������ϸ
		Map<String, Object> scoreAccountSqlMap = new HashMap<String, Object>();
		
		scoreAccountSqlMap.put("sql", "insert into reward_account_tb (uin,score,type,create_time,remark,target,ticket_id) values(?,?,?,?,?,?,?)");
		scoreAccountSqlMap.put("values", new Object[]{uin,score,1,ntime,"ͣ��ȯ �û�΢�ŵ����ȡ",2,ticketid});
		bathSql.add(scoreAccountSqlMap);
		
		scoreSqlMap.put("sql", "update user_info_tb set reward_score=reward_score-? where id=? ");
		scoreSqlMap.put("values", new Object[]{score, uin});
		bathSql.add(scoreSqlMap);
		
		boolean b = daService.bathUpdate(bathSql);
		logger.error("uin:"+uin+",b:"+b);
		if(b){
			infoMap.put("result", 1);
		}
		//AjaxUtil.ajaxOutput(response,StringUtils.createJson(infoMap));
		return StringUtils.createJson(infoMap);
	}

	private String sweepTicket(HttpServletRequest request, Long uin, Long comId) {
		Long ntime = System.currentTimeMillis()/1000;
		Double score = RequestUtil.getDouble(request, "score", 0d);//���Ļ���
		Integer bmoney = RequestUtil.getInteger(request, "bmoney", 0);//���
		Map<String, Object>  infoMap = new HashMap<String, Object>();
		Long ticketId = daService.getkey("seq_ticket_tb");
		logger.error("sweepticket>>>�շ�Ա��"+uin+",score:"+score+",bmoney:"+bmoney+",ticketId:"+ticketId);
		Map<String, Object> userMap = daService.getMap(
				"select id,nickname,reward_score from user_info_tb where id=? ",
				new Object[] { uin });
		Map<String, Object> comMap = daService.getMap("select company_name from com_info_tb where id=? ", new Object[]{comId});
		Double reward_score = Double.valueOf(userMap.get("reward_score") + "");
		if(reward_score < score){
			infoMap.put("result", -3);
			//AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));//���ֲ���
			logger.error("sendticket>>>���ͻ��ֲ��㣬�շ�Ա:"+uin+",score:"+score+",reward_score:"+reward_score);
			return StringUtils.createJson(infoMap);
		}
		Long ctime = ntime;
		String code = null;
		Long ticketids[] = new Long[]{ticketId};
		String []codes = StringUtils.getGRCode(ticketids);
		if(codes.length > 0){
			code = codes[0];
		}
		logger.error("sweepticket>>>�շ�Ա��"+uin+",code:"+code);
		List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
		//��ά��
		Map<String, Object> codeSqlMap = new HashMap<String, Object>();
		
		Map<String, Object> ticketSqlMap = new HashMap<String, Object>();
		
		codeSqlMap.put("sql", "insert into qr_code_tb(comid,uid,ctime,type,state,code,isuse,ticketid,score) values(?,?,?,?,?,?,?,?,?)");
		codeSqlMap.put("values", new Object[] { comId, uin, ctime, 6, 0, code, 1, ticketId, score });
		bathSql.add(codeSqlMap);
		
		ticketSqlMap.put("sql", "insert into ticket_tb(id,create_time,limit_day,money,state,comid,type) values(?,?,?,?,?,?,?)");
		ticketSqlMap.put("values", new Object[] {ticketId, TimeTools.getToDayBeginTime(),TimeTools.getToDayBeginTime()+16*24*60*60-1, bmoney, 0, comId, 1});
		bathSql.add(ticketSqlMap);
		
		boolean b = daService.bathUpdate(bathSql);
		logger.error("sweepticket>>>�շ�Ա��"+uin+",ticketId:"+ticketId+",code:"+code+",b:"+b);
		if(b){
			String url = "http://"+Constants.WXPUBLIC_S_DOMAIN+"/zld/qr/c/"+code;
			infoMap.put("result", 1);
			infoMap.put("code", url);
			infoMap.put("ticketid", ticketId);
			infoMap.put("cname", comMap.get("company_name"));
		}else{
			infoMap.put("result", -1);
		}
		return StringUtils.createJson(infoMap);
	}

	private String parkingList(HttpServletRequest request, Long uin) {
		Integer pageNum = RequestUtil.getInteger(request, "page", 1);
		Integer pageSize = RequestUtil.getInteger(request, "size", 20);
		Long btime = TimeTools.getToDayBeginTime() - 6* 24 * 60 *60;
		List<Object> params = new ArrayList<Object>();
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		String sql = "select uin,count(*) pcount from order_tb where state=? and uid=? and end_time>? and uin is not null group by uin order by pcount desc";
		String countsql = "select count(distinct uin) from order_tb where state=? and uid=? and end_time>? and uin is not null ";
		params.add(1);
		params.add(uin);
		params.add(btime);
		Long count = daService.getCount(countsql, params);
		if(count > 0){
			list = daService.getAll(sql, params, pageNum, pageSize);
			setCarNumber(list);
		}
		String result = "{\"count\":"+count+",\"info\":"+StringUtils.createJson(list)+"}";
		return result;
	}

	private String rewardList(HttpServletRequest request, Long uin) {
		Integer pageNum = RequestUtil.getInteger(request, "page", 1);
		Integer pageSize = RequestUtil.getInteger(request, "size", 20);
		List<Object> params = new ArrayList<Object>();
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		Long btime = TimeTools.getToDayBeginTime() - 6 * 24 * 60 * 60;
		params.add(uin);
		params.add(btime);
		String sql = "select uin,count(*) rcount,sum(money) rmoney from parkuser_reward_tb where uid=? and ctime>? group by uin order by rcount desc";
		String countsql = "select count(distinct uin) from parkuser_reward_tb where uid=? and ctime>? ";
		Long count = daService.getCount(countsql, params);
		if(count > 0){
			list = daService.getAll(sql, params, pageNum, pageSize);
			setCarNumber(list);
		}
		String result = "{\"count\":"+count+",\"info\":"+StringUtils.createJson(list)+"}";
		return result;
	}

	private String sendSuccess(HttpServletRequest request, Long uin) {
		Long ntime = System.currentTimeMillis()/1000;
		Long bonusId = RequestUtil.getLong(request, "bonusid", -1L);
		Double score = RequestUtil.getDouble(request, "score", 15d);
		Long ctime = ntime;
		logger.error("sendsuccess>>>������ͳɹ��ص�:bonusid:"+bonusId+",uin:"+uin+",score:"+score);
		if(bonusId != -1){
			Long count = daService.getLong("select count(*) from reward_account_tb where orderticket_id=? ", new Object[]{bonusId});
			logger.error("sendsuccess>>>������ͳɹ��ص�:bonusid:"+bonusId+",uin:"+uin+",count:"+count+",score:"+score);
			if(count == 0){
				Map<String, Object> userMap = daService.getMap(
						"select id,nickname,reward_score from user_info_tb where id=? ",
						new Object[] { uin });
				Double reward_score = Double.valueOf(userMap.get("reward_score") + "");
				logger.error("sendsuccess>>>������ͳɹ��ص�:bonusid:"+bonusId+",uin:"+uin+",score:"+score+",ʣ�����reward_score:"+reward_score+",�˴����Ļ���score:"+score);
				if(reward_score > score){
					List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
					//�����˻�
					Map<String, Object> scoreSqlMap = new HashMap<String, Object>();
					//������ϸ
					Map<String, Object> scoreAccountSqlMap = new HashMap<String, Object>();
					
					scoreAccountSqlMap.put("sql", "insert into reward_account_tb (uin,score,type,create_time,remark,target,orderticket_id) values(?,?,?,?,?,?,?)");
					scoreAccountSqlMap.put("values", new Object[]{uin,score,1,ctime,"��� ",1,bonusId});
					bathSql.add(scoreAccountSqlMap);
					
					scoreSqlMap.put("sql", "update user_info_tb set reward_score=reward_score-? where id=? ");
					scoreSqlMap.put("values", new Object[]{score, uin});
					bathSql.add(scoreSqlMap);
					boolean b = daService.bathUpdate(bathSql);
					logger.error("sendsuccess>>>������ͳɹ��ص�:bonusid:"+bonusId+",uin:"+uin+",b:"+b);
					if(b){
						//AjaxUtil.ajaxOutput(response, "1");
						return "1";
					}
				}
			}
		}
		//AjaxUtil.ajaxOutput(response, "-1");
		return "-1";
	}

	private String sendBonus(HttpServletRequest request, Long uin, Long comId) {
		Long ntime = System.currentTimeMillis()/1000;
		Integer bmoney = RequestUtil.getInteger(request, "bmoney", 0);//���
		Integer bnum = RequestUtil.getInteger(request, "bnum", 0);//����
		Double score = RequestUtil.getDouble(request, "score", 0d);//���Ļ���
		Map<String, Object> infoMap=new HashMap<String, Object>(); 
		logger.error("sendbonus>>>�շ�Ա��"+uin+",bmoney:"+bmoney+",bnum:"+bnum+",score:"+score);
		Map<String, Object> comMap = daService.getMap("select company_name from com_info_tb where id=? ", new Object[]{comId});
		Map<String, Object> userMap = daService.getMap(
				"select id,nickname,reward_score from user_info_tb where id=? ",
				new Object[] { uin });
		Double reward_score = Double.valueOf(userMap.get("reward_score") + "");
		if(reward_score < score){
			infoMap.put("result", -3);
			//AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));//���ֲ���
			logger.error("sendticket>>>���ͻ��ֲ��㣬�շ�Ա:"+uin+",score:"+score+",reward_score:"+reward_score);
			return StringUtils.createJson(infoMap);
		}
		Long ctime = ntime;
		Long exptime = ctime + 24*60*60;
		Long bonusId = daService.getkey("seq_order_ticket_tb");
		int result = daService.update("insert into order_ticket_tb (id,uin,order_id,money,bnum,ctime,exptime,bwords,type) values(?,?,?,?,?,?,?,?,?)",
						new Object[] { bonusId, uin, -1, bmoney, bnum, ctime, exptime, "ף��һ·������!", 2 });
		logger.error("sendbonus>>>:�շ�Ա"+uin+",result:"+result);
		if(result == 1){
			infoMap.put("result", 1);
			infoMap.put("bonusid", bonusId);
			infoMap.put("cname", comMap.get("company_name"));
		}else{
			infoMap.put("result", -1);
		}
		return  StringUtils.createJson(infoMap);
	}

	private String sendTicket(HttpServletRequest request, Long uin,Long comId) {
		Long ntime = System.currentTimeMillis()/1000;
		Integer bmoney = RequestUtil.getInteger(request, "bmoney", 0);//���
		Double score = RequestUtil.getDouble(request, "score", 0d);//���Ļ���
		String uins = RequestUtil.processParams(request, "uins");//�����˺�
		logger.error("sendticket>>>�շ�Ա:"+uin+",bmoney:"+bmoney+",score:"+score+",uins:"+uins);
		String ids[] = uins.split(",");
		if(ids.length == 0 || uins.length() == 0){
			//AjaxUtil.ajaxOutput(response, "-2");//δѡ����
			return "-2";
		}
		Long ctime = ntime;
		Map<String, Object> userMap = daService.getMap(
				"select id,nickname,reward_score from user_info_tb where id=? ",
				new Object[] { uin });
		Map<String, Object> comMap = daService.getMap(
				"select company_name from com_info_tb where id=? ",
				new Object[] { comId });
		List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
		//�����˻�
		Map<String, Object> scoreSqlMap = new HashMap<String, Object>();
		Long exptime = ctime + 24*60*60;
		for(int i = 0; i<ids.length; i++){
			//дȯ
			Map<String, Object> ticketSqlMap = new HashMap<String, Object>();
			//������ϸ
			Map<String, Object> scoreAccountSqlMap = new HashMap<String, Object>();
			
			Long cuin = Long.valueOf(ids[i]);
			String carNumber = publicMethods.getCarNumber(cuin);
			Long ticketId = daService.getkey("seq_ticket_tb");
			
			ticketSqlMap.put("sql", "insert into ticket_tb (id,create_time,limit_day,money,state,uin,type,comid) values(?,?,?,?,?,?,?,?)");
			ticketSqlMap.put("values", new Object[]{ticketId,TimeTools.getToDayBeginTime(),TimeTools.getToDayBeginTime()+16*24*60*60-1,bmoney,0,cuin,1,comId});
			bathSql.add(ticketSqlMap);
			
			scoreAccountSqlMap.put("sql", "insert into reward_account_tb (uin,score,type,create_time,remark,target,ticket_id) values(?,?,?,?,?,?,?)");
			scoreAccountSqlMap.put("values", new Object[]{uin,score,1,ctime,"ͣ��ȯ "+carNumber,2,ticketId});
			bathSql.add(scoreAccountSqlMap);
		}
		Double allscore = StringUtils.formatDouble(score * ids.length);
		logger.error("sendticket>>>�շ�Ա:"+uin+",allscore:"+allscore);
		Double reward_score = Double.valueOf(userMap.get("reward_score") + "");
		if(reward_score < allscore){
			//AjaxUtil.ajaxOutput(response, "-3");//���ֲ���
			logger.error("sendticket>>>���ͻ��ֲ��㣬�շ�Ա:"+uin+",allscore:"+allscore+",reward_score:"+reward_score);
			return "-3";
		}
		if(allscore > 0 && bathSql.size() > 0){
			scoreSqlMap.put("sql", "update user_info_tb set reward_score=reward_score-? where id=? ");
			scoreSqlMap.put("values", new Object[]{allscore, uin});
			bathSql.add(scoreSqlMap);
		}
		boolean b = daService.bathUpdate(bathSql);
		logger.error("sendticket>>>�շ�Ա��"+uin+",b:"+b);
		if(b){
			for(int i = 0;i<ids.length; i++){
				Long cuin = Long.valueOf(ids[i]);
				logService.insertUserMesg(5, cuin,"����ͣ�����շ�Ա" + userMap.get("nickname") + "����������"
								+ bmoney + "Ԫ" + comMap.get("company_name")
								+ "ר��ȯ���������ҳ���ͣ����", "ͣ��ȯ����");
			}
			sendWXMsg(ids, userMap, comMap, bmoney);
			//AjaxUtil.ajaxOutput(response, "1");
			return "1";
		}else{
			//AjaxUtil.ajaxOutput(response, "-1");
			return "-1";
		}
	}

	private String bonusInfo(HttpServletRequest request, Long uin) {
		String bonusinfo = CustomDefind.SENDTICKET;
		JSONArray jsonArray = JSONArray.fromObject(bonusinfo);
		for(int i=0; i<jsonArray.size(); i++){
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			int type = jsonObject.getInt("type");
			int bmoney = jsonObject.getInt("bmoney");
			double score = jsonObject.getDouble("score");
			if(type == 1 && bmoney == 5){
				Long btime = TimeTools.getToDayBeginTime();
				Long count = daService.getLong("select count(*) from reward_account_tb r,ticket_tb t where r.ticket_id=t.id and r.type=? and r.target=? and r.create_time>? and t.money=? and r.uin=? ",
								new Object[] { 1, 2, btime, 5, uin });
				score = score * (count + 1);
				logger.error("���շ�����Ԫȯ����count:"+count+",uid:"+uin+",today:"+btime+",��һ�����ѻ��֣�score��"+score);
				jsonObject.put("score", score);
				if(count >=10){
					jsonObject.put("limit", 1);
				}else{
					jsonObject.put("limit", 0);
				}
				break;
			}
		}
		logger.error("bonusinfo:"+jsonArray.toString()+",uin:"+uin);
		return jsonArray.toString();
	}

	private String rewardRank(HttpServletRequest request, Long uin) {
		Integer pageNum = RequestUtil.getInteger(request, "page", 1);
		Integer pageSize = RequestUtil.getInteger(request, "size", 20);
		List<Object> params = new ArrayList<Object>();
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		String monday = StringUtils.getMondayOfThisWeek();
		Long btime = TimeTools.getLongMilliSecondFrom_HHMMDD(monday)/1000;
		String sql = "select uid uin,sum(money) money from parkuser_reward_tb where ctime>=? group by uid order by money desc ";
		String countsql = "select count(distinct uid) from parkuser_reward_tb where ctime>=? ";
		params.add(btime);
		Long total = daService.getCount(countsql, params);
		if(total > 0){
			list = daService.getAll(sql, params, pageNum, pageSize);
			setinfo(list, pageNum, pageSize);
		}
		String result = "{\"count\":"+total+",\"info\":"+StringUtils.createJson(list)+"}";
		return result;
	}

	private String rscoreRank(HttpServletRequest request, Long uin) {
		Integer pageNum = RequestUtil.getInteger(request, "page", 1);
		Integer pageSize = RequestUtil.getInteger(request, "size", 20);
		Long btime = TimeTools.getToDayBeginTime();
		List<Object> params = new ArrayList<Object>();
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		String sql = "select uin,sum(score) score from reward_account_tb where create_time>=? and type=? group by uin order by score desc ";
		String countsql = "select count(distinct uin) from reward_account_tb where create_time>=? and type=? ";
		params.add(btime);
		params.add(0);
		Long total = daService.getCount(countsql, params);
		if(total > 0){
			list = daService.getAll(sql, params, pageNum, pageSize);
			setinfo(list, pageNum, pageSize);
		}
		String result = "{\"count\":"+total+",\"info\":"+StringUtils.createJson(list)+"}";
		return result;
	}

	private Map<String, Object> rewardscore(HttpServletRequest request,
			Long uin, Map<String, Object> infoMap) {
		Double remainscore = 0d;//ʣ�����
		Long rank = 0L;//���а�
		Double todayscore = 0d;//���ջ���
		Long btime = TimeTools.getToDayBeginTime();
		Map<String, Object> scoreMap = daService
				.getMap("select reward_score from user_info_tb where id=? ",
						new Object[] { uin });
		if(scoreMap != null){
			remainscore = Double.valueOf(scoreMap.get("reward_score") + "");
		}
		Long scoreCount = daService.getLong("select count(*) from reward_account_tb where create_time> ? and type=? and uin=? ",
						new Object[] { btime, 0, uin });
		if(scoreCount > 0){
			List<Map<String, Object>> scoreList = daService
					.getAll("select uin,sum(score) score from reward_account_tb where create_time> ? and type=? group by uin order by score desc ",
							new Object[] { btime, 0 });
			for(Map<String, Object> map : scoreList){
				Long uid = (Long)map.get("uin");
				rank++;
				if(uid.intValue() == uin.intValue()){
					todayscore = Double.valueOf(map.get("score") + "");
					break;
				}
			}
		}
		infoMap.put("todayscore", todayscore);
		infoMap.put("rank", rank);
		infoMap.put("remainscore", remainscore);
		infoMap.put("ticketurl", "http://mp.weixin.qq.com/s?__biz=MzA4MTAxMzA2Mg==&mid=208427604&idx=1&sn=a3de34b678869c4bbe54547396fcb2a3#rd");
		infoMap.put("scoreurl", "http://mp.weixin.qq.com/s?__biz=MzA4MTAxMzA2Mg==&mid=208445618&idx=1&sn=b4d99d5233921ae53c847165c62dec2b#rd");
		return infoMap;
	}

	private String getParkDetail(HttpServletRequest request, Long uin,Long comId,Long authFlag) {
		Long ntime = System.currentTimeMillis()/1000;
		authFlag = daService.getLong("select auth_flag from user_info_tb where id =? ", new Object[]{uin});
		String ret="{";
		Long b = TimeTools.getToDayBeginTime();
		Long e = ntime;
//		long b=1436544000;,e=1435916665;
		Double mmoney = 0d;
		Double cashmoney = 0d;
		Double total = 0d;
		if(authFlag==1){
			ArrayList list1 = new ArrayList();
			ArrayList list2 = new ArrayList();
			list1.add(b);
			list1.add(e);
			list1.add(0);
			list1.add(0);
			list1.add(comId);
			list2.add(b);
			list2.add(e);
			list2.add(0);
			list2.add(4);
			list2.add("ͣ����%");
			list2.add(comId);
			List park = daService.getAllMap( "select b.nickname, sum(a.amount) total,a.uid from park_account_tb a,user_info_tb b where a.create_time between ? and ? " +
					" and a.type= ? and a.source=? and a.comid=? and a.uid=b.id group by a.uid,b.nickname ",list1);//�����˻�ͣ����
			
			List parkuser = daService.getAllMap( "select b.nickname,sum(a.amount) total,a.uin uid from parkuser_account_tb a,user_info_tb b where a.create_time between ? and ? " +
					" and a.type= ? and a.target=? and a.remark like ? and a.uin=b.id and a.uin in (select id from user_info_tb where comid=?) group by a.uin,b.nickname",list2);//�շ�Ա�˻�ͣ����
			TreeSet<Long> set = new TreeSet<Long>();
			if(park!=null&&park.size()>0){
				if(parkuser!=null&&parkuser.size()>0)
					park.addAll(parkuser);
				for (int i = 0; i < park.size(); i++) {
//					System.out.println(park.size());
					Map obj1 = (Map)park.get(i);
					Long id1 = Long.valueOf(obj1.get("uid")+"");
					set.add(id1);
					Map cash = daService.getMap( "select sum(amount) total from parkuser_cash_tb where create_time between ? and ? " +
							" and uin=? ",new Object[]{b,e,id1});
					if(cash!=null&&cash.get("total")!=null){
						double cmoney = Double.valueOf(cash.get("total")+"");
						cashmoney+=cmoney;
						obj1.put("cash",StringUtils.formatDouble(cmoney ));//�շ�Ա�ֽ�����
					}else{
						obj1.put("cash",0.00);//�շ�Ա�ֽ�����
					}
					double ummoney = Double.valueOf(obj1.get("total")+"");
					mmoney+=ummoney;
					for (int j = i+1; j < park.size(); j++) {
//						System.out.println(park.size());
						Map obj2 = (Map)park.get(j);
						long id2 = Long.valueOf(obj2.get("uid")+"");
						if(id1==id2){
							double total1 =Double.valueOf(obj2.get("total")+"");
							mmoney+=total1;
							obj1.put("total", StringUtils.formatDouble(total1+ummoney));
							park.remove(j);
						}
					}
				}
			}else{
				park = parkuser;
				for (Object object : parkuser) {
					Map obj = (Map)object;
					Map cash = daService.getMap( "select sum(amount) total from parkuser_cash_tb where create_time between ? and ? " +
							" and uin=? ",new Object[]{b,e,Long.valueOf(obj.get("uid")+"")});
					set.add(Long.valueOf(obj.get("uid")+""));
					if(cash!=null&&cash.get("total")!=null){
						double cmoney = Double.valueOf(cash.get("total")+"");
						cashmoney+=cmoney;
					}
					if(obj.get("total")!=null)
						mmoney+=Double.valueOf(obj.get("total")+"");
				}
			}
			List user = daService.getAll( "select id, nickname from user_info_tb where comid=?",new Object[]{comId});
			for (Object object : user) {
				Map obj = (Map)object;
				if(set.add(Long.valueOf(obj.get("id")+""))){
					Map cash = daService.getMap( "select sum(amount) total from parkuser_cash_tb where create_time between ? and ? " +
							" and uin=? ",new Object[]{b,e,Long.valueOf(obj.get("id")+"")});
					if(cash!=null&&cash.get("total")!=null){
						double cmoney = Double.valueOf(cash.get("total")+"");
						cashmoney+=cmoney;
						Map tmap = new TreeMap();
						tmap.put("nickname",obj.get("nickname"));
						tmap.put("total",0.0);
						tmap.put("uid",obj.get("id"));
						tmap.put("cash",StringUtils.formatDouble(cmoney ));//�շ�Ա�ֽ�����
						park.add(tmap);
					}
				}
			}
			total=cashmoney+mmoney;
			String detail = StringUtils.createJson(park);
			ret+="\"total\":\""+StringUtils.formatDouble(total)+"\",\"mmoeny\":\""+StringUtils.formatDouble(mmoney)+
			"\",\"cashmoney\":\""+StringUtils.formatDouble(cashmoney)+"\",\"detail\":"+detail+"}";
		}else{
			//��û��Ȩ�޲鿴
			return "-1";
		}
		logger.error("getparkdetail>>>>��"+ret);
		return ret;
	}

	private String akeyCheckAccount(HttpServletRequest request, Long uin,
			Long comId) {
		Long ntime = System.currentTimeMillis()/1000;
		String ret = "{";
		Long b = TimeTools.getToDayBeginTime();
		Long e = ntime+60;
		Double parkmoney = 0d;
		Double parkusermoney = 0d;
		Double cashmoney = 0d;
		Long ordertotal = 0L;
		Long epayordertotal = 0L;
		Double ordertotalmoney = 0d;
		Double epaytotalmoney = 0d;
		//һ������   1:��������Ա    2�շ�Ա
		Map park = daService.getMap( "select sum(amount) total from park_account_tb where create_time between ? and ? " +
				" and type <> ? and uid=? and comid=? ",new Object[]{b,e,1,uin,comId});
		if(park!=null&&park.get("total")!=null)
			parkmoney = Double.valueOf(park.get("total")+"");//�����˻����루������Դ��
		
		Map parkuser = daService.getMap( "select sum(amount) total from parkuser_account_tb where create_time between ? and ? " +
				" and type= ? and uin = ? ",new Object[]{b,e,0,uin});
		if(parkuser!=null&&parkuser.get("total")!=null)
			parkusermoney = Double.valueOf(parkuser.get("total")+"");//�շ�Ա�˻����루������Դ��
		
		Map cash = daService.getMap( "select sum(amount) total from parkuser_cash_tb where create_time between ? and ? " +
				" and uin=? ",new Object[]{b,e,uin});
		if(cash!=null&&cash.get("total")!=null)
			cashmoney = Double.valueOf(cash.get("total")+"");//�շ�Ա�ֽ�����
		Map ordertotalMap = daService.getMap( "select count(*) scount,sum(total) total from order_tb where end_time between ? and ? " +
				" and uid=? and state=?",new Object[]{b,e,uin,1});//�ܵĶ���
		if(ordertotalMap!=null){
			if(ordertotalMap.get("total")!=null)
				ordertotalmoney = Double.valueOf(ordertotalMap.get("total")+"");
			if(ordertotalMap.get("scount")!=null)
				ordertotal = Long.valueOf(ordertotalMap.get("scount")+"");
		}
		Map epayordertotalMap = daService.getMap( "select count(*) scount,sum(total) total from order_tb where end_time between ? and ? " +
				" and uid=? and c_type=? and state=?",new Object[]{b,e,uin,4,1});//ֱ������
		if(epayordertotalMap!=null){
			if(epayordertotalMap.get("total")!=null)
				epaytotalmoney = Double.valueOf(epayordertotalMap.get("total")+"");
			if(epayordertotalMap.get("scount")!=null)
				epayordertotal = Long.valueOf(epayordertotalMap.get("scount")+"");
		}
		ret+="\"totalmoney\":\""+StringUtils.formatDouble((parkmoney+parkusermoney+cashmoney))+"\",\"mobilemoney\":\""+StringUtils.formatDouble((parkmoney+parkusermoney))+
		"\",\"cashmoney\":\""+StringUtils.formatDouble(cashmoney)+"\",\"mycount\":\""+StringUtils.formatDouble(parkusermoney)+"\",\"parkaccout\":\""+StringUtils.formatDouble(parkmoney)+"\",\"timeordercount\":\""+
		(ordertotal-epayordertotal)+"\",\"timeordermoney\":\""+StringUtils.formatDouble((ordertotalmoney-epaytotalmoney))+
		"\",\"epayordercount\":\""+epayordertotal+"\",\"epaymoney\":\""+StringUtils.formatDouble(epaytotalmoney)+"\"}";
		logger.error("akeycheckaccount>>>��"+ret);
		return ret;
	}

	private String goOffWork(HttpServletRequest request, Long uin) {
		Long  worksiteid =RequestUtil.getLong(request, "worksiteid",-1L);
		if(worksiteid <= 0){
			return "-1";
		}
		long endtime = System.currentTimeMillis() / 1000;
		int result = daService.update("update parkuser_work_record_tb set end_time=? where worksite_id = ? and uid = ? and end_time is null",
						new Object[] { endtime, worksiteid, uin });
		logger.error("gooffwork>>>>>�°�result��"+result+",uin:"+uin+",worksiteid:"+worksiteid);
		if(result > 0){
			return "1";
		}else{
			return "-1";
		}
	}

	private String bindWorkSite(HttpServletRequest request, Long uin) {
		Long wid = RequestUtil.getLong(request, "wid", -1L);
		logger.error(">>>>disbind,wid:"+wid);
		int ret =0;
		if(uin!=-1){
			if(wid==-1){//���
				ret = daService.update("delete from user_worksite_tb where uin = ? ", new Object[]{uin});
				logger.error(">>>>disbind  �շ�Ա���   worksite,user:"+uin+"ret:"+ret);
				ret = 1;
			}else {//��
				//��ǰ�ȴ���������վ���¸�
				ret = daService.update("delete from user_worksite_tb where uin = ?  ", new Object[]{uin});
				logger.error(">>>>bind �շ�Ա�ϸڣ�ɾ��ԭ���ڵĹ���վ:"+ret);
				//ɾ��ԭ���շ�Ա
				Map oldMap = daService.getMap("select uin from user_worksite_tb where worksite_id=? ", new Object[]{wid});
				if(oldMap!=null){
					ret = daService.update("delete from user_worksite_tb where worksite_id = ?  ", new Object[]{wid});
					if(ret>0){
						Long uid =(Long)oldMap.get("uin");
						if(uid!=null&&uid>0)
							ret = daService.update("insert into order_message_tb(message_type,state,uin)" +
								" values(?,?,?)", new Object[]{4,0,uid});//����Ϣ���շ�Ա��֪ͨ���Ѳ��ڸ�
						logger.error(">>>>disbind �շ�Ա�ϸڣ�ԭ�շ�Ա�¸�  worksite,delete old user:"+uid+",ret:"+ret);
					}
				}
				//���շ�Ա
				ret = daService.update("insert into user_worksite_tb (worksite_id,uin) values(?,?)",new Object[]{wid,uin});
				logger.error(">>>>bind worksite,�շ�Ա�ϸ�  bind new user:"+uin+", ret="+ret);
			}
		}
		return "{\"result\":\""+ret+"\"}";
	}

	private String incomAnly(HttpServletRequest request, Long uin,Long comId) {
		//0�Լ�,1����
		Integer acctype = RequestUtil.getInteger(request, "acctype", 0);
		//0ͣ���ѣ�1���� ��2����,3 ȫ��
		Integer income = RequestUtil.getInteger(request, "incom", 0);
		//0���죬1���죬2���ܣ�3����
		Integer datetype = RequestUtil.getInteger(request, "datetype", 0);
		Integer page = RequestUtil.getInteger(request, "page", 1);
		page = page<1?1:page;
		List<Object> params = new ArrayList<Object>();
		
		String sql = "";
		String totalSql = "";
		if(acctype==0){//0�Լ�,1����
			sql +="select amount money,type mtype,create_time," +
					"remark note,target from parkuser_account_tb where uin=? ";
			totalSql = "select sum(amount) total from parkuser_account_tb where uin=?";
			params.add(uin);
		}else if(acctype==1){
			sql +=" select create_time ,remark r,amount money,type mtype  from park_account_tb where comid=? ";
			totalSql = "select sum(amount) total from park_account_tb where comid=?";
			params.add(comId);
		}
		if(income==0){//0ͣ����
			if(acctype==0){//0�Լ�,1����
				sql +=" and type=? and target=? ";
				totalSql +=" and type=? and target=? ";
				params.add(0);
				params.add(4);
			}else if(acctype==1){
				sql +=" and type= ? ";
				totalSql +=" and type= ? ";
				params.add(0);
			}
		}else if(income==1){//1���� 
			if(acctype==0){//0�Լ�,1����
				sql +=" and type=? and target=? and amount =? ";
				totalSql +=" and type=? and target=? and amount =? ";
				params.add(0);
				params.add(3);
				params.add(2d);
			}else if(acctype==1){
				sql +=" and type= ? ";
				totalSql +=" and type= ? ";
				params.add(2);
			}
		}else if(income==2){//2����
			if(acctype==0){//0�Լ�,1����
				sql +=" and type=? and target=? and amount >? ";
				totalSql +=" and type=? and target=? and amount >? ";
				params.add(0);
				params.add(3);
				params.add(2d);
			}else if(acctype==1){
				sql +=" and type= ? ";
				totalSql +=" and type= ? ";
				params.add(3);
			}
		}
		
		Long btime = TimeTools.getToDayBeginTime();
		Long etime = btime+24*60*60;
		if(datetype==1){
			etime = btime ;
			btime = btime-24*60*60;
		}else if(datetype==2){
			btime = TimeTools.getWeekStartSeconds();
		}else if(datetype==3){
			btime = TimeTools.getMonthStartSeconds();
		}
		sql +=" and create_time between ? and ? order by create_time desc";
		totalSql +=" and create_time between ? and ? ";
		params.add(btime);
		params.add(etime);
//		System.out.println(sql);
//		System.out.println(totalSql);
		System.err.println(">>>>>>incomanly:"+sql+":"+params);
		Map totalMap = pService.getMap(totalSql, params);
		List reslutList = pService.getAll(sql, params,page,20);	
		setAccountList(reslutList,acctype);
		String total = totalMap.get("total")+"";
		if(total.equals("null"))
			total = "0.0";
		String reslut =  "{\"total\":\""+total+"\",\"info\":"+StringUtils.createJson(reslutList)+"}";
		System.err.println(reslut);
		return reslut;
	}

	private String queryCarPics(HttpServletRequest request, Long uin,Long comId) {
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		String sql = "select distinct(car_number) from order_tb where comid=? and c_type=? and create_time between ? and ? ";
		SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
		String nowtime= df2.format(System.currentTimeMillis());
		Long endTime = TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(nowtime+" 23:59:59");
		Long beginTime = endTime - 30*24*60*60;
		list = pService.getAll(sql, new Object[]{comId,2,beginTime,endTime});
		String result = StringUtils.createJson(list);
		return result;
	}

	private String getNewIncome(HttpServletRequest request, Long uin) {
		String lock = null;
		Long ntime = System.currentTimeMillis()/1000;
		Map<String, Object> map = new HashMap<String, Object>();
		String btime = RequestUtil.processParams(request, "btime");
		String etime = RequestUtil.processParams(request, "etime");
		Long logonTime = RequestUtil.getLong(request, "logontime", -1L);//20150618���ϴ����¼ʱ�� 
		SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
		Long worksiteid = RequestUtil.getLong(request, "worksite_id",-1L);
		Long comid = RequestUtil.getLong(request, "comid",-1L);
		String nowtime= df2.format(System.currentTimeMillis());
		Long b = ntime;
		Long e =b;
		if(logonTime!=-1){
			b = logonTime;
			//logger.error(b);
			b = (logonTime/60)*60;
		}else {
			if(btime.equals("")){
				btime = nowtime;
			}
			if(etime.equals("")){
				etime = nowtime;
			}
			b = TimeTools.getLongMilliSecondFrom_HHMMDD(btime)/1000;
			e =  TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(etime+" 23:59:59");
		}
		if(worksiteid!=-1){
			lock = "parkuser_work_record_tb" + uin;
			if(!memcacheUtils.addLock(lock, 5)){//10s��һ��
				logger.error("getnewincome lock:"+lock+",5�����ڲ����ظ�����...");
				return StringUtils.createJson(map);
			}
			Map ret = pService.getMap(
					"select * from parkuser_work_record_tb where end_time is null and uid=? and worksite_id = ?",
					new Object[] {uin,worksiteid});
			if(ret!=null){
				b = Long.valueOf(ret.get("start_time")+"");
				map.put("start_time", b);
				if(ret.get("end_time")==null){
					e=Long.MAX_VALUE;
				}else{
					e=Long.valueOf(ret.get("end_time")+"");
				}
			}
		}
		if(b > ntime - 10 * 24 * 60 * 60 && e > b){
			Map<String, Object> incomeMap = commonMethods.getIncome(b, e, uin);
			Double prepay_cash = Double.valueOf(incomeMap.get("prepay_cash") + "");//�ֽ�Ԥ֧��
			Double add_cash = Double.valueOf(incomeMap.get("add_cash") + "");//�ֽ𲹽�
			Double refund_cash = Double.valueOf(incomeMap.get("refund_cash") + "");//�ֽ��˿�
			Double pursue_cash = Double.valueOf(incomeMap.get("pursue_cash") + "");//�ֽ�׷��
			Double pfee_cash = Double.valueOf(incomeMap.get("pfee_cash") + "");//�ֽ�ͣ���ѣ���Ԥ����
			Double prepay_epay = Double.valueOf(incomeMap.get("prepay_epay") + "");//����Ԥ֧��
			Double add_epay = Double.valueOf(incomeMap.get("add_epay") + "");//���Ӳ���
			Double refund_epay = Double.valueOf(incomeMap.get("refund_epay") + "");//�����˿�
			Double pursue_epay = Double.valueOf(incomeMap.get("pursue_epay") + "");//����׷��
			Double pfee_epay = Double.valueOf(incomeMap.get("pfee_epay") + "");//����ͣ���ѣ���Ԥ����
			Double cash = StringUtils.formatDouble(pfee_cash + pursue_cash + prepay_cash + add_cash);
			Double epay = StringUtils.formatDouble(pfee_epay + pursue_epay + prepay_epay + add_epay);
			map.put("cashpay", cash);
			map.put("mobilepay", epay);
		}else{
			logger.error("b:"+b+",e:"+e+",ntime:"+ntime);
		}
		logger.error("getnewincome,uid:"+uin+",return:"+map);
		return StringUtils.createJson(map);
	}

	private String getIncome(HttpServletRequest request, Long uin) {
		Long ntime = System.currentTimeMillis()/1000;
		String btime = RequestUtil.processParams(request, "btime");
		String etime = RequestUtil.processParams(request, "etime");
		Long logonTime = RequestUtil.getLong(request, "logontime", -1L);//20150618���ϴ����¼ʱ�� 
		SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
		String nowtime= df2.format(System.currentTimeMillis());
		Long b = ntime;
		Long e =b;
		if(logonTime!=-1){
			b = logonTime;
			//logger.error(b);
			b = (logonTime/60)*60;
		}else {
			if(btime.equals("")){
				btime = nowtime;
			}
			if(etime.equals("")){
				etime = nowtime;
			}
			b = TimeTools.getLongMilliSecondFrom_HHMMDD(btime)/1000;
			e =  TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(etime+" 23:59:59");
		}
		//logger.error(b);
		String sql = "select sum(total) money,pay_type from order_tb where create_time>? and  " +
				"uid=? and c_type=? and state=? and end_time between ? and ? " +
				"group by pay_type order by pay_type desc ";
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		list = pService.getAll(sql, new Object[]{ntime-30*86400,uin,3,1,b,e});
		Map<String, Object> map = new HashMap<String, Object>();
		for(Map<String, Object> map2 : list){
			Integer pay_type = (Integer)map2.get("pay_type");
			if(pay_type == 2){//�ֻ�֧��
				map.put("mobilepay", map2.get("money"));
			}else if(pay_type == 1){//�ֽ�֧��
				map.put("cashpay", map2.get("money"));
			}
		}
		//logger.error(map);
		return StringUtils.createJson(map);
	}

	private String getMesg(HttpServletRequest request, Long uin) {
		Long maxid = RequestUtil.getLong(request, "maxid", -1L);
		Integer page = RequestUtil.getInteger(request, "page", 1);
		if(maxid>-1){
			Long count = daService.getLong("select count(ID) from parkuser_message_tb where uin=? and id>?", new Object[]{uin,maxid});
			return count+"";
		}else{
			List<Object> params = new ArrayList<Object>();
			params.add(uin);
			List<Map<String, Object>> list = daService.getAll("select * from parkuser_message_tb where uin=? order by id desc",
					params,page,10);
			return StringUtils.createJson(list);
		}
		//http://127.0.0.1/zld/carowner.do?action=getmesg&token=&page=-1&maxid=0
	}

	private String recominfo(HttpServletRequest request, Long uin) {
		Integer rtype = RequestUtil.getInteger(request, "type", 0);//0:������1:����
		List<Map<String, Object>> list =null;
		if(rtype==0){
			list = daService.getAll("select c.nid,u.mobile uin,c.state,c.money from recommend_tb c left join user_info_tb u on c.nid=u.id where pid=? and c.type=? order by c.id desc ",new Object[]{uin,rtype});
		}else {
			list  =daService.getAll("select nid uin,state,money from recommend_tb where pid=? and type=? order by id desc",new Object[]{uin,rtype});
		}
		if(list!=null&&!list.isEmpty()){
			for(Map<String, Object> map :list){
				Integer state = (Integer)map.get("state");
				if(state==null||state!=1)
					continue;
				Double money = StringUtils.formatDouble(map.get("money"));
				if(rtype==0&&money==0)
					map.put("money", 5);
				else if(rtype==1&&money==0){
					map.put("money", 30);
				}
			}
		}
		return StringUtils.createJson(list);
		//http://127.0.0.1/zld/collectorrequest.do?action=recominfo&token=40ffacdad78acf0c43e0aabae9712602
	}

	private String regcolmsg(HttpServletRequest request, Long uin, Long comId,String out) {
		Long ntime = System.currentTimeMillis()/1000;
		System.err.println(">>>>>>>>>>>>>>>>>>>>>>>>>>�����Ƽ�����+"+uin);
		Long tid = daService.getkey("seq_transfer_url_tb");
		//String url = "http://192.168.199.240/zld/turl?p="+tid;
		String url = "http://s.tingchebao.com/zld/turl?p="+tid;
		int result = daService.update("insert into transfer_url_tb(id,url,ctime,state) values (?,?,?,?)",
				new Object[]{tid,"regparker.do?action=toregpage&recomcode="+uin,
						ntime,0});
		
		if(result!=1)
			url="�Ƽ�ʧ��!";
		if(out.equals("json")){
			return  "{\"url\":\""+url+"\"}";
		}else {
			return url;
		}
		//http://127.0.0.1/zld/collectorrequest.do?action=regcarmsg&token=6ed161cde6c7149de49d72719f2eb39b
	}

	private String reguser(HttpServletRequest request, Long uin, Long comId) {
		Long ntime = System.currentTimeMillis()/1000;
		String carNumber =AjaxUtil.decodeUTF8( RequestUtil.getString(request, "carnumber"));
		carNumber = carNumber.toUpperCase().trim();
		carNumber = carNumber.replace("I", "1").replace("O", "0");
		String mobile = RequestUtil.getString(request, "mobile");
		Long curTime = System.currentTimeMillis()/1000;
		if(!carNumber.equals("")){
			if(mobile.equals("")){//��֤���ƺ�
				Long count = daService.getLong("select count(id) from car_info_tb where car_number=?", new Object[]{carNumber});
				if(count>0){
					return "-1";
				}
			}else {//ע�ᳵ����ͬʱֻ��֤�ֻ���
				Long count = daService.getLong("select count(id) from user_info_tb where mobile=? and auth_flag=? ", new Object[]{mobile,4});
				if(count>0){
					return "-1";
				}
				//д�û�����
				List<Map<String, Object>> sqlList = new ArrayList<Map<String,Object>>();
				//�û���Ϣ
				Map<String, Object> userSqlMap = new HashMap<String, Object>();
				//��һ���û��˺�
				Long key = daService.getkey("seq_user_info_tb");
				userSqlMap.put("sql", "insert into user_info_tb (id,nickname,strid,mobile,reg_time,comid,auth_flag,recom_code,media) " +
						"values(?,?,?,?,?,?,?,?,?)");
				userSqlMap.put("values", new Object[]{key,"����","zlduser"+key,mobile,ntime,0,4,uin,999});
				sqlList.add(userSqlMap);
				//������Ϣ
				Map<String, Object> carSqlMap = new HashMap<String, Object>();
				carSqlMap.put("sql", "insert into car_info_tb(uin,car_number,create_time) values(?,?,?)");
				carSqlMap.put("values", new Object[]{key,carNumber,curTime});
				sqlList.add(carSqlMap);
				//�Ƽ���Ϣ
				Map<String, Object> recomSqlMap = new HashMap<String, Object>();
				recomSqlMap.put("sql", "insert into recommend_tb (pid,nid,type,state,create_time) values(?,?,?,?,?)");
				recomSqlMap.put("values", new Object[]{uin,key,0,0,ntime});
				sqlList.add(recomSqlMap);
				
				boolean ret = daService.bathUpdate(sqlList);
				if(!ret){
					return "-2";
				}else {//��������30Ԫͣ��ȯ
					//�Ƽ��������շ�Ա��1��
					//logService.updateScroe(5, uin, comId);
					int result=publicMethods.backNewUserTickets(ntime, key);// daService.bathInsert(tsql, values, new int[]{4,4,4,4,4});
					if(result==0){
						/*String bsql = "insert into bonus_record_tb (bid,ctime,mobile,state,amount) values(?,?,?,?,?) ";
						Object [] values = new Object[]{999,ntime,mobile,0,10};//�Ǽ�Ϊδ��ȡ�������¼ʱд��ͣ��ȯ���ж��Ƿ��Ǻ�������
						logger.error(">>>>>>>>�շ�Ա�Ƽ�����("+mobile+")����30Ԫͣ��ȯ��д������¼����¼ʱ������"+daService.update(bsql,values));*/
					
					}
					int	eb = daService.update("insert into user_profile_tb (uin,low_recharge,limit_money,auto_cash," +
							"create_time,update_time) values(?,?,?,?,?,?)", 
							new Object[]{key,10,25,1,ntime,ntime});
					
					logger.error("�˻�:"+uin+",�ֻ���"+mobile+",��ע���û�(�����շ�Ա�Ƽ�)��д����ͣ��ȯ"+result+"��,�Զ�֧��д�룺"+eb);
					String mesg ="��ʵ���ͣ���ѣ���������ͣ������ͣ���������Żݣ�8ԪǮͣ5�γ������ص�ַ�� http://t.cn/RZJ4UAv ��ͣ������";
					SendMessage.sendMultiMessage(mobile, mesg);
				}
			}
		}else {
			return  "0";
		}
		//http://127.0.0.1/zld/collectorrequest.do?action=reguser&token=6ed161cde6c7149de49d72719f2eb39b&mobile=15801482645&carnumber=123456
		return  "1";
	}

	private String uploadll(HttpServletRequest request, Long uin, Long comId,Long authFlag) {
		Long ntime = System.currentTimeMillis()/1000;
		authFlag = daService.getLong("select auth_flag from user_info_tb where id =? ", new Object[]{uin});
		Double lon = RequestUtil.getDouble(request, "lon", 0d);
		Double lat = RequestUtil.getDouble(request, "lat", 0d);
		if(lat==0||lon==0){
			return "0";
		}else if(comId==null||comId<1){
			return "0";
		}
		Map comMap = daService.getMap("select longitude,latitude from com_info_Tb where id =? ", new Object[]{comId});
		Integer isOnseat = 0;
		Double distance =1000.0;
		if(comMap!=null&&comMap.get("longitude")!=null){
			Double lon1 = Double.valueOf(comMap.get("longitude")+"");
			Double lat1 = Double.valueOf(comMap.get("latitude")+"");
			distance = StringUtils.distance(lon, lat, lon1, lat1);
			if(distance<500){//�ڳ���500�׷�Χ��ʱ����Ϊ��λ��
				isOnseat = 1;
			}
			logger.error(">>>>>parkuser distance,uin:"+uin+",dis:"+distance+",authflag:"+authFlag);
		}
		//�����շ�Ա��λ��Ϣ��23��ʾ��λ
		if(authFlag!=13){//����Ա�ϴ�����״̬
			daService.update("update user_info_tb set online_flag =? where id=? ", new Object[]{22+isOnseat,uin});
		}
		//д��λ���ϴ���־ 
		int result = daService.update("insert into user_local_tb (uid,lon,lat,distance,is_onseat,ctime) values(?,?,?,?,?,?)",
				new Object[]{uin,lon,lat,distance,isOnseat,ntime});
		Long count = daService.getLong("select count(id) from user_info_Tb where comid =? and online_flag=? ", new Object[]{comId,23});
		if(count>0){//���շ�Ա��λ,���³����Ƿ����շ�Ա��λ��־
			daService.update("update com_info_tb set is_hasparker=?, update_time=? where id = ? and is_hasparker=? ", new Object[]{1,ntime,comId,0});
		}else {
			daService.update("update com_info_tb set is_hasparker=?, update_time=? where id = ? and is_hasparker=?", new Object[]{0,ntime,comId,1});
		}
		//http://127.0.0.1/zld/collectorrequest.do?action=uploadll&token=aa9a48d2f41bb2722f29c8714cbc754c&lon=&lat=
		return ""+result;
	}

	private String editpBank(HttpServletRequest request, Long uin, Long comId) {

		Long id = RequestUtil.getLong(request, "id", -1L);
		String name =AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "name"));
		String card_number =AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "card_number"));
		String mobile =RequestUtil.processParams(request, "mobile");
		String bank_name =AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "bank_name"));
		String area =AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "area"));
		String bank_point =AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "bank_pint"));
		String userId =RequestUtil.processParams(request, "user_id");
		Integer atype = RequestUtil.getInteger(request, "atype", 0);//0���п���1֧������2΢��
		int result = 0;
		if(!card_number.equals("")&&!mobile.equals("")&&!bank_name.equals("")&&id!=-1){
			result = daService.update("update com_account_tb set name=?,card_number=?,mobile=?,bank_name=?," +
					"area=?,bank_pint=?,atype=?,user_id=? where id = ? and type=? ",
					new Object[]{name,card_number,mobile,bank_name,area,bank_point,atype,userId,id,0});
		}
		//http://127.0.0.1/zld/collectorrequest.do?action=editpbank&token=aa9a48d2f41bb2722f29c8714cbc754c
		//&name=&card_number=&mobile=&bank_name=&area=&bank_point=&atype=&note=user_id=&id=
		return  result+"";
	}

	private String addParkBank(HttpServletRequest request, Long uin, Long comId) {
		String name =AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "name"));
//		Long uin =RequestUtil.getLong(request, "uin",-1L);
		String card_number =AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "card_number"));
		String mobile =RequestUtil.processParams(request, "mobile");
		String userId =RequestUtil.processParams(request, "user_id");
		String bank_name =AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "bank_name"));
		String area =AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "area"));
		String bank_point =AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "bank_pint"));
		int result = 0;
		if(!card_number.equals("")&&!mobile.equals("")&&!bank_name.equals("")){
			result = daService.update("insert into com_account_tb (comid,uin,name,card_number,mobile," +
					"bank_name,atype,area,bank_pint,type,state,user_id)" +
					" values(?,?,?,?,?,?,?,?,?,?,?,?)",
					new Object[]{comId,uin,name,card_number,mobile,bank_name,0,area,bank_point,0,0,userId});
		}
		//http://127.0.0.1/zld/collectorrequest.do?action=addparkbank&token=aa9a48d2f41bb2722f29c8714cbc754c
		//&name=&card_number=&mobile=&bank_name=&area=&bank_point=&atype=&note=
		logger.error(result);
		return  result+"";
	}

	private String withdraw(HttpServletRequest request, Long uin,Long comId) {
		Long ntime = System.currentTimeMillis()/1000;
		String result ="";
		Double money = RequestUtil.getDouble(request, "money", 0d);
		Long count = daService.getLong("select count(*) from park_account_tb where comid= ? and create_time>? and type=?  ", 
				new Object[]{comId,TimeTools.getToDayBeginTime(),1}) ;
		if(count>2){//ÿ��ֻ������
			result= "{\"result\":-2,\"times\":"+count+"}";
			return result;
		}
		
		List<Map> accList = daService.getAll("select id,type from com_account_tb where comid =? and type in(?,?) and state =? order by id desc",
				new Object[]{comId,0,2,0});
		Long accId = null;
		Integer type =0;
		if(accList!=null&&!accList.isEmpty()){
			accId = null;
			for(Map m: accList){
				type = (Integer)m.get("type");
				if(type!=null&&type==2){
					accId =  (Long)m.get("id");	
					break;
				}
			}
			if(accId==null)
				accId=(Long)accList.get(0).get("id");
		}else{
			//û�����������˻�
			result=  "{\"result\":-1,\"times\":0}";
			return result;
		}
		//���ֲ���
		boolean isupdate =false;
		if(money>0){
			Map userMap = daService.getMap("select money from com_info_Tb where id=? ", new Object[]{comId});
			//�û����
			Double balance =StringUtils.formatDouble(userMap.get("money"));
			if(money<=balance){//���ֽ��������
				//�۳��ʺ����//д���������
				List<Map<String, Object>> sqlList = new ArrayList<Map<String,Object>>();
				Map<String, Object> userSqlMap = new HashMap<String, Object>();
				userSqlMap.put("sql", "update com_info_Tb set money = money-? where id= ?");
				userSqlMap.put("values", new Object[]{money,comId});
				Map<String, Object> withdrawSqlMap = new HashMap<String, Object>();
				withdrawSqlMap.put("sql", "insert into withdrawer_tb  (comid,amount,create_time,acc_id,uin,wtype) values(?,?,?,?,?,?)");
				withdrawSqlMap.put("values", new Object[]{comId,money,ntime,accId,uin,type});
				Map<String, Object> moneySqlMap = new HashMap<String, Object>();
				moneySqlMap.put("sql", "insert into park_account_tb (comid,amount,create_time,type,remark,uid,source) values(?,?,?,?,?,?,?)");
				moneySqlMap.put("values", new Object[]{comId,money,ntime,1,"����",uin,5});
				sqlList.add(userSqlMap);
				sqlList.add(withdrawSqlMap);
				sqlList.add(moneySqlMap);
				isupdate = daService.bathUpdate(sqlList);
			}
			if(isupdate)
				result="{\"result\":1,\"times\":"+count+"}";
			else {
				result="{\"result\":0,\"times\":"+count+"}";
			}
		}
		return result;
	}

	/**
	 * //�˻���ϸ
	 * @param request
	 * @param comId
	 * @return
	 */
	private String getpaDetail(HttpServletRequest request, Long comId) {

		Integer pageNum = RequestUtil.getInteger(request, "page", 1);
		Integer pageSize = RequestUtil.getInteger(request, "size", 20);
		Long stype=RequestUtil.getLong(request, "stype", -1L);//0:���룬1����
		String sql = "select create_time ,remark r,amount money,type mtype  from park_account_tb where comid=?";
		String countSql = "select count(id)  from park_account_tb where comid=?";
		List<Object> params = new ArrayList<Object>();
		params.add(comId);
		
		if(stype>-1){
			if(stype==1){//����
				sql +=" and type=? ";
				countSql +=" and type=? ";
				params.add(stype);
			}else {//�����ͣ�������� 
				sql +=" and type in(?,?) ";
				countSql +=" and type in(?,?) ";
				params.add(stype);
				params.add(2L);
			}
		}
		Long count= daService.getCount(countSql, params);
		List pamList = null;//daService.getPage(sql, null, 1, 20);
		if(count>0){
			pamList = daService.getAll(sql+" order by id desc ", params, pageNum, pageSize);
		}
		
		if(pamList!=null&&!pamList.isEmpty()){
			for(int i=0;i<pamList.size();i++){
				Map map = (Map)pamList.get(i);
				Integer type = (Integer)map.get("mtype");
				String remark = (String)map.get("r");
				if(type==0){
					if(remark.indexOf("_")!=-1){
						map.put("note", remark.split("_")[0]);
						map.put("target", remark.split("_")[1]);
					}
				}else if(type==1){
					map.put("note", "����");
					map.put("target", "���п�");
				}else if(type==2){
					map.put("note", "����");
					map.put("target", "ͣ����");
					map.put("mtype", 1);
				}
				map.remove("r");
			}
		}
		String reslut =  "{\"count\":"+count+",\"info\":"+StringUtils.createJson(pamList)+"}";
		return reslut;
	}

	/**һ����ѯ
	 * @param request
	 * @param uin
	 * @param comId
	 * @return
	 */
	private String corder(HttpServletRequest request, Long comId) {
		Long ntime = System.currentTimeMillis()/1000;
		/*
		 * ������Ŀǰͣ�˶��ٳ�������ǰ����������
			�������볡��������������ʷ����������
			�����Ѿ��յ�����������ʷ�����ܽ�
		 */
		Long btime = TimeTools.getToDayBeginTime();
		Long etime = ntime;
		Long ccount =0L;//��ǰ������
		int ocount =0;//�ѽ��㶩����
		Long tcount = 0L;//���յ�ǰ������
		Double total =0d;
		List<Map<String, Object>> orderList = daService.getAll("select  total,state from order_tb where create_time>? and comid=? and end_time between ? and ? and state=? ",
				new Object[]{ntime-30*86400,comId,btime,etime,1});
		if(orderList!=null){
			ocount=orderList.size();
			for(Map<String,Object> map: orderList){
				total += Double.valueOf(map.get("total")+"");
			}
		}
		ccount = pService.getLong("select count(*) from order_tb where create_time>? and  comid=? and state=? ",
				new Object[]{ntime-30*86400,comId,0});
		tcount = pService.getLong("select count(1) from order_tb where create_time between ? and ? and comid=? and state=? "
				, new Object[]{btime,etime,comId,0});
		String result = "{\"ccount\":\""+ccount+"\",\"ocount\":\""+ocount+"\",\"tcount\":\""+tcount+"\",\"total\":\""+StringUtils.formatDouble(total)+"\"}";		
		//test:http://127.0.0.1/zld/collectorrequest.do?action=corder&token=b4e6727f914157c8745f6f2c023c8c96
		return result;
	}

	/**
	 * ������Ϣ
	 * @param request
	 * @param comId
	 * @param infoMap
	 */
	private void comInfo(HttpServletRequest request,Long comId,
			Map<String, Object> infoMap) {

		Map comMap = daService.getPojo("select * from com_info_tb where id=?", new Object[]{comId});
		List<Map<String, Object>> picMap = daService.getAll("select picurl from com_picturs_tb where comid=? order by id desc limit ? ",
				new Object[]{comId,1});
		String picUrls = "";
		if(picMap!=null&&!picMap.isEmpty()){
			for(Map<String, Object> map : picMap){
				picUrls +=map.get("picurl")+";";
			}
			if(picUrls.endsWith(";"))
				picUrls = picUrls.substring(0,picUrls.length()-1);
		}
		if(comMap!=null&&comMap.get("id")!=null){
			String mobile = (String)comMap.get("mobile");
			String phone = (String)comMap.get("phone");
			Integer city = (Integer)comMap.get("city");
			if(phone==null||phone.equals(""))
				phone = mobile;
			Map priceMap = getPriceMap(comId);
			String timeBetween = "";
			Double price = 0d;
			if(priceMap!=null){
				Integer payType = (Integer)priceMap.get("pay_type");
				if(payType==0){
					Integer start = (Integer)priceMap.get("b_time");
					Integer end = (Integer)priceMap.get("e_time");
					if(start<10&&end<10)
						timeBetween = "0"+start+":00-0"+end+":00";
					else if(start<10&&end>9){
						timeBetween = "0"+start+":00-"+end+":00";
					}else if(start>9){
						timeBetween = start+":00-"+end+":00";
					}
				}else {
					timeBetween = "00:00-24:00";
				}
				if(priceMap.get("price")!=null)
					price = Double.valueOf(priceMap.get("price")+"");
			}
			Integer parkType = (Integer)comMap.get("parking_type");
			parkType = parkType==null?0:parkType;
			String ptype = "����";
			if(parkType==1)
				ptype="����";
			else if(parkType==2){
				ptype="ռ��";
			}
			Integer stopType = (Integer)comMap.get("stop_type");
			String sType = "ƽ������";
			if(stopType==1)
				sType="��������";
			infoMap.put("name", comMap.get("company_name"));
			infoMap.put("address", comMap.get("address"));
			infoMap.put("parkingtotal", comMap.get("parking_total"));
			infoMap.put("parktype",ptype);
			infoMap.put("phone", phone);
			infoMap.put("timebet", timeBetween);
			infoMap.put("price", price);
			infoMap.put("stoptype", sType);
			infoMap.put("service", "�˹�����");
			infoMap.put("id", comId);
			infoMap.put("resume", comMap.get("resume")==null?"":comMap.get("resume"));
			infoMap.put("longitude", comMap.get("longitude"));
			infoMap.put("latitude", comMap.get("latitude"));
			infoMap.put("isfixed", comMap.get("isfixed"));
			infoMap.put("picurls",picUrls);
			List<Map<String, Object>> carTypeList = commonMethods.getCarType(comId);
			String carTypes = StringUtils.createJson(carTypeList);
			carTypes = carTypes.replace("value_no", "id").replace("value_name", "name");
			infoMap.put("car_type", comMap.get("car_type"));
			infoMap.put("allCarTypes", carTypes);
			infoMap.put("passfree", comMap.get("passfree"));
			infoMap.put("ishdmoney", comMap.get("ishdmoney"));
			infoMap.put("ishidehdbutton", comMap.get("ishidehdbutton"));
			infoMap.put("issuplocal", 0);
			infoMap.put("fullset",comMap.get("full_set"));//��λ�����ܷ����
			infoMap.put("leaveset",comMap.get("leave_set"));//����ʶ��ʶ��̧������  ���е��¿�����û���շѣ����շѣ���
			infoMap.put("liftreason",getLiftReason(comId));
			List list = daService.getAll("select id as value_no,name as value_name from free_reasons_tb where comid=? order by sort , id desc ", new Object[]{comId});
			infoMap.put("freereasons",list);
			String swith=publicMethods.getCollectMesgSwith();
			if("1".equals(swith)){
				if(city!=null&&city==110000)//0605��֪ͨ�������շ�Ա
					infoMap.put("mesgurl", "collectmesg.png");
				else {//֪ͨ����������շ�Ա
					infoMap.put("mesgurl", "collectmesg_jn.png");
				}
			}
		}else {
			infoMap.put("info", "token is invalid");
		}
	}

	private String getLiftReason(Long comid) {
		String reason = CustomDefind.getValue("LIFTRODREASON"+comid);
		String ret = "[";
		if(reason!=null){
			String res[] = reason.split("\\|");
			for(int i=0;i<res.length;i++){
				ret+="{value_no:"+i+",value_name:\""+res[i]+"\"},";
			}
		}
		if(ret.endsWith(","))
			ret = ret.substring(0,ret.length()-1);
		ret +="]";
		return ret;
	}
	
	/**
	 * ��ѷ���
	 * @param request
	 * @param uin
	 * @param comId
	 * @return
	 */
	private String freeOrder(HttpServletRequest request, Long uin, Long comId) {
		Long ntime = System.currentTimeMillis()/1000;
		Long orderId = RequestUtil.getLong(request, "orderid", -1L);
		Long out_passid = RequestUtil.getLong(request, "passid", -1L);//����ͨ��id
		Long isPolice = RequestUtil.getLong(request, "isPolice", -1L);//�Ƿ������
		Long freereasons = RequestUtil.getLong(request, "freereasons", -1L);//���ԭ��
		int result =0;
		if(orderId != -1){
			logger.error("�շ�Ա��"+uin+"�Ѷ�����"+orderId+"��Ϊ��ѷ���:"+freereasons);
			if(isPolice==1){
				result = daService.update("update order_tb set total=?,state=?,end_time=?,out_passid=?,uid=?,isclick=?,freereasons=? where id=? ",
						new Object[]{0,1,ntime,out_passid,uin,0,freereasons,orderId});
			}else{
				//����Ӧ�ռ۸�
				Map map = daService.getPojo("select * from order_tb where id=? ", new Object[]{orderId});
				Integer pid = (Integer)map.get("pid");
				Integer car_type = (Integer)map.get("car_type");//0��ͨ�ã�1��С����2����
				Long start= (Long)map.get("create_time");
				Long end =  ntime;
				Double total  = 0d;
				if(map.get("end_time") != null){
					end = (Long)map.get("end_time");
				}
				Map ordermap = commonMethods.getOrderInfo(orderId, -1L, end);
				total = Double.valueOf(ordermap.get("aftertotal") + "");
				String sql = "update order_tb set total=?,state=?,end_time=?,pay_type=?,out_passid=?,uid=?,freereasons=? where id=? ";
				Object []values = new Object[]{total,1,ntime,8,out_passid,uin,freereasons,orderId};
				Integer isClick = (Integer)map.get("isclick");
				if(isClick==null||isClick!=1){
					sql ="update order_tb set total=?,state=?,end_time=?,pay_type=?,out_passid=?,uid=?,freereasons=?,isclick=? where id=? ";
					values = new Object[]{total,1,ntime,8,out_passid,uin,freereasons,0,orderId};
				}
				result = daService.update(sql,values);
			}
			
			int r = daService.update("update parkuser_cash_tb set amount=? where orderid=? and type=? ",
					new Object[] { 0, orderId, 0 });
			logger.error("freeorder>>>>�ֽ��շ���ϸ��Ϊ0��orderid:"+orderId+",r:"+r);
		}
		return result+"";
	}
	
	private String manuPayPosOrder(HttpServletRequest request, Long uid, Integer payType, Long groupId){
		try {
			Long orderId = RequestUtil.getLong(request, "orderid", -1L);
			Double total = RequestUtil.getDouble(request, "total", 0d);
			String imei  =  RequestUtil.getString(request, "imei");
			Integer version = RequestUtil.getInteger(request, "version", 0);//�汾��2.0���Ϸ���json����
			String nfc_uuid = RequestUtil.processParams(request, "uuid");//ˢ��֧���Ŀ�Ƭ���
			Integer bindcard = RequestUtil.getInteger(request, "bindcard", 0);//0:�ͻ��˵����󶨳����ֻ��ŵ���ʾ�� 1����������ʾ��ֱ��ˢ��Ԥ��
			Long endtime = RequestUtil.getLong(request, "endtime", -1L);//��������ʱ�䣬�����ɽ������ʱ��
			//-----------------------------��������---------------------------------//
			Integer workid = RequestUtil.getInteger(request, "workid",0);//�������
			Long berthorderid = RequestUtil.getLong(request, "berthorderid", -1L);
			//-----------------------------�����߼� --------------------------------//
			ManuPayPosOrderFacadeReq req = new ManuPayPosOrderFacadeReq();
			req.setOrderId(orderId);
			req.setMoney(total);
			req.setImei(imei);
			req.setUid(uid);
			req.setVersion(version);
			req.setPayType(payType);
			req.setNfc_uuid(nfc_uuid);
			req.setBindcard(bindcard);
			req.setGroupId(groupId);
			req.setEndTime(endtime);
			ManuPayPosOrderResp resp = payOrderFacade.manuPayPosOrder(req);
			if(resp != null){
				JSONObject object = JSONObject.fromObject(resp);
				return object.toString();
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return "{\"result\":\"-1\",\"errmsg\":\"����ʧ��!\"}";
	}
	
	private String orderCash(HttpServletRequest request, String out, Long uin, Long groupId) {
		String result="";
		Long orderId = RequestUtil.getLong(request, "orderid", -1L);
		Double total = RequestUtil.getDouble(request, "total", 0d);
		String imei  =  RequestUtil.getString(request, "imei");
		Integer version = RequestUtil.getInteger(request, "version", 0);//�汾��2.0���Ϸ���json����
		Integer workid = RequestUtil.getInteger(request, "workid",0);//�������
		Long berthorderid = RequestUtil.getLong(request, "berthorderid", -1L);
		Long endtime = RequestUtil.getLong(request, "endtime", -1L);
		
		if(version > 1340){
			return manuPayPosOrder(request, uin, 0, groupId);//�ֽ�֧��
		}
		logger.error("ordercash  payOrder >>>>:orderid:"+orderId+",total:"+total+",uin:"+uin+",berthorder:"+berthorderid+",endtime:"+endtime);
		if(orderId!=-1){
			result = payOrder(orderId, total, uin, workid, out, imei, version,0,berthorderid,endtime);
		}
		logger.error(">>>>ordercash �շ�Ա�ֽ���㣬����:"+result);
		return result;
	}

	/**
	 * �ֽ���㶩��
	 * @param orderId
	 * @param total
	 * @param uin
	 * @param workid
	 * @param out
	 * @param imei
	 * @param version
	 * @param isEsc 1�����ӵ���0��ͨ����
	 * @return
	 */
	private String payOrder(Long orderId,Double total,Long uin,Integer workid,String out,
			String imei,Integer version,Integer isEsc,Long berthorderid,Long endtime){
		Long ntime = System.currentTimeMillis()/1000;
		logger.error("payOrder isEsc:"+isEsc);
		Map<String, Object> infoMap = new HashMap<String, Object>();
		String result = "";
		Map orderMap = daService.getPojo("select * from order_tb where id=?", new Object[]{orderId});
		Integer state = (Integer)orderMap.get("state");
		if(state!=null&&state==1){//�ѽ��㣬����
			return "{\"result\":\"-2\",\"errmsg\":\"�����ѽ���!\"}";
		}
		Double bakMoney=0.0;
		Double prepay = 0.0;
		if(endtime>0){
			ntime = endtime;
		}
		Long create_time = (Long)orderMap.get("create_time");
		berthorderid = commonMethods.getBerthOrderId(orderId);
		Long end_time = commonMethods.getOrderEndTime(berthorderid, uin, ntime);
		if(orderMap.get("prepaid")!=null) {
			prepay=StringUtils.formatDouble(orderMap.get("prepaid"));
			Double prePay = StringUtils.formatDouble(orderMap.get("total"));
			Integer payType =(Integer)orderMap.get("pay_type");
			Long userId = (Long)orderMap.get("uin");//�Ȳ�һ���Ƿ���ע�ᳵ����pos���շ�һ��û��ע��ĳ���
			if(userId!=null&&userId>0&&prePay>0&&payType!=null&&payType==2){//��ע�ᳵ���������˿�
				boolean _result = prepayRefund(orderMap,prePay);
				//Ԥ֧������˿�
				logger.error("payOrder �ֽ����Ԥ�����˿�:"+_result+",orderid:"+orderId);
			}
		}
		Long berthnumber =  (Long)orderMap.get("berthnumber");
		List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
		//���¶���״̬���շѳɹ�
		Map<String, Object> orderSqlMap = new HashMap<String, Object>();
		Map<String, Object> cashsqlMap =new HashMap<String, Object>();
		bakMoney = StringUtils.formatDouble(prepay-total);
		if(isEsc==0){
			if(state==2){//��Ϊδ�ɺ󲻿ɵ�����ͨ��������
				return "{\"result\":\"-1\",\"errmsg\":\"����Ϊδ�ɲ�����������!\"}";
			}
			orderSqlMap.put("sql", "update order_tb set state=?,total=?,end_time=?,pay_type=?,imei=?,out_uid=? where id=?");
			orderSqlMap.put("values", new Object[]{1,total,end_time,1,imei,uin,orderId});
			// target integer DEFAULT 0, -- �����ֶβ�����type=1��0��ͣ���ѣ���Ԥ������1��Ԥ��ͣ���ѣ�2��Ԥ���˿Ԥ�������3��Ԥ�����ɣ�Ԥ�����㣩��4��׷��ͣ����
			Integer target = 3;//Ԥ�����ɣ�Ԥ�����㣩
			Double money = bakMoney;
			Integer ctype=0;//����
			if(bakMoney>0){
				target = 2;//Ԥ���˿Ԥ�����
				ctype=1;//֧��
			}else 
				money = StringUtils.formatDouble(total-prepay);
			Long c = pService.getLong("select count (*) from parkuser_cash_tb where orderid=? ", new Object[]{orderId}) ;
			if(c>0&&prepay==0){//�Ѵ��� �����¼�¼
				cashsqlMap.put("sql", "update  parkuser_cash_tb set uin=?,amount=?,type=?,create_time=?,target=?,ctype=? where orderid =? ");
				cashsqlMap.put("values",  new Object[]{uin,money,0,end_time,target,ctype,orderId});
			}else {
				cashsqlMap.put("sql", "insert into parkuser_cash_tb(uin,amount,type,orderid,create_time,target,ctype) values(?,?,?,?,?,?,?)");
				cashsqlMap.put("values",  new Object[]{uin,money,0,orderId,end_time,target,ctype});
			}
			bathSql.add(cashsqlMap);
		}else {//�ǽ����ӵ� �����޸Ľ���ʱ��
			orderSqlMap.put("sql", "update order_tb set state=?,total=?,pay_type=?,imei=?,out_uid=? where id=?");
			orderSqlMap.put("values", new Object[]{1,total,1,imei,uin,orderId});
			// target integer DEFAULT 0, -- �����ֶβ�����type=1��0��ͣ���ѣ���Ԥ������1��Ԥ��ͣ���ѣ�2��Ԥ���˿Ԥ�������3��Ԥ�����ɣ�Ԥ�����㣩��4��׷��ͣ����
			Double money = bakMoney;
			if(bakMoney<0)
				money = StringUtils.formatDouble(total-prepay);
			Long c = pService.getLong("select count (*) from parkuser_cash_tb where orderid=? ", new Object[]{orderId}) ;
			if(c>0&&prepay==0){
				cashsqlMap.put("sql", "update parkuser_cash_tb set uin=?,amount=?,type=?,create_time=?,target=? where orderid=? ");
				cashsqlMap.put("values",  new Object[]{uin,money,0,end_time,4,orderId});
			}else {
				cashsqlMap.put("sql", "insert into parkuser_cash_tb(uin,amount,type,orderid,create_time,target) values(?,?,?,?,?,?)");
				cashsqlMap.put("values",  new Object[]{uin,money,0,orderId,end_time,4});
			}
			bathSql.add(cashsqlMap);
		}
		bathSql.add(orderSqlMap);
		
		//�ֽ���ϸ
		boolean b = daService.bathUpdate(bathSql);
		logger.error("payOrder >>>>ordreid:"+orderId+",b:"+b);
		if(b){
			infoMap.put("info", "�ֽ��շѳɹ�!");
			if(isEsc==1){//�ӵ�ʱ�������ӵ�
				int re = daService.update("update  no_payment_tb set state=?,pursue_uid=?,pursue_time=?,act_total=?  where order_id=? ", 
						new Object[]{1,uin,end_time,total,orderId});
				logger.error("payOrder �����ӵ���ret :"+re);
			}else {
				if(berthnumber!=null&&berthnumber>0){//���ݲ�λ�Ÿ��²�λ״̬
					int re =daService.update("update com_park_tb set state=?,order_id=? where id =? and order_id=?",  new Object[]{0,null,berthnumber,orderId});
					logger.error("payOrder �����ӵ����²�λ��ret :"+re+",berthnumber��"+berthnumber+",orderid:"+orderId);
				}
			}
			//���¶�����Ϣ�е�״̬ 
			daService.update("update order_message_tb set state=? where orderid=?", new Object[]{2,orderId});
			int r = 0;
			if(berthorderid>0){
				r = daService.update("update berth_order_tb set out_uid=?,order_total=?  where id=? ", new Object[]{uin,total,berthorderid});
			}
			logger.error("payOrder ���µشŶ�����ret :"+r);
			if(out.equals("json")){
				if(version>=2){//�汾2.0���ϣ�pos������ʱ�������Ԥ�ս�� ��Ҫ���ض����ٲ���ϸ
					String mesg = "�շѳɹ�";
					if(bakMoney>0)
						mesg = "Ԥ�ս�"+prepay+"Ԫ��Ӧ�ս�"+total+"Ԫ��Ӧ�˿"+bakMoney+"Ԫ";
					else if(bakMoney<0)
						mesg= "Ԥ�ս�"+prepay+"Ԫ��Ӧ�ս�"+total+"Ԫ��Ӧ���գ�"+StringUtils.formatDouble(total-prepay)+"Ԫ";
					
					String duration = StringUtils.getTimeString(create_time, end_time);
					result ="{\"result\":\"1\",\"errmsg\":\""+mesg+"\",\"duration\":\""+duration+"\"}";
				}else {
					result= "1";
				}
			}else {
				result = StringUtils.createXML(infoMap);
			}
			if(workid>0){//�й������ʱ����һ�´˶����ǲ������ϰ��ڼ�����ģ�������ǣ�Ҫ�Ѵ�ǰԤ�������빤����α�ǩ��ʱ�۳��ⲿ�֣����Ѷ������д�빤����α�
				Long count = daService.getLong("select count(ID) from work_detail_tb where workid=? and orderid=? ",
						new Object[]{workid,orderId});
				if(count<1){//���������ϰ��ڼ������
					int ret = 0;
					if(prepay>0){
						ret = daService.update("update parkuser_work_record_tb set history_money = history_money+? where id =? ",
								new Object[]{prepay,workid});
						logger.error("payOrder pos���ֽ���㣬�����ڱ���β����Ķ�����Ԥ�ս�"+prepay+"д���α�"+ret);
					}
					if(total>0){
						ret = daService.update("insert into work_detail_tb (uid,orderid,bid,workid,berthsec_id) values(?,?,?,?,?)", 
								new Object[]{uin,orderId,berthnumber,workid,orderMap.get("berthsec_id")});
						logger.error("payOrder pos���ֽ���㣬�����ڱ���β����Ķ�����������ţ�"+orderId+"����κţ�"+workid+",д���������"+ret);
					}
				}
			}
			
		}else {
			infoMap.put("info", "�ֽ��շ�ʧ��!");
			if(out.equals("json")){
				if(version>=2){//�汾2.0���ϣ�pos������ʱ�������Ԥ�ս�� ��Ҫ���ض����ٲ���ϸ
					result ="{\"result\":\"-1\",\"errmsg\":\"�ֽ��շ�ʧ��!\"}";
				}else {
					result= "-1";
				}
			}else {
				result = StringUtils.createXML(infoMap);
			}
		}
		logger.error("payOrder result:"+result);
		return result;
	}
	/**
	 * ��ѯ��ʷ����
	 * @param request
	 * @param comId
	 * @param out
	 * @return
	 */
	private String orderHistory(HttpServletRequest request,Long comId,String out, long groupId) {
		Map<String, Object> infoMap = new HashMap<String, Object>();
		String result="";
		Integer pageNum = RequestUtil.getInteger(request, "page", 1);
		Integer pageSize = RequestUtil.getInteger(request, "size", 20);
		Long _uid = RequestUtil.getLong(request, "uid", -1L);
		String day = RequestUtil.processParams(request, "day");
		String ptype = RequestUtil.getString(request, "ptype");//֧����ʽ
		List<Object> params = new ArrayList<Object>();
		params.add(0);
		params.add(comId);
//		Map com = daService.getMap( "select isshowepay from com_info_tb where id=? and isshowepay=?",new Object[]{comId,1});
//		if(com!=null&&com.get("isshowepay")!=null){
//			params.add(5);//ֱ������������
//		}else{
//			params.add(4);//ֱ������������
//		}
//		params.add(5);//�޸�Ŀǰ�¿���������ʾ
		String countSql = "select count(*) from order_tb where state>? and comid=? ";
		String sql = "select * from order_tb where state>?  and comid=?  ";//order by id desc ";
		String priceSql = "select sum(total) total,uid from order_tb where state>?  and comid=? ";
//		String countSql = "select count(*) from order_tb where state>? and comid=? and c_type<? ";
//		String sql = "select * from order_tb where state>?  and comid=? and c_type<?  ";//order by id desc ";
//		String priceSql = "select sum(total) total,uid from order_tb where state>?  and comid=? and c_type<? ";
		Long time = TimeTools.getToDayBeginTime();
		if(_uid!=-1){
			sql +=" and uid=? and end_time between ? and ?";
			countSql+=" and uid=? and end_time between ? and ?";
			priceSql +=" and uid=? and end_time between ? and ?";
			params.add(_uid);
			Long btime = time;
			if(day.equals("last")){
				params.add(btime-24*60*60);
				params.add(btime);
			}else {
				params.add(btime);
				params.add(btime+24*60*60);
			}
			if(ptype.equals("2")){//�ֻ�֧��
				sql +=" and pay_type=? ";
				countSql+=" and pay_type=? ";
				priceSql +=" and pay_type=? ";
				params.add(2);
			}else if(ptype.equals("3")){//����֧��
				sql +=" and pay_type=? ";
				countSql+=" and pay_type=? ";
				priceSql +=" and pay_type=? ";
				params.add(3);
			}else if(ptype.equals("4")){//ֱ������
				sql +=" and c_type=? ";
				countSql+=" and c_type=? ";
				priceSql +=" and c_type=? ";
				params.add(4);
			}
		}
		Long _total = pService.getCount(countSql,params);
		Object totalPrice = "0";
		Map pMap  = pService.getMap(priceSql+" group by uid ", params);
		if(pMap!=null&&pMap.get("total")!=null){
			totalPrice=pMap.get("total");
		}
		List<Map<String, Object>> list = pService.getAll(sql +" order by end_time desc ",// and create_time>?",
				params, pageNum, pageSize);
		logger.error("historyorder:"+_total+",totalprice:"+totalPrice);
		setPicParams(list);
		Integer ismonthuser = 0;//�ж��Ƿ��¿��û�
		if(list!=null&&list.size()>0){
			List<Map<String, Object>> infoMaps = new ArrayList<Map<String,Object>>();
			for(Map map : list){
				Map<String, Object> info = new HashMap<String, Object>();
				Long uid = (Long)map.get("uin");
				info.put("uin", map.get("uin"));
				String nfc_uuid = (String)map.get("nfc_uuid");
				String carNumber = "���ƺ�δ֪";
				if(map.get("car_number")!=null&&!"".equals((String)map.get("car_number"))){
					carNumber = map.get("car_number")+"";
				    if(StringUtils.isNumber(carNumber)){
				    	carNumber = "���ƺ�δ֪";
				    }
				}else {
					if(uid!=-1){
						carNumber = publicMethods.getCarNumber(uid);
					}
				}
				info.put("carnumber", carNumber);
				Long start= (Long)map.get("create_time");
				Long end= (Long)map.get("end_time");
				Double total =StringUtils.formatDouble(map.get("total"));// countPrice(start, end, comId);
				info.put("total", StringUtils.formatDouble(total));
				info.put("id", map.get("id"));
				info.put("state", map.get("state"));
				info.put("ptype", map.get("pay_type"));
				if(map.get("c_type")!=null&&Integer.valueOf(map.get("c_type")+"")==4){
					info.put("duration", "ֱ��֧��");
				}else {
					info.put("duration", "ͣ�� "+StringUtils.getTimeString(start,end));
				}
				info.put("btime", TimeTools.getTime_yyyyMMdd_HHmm(start*1000));
				//�ж��Ƿ����¿��û�
//				boolean b = publicMethods.isMonthUser(uid, comId);
				info.put("ctype", map.get("c_type"));
				if(Long.parseLong(map.get("c_type")+"")==5){
					ismonthuser = 1;//���¿��û�
				}else{
					ismonthuser = 0;//�����¿��û�
				}
				info.put("ismonthuser", ismonthuser);
				info.put("car_type", map.get("car_type"));
				//������Ƭ�������ã�HD����Ҫ��
				info.put("lefttop", map.get("lefttop"));
				info.put("rightbottom", map.get("rightbottom"));
				info.put("width", map.get("width"));
				info.put("height", map.get("height"));
				boolean is_card = commonMethods.cardUser(carNumber, groupId);
				if(is_card){
					info.put("is_card", 1);
				}else{
					info.put("is_card", 0);
				}
				infoMaps.add(info);
			}
			if(out.equals("json")){
				result = "{\"count\":"+_total+",\"price\":"+totalPrice+",\"info\":"+StringUtils.createJson(infoMaps)+"}";
			}else {
				result = StringUtils.createXML(infoMaps,_total);
			}
		}else {
			infoMap.put("info", "û�м�¼");
			result = StringUtils.createJson(infoMap);
		}
		return result;
	}
	//pos�����ɶ���
	private String posIncome(HttpServletRequest request, Long comId, Long groupId, Long uid) {
		//ȡ���ѻ����posi��Ϣ
		String carNumber=AjaxUtil.decodeUTF8(RequestUtil.getString(request, "carnumber"));
		/*List<String> posiList =memcacheUtils.doListStringCache("posorder_carnum_comid", null, null);
		if(posiList != null && posiList.contains(carNumber+comId)){
			return "{\"result\":\"0\",\"errmsg\":\"�������ɶ����������ĵȺ�\"}";
		}
		//����carNumber��comId
		if(posiList == null){
			posiList = new ArrayList<String>();
		}
		posiList.add(carNumber+comId);
		memcacheUtils.doListStringCache("posorder_carnum_comid", posiList, "update");*/
		try {
			Long curTime = System.currentTimeMillis()/1000;
			String imei  =  RequestUtil.getString(request, "imei");
			//String carNumber=AjaxUtil.decodeUTF8(RequestUtil.getString(request, "carnumber"));
			Long bid = RequestUtil.getLong(request, "bid", -1L);//��λ���
			Long orderId = RequestUtil.getLong(request, "orderid", -1L);//Ԥȡ�Ķ�����(������ӵĲ���)
			//--------------------------------Ԥ֧���Ĳ���---------------------------------------------//
			String nfc_uuid = RequestUtil.processParams(request, "uuid");//Ԥ֧���õ��Ĳ�����ˢ��Ԥ֧����Ƭ����Ψһ���
			Double prepay= RequestUtil.getDouble(request,  "prepay", 0d);//Ԥ�����
			Integer bindcard = RequestUtil.getInteger(request, "bindcard", 0);//Ԥ֧���õ��Ĳ�����0:�ͻ��˵����󶨳����ֻ��ŵ���ʾ�� 1����������ʾ��ֱ��ˢ��Ԥ��
			Integer payType = RequestUtil.getInteger(request, "paytype", 0);//Ԥ֧���õ��Ĳ�����0���ֽ�Ԥ�� 1��ˢ��Ԥ��
			//--------------------------------���õĲ���---------------------------------------------//
			Long workId = RequestUtil.getLong(request, "workid", -1L);//ǩ������(���ã���parkuser_work_record_tb��ѯ)
			Long berthid= RequestUtil.getLong(request, "berthid", -1L);//���ڲ�λ�α��(����,�Ӳ�λ�����ѯ)
			Long berthOrderId = RequestUtil.getLong(request, "berthorderid", -1L);//�ѷ�����֮ǰ�Ǵӿͻ��˴����������ݣ������ǴӺ�̨��ѯ
			Integer ismonthuser = RequestUtil.getInteger(request, "ismonthuser", 0);
			Integer carType = RequestUtil.getInteger(request, "car_type", 0);
			logger.error("uid:"+uid+",workId:"+workId+",berthid:"+berthid+",bid:"+bid+
					",ismonthuser:"+ismonthuser+",berthOrderId:"+berthOrderId+",orderId:"+orderId
					+",prepay:"+prepay+",imei:"+imei+",carNumber:"+carNumber+",comid:"+comId
					+ ",bindcard:"+bindcard+",payType:"+payType);
			
			GenPosOrderFacadeReq req = new GenPosOrderFacadeReq();
			req.setBerthId(bid);
			req.setBindcard(bindcard);
			req.setCarNumber(carNumber);
			req.setGroupId(groupId);
			req.setImei(imei);
			req.setNfc_uuid(nfc_uuid);
			req.setOrderId(orderId);
			req.setParkId(comId);
			req.setPayType(payType);
			req.setPrepay(prepay);
			req.setUid(uid);
			req.setCarType(carType);
			GenPosOrderFacadeResp resp = genOrderFacade.genPosOrder(req);
			logger.error(resp.toString());
			if(resp != null){
				JSONObject object = JSONObject.fromObject(resp);
				logger.error(object.toString());
				Integer result = object.getInt("result");
				Integer ctype = object.getInt("ctype");
				if(result==1&&(ctype!=null&&ctype!=5)){//�����ɹ�,�����¿�����ʱ��������������ƽ̨
					Long orderid = object.getLong("orderid");//������
					publicMethods.sendOrderToBolink(orderid, carNumber, comId);
				}
				//�ӻ���ɾ���Ѵ����carNumber��comId
				/*posiList.remove(carNumber+comId);
				memcacheUtils.doListStringCache("posorder_carnum_comid", posiList, "update");*/
				return object.toString();
			}
		} catch (Exception e) {
			logger.error(e);
			//�ӻ���ɾ���Ѵ����carNumber��comId
//			posiList.remove(carNumber+comId);
//			memcacheUtils.doListStringCache("posorder_carnum_comid", posiList, "update");
		}
		return "{\"result\":\"0\",\"errmsg\":\"�������������²���\"}";
	}


	private String currOrders(HttpServletRequest request,Long uin,Long comId,String out,
			Map<String, Object> infoMap) {
		Long ntime = System.currentTimeMillis()/1000;
		Integer pageNum = RequestUtil.getInteger(request, "page", 1);
		Integer pageSize = RequestUtil.getInteger(request, "size", 20);
		List<Object> params = new ArrayList<Object>();
		params.add(0);
		params.add(comId);
		Long _total = pService.getLong("select count(*) from order_tb where state=? and comid=? ", 
				new Object[]{0,comId});
		//��ͣ������
		List<Map<String,Object>> list = pService.getAll("select * from order_tb where state=? and comid=? order by id desc ",//and create_time>?",
				params, pageNum, pageSize);
		
		//�鲴������
		List<Map<String,Object>> csList =null;// daService.getAll("select c.id,c.state,c.buid,c.euid,c.car_number,c.btime,c.start_time,t.next_price,t.max_price  " +
				//"from carstop_order_tb c left join car_stops_tb t on c.cid = t.id where (c.buid=? and c.state in(?,?)) or (c.euid=? and c.state in(?,?)) ",
				//new Object[]{uin,1,2,uin,5,6});
		
		
		//logger.error("currentorder:"+_total);
		List<Map<String, Object>> infoMaps = new ArrayList<Map<String,Object>>();
		Double ptotal = 0d;
		Long end=ntime;
		if(list!=null&&list.size()>0){
			for(Map map : list){
				Map<String, Object> info = new HashMap<String, Object>();
				Long uid = (Long)map.get("uin");
				String carNumber = "���ƺ�δ֪";
				if(map.get("car_number")!=null&&!"".equals((String)map.get("car_number"))){
					carNumber = (String)map.get("car_number");
				}else {
					if(uid!=-1){
						carNumber = publicMethods.getCarNumber(uid);
					}
				}
				info.put("carnumber", carNumber);
				Long start= (Long)map.get("create_time");
				
				Integer pid = (Integer)map.get("pid");
				Integer car_type = (Integer)map.get("car_type");//0��ͨ�ã�1��С����2����
				end = ntime;
				if(pid>-1){
					info.put("total",publicMethods.getCustomPrice(start, end, pid));
				}else {
					info.put("total",publicMethods.getPrice(start, end, comId, car_type));
//					int isspecialcar = 0;
//					Map map1 = daService.getMap("select typeid from car_number_type_tb where car_number = ? and comid=?", new Object[]{carNumber, comId});
//					if(map1!=null&&map1.size()>0){
//						isspecialcar = 1;
//					}
//					infoMap.put("total",publicMethods.getPriceHY(start, end, comId, car_type, isspecialcar));
				}
				info.put("id", map.get("id"));
				info.put("type", "order");
				info.put("state","-1");
				info.put("duration", "��ͣ "+StringUtils.getTimeString(start,end));
				info.put("btime", TimeTools.getTime_yyyyMMdd_HHmm(start*1000));
				infoMaps.add(info);
			}
		}
		
		if(csList!=null&&!csList.isEmpty()){
			for(Map<String, Object> map : csList){
				Map<String, Object> info = new HashMap<String, Object>();
				info.put("id", map.get("id"));
				Long start = (Long)map.get("btime");
				Integer state = (Integer)map.get("state");
				if(state>2){
					info.put("duration", "��ͣ "+StringUtils.getTimeString(start,end));
					info.put("btime", TimeTools.getTime_yyyyMMdd_HHmm(start*1000));
					Double nprice = Double.valueOf(map.get("next_price")+"");//ʱ��
					Object tp = map.get("max_price");//��߼�
					Double tprice =-1d;
					if(tp!=null)
						tprice = Double.valueOf(tp.toString());
					Long h = StringUtils.getHour(start, end);
					Double total = StringUtils.formatDouble(h*nprice);
					if(tprice!=-1&&total>tprice)
						total = tprice;
					info.put("total",total);
				}else {
					start = (Long)map.get("start_time");
					info.put("total","0.0");
					info.put("btime",TimeTools.getTime_yyyyMMdd_HHmm(start*1000));
					info.put("duration","���ڽӳ�");
				}
				info.put("carnumber", map.get("car_number"));
				info.put("state", map.get("state"));
				infoMaps.add(info);
			}
		}
		Collections.sort(infoMaps,new OrderSortCompare());
		
		String result = "";
		ptotal = StringUtils.formatDouble(ptotal);
		if(out.equals("json")){
			result = "{\"count\":"+_total+",\"price\":"+ptotal+",\"info\":"+StringUtils.createJson(infoMaps)+"}";
		}else {
			result = StringUtils.createXML(infoMaps,_total);
		}
		return result;
	}


	private void orderDetail(HttpServletRequest request,Long comId, Long uid,
			Map<String, Object> infoMap) {
		//http://127.0.0.1/zld/collectorrequest.do?action=orderdetail&token=27ffc2991d9385e6c18690fc0a3e9899&orderid=18245855&out=json&r=15
		Long ntime = System.currentTimeMillis()/1000;
		Long orderId = RequestUtil.getLong(request, "orderid", -1L);
		//Long brethorderid = RequestUtil.getLong(request, "brethorderid", -1L);
		logger.error("orderDetail>>>orderId:"+orderId);
		//http://127.0.0.1:8080/zld/collectorrequest.do?action=orderdetail&token=6f56758f82c1ccf17d4519918339dc2c&orderid=826699&out=json
		if(orderId!=-1){
			Map orderMap = pService.getPojo("select o.*,c.cid from order_tb o left join com_park_tb c on c.order_id=o.id" +
					" where o.id=?", new Object[]{orderId});
			if(orderMap!=null&&!orderMap.isEmpty()){
				Long start= (Long)orderMap.get("create_time");
				Long curtime = ntime;
				Long brethorderid = commonMethods.getBerthOrderId(orderId);
				Long end = commonMethods.getOrderEndTime(brethorderid, uid, curtime);
				Integer car_type = (Integer)orderMap.get("car_type");//0��ͨ�ã�1��С����2����
				if(orderMap.get("end_time")!=null)
					end = (Long)orderMap.get("end_time");
				Integer state = (Integer)orderMap.get("state");
				String _state="δ����";
				
				if(state==1){
					_state="�ѽ���";
				}
				Long uin = (Long)orderMap.get("uin");
				Map userMap = daService.getMap("select mobile from user_info_Tb where id=?", new Object[]{uin});
				
				String mobile = "";
				if(userMap!=null&&userMap.get("mobile")!=null){
					mobile = userMap.get("mobile")+"";
				}
				if(orderMap!=null&&Integer.valueOf(orderMap.get("c_type")+"")==4){
					infoMap.put("showepay", "ֱ��֧��");
				}
				
				String carNumber =orderMap.get("car_number")+"";
				if(StringUtils.isNumber(carNumber)){
					carNumber = "���ƺ�δ֪";
				}
				if(carNumber.equals("null")||carNumber.equals("")){
					carNumber =publicMethods.getCarNumber(uin);
				}
				if("".equals(carNumber.trim())||"���ƺ�δ֪".equals(carNumber.trim()))
					carNumber ="null";
				if(orderMap.get("prepaid")!=null)
					infoMap.put("prepay", StringUtils.formatDouble(orderMap.get("prepaid")));
				Integer pid = (Integer)orderMap.get("pid");
				if(pid>-1){
					infoMap.put("total",publicMethods.getCustomPrice(start, end, pid));
				}else {
					int isspecialcar = 0;
					Map map = daService.getMap("select typeid from car_number_type_tb where car_number = ? and comid=?", new Object[]{carNumber, comId});
					if(map!=null&&map.size()>0){
						isspecialcar = 1;
					}
					logger.error(isspecialcar);
					infoMap.put("total",publicMethods.getPriceHY(start, end, comId, car_type, isspecialcar));
				}
				if(orderMap.get("c_type")!=null&&Integer.valueOf(orderMap.get("c_type")+"")==5){
					infoMap.put("total", StringUtils.formatDouble(0.0));
				}
				if(orderMap.get("state")!=null&&Integer.valueOf(orderMap.get("state")+"")==1){
					infoMap.put("total", StringUtils.formatDouble(orderMap.get("total")));
				}
				if(orderMap.get("c_type")!=null&&Integer.valueOf(orderMap.get("c_type")+"")==4){
					infoMap.put("total", StringUtils.formatDouble(orderMap.get("total")));
				}
				infoMap.put("orderid", orderId);
				infoMap.put("prepaymoney", orderMap.get("prepaid"));
				infoMap.put("begin", start);
				infoMap.put("end", end);
				infoMap.put("state",_state);
				infoMap.put("mobile", mobile);
				infoMap.put("car_type", orderMap.get("car_type"));
				infoMap.put("berthnumber", orderMap.get("berthnumber")==null?"":orderMap.get("berthnumber"));
				infoMap.put("park", orderMap.get("cid")==null?"":orderMap.get("cid"));
				infoMap.put("carnumber", carNumber);
				infoMap.put("duration", StringUtils.getTimeStringSenconds(start, end));
			}else {
				infoMap.put("info", "�޴˶�����Ϣ");
			}
		}else {
			infoMap.put("info", "�޴˶�����Ϣ");
		}
		logger.error(">>>>>>orderdetail ��"+infoMap);
	}

	private void setRemark(List<Map<String, Object>> list){
		if(list != null && !list.isEmpty()){
			for(Map<String, Object> map : list){
				if(map.get("remark") != null){
					String remark = (String)map.get("remark");
					remark = remark.split("_")[0];
					map.put("remark", remark);
				}
			}
		}
	}
	
	/**
	 * ��ѯ��λ��Ϣ
	 * @param comId
	 * @return
	 */
	private String getComParks(Long comId) {
		//���³�λ���Ѿ�����Ķ�������Ϊδռ��״̬
		//commonMethods.updateParkInfo(comId);
		List<Map<String, Object>> list = daService.getAll("select c.cid,c.state,o.id orderid,o.car_number,o.create_time btime,o.uin, o.end_time etime " +
				"from com_park_tb c left join order_tb o on c.order_id = o.id where c.comid=? order by c.id", new Object[]{comId});
		if(list!=null&&!list.isEmpty()){
			return StringUtils.createJson(list);
		}
		return "[]";
	}

	
	
	private void setCarNumber(List<Map<String, Object>> list){
		List<Object> uins = new ArrayList<Object>();
		for(Map<String, Object> map : list){
			uins.add(map.get("uin"));
		}
		if(!uins.isEmpty()){
			String preParams  ="";
			for(Object uin : uins){
				if(preParams.equals(""))
					preParams ="?";
				else
					preParams += ",?";
			}
			List<Map<String, Object>> resultList = new ArrayList<Map<String,Object>>();
			resultList = daService.getAllMap("select u.id,car_number from user_info_tb u left join car_info_tb c on u.id=c.uin where u.id in ("
									+ preParams + ")", uins);
			List<Object> binduins = new ArrayList<Object>();
			List<Object> nobinduins = new ArrayList<Object>();
			for(Map<String, Object> map : resultList){
				Long uin = (Long)map.get("id");
				if(!binduins.contains(uin)){
					for(Map<String, Object> map2 : list){
						Long id = (Long)map2.get("uin");
						if(uin.intValue() == id.intValue()){
							if(map.get("car_number") != null){
								map2.put("carnumber", map.get("car_number"));
							}
						}
					}
					binduins.add(uin);
				}
			}
		}
	}
	
	private void sendWXMsg(String[] ids, Map userMap,Map comMap,Integer money){
		Long exptime = TimeTools.getToDayBeginTime()+16*24*60*60;
		String exp = TimeTools.getTimeStr_yyyy_MM_dd(exptime * 1000);
		List<Object> uins = new ArrayList<Object>();
		List<Map<String, Object>> openids = new ArrayList<Map<String, Object>>();
		
		for(int i=0;i<ids.length; i++){
			Long uin = Long.valueOf(ids[i]);
			uins.add(uin);
		}
		if(!uins.isEmpty()){
			String preParams  ="";
			for(Object uin : uins){
				if(preParams.equals(""))
					preParams ="?";
				else
					preParams += ",?";
			}
			List<Map<String, Object>> resultList = new ArrayList<Map<String,Object>>();
			List<Object> binduins = new ArrayList<Object>();
			List<Object> nobinduins = new ArrayList<Object>();//�����˻�
			resultList = daService.getAllMap(
					"select id,wxp_openid from user_info_tb where id in (" + preParams + ") ", uins);
			for(Map<String, Object> map : resultList){
				Map<String, Object> map2 = new HashMap<String, Object>();
				Long uin = (Long)map.get("id");
				if(map.get("wxp_openid") != null){
					map2.put("openid", map.get("wxp_openid"));
					map2.put("bindflag", 1);
					openids.add(map2);
				}
				binduins.add(uin);
			}
			for(Object object: uins){
				if(!binduins.contains(object)){
					nobinduins.add(object);
				}
			}
			logger.error("sendWXMsg>>>�����˻���"+nobinduins.toString());
			if(!nobinduins.isEmpty()){
				preParams  ="";
				for(Object uin : nobinduins){
					if(preParams.equals(""))
						preParams ="?";
					else
						preParams += ",?";
				}
				resultList = daService.getAllMap(
						"select openid from wxp_user_tb where uin in (" + preParams + ") ", nobinduins);
				for(Map<String, Object> map : resultList){
					Map<String, Object> map2 = new HashMap<String, Object>();
					if(map.get("openid") != null){
						map2.put("openid", map.get("openid"));
						map2.put("bindflag", 0);
						openids.add(map2);
					}
				}
			}
			logger.error("sendWXMsg>>>:����Ϣ��openid:"+openids.toString());
			if(openids.size() > 0){
				for(Map<String, Object> map : openids){
					try {
						String openid = (String)map.get("openid");
						Integer bindflag = (Integer)map.get("bindflag");
						
						String url = "http://"+Constants.WXPUBLIC_REDIRECTURL+"/zld/wxpaccount.do?action=toticketpage&openid="+openid;
						if(bindflag == 0){
							url = "https://open.weixin.qq.com/connect/oauth2/authorize?appid="+Constants.WXPUBLIC_APPID+"&redirect_uri=http%3a%2f%2f"+Constants.WXPUBLIC_REDIRECTURL+"%2fzld%2fwxpaccount.do&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
						}
						Map<String, String> baseinfo = new HashMap<String, String>();
						List<Map<String, String>> orderinfo = new ArrayList<Map<String,String>>();
						String first = "��ϲ����շ�Ա"+userMap.get("nickname")+"("+userMap.get("id")+")���͵�"+comMap.get("company_name")+"ר��ȯ";
						String remark = "����鿴���飡";
						String remark_color = "#000000";
						baseinfo.put("url", url);
						baseinfo.put("openid", openid);
						baseinfo.put("top_color", "#000000");
						baseinfo.put("templeteid", Constants.WXPUBLIC_TICKET_ID);
						Map<String, String> keyword1 = new HashMap<String, String>();
						keyword1.put("keyword", "coupon");
						keyword1.put("value", money+"Ԫ");
						keyword1.put("color", "#000000");
						orderinfo.add(keyword1);
						Map<String, String> keyword2 = new HashMap<String, String>();
						keyword2.put("keyword", "expDate");
						keyword2.put("value", exp);
						keyword2.put("color", "#000000");
						orderinfo.add(keyword2);
						Map<String, String> keyword3 = new HashMap<String, String>();
						keyword3.put("keyword", "remark");
						keyword3.put("value", remark);
						keyword3.put("color", remark_color);
						orderinfo.add(keyword3);
						Map<String, String> keyword4 = new HashMap<String, String>();
						keyword4.put("keyword", "first");
						keyword4.put("value", first);
						keyword4.put("color", "#000000");
						orderinfo.add(keyword4);
						publicMethods.sendWXTempleteMsg(baseinfo, orderinfo);
					} catch (Exception e) {
						// TODO: handle exception
						e.printStackTrace();
					}
					
				}
			}
		}
	}
	
	private void setinfo(List<Map<String, Object>> list,Integer pageNum,Integer pageSize){
		List<Object> uids = new ArrayList<Object>();
		Integer sort = (pageNum - 1)*pageSize;//����
		for(Map<String, Object> map : list){
			Long uin = (Long)map.get("uin");
			uids.add(uin);
			
			sort++;
			map.put("sort", sort);
		}
		if(!uids.isEmpty()){
			String preParams  ="";
			for(Object uid : uids){
				if(preParams.equals(""))
					preParams ="?";
				else
					preParams += ",?";
			}
			List<Map<String, Object>> resultList = new ArrayList<Map<String,Object>>();
			resultList = daService.getAllMap(
							"select u.id,nickname,company_name from user_info_tb u,com_info_tb c where u.comid=c.id and u.id in ("
									+ preParams + ") ", uids);
			for(Map<String, Object> map : resultList){
				Long id = (Long)map.get("id");
				String nickname = null;
				String cname = null;
				if(map.get("nickname") != null && ((String)map.get("nickname")).length() > 0){
					nickname = ((String)map.get("nickname")).substring(0, 1);
					for(int i=1;i<((String)map.get("nickname")).length();i++){
						nickname += "*";
					}
				}
				if(map.get("company_name") != null && ((String)map.get("company_name")).length() > 0){
					cname = ((String)map.get("company_name")).substring(0, 1);
					cname += "****ͣ����";
				}
				for(Map<String, Object> map2: list){
					Long uid = (Long)map2.get("uin");
					if(id.intValue() == uid.intValue()){
						map2.put("nickname", nickname);
						map2.put("cname", cname);
					}
				}
			}
		}
	}
	private String autoUp(HttpServletRequest request,Long comId,Long uid) {
		Long ntime = System.currentTimeMillis()/1000;
		String carNumber = AjaxUtil.decodeUTF8(RequestUtil.getString(request, "carnumber"));
		String cardno = AjaxUtil.decodeUTF8(RequestUtil.getString(request, "cardno"));
		//�������̣����ݳ��Ʋ鶩����
		/*
		 * 1:�ж��������Ƿ���Ԥ֧�� ��
		 * 		������Ԥ���������Ƿ���㣬
		 * 			�������㣺���أ�{state:1,orderid,btime,etime,carnumber,duration,total}
		 * 			�������㣺����   {state:2,prefee,total,collect}
		 * 		������Ԥ�������Ƿ��ǻ�Ա
		 * 			������Ա ������Ƿ����
		 * 				�������㣬�Ƿ��Զ�����  
		 * 					�����ǣ����أ�{state:1,orderid,btime,etime,carnumber,duration,total}
		 * 					���������ֽ𷵻أ�{state:0,orderid,btime,etime,carnumber,duration,total}
		 * 				���������� �� ���ֽ� ���أ�{state:0,orderid,btime,etime,carnumber,duration,total}
		 * 			�����ǻ�Ա :���ֽ𣺷��أ�{state:0,orderid,btime,etime,carnumber,duration,total}
		 * 2���޶��������ɶ�����
		 * 		������Ա ������Ƿ����
		 * 				�������㣬�Ƿ��Զ�����  
		 * 					�����ǣ����أ�{state:1,orderid,btime,etime,carnumber,duration,total}
		 * 					���������ֽ𷵻أ�{state:0,orderid,btime,etime,carnumber,duration,total}
		 * 				���������� �� ���ֽ� ���أ�{state:0,orderid,btime,etime,carnumber,duration,total}
		 * 		�����ǻ�Ա :���ֽ𣺷��أ�{state:0,orderid,btime,etime,carnumber,duration,total}
		 *
		 */
		Double price = RequestUtil.getDouble(request, "price", 0d);
		//System.out.println(carNumber);
		String result = "{}";
		//���ɶ���������
		if(comId==null||uid==null||uid==-1||comId==-1){
			result="{\"state\":\"-3\",\"errmsg\":\"û��ͣ�������շ�Ա��Ϣ�������µ�¼!\"}";
			return result;
		}
		Long uin = -1L;
		if(!carNumber.equals("")){
			Map carMap = daService.getMap("select uin from car_info_tb where car_number=?", new Object[]{carNumber});
			if(carMap!=null)
				uin = (Long)carMap.get("uin");
		}
		boolean isvip = true;//��Ա
		if(uin==null||uin==-1) {
			result="{\"state\":\"-1\",\"errmsg\":\"����δע��!\",\"orderid\":\"\"}";
			isvip=false;
			uin = -1L;
		}
		//�鶩��:
		Map<String,Object> orderMap =null;
		Long orderId = null;
		boolean isOrder= false;
		if("".equals(carNumber)&&!"".equals(cardno)){//���ٵ�����������
			String uuid = comId+"_"+cardno;
			Long ncount  = daService.getLong("select count(*) from com_nfc_tb where nfc_uuid=? and state=?", 
					new Object[]{uuid,0});
			if(ncount==0){
				logger.error("����ͨˢ��...���ţ�"+uuid+",δע��....");
				result="{\"state\":\"-10\",\"errmsg\":\"����û��ע��!\",\"orderid\":\"-1\"}";
			}
			orderMap = pService.getMap("select * from order_tb where comid=? and nfc_uuid=? and state=? ", new Object[]{comId,uuid,0});
			if(orderMap==null||orderMap.isEmpty()){
				if(price<0){
					result="{\"state\":\"-2\",\"errmsg\":\"�۸񲻶�:"+price+"!\",\"orderid\":\"-1\"}";
				}else {
					//���ɶ���
					
					orderId = daService.getkey("seq_order_tb");
					int ret = daService.update("insert into order_tb (id,create_time,end_time,comid,uin,state,pay_type,c_type,uid,nfc_uuid,type,total) values(?,?,?,?,?,?,?,?,?,?,?,?)", 
							new Object[]{orderId,ntime,ntime+60,comId,uin,1,1,3,uid,uuid,2,0.0});
					if(ret!=1){//����д�����
						result="{\"state\":\"-4\",\"errmsg\":\"���ɶ���ʧ��!\",\"orderid\":\""+orderId+"\"}";
					}
					if(ncount>0)
						return "{\"state\":\"-11\",\"errmsg\":\"����δԤ֧��!\",\"orderid\":\"\"}";
				}
			}else {
				orderId = (Long)orderMap.get("id");
				Double prePay = StringUtils.formatDouble(orderMap.get("total"));
				uin = (Long)orderMap.get("uin");
				Long ouid = (Long)orderMap.get("uid");
				logger.error("orderid:"+orderId+",prepay:"+prePay+",uin:"+uin+",total:"+price);
				if(uid!=null&&ouid!=null&&!ouid.equals(uid)){
					daService.update("update order_tb set uid=? where id =? ", new Object[]{uid,orderId});
				}
				if(prePay>0){//��Ԥ֧�� 
					Integer ret = publicMethods.doPrePayOrder(orderMap, price);
					if(ret==1){//֧���ɹ�
						if(prePay>=price){//������
							orderMap = daService.getMap("select * from order_tb where id=? ", new Object[]{orderId});
							result=getThirdCardOrderInfo(orderMap);//"{\"state\":\"1\"}";//{state:1,orderid,btime,etime,carnumber,duration,total}
						}else {
							result="{\"state\":\"2\",\"prefee\":\""+prePay+"\",\"total\":\""+price+"\",\"collect\":\""+StringUtils.formatDouble((price-prePay))+"\"}";
						}
					}
				}else {
					if(!isvip){
						daService.update("update order_tb set state=? ,total=?,end_time=?,pay_type=? where id = ? ", new Object[]{1,price,ntime,1,orderId});
						if(ncount>0)
							return "{\"state\":\"-11\",\"errmsg\":\"����δԤ֧��!\",\"orderid\":\"\"}";
					}
				}
			}
			
		}else {//����ͨ���ƽ���
			orderMap = pService.getMap("select * from order_tb where comid=? and car_number=? and state=? ", new Object[]{comId,carNumber,0});
			if(orderMap!=null){//�ж���
				orderId = (Long)orderMap.get("id");
				Double prePay = StringUtils.formatDouble(orderMap.get("total"));
				logger.error("����ͨ>>>>orderid:"+orderId+",uin:"+uin+",prePay:"+prePay+",price:"+price+",isvip:"+isvip);
				uin = (Long)orderMap.get("uin");
				Long ouid = (Long)orderMap.get("uid");
				if(uid!=null&&ouid!=null&&!ouid.equals(uid)){
					daService.update("update order_tb set uid=? where id =? ", new Object[]{uid,orderId});
				}
				if(prePay>0){//��Ԥ֧�� 
					Integer ret = publicMethods.doPrePayOrder(orderMap, price);
					if(ret==1){//֧���ɹ�
						if(prePay>=price){//������
							orderMap = daService.getMap("select * from order_tb where id=? ", new Object[]{orderId});
							result=getOrderInfo(orderMap);//"{\"state\":\"1\"}";//{state:1,orderid,btime,etime,carnumber,duration,total}
						}else {
							result="{\"state\":\"2\",\"prefee\":\""+prePay+"\",\"total\":\""+price+"\",\"collect\":\""+StringUtils.formatDouble((price-prePay))+"\"}";
						}
					}
					return result;
				}else {//��Ԥ֧��
					isOrder= true;
				}
				if(!isvip){
					daService.update("update order_tb set state=? ,total=?,end_time=?,pay_type=? where id = ? ", new Object[]{1,price,ntime,1,orderId});
					return result;
				}
			}else{//�޶���
				//�鳵��
				if(price<0){
					result="{\"state\":\"-2\",\"errmsg\":\"�۸񲻶�:"+price+"!\",\"orderid\":\""+orderId+"\"}";
				}else {
					//���ɶ���
					orderId = daService.getkey("seq_order_tb");
					String sql = "insert into order_tb (id,create_time,comid,uin,state,c_type,uid,car_number,type) values(?,?,?,?,?,?,?,?,?)";
					Object [] values =new Object[]{orderId,ntime,comId,uin,0,3,uid,carNumber,1};
					if(uin==-1){//�ǻ�Ա
						sql ="insert into order_tb (id,create_time,comid,uin,state,total,end_time,pay_type,c_type,uid,car_number,type) values(?,?,?,?,?,?,?,?,?,?,?,?)";
						values =new Object[]{orderId,ntime,comId,uin,1,price,ntime+60,1,3,uid,carNumber,1};
					}
					int ret = daService.update(sql, values)	;
					if(ret==1){//������д��
						isOrder = true;
					}else {
						result="{\"state\":\"-4\",\"errmsg\":\"���ɶ���ʧ��!\",\"orderid\":\""+orderId+"\"}";
						return result;
					}
				}
			}
			if(isOrder&&uin!=-1){//�ж���Ҫ֧�� --->>>>
				//�����ͣ��ȯ
				Map tempMap = publicMethods.useTickets(uin, price, comId,uid,0);
				Long ticketId = null;
				if(tempMap!=null){
					ticketId = (Long)tempMap.get("id");
				}
				//�鵱ǰ����
				tempMap = daService.getMap("select * from order_tb where id =? ", new Object[]{orderId});
				
				//�Զ�֧������
				int isautopay = isAutoPay(uin,price);
				if(isautopay==-1){//����δ�����Զ�֧��
					result="{\"state\":\"-8\",\"errmsg\":\"����δ�����Զ�֧��!\",\"orderid\":\""+orderId+"\",\"carnumber\":\""+carNumber+"\",\"total\":\""+price+"\"}";
					daService.update("update order_tb set state=? ,total=?,pay_type=?,end_time=?  where id = ? ", new Object[]{1,price,1,ntime,orderId});
					return result;
				}else if(isautopay==-2){//���������Զ�֧���޶�
					result="{\"state\":\"-9\",\"errmsg\":\"���������Զ�֧���޶�!\",\"orderid\":\""+orderId+"\",\"carnumber\":\""+carNumber+"\",\"total\":\""+price+"\"}";
					daService.update("update order_tb set state=? ,total=?,pay_type=?,end_time=?  where id = ? ", new Object[]{1,price,1,ntime,orderId});
					return result;
				}
				//���㶩��
				int re = publicMethods.payOrder(tempMap, price, uin, 2,0,ticketId,null, -1L, uid);
				logger.info(">>>>>>>>>>>>�����˻�֧�� ��"+re+",orderid:"+orderId);
				if(re==5){//����ɹ�
					tempMap = daService.getMap("select * from order_tb where id =? ", new Object[]{orderId});
					result=getOrderInfo(tempMap);//"{\"state\":\"1\",\"errmsg\":\"����֧���ɹ�!\"}";//{state:1,orderid,btime,etime,carnumber,duration,total}
				}else{
					switch (re) {
					case -8://��֧���������ظ�֧��
						result="{\"state\":\"-5\",\"errmsg\":\"��֧���������ظ�֧��!\",\"orderid\":\""+orderId+"\"}";
						break;
					case -7://֧��ʧ��
						result="{\"state\":\"-6\",\"errmsg\":\"֧��ʧ��!\",\"orderid\":\""+orderId+"\"}";						
						break;
					case -12://����
						result="{\"state\":\"-7\",\"errmsg\":\"����!\",\"orderid\":\""+orderId+"\",\"carnumber\":\""+carNumber+"\",\"total\":\""+price+"\"}";
						daService.update("update order_tb set state=? ,total=?,pay_type=?,end_time=?  where id = ? ", new Object[]{1,price,1,ntime,orderId});
						break;
					default:
						result="{\"state\":\"-6\",\"errmsg\":\"֧��ʧ��!\",\"orderid\":\""+orderId+"\"}";						
						break;
					}
				}
			}
		}
		//Ԥ֧�����㣺result="{\"result\":\"2\",\"prefee\":\""+prefee+"\",\"total\":\""+money+"\",\"collect\":\""+(money-prefee)+"\"}";
		//����ɹ���{"total":"79.4","duration":"5�� 18Сʱ24����","carnumber":"��AFY123","etime":"10:38","state":"2","btime":"16:14","orderid":"786636"} 
		if(result.equals("{}"))
			result="{\"state\":\"-6\",\"errmsg\":\"֧��ʧ��!\",\"orderid\":\""+orderId+"\"}";
		logger.error(">>>>>>����ͨ:"+result);
		return result;
	}
	/**�Ƿ��Զ�֧��***/
	private int isAutoPay(Long uin, Double price) {
		//�鳵�����ã��Ƿ��������Զ�֧����û������ʱ��Ĭ��25Ԫ�����Զ�֧�� 
		Integer autoCash=1;
		Map upMap = daService.getPojo("select auto_cash,limit_money from user_profile_tb where uin =?", new Object[]{uin});
		Integer limitMoney =25;
		if(upMap!=null&&upMap.get("auto_cash")!=null){//�������Զ�֧������
			autoCash= (Integer)upMap.get("auto_cash");
			limitMoney = (Integer)upMap.get("limit_money");
			if(autoCash!=null&&autoCash==1){//�������Զ�֧��
				if(limitMoney==-1)//�����Ͻ��
					return 1;
				else if(price>limitMoney){//�����������Զ�֧���޶�
					return -2;
				}
			}else//�����˲��Զ�֧��
				return -1;
		}
		//����û���Զ�֧�����ã����ؿ�֧��
		return 1;
	}
	private String getOrderInfo(Map orderMap){
		Long btime = (Long)orderMap.get("create_time");
		Long etime = (Long)orderMap.get("end_time");
		String dur = StringUtils.getTimeString(btime,etime);
		String bt = TimeTools.getTime_yyyyMMdd_HHmm(btime*1000).substring(11);
		String et = TimeTools.getTime_yyyyMMdd_HHmm(etime*1000).substring(11);
		String ret = "{\"state\":\"1\",\"orderid\":\""+orderMap.get("id")+"\",\"btime\":\""+bt+"\",\"etime\":\""+et+"\"," +
				"\"carnumber\":\""+orderMap.get("car_number")+"\",\"duration\":\""+dur+"\",\"total\":\""+orderMap.get("total")+"\"}";
		return ret;
	}
	
	private String getThirdCardOrderInfo(Map orderMap){
		Long btime = (Long)orderMap.get("create_time");
		Long etime = (Long)orderMap.get("end_time");
		String dur = StringUtils.getTimeString(btime,etime);
		String bt = TimeTools.getTime_yyyyMMdd_HHmm(btime*1000).substring(11);
		String et = TimeTools.getTime_yyyyMMdd_HHmm(etime*1000).substring(11);
		String uuid = (String)orderMap.get("nfc_uuid");
		if(uuid!=null&&uuid.indexOf("_")!=-1)
			uuid = uuid.split("_")[1];
		else {
			uuid = "";
		}
		String ret = "{\"state\":\"1\",\"orderid\":\""+orderMap.get("id")+"\",\"btime\":\""+bt+"\",\"etime\":\""+et+"\"," +
				"\"carnumber\":\""+uuid+"\",\"duration\":\""+dur+"\",\"total\":\""+orderMap.get("total")+"\"}";
		return ret;
	}

	
	/**
	 * ����λ
	 * @param comId ͣ�������
	 * @param uin �ͻ���� 
	 * @param number ������
	 * @param infoMap ���ؽ��
	 */
	private void doShare(Long comId,Long uin,Integer number,Map<String,Object> infoMap,boolean isCanLalaRecord){
		Long ntime = System.currentTimeMillis()/1000;
		//���¹�˾����ͣ�����ķ���������
		if(comId!=null&&uin!=null){
			int result = daService.update("update com_info_tb set share_number =?,update_time=? where id=?",
					new Object[]{number,ntime,comId});
			//���㷵�ؿ�������
			if(result==1){
//				if(isCanLalaRecord)
//					doCollectorSort(number,uin,comId);
				//��ѯ��ǰδ����Ķ�����������������ռ��λ����
				Long count = pService.getLong("select count(*) from order_tb where comid=? and state=? ",//and create_time>?",
						new Object[]{comId,0});//,TimeTools.getToDayBeginTime()});
				infoMap.put("info", "success");
				infoMap.put("busy", count+"");
				logService.updateShareLog(comId, uin, number);
			}else {
				infoMap.put("info", "fail");
				infoMap.put("message", "����λʧ�ܣ����Ժ�����!");
			}
		}else {
			infoMap.put("info", "fail");
			infoMap.put("message", "��˾��Ա�����Ϸ�!");
		}
	}
	
	/*private  void doCollectorSort(Integer number,Long uin,Long comId){
		
		Long time = ntime;
		boolean isLala  = false;
		try {
			isLala = publicMethods.isCanLaLa(number, uin, time);
		} catch (Exception e) {
			logger.error("memcacahe error:"+e.getMessage());
			isLala=ParkingMap.isCanRecordLaLa(uin);
		}
		if(isLala){
			logService.updateScroe(1, uin,comId);
		}
	}*/
	
	/**
	 * ���۴���
	 * @param comId ͣ�������
	 * @param uin �ͻ���� 
	 * @param hour  �Ż�Сʱ 
	 * @param orderId �������
	 * @param infoMap ���ؽ��
	 */
	private void doSale(Long comId,Long uin,Integer hour,Long orderId,Map<String,Object> infoMap){
		//���¶�����Ľ�ͣ�������ܶ�����������,
		Map orderMap = daService.getPojo("select * from order_tb where id=?", new Object[]{orderId});
		if(orderMap!=null){
			Long cid  = (Long)orderMap.get("comid");
			Long uid = (Long)orderMap.get("uin");
			if(cid.intValue()==comId.intValue()){//��֤�����Ƿ���ȷ 
				Double total = getPrice(hour, comId);
				//���¶������
				List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
				Map<String, Object> orderSqlMap = new HashMap<String, Object>();
				orderSqlMap.put("sql", "update order_tb set total = total-? where id =?");
				orderSqlMap.put("values", new Object[]{total,orderId});
				bathSql.add(orderSqlMap);
				//����ͣ�����ܶ���
				Map<String, Object> comSqlMap = new HashMap<String, Object>();
				comSqlMap.put("sql", "update com_info_tb set " +
						"total_money=total_money-? ,money=money-? where id=?");
				comSqlMap.put("values", new Object[]{total,total,comId});
				bathSql.add(comSqlMap);
				//���³������
				Map<String, Object> userSqlMap = new HashMap<String, Object>();
				userSqlMap.put("sql", "update user_info_tb set balance = balance+? where id =?");
				userSqlMap.put("values", new Object[]{total,uid});
				bathSql.add(userSqlMap);
				boolean result = daService.bathUpdate(bathSql);
				if(result){//�������ɹ�ʱ��������Ϣ����дϵͳ��־
					infoMap.put("info", "success");
					infoMap.put("message", "�Żݳɹ�!");
					//дϵͳ��־
					doLog(comId, uin, TimeTools.gettime()+",�Ż��˶�������ţ�"+orderId+",�Żݽ�"+total,2);
					//дϵͳ��Ϣ����������ͨ��ˢ����Ϣȡ��
					doMessage(uid, TimeTools.gettime()+",���Ķ���(��ţ�"+orderId+")�Ż���"+total+"Ԫ,�Ѿ����������������ա�");
				}else {
					infoMap.put("info", "fail");
					infoMap.put("message", "�Ż�ʧ�ܣ����Ժ�����!");
				}
			}
		}else{
			infoMap.put("info", "fail");
			infoMap.put("message", "��������!");
		}
			
		//���������ˮ
		//д������־ 
	}

	private Double getPrice (Integer hour,Long comId){
		//�����Żݽ��
		Map priceMap = daService.getPojo("select * from price_tb where comid=?" +
				" and state=? order by id desc",new Object[]{comId,1});
		Double price = 0d;
		if(priceMap!=null){
			Integer payType = (Integer)priceMap.get("pay_type");
			price = Double.valueOf(priceMap.get("price")+"");
			switch (payType) {
			case 0://�ֶ�
				Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
				calendar.setTimeInMillis(System.currentTimeMillis());
				//��ʼСʱ
				int nhour = calendar.get(Calendar.HOUR_OF_DAY);
				Integer bTime = (Integer)priceMap.get("b_time");
				Integer eTime = (Integer)priceMap.get("e_time");
				//��ǰʱ���ڷֶ�������
				if(nhour>bTime&&nhour<eTime)
					price = price*hour;
				break;
			case 2://��ʱ�䵥λ
				Integer unit = (Integer)priceMap.get("unit");
				price = hour*60/unit*price;
				break;		
			default:
				break;
			}
		}
		return price;
		
	}
	/**
	 * //дϵͳ��Ϣ�����շ�Ա��ʱȡ��Ϣ��
	 * @param comId
	 * @param mesgType  0:�շ�Ա��Ϣ   ,1:������Ϣ
	 * @param uin
	 * @param body
	 * @param orderId
	 * @param total
	 */
	private void doMessage(Long uin,String body){
		Long ntime = System.currentTimeMillis()/1000;
		daService.update("insert into message_tb (type,uin,create_time,content,state) values (?,?,?,?,?)", 
				new Object[]{1,uin,ntime,body,0});
	}
	/*
	 * дϵͳ��־ 
	 */
	private void doLog(Long comid,Long uin,String log,Integer type){
		logService.updateOrderLog(comid, uin, log, type);
	}
	/**
	 * ���㶩�����
	 * @param start
	 * @param end
	 * @param comId
	 * @return �������_�Ƿ��Ż�
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map getPriceMap(Long comId){
		Map priceMap1=null;
		List<Map> priceList=daService.getAll("select * from price_tb where comid=? " +
				"and state=? order by id desc", new Object[]{comId,0});
		if(priceList==null||priceList.size()==0){
			//�����Ÿ�����Ա��ͨ�����úü۸�
		}else {
			try{
				priceMap1=priceList.get(0);
				boolean pm1 = false;//�ҵ�map1,�����ǽ���ʱ����ڿ�ʼʱ��
				Integer payType = (Integer)priceMap1.get("pay_type");
				if(payType==0&&priceList.size()>1){
					for(Map map : priceList){
						if(pm1)
							break;
						payType = (Integer)map.get("pay_type");
						Integer btime = (Integer)map.get("b_time");
						Integer etime = (Integer)map.get("e_time");
						if(payType==0&&etime>btime){
							if(!pm1){
								priceMap1 = map;
								pm1=true;
							}
						}
					}
				}
			}catch(Exception e){
				logger.error(">>>>>>>>>>>>>>>>>>>>>>>>>��ѯ�����ļ۸���Ϣ�쳣"+comId);
			}
		}
		return priceMap1;	
	}
	
	private List<Map<String, Object >> setScroeList(List<Map> list){
		List<Map<String, Object >> templiList = new ArrayList<Map<String, Object >>();
		List<Object> uins = new ArrayList<Object>();
		if(list!=null&&list.size()>0){
			for(int i=0;i<list.size();i++){
				Map map = (Map)list.get(i);
				if(map.get("uin")!=null){
					Long uin = (Long)map.get("uin");
//					if(!uins.contains(uin))
						uins.add(uin);
				}
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
			uins.add(0);
			List<Map<String, Object>> resultList = daService.getAllMap("select u.id,u.mobile ,u.nickname as uname,c.company_name cname ," +
					"c.uid from user_info_tb u,com_info_tb c" +
					" where u.comid=c.id and  u.id in ("+preParams+")  and c.state=?", uins);
			
			//Map<String ,Object> markerMap = new HashMap<String ,Object>();
			if(resultList!=null&&!resultList.isEmpty()){
				
				for(int i=0;i<list.size();i++){
					Map map1 = (Map)list.get(i);
					for(Map<String,Object> map: resultList){
						Long uin = (Long)map.get("id");
						if(map1.get("uin").equals(uin)){
							templiList.add(map1);
							map1.put("nickname", "-");
							String cname = (String)map.get("cname");
							if(cname.length() > 1){
								String hidecname = "***";
								/*for(int j=0;j<cname.length()-2;j++){
									hidecname += "*";
								}*/
								hidecname =cname.substring(0, 1) +hidecname + cname.substring(cname.length()-1, cname.length());
								cname = hidecname;
							}
							map1.put("cname", cname);
							map1.put("score", StringUtils.formatDouble(map1.get("score")));
							break;
						}
					}
				}
			}
		}
		return templiList;
	}
	
	private String myInfo(Long uin){
		Map userMap = daService.getMap("select id, nickname,auth_flag,mobile from user_info_tb where id=?",new Object[]{uin});
		String info="";
		if(userMap!=null){
			Long count = daService.getLong("select Count(id) from collector_account_pic_tb where uin=? and state=? ", new Object[]{uin,0});
			Long role = (Long)userMap.get("auth_flag");
			String _role = "�շ�Ա";
			if(role==1)
				_role = "����Ա";
			return "{\"name\":\""+userMap.get("nickname")+"\",\"uin\":\""+userMap.get("id")+
					"\",\"role\":\""+_role+"\",\"mobile\":\""+userMap.get("mobile")+"\",\"pic\":\""+count+"\"}";
		}
		return "{}";
	}
	
	private void setSort(List list){
		if(list!=null&&list.size()>0){
			for(int i=0;i<list.size();i++){
				Map map = (Map)list.get(i);
				map.put("sort", i+1);
			}
		}
	}
	
	private void setAccountList (List<Map<String, Object>> list,Integer ptype){
		if(list!=null&&!list.isEmpty()){
			if(ptype==0){
				for(Map<String, Object> map :list){
					Integer target = (Integer)map.get("target");
					if(target!=null){
						switch (target) {
						case 0:
							map.put("target", "���п�");
							break;
						case 1:
							map.put("target", "֧����");					
							break;
						case 2:
							map.put("target", "΢��");
							break;
						case 3:
							map.put("target", "ͣ����");
							break;
						case 4:
							String note = (String)map.get("note");
							String [] notes  = note.split("_");
							map.put("note",notes[0]);
							if(notes.length==2)
								map.put("target", notes[1]);
							else
								map.put("target","");
							break;
						default:
							break;
						}
					}
				}
			}else if(ptype==1){
				if(list!=null&&!list.isEmpty()){
					for(int i=0;i<list.size();i++){
						Map map = (Map)list.get(i);
						Integer type = (Integer)map.get("mtype");
						String remark = (String)map.get("r");
						if(type==0){
							if(remark.indexOf("_")!=-1){
								map.put("note", remark.split("_")[0]);
								map.put("target", remark.split("_")[1]);
							}
						}else if(type==1){
							map.put("note", "����");
							map.put("target", "���п�");
						}else if(type==2){
							map.put("note", "����");
							map.put("target", "ͣ����");
						}
						map.remove("r");
					}
				}
			}
			
		}
	}
	
	/*
	 * ���ó�����Ƭ����
	 */
	private void setPicParams(List list){
		List<Object> orderids = new ArrayList<Object>();
		if(list != null && !list.isEmpty()){
			for(int i=0;i<list.size();i++){
				Map map = (Map)list.get(i);
				orderids.add(map.get("id"));
			}
		}
		if(!orderids.isEmpty()){
			String preParams  ="";
			for(Object orderid : orderids){
				if(preParams.equals(""))
					preParams ="?";
				else
					preParams += ",?";
			}
			List<Map<String, Object>> resultList = new ArrayList<Map<String,Object>>();
			resultList = daService.getAllMap("select * from car_picturs_tb where orderid in ("+preParams+") order by pictype", orderids);
			if(!resultList.isEmpty()){
				for(int i=0;i<list.size();i++){
					Map map1 = (Map)list.get(i);
					Long id=(Long)map1.get("id");
					for(Map<String,Object> map: resultList){
						Long orderid = (Long)map.get("orderid");
						if(id.intValue()==orderid.intValue()){
							Integer pictype = (Integer)map.get("pictype");
							if(pictype == 0){
								map1.put("lefttop", map.get("lefttop"));
								map1.put("rightbottom", map.get("rightbottom"));
								map1.put("width", map.get("width"));
								map1.put("height", map.get("height"));
								break;
							}
						}
					}
				}
			}
		}
	}
	/**
	 * 
	 * @param orderMap   ����
	 * @param prepaymoney   Ԥ֧�����
	 * @return
	 */
	private boolean prepayRefund(Map orderMap , Double prepaymoney){
		Long orderId = (Long)orderMap.get("id");
		Map<String, Object> ticketMap = daService.getMap(
				"select * from ticket_tb where orderid=? order by utime limit ?",
				new Object[] { orderId,1});
		DecimalFormat dFormat = new DecimalFormat("#.00");
		Double back = 0.0;
		List<Map<String, Object>> backSqlList = new ArrayList<Map<String,Object>>();
		if(ticketMap != null){
			logger.error(">>>>>>>>>>>>ʹ�ù�ȯ��ticketid:"+ticketMap.get("id")+",orderid="+orderId);
			Integer money = (Integer)ticketMap.get("money");
			Double umoney = Double.valueOf(ticketMap.get("umoney")+"");
			umoney = Double.valueOf(dFormat.format(umoney));
			back = Double.valueOf(dFormat.format(prepaymoney - umoney));
			logger.error(">>>>>>>>>>>Ԥ֧�����prefee��"+prepaymoney+",ʹ��ȯ�Ľ��umoney��"+umoney+",Ӧ�˿��"+back+",orderid:"+orderId);
			Map<String, Object> tcbAccountsqlMap = new HashMap<String, Object>();
			tcbAccountsqlMap.put("sql", "insert into tingchebao_account_tb(amount,type,create_time,remark,utype,orderid) values(?,?,?,?,?,?)");
			tcbAccountsqlMap.put("values", new Object[]{umoney,0,System.currentTimeMillis() / 1000 ,"ͣ��ȯ������", 6, orderId });
			backSqlList.add(tcbAccountsqlMap);
		}else{
			logger.error(">>>>>>>>>>>>û��ʹ�ù�ȯ>>>>>>>>>>>>>orderid:"+orderId);
			back = Double.valueOf(dFormat.format(prepaymoney));
		}
		Long uin = (Long)orderMap.get("uin");
		if(back > 0){
			Map count = daService.getPojo("select * from user_info_tb where id=? ", new Object[]{uin});
			Map<String, Object> usersqlMap = new HashMap<String, Object>();
			if(count != null){//��ʵ�ʻ�
				usersqlMap.put("sql", "update user_info_tb set balance=balance+? where id=? ");
				usersqlMap.put("values", new Object[]{back,uin});
				backSqlList.add(usersqlMap);
			}else{//�����˻�
				usersqlMap.put("sql", "update wxp_user_tb set balance=balance+? where uin=? ");
				usersqlMap.put("values", new Object[]{back,uin});
				backSqlList.add(usersqlMap);
			}
			Map<String, Object> userAccountsqlMap = new HashMap<String, Object>();
			userAccountsqlMap.put("sql", "insert into user_account_tb(uin,amount,type,create_time,remark,pay_type,orderid) values(?,?,?,?,?,?,?)");
			userAccountsqlMap.put("values", new Object[]{uin,back,0,System.currentTimeMillis() / 1000 - 2,"�ֽ����Ԥ֧��Ԥ֧������", 12, orderId });
			backSqlList.add(userAccountsqlMap);
			boolean b = daService.bathUpdate(backSqlList);
			logger.error(">>>>>>>>>>Ԥ֧����������"+b+",orderid:"+orderId);
			try {
				String openid = "";
				if(count!=null)
					openid = count.get("wxp_openid")+"";
				if(!StringUtils.isNotNull(openid)){
					Map wx = daService.getPojo("select * from wxp_user_tb where uin=? ", new Object[]{uin});
					openid = wx.get("openid")+"";
				}
				if(!openid.equals("")){
					logger.error(">>>>>>>>>>>Ԥ֧�����ֽ���㶩���˻�Ԥ֧����   ΢������Ϣ,uin:"+uin+",openid:"+openid);
					String first = "���ֽ���㣬Ԥ֧���˿�";
					Map<String, String> baseinfo = new HashMap<String, String>();
					List<Map<String, String>> orderinfo = new ArrayList<Map<String,String>>();
					String url = "http://"+Constants.WXPUBLIC_REDIRECTURL+"/zld/wxpaccount.do?action=balance&openid="+openid;
					baseinfo.put("url", url);
					baseinfo.put("openid", openid);
					baseinfo.put("top_color", "#000000");
					baseinfo.put("templeteid", Constants.WXPUBLIC_BACK_NOTIFYMSG_ID);
					Map<String, String> keyword1 = new HashMap<String, String>();
					keyword1.put("keyword", "orderProductPrice");
					keyword1.put("value",back+"Ԫ");
					keyword1.put("color", "#000000");
					orderinfo.add(keyword1);
					Map<String, String> keyword2 = new HashMap<String, String>();
					keyword2.put("keyword", "orderProductName");
					keyword2.put("value", "Ԥ֧���˿�");
					keyword2.put("color", "#000000");
					orderinfo.add(keyword2);
					Map<String, String> keyword3 = new HashMap<String, String>();
					keyword3.put("keyword", "orderName");
					keyword3.put("value", orderId+"");
					keyword3.put("color", "#000000");
					orderinfo.add(keyword3);
					Map<String, String> keyword4 = new HashMap<String, String>();
					keyword4.put("keyword", "Remark");
					keyword4.put("value", "���������˻���");
					keyword4.put("color", "#000000");
					orderinfo.add(keyword4);
					Map<String, String> keyword5 = new HashMap<String, String>();
					keyword5.put("keyword", "first");
					keyword5.put("value", first);
					keyword5.put("color", "#000000");
					orderinfo.add(keyword5);
					publicMethods.sendWXTempleteMsg(baseinfo, orderinfo);
				}
			} catch (Exception e) {
				logger.error("�˻سɹ�����Ϣ����ʧ��");
				e.printStackTrace();
				return true;
			}
			logger.error("�˻سɹ� ....");	
			
			return true;
		}else{
			logger.error(">>>>>>>>>>>>>>>�˻����backС��0��orderid��"+orderId);
			return false;
		}
	}
	
	class ExeCallable implements Callable<Object>{
		private List<Map<String, Object>> list;
		private Long groupId = -1L;
		private int type;
		ExeCallable(List<Map<String, Object>> list, Long groupId, int type){
			this.list = list;
			this.groupId = groupId;
			this.type = type;
		}
		private Long parkId = -1L;
		ExeCallable(Long parkId, int type){
			this.parkId = parkId;
			this.type = type;
		}
		private Long berthSegId = -1L;
		private Long uid = -1L;
		private Long ntime = -1L;
		private String device_code;
		ExeCallable(Long berthSegId, Long uid, 
				Long ntime, String device_code, int type){
			this.berthSegId = berthSegId;
			this.uid = uid;
			this.ntime = ntime;
			this.device_code = device_code;
			this.type = type;
		}
		
		public Object call() throws Exception {
			Object result = null;
			try {
				switch (type) {
				case 0://��ȡ������Ϣ
					getOrderInfo(list, groupId);
					break;
				case 1:
					getSensorInfo(list);
					break;
				case 2:
					Map<String, Object> parkMap = pService.getMap("select company_name " +
							" from com_info_tb where id=? ", new Object[]{parkId});
					if(parkMap != null){
						result = parkMap.get("company_name");
					}
					break;
				case 3:
					result = getCarTypeInfo(parkId);
					break;
				case 4:
					result = signIn(berthSegId, ntime, uid, device_code);
					break;
				default:
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return result;
		}
	}
}
