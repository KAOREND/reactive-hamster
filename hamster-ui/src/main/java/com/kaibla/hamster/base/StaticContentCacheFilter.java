/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * .
 */
package com.kaibla.hamster.base;

import java.io.IOException;
import java.util.Calendar;
import static java.util.Calendar.getInstance;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 *
 * @author kai
 */
public class StaticContentCacheFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        
        Calendar inOneYear = getInstance();
        inOneYear.add(Calendar.YEAR, 1);
        httpServletResponse.setDateHeader("Expires", inOneYear.getTimeInMillis());     
        httpServletResponse.setHeader( "Cache-Control", "public, max-age=9999999" );
        
        /*
         * By default, some servers (e.g. Tomcat) will set headers on any SSL content to deny caching. Omitting the
         * Pragma header takes care of user-agents implementing HTTP/1.0.
         */
        chain.doFilter(httpServletRequest, new HttpServletResponseWrapper(httpServletResponse) {
            @Override
            public void addHeader(String name, String value) {
                if (!"PRAGMA".equalsIgnoreCase(name)) {
                    super.addHeader(name, value);
                }
            }

            @Override
            public void setHeader(String name, String value) {
                if (!"PRAGMA".equalsIgnoreCase(name)) {
                    super.setHeader(name, value);
                }
            }
        });
    }

    @Override
    public void destroy() {

    }

}
