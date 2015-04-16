package org.apache.nutch.parse.s2jh;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 
 * @author ben
 *
 */
public class CrawlData implements Serializable {

    private static final long serialVersionUID = -2670885640064330298L;

    private String title;
    private String category;
    private BigDecimal price;
    private BigDecimal promotionsPrice;
    private String productSpec;
    private int isVoucher;
    private String promotions;
    private String premiums;
    private String productDescription;
    private String tags;
    private String srcFrom;
    
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public BigDecimal getPrice() {
		return price;
	}
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	public BigDecimal getPromotionsPrice() {
		return promotionsPrice;
	}
	public void setPromotionsPrice(BigDecimal promotionsPrice) {
		this.promotionsPrice = promotionsPrice;
	}
	public String getProductSpec() {
		return productSpec;
	}
	public void setProductSpec(String productSpec) {
		this.productSpec = productSpec;
	}
	public int getIsVoucher() {
		return isVoucher;
	}
	public void setIsVoucher(int isVoucher) {
		this.isVoucher = isVoucher;
	}
	public String getPromotions() {
		return promotions;
	}
	public void setPromotions(String promotions) {
		this.promotions = promotions;
	}
	public String getPremiums() {
		return premiums;
	}
	public void setPremiums(String premiums) {
		this.premiums = premiums;
	}
	public String getProductDescription() {
		return productDescription;
	}
	public void setProductDescription(String productDescription) {
		this.productDescription = productDescription;
	}
	public String getTags() {
		return tags;
	}
	public void setTags(String tags) {
		this.tags = tags;
	}
	public String getSrcFrom() {
		return srcFrom;
	}
	public void setSrcFrom(String srcFrom) {
		this.srcFrom = srcFrom;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

}
