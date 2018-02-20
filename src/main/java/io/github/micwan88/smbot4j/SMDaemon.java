package io.github.micwan88.smbot4j;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import io.github.micwan88.smbot4j.bean.SMBalance;
import io.github.micwan88.smbot4j.bean.SMProfit;
import io.github.micwan88.smbot4j.bean.SMProfitMessage;
import io.github.micwan88.util.AppPropertiesUtil;

public class SMDaemon implements AutoCloseable {
	public static final String PID_FILE_NAME = "SMDaemon.pid";
	public static final long DEFAULT_SLEEP_TIME = 60000L;
	
	public static final int DEFAULT_CONNECTION_POOL_REQUEST_TIMEOUT_VALUE = 3000;
	public static final int DEFAULT_CONNECTION_TIMEOUT_VALUE = 3000;
	public static final int DEFAULT_SOCKET_TIMEOUT_VALUE = 3000;
	
	/****************************************************************************************************/
	
	public static final String URL_TELEGRAM_BOT_BASE = "https://api.telegram.org/bot";
	
	public static final String SUN_MINING_DOMAIN_NAME = "sun-mining.com";
	public static final String SUN_MINING_COOKIE_SESSION_NAME = "laravel_session";
	public static final String SUN_MINING_COOKIE_SESSION_PATH = "/";
	public static final String URL_SUN_MINING_BASE = "https://" + SUN_MINING_DOMAIN_NAME + "/en";
	public static final String URL_SUN_MINING_LOGIN_PAGE = URL_SUN_MINING_BASE + "/login";
	public static final String URL_SUN_MINING_DASHBOARD_PAGE = URL_SUN_MINING_BASE + "/dashboard";
	public static final String URL_SUN_MINING_BALANCE_PAGE = URL_SUN_MINING_BASE + "/balance";
	//public static final String[] SUN_MINING_COINS_ARRAY = {"Btc", "Eth", "Dash", "Ltc"};
	public static final String[] SUN_MINING_COINS_ARRAY = {"Btc", "Eth", "Ltc"};
	
	public static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
	public static final String CONTENT_TYPE_JSON = "application/json";
	
