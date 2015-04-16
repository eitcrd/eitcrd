package org.apache.nutch.parse.s2jh;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.nutch.parse.HTMLMetaTags;
import org.apache.nutch.parse.Outlink;
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
public class MOMOHtmlParseFilter extends AbstractHtmlParseFilter {

	public static final Logger LOG = LoggerFactory.getLogger(MOMOHtmlParseFilter.class);

	@SuppressWarnings("unchecked")
	@Override
	public Parse filterInternal(String url, WebPage page, Parse parse, HTMLMetaTags metaTags, DocumentFragment doc) {

		try {

		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
		}
		if(StringUtils.isNotBlank(getXPathValue(doc, "//DIV[@class='prdnoteArea']/H1"))){
			//開始擷取所需資料
			CrawlData crawlData = new CrawlData();
			crawlData.setTitle(getXPathValue(doc, "//DIV[@class='prdnoteArea']/H1"));
			LOG.info("price : [" + getXPathValue(doc, "//TABLE[@class='PriceDetail']/TBODY/TR/TD/DEL") + "]");
			crawlData.setPrice(new BigDecimal(StringUtils.replace(getXPathValue(doc, "//TABLE[@class='PriceDetail']/TBODY/TR/TD/DEL"), ",", "")));
			crawlData.setCategory("");
			crawlData.setPromotionsPrice(new BigDecimal(StringUtils.replace(getXPathValue(doc, "//TABLE[@class='PriceDetail']/TBODY/TR/TD/B"), ",", "")));
			crawlData.setProductSpec(getXPathValue(doc, "//DIV[@class='vendordetailview specification']/P"));
			if(StringUtils.isNotBlank(getXPathValue(doc, "//DIV[@class='prdimgArea']/P")))
				crawlData.setIsVoucher(0);//不可使用折價卷
			else
				crawlData.setIsVoucher(1);//可使用折價卷
			//促銷內容
			String activity = getXPathValue(doc, "//TABLE[@class='ineventArea']/TBODY/TR/TD/P");
			String rule = getXPathListValue(doc,  "//TABLE[@class='eventListTable']/TBODY/TR/TD/P", "，");
			if(StringUtils.isNotBlank(activity))
				crawlData.setPromotions(activity + "[" + rule + "]");		
			else 
				crawlData.setPromotions(null);	
			//贈品處理
			String  per = getXPathListValue(doc,  "//DL[@class='preferential']/DD/SPAN", "/");
			if(StringUtils.isNotBlank(per))
				crawlData.setPremiums(per + "/" + getXPathValue(doc, "//DIV[@class='vendordetailview giftsArea']/P"));		
			else
				crawlData.setPremiums(null);
			//商品敘述處理，並且去除品號
			String dec = getXPathListValue(doc, "//UL[@id='categoryActivityInfo']/LI[position()>1]" , "，");
			crawlData.setProductDescription(getXPathValue(doc, "//P[@id='sloganTitle']") + dec);
			//取得相關類別的資訊
			String tagDD = getXPathListValue(doc, "//DIV[@class='vendordetailview related_category']/DL/DD");
			String tagDT = getXPathListValue(doc, "//DIV[@class='vendordetailview related_category']/DL/DT");
			List<String> tags = null;
			String tag = null;
			if(StringUtils.isNotBlank(tagDT) && StringUtils.isNotBlank(tagDD)){
				tags = ListUtils.union(Arrays.asList(StringUtils.split(tagDD, ",")), Arrays.asList(StringUtils.split(tagDT, ",")));
				tag = replaceRepeat(tags);
			} else {
				tag = StringUtils.EMPTY;
			}
			crawlData.setTags(tag);
			crawlData.setSrcFrom("momo");			
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
		String totalPageNum = getXPathValue(doc, "//DIV[@class='toppageArea bt770class']/UL/LI[@class='pageval']");
		totalPageNum = StringUtils.removeStart(totalPageNum, "頁數 1 / ");
		if(StringUtils.isNotBlank(totalPageNum) && StringUtils.startsWith(totalPageNum, "頁數 1 / ")){
			//在商品分類第一頁中若有分類則將分類頁面加入outlink			
			totalPageNum = StringUtils.removeStart(totalPageNum, "頁數 1 / ");
			Outlink[] outlinks = parse.getOutlinks();
			List<Outlink> arrayList = new ArrayList<Outlink>();
			for(int i=0 ; i<outlinks.length ; i++){
				arrayList.add(outlinks[i]);
			}
			for(int i=2 ; i<=Integer.valueOf(totalPageNum) ; i++){
				try {	
					arrayList.add(new Outlink(url + "&p_pageNum=" + i, "分頁" + i)) ;
				} catch (MalformedURLException e) {
					LOG.error(e.getMessage());
				}
			}
			parse.setOutlinks(arrayList.toArray(outlinks));
		}
		return parse;
	}

	@Override
	public String getUrlFilterRegex() {
		return "http://*";
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
