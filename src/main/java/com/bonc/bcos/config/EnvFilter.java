package com.bonc.bcos.config;

import com.bonc.bcos.sys.SessionCache;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class EnvFilter extends OncePerRequestFilter {
    private final Boolean mainFlag;

    private static final String[] DEFAULT_EXECUTE = new String[]{
            "/v1/callback",
            "/v1/version",
            "/v1/image",
            "/v1/env"
    };


    // 制作版本库的时候需要用到的特殊地址
    private static final String[] MAIN_EXECUTE = new String[]{
            "/v1/exec",
            "/v1/global",
            "/v1/elfinder"
    };

    public EnvFilter() {
        this(false);
    }

    public EnvFilter(Boolean mainFlag) {
        this.mainFlag = mainFlag;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String url = request.getRequestURI();
        if (checkUri(url)){
            HttpSession session = request.getSession();
            if(StringUtils.isBlank(SessionCache.getCode(session))){
                response.sendRedirect(request.getContextPath());
                return;
            }
        }
        filterChain.doFilter(request,response);
    }

    private boolean checkUri(String uri) {
        for (String execute: DEFAULT_EXECUTE){
            if (uri.contains(execute)){
                return false;
            }
        }

        if (mainFlag){
            for (String execute: MAIN_EXECUTE){
                if (uri.contains(execute)){
                    return false;
                }
            }
        }
        return true;
    }
}
