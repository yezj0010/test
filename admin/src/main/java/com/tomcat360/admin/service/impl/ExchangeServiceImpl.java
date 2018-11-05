package com.tomcat360.admin.service.impl;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.google.common.collect.Maps;
import com.tomcat360.admin.enums.EnumResponseMsg;
import com.tomcat360.admin.model.JSONData;
import com.tomcat360.admin.model.TbMerchantAccount;
import com.tomcat360.admin.properties.AppProperties;
import com.tomcat360.admin.service.ExchangeService;
import com.tomcat360.admin.service.TbMerchantAccountService;
import com.tomcat360.admin.util.CalendarUtil;
import com.tomcat360.admin.util.LogUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ExchangeServiceImpl implements ExchangeService {
	@Autowired
	private AppProperties appProperties;
	@Autowired
	private TbMerchantAccountService tbMerchantAccountService;

	@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	@Override
	public JSONData findOperateAccountInfo(Map params) {

		Integer page = (Integer) params.get("page");
		Integer size = (Integer) params.get("size");
		String startTime = (String) params.get("startTime");
		String endTime = (String) params.get("endTime");
		Page<Map<String, Object>> list2 = PageHelper.startPage(page, size);
		List<TbMerchantAccount> list = tbMerchantAccountService.findOperateAccountInfo(params);

		// 组装参数
		Map<String, String> startCheckParams = new HashMap<String, String>();
		startCheckParams.put("merchantId", appProperties.getMerchantNo());// 商户号
		if (!StringUtils.isEmpty(startTime)) {
			startCheckParams.put("startTime", CalendarUtil.toDate(startTime, CalendarUtil.DATE_FMT_5).getTime() + "");// 开始时间
		}
		if (!StringUtils.isEmpty(endTime)) {
			startCheckParams.put("endTime", CalendarUtil.toDate(endTime, CalendarUtil.DATE_FMT_5).getTime() + "");// 结束时间
		}
		JSONData accountInfo = noAuthDoPost(
				getUrl("/api/merchantUserCapital/" + appProperties.getMerchantNo(), startCheckParams));
		System.out.println(accountInfo);
		if (accountInfo == null) {
			log.error("交易所返回超时");
			return JSONData.builder().code(EnumResponseMsg.RESP_ERROR_EXCHANFE_ERROR.getCode())
					.msg(EnumResponseMsg.RESP_ERROR_EXCHANFE_ERROR.getMsg()).build();
		} else if (!EnumResponseMsg.isSuccess(accountInfo.getCode())) {
			log.error("返回响应码：code=" + accountInfo.getCode() + ",返回响应信息" + accountInfo.getMsg());
			return JSONData.builder().code(accountInfo.getCode()).msg(accountInfo.getMsg()).build();
		}
		Map infoResult = accountInfo.getData();
		List<Map> accountList = (List<Map>) infoResult.get("merchantCapital");
		System.out.println(accountList);

		for (TbMerchantAccount map : list) {
			for (Map exAccount : accountList) {
				String feeRevenue = (String) exAccount.get("feeRevenue");// 手续费累积收入
				String cashRevenue = (String) exAccount.get("cashRevenue");
				;// 取款累积收入（不含手续费）
				String coinBalance = (String) exAccount.get("coinBalance");// 账户余额
				String coinName = ((String) exAccount.get("coinName")).toUpperCase();// 币种名称
				String accountAddress = (String) exAccount.get("coinAddress");// 账户地址
				// 如果不是同一个币种，继续找
				if (!coinName.equals((map.getCurrency()).toUpperCase())) {
					continue;
				}
				map.setUpdateTime(new Date());
				map.setAccountAdress(accountAddress);
				map.setAccountBalance(new BigDecimal(coinBalance));
				map.setTotalWithdrawal(new BigDecimal(cashRevenue));// 取款累积收入
				map.setTotalWithdrawalFee(new BigDecimal(feeRevenue));// 取款手续费
				tbMerchantAccountService.update(map);
			}
		}
		Map data = new HashMap();

		data.put("list", list);
		data.put("pageSize", list2.getPageSize());
		data.put("totalPages", list2.getPages());
		data.put("totalNumber", list2.getTotal());
		data.put("page", list2.getPageNum());

		return JSONData.builder().code(EnumResponseMsg.RESP_SUCCESS.getCode())
				.msg(EnumResponseMsg.RESP_SUCCESS.getMsg()).data(data).build();
	}

	/**
	 * 无需登录，调用http请求交易所
	 *
	 * @param url
	 * @return JSONData
	 */
	@SuppressWarnings({ "unchecked", "rawtypes", })
	public JSONData noAuthDoPost(String url) {
		CloseableHttpClient httpClient = HttpClients.createDefault();
		RequestConfig config = RequestConfig.custom().setConnectionRequestTimeout(5000).// connection超时时间
				setSocketTimeout(20000).// 获取数据超时时间,等待返回时间
				setConnectTimeout(10000).build();// 连接超时时间
		HttpPost post = new HttpPost(url);
		post.setConfig(config);
		// 使用addHeader方法添加请求头部,诸如User-Agent, Accept-Encoding等参数.
		post.setHeader("Content-Type", "application/json;charset=UTF-8");
		String string = "";
		try {
			log.info("调用交易所接口" + url);
			log.info("入参：无");
			CloseableHttpResponse response = httpClient.execute(post);
			string = EntityUtils.toString(response.getEntity());
			log.info("出参：" + string);
			// 将string转成map
			Map result = JSONObject.parseObject(string);
			if (result == null) {
				return JSONData.builder().code(EnumResponseMsg.RESP_ERROR_SYSTEM_FAILURE.getCode())
						.msg(EnumResponseMsg.RESP_ERROR_SYSTEM_FAILURE.getMsg()).build();
			}
			if (("0000").equals(result.get("code") + "")) {
				return JSONData.builder().code(EnumResponseMsg.RESP_SUCCESS.getCode())
						.msg(EnumResponseMsg.RESP_SUCCESS.getMsg()).data((Map) result.get("data")).build();
			}

			if ("E001".equals(result.get("code") + "")) {// 交易所请求子服务超时
				return null;
			}

			if (!StringUtils.isEmpty(result.get("code"))) {
				String msg = (String) result.get("msg");
				if (!StringUtils.isEmpty(msg) && msg.length() > 150) {
					msg = msg.substring(0, 150);
				}
				return JSONData.builder().code(result.get("code") + "").msg(msg).build();
			} else if (!StringUtils.isEmpty(result.get("status"))) {
				String msg = (String) result.get("error");
				if (!StringUtils.isEmpty(msg) && msg.length() > 150) {
					msg = msg.substring(0, 150);
				}
				return JSONData.builder().code(result.get("status") + "").msg(msg).build();
			} else {
				return JSONData.builder().code(EnumResponseMsg.RESP_ERROR_SYSTEM_FAILURE.getCode())
						.msg(EnumResponseMsg.RESP_ERROR_SYSTEM_FAILURE.getMsg()).build();
			}

		} catch (Exception e) {
			log.info(LogUtils.getExceptionInfo(e));
			return null;
		}
	}

	/**
	 *
	 * @param interfaceCode
	 *            接口名称
	 * @return
	 */
	public String getUrl(String interfaceCode, Map<String, String> params) {
		// 公钥
		String accessKey = appProperties.getAccessKey();
		// 私钥
		String secretKey = appProperties.getSecretKey();
		String requestUrl = interfaceCode;
		String localhost = appProperties.getNoauthBaseUrl();

		Map<String, String> common = Maps.newHashMap();
		common.put("accessKey", accessKey);
		common.put("_dt", String.valueOf(System.currentTimeMillis()));
		common.putAll(params);
		String sign = genSign(requestUrl, common, secretKey, accessKey);
		System.out.println(sign);
		StringBuilder url = new StringBuilder(localhost + requestUrl + "?sign=" + sign);
		if (!StringUtils.isEmpty(common)) {
			common.forEach((key, value) -> {
				url.append("&" + key + "=" + value);
			});
		}
		return url.toString();
	}

	public String genSign(String url, Map<String, String> params, String secretKey, String accessKey) {

		if (StringUtils.isEmpty(url) || CollectionUtils.isEmpty(params)) {
			// wrong
			return "";
		}

		if (!params.keySet().contains("_dt") || !params.keySet().contains("accessKey")) {
			// wrong
			return "";
		}

		Object[] bodyKs = params.keySet().toArray();
		Arrays.sort(bodyKs); // key 排序

		StringBuilder wait = new StringBuilder(url);
		wait.append("|");
		for (int i = 0; i < bodyKs.length; i++) {
			String key = (String) bodyKs[i];
			String value = params.get(key);
			if (StringUtils.isEmpty(value)) {
				// wrong
				return "";
			}
			if ("sign".equals(key)) {
				continue;
			}
			wait.append(key).append("=").append(value);
		}
		wait.append("|");
		wait.append(secretKey);

		// 验签
		String sign = "";
		try {
			sign = getMD5Code(wait.toString());
		} catch (Exception e) {
		}

		return sign;
	}

	public static String getMD5Code(String strObj) throws Exception {
		MessageDigest md = MessageDigest.getInstance("MD5");
		// md.digest() 该函数返回值为存放哈希值结果的byte数组
		return byteToString(md.digest(strObj.getBytes()));
	}

	private static String byteToString(byte[] bByte) {
		StringBuffer sBuffer = new StringBuffer();
		for (int i = 0; i < bByte.length; i++) {
			sBuffer.append(byteToArrayString(bByte[i]));
		}
		return sBuffer.toString();
	}

	private static String byteToArrayString(byte bByte) {
		int iRet = bByte;
		if (iRet < 0) {
			iRet += 256;
		}
		int iD1 = iRet / 16;
		int iD2 = iRet % 16;
		return strDigits[iD1] + strDigits[iD2];
	}

	private static final String[] strDigits = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d",
			"e", "f" };

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public JSONData getQuotation(String deductCurrency, String withdrawCurrency) {
		Map<String, String> deductCurrencyParams = new HashMap<String, String>();
		deductCurrencyParams.put("deductCurrency", deductCurrency);
		deductCurrencyParams.put("withdrawCurrency", withdrawCurrency);
		// 暂时使用档板
		//JSONData accountInfo = noAuthDoPost(getUrl("/api/XXXX/" + appProperties.getMerchantNo(), deductCurrencyParams));
		JSONData accountInfo = JSONData.builder().code(EnumResponseMsg.RESP_SUCCESS.getCode())
				.msg(EnumResponseMsg.RESP_SUCCESS.getMsg()).build();
		System.out.println(accountInfo);
		if (accountInfo == null) {
			log.error("交易所返回超时");
			return JSONData.builder().code(EnumResponseMsg.RESP_ERROR_EXCHANFE_ERROR.getCode())
					.msg(EnumResponseMsg.RESP_ERROR_EXCHANFE_ERROR.getMsg()).build();
		} else if (!EnumResponseMsg.isSuccess(accountInfo.getCode())) {
			log.error("返回响应码：code=" + accountInfo.getCode() + ",返回响应信息" + accountInfo.getMsg());
			return JSONData.builder().code(accountInfo.getCode()).msg(accountInfo.getMsg()).build();
		}
		//Map infoResult = accountInfo.getData();
		//List<Map> accountList = (List<Map>) infoResult.get("merchantCapital");
		
		Map infoResult = new HashMap();
		List<Map> accountList = new ArrayList<Map>();
		System.out.println(accountList);
		
		Integer price = 0;
		
		Map map = new HashMap();
		if(deductCurrency.equals("BTC")){
			price = 6300;
		}else if(deductCurrency.equals("ETH")){
			price = 208;
		}else if(deductCurrency.equals("LTC")){
			price = 60;
		}else if(deductCurrency.equals("BTT")){
			price = 6000;
		}else if(deductCurrency.equals("EOS")){
			price = 7;
		}
		map.put("quotationCurrency", price);
		accountList.add(map);
		infoResult.put("merchantCapital", accountList);
		accountInfo.setData(infoResult);
		return accountInfo;
	}

}
