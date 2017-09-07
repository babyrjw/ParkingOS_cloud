package com.zld.struts.request;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.zld.AjaxUtil;
import com.zld.impl.MongoClientFactory;
import com.zld.service.DataBaseService;
import com.zld.service.PgOnlyReadService;
import com.zld.utils.RequestUtil;
import com.zld.utils.StringUtils;

public class PicsHandleAction extends Action {
	@Autowired
	private DataBaseService daService;
	@Autowired
	private PgOnlyReadService pgOnlyReadService;
	
	private Logger logger = Logger.getLogger(PicsHandleAction.class);
	public ActionForward execute(ActionMapping mapping,ActionForm form,HttpServletRequest request,HttpServletResponse response) throws Exception{
		String action = RequestUtil.processParams(request, "action");
		String token =RequestUtil.processParams(request, "token");
		Long comid = RequestUtil.getLong(request, "comid", -1L);
		Map<String,Object> infoMap  = new HashMap<String, Object>();
		if(token==null||"null".equals(token)||"".equals(token)){
			infoMap.put("result", "fail");
			infoMap.put("message", "token��Ч!");
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));
			return null;
		}
		Long uid =validToken(token);
		if(uid == null){
			infoMap.put("result", "fail");
			infoMap.put("message", "token��Ч!");
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));
			return null;
		}
		if(action.equals("downloadvisitpics")){
			Long ticketid = RequestUtil.getLong(request, "shopticket_id", -1L);
			downloadticketPics(ticketid, request, response);
			//http://192.168.199.239/zld/picshandle.do?action=downloadvisitpics&shopticket_id=35057&token=1856ed1581facbd63d22e76e461f59f3
		}else if(action.equals("uploadticketpics")){
			Long shopticket_id = RequestUtil.getLong(request, "shopticket_id", -1L);
			if(shopticket_id == -1){
				AjaxUtil.ajaxOutput(response, "-1");
				return null;
			}
			String r = uploadticketPic2Mongodb(request, response, shopticket_id);
			AjaxUtil.ajaxOutput(response, r);
			//http://192.168.199.239/zld/picshandle.do?action=uploadticketpics&token=e6c435a27cf1f4a11d11c56d0cebc614&shopticket_id=
		}
		return null;
	}
	
	/**
	 * ��֤token�Ƿ���Ч
	 * @param token
	 * @return uin
	 */
	private Long validToken(String token) {
		Map tokenMap = pgOnlyReadService.getMap("select * from user_session_tb where token=?", new Object[]{token});
 		Long uin = null;
		if(tokenMap!=null&&tokenMap.get("uin")!=null){
			uin = (Long) tokenMap.get("uin");
		}
		return uin;
	}
	
	private void downloadticketPics (Long ticketid,HttpServletRequest request,HttpServletResponse response) throws Exception{
		logger.error("download ticketPics from mongodb....");
		System.err.println("downloadticketPics from mongodb file:ticketid="+ticketid);
		if(ticketid!=null){
			DB db = MongoClientFactory.getInstance().getMongoDBBuilder("zld");//
			DBCollection collection = db.getCollection("ticket_pics");
			BasicDBObject document = new BasicDBObject();
			BasicDBObject condation = new BasicDBObject();
			document.put("ticketid", ticketid);
			//������ʱ������������
			condation.put("ctime", -1);
			DBCursor objs = collection.find(document).sort(condation).limit(1);
			DBObject obj = null;
			while (objs.hasNext()) {
				obj = objs.next();
			}
			if(obj == null){
				AjaxUtil.ajaxOutput(response, "");
				return;
			}
			byte[] content = (byte[])obj.get("content");
			db.requestDone();
			response.setDateHeader("Expires", System.currentTimeMillis()+4*60*60*1000);
			//response.setStatus(httpc);
			Calendar c = Calendar.getInstance();
			c.set(1970, 1, 1, 1, 1, 1);
			response.setHeader("Last-Modified", c.getTime().toString());
			response.setContentLength(content.length);
			response.setContentType("image/jpeg");
		    OutputStream o = response.getOutputStream();
		    o.write(content);
		    o.flush();
		    o.close();
		    response.flushBuffer();
		    //response.reset();
		    System.out.println("mongdb over.....");
		}else {
			AjaxUtil.ajaxOutput(response, "-1");
		}
	}
	
	
	/*
	 * �ϴ��Ż�ȯͼƬ
	 */
	private String uploadticketPic2Mongodb (HttpServletRequest request,HttpServletResponse response,Long ticketid) throws Exception{
		logger.error("begin upload ticket picture....");
		Map<String, String> extMap = new HashMap<String, String>();
	    extMap.put(".jpg", "image/jpeg");
	    extMap.put(".jpeg", "image/jpeg");
	    extMap.put(".png", "image/png");
	    extMap.put(".gif", "image/gif");
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
			return "-1";
		}
		String filename = ""; // �ϴ��ļ����浽���������ļ���
		InputStream is = null; // ��ǰ�ϴ��ļ���InputStream����
		// ѭ�������ϴ��ļ�
		for (FileItem item : items){
			if (!item.isFormField()){
				if (item.getName() != null && !item.getName().equals("")){// �����ϴ��ļ�
					// �ӿͻ��˷��͹������ϴ��ļ�·���н�ȡ�ļ���
					logger.error(item.getName());
					filename = item.getName().substring(
							item.getName().lastIndexOf("\\")+1);
					is = item.getInputStream(); // �õ��ϴ��ļ���InputStream����
					}
				}
		}
		String file_ext =filename.substring(filename.lastIndexOf(".")).toLowerCase();// ��չ��
		String picurl = ticketid + "_" + System.currentTimeMillis()/1000 + file_ext;
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
			  
		    DBCollection collection = mydb.getCollection("ticket_pics");
		  //  DBCollection collection = mydb.getCollection("records_test");
			  
			BasicDBObject document = new BasicDBObject();
			document.put("ticketid", ticketid);
			document.put("ctime",  System.currentTimeMillis()/1000);
			document.put("type", extMap.get(file_ext));
			document.put("content", content);
			document.put("filename", picurl);
			  //��ʼ����
			mydb.requestStart();
			collection.insert(document);
			  //��������
			mydb.requestDone();
			in.close();        
		    is.close();
		    byteout.close();
		} catch (Exception e) {
			e.printStackTrace();
			return "-1";
		}finally{
			if(in!=null)
				in.close();
			if(byteout!=null)
				byteout.close();
			if(is!=null)
				is.close();
		}
	  
		return "1";
	}
}
