package pay;

/* *
 *������AlipayConfig
 *���ܣ�����������
 *��ϸ�������ʻ��й���Ϣ������·��
 *�汾��3.3
 *���ڣ�2012-08-10
 *˵����
 *���´���ֻ��Ϊ�˷����̻����Զ��ṩ���������룬�̻����Ը����Լ���վ����Ҫ�����ռ����ĵ���д,����һ��Ҫʹ�øô��롣
 *�ô������ѧϰ���о�֧�����ӿ�ʹ�ã�ֻ���ṩһ���ο���
	
 *��ʾ����λ�ȡ��ȫУ����ͺ��������ID
 *1.������ǩԼ֧�����˺ŵ�¼֧������վ(www.alipay.com)
 *2.������̼ҷ���(https://b.alipay.com/order/myOrder.htm)
 *3.�������ѯ���������(PID)��������ѯ��ȫУ����(Key)��

 *��ȫУ����鿴ʱ������֧�������ҳ��ʻ�ɫ��������ô�죿
 *���������
 *1�������������ã������������������������
 *2���������������ԣ����µ�¼��ѯ��
 */

public class AlipayConfig {
	
	//�����������������������������������Ļ�����Ϣ������������������������������
	// ���������ID����2088��ͷ��16λ��������ɵ��ַ���
	public static String partner =PayConfigDefind.getValue("PARTNER");
	
	 /** ����appId  */
    //TODO !!!! ע����appId������Ϊ�������Լ��ķ���id  ����ֻ�Ǹ�����id
    public static final String APP_ID =PayConfigDefind.getValue("APP_ID")
    
	
	/**֧��������*/
    public static final String ALIPAY_GATEWAY    = PayConfigDefind.getValue("ALI_PAY_GATEWAY");
		// ֧�����Ĺ�Կ�������޸ĸ�ֵ
	public static String ali_public_key  =PayConfigDefind.getValue("ALI_PUBLIC_KEY");
	//ɨ�빫Կ
	public static String ALIPUBLICKEY4QR  =PayConfigDefind.getValue("ALI_PUBLICKEY_4QR");
	
	// �ַ������ʽ Ŀǰ֧�� gbk �� utf-8
	public static String input_charset =PayConfigDefind.getValue("CHARSET");
	
	// ǩ����ʽ �����޸�
	public static String sign_type =PayConfigDefind.getValue("SIGN_TYPE");

}
