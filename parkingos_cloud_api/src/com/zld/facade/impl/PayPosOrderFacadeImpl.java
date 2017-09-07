package com.zld.facade.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.omg.CORBA.PUBLIC_MEMBER;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zld.facade.PayPosOrderFacade;
import com.zld.facade.impl.GenPosOrderFacadeImpl.ExeCallable;
import com.zld.impl.CommonMethods;
import com.zld.impl.MemcacheUtils;
import com.zld.impl.PublicMethods;
import com.zld.pojo.AutoPayPosOrderFacadeReq;
import com.zld.pojo.AutoPayPosOrderReq;
import com.zld.pojo.AutoPayPosOrderResp;
import com.zld.pojo.Berth;
import com.zld.pojo.Car;
import com.zld.pojo.GenPosOrderFacadeReq;
import com.zld.pojo.ManuPayPosOrderFacadeReq;
import com.zld.pojo.ManuPayPosOrderReq;
import com.zld.pojo.ManuPayPosOrderResp;
import com.zld.pojo.Order;
import com.zld.pojo.PayEscapePosOrderFacadeReq;
import com.zld.pojo.PayEscapePosOrderReq;
import com.zld.pojo.PayEscapePosOrderResp;
import com.zld.pojo.WorkRecord;
import com.zld.service.DataBaseService;
import com.zld.service.PayPosOrderService;
import com.zld.service.PgOnlyReadService;
import com.zld.utils.ExecutorsUtil;
import com.zld.utils.StringUtils;
@Component
public class PayPosOrderFacadeImpl implements PayPosOrderFacade {
	@Autowired
	private DataBaseService writeService;
	@Autowired
	private PgOnlyReadService readService;
	@Autowired
	private CommonMethods commonMethods;
	@Autowired
	@Resource(name = "payMonth")
	private PayPosOrderService payMonthService;
	@Autowired
	@Resource(name = "payEpay")
	private PayPosOrderService payEpayService;
	@Autowired
	@Resource(name = "payCash")
	private PayPosOrderService payCashService;
	@Autowired
	@Resource(name = "payCard")
	private PayPosOrderService payCardService;
	@Autowired
	@Resource(name = "bolinkPay")
	private PayPosOrderService payBolinkService;
	@Autowired
	private MemcacheUtils memcacheUtils;
	@Autowired
	private PublicMethods publicMethods;
	
	
	Logger logger = Logger.getLogger(PayPosOrderFacadeImpl.class);
	
