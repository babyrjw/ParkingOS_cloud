package com.zld.facade.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zld.facade.GenPosOrderFacade;
import com.zld.impl.CommonMethods;
import com.zld.impl.MemcacheUtils;
import com.zld.pojo.Berth;
import com.zld.pojo.Car;
import com.zld.pojo.GenPosOrderFacadeReq;
import com.zld.pojo.GenPosOrderFacadeResp;
import com.zld.pojo.GenPosOrderReq;
import com.zld.pojo.GenPosOrderResp;
import com.zld.pojo.WorkRecord;
import com.zld.service.DataBaseService;
import com.zld.service.GenPosOrderService;
import com.zld.service.PgOnlyReadService;
import com.zld.utils.ExecutorsUtil;
import com.zld.utils.TimeTools;
@Component
public class GenPosOrderFacadeImpl implements GenPosOrderFacade {
	@Autowired
	private DataBaseService writeService;
	@Autowired
	private PgOnlyReadService readService;
	@Autowired
	private CommonMethods commonMethods;
	@Autowired
	@Resource(name = "genCash")
	private GenPosOrderService genOrderCashService;
	@Autowired
	@Resource(name = "genCard")
	private GenPosOrderService genOrderCardService;
	@Autowired
	private MemcacheUtils memcacheUtils;
	
