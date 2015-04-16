package org.apache.nutch.parse.s2jh;

import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

//import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.parse.HTMLMetaTags;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.parse.ParseFilter;
import org.apache.nutch.storage.WebPage;
import org.apache.nutch.storage.WebPage.Field;
import org.apache.nutch.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.sun.org.apache.xpath.internal.XPathAPI;

/**
 * 
 * @author EMAIL:xautlx@hotmail.com , QQ:2414521719
 *
 */
public abstract class AbstractHtmlParseFilter implements ParseFilter {

	public static final Logger LOG = LoggerFactory.getLogger(AbstractHtmlParseFilter.class);

	private static final long start = System.currentTimeMillis(); // start time of fetcher run

	private AtomicInteger pages = new AtomicInteger(0); // total pages fetched

	private Pattern filterPattern;

	protected Transformer transformer;

	private Configuration conf;

	public void setConf(Configuration conf) {
		this.conf = conf;
		String filterRegex = getUrlFilterRegex();
		if (StringUtils.isNotBlank(filterRegex)) {
			this.filterPattern = Pattern.compile(getUrlFilterRegex());
		}
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
			//transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			//transformer.setOutputProperty(OutputKeys.INDENT, "no");
			//transformer.setOutputProperty(OutputKeys.METHOD, "html");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public Configuration getConf() {
		return this.conf;
	}

	protected static NodeList selectNodeList(Node node, String xpath) {
		try {
			return XPathAPI.selectNodeList(node, xpath);
		} catch (TransformerException e) {
			LOG.warn("Bad 'xpath' expression [{}]", xpath);
		}
		return null;
	}

	protected Node selectSingleNode(Node contextNode, String xpath) {
		try {
			return XPathAPI.selectSingleNode(contextNode, xpath);
		} catch (TransformerException e) {
			LOG.warn("Bad 'xpath' expression [{}]", xpath);
		}
		return null;
	}	

	protected String getXPathValue(Node contextNode, String xpath) {
		return getXPathValue(contextNode, xpath, null);
	}

	protected String getXPathValue(Node contextNode, String xpath, String defaultVal) {
		Node node = selectSingleNode(contextNode, xpath);
		/* FOR DEBUG NODE CONTANT */
		//		        StringWriter sw = new StringWriter();
		//		        try {
		//		          Transformer t = TransformerFactory.newInstance().newTransformer();
		//		          t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		//		          t.transform(new DOMSource(node), new StreamResult(sw));
		//		        } catch (TransformerException te) {
		//		          System.out.println("nodeToString Transformer Exception");
		//		        }
		//		        LOG.info("node contant : [" + sw.toString() + "]");
		if (node == null) {
			return defaultVal;
		} else {
			String txt = null;
			if (node instanceof Text) {
				txt = node.getNodeValue();
			} else {
				txt = node.getTextContent();
			}
			return cleanInvisibleChar(txt);
		}
	}

	protected String getXPathListValue(Node contextNode, String xpath) {
		return getXPathListValue(contextNode, xpath, ",");
	}

	protected String getXPathListValue(Node contextNode, String xpath, String splitVal) {
		NodeList nodeList = selectNodeList(contextNode, xpath);
		if(nodeList.getLength() != 0){
			List<String> list = new ArrayList<String>();
			for(int i=0; i<nodeList.getLength() ; i++){
				list.add(nodeList.item(i).getTextContent());
			}
			return cleanInvisibleChar(StringUtils.join(list, splitVal));
		} else {
			return null;
		}
	}

	protected String getXPathMapValue(Node contextNode, String xpathKey, String xpathValue, String splitVal){
		NodeList keyList = selectNodeList(contextNode, xpathKey);
		NodeList valueList = selectNodeList(contextNode, xpathValue);
		if(keyList.getLength() != 0 && valueList.getLength() !=0){
			List<String> list = new ArrayList<String>();
			for(int i=0; i<keyList.getLength() ; i++){
				list.add(keyList.item(i).getTextContent() + ":" + valueList.item(i).getTextContent());
			}
			return StringUtils.join(list, splitVal);
		} else {
			return null;
		}
	}

