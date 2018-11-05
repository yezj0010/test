package com.tomcat360.admin.service.impl;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.tomcat360.admin.enums.EnumResponseMsg;
import com.tomcat360.admin.mapper.TbTransMapper;
import com.tomcat360.admin.model.JSONData;
import com.tomcat360.admin.model.TbSettings;
import com.tomcat360.admin.model.TbSettingsExample;
import com.tomcat360.admin.model.TbTrans;
import com.tomcat360.admin.model.TbTransExample;
import com.tomcat360.admin.model.TbUserInfo;
import com.tomcat360.admin.service.ExchangeService;
import com.tomcat360.admin.service.TbSettingsService;
import com.tomcat360.admin.service.TbTransService;
import com.tomcat360.admin.service.TbUserInfoService;
import com.tomcat360.admin.util.CalendarUtil;
import com.tomcat360.admin.util.DecimalUtil;

@Service
public class TbTransServiceImpl implements TbTransService {
	@Autowired
	private TbTransMapper tbTransMapper;
	@Autowired
	private TbUserInfoService tbUserInfoService;
	@Autowired
	private ExchangeService exchangeService;
	@Autowired
	private TbSettingsService tbSettingsService;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public JSONData findWithdrawInfo(Map<String, Object> params) {
		Integer page = Integer.valueOf(params.get("page").toString());
		Integer size = Integer.valueOf(params.get("size").toString());
		Page<Map<String, Object>> pages = PageHelper.startPage(page, size);
		tbTransMapper.findWithdrawInfo(params);
		List<Map<String, Object>> list = pages.getResult();
		if (list != null && list.size() > 0) {
			for (int i = 0; i < list.size(); i++) {
				Map<String, Object> map = list.get(i);
				String deductMoney = map.get("deductMoney") == null ? null : map.get("deductMoney").toString();
				String transFee = map.get("transFee") == null ? null : map.get("transFee").toString();
				if (!StringUtils.isEmpty(deductMoney)) {
					// 取款金额 保留5位小数
					DecimalFormat df = new DecimalFormat("#.00000");
					double d = Double.valueOf(deductMoney);
					deductMoney = df.format(d);
					int of = deductMoney.indexOf(".");
					if (of == 0) {
						deductMoney = "0" + deductMoney;
					}
					deductMoney = deductMoney + " "
							+ (map.get("deductCurrency") == null ? "" : map.get("deductCurrency").toString());
				}
				if (!StringUtils.isEmpty(transFee)) {
					// 手续费 保留8位小数

					DecimalFormat df = new DecimalFormat("#.00000000");
					double d = Double.valueOf(transFee);
					transFee = df.format(d);
					int of = transFee.indexOf(".");
					if (of == 0) {
						transFee = "0" + transFee;
					}
					transFee = transFee + " "
							+ (map.get("deductCurrency") == null ? "" : map.get("deductCurrency").toString());
				}
				// 给数字货币行情字段添加币种信息如 BTC=6500USD
				Object price = map.get("price");
				String priceCurr = null;
				if (price != null) {
					// 舍掉小数位
					DecimalFormat df = new DecimalFormat("#.00");
					double d = Double.valueOf(price.toString());
					price = df.format(d);
					int of = price.toString().indexOf(".");
					if (of == 0) {
						price = "0" + price;
					}

					priceCurr = price.toString() + " "
							+ (map.get("withdrawCurrency") == null ? "" : map.get("withdrawCurrency").toString());
				}
				// 给取款金额字段添加取款币种
				Object withdrawMoney = map.get("withdrawMoney");
				String withdrawMoneyCurr = null;
				if (withdrawMoney != null) {

					// 舍掉小数位
					DecimalFormat df = new DecimalFormat("#.00");
					double d = Double.valueOf(withdrawMoney.toString());
					withdrawMoney = df.format(d);
					int of = withdrawMoney.toString().indexOf(".");
					if (of == 0) {
						withdrawMoney = "0" + withdrawMoney;
					}

					withdrawMoneyCurr = withdrawMoney.toString() + " "
							+ (map.get("withdrawCurrency") == null ? "" : map.get("withdrawCurrency").toString());
				}
				// 修改返回的userId字段
				String userId = map.get("userId").toString();
				TbUserInfo tbUserInfo = tbUserInfoService.findByUserId(userId);
				map.put("userName", tbUserInfo.getUserName());
				map.put("withdrawMoney", withdrawMoneyCurr);
				map.put("price", priceCurr);
				map.put("transFee", transFee);
				map.put("deductMoney", deductMoney);
			}
		}

		Map result = new HashMap();
		result.put("list", pages.getResult());
		result.put("totalNumber", pages.getTotal());
		result.put("totalNum", pages.getTotal());
		result.put("page", pages.getPageNum());
		result.put("pageSize", pages.getPageSize());
		result.put("totalPages", pages.getPages());
		return JSONData.builder().code(EnumResponseMsg.RESP_SUCCESS.getCode())
				.msg(EnumResponseMsg.RESP_SUCCESS.getMsg()).data(result).build();
	}

