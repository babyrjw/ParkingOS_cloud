package com.zld.utils;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zld.AjaxUtil;

/**
 * ���ŷ���
 * @author needytwh
 *
 */
public class MessageUtils {
	
	private MessageUtils(){};
	
	private static final  String sendUrl="http://112.74.139.4:8002/sms3_api/xmlapi/send.jsp";
	
	private static final  String statusUrl="http://112.74.139.4:8002/sms3_api/xmlapi/report.jsp";
	
	private static final  String upUrl="http://112.74.139.4:8002/sms3_api/xmlapi/upmsg.jsp";
	
	private static final String userid = "201350";
	private static final String password = "db3971874d0fc29b37b9fb06f86996ef";
	
	public static String sendMsg(String phone,String content) throws HttpException, IOException{
		
		Document createDocument = DocumentHelper.createDocument();
		Element rootElement = DocumentHelper.createElement("root");
		createDocument.setRootElement(rootElement);
		
		rootElement.addAttribute("userid", userid);
		rootElement.addAttribute("password", password);
		
		Element submitElement = rootElement.addElement("submit");
		submitElement.addAttribute("phone", phone);
		submitElement.addAttribute("content", content);
		
		String result = submitXMLUrl(createDocument,sendUrl);
		return result;
	}

	
	private static String submitUrl(Document createDocument,String url) throws IOException, HttpException {
		HttpClient httpClient = new HttpClient();
		PostMethod method = new PostMethod(url);
		method.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "UTF-8");
		httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
        httpClient.getHttpConnectionManager().getParams().setSoTimeout(300000);
		String paramXML = createDocument.asXML();
		System.err.println(paramXML);
		RequestEntity requestEntity=new StringRequestEntity(paramXML);
		method.setRequestEntity(requestEntity);
		httpClient.executeMethod(method);
		String result = method.getResponseBodyAsString();
		return result;
	}
	private static String submitXMLUrl(Document createDocument,String url) throws IOException, HttpException {
		String paramXML = createDocument.asXML();
		System.err.println(">>>>>>>>>>>>>>>>sendphonemessage ���ŷ��ͣ�"+paramXML);
		String result = HttpAccess.postXmlRequest(url, paramXML, "utf-8", "");
		System.err.println(">>>>>>>>>>>>>>>>sendphonemessage ���ͽ����"+result);
		return result;
	}
	
	public static String getMsgStatus(String userid,String password) throws HttpException, IOException{
		Document createDocument = DocumentHelper.createDocument();
		Element rootElement = DocumentHelper.createElement("root");
		createDocument.setRootElement(rootElement);
		
		rootElement.addAttribute("userid", userid);
		rootElement.addAttribute("password", password);
		String result = submitUrl(createDocument,statusUrl);
		return result;
	}
	
	public static String getMsgUp(String userid,String password) throws HttpException, IOException{
		Document createDocument = DocumentHelper.createDocument();
		Element rootElement = DocumentHelper.createElement("root");
		createDocument.setRootElement(rootElement);
		
		rootElement.addAttribute("userid", userid);
		rootElement.addAttribute("password", password);
		String result = submitUrl(createDocument,upUrl);
		return result;
	}
	
	
	public static MessageUtils getInstance() {
		return new MessageUtils();
	}
	
	public static void main(String[] args) {
		try {
			//System.out.println(sendMsg("93104","callbkf","18101333937","��ͣ������ ����"));
			String content = "������֤����8449";
			String result = sendMsg("18101333937",content);
			if(result!=null&&result.indexOf("return=\"0\"")!=-1)
				 System.err.println("���ͳɹ�");
		} catch (HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