	protected String getXPathHtml(Node contextNode, String xpath) {
		Node node = selectSingleNode(contextNode, xpath);
		return asString(node);
	}

	//去除重複內容
	protected String replaceRepeat(List<String> list){
		Set<String> set = new HashSet<String>();
		for (String element : list) {
			set.add(element);
		}
		List<String> resultList = new ArrayList<String>();
		resultList.addAll(set);
		return StringUtils.join(set, "，");	
	}

	private String asString(Node node) {
		if (node == null) {
			return "";
		}
		try {
			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(node), new StreamResult(writer));
			String xml = writer.toString();
			xml = StringUtils.substringAfter(xml, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			xml = xml.trim();
			return xml;
		} catch (Exception e) {
			throw new IllegalArgumentException("error for parse node to string.", e);
		}
	}

	/**
	 * 處理不同src圖片屬性格式，回傳統一格式的http格式圖片的URL
	 * @param url
	 * @param imgSrc
	 * @return
	 */
	protected String parseImgSrc(String url, String imgSrc) {
		if (StringUtils.isBlank(imgSrc)) {
			return null;
		}
		imgSrc = imgSrc.trim();
		//去掉链接最后的#号
		imgSrc = StringUtils.substringBefore(imgSrc, "#");
		if (imgSrc.startsWith("http")) {
			return imgSrc;
		} else if (imgSrc.startsWith("/")) {
			if (url.indexOf(".com") > -1) {
				return StringUtils.substringBefore(url, ".com/") + ".com" + imgSrc;
			} else if (url.indexOf(".net") > -1) {
				return StringUtils.substringBefore(url, ".net/") + ".net" + imgSrc;
			} else {
				throw new RuntimeException("Undefined site domain suffix");
			}
		} else {
			return StringUtils.substringBeforeLast(url, "/") + "/" + imgSrc;
		}
	}

	/**
	 * 清除無關的不可見空白字符
	 * @param str
	 * @return
	 */
	protected String cleanInvisibleChar(String str) {
		if (str != null) {
			str = StringUtils.remove(str, (char) 160);
			//str = StringUtils.remove(str, " ");
			str = StringUtils.remove(str, "\r");
			str = StringUtils.remove(str, "\n");
			str = StringUtils.remove(str, "\t");
			str = StringUtils.remove(str, "\\s*");
			str = StringUtils.remove(str, "◆");
			str = StringUtil.cleanField(str);
			str = str.trim();
		}
		return str;
	}

	/**
	 * 清除無關的Node節點元素
	 * @param str
	 * @return
	 */
	protected void cleanUnusedNodes(Node doc) {
		cleanUnusedNodes(doc, "//STYLE");
		cleanUnusedNodes(doc, "//MAP");
		cleanUnusedNodes(doc, "//SCRIPT");
		cleanUnusedNodes(doc, "//script");
	}

