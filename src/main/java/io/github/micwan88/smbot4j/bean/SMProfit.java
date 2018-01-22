package io.github.micwan88.smbot4j.bean;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SMProfit {
	private Date profitDate = null;
	private BigDecimal profitValue = new BigDecimal(0);
	
	/**
	 * @param profitDate
	 * @param profitValue
	 */
	public SMProfit(Date profitDate, BigDecimal profitValue) {
		this.profitDate = profitDate;
		this.profitValue = profitValue;
	}

	public SMProfit(String profitDateString, BigDecimal profitValue) throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		this.profitDate = dateFormat.parse(profitDateString);
		this.profitValue = profitValue;
	}
	
	/**
	 * @return the profitDate
	 */
	public Date getProfitDate() {
		return profitDate;
	}

	/**
	 * @param profitDate the profitDate to set
	 */
	public void setProfitDate(Date profitDate) {
		this.profitDate = profitDate;
	}

	/**
	 * @return the profitValue
	 */
	public BigDecimal getProfitValue() {
		return profitValue;
	}

	/**
	 * @param profitValue the profitValue to set
	 */
	public void setProfitValue(BigDecimal profitValue) {
		this.profitValue = profitValue;
	}
}
