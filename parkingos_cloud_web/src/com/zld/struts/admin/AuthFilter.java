package com.zld.struts.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.zld.AjaxUtil;

public class AuthFilter implements Filter {
	private String failurl = null;
	
	Logger logger = Logger.getLogger(AuthFilter.class);
	
	@SuppressWarnings("unchecked")
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		/*try {
			HttpServletRequest httpServletRequest = ((HttpServletRequest) request);
			String requestUrl = httpServletRequest.getRequestURI().split("/")[1];
			String action = httpServletRequest.getParameter("action");
			String curl = (action != null)? (requestUrl + "?action=" +action ) : requestUrl;
			RequestDispatcher dispatcher = request.getRequestDispatcher(failurl);
			List<Map<String, Object>> authList = null;
			List<Map<String, Object>> allauth = null;
			if(httpServletRequest.getSession().getAttribute("authlist") != null){
				authList= (List<Map<String, Object>>)httpServletRequest.getSession().getAttribute("authlist");
			}
			if(httpServletRequest.getSession().getAttribute("allauth") != null){
				allauth= (List<Map<String, Object>>)httpServletRequest.getSession().getAttribute("allauth");
			}
			if(allauth != null ){
				boolean regflag = false;
				List<String> allauthList = new ArrayList<String>();
				List<String> ownauthList = new ArrayList<String>();
				for(Map<String, Object> map : allauth){//�������е���ע��Ȩ��
					String url = (String)map.get("url");
					String actions = null;
					if(map.get("actions") != null){
						actions = (String)map.get("actions");
					}
					allauthList.add(url);
					if(url.contains("?")){//��Щurl������в���
						url = url.split("\\?")[0];//����?��������ʾ������Ӧ�Ĳ�ͬ���壬������ʹ��ʱҪ����ת�崦��
					}
					if(actions != null){
						String[] actionarr = actions.split(",");
						for(int i = 0; i< actionarr.length; i++){
							String act = actionarr[i];
							if(act.contains(".")){//��Щaction��������url,����parkinfo.do?action=withdraw
								allauthList.add(act);
							}else if(!act.equals("")){
								allauthList.add(url + "?action=" + act);
							}
						}
					}
				}
				if(authList != null){
					for(Map<String, Object> map : authList){//����ӵ�е�Ȩ��
						String url = (String)map.get("url");
						String actions = null;
						String sub_auth = null;
						if(map.get("actions") != null){
							actions = (String)map.get("actions");
						}
						if(map.get("sub_auth") != null){
							sub_auth = (String)map.get("sub_auth");
						}
						ownauthList.add(url);
						if(url.contains("?")){
							url = url.split("\\?")[0];//����?��������ʾ������Ӧ�Ĳ�ͬ���壬������ʹ��ʱҪ����ת�崦��
						}
						if(actions != null && sub_auth != null){
							String[] actionarr = actions.split(",");
							String[] authnum = sub_auth.split(",");
							for(int j=0;j<authnum.length;j++){
								Integer actnum = Integer.valueOf(authnum[j]);
								for(int i = 0; i< actionarr.length; i++){
									if(i == actnum){
										String act = actionarr[i];
										if(act.contains(".")){//��Щaction��������url,����parkinfo.do?action=withdraw
											ownauthList.add(act);
										}else if(!act.equals("")){
											ownauthList.add(url + "?action=" + act);
										}
									}
								}
							}
						}
					}
				}
				
				for(String aurl : allauthList){//�жϵ�ǰ�����Ƿ�����ע��Ȩ����
					if(aurl.contains(curl)){
						regflag = true;
						break;
					}
				}
				if(regflag){
					boolean authflag = false;
					for(String aurl : ownauthList){//�жϵ�ǰ�����Ƿ�����ע��Ȩ����
						if(aurl.contains(curl)){
							authflag = true;
							break;
						}
					}
					if(!authflag){
						dispatcher.forward(request, response);
						return;
					}
				}
			} 
		} catch (Exception e) {
			logger.error("auth check filter exception", e);
		}*/
		chain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub
		failurl = filterConfig.getInitParameter("failurl");
	}
	
	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}
}