	@Override
	public List<TbTrans> selectExample(TbTransExample tbTransExample) {
		// TODO Auto-generated method stub
		return tbTransMapper.selectByExample(tbTransExample);
	}

	@Override
	public TbTrans selectNumCurrency(String settingsValue) {
		// TODO Auto-generated method stub
		return tbTransMapper.selectNumCurrency(settingsValue);
	}

	@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	@Override
	public Map getTransSum(Map map) {
		String endTime = map.get("date").toString();
		String firstTime = null;

		// 记录数据的日期范围
		List<String> listDate = new ArrayList<String>();
		SimpleDateFormat parse = new SimpleDateFormat("yyyy-MM-dd");
		try {

			Date reduceDay = CalendarUtil.reduceDay(parse.parse(endTime), -7);
			firstTime = parse.format(reduceDay);
			endTime = parse.format(CalendarUtil.reduceDay(parse.parse(endTime), +1));
			map.put("firstTime", firstTime);
			map.put("endTime", endTime);

			// 算出每一天的数据存在数组里
			Date parse2 = parse.parse(firstTime);
			for (int j = 1; j < 8; j++) {
				Date day1 = CalendarUtil.reduceDay(parse2, j);
				String format = parse.format(day1);
				listDate.add(format);
			}

		} catch (ParseException e) {
			// TODO Auto-generatexample2ed catch block
			e.printStackTrace();
		}
		map.put("interfaceCode", "/api/auth/drawMoney");
		List<Map> drawList = tbTransMapper.getTransSum(map);
		map.put("interfaceCode", "/api/auth/depositMoney");
		List<Map> depositList = tbTransMapper.getTransSum(map);

		Map data = new HashMap();
		// 遍历取款集合
		Set<Integer> depositSet = new HashSet<Integer>();

		TbSettingsExample example = new TbSettingsExample();
		example.createCriteria().andSettingsTypeEqualTo("2");
		List<TbSettings> TbSettingsList = tbSettingsService.selectExample(example);
		ArrayList<String> cuList = new ArrayList<String>();
		for (TbSettings tbSettings : TbSettingsList) {
			cuList.add(tbSettings.getSettingsValue());
		}

		// 遍历存款集合
		for (int i = 0; i < cuList.size(); i++) {
			List<String> depositFrenchMoneyList = new ArrayList<String>();// 存款金额list
			List<String> depositNumList = new ArrayList<String>();// 存款笔数list
			List<String> depositTransFeeList = new ArrayList<String>();// 手续费list
			List<String> depositNumberMoneyList = new ArrayList<String>();// 实付的数字货币list
			// 币种名称
			String cuString = cuList.get(i);
			for (int j = 0; j < listDate.size(); j++) {
				// 日期
				String thisDate = listDate.get(j);
				// 判断该币种再这一天是否有数据
				boolean dateflag = true;
				for (int k = 0; k < depositList.size(); k++) {
					// 存款信息；
					Map depositMap = depositList.get(k);
					String transTime = depositMap.get("transTime").toString().substring(0, 10);
					String deductCurrency = (String) depositMap.get("deductCurrency");
					if (thisDate.equals(transTime) && cuString.equals(deductCurrency)) {
						String transFee = depositMap.get("transFee").toString();
						String deductMoney = depositMap.get("deductMoney").toString();
						BigDecimal withdrawMoney = (BigDecimal) depositMap.get("withdrawMoney");
						String num = depositMap.get("num").toString();

						// 手续费保留八位小数
						DecimalFormat df = new DecimalFormat("#.00000000");
						double d = Double.valueOf(transFee);
						transFee = df.format(d);

						// 取款金额保留5位小数
						DecimalFormat df1 = new DecimalFormat("#.00000");
						double d1 = Double.valueOf(deductMoney);
						deductMoney = df.format(d);

						depositFrenchMoneyList.add(j, withdrawMoney.intValue() + "");
						depositNumList.add(j, num);
						depositTransFeeList.add(j, transFee);
						depositNumberMoneyList.add(j, deductMoney);
						dateflag = false;
					}

				}
				if (dateflag) {
					depositFrenchMoneyList.add(j, "");
					depositNumList.add(j, "");
					depositTransFeeList.add(j, "");
					depositNumberMoneyList.add(j, "");
				}

			}

			if (cuString.equals("BTC")) {
				data.put("depositFrenchMoney" + cuString + "List", depositFrenchMoneyList);
			} else {
				data.put("depositMoney" + cuString + "List", depositFrenchMoneyList);
			}
			data.put("depositNumberMoney" + cuString + "List", depositNumberMoneyList);
			data.put("depositTransFee" + cuString + "List", depositTransFeeList);
			data.put("depositNum" + cuString + "List", depositNumList);
		}

		// 遍历取款集合
		Set<Integer> darwSet = new HashSet<Integer>();
		for (int i = 0; i < cuList.size(); i++) {
			List<String> drawFrenchMoneyList = new ArrayList<String>();// 取款金额list
			List<String> drawNumList = new ArrayList<String>();// 取款笔数list
			List<String> drawTransFeeList = new ArrayList<String>();// 手续费list
			List<String> drawNumberMoneyList = new ArrayList<String>();// 实收的数字货币list
			// 币种名称
			String cuString = cuList.get(i);
			for (int j = 0; j < listDate.size(); j++) {
				// 日期
				String thisDate = listDate.get(j);
				// 判断该币种再这一天是否有数据
				boolean dateflag = true;
				for (int k = 0; k < drawList.size(); k++) {
					// 存款信息；
					Map drawMap = drawList.get(k);
					String transTime = drawMap.get("transTime").toString().substring(0, 10);
					String deductCurrency = (String) drawMap.get("deductCurrency");
					if (thisDate.equals(transTime) && cuString.equals(deductCurrency)) {
						String transFee = drawMap.get("transFee").toString();
						String deductMoney = drawMap.get("deductMoney").toString();
						BigDecimal withdrawMoney = (BigDecimal) drawMap.get("withdrawMoney");
						String num = drawMap.get("num").toString();

						// 手续费保留八位小数
						DecimalFormat df = new DecimalFormat("#.00000000");
						double d = Double.valueOf(transFee);
						transFee = df.format(d);

						// 取款金额保留5位小数
						DecimalFormat df1 = new DecimalFormat("#.00000");
						double d1 = Double.valueOf(deductMoney);
						deductMoney = df.format(d);

						drawFrenchMoneyList.add(j, withdrawMoney.intValue() + "");
						drawNumList.add(j, num);
						drawTransFeeList.add(j, transFee);
						drawNumberMoneyList.add(j, deductMoney);
						dateflag = false;
					}

				}
				if (dateflag) {
					drawFrenchMoneyList.add(j, "");
					drawNumList.add(j, "");
					drawTransFeeList.add(j, "");
					drawNumberMoneyList.add(j, "");
				}
			}

			if (cuString.equals("BTC")) {
				data.put("drawFrenchMoney" + cuString + "List", drawFrenchMoneyList);
			} else {
				data.put("drawMoney" + cuString + "List", drawFrenchMoneyList);
			}
			data.put("drawNumberMoney" + cuString + "List", drawNumberMoneyList);
			data.put("drawTransFee" + cuString + "List", drawTransFeeList);
			data.put("drawNum" + cuString + "List", drawNumList);
		}

		data.put("listDate", listDate);

		return data;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Map getProfitSum(Map map) {
		List<Map> list = tbTransMapper.getProfitSum(map);
		BigDecimal sumBigDecimal = new BigDecimal("0");

		// 查询出所有币种
		TbSettingsExample example = new TbSettingsExample();
		example.createCriteria().andSettingsTypeEqualTo("2");
		List<TbSettings> settingsList = tbSettingsService.selectExample(example);
		// 所有币种名称的list
		List<String> deductCurrencyList = new ArrayList<String>();
		// 已有数据的list
		List<String> exitList = new ArrayList<String>();

		for (int i = 0; i < settingsList.size(); i++) {
			String settingsValue = settingsList.get(i).getSettingsValue();
			deductCurrencyList.add(settingsValue);
		}
		String deductCurrency = "";
		String withdrawCurrency = "";
		for (Map map2 : list) {
			deductCurrency = (String) map2.get("deductCurrency");
			exitList.add(deductCurrency);

			withdrawCurrency = (String) map2.get("withdrawCurrency");
			if (!StringUtils.isEmpty(deductCurrency)) {
				JSONData quotation = exchangeService.getQuotation(deductCurrency, withdrawCurrency);
				Map data = quotation.getData();
				// 暂时取挡板数据
				List<Map> accountList = (List<Map>) data.get("merchantCapital");
				Map map3 = accountList.get(0);
				String currencyQuotation = map3.get("quotationCurrency") == null ? null
						: map3.get("quotationCurrency").toString();
				map2.put("quotation", currencyQuotation);
				// ((totalInCurrency-totalOutCurrency+totalFeeCurrency)*quotationCurrency)
				// - (totalDrawAmt-totalDepositAmt)
				String totalInCurrency = map2.get("totalInCurrency").toString();
				BigDecimal totalInCurrencyNum = new BigDecimal(totalInCurrency);// 收到的数字货币

				String totalOutCurrency = map2.get("totalOutCurrency").toString();
				BigDecimal totalOutCurrencyNum = new BigDecimal(totalOutCurrency);// 给出的数字货币

				String totalFeeCurrency = map2.get("totalFeeCurrency").toString();
				BigDecimal totalFeeCurrencyNum = new BigDecimal(totalFeeCurrency);// 收到的手续费总计

				BigDecimal currencyQuotationNum = new BigDecimal(currencyQuotation);// 行情

				String totalDrawAmt = map2.get("totalDrawAmt").toString();
				BigDecimal totalDrawAmtNum = new BigDecimal(totalDrawAmt);// 给出的法币总计

				String totalDepositAmt = map2.get("totalDepositAmt").toString();

				BigDecimal totalDepositAmtNum = new BigDecimal(totalDepositAmt);// 收到的法币总计

				// 算出收到的数字货币在当下行情的法币总额
				BigDecimal add = totalInCurrencyNum.subtract(totalOutCurrencyNum).add(totalFeeCurrencyNum)
						.multiply(currencyQuotationNum);

				// 算出所有给出的法币总额
				BigDecimal subtract = totalDrawAmtNum.subtract(totalDepositAmtNum);
				// 用收到的法币总额减去给出的法币总额算出盈利
				BigDecimal profit = add.subtract(subtract);

				map2.put("profit", profit);
				sumBigDecimal = sumBigDecimal.add(profit);
			}
		}

		for (Map map2 : list) {
			String totalInCurrency = map2.get("totalInCurrency") == null ? "" : map2.get("totalInCurrency").toString();
			String totalOutCurrency = map2.get("totalOutCurrency") == null ? ""
					: map2.get("totalOutCurrency").toString();
			String totalFeeCurrency = map2.get("totalFeeCurrency") == null ? ""
					: map2.get("totalFeeCurrency").toString();
			String profit = map2.get("profit") == null ? "" : map2.get("profit").toString();
			totalInCurrency = DecimalUtil.getRounding(totalInCurrency, 5);
			totalOutCurrency = DecimalUtil.getRounding(totalOutCurrency, 5);
			totalFeeCurrency = DecimalUtil.getRounding(totalFeeCurrency, 8);
			profit = DecimalUtil.getRounding(profit, 3);

			map2.put("totalInCurrency", totalInCurrency);
			map2.put("totalOutCurrency", totalOutCurrency);
			map2.put("totalFeeCurrency", totalFeeCurrency);
			map2.put("profit", profit);

		}

		deductCurrencyList.removeAll(exitList);
		if (!StringUtils.isEmpty(withdrawCurrency)) {
			for (String string : deductCurrencyList) {
				Map cuMap = new HashMap();
				JSONData quotation = exchangeService.getQuotation(string, withdrawCurrency);
				Map data = quotation.getData();
				// 暂时取挡板数据
				List<Map> accountList = (List<Map>) data.get("merchantCapital");
				Map map3 = accountList.get(0);
				String currencyQuotation = map3.get("quotationCurrency") == null ? null
						: map3.get("quotationCurrency").toString();
				cuMap.put("quotation", currencyQuotation);

				cuMap.put("deductCurrency", string);
				cuMap.put("withdrawCurrency", withdrawCurrency);
				cuMap.put("totalDrawAmt", 0);
				cuMap.put("totalDrawNum", 0);
				cuMap.put("totalDepositAmt", 0);
				cuMap.put("totalDepositNum", 0);
				cuMap.put("totalInCurrency", 0);
				cuMap.put("totalOutCurrency", 0);
				cuMap.put("totalFeeCurrency", 0);
				cuMap.put("profit", 0);
				list.add(cuMap);
			}
		}
		
		Map data = new HashMap();
		String sumStr = DecimalUtil.getRounding(sumBigDecimal.toString(), 3);
		data.put("sumProfit", sumStr);

		data.put("list", list);

		return data;
	}

}