	/**
	 * 清除無關的Node節點元素
	 * @param str
	 * @return
	 */
	protected void cleanUnusedNodes(Node node, String xpath) {
		try {
			NodeList nodes = XPathAPI.selectNodeList(node, xpath);
			for (int i = 0; i < nodes.getLength(); i++) {
				Element element = (Element) nodes.item(i);
				element.getParentNode().removeChild(element);
			}
		} catch (DOMException e) {
			throw new IllegalStateException(e);
		} catch (TransformerException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Parse filter(String url, WebPage page, Parse parse, HTMLMetaTags metaTags, DocumentFragment doc) {
		LOG.debug("Invoking parse  {} for url: {}", this.getClass().getName(), url);
		try {
			//URL檢核
			if (filterPattern != null && !filterPattern.matcher(url).find()) {
				LOG.debug("Skipped {} as not match regex [{}]", this.getClass().getName(), getUrlFilterRegex());
				return parse;
			}

			if (page.getContent() == null) {
				LOG.warn("Empty content for url: {}", url);
				return parse;
			}

			//判斷頁面內容是否業務關注的頁面
			String html = asString(doc);
			if (!isContentMatchedForParse(url, html)) {
				LOG.debug("Skipped as content not match excepted");
				return parse;
			}

			//清除無關的Node節點元素
			cleanUnusedNodes(doc);

			pages.incrementAndGet();
			parse = filterInternal(url, page, parse, metaTags, doc);

			if (LOG.isInfoEnabled()) {
				long elapsed = (System.currentTimeMillis() - start) / 1000;
				float avgPagesSec = (float) pages.get() / elapsed;
				LOG.info(" - Custom prased total " + pages.get() + " pages, " + elapsed + " seconds, avg " + avgPagesSec + " pages/s");
			}
			return parse;
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	private static final String selectSQL = "SELECT count(*) from CRAWL_DATA where URL=?";
	private static final String deleteSQL = "DELETE from CRAWL_DATA where URL=?";
	private static final String insertSQL = "INSERT INTO CRAWL_DATA(URL, TITLE, PRICE, CATEGORY, PROMOTIONS_PRICE, PRODUCT_SPEC, ISVOUNCHER, PROMOTIONS, PREMIUMS, PRODUCT_DESCRIPTION, SRC_FROM, TAGS) "
			+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

	protected void saveCrawlData(String url, CrawlData crawlData) {
		String persistMode = conf.get("parse.data.persist.mode");
		//		if (StringUtils.isBlank(persistMode) || "println".equalsIgnoreCase(persistMode)) {
		//			System.out.println("Parsed data properties:");
		//			for (CrawlData crawlData : crawlDatas) {
		//				System.out.println(" - " + crawlData.getUrl() + " : " + crawlData.getDisplayValue());
		//			}
		//			return;
		//		}

		if ("jdbc".equalsIgnoreCase(persistMode)) {
			Connection conn = null;
			try {
				Class.forName(conf.get("jdbc.driver"));
				conn = DriverManager.getConnection(conf.get("jdbc.url"), conf.get("jdbc.username"), conf.get("jdbc.password"));
				PreparedStatement selectPS = conn.prepareStatement(selectSQL);
				selectPS.setString(1, url);
				ResultSet rs = selectPS.executeQuery();
				if (rs.next()) {
					int cnt = rs.getInt(1);
					rs.close();
					selectPS.close();
					if (cnt > 0) {
						LOG.debug("Cleaning exists properties for url: {}", url);
						PreparedStatement deletePS = conn.prepareStatement(deleteSQL);
						deletePS.setString(1, url);
						deletePS.execute();
						deletePS.close();
					}
					LOG.debug("Saving properties for url: {}", url);
					PreparedStatement insertPS = conn.prepareStatement(insertSQL);
					//					for (CrawlData crawlData : crawlDatas) {
					//						if (!crawlData.getUrl().equals(url)) {
					//							LOG.error("Invalid crawlData not match url: {}", url);
					//							continue;
					//						}
					//			LOG.debug(" - {} : {}", crawlData.getKey(), crawlData.getDisplayValue());
					insertPS.setString(1, url);
					insertPS.setString(2, crawlData.getTitle());
					insertPS.setBigDecimal(3, crawlData.getPrice());
					insertPS.setString(4, crawlData.getCategory());
					insertPS.setBigDecimal(5, crawlData.getPromotionsPrice());
					insertPS.setString(6, crawlData.getProductSpec());
					insertPS.setInt(7, crawlData.getIsVoucher());
					insertPS.setString(8, crawlData.getPromotions());
					insertPS.setString(9, crawlData.getPremiums());
					insertPS.setString(10, crawlData.getProductDescription());
					insertPS.setString(11, crawlData.getSrcFrom());
					insertPS.setString(12, crawlData.getTags());
					insertPS.addBatch();
					//					}
					insertPS.executeBatch();
					insertPS.close();
				}
			} catch (Exception e) {
				LOG.error("Error to get jdbc operation", e);
			} finally {
				try {
					if (conn != null) {
						conn.close();
					}
				} catch (Exception e) {
					LOG.error("Error to close jdbc connection", e);
				}
			}
			return;
		}

		//		if ("mongodb".equalsIgnoreCase(persistMode)) {
		//			try {
		//				MongoClient mongoClient = new MongoClient(conf.get("mongodb.host"), Integer.valueOf(conf.get("mongodb.port")));
		//				DB db = mongoClient.getDB(conf.get("mongodb.db"));
		//				DBCollection coll = db.getCollection("crawl_data");
		//				BasicDBObject bo = new BasicDBObject("url", url).append("fetch_time", new Date());
		//				LOG.debug("Saving properties for url: {}", url);
		//				for (CrawlData crawlData : crawlDatas) {
		//					if (!crawlData.getUrl().equals(url)) {
		//						LOG.error("Invalid crawlData not match url: {}", url);
		//						continue;
		//					}
		//					Map<String, Object> data = crawlData.getMapValue();
		//					LOG.debug(" - {} : {}", crawlData.getKey(), data);
		//					if (data.size() == 1) {
		//						bo.append(crawlData.getKey(), crawlData.getMapValue().entrySet().iterator().next().getValue());
		//					} else {
		//						bo.append(crawlData.getKey(), crawlData.getMapValue());
		//					}
		//				}
		//				coll.update(new BasicDBObject("url", url), bo, true, false);
		//				mongoClient.close();
		//			} catch (Exception e) {
		//				LOG.error(e.getMessage(), e);
		//			}
		//		}
	}

	@Override
	public Collection<Field> getFields() {
		return null;
	}

	/**
	 * 檢查url取得頁面的內容是否已經載入完畢，主要用來支持一些AJAX頁面延遲等待載入
	 * 返回false則表示告知Fetcher處理程序繼續AJAX執行短暫等待後再呼叫此方法直到返回true標示内容已載入完畢
	 * @param fetchUrl
	 * @param html 頁面HTML
	 * @return 默認返回true，子類別根據需要制定判斷邏輯
	 */
	public boolean isParseDataFetchLoaded(String url, String html) {
		if (filterPattern == null) {
			//没有url控制规则，直接放行
			return true;
		}
		//首先判斷url是否符合目前的過濾器，如果符合則繼續呼叫內容判斷邏輯
		if (filterPattern.matcher(url).find()) {
			if (StringUtils.isBlank(html)) {
				return false;
			}
			return isParseDataFetchLoaded(html);
		}
		return true;
	}

	/**
	 * 設置目前解析過濾器符合的URL正則表達式
	 * 只有符合的url才呼叫目前解析處理邏輯
	 * @return
	 */
	protected abstract String getUrlFilterRegex();

	/**
	 * 檢查url取得頁面的內容是否已經載入完畢，主要用來支持一些AJAX頁面延遲等待載入
	 * 返回false則表示告知Fetcher處理程序繼續AJAX執行短暫等待後再呼叫此方法直到返回true標示内容已載入完畢
	 * @param html 頁面HTML
	 * @return 默認返回true，子類別根據需要制定判斷邏輯
	 */
	protected abstract boolean isParseDataFetchLoaded(String html);

	/**
	 * 判斷頁面內容是否業務關注的頁面
	 * @param url
	 * @param html
	 * @return
	 */
	protected abstract boolean isContentMatchedForParse(String url, String html);

	/**
	 * 子類別實現具體的頁面數據解析邏輯
	 * @return
	 */
	public abstract Parse filterInternal(String url, WebPage page, Parse parse, HTMLMetaTags metaTags, DocumentFragment doc);
}
