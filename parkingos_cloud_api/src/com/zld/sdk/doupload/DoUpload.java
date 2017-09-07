package com.zld.sdk.doupload;

import net.sf.json.JSONObject;

/**
 * @author liuqb
 * @date  2017-3-31
 */
public interface DoUpload {
	
	/**
	 * �ϴ�̧�˼�¼�ӿ�����
	 * @param data
	 * @return
	 */
	public String uploadLiftrod(String comid,String data);
	/**
	 * �ϴ��󶨳��ͽӿ�����
	 * ����json�а���
	 * ������ţ�comid���󶨳�����Ϣ��carNumberTypeInfo;
	 * �ϴ�����ֵ�����
	 * ����̧�˼�¼ʱ���ɹ�����ʧ�ܣ�state��lineId��line_id�����ذ󶨳���ID��localId��
	 * ���̧�˼�¼ʱ���ɹ�����ʧ�ܣ�state��
	 * @param data
	 * @return
	 */
	public String doUploadCarType(String sign, String token,JSONObject data);
	/**
	 * �ϴ�Ա��������¼�ӿ�����
	 * @param data
	 * @return
	 */
	public String uploadWorkRecord(String comid,String data);
	/**
	 * �ϴ�����������״̬�ӿ�����
	 * @param data
	 * @return
	 */
	public String doUploadSeverState(String sign, String token,JSONObject data);
	/**
	 * �ϴ���ͤ�˹���״̬�ӿ�����
	 * @param data
	 * @return
	 */
	public String doUploadBrakeState(String sign, String token,JSONObject data);
	
	/**
	 * ��¼tcp����Ľӿ�����
	 * @param data
	 * @param sign
	 * @return
	 */
	public String doLogin(JSONObject data,String sign,String sourceIP);
	
	/**
	 * ������¼�ɹ�����֤ǩ���Ƿ�һ��
	 * ����ֵΪ1��ʾ����ǩ��һ�£�������������쳣
	 * @param preSign
	 * @param token
	 * @param data
	 * @return
	 */
	public String checkSign(String preSign,String ukey,String data);
	
	/**
	 * �����볡ʱ�ӿ�����
	 * @param token��������Ӧ��token��ʶ����������ύ
	 * @param data������sdk�ϴ�����
	 * @return
	 */
	public String enterPark(String comid,String data);
	
	/**
	 * ���������ϴ�ʱ�ӿ�����
	 * @param token��������Ӧ��token��ʶ����������ύ
	 * @param data������sdk�ϴ�����
	 * @return
	 */
	public String exitPark(String comid,String data);
	/**
	 * ��������ʱ�ӿ�����
	 * @param token��������Ӧ��token��ʶ����������ύ
	 * @param data������sdk�ϴ�����
	 * @return
	 */
	public String outPark(String comid,String data);
	
	/**
	 * ������־�ϴ��ӿ�����
	 * @param token��������Ӧ��token��ʶ����������ύ
	 * @param data������sdk�ϴ�����
	 * @return
	 */
	public String uploadLog(String comid,String data);
	/**
	 * �ϴ�����ȯ�ӿ�����
	 * 
	 * @param data
	 * @return
	 */
	public String uploadTicket(String comid,String data);
	
	/**
	 * �ϴ��¿���Ա��Ϣ
	 * @param data
	 * @return
	 */
	public String uploadMonthMember(String comid,String data);
	
	/**
	 * �ϴ��¿���Ϣ
	 * @param data
	 * @return
	 */
	public String uploadMonthCard(String comid,String data);
	
	/**
	 * �ϴ��۸���Ϣ
	 * @param data
	 * @return
	 */
	public String uploadPrice(String token,String data);
	
	/**
	 * ����ͼƬ�ϴ�
	 * @param parkId
	 * @param data
	 * @return
	 */
	public String uploadCarpic(String parkId,String data);
	/**
	 * �¿����Ѽ�¼�ϴ�
	 * @param parkId
	 * @param data
	 * @return
	 */
	public String monthPayRecord(String parkId,String data);
	
	/**
	 * �۸���Ϣͬ�����޸����ݿ�״̬
	 * @param comid
	 * @param id
	 * @return
	 */
	public String priceSyncAfter(Long comid,String id,Integer state,Integer operate);
	
	/**
	 * �¿���Ϣͬ�����޸����ݿ�״̬
	 * @param comid
	 * @param id
	 * @return
	 */
	public String packageSyncAfter(Long comid,String id,Integer state,Integer operate);
	
	/**
	 * �¿���Ա��Ϣͬ�����޸����ݿ�״̬
	 * @param comid
	 * @param id
	 * @return
	 */
	public String userInfoSyncAfter(Long comid,String id,Integer state,Integer operate);
	/**
	 * ����tokenֵ��ѯ��Ӧ��ע�ᳵ��
	 * @param token
	 * @return
	 */
	public String tokenCheck(String token);
	//�˳���¼
	public void logout(String parkId );
	//����
	public void lockCar(String jsonData);
	public String checkTokenSign(String token, String sign,String data);
	/**
	 * �ϴ������շ�Ա��Ϣ�ӿ�
	 * @param parkId
	 * @param data
	 * @return
	 */
	public String uploadCollector(String parkId, String data);
	
	/**
	 * �����շ�ϵͳ�ϴ����¿��ײͼ۸�洢��memcacheUtils��
	 * @param parkId
	 * @param data
	 */
	public String queryProdprice(String parkId,String data);
	
	/**
	 * ̧֪ͨ�˲�����Ϣ
	 * @param jsonData
	 */
	public void operateLiftrod(Long comid,String channelId,Integer state,Integer operate);
}

