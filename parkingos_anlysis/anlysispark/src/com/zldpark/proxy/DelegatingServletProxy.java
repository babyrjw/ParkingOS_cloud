package com.zldpark.proxy;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class DelegatingServletProxy extends GenericServlet {

	private String targetBean;
    private Servlet proxy;
    public void init() throws ServletException {
        this.targetBean = this.getServletName();
        getServletBean();
        proxy.init(getServletConfig());
    }
    
    @Override
    public void service(ServletRequest req, ServletResponse res)
            throws ServletException, IOException {
        proxy.service(req, res);
    }
 
    private void getServletBean() {
        //ʵ��һ��servlet����,�ô�����WebApplicationContext�������applicationContext.xml�ж����servlet�Ķ��󣬲�������ί�и�applicationContext.xml�ж����servlet��
        WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
        this.proxy = (Servlet) wac.getBean(targetBean);
    }

}
