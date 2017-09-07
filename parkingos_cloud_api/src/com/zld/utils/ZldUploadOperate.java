package com.zld.utils;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.zld.service.DataBaseService;
import com.zld.service.PgOnlyReadService;

@Repository
public class ZldUploadOperate {
	/**
	 * ��������
	 * @param context
	 * @param paramMap
	 * @param string
	 * @param type 0ע�� 1���� 2ɾ�� -1ʱȡoperateֵ
	 * @return
	 */
	public Map<String, Object> handleData(ServletContext context,Map<String, String> paramMap,String params,
			String tableName, int type) {
		Map<String, Object> returnMap =new HashMap<String, Object>();
		if(type==-1){//�ϴ������º�ɾ����ͬһ�ӿ���
			String operate = paramMap.get("operate");
			if(operate!=null&&Check.isNumber(operate)){
				type = Integer.valueOf(operate);
				//paramMap.remove("operate");
			}
			if(type<0||type>2){
				returnMap.put("status", "2");
				returnMap.put("resultCode", "100");
				returnMap.put("message", "������operate�Ƿ�Ϸ�,����ֵ��0,1,2��ʵ�ʣ�"+operate);
				returnMap.put("data", "{}");
			}
		}
		String message="ע��ɹ�";
		if(type==1){
			message="���³ɹ�";
		}else if(type==2){
			message="ɾ���ɹ�";
		}
		if(validateToken(paramMap.get("token"), context)){//token��Ч
			if(ZldUploadUtils.validateSign(params)){//ǩ����Ч
				Map<String, Object> preUpdateMap = ZldUploadUtils.getData(paramMap,tableName,type);
				String errmesg = (String)preUpdateMap.get("errmesg");
				if(errmesg!=null&&!"".equals(errmesg)){//�д��󣬷���
					returnMap.put("status", "2");
					returnMap.put("resultCode", "100");
					returnMap.put("message", preUpdateMap.get("errmesg"));
					returnMap.put("data", "{}");
				}else {//û�д��󣬴������ݿ�
					String sql = (String)preUpdateMap.get("sql");
					Object[] sqlParams = (Object[])preUpdateMap.get("params");
					int ret = 0;
					try {
						ret =update(sql, sqlParams, context);
					} catch (Exception e) {
						System.out.println(e.getMessage());
						if(e.getMessage().indexOf("order_tb_uin_create_time_end_time_key")!=-1){
							returnMap.put("status", "1");
							returnMap.put("resultCode", "0");
							returnMap.put("message", "����Ϣ���ظ�");
							returnMap.put("data", "{}");
						}else {
							ret =-1;
							String err = "��������Ƿ���ȷ";
							returnMap.put("status", "2");
							returnMap.put("resultCode", "500");
							returnMap.put("message", "д�����ݴ���:"+err);
							returnMap.put("data", "{}");
						}
					}
					if(ret==1){//���ݿ�����ɹ�
						returnMap.put("status", "1");
						returnMap.put("resultCode", "0");
						returnMap.put("message", message);
						returnMap.put("data", "{}");
					}else if(ret==0){//���ݿ��������
						returnMap.put("status", "2");
						returnMap.put("resultCode", "500");
						returnMap.put("message", "д�����ݴ���");
						returnMap.put("data", "{}");
					}
				}
			}else {//ǩ����Ч
				returnMap.put("status", "2");
				returnMap.put("resultCode", "102");
				returnMap.put("message", "ǩ����Ч");
				returnMap.put("data", "{}");
			}
		}else {
			returnMap.put("status", "2");
			returnMap.put("resultCode", "101");
			returnMap.put("message", "token��Ч");
			returnMap.put("data", "{}");
		}
		System.out.println(returnMap);
		return returnMap;
	}

	
	//��֤token
	private boolean validateToken(String token,ServletContext context){
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
		PgOnlyReadService daService = (PgOnlyReadService) ctx.getBean("pgOnlyReadService");
		Map comMap = daService.getPojo("select * from user_session_tb where token=?", new Object[]{token});
		if(comMap!=null){
			return true;
		}
		return false;
	}
	
	//д�����ݿ�
	private int update(String sql,Object[] parmas,ServletContext context){
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
		DataBaseService daService = (DataBaseService) ctx.getBean("dataBaseService");
		int ret = daService.update(sql, parmas);
		return ret;
	}
	//���ݳ���uuid�鳵��ID
	public String getComIdByParkUUID(String uuid,ServletContext context){
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
		DataBaseService daService = (DataBaseService) ctx.getBean("dataBaseService");
		Map comMap = daService.getPojo("select id from com_info_tb where park_uuid=?", new Object[]{uuid});
		if(comMap!=null&&comMap.get("id")!=null){
			return comMap.get("id")+"";
		}
		return "-1";
	}
	//�����շ�Աuuid��ѯ�շ�Ա���
	public String getUserIdByUUID(String uuid,ServletContext context){
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
		DataBaseService daService = (DataBaseService) ctx.getBean("dataBaseService");
		Map comMap = daService.getPojo("select id from user_info_tb where uuid=?", new Object[]{uuid});
		if(comMap!=null&&comMap.get("id")!=null){
			return comMap.get("id")+"";
		}
		return "-1";
	}
	
	//����uuid��ѯ��Ӫ��˾���
	public String getCompanyIddByUUID(String uuid,ServletContext context){
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
		DataBaseService daService = (DataBaseService) ctx.getBean("dataBaseService");
		Map comMap = daService.getPojo("select id from company_tb where uuid=?", new Object[]{uuid});
		if(comMap!=null&&comMap.get("id")!=null){
			return comMap.get("id")+"";
		}
		return "-1";
	}
	//����uuid��ѯ��λ�α��
	public String getBerthsecIdIdByUUID(String uuid,ServletContext context){
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
		DataBaseService daService = (DataBaseService) ctx.getBean("dataBaseService");
		Map comMap = daService.getPojo("select id from com_berthsecs_tb where uuid=?", new Object[]{uuid});
		if(comMap!=null&&comMap.get("id")!=null){
			return comMap.get("id")+"";
		}
		return "-1";
	}
	//����uuid��ѯ��λ�α��
	public Long getUinByCarNumber(String car_number,ServletContext context){
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
		DataBaseService daService = (DataBaseService) ctx.getBean("dataBaseService");
		Map comMap = daService.getPojo("select uin from car_info_tb where car_number=?", new Object[]{car_number});
		if(comMap!=null&&comMap.get("uin")!=null){
			return (Long)comMap.get("uin");
		}
		return -1L;
	}
}
