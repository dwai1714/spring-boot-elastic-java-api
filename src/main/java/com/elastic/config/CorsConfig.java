package com.elastic.config;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Configuration;

@Configuration
public class CorsConfig implements Filter {

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * This filter enables backend CORs support so our client app can "talk" to it.
	 * 
	 * @param servletRequest - the current servlet request
	 * @param servletResponse - the current servlet response
	 * @param filterChain - the current servlet filter
	 */

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		HttpServletResponse currentResponse = (HttpServletResponse) servletResponse;

		  currentResponse.setHeader("Access-Control-Allow-Origin", "*");
		  currentResponse.setHeader("Access-Control-Allow-Credentials", "true");
		  currentResponse.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
		  currentResponse.setHeader("Access-Control-Max-Age", "3600");
		  currentResponse.setHeader("Access-Control-Allow-Headers",
		      "Content-Type, Accept, X-Requested-With, X-AUTH-TOKEN");

		  filterChain.doFilter(servletRequest, servletResponse);

	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		// TODO Auto-generated method stub
		
	}

}
