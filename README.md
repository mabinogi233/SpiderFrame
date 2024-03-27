# SpiderFrame: crawl web page by xml without coding 

https://github.com/mabinogi233/SpiderFrame
___

## · what is it

SpiderFrame is a tool to help you to use selenium to crawl web page without coding,
You **just need to configure the xml file** to use it to scrape web data from url txt file to data json file.

## · refer

1. selenium: https://www.selenium.dev/
2. UndetectedChromedriver for Java: https://github.com/mabinogi233/UndetectedChromedriver

---

## · prepare to use

1. install java, version >= 11 (must Java 11 when use jar file, because I use java 11 to compile it
   
2. install Chrome version >= 108 

3. download chromeDriver of suitable version for your Chrome from https://chromedriver.chromium.org/downloads or other

4. for Linux need run ```chmod +x chromedriverPath``` in terminal to make chromedriver executable

5. download SpiderFrame.jar if you want to use it directly. If you want to use by source code, you can pass this step.

---

## · how to use

1, if you want use source code, you can clone this project and use it in your project

2, if you want use jar file, you can download SpiderFrame.jar

3, configure the xml file, you can refer to the example.xml in the project

```xml
<spider>
    <!-- spidername: spider name -->
    <spidername>spider1</spidername>
    <!-- urlfilepath: the file path of the url list to be crawled -->
    <urlfilepath>/Users/lwz/Downloads/data_11112222.txt</urlfilepath>
    <!-- outputfilepath: the file path of the output data saved -->
    <outputfilepath>/Users/lwz/Downloads/data_11112222.json</outputfilepath>
    <!-- restartfilepath: the file path of the restart data saved,
    this file will be created automatically and used to restart the spider -->
    <restartfilepath>/Users/lwz/Downloads/data_11112222_log.txt</restartfilepath>
    <!-- driverhome: the file path of the chromedriver -->
    <driverhome>/Users/lwz/Documents/software/chromedriver/chromedriver-2</driverhome>
    <!-- proxy_url: the url of the proxy server, response must be a string like ip:port -->
    <proxy_url>null</proxy_url>
    <!-- one_proxy_use_num: the number of one proxy used for url number -->
    <one_proxy_use_num>5</one_proxy_use_num>
    <!-- write_waittime: the wait time of writing data to the output file between two writes -->
    <write_waittime>60000</write_waittime>
    <!-- spider_waittime: the wait time of the spider between two urls -->
    <spider_waittime>8000</spider_waittime>
    <!-- thread_num: the number of threads to crawl the urls -->
    <thread_num>3</thread_num>
    <!-- data: the data to be crawled -->
    <data>
        <!-- elements: the elements of the data, elements must has one element tag at least,
        it could has elements tag in elements tag -->
        <elements>
            <!-- name: the name of the elements -->
            <name>one_info</name>
            <!-- type: the type to locate the elements, support css\xpath\id\class\name,
            css: use css selector to locate the elements
            xpath: use xpath to locate the elements
            id: use id to locate the elements
            class: use css class name to locate the elements
            name: use name to locate the elements -->
            <type>css</type>
            <!-- parse: the location of the elements, it must be suitable for the type -->
            <parse>div.result.c-container.xpath-log.new-pmd</parse>
            <!-- element: the element for one metadata -->
            <element>
                <!-- name: the name of the element -->
                <name>title</name>
                <!-- type: the type to locate the element, support css\xpath\id\class\name,
                css: use css selector to locate the element
                xpath: use xpath to locate the element
                id: use id to locate the element
                class: use css class name to locate the element
                name: use name to locate the element -->
                <type>css</type>
                <!-- parse: the location of the element, it must be suitable for the type,
                ** this parse must be a relative path relative to the parent node parse ** -->
                <parse>div.c-container > div:nth-child(1) > h3 > a</parse>
                <!--> only support text or attribute <-->
                <!-- text means the content text of the element -->
                <text>
                    <!-- only support delete or find -->
                    <!-- delete: delete the regex matched content -->
                    <!-- find: find the regex matched content -->
                    <type>delete</type>
                    <!-- regex: the regex to match the content -->
                    <regex>&lt;/?em&gt;</regex>
                </text>
            </element>
            <element>
                <name>url</name>
                <type>css</type>
                <parse>div.c-container > div:nth-child(1) > h3 > a</parse>
                <!-- attribute means the attribute of the element -->
                <attribute>
                    <!-- the attribute name to get the attribute value -->
                    <type>href</type>
                </attribute>
            </element>
        </elements>
    </data>
</spider>
```

4, test to run the jar file in terminal

```shell
java -jar /www/wwwroot/testspider/SpiderFrame.jar "xml file absolute path" test
```

5, run the jar file in terminal

```shell
java -jar /www/wwwroot/testspider/SpiderFrame.jar "xml file absolute path" run
```