	@Override
	public AutoPayPosOrderResp autoPayPosOrder(AutoPayPosOrderFacadeReq req) {
		AutoPayPosOrderResp resp = new AutoPayPosOrderResp();
		String lock = null;//�ֲ�ʽ��
		long workId = -1L;//������¼���
		boolean result = false;//������¼���ɽ��
		try {
			logger.error(req.toString());
			//----------------------------����--------------------------------//
			Long curTime = req.getCurTime();
			Long orderId = req.getOrderId(); 
			Double money = req.getMoney();//�ܽ��
			String imei  =  req.getImei();//�ֻ�����
			Integer version = req.getVersion();//�汾��
			Long uid = req.getUid();
			Long groupId = req.getGroupId();//�շ�Ա������Ӫ����
			Long endTime = req.getEndTime();
			//----------------------------У�����--------------------------------//
			if(orderId <= 0 
					|| uid <= 0 
					|| money < 0
					|| groupId <= 0){//money����Ϊ��
				resp.setResult(-1);
				resp.setErrmsg("��������");
				return resp;
			}
			//----------------------------�ֲ�ʽ��--------------------------------//
			lock = commonMethods.getLock(orderId);
			if(!memcacheUtils.addLock(lock)){
				logger.error("lock:"+lock);
				resp.setResult(-1);
				resp.setErrmsg("�����������");
				return resp;
			}
			//------------------------���̲߳��в�ѯ------------------------//
			ExecutorService pool = ExecutorsUtil.getExecutorService();
			ExeCallable callable0 = new ExeCallable(uid, orderId, 0);//�ϰ��¼
			ExeCallable callable1 = new ExeCallable(uid, orderId, 1);//������Ϣ
			ExeCallable callable2 = new ExeCallable(uid, orderId, 2);//�����󶨵ĳ���������
			
			Future<Object> future0 = pool.submit(callable0);
			Future<Object> future1 = pool.submit(callable1);
			Future<Object> future2 = pool.submit(callable2);
			
			WorkRecord workRecord = (WorkRecord)future0.get();
			Order order = (Order)future1.get();
			Long brethOrderId = (Long)future2.get();
			//----------------------------У���ϰ��¼--------------------------------//
			if(workRecord == null){
				resp.setResult(-1);
				resp.setErrmsg("δ�ڵ�ǰ��λ�����Ĳ�λ��ǩ��");
				return resp;
			}
			workId = workRecord.getId();
			logger.error("workId:"+workId);
			//----------------------------��ȡ������Ϣ--------------------------------//
			if(order == null){
				resp.setResult(-1);
				resp.setErrmsg("������¼������");
				return resp;
			}
			logger.error(order.toString());
			if(order.getState() == 1){
				resp.setResult(-1);
				resp.setErrmsg("�����ѽ���");
				return resp;
			}
			if(order.getState() == 2){
				resp.setResult(-1);
				resp.setErrmsg("��������Ϊ�ӵ�");
				return resp;
			}
			int payType = order.getPay_type();
			int cType = order.getC_type();
			double prepay = order.getPrepaid();
			if(endTime < 0){//�ͻ��˵Ͱ汾û�д�endtime����
				endTime = commonMethods.getOrderEndTime(brethOrderId, uid, curTime);
			}
			long userId = order.getUin();
			String carNumber = order.getCar_number();
			long startTime = order.getCreate_time();
			//----------------------------�û���Ϣ--------------------------------//
			if(userId <= 0 && carNumber != null && !"".equals(carNumber)){
				Car car = readService.getPOJO("select * from car_info_tb " +
						" where car_number=? and state=?", new Object[]{carNumber, 1}, Car.class);
				if(car != null && car.getUin() > 0){
					userId = car.getUin();
				}
			}
			logger.error(userId);
			
			AutoPayPosOrderResp autoPayResp = null;
			
			AutoPayPosOrderReq autoPayReq = new AutoPayPosOrderReq();
			autoPayReq.setOrder(order);
			autoPayReq.setMoney(money);
			autoPayReq.setImei(imei);
			autoPayReq.setUid(uid);
			autoPayReq.setVersion(version);
			autoPayReq.setWorkId(workId);
			autoPayReq.setUserId(userId);
			autoPayReq.setEndTime(endTime);
			autoPayReq.setBerthOrderId(brethOrderId);
			autoPayReq.setGroupId(groupId);
			
			/**����֧��****/
			String bolinkPayRes ="cash";//��������֧����ʽ cash��Ҫ����������� ��app��ʾ�����ڲ����շ�
			
			if(prepay <= 0.0&&cType!=5){//û��Ԥ֧���Ҳ����¿����������ò���֧��
				bolinkPayRes =publicMethods.sendPayOrderToBolink(orderId,endTime,money,order.getComid());
				if(bolinkPayRes.equals("app")){//����ƽ̨�Ѿ�����֧�������ﴦ��Ϊ����֧��
					logger.error("��������>>>orderid:"+orderId);
					autoPayResp = payBolinkService.autoPayPosOrder(autoPayReq);
					logger.error(autoPayReq.toString());
					if(autoPayResp.getResult() == 1){
						result = true;//��������ɹ�
						resp.setResult(1);//�¿������ʶ
						resp.setErrmsg(autoPayResp.getErrmsg());
						return resp;
					}
				}
			}
			/**����֧��***/
			//------------------------���ݳ����׶�����ȡ����������Ϣ----------------------//
			
			String duration = StringUtils.getTimeString(startTime, endTime);
			logger.error("endTime:" + endTime+ ",brethOrderId:"+brethOrderId);
			//-----------------------------�߼�����-------------------------------//
			/*���Ԥ֧�����㣬��������һ��֧����ʽ����Ԥ֧����ͬ��֧����ʽ���Զ����㣬
			���磬�ֽ�Ԥ֧�����㣬�����Կ�Ƭ������ͣ�����˻�������*/
			
			
			double refundMoney = 0;
			if(prepay > money){
				refundMoney = StringUtils.formatDouble(prepay - money);
			}
			logger.error("refundMoney:"+refundMoney);
			resp.setDuration(duration);
			if(cType == 5){//�����¿�����
				autoPayReq.setPayType(3);
				logger.error("�¿��Զ�����>>>orderid:"+orderId);
				autoPayResp = payMonthService.autoPayPosOrder(autoPayReq);
				logger.error(autoPayReq.toString());
				if(autoPayResp.getResult() == 1){
					result = true;//��������ɹ�
					resp.setResult(1);//�¿������ʶ
					resp.setErrmsg(autoPayResp.getErrmsg());
					return resp;
				}
			}
			if(payType == 4 && prepay >= money
					|| payType == 0 && money == 0 && prepay == 0){//�ֽ�Ԥ֧������δԤ֧������0Ԫ����Ķ���
				logger.error("�ֽ�Ԥ֧���㹻����δԤ֧����0Ԫ����>>>orderid:"+orderId);
				autoPayResp = payCashService.autoPayPosOrder(autoPayReq);
				logger.error(autoPayReq.toString());
				if(autoPayResp.getResult() == 1){
					result = true;//��������ɹ�
					resp.setResult(3);//�ֽ�����ʶ
					resp.setErrmsg("Ԥ�ս�"+prepay+
							"Ԫ��Ӧ�ս�"+money+"Ԫ��Ӧ�˿"+refundMoney+"Ԫ");
					return resp;
				}
			}
			if(payType == 9
					|| payType == 0 && prepay == 0){//����ˢ������
				logger.error("ˢ��Ԥ֧������δԤ֧��>>>orderid:"+orderId);
				autoPayResp = payCardService.autoPayPosOrder(autoPayReq);
			    //logger.error(autoPayReq.toString());
				if(autoPayResp.getResult() == 1){
					result = true;//��������ɹ�
					resp.setResult(4);//ˢ�������ʶ
					resp.setErrmsg(autoPayResp.getErrmsg());
					return resp;
				}
			}
			if(userId > 0 
					&& (payType == 2 && prepay >= money
					|| payType == 0 && prepay == 0)){//����Ԥ֧������δԤ֧�����Ķ���
				//ע��!!!�������һ�����⣬����Ԥ֧������Ļ���Ԥ֧����Ǯ�����˳����˻�
				logger.error("����Ԥ֧���㹻����δԤ֧����>>>orderid:"+orderId);
				autoPayResp = payEpayService.autoPayPosOrder(autoPayReq);
				logger.error(autoPayReq.toString());
				if(autoPayResp.getResult() == 1){
					result = true;//��������ɹ�
					resp.setResult(2);//����֧����ʶ
					resp.setErrmsg(autoPayResp.getErrmsg());
					return resp;
				}
			}
			if(autoPayResp != null && autoPayResp.getResult() < 0){
				logger.error("����ʧ��>>>orderid:"+orderId+",result:"
						+autoPayResp.getResult()+",errmsg:"+autoPayResp.getErrmsg());
				logger.error(autoPayResp.toString());
				resp.setResult(-1);//Ӧ���Զ����㣬���ǽ���ʧ��
				resp.setErrmsg(autoPayResp.getErrmsg());
				return resp;
			}
			if(autoPayResp == null || autoPayResp.getResult() == 0){
				logger.error("���ֶ�����>>>orderid:"+orderId);
				resp.setResult(0);//��Ҫ�ֶ������ʶ
				resp.setErrmsg("�Զ����㶩��ʧ�ܣ����ֶ�����");
				return resp;
			}
		} catch (Exception e) {
			logger.error(e);
		} finally {//ɾ����
			boolean b = memcacheUtils.delLock(lock);
			logger.error("ɾ����lock:"+lock+"b:"+b);
			if(result){//��������ɹ������³�������
				boolean b1 = commonMethods.updateInOutCar(workId, 1);
				logger.error("���³�������,b1:"+b1);
			}
		}
		resp.setResult(-1);//Ӧ���Զ����㣬���ǽ���ʧ��
		resp.setErrmsg("ϵͳ����");
		return resp;
	}

