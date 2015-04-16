package org.apache.nutch.parse.s2jh;

import java.math.BigDecimal;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.nutch.parse.HTMLMetaTags;
import org.apache.nutch.parse.Parse;
import org.apache.nutch.storage.WebPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DocumentFragment;

/**
 * 
 * @author ben
 *
 */
public class YAHOOHtmlParseFilter extends AbstractHtmlParseFilter {

	public static final Logger LOG = LoggerFactory.getLogger(MOMOHtmlParseFilter.class);

	@Override
	public Parse filterInternal(String url, WebPage page, Parse parse, HTMLMetaTags metaTags, DocumentFragment doc) {

		try {

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		if(StringUtils.isNotBlank(getXPathValue(doc, "//DIV[@class='title']"))){
			//開始擷取所需資料
			CrawlData crawlData = new CrawlData();
			crawlData.setTitle(getXPathValue(doc, "//DIV[@class='title']"));
			crawlData.setPrice(new BigDecimal(StringUtils.replace(getXPathValue(doc, "//DIV[@class='priceinfo']/SPAN[@class='price']"), ",", "")));
			crawlData.setCategory(null);
			String promotionsPrice = getXPathValue(doc, "//DIV[@class='suggest']/SPAN[@class='price']");
			promotionsPrice = StringUtils.replace(promotionsPrice, "$", "");
			crawlData.setPromotionsPrice(new BigDecimal(StringUtils.replace(promotionsPrice, ",", "")));
			String productSpec = getXPathMapValue(doc, "//TABLE[@id='StructuredDataTable']/TBODY/TR/TH", "//TABLE[@id='StructuredDataTable']/TBODY/TR/TD", "，");
			crawlData.setProductSpec(productSpec);
			crawlData.setPromotions(getXPathValue(doc, "//DIV[@class='detail yui3-u']/A[@class='readme discount']"));
			crawlData.setProductDescription(getXPathListValue(doc,  "//DIV[@class='yui3-u item-spec']/UL[@class='desc-list yui3-g']", "，"));
			crawlData.setPremiums(getXPathListValue(doc, "//UL[@class='giftlist yui3-g itmlstbox']/LI/DIV[contains(@class, 'desc')]", "，"));
			crawlData.setTags(null);
			crawlData.setSrcFrom("yahoo購物中心");
			LOG.info("parased data [" + ToStringBuilder.reflectionToString(crawlData) + "]");
			saveCrawlData(url, crawlData);
		}
		// 用於網頁內容索引的頁面內容，一班是去頭去尾用處理後的有效訊息內容
		String txt = getXPathValue(doc, "//DIV[@id='BodyBase']");
		if (StringUtils.isNotBlank(txt)) {
			parse.setText(txt);
		} else {
			LOG.warn("NO data parased");
		}
//		//分頁處理
//		String totalPageNum = getXPathValue(doc, "//DIV[@class='toppageArea bt770class']/UL/LI[@class='pageval']");
//		totalPageNum = StringUtils.removeStart(totalPageNum, "頁數 1 / ");
//		if(StringUtils.isNotBlank(totalPageNum) && StringUtils.startsWith(totalPageNum, "頁數 1 / ")){
//			//在商品分類第一頁中若有分類則將分類頁面加入outlink			
//			totalPageNum = StringUtils.removeStart(totalPageNum, "頁數 1 / ");
//			Outlink[] outlinks = parse.getOutlinks();
//			List<Outlink> arrayList = new ArrayList<Outlink>();
//			for(int i=0 ; i<outlinks.length ; i++){
//				arrayList.add(outlinks[i]);
//			}
//			for(int i=2 ; i<=Integer.valueOf(totalPageNum) ; i++){
//				try {	
//					arrayList.add(new Outlink(url + "&p_pageNum=" + i, "分頁" + i)) ;
//				} catch (MalformedURLException e) {
//					LOG.error(e.getMessage());
//				}
//			}
//			parse.setOutlinks(arrayList.toArray(outlinks));
//		}
		return parse;
	}

	@Override
	public String getUrlFilterRegex() {
		return "https://*";
	}

	@Override
	protected boolean isParseDataFetchLoaded(String html) {
		return !html.contains("內容載入中");
	}

	@Override
	protected boolean isContentMatchedForParse(String url, String html) {
		return true;
	}
}
