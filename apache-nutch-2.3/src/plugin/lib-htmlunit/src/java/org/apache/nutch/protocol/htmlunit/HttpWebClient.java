package org.apache.nutch.protocol.htmlunit;

import java.net.URL;

import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Htmlunit WebClient Helper
 * Use one WebClient instance per thread by ThreadLocal to support multiple threads execution
 * 
 * @author EMAIL:xautlx@hotmail.com , QQ:2414521719
 */
public class HttpWebClient {

    private static final Logger LOG = LoggerFactory.getLogger(HttpWebClient.class);

    private static ThreadLocal<WebClient> threadWebClient = new ThreadLocal<WebClient>();

    private static String acceptLanguage;

    public static HtmlPage getHtmlPage(String url, Configuration conf) {
        synchronized (Thread.currentThread()) {
            try {
                WebRequest req = new WebRequest(new URL(url));
                req.setAdditionalHeader("Accept-Language", acceptLanguage);
                //req.setAdditionalHeader("Cookie", "");

                WebClient webClient = threadWebClient.get();
                if (webClient == null) {
                    LOG.info("Initing web client for thread: {}", Thread.currentThread().getId());
                    //FIREFOX才能抓到pchome分類頁面內的一些需要執行javascript的連結
                    webClient = new WebClient(BrowserVersion.FIREFOX_24);
                    //pchome需要執行javascript
                    webClient.getOptions().setJavaScriptEnabled(true);
                    webClient.getOptions().setCssEnabled(false);
                    webClient.getOptions().setAppletEnabled(false);
                    webClient.getOptions().setThrowExceptionOnScriptError(false);
                    // AJAX support
                    webClient.setAjaxController(new NicelyResynchronizingAjaxController());
                    // Use extension version htmlunit cache process
                    webClient.setCache(new ExtHtmlunitCache());
                    // Enhanced WebConnection based on urlfilter
                    webClient.setWebConnection(new RegexHttpWebConnection(webClient));
                    webClient.waitForBackgroundJavaScript(600 * 1000);
                    //設定足夠高度來支援一些需要內容需要螢幕滾動顯示的頁面
                    webClient.getCurrentWindow().setInnerHeight(6000);
                    if (acceptLanguage == null && conf != null) {
                        acceptLanguage = conf.get("http.accept.language", " zh-tw,zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
                    }
                    threadWebClient.set(webClient);
                }
                HtmlPage page = webClient.getPage(req);
                return page;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static HtmlPage getHtmlPage(String url) {
        return getHtmlPage(url, null);
    }

    public static WebClient buildWebClient() {
        WebClient webClient = threadWebClient.get();
        if (webClient == null) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Building WebClient for Thread: {}", Thread.currentThread().getId());
            }
            synchronized (threadWebClient) {

                webClient = new WebClient(BrowserVersion.CHROME);
                webClient.getOptions().setJavaScriptEnabled(true);
                webClient.getOptions().setCssEnabled(true);
                webClient.getOptions().setAppletEnabled(false);
                webClient.getOptions().setThrowExceptionOnScriptError(false);
                // AJAX support
                webClient.setAjaxController(new NicelyResynchronizingAjaxController());
                // Use extension version htmlunit cache process
                webClient.setCache(new ExtHtmlunitCache());
                // Enhanced WebConnection based on urlfilter
                webClient.setWebConnection(new RegexHttpWebConnection(webClient));
                webClient.waitForBackgroundJavaScript(600 * 1000);
                //設定足夠高度來支援一些需要內容需要螢幕滾動顯示的頁面
                webClient.getCurrentWindow().setInnerHeight(6000);
                threadWebClient.set(webClient);
            }
        }
        return webClient;
    }

}