	public static final String HTTPCLIENT_ROBOT_USERAGENT_FIREFOX = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:28.0) Gecko/20100101 Firefox/28.0";
	
	private static final Logger myLogger = LogManager.getLogger(SMDaemon.class);
	
	/****************************************************************************************************/
	
	private final Pattern PATTERN_TELEGRAM_BOT_RESP_MSG_OK = Pattern.compile("\"ok\"\\s*\\:\\s*true");
	private final Matcher MATCHER_TELEGRAM_BOT_RESP_MSG_OK = PATTERN_TELEGRAM_BOT_RESP_MSG_OK.matcher("");
	
	private final Pattern PATTERN_SM_RESP_DASHBOARD_LATEST_DATE = Pattern.compile("categories:\\s*\\[\\s*(?:\"\\d{4}-\\d{2}-\\d{2}\",\\s*)+\"(\\d{4}-\\d{2}-\\d{2})\",\\s*\\]", Pattern.MULTILINE);
	private final Matcher MATCHER_SM_RESP_DASHBOARD_LATEST_DATE = PATTERN_SM_RESP_DASHBOARD_LATEST_DATE.matcher("");
	private final Pattern PATTERN_SM_RESP_DASHBOARD_LATEST_PROFIT = Pattern.compile("data:\\s*\\[\\s*(?:\\d+\\.*\\d*,\\s*)+(\\d+\\.?\\d*),\\s*\\]", Pattern.MULTILINE);
	private final Matcher MATCHER_SM_RESP_DASHBOARD_LATEST_PROFIT = PATTERN_SM_RESP_DASHBOARD_LATEST_PROFIT.matcher("");
	//private final Pattern PATTERN_SM_RESP_COIN_BALANCE = Pattern.compile("<tr>\\s*<td>(?:Bitcoin|Ether|Dash|Litecoin)</td>\\s*<td>(BTC|ETH|DASH|LTC)</td>\\s*<td>(\\d+(?:\\.\\d+)?)</td>\\s*<td>\\s*(\\d+(?:\\.\\d+)?)\\s*</td>", Pattern.MULTILINE);
	private final Pattern PATTERN_SM_RESP_COIN_BALANCE = Pattern.compile("<tr>\\s*<td>(?:Bitcoin|Ether|Litecoin)</td>\\s*<td>(BTC|ETH|LTC)</td>\\s*<td>(\\d+(?:\\.\\d+)?)</td>\\s*<td>\\s*(\\d+(?:\\.\\d+)?)\\s*</td>", Pattern.MULTILINE);
	private final Matcher MATCHER_SM_RESP_COIN_BALANCE = PATTERN_SM_RESP_COIN_BALANCE.matcher("");
	
	private CloseableHttpClient httpClient = null;
	private HttpClientContext httpContext = null;
	private BasicCookieStore cookieStore = new BasicCookieStore();
	
	private String smLoginSession = null;
	private String tBotToken = null;
	private String tBotChatID = null;
	private long sleepTime = DEFAULT_SLEEP_TIME;
	private long errSleepTime = DEFAULT_SLEEP_TIME;
	
	public SMDaemon(Properties appProperties) {
		init(appProperties);
	}
	
	private void init(Properties appProperties) {
		String tempStr = null;
		
		tBotToken = appProperties.getProperty("tbot.token");
		tBotChatID = appProperties.getProperty("tbot.chatID");
		
		myLogger.debug("tbot.token: {}" , tBotToken);
		myLogger.debug("tbot.chatID: {}" , tBotChatID);
		
		if (tBotToken == null || tBotToken.trim().length() == 0)
			throw new IllegalArgumentException("tbot.token cannot be empty");
		
		if (tBotChatID == null || tBotChatID.trim().length() == 0)
			throw new IllegalArgumentException("tbot.chatID cannot be empty");
		
		smLoginSession = appProperties.getProperty("smdaemon.loginSession");
		myLogger.debug("smdaemon.loginSession: {}" , smLoginSession);
		
		if (smLoginSession == null || smLoginSession.trim().length() == 0)
			throw new IllegalArgumentException("smdaemon.loginSession cannot be empty");
		
		tempStr = appProperties.getProperty("smdaemon.defaultsleeptime");
		if (tempStr != null && tempStr.trim().length() != 0) {
			try {
				sleepTime = Long.parseLong(tempStr);
			} catch (Exception e) {
				myLogger.error("Cannot parse value from smdaemon.defaultsleeptime: {}", tempStr);
			}
		}
		myLogger.debug("smdaemon.defaultsleeptime: {}" , tempStr);
		
		tempStr = appProperties.getProperty("smdaemon.defaulterrorsleeptime");
		if (tempStr != null && tempStr.trim().length() != 0) {
			try {
				errSleepTime = Long.parseLong(tempStr);
			} catch (Exception e) {
				myLogger.error("Cannot parse value from smdaemon.defaulterrorsleeptime: {}", tempStr);
			}
		}
		myLogger.debug("smdaemon.defaulterrorsleeptime: {}" , tempStr);
		
		RequestConfig httpConfig = RequestConfig.custom()
				.setContentCompressionEnabled(true)
				.setRedirectsEnabled(true)
				.setRelativeRedirectsAllowed(true)
				.setCircularRedirectsAllowed(true)
				.setConnectionRequestTimeout(DEFAULT_CONNECTION_POOL_REQUEST_TIMEOUT_VALUE)
				.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT_VALUE)
				.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT_VALUE)
				.build();
		
		httpClient = HttpClients.custom()
				.setDefaultRequestConfig(httpConfig)
				.setRetryHandler(new DefaultHttpRequestRetryHandler())
				.setDefaultCookieStore(cookieStore)
				.setUserAgent(HTTPCLIENT_ROBOT_USERAGENT_FIREFOX)
				.build();
		
		httpContext = HttpClientContext.create();
		
		initCookieStore();
	}
	
	private void initCookieStore() {
		BasicClientCookie sessionCookie = new BasicClientCookie(SUN_MINING_COOKIE_SESSION_NAME, smLoginSession);
		sessionCookie.setDomain(SUN_MINING_DOMAIN_NAME);
		sessionCookie.setPath(SUN_MINING_COOKIE_SESSION_PATH);
		
		cookieStore.addCookie(sessionCookie);
	}
	
	public static void main(String[] args) {
		Path pidFile = Paths.get(PID_FILE_NAME);
		
		myLogger.debug("SMDaemon start ...");
		try {
			if (!Files.isRegularFile(pidFile))
				Files.createFile(pidFile);
		} catch (IOException e) {
			myLogger.error("Cannot create PID file: {}", pidFile.toAbsolutePath());
			myLogger.error("Stack", e);
			System.exit(-1);
		}
		pidFile.toFile().deleteOnExit();
		
		myLogger.debug("Loading appProperties ...");
		AppPropertiesUtil appPropertyUtil = new AppPropertiesUtil();
		Properties appProperties = appPropertyUtil.getAppProperty();
		if (appProperties == null) {
			myLogger.error("Cannot load appProperties: {}", AppPropertiesUtil.APP_PROPERTY_FILE);
			System.exit(-2);
		}
		
		boolean gotError = false;
		String notifyMsg = null;
		String respMsg = null;
		SMProfit[] lastSMProfit = new SMProfit[SUN_MINING_COINS_ARRAY.length]; 
		try (SMDaemon smDaemon = new SMDaemon(appProperties)) {
			
			myLogger.debug("Start looping ...");
			
			while (Files.isRegularFile(pidFile)) {
				gotError = false;
				
				SMProfitMessage smProfitMsg = smDaemon.getSMProfit();
				if (smProfitMsg.getErrCode() == -1) {
					respMsg = smDaemon.postNotification("Cannot login to SM");
					if (respMsg == null || !respMsg.equals(""))
						myLogger.error("Cannot post notification: {}" , respMsg);
					
					myLogger.error("Lost session, break process ...");
					break;
				} else if (smProfitMsg.getErrCode() == 0) {
					notifyMsg = smDaemon.composeProfitNotification(smProfitMsg.getSmProfitList(), lastSMProfit);
					if (notifyMsg.length() > 0) {
						myLogger.debug("SM profit change, send notification: {}", notifyMsg);
						respMsg = smDaemon.postNotification(notifyMsg);
						if (respMsg == null || !respMsg.equals(""))
							myLogger.error("Cannot post notification: {}" , respMsg);
						
						ArrayList<SMBalance> smCoinBalanceList = smDaemon.getSMBalance();
						if (smCoinBalanceList == null) {
							gotError = true;
							smDaemon.postNotification("Cannot get balance");
							myLogger.error("Cannot get balance");
							continue;
						}
						
						respMsg = smDaemon.postNotification(smDaemon.composeBalanceNotification(smCoinBalanceList));
						if (respMsg == null || !respMsg.equals(""))
							myLogger.error("Cannot post notification: {}" , respMsg);
					}
				} else
					gotError = true;
				
				if (gotError)
					smDaemon.resetHttpClient();
				
				smDaemon.sleep(gotError);
			}
			
			myLogger.debug("Process break or PID file not exist, so quit ... : {}", pidFile.toAbsolutePath());
		} finally {
			myLogger.debug("SMDaemon end");
		}
	}
	
	@Override
	public void close() {
		if (httpClient != null)
			HttpClientUtils.closeQuietly(httpClient);
	}
	
	public void resetHttpClient() {
		myLogger.debug("Reset Http client");
		cookieStore.clear();
		httpContext = HttpClientContext.create();
		
		initCookieStore();
	}
	
	public void sleep() {
		sleep(false);
	}
	
	public void sleep(boolean isErrorSleep) {
		try {
			Thread.sleep(isErrorSleep?errSleepTime:sleepTime);
		} catch (InterruptedException e) {
			//Do Nothing
		}
	}
	
	public String postNotification(String notificationMsg) {
		String apiURL = URL_TELEGRAM_BOT_BASE + tBotToken + "/sendMessage";
		
		myLogger.debug("postNotification URL: {}", apiURL);
		myLogger.debug("notificationMsg: {}", notificationMsg);
		
		HttpPost httpPost = new HttpPost(apiURL);
		
		JsonObject outputJson = new JsonObject();
		outputJson.addProperty("chat_id", tBotChatID);
		outputJson.addProperty("text", notificationMsg);
		outputJson.addProperty("parse_mode", "HTML");
		
		httpPost.setEntity(new StringEntity(outputJson.toString(), "UTF-8"));
		httpPost.setHeader(HTTP_HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
		
		CloseableHttpResponse httpResponse = null;
		try {
			httpResponse = httpClient.execute(httpPost, httpContext);
			HttpEntity httpEntity = httpResponse.getEntity();
			if (httpEntity == null) {
				myLogger.error("No content on the request: {}", apiURL);
				return null;
			}
			
			String responseMsg = EntityUtils.toString(httpEntity, "UTF-8");
			MATCHER_TELEGRAM_BOT_RESP_MSG_OK.reset(responseMsg);
			if (MATCHER_TELEGRAM_BOT_RESP_MSG_OK.find())
				return "";
			
			myLogger.error("Unknown response msg: {}", responseMsg);
		    return responseMsg;
		} catch (IOException e) {
			myLogger.error("Cannot execute http request", e);
		} catch (Exception e) {
			myLogger.error("Unknown error in executing http request", e);
		} finally {
			HttpClientUtils.closeQuietly(httpResponse);
			myLogger.debug("postNotification end");
		}
		return null;
	}
	
	public SMProfitMessage getSMProfit() {
		String apiURL = URL_SUN_MINING_DASHBOARD_PAGE;
		myLogger.debug("getSMProfit URL: {}", apiURL);
		
		HttpGet httpget = new HttpGet(apiURL);
		
		CloseableHttpResponse httpResponse = null;
		try {
			httpResponse = httpClient.execute(httpget, httpContext);
			
			List<URI> redirectList = httpContext.getRedirectLocations();
			if (redirectList != null)
				if (redirectList.get(redirectList.size()-1).toString().startsWith(URL_SUN_MINING_LOGIN_PAGE)) {
					myLogger.error("Cannot login to SM");
					return new SMProfitMessage(-1);
				};
			
			HttpEntity httpEntity = httpResponse.getEntity();
			if (httpEntity == null) {
				myLogger.error("No content on the request: {}", apiURL);
				return new SMProfitMessage(-2);
			}
			
			String responseMsg = EntityUtils.toString(httpEntity, "UTF-8");
			MATCHER_SM_RESP_DASHBOARD_LATEST_DATE.reset(responseMsg);
			MATCHER_SM_RESP_DASHBOARD_LATEST_PROFIT.reset(responseMsg);
			
			ArrayList<SMProfit> profitList = new ArrayList<>();
			while (MATCHER_SM_RESP_DASHBOARD_LATEST_DATE.find()) {
				if (MATCHER_SM_RESP_DASHBOARD_LATEST_PROFIT.find()) {
					myLogger.debug("Date: {} Profit: {}", MATCHER_SM_RESP_DASHBOARD_LATEST_DATE.group(1), MATCHER_SM_RESP_DASHBOARD_LATEST_PROFIT.group(1));
					SMProfit smProfit = new SMProfit(MATCHER_SM_RESP_DASHBOARD_LATEST_DATE.group(1), 
							new BigDecimal(MATCHER_SM_RESP_DASHBOARD_LATEST_PROFIT.group(1)));
					profitList.add(smProfit);
				} else {
					myLogger.error("Unknown response msg: {}", responseMsg);
					return new SMProfitMessage(-3);
				}
			}
			
			if (profitList.isEmpty()) {
				myLogger.error("Unknown response msg: {}", responseMsg);
				return new SMProfitMessage(-3);
			}
			
			if (profitList.size() < SUN_MINING_COINS_ARRAY.length) {
				myLogger.error("Cannot parse all coins profit from SM page: {}", responseMsg);
				return new SMProfitMessage(-4);
			}
			
			return new SMProfitMessage(profitList);
		} catch (ParseException e) {
			myLogger.error("Cannot parse profit value", e);
		} catch (IOException e) {
			myLogger.error("Cannot execute http request", e);
		} catch (Exception e) {
			myLogger.error("Unknown error in executing http request", e);
		} finally {
			HttpClientUtils.closeQuietly(httpResponse);
			myLogger.debug("getSMProfit end");
		}
		return new SMProfitMessage(-5);
	}
	
	public ArrayList<SMBalance> getSMBalance() {
		String apiURL = URL_SUN_MINING_BALANCE_PAGE;
		myLogger.debug("getSMBalance URL: {}", apiURL);
		
		HttpGet httpget = new HttpGet(apiURL);
		
		CloseableHttpResponse httpResponse = null;
		try {
			httpResponse = httpClient.execute(httpget, httpContext);
			
			List<URI> redirectList = httpContext.getRedirectLocations();
			if (redirectList != null)
				if (redirectList.get(redirectList.size()-1).toString().startsWith(URL_SUN_MINING_LOGIN_PAGE)) {
					myLogger.error("Cannot login to SM");
					return null;
				};
			
			HttpEntity httpEntity = httpResponse.getEntity();
			if (httpEntity == null) {
				myLogger.error("No content on the request: {}", apiURL);
				return null;
			}
			
			ArrayList<SMBalance> smCoinBalanceList = new ArrayList<>();
			String responseMsg = EntityUtils.toString(httpEntity, "UTF-8");
			MATCHER_SM_RESP_COIN_BALANCE.reset(responseMsg);
			while (MATCHER_SM_RESP_COIN_BALANCE.find()) {
				myLogger.debug("Balance {}: {} ({} USD)", MATCHER_SM_RESP_COIN_BALANCE.group(1),
						MATCHER_SM_RESP_COIN_BALANCE.group(2), MATCHER_SM_RESP_COIN_BALANCE.group(3));
				
				smCoinBalanceList.add(new SMBalance(MATCHER_SM_RESP_COIN_BALANCE.group(1).trim(),
						new BigDecimal(MATCHER_SM_RESP_COIN_BALANCE.group(2)),
						new BigDecimal(MATCHER_SM_RESP_COIN_BALANCE.group(3))));
			}
			
			if (smCoinBalanceList.isEmpty()) {
				myLogger.error("Unknown response msg: {}", responseMsg);
				return null;
			}
			
			return smCoinBalanceList;
		} catch (IOException e) {
			myLogger.error("Cannot execute http request", e);
		} catch (Exception e) {
			myLogger.error("Unknown error in executing http request", e);
		} finally {
			HttpClientUtils.closeQuietly(httpResponse);
			myLogger.debug("getSMBalance end");
		}
		return null;
	}
	
	public String composeProfitNotification(ArrayList<SMProfit> smProfitList, SMProfit[] lastSMProfit) {
		StringBuffer notificationMsg = new StringBuffer();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String todayDateStr = dateFormat.format(new Date());
		
		for (int i=0; i<SUN_MINING_COINS_ARRAY.length; i++) {
			SMProfit smProfit = smProfitList.get(i);
			//myLogger.debug(smProfit.getProfitDate() + ":" + smProfit.getProfitValue());
			if (dateFormat.format(smProfit.getProfitDate()).equals(todayDateStr) && smProfit.getProfitValue().compareTo(new BigDecimal(0)) > 0) {
				if (lastSMProfit[i] == null || !dateFormat.format(lastSMProfit[i].getProfitDate()).equals(todayDateStr)
						|| !lastSMProfit[i].getProfitValue().equals(smProfit.getProfitValue())) {
					lastSMProfit[i] = smProfit;
					
					notificationMsg.append(SUN_MINING_COINS_ARRAY[i] + ":" + smProfit.getProfitValue() + "\n");
					myLogger.debug(SUN_MINING_COINS_ARRAY[i] + ":" + smProfit.getProfitValue());
				}
			}
		}
		
		if (notificationMsg.length() > 0)
			notificationMsg.insert(0, "SM Profit of <b>" + todayDateStr + "</b>\n");
		
		return notificationMsg.toString();
	}
	
	public String composeBalanceNotification(ArrayList<SMBalance> smCoinBalanceList) {
		StringBuffer notificationMsg = new StringBuffer("Coin Balance of <b>");
		DecimalFormat coinBalanceFormat = new DecimalFormat("0.00000000");
		DecimalFormat usdBalanceFormat = new DecimalFormat("0.00");
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String todayDateStr = dateFormat.format(new Date());
		
		notificationMsg.append(todayDateStr).append("</b>\n");
		
		for (SMBalance smCoinBalance : smCoinBalanceList) {
			notificationMsg.append(smCoinBalance.getCoinName()).append(": ");
			notificationMsg.append(coinBalanceFormat.format(smCoinBalance.getBalance())).append(" (");
			notificationMsg.append(usdBalanceFormat.format(smCoinBalance.getUsdEquiv())).append(" USD)\n");
		}
		
		return notificationMsg.toString();
	}
}