	@Override
	public ManuPayPosOrderResp manuPayPosOrder(ManuPayPosOrderFacadeReq req) {
		ManuPayPosOrderResp resp = new ManuPayPosOrderResp();
		String lock = null;
		long workId = -1L;//������¼���
		boolean result = false;//������¼���ɽ��
		try {
			logger.error(req.toString());
			//----------------------------����--------------------------------//
			Long curTime = req.getCurTime();
			Long orderId = req.getOrderId(); 
			Double money = req.getMoney();//�ܽ��
			String imei  =  req.getImei();//�ֻ�����
			Integer version = req.getVersion();//�汾��
			Long uid = req.getUid();
			String nfc_uuid = req.getNfc_uuid();
			int payType = req.getPayType();//֧����ʽ 0���ֽ�֧�� 1��ˢ��֧��
			int bindcard = req.getBindcard();//0:�ͻ��˵����󶨳����ֻ��ŵ���ʾ�� 1����������ʾ��ֱ��ˢ��Ԥ��
			Long groupId = req.getGroupId();
			Long endTime = req.getEndTime();
			//----------------------------У�����--------------------------------//
			if(orderId <= 0 
					|| uid <= 0 
					|| money < 0
					|| groupId <= 0){//money����Ϊ��
				resp.setResult(-1);
				resp.setErrmsg("��������");
				return resp;
			}
			//----------------------------�ֲ�ʽ��--------------------------------//
			lock = commonMethods.getLock(orderId);
			if(!memcacheUtils.addLock(lock)){
				logger.error("lock:"+lock);
				resp.setResult(-1);
				resp.setErrmsg("�����������");
				return resp;
			}
			//------------------------���̲߳��в�ѯ------------------------//
			ExecutorService pool = ExecutorsUtil.getExecutorService();
			ExeCallable callable0 = new ExeCallable(uid, orderId, 0);//�ϰ��¼
			ExeCallable callable1 = new ExeCallable(uid, orderId, 1);//������Ϣ
			ExeCallable callable2 = new ExeCallable(uid, orderId, 2);//�����󶨵ĳ���������
			
			Future<Object> future0 = pool.submit(callable0);
			Future<Object> future1 = pool.submit(callable1);
			Future<Object> future2 = pool.submit(callable2);
			
			WorkRecord workRecord = (WorkRecord)future0.get();
			Order order = (Order)future1.get();
			Long brethOrderId = (Long)future2.get();
			//----------------------------У���ϰ��¼--------------------------------//
			if(workRecord == null){
				resp.setResult(-1);
				resp.setErrmsg("δ�ڵ�ǰ��λ�����Ĳ�λ��ǩ��");
				return resp;
			}
			workId = workRecord.getId();
			logger.error("workId:"+workId);
			//----------------------------��ȡ������Ϣ--------------------------------//
			if(order == null){
				resp.setResult(-1);
				resp.setErrmsg("������¼������");
				return resp;
			}
			//logger.error(order.toString());
			double prepay = order.getPrepaid();
			long userId = order.getUin();
			String carNumber = order.getCar_number();
			long startTime = order.getCreate_time();
			int state = order.getState();
			int cType = order.getC_type();
			if(state == 1){
				resp.setResult(-1);
				resp.setErrmsg("�����ѽ���");
				return resp;
			}
			if(state == 2){
				resp.setResult(-1);
				resp.setErrmsg("��������Ϊ�ӵ�");
				return resp;
			}
			if(prepay >= money || cType == 5){//Ԥ��������֧������ʱӦ����autoPayPosOrder����
				resp.setResult(-1);
				resp.setErrmsg("����ʧ��");
				return resp;
			}
			double pursueMoney = money;
			if(prepay > 0){
				pursueMoney = StringUtils.formatDouble(money - prepay);
			}
			logger.error("pursueMoney:"+pursueMoney);
			//----------------------------�û���Ϣ--------------------------------//
			if(userId <= 0 && carNumber != null && !"".equals(carNumber)){
				Car car = readService.getPOJO("select * from car_info_tb " +
						" where car_number=? and state=?", new Object[]{carNumber, 1}, Car.class);
				if(car != null && car.getUin() > 0){
					userId = car.getUin();
				}
			}
			logger.error(userId);
			//------------------------���ݳ����׶�����ȡ����������Ϣ----------------------//
			if(endTime < 0){
				endTime = commonMethods.getOrderEndTime(brethOrderId, uid, curTime);
			}
			String duration = StringUtils.getTimeString(startTime, endTime);
			logger.error("endTime:" + endTime+ ",brethOrderId:"+brethOrderId);
			//------------------------------�����߼�------------------------------//
			
			ManuPayPosOrderReq manuPayReq = new ManuPayPosOrderReq();
			manuPayReq.setOrder(order);
			manuPayReq.setMoney(money);
			manuPayReq.setImei(imei);
			manuPayReq.setUid(uid);
			manuPayReq.setVersion(version);
			manuPayReq.setWorkId(workId);
			manuPayReq.setUserId(userId);
			manuPayReq.setEndTime(endTime);
			manuPayReq.setBerthOrderId(brethOrderId);
			manuPayReq.setNfc_uuid(nfc_uuid);
			manuPayReq.setBindcard(bindcard);
			manuPayReq.setGroupId(groupId);
			//logger.error(manuPayReq.toString());
			ManuPayPosOrderResp manuPayResp = null;
			if(payType == 0){
				logger.error("�ֽ����>>>orderid:"+orderId);
				manuPayResp = payCashService.manuPayPosOrder(manuPayReq);
				logger.error(manuPayResp.toString());
				if(manuPayResp.getResult() == 1){
					result = true;//��������ɹ�
					resp.setErrmsg("Ԥ�ս�"+prepay+"Ԫ��Ӧ�ս�"+money+"Ԫ��Ӧ���գ�"+pursueMoney+"Ԫ");
					resp.setDuration(duration);
					resp.setResult(1);
					return resp;
				}
			}else if(payType == 1){
				logger.error("ˢ������>>>orderid:"+orderId);
				manuPayResp = payCardService.manuPayPosOrder(manuPayReq);
				logger.error(manuPayResp.toString());
				if(manuPayResp.getResult() == 1){
					result = true;//��������ɹ�
					resp.setErrmsg(manuPayResp.getErrmsg());
					resp.setDuration(duration);
					resp.setResult(1);
					return resp;
				}else if(manuPayResp.getResult() == -5){
					resp.setErrmsg(manuPayResp.getErrmsg());
					resp.setResult(-5);
					return resp;
				}else if(manuPayResp.getResult() == -6){
					logger.error("��ʾ���û�>>>orderid:"+orderId);
					resp.setErrmsg(manuPayResp.getErrmsg());
					resp.setResult(-6);
					return resp;
				}
			}
			if(manuPayResp != null && manuPayResp.getResult() != 1){
				logger.error("����ʧ��>>>orderid:"+orderId);
				resp.setResult(-1);//Ӧ���Զ����㣬���ǽ���ʧ��
				resp.setErrmsg(manuPayResp.getErrmsg());
				return resp;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {//ɾ����
			boolean b = memcacheUtils.delLock(lock);
			logger.error("ɾ����lock:"+lock+",b:"+b);
			if(result){//��������ɹ������³�������
				boolean b1 = commonMethods.updateInOutCar(workId, 1);
				logger.error("���³�������,b1:"+b1);
			}
		}
		resp.setResult(-1);//Ӧ���Զ����㣬���ǽ���ʧ��
		resp.setErrmsg("ϵͳ����");
		return resp;
	}

	@Override
	public PayEscapePosOrderResp payEscapePosOrder(PayEscapePosOrderFacadeReq req) {
		PayEscapePosOrderResp resp = new PayEscapePosOrderResp();
		String lock = null;
		try {
			logger.error(req.toString());
			//----------------------------����--------------------------------//
			Long curTime = req.getCurTime();
			Long orderId = req.getOrderId(); 
			Double money = req.getMoney();//�ܽ��
			String imei  =  req.getImei();//�ֻ�����
			Integer version = req.getVersion();//�汾��
			Long uid = req.getUid();
			String nfc_uuid = req.getNfc_uuid();
			int payType = req.getPayType();//֧����ʽ 0���ֽ�֧�� 1��ˢ��֧��
			int bindcard = req.getBindcard();//0:�ͻ��˵����󶨳����ֻ��ŵ���ʾ�� 1����������ʾ��ֱ��ˢ��Ԥ��
			Long groupId = req.getGroupId();//׷���շ�Ա���ڵ���Ӫ���ű��
			Long parkId = req.getParkId();//׷���շ�Ա���ڵĳ���
			Long berthId = req.getBerthId();//׷�ɶ������ڵĲ�λ��ţ�2016-10-14�����,Ϊ�˼�¼���ĸ���λ��׷�ɵĶ�����
			//----------------------------У�����--------------------------------//
			if(orderId <= 0 
					|| uid <= 0 
					|| money < 0
					|| groupId <= 0
					|| parkId <= 0){//money����Ϊ��
				resp.setResult(-1);
				resp.setErrmsg("��������");
				return resp;
			}
			//----------------------------�ֲ�ʽ��--------------------------------//
			lock = commonMethods.getLock(orderId);
			if(!memcacheUtils.addLock(lock)){
				logger.error("lock:"+lock);
				resp.setResult(-1);
				resp.setErrmsg("�����������");
				return resp;
			}
			//------------------------���̲߳��в�ѯ------------------------//
			ExecutorService pool = ExecutorsUtil.getExecutorService();
			ExeCallable callable0 = new ExeCallable(uid, orderId, 0);//�ϰ��¼
			ExeCallable callable1 = new ExeCallable(uid, orderId, 1);//������Ϣ
			ExeCallable callable2 = new ExeCallable(uid, orderId, 2);//�����󶨵ĳ���������
			
			Future<Object> future0 = pool.submit(callable0);
			Future<Object> future1 = pool.submit(callable1);
			Future<Object> future2 = pool.submit(callable2);
			
			WorkRecord workRecord = (WorkRecord)future0.get();
			Order order = (Order)future1.get();
			Long brethOrderId = (Long)future2.get();
			//----------------------------У���ϰ��¼--------------------------------//
			if(workRecord == null){
				resp.setResult(-1);
				resp.setErrmsg("δ�ڵ�ǰ��λ�����Ĳ�λ��ǩ��");
				return resp;
			}
			long workId = workRecord.getId();
			long berthSegId = workRecord.getBerthsec_id();
			logger.error("workId:"+workId+",berthSegId:"+berthSegId);
			//----------------------------��ȡ������Ϣ--------------------------------//
			if(order == null){
				resp.setResult(-1);
				resp.setErrmsg("������¼������");
				return resp;
			}
			logger.error(order.toString());
			double prepay = order.getPrepaid();
			long userId = order.getUin();
			String carNumber = order.getCar_number();
			int state = order.getState();
			int cType = order.getC_type();
			if(state == 0){
				resp.setResult(-1);
				resp.setErrmsg("���ӵ�������������");
				return resp;
			}
			if(state == 1){
				resp.setResult(-1);
				resp.setErrmsg("�����ѽ���");
				return resp;
			}
			if(prepay >= money || cType == 5){//Ԥ��������֧������ʱӦ����autoPayPosOrder����
				resp.setResult(-1);
				resp.setErrmsg("����ʧ��");
				return resp;
			}
			double pursueMoney = money;
			if(prepay > 0){
				pursueMoney = StringUtils.formatDouble(money - prepay);
			}
			logger.error("pursueMoney:"+pursueMoney);
			//----------------------------�û���Ϣ--------------------------------//
			if(userId <= 0 && carNumber != null && !"".equals(carNumber)){
				Car car = readService.getPOJO("select * from car_info_tb " +
						" where car_number=? and state=?", 
						new Object[]{carNumber, 1}, Car.class);
				if(car != null && car.getUin() > 0){
					userId = car.getUin();
				}
			}
			logger.error(userId);
			//------------------------���ݳ����׶�����ȡ����������Ϣ----------------------//
			logger.error("brethOrderId:"+brethOrderId);
			//------------------------------�����߼�------------------------------//
			
			PayEscapePosOrderReq escapeReq = new PayEscapePosOrderReq();
			escapeReq.setOrder(order);
			escapeReq.setMoney(money);
			escapeReq.setImei(imei);
			escapeReq.setUid(uid);
			escapeReq.setVersion(version);
			escapeReq.setBerthSegId(berthSegId);
			escapeReq.setUserId(userId);
			escapeReq.setBerthOrderId(brethOrderId);
			escapeReq.setNfc_uuid(nfc_uuid);
			escapeReq.setBindcard(bindcard);
			escapeReq.setGroupId(groupId);
			escapeReq.setBerthId(berthId);
			escapeReq.setParkId(parkId);
			logger.error(escapeReq.toString());
			PayEscapePosOrderResp payEscapeResp = null;
			if(payType == 0){
				logger.error("�ֽ����>>>orderid:"+orderId);
				payEscapeResp = payCashService.payEscapePosOrder(escapeReq);
				logger.error(payEscapeResp.toString());
				if(payEscapeResp.getResult() == 1){
					resp.setErrmsg("Ԥ�ս�"+prepay+"Ԫ��Ӧ�ս�"+money+"Ԫ��Ӧ׷�ɣ�"+pursueMoney+"Ԫ");
					resp.setResult(1);
					return resp;
				}
			}else if(payType == 1){
				logger.error("ˢ������>>>orderid:"+orderId);
				payEscapeResp = payCardService.payEscapePosOrder(escapeReq);
				logger.error(payEscapeResp.toString());
				if(payEscapeResp.getResult() == 1){
					resp.setErrmsg(payEscapeResp.getErrmsg());
					resp.setResult(1);
					return resp;
				}else if(payEscapeResp.getResult() == -5){//��Ƭ��Ҫ����
					resp.setErrmsg(payEscapeResp.getErrmsg());
					resp.setResult(-5);
					return resp;
				}else if(payEscapeResp.getResult() == -6){
					logger.error("��ʾ���û�>>>orderid:"+orderId);
					resp.setErrmsg(payEscapeResp.getErrmsg());
					resp.setResult(-6);
					return resp;
				}
			}
			if(payEscapeResp != null && payEscapeResp.getResult() != 1){
				logger.error("����ʧ��>>>orderid:"+orderId);
				resp.setResult(-1);//Ӧ���Զ����㣬���ǽ���ʧ��
				resp.setErrmsg(payEscapeResp.getErrmsg());
				return resp;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {//ɾ����
			boolean b = memcacheUtils.delLock(lock);
			logger.error("ɾ����lock:"+lock+",b:"+b);
		}
		resp.setResult(-1);//Ӧ���Զ����㣬���ǽ���ʧ��
		resp.setErrmsg("ϵͳ����");
		return resp;
	}
	
	class ExeCallable implements Callable<Object>{
		private Long uid = -1L;
		private Long orderId = -1L;
		private int type;
		ExeCallable(Long uid, Long orderId, int type){
			this.uid = uid;
			this.orderId = orderId;
			this.type = type;
		}
		@Override
		public Object call() throws Exception {
			Object result = null;
			try {
				switch (type) {
				case 0:
					result = commonMethods.getWorkRecord(uid);
					break;
				case 1:
					result = writeService.getPOJO("select * from order_tb where id=? ", 
							new Object[]{orderId}, Order.class);
					break;
				case 2:
					result = commonMethods.getBerthOrderId(orderId);//�شŶ������
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