	Logger logger = Logger.getLogger(GenPosOrderFacadeImpl.class);
	@Override
	public GenPosOrderFacadeResp genPosOrder(GenPosOrderFacadeReq req) {
		GenPosOrderFacadeResp resp = new GenPosOrderFacadeResp();
		String lock1 = null;
		String lock2 = null;
		long workId = -1L;//������¼���
		boolean result = false;//������¼���ɽ��
		try {
			logger.error(req.toString());
			Long orderId = req.getOrderId();//������
			String carNumber = req.getCarNumber();//���ƺ�
			Long berthId = req.getBerthId();//��λ
			String imei = req.getImei();
			Long uid = req.getUid();//�շ�Ա���
			Integer version = req.getVersion();//�汾��
			Long parkId = req.getParkId();//�������
			Long groupId = req.getGroupId();//��Ӫ���ű��
			Long curTime = req.getCurTime();//��ǰʱ��
			Integer carType =req.getCarType();//��������
			//-------------------------Ԥ������-----------------------//
			Integer payType = req.getPayType();//Ԥ������ 0:�ֽ�Ԥ�� 1��ˢ��Ԥ��
			String nfc_uuid = req.getNfc_uuid();//ˢ��Ԥ֧���Ŀ�Ƭ���
			Integer bindcard = req.getBindcard();//Ԥ֧���õ��Ĳ�����0:�ͻ��˵����󶨳����ֻ��ŵ���ʾ�� 1����������ʾ��ֱ��ˢ��Ԥ��
			Double prepay = req.getPrepay();//Ԥ֧�����
			//-------------------------У�����-----------------------//
			if(carNumber == null
					|| "".equals(carNumber)
					|| berthId <= 0
					|| uid <= 0
					|| parkId <= 0
					|| groupId <= 0){
				resp.setResult(0);
				resp.setErrmsg("��������");
			}
			//-------------------------��������-----------------------//
			if(orderId <= 0){
				orderId = writeService.getkey("seq_order_tb");
			}
			logger.error("orderid:"+orderId);
			//----------------------------�ֲ�ʽ��--------------------------------//
			lock1 = commonMethods.getLock(carNumber);
			if(!memcacheUtils.addLock(lock1)){
				logger.error("lock1:"+lock1);
				resp.setResult(0);
				resp.setErrmsg("�����������");
				return resp;
			}
			lock2 = commonMethods.getLock(berthId);
			if(!memcacheUtils.addLock(lock2)){
				logger.error("lock2:"+lock2);
				resp.setResult(0);
				resp.setErrmsg("�����������");
				return resp;
			}
			//------------------------���̲߳��в�ѯ------------------------//
			ExecutorService pool = ExecutorsUtil.getExecutorService();
			ExeCallable callable0 = new ExeCallable(req, 0);//�ϰ��¼
			ExeCallable callable1 = new ExeCallable(req, 1);//��λ��Ϣ
			ExeCallable callable2 = new ExeCallable(req, 2);//���������ѯ��������
			ExeCallable callable3 = new ExeCallable(req, 3);//���������ѯ��������
			ExeCallable callable4 = new ExeCallable(req, 4);//�û���Ϣ
			ExeCallable callable5 = new ExeCallable(req, 5);//��������;
			ExeCallable callable6 = new ExeCallable(req, 6);//�¿���Ա
			ExeCallable callable7 = new ExeCallable(req, 7);//�ɰ󶨵ĳ���������
			
			Future<Object> future0 = pool.submit(callable0);
			Future<Object> future1 = pool.submit(callable1);
			Future<Object> future2 = pool.submit(callable2);
			Future<Object> future3 = pool.submit(callable3);
			Future<Object> future4 = pool.submit(callable4);
			Future<Object> future5 = pool.submit(callable5);
			Future<Object> future6 = pool.submit(callable6);
			Future<Object> future7 = pool.submit(callable7);
			
			WorkRecord workRecord = (WorkRecord)future0.get();
			Berth berth = (Berth)future1.get();
			Long count = (Long)future2.get();
			Long bcount = (Long)future3.get();
			Car car = (Car)future4.get();
			carType = (Integer)future5.get();
			Boolean monthUser = (Boolean)future6.get();
			Long berthOrderId = (Long)future7.get();
			//------------------------У���ϰ��¼---------------------//
			if(workRecord == null){
				resp.setResult(0);
				resp.setErrmsg("δ�ڵ�ǰ��λ��ǩ��");
				return resp;
			}
			workId = workRecord.getId();
			//logger.error("workId:"+workId);
			//----------------------------��λ��ϢУ��--------------------//
			if(berth == null){
				resp.setResult(0);
				resp.setErrmsg("��λ��Ϣ����");
				return resp;
			}
			//-------------------------У�����----------------------//
			//logger.error("count:"+count);
			if(count > 0){
				resp.setResult(0);
				resp.setErrmsg("�ó��������볡����");
				return resp;
			}
			//logger.error("bcount:"+bcount);
			if(bcount > 0){
				resp.setResult(0);
				resp.setErrmsg("�ò�λ�����볡����");
				return resp;
			}
			//----------------------------��ȡ�볡�û���Ϣ-----------------------//
			Long userId = -1L;//�������������˺�
			if(car != null){
				userId = car.getUin();
			}
			//----------------------------��ȡ��������--------------------------//
			//logger.error("carType:"+carType+",userId:"+userId);
			//---------------------------ȷ��������ʽ---------------------------//
			int cType = 2;//2:���ƽ���
			if(monthUser){//�¿���Ա
				cType =5;
				resp.setCtype(cType);
			}
			//logger.error("cType:"+cType);
			//--------------------------��ȡ�ɰ󶨵ĳ�����������Ϣ-------------------//
			Long startTime = commonMethods.getOrderStartTime(berthOrderId, uid, curTime);
			//logger.error("berthOrderId:"+berthOrderId+",orderid:"+orderId+",startTime:"+startTime);
			//---------------------------�����߼�---------------------------//
			resp.setOrderid(orderId);
			resp.setBtime(TimeTools.getTime_yyyyMMdd_HHmmss(startTime * 1000));
			
			GenPosOrderReq genPosOrderReq = new GenPosOrderReq();
			genPosOrderReq.setBerth(berth);
			genPosOrderReq.setBerthOrderId(berthOrderId);
			genPosOrderReq.setBindcard(bindcard);
			genPosOrderReq.setCarNumber(carNumber);
			genPosOrderReq.setCarType(carType);
			genPosOrderReq.setcType(cType);
			genPosOrderReq.setGroupId(groupId);
			genPosOrderReq.setImei(imei);
			genPosOrderReq.setNfc_uuid(nfc_uuid);
			genPosOrderReq.setOrderId(orderId);
			genPosOrderReq.setParkId(parkId);
			genPosOrderReq.setPrepay(prepay);
			genPosOrderReq.setStartTime(startTime);
			genPosOrderReq.setUid(uid);
			genPosOrderReq.setUserId(userId);
			genPosOrderReq.setVersion(version);
			genPosOrderReq.setWorkId(workId);
			
			
			GenPosOrderResp genPosOrderResp = null;
			if(payType == 0){
				//logger.error("�ֽ�Ԥ֧��");
				genPosOrderResp = genOrderCashService.genPosOrder(genPosOrderReq);
				//logger.error(genPosOrderResp.toString());
				if(genPosOrderResp.getResult() == 1){
					result = true;//�������ɳɹ�
					resp.setResult(1);
					resp.setErrmsg(genPosOrderResp.getErrmsg());
					return resp;
				}
			}else if(payType == 1){
				logger.error("ˢ��Ԥ֧��");
				genPosOrderResp = genOrderCardService.genPosOrder(genPosOrderReq);
				logger.error(genPosOrderResp.toString());
				if(genPosOrderResp.getResult() == 1){
					result = true;//�������ɳɹ�
					resp.setResult(1);
					resp.setErrmsg(genPosOrderResp.getErrmsg());
					return resp;
				}else if(genPosOrderResp.getResult() == -5){
					resp.setResult(-5);//δ���Ƭ,��ʾȥ����
					resp.setErrmsg(genPosOrderResp.getErrmsg());
					return resp;
				}else if(genPosOrderResp.getResult() == -6){
					resp.setResult(-6);//��ʾ�󶨿�Ƭ
					resp.setErrmsg(genPosOrderResp.getErrmsg());
					return resp;
				}
			}
			if(genPosOrderResp != null && genPosOrderResp.getResult() != 1){
				logger.error("��������ʧ��>>>orderid:"+orderId);
				resp.setResult(0);//Ӧ���Զ����㣬���ǽ���ʧ��
				resp.setErrmsg(genPosOrderResp.getErrmsg());
				return resp;
			}
			logger.error("�߼�����>>>orderid:"+orderId);
		} catch (Exception e) {
			logger.error(e);
		} finally {//ɾ����
			boolean b1 = memcacheUtils.delLock(lock1);
			boolean b2 = memcacheUtils.delLock(lock2);
			logger.error("ɾ����lock1:"+lock1+",b1:"+b1+",lock2:"+lock2+",b2:"+b2);
			if(result){//�������ɳɹ������½�������
				boolean b3 = commonMethods.updateInOutCar(workId, 0);
				logger.error("���½�������,b3:"+b3);
			}
		}
		resp.setResult(0);
		resp.setErrmsg("ϵͳ����");
		return resp;
	}
	
