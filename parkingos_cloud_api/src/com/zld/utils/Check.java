package com.zld.utils;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * 
 * ��Ŀ���ƣ�callback �����ƣ�Check �������� ���һ���������ֻ��Ż��ߵ绰��,uin�ŵȵȷ��� �����ˣ�shanyz ����ʱ�䣺Apr 3,
 * 2010 2:54:36 PM �޸��ˣ�shanyz �޸�ʱ�䣺Apr 3, 2010 2:54:36 PM �޸ı�ע��
 * 
 * @version
 * 
 */
public class Check {

	public static boolean checkPhone(String phonenumber) {
		if (phonenumber == null || phonenumber.equals(""))
			return false;
		String phone = "0\\d{2,3}\\d{7,8}";//������
		String shortPhone="\\d{7,8}";//��������
		Pattern p = Pattern.compile(phone);
		Matcher m = p.matcher(phonenumber);
		if(!m.matches()){
			p = Pattern.compile(shortPhone);
			m=p.matcher(phonenumber);
		}
		return m.matches();
	}

	public static boolean checkMobile(String mobilenumber) {
		if (mobilenumber == null || mobilenumber.equals(""))
			return false;
		String mobile = "^((\\+{0,1}0){0,1})1[0-9]{10}";
		Pattern p = Pattern.compile(mobile);
		Matcher m = p.matcher(mobilenumber);
		return m.matches();
	}

	public static boolean checkUin(String uin) {
		if (uin == null || uin.equals(""))
			return false;
		Pattern pattern = Pattern.compile("[0-9]*");
		Matcher m = pattern.matcher(uin);
		  return m.matches();
	}
	/**
	 * 
	* checkMobileHead   
	* TODO  ��� �ֻ��Ƿ���0��ͷ�������ͷ��ȥ��0
	* @param   name   
	* @param  @return    
	* @return String     
	* @Exception   
	* @since  CodingExample��Ver 1.1
	 */
	public static String checkMobileHead(String mobilenumber) {
		if (mobilenumber == null || mobilenumber.equals(""))
			return null;
		if(checkMobile(mobilenumber)&&mobilenumber.startsWith("0")) {
			mobilenumber=mobilenumber.substring(1, mobilenumber.length());
		}
		  return mobilenumber;
	}
	/**
	 * 
	* getKufuName   
	* TODO  ͨ�������� talk_content �� �õ��ͷ�����
	* @param   name   
	* @param  @return    
	* @return String     
	* @Exception   
	* @since  CodingExample��Ver 1.1
	 */
	public static String  getKufuName(String talk_content) {
		if(talk_content==null){
			return "";
		}
		String pString="<br>+[^<br>]+\\(\\d{4,8}\\)";//ƥ����:<br>��С��(1086)
		String resultString="";
		Pattern p = Pattern.compile(pString);
		Matcher m = p.matcher(talk_content);
		if(m.find()) {
			resultString=m.group();
			resultString=resultString.substring(resultString.indexOf("<br>")+4, resultString.indexOf("("));
		}		
		return resultString;
	}
/**
 * 
* dealPhone   
* TODO  	//01013911001412 ȥ���ֻ�ǰ������,�̶��绰��������
* @param   name   
* @param  @return    
* @return String     
* @Exception   
* @since  CodingExample��Ver 1.1
 */
	public static String dealPhone(String phone) {
		if (phone.length() > 13) {//����Ϊ�ֻ�������
			String[] quhaoStrings = { "010", "020", "021", "022", "023", "024",
					"025", "027", "028", "029" };
			String result = null;
			for (int i = 0; i < quhaoStrings.length; i++) {
				if (phone.startsWith(quhaoStrings[i])) {
					result = phone.substring(3, phone.length());
					break;
				}
			}
			if (result == null)
				result = phone.substring(4, phone.length());
			return result;
		} else {
			return phone;
		}

	}
	/**��֤�绰 
	 * �ֻ���1��ͷ���ڶ�λ��3��4��5��8������11λ���֣�����0��ͷ��������ǰ��һ����12λ����
	 * ��ȷ��1340000011��013269710010(��;)
	 * �̶��绰����2��9��ͷ������7��8λ���ֻ���0��ͷ�ڶ�λ��1��9��������11��12λ���֣�
	 * �����Ƿ����֣����ܴ��ֻ��ţ�
	 * ��010-88998899��010-88998899-22��(010)88999933
	 * Params:type:"m"�ֻ�,"t"�̶��绰
	*/
    public static boolean checkPhone(String phone,String type){  
    	String teleReg ="^(0[1-9]{1}\\d{9,10})|([2-9]\\d{6,7})$";
		String mobilReg = "^(1[3-8]\\d{9})|(01[3-8]\\d{9})$";
		if(type.equals("m"))
			return phone.matches(mobilReg);
		else if(type.equals("t"))
			return phone.matches(teleReg);
		return false;
    } 
    /**
     * �ж��Ƿ�������
     * @param value
     * @return
     */
    public static boolean isNumber(String value){
    	// int -2147483648����2147483647
    	if(value==null)
    		return false;
    	if(value.length()>9&&isLong(value)){
    		Long  l = Long.parseLong(value);
    		if(l<=Integer.MAX_VALUE)
    			return true;
    	}
    	return value.matches("^\\d{1,9}");
    }
    /**
     * �ж��Ƿ��ǳ�����
     * @param value
     * @return
     */
    public static boolean isLong(String value){
    	if(value==null)
    		return false;
//    	if( value.matches("^\\d+")){
//    		
//    	}
    	try {
			if (Long.parseLong(value) <= Long.MAX_VALUE)
				return true;
		} catch (Exception e) {
			return false;
		}
    	return false;
    }
    
    public static boolean checkEmail(String value) {
    	if(value == null) {
    		return false;
    	} 
    	return value.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$");
    }
    
    /**
     * �ж��Ƿ��ǳ�����
     * @param value
     * @return
     */
    public static boolean isDouble(String value){
    	if(value==null)
    		return false;
		try {
			Double d = Double.valueOf(value);
			return true;
		} catch (Exception e) {
			return false;
		}
    }
    
    /**
     * �ж��Ƿ�Ϊ��
     * @param value
     * @return
     */
    public static boolean isEmpty(String value) {
		int strLen;
		if (value == null || (strLen = value.length()) == 0|| "null".equals(value)) {
			return true;
		}
		for (int i = 0; i < strLen; i++) {
			if ((Character.isWhitespace(value.charAt(i)) == false)) {
				return false;
			}
		}
		return true;
	} 
}
