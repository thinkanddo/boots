package com.bonc.bcos.config;

import com.bonc.bcos.service.tasks.FTPListener;
import com.bonc.bcos.service.tasks.FTPUtil;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.*;

@WebListener
public class SessionListener implements HttpSessionListener, HttpSessionAttributeListener {

    @Override
    public void attributeRemoved(HttpSessionBindingEvent httpSessionBindingEvent) {
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent httpSessionBindingEvent) {
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) throws ClassCastException {
        HttpSession session = event.getSession();
        FTPListener listener = (FTPListener)session.getAttribute(FTPUtil.PACKAGE_LISTENER_KEY);
        if(null!=listener){
            listener.stop();
        }
        session.removeAttribute(FTPUtil.PACKAGE_LISTENER_KEY);
    }
}