	class ExeCallable implements Callable<Object>{
		private GenPosOrderFacadeReq req;
		private int type;
		ExeCallable(GenPosOrderFacadeReq req, int type){
			this.req = req;
			this.type = type;
		}
		@Override
		public Object call() throws Exception {
			Object result = null;
			try {
				switch (type) {
				case 0:
					result = commonMethods.getWorkRecord(req.getUid());
					break;
				case 1:
					result = commonMethods.berth(req.getBerthId());
					break;
				case 2:
					result = writeService.getLong("select count(ID) from order_tb where " +
							"state =? and car_number=? and comid=? ", 
							new Object[]{0, req.getCarNumber(), req.getParkId()});//���������ѯ��������
					break;
				case 3:
					result = writeService.getLong("select count(p.id) from com_park_tb p,order_tb o" +
							" where p.order_id=o.id and p.state=? and o.state=? and p.id=?", 
							new Object[]{1, 0, req.getBerthId()});
					break;
				case 4:
					result = readService.getPOJO("select * from car_info_tb where " +
							" car_number=? and state=? ", new Object[]{req.getCarNumber(), 1}, Car.class);
					break;
				case 5:
					if(req.getCarType() <= 0){
						result = commonMethods.getCarType(req.getCarNumber(), req.getParkId());
					}else{
						result = req.getCarType();//pos�����ܴ��ò���
					}
					break;
				case 6:
					result = commonMethods.isMonthUser(req.getCarNumber(), req.getParkId());;
					break;
				case 7:
					result = commonMethods.getPreBerthOrderId(req.getBerthId());
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
