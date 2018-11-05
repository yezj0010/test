package com.tomcat360.admin.service;

import java.util.Map;

import com.tomcat360.admin.model.JSONData;

public interface ExchangeService {

	JSONData findOperateAccountInfo(Map params);

	JSONData getQuotation(String deductCurrency,String withdrawCurrency);
	
}
