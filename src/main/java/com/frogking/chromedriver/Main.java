package com.frogking.chromedriver;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.ConsoleAppender;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.frogking.chromedriver.ChromeDriverBuilder;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import java.io.StringReader;


/**
 * URLPipeline is a class to store urls, and get url from it
 */
class URLPipeline{
    private List<String> urls = new ArrayList<>();

    synchronized public String getUrl(){
        if (urls.size() <= 0){
            return null;
        }else {
            return urls.remove(0);
        }
    }

    synchronized public void addUrl(String url){
        urls.add(url);
    }

    synchronized public void addAll(List<String> url){
        urls.addAll(url);
    }

    synchronized public int size(){
        return urls.size();
    }

    synchronized public void clear(){
        urls.clear();
    }

    synchronized public List<String> getAll(){
        return urls;
    }

    synchronized public void readTxt(String path,String restartFilePath) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line;
            while ((line = br.readLine()) != null) {
                urls.add(line.strip());
            }
            if (restartFilePath != null && new File(restartFilePath).exists()){
                while ((line = br.readLine()) != null){
                    if (urls.contains(line.strip())){
                        urls.remove(line.strip());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

class DataPipeline<T>{
    private List<Map<String,Object>> data = new ArrayList<>();

    synchronized public void addData(String url,T d){
        Map<String,Object> map = new ConcurrentHashMap<>();
        map.put("url",url);
        map.put("data",d);
        data.add(map);
    }

    synchronized public Map<String,Object> getData(){
        if (data.size() <= 0){
            return null;
        }else {
            return data.remove(0);
        }
    }

    synchronized public int size(){
        return data.size();
    }

    synchronized public void clear(){
        data.clear();
    }

    synchronized public void addAll(List<Map<String,Object>> d){
        data.addAll(d);
    }

    synchronized public List<Map<String,Object>> getAll(){
        return data;
    }

    synchronized public void writeJSON(String path){
        try {
            FileWriter fw = new FileWriter(path);
            for (Map<String,Object> d : data){
                fw.write(JSONObject.toJSONString(d) + "\n");
            }
            fw.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}


abstract class Spider<T> {

    private static final Logger logger = LogbackConfig.createLogger(Spider.class);

    private String method;

    private String driver_home;

    private URLPipeline urlPipeline;

    private DataPipeline<T> dataPipeline;

    public void setDriver_home(String driver_home) {
        this.driver_home = driver_home;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Spider(String method,
                  String driver_home,
                  URLPipeline urlPipeline,
                  DataPipeline<T> dataPipeline){
        if (method!=null && (method.equals("chromedriver") || method.equals("http"))){
            this.method = method;
        }else {
            this.method = "http";
        }
        if (this.method.equals("chromedriver") && driver_home == null){
            throw new RuntimeException("driver_home is necessary");
        }else {
            this.driver_home = driver_home;
        }
        if (urlPipeline == null){
            throw new RuntimeException("urlPipeline is necessary");
        }
        this.urlPipeline = urlPipeline;
        if (dataPipeline == null){
            throw new RuntimeException("dataPipeline is necessary");
        }
        this.dataPipeline = dataPipeline;
    }

    /**
     * get proxy
     * @return proxy host:port string
     */
    protected String getProxy(){
        return null;
    }

    /**
     * create chrome driver by UndetectedChromeDriver
     * @param driver_home chromedriver path
     * @return ChromeDriver instance same as Selenium
     */

    protected synchronized ChromeDriver createChrome(String driver_home){
        //String driver_home = "/Users/lwz/Documents/software/chromedriver/chromedriver-5";
        // 1  if use chromeOptions, recommend use this
        // ChromeDriverBuilder could throw RuntimeError, you can catch it, *catch it is unnecessary
        ChromeOptions chrome_options = new ChromeOptions();
        chrome_options.addArguments("--window-size=1920,1080");
        String proxy = getProxy();
        if (proxy != null){
            chrome_options.addArguments("--proxy-server="+proxy);
        }
        chrome_options.addArguments("--headless=new");
        //chrome_options.addArguments("--no-sandbox");
        //chrome_options.addArguments("--disable-dev-shm-usage");
        ChromeDriverService service = new ChromeDriverService.Builder()
                .usingDriverExecutable(new File(driver_home))
                .usingAnyFreePort()
                .build();
        //ChromeDriver chromeDriver1 = new ChromeDriver(service);
        ChromeDriver chromeDriver1 = new ChromeDriverBuilder()
                .build(chrome_options,driver_home);
        // 2  don't use chromeOptions
        //ChromeDriver chromeDriver2 = new ChromeDriverBuilder()
        //        .build("/Users/lwz/Documents/software/chromedriver/chromedriver-2");
        logger.info("create chrome driver finished");
        return chromeDriver1;
    }

    /**
     * get one page from url
     * @return
     */
    protected Object getPage(String url,ChromeDriver driver){
        if (url == null){
            return null;
        }
        // case method
        if (method.equals("chromedriver")){
            driver.get(url);
            logger.info("get page from url: " + url);
            return driver;
        }else if (method.equals("http")){
            logger.info("get page from url: " + url);
            return HttpUtil.get(url);
        }else {
            throw new RuntimeException("method is not supported");
        }
    }

    protected T spider_page(Object page){
        if (page == null){
            return null;
        }else if (this.method.equals("chromedriver")) {
            ChromeDriver driver = (ChromeDriver) page;
            logger.info("parse page start");
            return parse_driver_page(driver);
        }else if (this.method.equals("http")) {
            String html = (String) page;
            logger.info("parse page start");
            return parse_http_page(html);
        }else {
            throw new RuntimeException("method is not supported");
        }
    }

    abstract protected T parse_driver_page(ChromeDriver driver);

    abstract protected T parse_http_page(String html);

    protected void save_data(String url,T data){
        if (data == null){
            return;
        }
        this.dataPipeline.addData(url,data);
    }

    public Thread[] run(int waitTime,int chromeDriverNum,int threadNum) throws InterruptedException {
        Thread[] threads = new Thread[threadNum];
        for (int j = 0; j < threadNum; j++) {
            int finalJ = j;
            threads[j] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (method.equals("chromedriver")) {
                            int i = 0;
                            ChromeDriver driver = null;
                            while (true) {
                                try {
                                    if (urlPipeline.size() <= 0) {
                                        if (driver != null) {
                                            driver.quit();
                                        }
                                        break;
                                    }
                                    if (i % chromeDriverNum == 0) {
                                        if (driver != null) {
                                            driver.quit();
                                        }
                                        driver = createChrome(driver_home);
                                    }
                                }catch (Exception e){
                                    logger.error("create chrome driver failed");
                                    logger.error(e.toString());
                                    e.printStackTrace();
                                    break;
                                }
                                String url = urlPipeline.getUrl();
                                Object page = null;
                                try {
                                    page = getPage(url, driver);
                                }catch (Exception e){
                                    logger.error("get page failed");
                                    logger.error(e.toString());
                                    e.printStackTrace();
                                    continue;
                                }
                                T data = null;
                                try{
                                    data = spider_page(page);
                                }catch (Exception e){
                                    logger.error("spider page failed");
                                    logger.error(e.toString());
                                    e.printStackTrace();
                                    continue;
                                }
                                //System.out.println(String.valueOf(finalJ)+" "+data);
                                save_data(url,data);
                                i++;
                                try {
                                    Random random = new Random();
                                    random.setSeed(System.currentTimeMillis());
                                    int delta = random.nextInt(waitTime);
                                    Thread.sleep((long) (waitTime + delta));
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else if (method.equals("http")) {
                            while (true) {
                                if (urlPipeline.size() <= 0){
                                    break;
                                }
                                String url = urlPipeline.getUrl();
                                Object page = null;
                                try {
                                    page = getPage(url, null);
                                }catch (Exception e){
                                    logger.error("get page failed");
                                    logger.error(e.toString());
                                    e.printStackTrace();
                                    continue;
                                }

                                T data = null;
                                try{
                                    data = spider_page(page);
                                }catch (Exception e){
                                    logger.error("spider page failed");
                                    logger.error(e.toString());
                                    e.printStackTrace();
                                    continue;
                                }
                                save_data(url,data);
                                try {
                                    Random random = new Random();
                                    random.setSeed(System.currentTimeMillis());
                                    int delta = random.nextInt(waitTime);
                                    Thread.sleep((long) (waitTime + delta));
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            throw new RuntimeException("method is not supported");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error(e.toString());
                    }
                }
            });
            threads[j].start();
        }
        for (int j = 0; j < threadNum; j++) {
            threads[j].join();
        }

        return threads;
    }
}


class WriterThread<T>{

    private static final Logger logger = LogbackConfig.createLogger(WriterThread.class);

    private DataPipeline<T> dataPipeline;

    private String jsonPath;

    private long waittime;

    private String restartFilePath;

    public WriterThread(DataPipeline<T> dataPipeline,
                        String jsonPath,
                        String restartFilePath,
                        long waittime){
        this.dataPipeline = dataPipeline;
        this.jsonPath = jsonPath;
        this.waittime = waittime;
        this.restartFilePath = restartFilePath;
    }

    public void start(Thread[] insertThreads){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    if (dataPipeline.size() <= 0){
                        boolean flag = true;
                        for (Thread thread : insertThreads){
                            if (thread.isAlive()){
                                flag = false;
                                break;
                            }
                        }
                        if (flag){
                            break;
                        }
                    }
                    if (dataPipeline.size() > 0){
                        Map<String,Object> data = dataPipeline.getData();
                        String url = (String) data.get("url");
                        T d = (T) data.get("data");
                        String json = JSONObject.toJSONString(data);
                        try {
                            if (new File(jsonPath).exists()){
                                FileWriter fileWriter = new FileWriter(jsonPath,true);
                                fileWriter.write(json);
                                fileWriter.write("\n");
                                fileWriter.flush();
                                fileWriter.close();
                            }else{
                                new File(jsonPath).createNewFile();
                                FileWriter fileWriter = new FileWriter(jsonPath);
                                fileWriter.write(json);
                                fileWriter.write("\n");
                                fileWriter.flush();
                                fileWriter.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            if (new File(restartFilePath).exists()){
                                FileWriter fileWriter = new FileWriter(restartFilePath,true);
                                fileWriter.write(url);
                                fileWriter.write("\n");
                                fileWriter.flush();
                                fileWriter.close();
                            }else{
                                new File(restartFilePath).createNewFile();
                                FileWriter fileWriter = new FileWriter(restartFilePath,true);
                                fileWriter.write(url);
                                fileWriter.write("\n");
                                fileWriter.flush();
                                fileWriter.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(waittime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }
}


class XmlSpider extends Spider<Map<String,Object>>{

    private static final Logger logger = LogbackConfig.createLogger(XmlSpider.class);

    private Document document;

    private String proxy_url;

    private int one_proxy_use_num;

    private int t;

    private String proxy;

    public XmlSpider(Document document,String driverHome,
                     URLPipeline urlPipeline,
                     DataPipeline<Map<String,Object>> dataPipeline,
                     String proxy_url,
                     int one_proxy_use_num){
        super("chromedriver",driverHome,urlPipeline,dataPipeline);
        this.document = document;
        this.proxy_url = proxy_url;
        this.one_proxy_use_num = one_proxy_use_num;
        this.t = 0;
    }

    @Override
    protected String getProxy(){
        if (proxy_url == null || proxy_url.equals("null")){
            return null;
        }
        if (t % one_proxy_use_num == 0){
            t = 0;
            this.proxy = HttpUtil.get(proxy_url);
        }else {
            t++;
        }
        return this.proxy;
    }

    @Override
    protected Map<String, Object> parse_driver_page(ChromeDriver driver) {
        Map<String,Object> resultMap = new HashMap<>();
        for (int i = 0; i < document.getElementsByTagName("data").item(0).getChildNodes().getLength();i++) {
            Node node = document.getElementsByTagName("data").item(0).getChildNodes().item(i);
            if (node.getNodeName().equals("elements")) {
                List<Map<String, Object>> elements = new ArrayList<>();
                String name = "";
                String type = "";
                String parse = "";
                String click_parse = "";
                for (int j = 0; j < node.getChildNodes().getLength(); j++) {
                    Node element = node.getChildNodes().item(j);
                    if (element.getNodeName().equals("name")) {
                        name = element.getTextContent();
                    } else if (element.getNodeName().equals("type")) {
                        type = element.getTextContent();
                    } else if (element.getNodeName().equals("parse")) {
                        parse = element.getTextContent();
                    } else if (element.getNodeName().equals("click")){
                        click_parse = element.getTextContent();
                    }
                }

                if (!click_parse.equals("")){
                    if (type.equals("xpath")) {
                        WebElement click = driver.findElement(By.xpath(click_parse));
                        driver.executeScript("arguments[0].click();", click);
                    }else if (type.equals("id")){
                        WebElement click = driver.findElement(By.id(click_parse));
                        driver.executeScript("arguments[0].click();", click);
                    }else if (type.equals("class")){
                        WebElement click = driver.findElement(By.className(click_parse));
                        driver.executeScript("arguments[0].click();", click);
                    }else if (type.equals("css")){
                        WebElement click = driver.findElement(By.cssSelector(click_parse));
                        driver.executeScript("arguments[0].click();", click);
                    }else if (type.equals("name")){
                        WebElement click = driver.findElement(By.name(click_parse));
                        driver.executeScript("arguments[0].click();", click);
                    }
                }

                List<WebElement> _webElements = null;
                if (type.equals("xpath")) {
                    _webElements = driver.findElements(By.xpath(parse));
                } else if (type.equals("id")) {
                    _webElements = driver.findElements(By.id(parse));
                } else if (type.equals("class")) {
                    _webElements = driver.findElements(By.className(parse));
                } else if (type.equals("css")) {
                    _webElements = driver.findElements(By.cssSelector(parse));
                } else if (type.equals("name")) {
                    _webElements = driver.findElements(By.name(parse));
                }
                if (_webElements != null) {
                    for (WebElement _webElement : _webElements) {
                        Map<String, Object> _resultMap = new HashMap<>();
                        for (int k = 0; k < node.getChildNodes().getLength(); k++) {
                            Node _node = node.getChildNodes().item(k);
                            if (_node.getNodeName().equals("elements") || _node.getNodeName().equals("element")) {
                                parse_element(_webElement, _node, _resultMap,driver);
                            }
                        }
                        elements.add(_resultMap);
                    }
                    resultMap.put(name, elements);
                }
            } else if (node.getNodeName().equals("element")) {
                String name = "";
                String type = "";
                String parse = "";
                String click_parse = "";
                for (int j = 0; j < node.getChildNodes().getLength(); j++) {
                    Node element = node.getChildNodes().item(j);
                    if (element.getNodeName().equals("name")) {
                        name = element.getTextContent();
                    } else if (element.getNodeName().equals("type")) {
                        type = element.getTextContent();
                    } else if (element.getNodeName().equals("parse")) {
                        parse = element.getTextContent();
                    } else if (element.getNodeName().equals("click")){
                        click_parse = element.getTextContent();
                    }
                }

                if (!click_parse.equals("")){
                    if (type.equals("xpath")) {
                        WebElement click = driver.findElement(By.xpath(click_parse));
                        driver.executeScript("arguments[0].click();", click);
                    }else if (type.equals("id")){
                        WebElement click = driver.findElement(By.id(click_parse));
                        driver.executeScript("arguments[0].click();", click);
                    }else if (type.equals("class")){
                        WebElement click = driver.findElement(By.className(click_parse));
                        driver.executeScript("arguments[0].click();", click);
                    }else if (type.equals("css")){
                        WebElement click = driver.findElement(By.cssSelector(click_parse));
                        driver.executeScript("arguments[0].click();", click);
                    }else if (type.equals("name")){
                        WebElement click = driver.findElement(By.name(click_parse));
                        driver.executeScript("arguments[0].click();", click);
                    }
                    try {
                        Thread.sleep(300);
                    }catch (Exception ignored){ }
                }

                WebElement _webElement = null;
                if (type.equals("xpath")) {
                    _webElement = driver.findElement(By.xpath(parse));
                } else if (type.equals("id")) {
                    _webElement = driver.findElement(By.id(parse));
                } else if (type.equals("class")) {
                    _webElement = driver.findElement(By.className(parse));
                } else if (type.equals("css")) {
                    _webElement = driver.findElement(By.cssSelector(parse));
                } else if (type.equals("name")) {
                    _webElement = driver.findElement(By.name(parse));
                }
                if (_webElement != null) {
                    for (int j = 0; j < node.getChildNodes().getLength(); j++) {
                        Node element = node.getChildNodes().item(j);
                        if (element.getNodeName().equals("text")) {
                            String text = _webElement.getText();
                            String _type = "";
                            String _regex = "";
                            for (int k = 0; k < element.getChildNodes().getLength(); k++) {
                                Node _node = element.getChildNodes().item(k);
                                if (_node.getNodeName().equals("type")) {
                                    _type = _node.getTextContent();
                                } else if (_node.getNodeName().equals("regex")) {
                                    _regex = _node.getTextContent();
                                }
                            }
                            if (_type.equals("find")) {
                                Pattern pattern = Pattern.compile(_regex);
                                Matcher matcher = pattern.matcher(text);
                                if (matcher.find()) {
                                    resultMap.put(name, matcher.group());
                                }
                            } else if (_type.equals("delete")) {
                                resultMap.put(name, text.replaceAll(_regex, ""));
                            }
                        } else if (element.getNodeName().equals("attribute")) {
                            String attr = _webElement.getAttribute(element.getFirstChild().getTextContent());
                            resultMap.put(name, attr);
                        }
                    }
                }
            }
        }
        //logger.info("resultMap:{}",resultMap);
        return resultMap;
    }

    private void parse_element(WebElement webElement,Node node,Map<String,Object> resultMap,ChromeDriver driver){
        if(node.getNodeName().equals("elements")){
            List<Map<String,Object>> elements = new ArrayList<>();
            String name = "";
            String type = "";
            String parse = "";
            String click_parse = "";
            for (int j = 0; j < node.getChildNodes().getLength(); j++) {
                Node element = node.getChildNodes().item(j);
                if (element.getNodeName().equals("name")){
                    name = element.getTextContent();
                }else if (element.getNodeName().equals("type")){
                    type = element.getTextContent();
                }else if (element.getNodeName().equals("parse")){
                    parse = element.getTextContent();
                }else if (element.getNodeName().equals("click")){
                    click_parse = element.getTextContent();
                }
            }
            if (!click_parse.equals("")){
                if (type.equals("xpath")) {
                    WebElement click = webElement.findElement(By.xpath(click_parse));
                    driver.executeScript("arguments[0].click();", click);
                }else if (type.equals("id")){
                    WebElement click = webElement.findElement(By.id(click_parse));
                    driver.executeScript("arguments[0].click();", click);
                }else if (type.equals("class")){
                    WebElement click = webElement.findElement(By.className(click_parse));
                    driver.executeScript("arguments[0].click();", click);
                }else if (type.equals("css")){
                    WebElement click = webElement.findElement(By.cssSelector(click_parse));
                    driver.executeScript("arguments[0].click();", click);
                }else if (type.equals("name")){
                    WebElement click = webElement.findElement(By.name(click_parse));
                    driver.executeScript("arguments[0].click();", click);
                }
                try {
                    Thread.sleep(300);
                }catch (Exception ignored){ }
            }
            List<WebElement> _webElements = null;
            if (type.equals("xpath")) {
                _webElements = webElement.findElements(By.xpath(parse));
            }else if (type.equals("id")){
                _webElements = webElement.findElements(By.id(parse));
            }else if (type.equals("class")){
                _webElements = webElement.findElements(By.className(parse));
            }else if (type.equals("css")){
                _webElements = webElement.findElements(By.cssSelector(parse));
            }else if (type.equals("name")){
                _webElements = webElement.findElements(By.name(parse));
            }
            if (_webElements != null){
                for (WebElement _webElement : _webElements) {
                    Map<String, Object> _resultMap = new HashMap<>();
                    for (int k = 0; k < node.getChildNodes().getLength(); k++) {
                        Node _node = node.getChildNodes().item(k);
                        if (_node.getNodeName().equals("elements") || _node.getNodeName().equals("element")) {
                            parse_element(_webElement, _node, _resultMap, driver);
                        }
                    }
                    elements.add(_resultMap);
                }
                resultMap.put(name, elements);
            }
        }else if (node.getNodeName().equals("element")){
            String name = "";
            String type = "";
            String parse = "";
            String click_parse = "";
            for (int j = 0; j < node.getChildNodes().getLength(); j++) {
                Node element = node.getChildNodes().item(j);
                if (element.getNodeName().equals("name")){
                    name = element.getTextContent();
                }else if (element.getNodeName().equals("type")){
                    type = element.getTextContent();
                }else if (element.getNodeName().equals("parse")){
                    parse = element.getTextContent();
                }else if (element.getNodeName().equals("click")){
                    click_parse = element.getTextContent();
                }
            }

            if (!click_parse.equals("")){
                if (type.equals("xpath")) {
                    WebElement click = webElement.findElement(By.xpath(click_parse));
                    driver.executeScript("arguments[0].click();", click);
                }else if (type.equals("id")){
                    WebElement click = webElement.findElement(By.id(click_parse));
                    driver.executeScript("arguments[0].click();", click);
                }else if (type.equals("class")){
                    WebElement click = webElement.findElement(By.className(click_parse));
                    driver.executeScript("arguments[0].click();", click);
                }else if (type.equals("css")){
                    WebElement click = webElement.findElement(By.cssSelector(click_parse));
                    driver.executeScript("arguments[0].click();", click);
                }else if (type.equals("name")){
                    WebElement click = webElement.findElement(By.name(click_parse));
                    driver.executeScript("arguments[0].click();", click);
                }
            }

            WebElement _webElement = null;
            if (parse.equals("")){
                _webElement = webElement;
            }else if (type.equals("xpath")) {
                _webElement = webElement.findElement(By.xpath(parse));
            }else if (type.equals("id")){
                _webElement = webElement.findElement(By.id(parse));
            }else if (type.equals("class")){
                _webElement = webElement.findElement(By.className(parse));
            }else if (type.equals("css")){
                _webElement = webElement.findElement(By.cssSelector(parse));
            }else if (type.equals("name")){
                _webElement = webElement.findElement(By.name(parse));
            }

            if (_webElement != null) {
                for (int j = 0; j < node.getChildNodes().getLength(); j++) {
                    Node element = node.getChildNodes().item(j);
                    if (element.getNodeName().equals("text")) {
                        String text = _webElement.getText();
                        String _type = "";
                        String _regex = "";
                        for (int k = 0; k < element.getChildNodes().getLength(); k++) {
                            Node _node = element.getChildNodes().item(k);
                            if (_node.getNodeName().equals("type")) {
                                _type = _node.getTextContent();
                            }else if (_node.getNodeName().equals("regex")){
                                _regex = _node.getTextContent();
                            }
                        }
                        if (_type.equals("find")){
                            Pattern pattern = Pattern.compile(_regex);
                            Matcher matcher = pattern.matcher(text);
                            if (matcher.find()){
                                resultMap.put(name, matcher.group());
                            }
                        }else if (_type.equals("delete")){
                            resultMap.put(name, text.replaceAll(_regex, ""));
                        }
                    }
                    else if (element.getNodeName().equals("attribute")) {
                        String _type = "";
                        for (int k = 0; k < element.getChildNodes().getLength(); k++) {
                            Node _node = element.getChildNodes().item(k);
                            if (_node.getNodeName().equals("type")) {
                                _type = _node.getTextContent();
                            }
                        }
                        String attr = _webElement.getAttribute(_type);
                        resultMap.put(name, attr);
                    }
                }
            }
        }
    }

    @Override
    protected Map<String, Object> parse_http_page(String html) {
        // only support chrome driver
        return null;
    }
}


class XmlSpiderBuilder{

    private static final Logger logger = LogbackConfig.createLogger(XmlSpiderBuilder.class);

    private Document document;

    XmlSpiderBuilder(String xmlPath){
        String xmlContent = getXmlContent(xmlPath);
        if (xmlContent == null){
            logger.error("xml file not found");
            throw new RuntimeException("xml file not found");
        }
        Document document = parseXml(xmlContent);
        if (document == null){
            logger.error("xml parse error");
            throw new RuntimeException("xml parse error");
        }
        this.document = document;
    }

    public void run(){
        String spiderName = document.getElementsByTagName("spidername").item(0).getTextContent();
        logger.info("spider name: " + spiderName);
        String urlFilePath = document.getElementsByTagName("urlfilepath").item(0).getTextContent();
        logger.info("url file path: " + urlFilePath);
        String jsonFilePath = document.getElementsByTagName("outputfilepath").item(0).getTextContent();
        logger.info("json file path: " + jsonFilePath);
        String restartFilePath = document.getElementsByTagName("restartfilepath").item(0).getTextContent();
        logger.info("restart file path: " + restartFilePath);

        String proxyUrl = document.getElementsByTagName("proxy_url").item(0).getTextContent();
        logger.info("proxy url: " + proxyUrl);

        int oneProxyUseNum = Integer.parseInt(document.getElementsByTagName("one_proxy_use_num").item(0).getTextContent());
        logger.info("one proxy use num: " + oneProxyUseNum);

        String driverHome = document.getElementsByTagName("driverhome").item(0).getTextContent();
        logger.info("driver home: " + driverHome);

        long writeWaittime = Long.parseLong(document.getElementsByTagName("write_waittime").item(0).getTextContent());
        logger.info("write waittime: " + writeWaittime);

        int spiderWaittime = Integer.parseInt(document.getElementsByTagName("spider_waittime").item(0).getTextContent());
        logger.info("spider waittime: " + spiderWaittime);

        int threadNum = Integer.parseInt(document.getElementsByTagName("thread_num").item(0).getTextContent());
        logger.info("thread num: " + threadNum);

        URLPipeline urlPipeline = new URLPipeline();
        urlPipeline.readTxt(urlFilePath,restartFilePath);

        DataPipeline<Map<String,Object>> dataPipeline = new DataPipeline<Map<String,Object>>();

        XmlSpider xmlSpider =
                new XmlSpider(document,driverHome,urlPipeline,dataPipeline,proxyUrl,1);

        logger.info("xml spider init success");

        WriterThread<Map<String,Object>> writerThread =
                new WriterThread<Map<String,Object>>(dataPipeline,jsonFilePath,restartFilePath,writeWaittime);

        logger.info("writer thread init success");

        try {
            Thread[] ts = xmlSpider.run(spiderWaittime, oneProxyUseNum, threadNum);
            logger.info("spider run success");
            writerThread.start(ts);
            logger.info("writer thread start success");
        } catch (InterruptedException e) {
            logger.error("spider run error");
            e.printStackTrace();
        }
    }

    private String getXmlContent(String xmlPath){
        try {
            File file = new File(xmlPath);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = null;
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            reader.close();
            return stringBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Document parseXml(String xmlContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xmlContent));
            return builder.parse(is);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void test(){
        String spiderName = document.getElementsByTagName("spidername").item(0).getTextContent();
        logger.info("test: spider name: " + spiderName);
        String urlFilePath = document.getElementsByTagName("urlfilepath").item(0).getTextContent();
        logger.info("test:url file path: " + urlFilePath);
        String jsonFilePath = document.getElementsByTagName("outputfilepath").item(0).getTextContent();
        logger.info("test:json file path: " + jsonFilePath);
        String restartFilePath = document.getElementsByTagName("restartfilepath").item(0).getTextContent();
        logger.info("test:restart file path: " + restartFilePath);

        String proxyUrl = document.getElementsByTagName("proxy_url").item(0).getTextContent();
        logger.info("test: proxy url: " + proxyUrl);

        int oneProxyUseNum = Integer.parseInt(document.getElementsByTagName("one_proxy_use_num").item(0).getTextContent());
        logger.info("test: one proxy use num: " + oneProxyUseNum);

        String driverHome = document.getElementsByTagName("driverhome").item(0).getTextContent();
        logger.info("test: driver home: " + driverHome);

        long writeWaittime = Long.parseLong(document.getElementsByTagName("write_waittime").item(0).getTextContent());
        logger.info("test: write waittime: " + writeWaittime);

        int spiderWaittime = Integer.parseInt(document.getElementsByTagName("spider_waittime").item(0).getTextContent());
        logger.info("test: spider waittime: " + spiderWaittime);

        int threadNum = Integer.parseInt(document.getElementsByTagName("thread_num").item(0).getTextContent());
        logger.info("test: thread num: " + threadNum);

        URLPipeline urlPipeline = new URLPipeline();

        DataPipeline<Map<String,Object>> dataPipeline = new DataPipeline<Map<String,Object>>();

        XmlSpider xmlSpider =
                new XmlSpider(document,driverHome,urlPipeline,dataPipeline,"null",1);
        ChromeDriver driver = xmlSpider.createChrome(driverHome);

        logger.info("test: xml spider init success");

        driver.get("http://www.bilibili.com");
        logger.info("test for get :" + driver.getCurrentUrl());
        try{
            WebElement e = driver.findElement(By.className("entry-title"));
            String u = e.getAttribute("href");
            logger.info("test to get url: " + u);
            if (u.contains("bilibili")){
                logger.info("test for get url success");
            }
        }catch (Exception e){
            logger.error("test for get url error");
        }
    }
}


class LogbackConfig {

    public static <T> Logger createLogger(Class<T> clazz) {
        // get logger context
        LoggerContext context = new LoggerContext();

        // create PatternLayoutEncoder
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%date{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();

        // create ConsoleAppender
        ConsoleAppender consoleAppender = new ConsoleAppender();
        consoleAppender.setContext(context);
        consoleAppender.setEncoder(encoder);
        consoleAppender.start();

        // create logger
        Logger logger = context.getLogger(clazz);
        logger.addAppender(consoleAppender);
        logger.setAdditive(false); // avoid root logger

        return logger;
    }
}


public class Main{
    public static void main(String[] args){
        String xmlPath = args[0];
        XmlSpiderBuilder xmlSpiderBuilder = new XmlSpiderBuilder(xmlPath);
        String method = args[1];
        if (method.equals("test")) {
            xmlSpiderBuilder.test();
        } else if (method.equals("run")) {
            xmlSpiderBuilder.run();
        }
    }
}
