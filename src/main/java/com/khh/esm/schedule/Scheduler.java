package com.khh.esm.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khh.esm.model.MindCare;
import com.khh.esm.model.Omnifit2;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;

import javax.mail.internet.MimeMessage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class Scheduler {


    @Autowired
    JavaMailSender javaMailSender;

//    @Autowired
//    MindCareRepository mindCareRepository;

    @Qualifier("elasticsearchClient")
    @Autowired
    RestHighLevelClient restHighLevelClient;
//    SpringTemplateEngine

    @Autowired
    private TemplateEngine templateEngine;
    //    JavaM
    //	https://postitforhooney.tistory.com/entry/SpringScheduled-Cron-Example-%ED%91%9C%ED%98%84%EC%8B%9D
//
//	초    |   분   |   시   |   일   |   월   |  요일  |   연도
//	0~59 |   0~59 |   0~23|   1~31 | 1~12  |  0~6  | 생략가능
//											(Sunday=0 or 7)
//	이러면 30초마다 실행되는 것이다.
//	@Scheduled(cron="*/30 * * * * *")
//     매월요일 1시 10분 10초
//	@Scheduled(cron = "1 * * * * *")
//	@Scheduled(cron = "*/10 * * * * *")
//  @Scheduled(cron = "10 10 1 * * 1")



//    @Scheduled(cron = "*/5 * * * * *")
//    public void gg() throws Throwable {
//        Context context = new Context();
//        context.setVariable("message", "ffff");
//        String i = templateEngine.process("mail-template", context);
//        log.debug("-=="+i);
//    }







    Date omnifit2Last = new Date();
    @Scheduled(cron = "*/5 * * * * *")
    public void omnifit2Monitor() throws Throwable {
        log.debug("-==");
        SearchRequest searchRequest = new SearchRequest("omnifit2");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(100);
        searchSourceBuilder.sort(new FieldSortBuilder("@timestamp").order(SortOrder.DESC));


        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        BoolQueryBuilder shouldQueryBuilder = QueryBuilders.boolQuery().should(QueryBuilders.matchQuery("log_level", "ERROR"));
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        boolQueryBuilder.filter(shouldQueryBuilder).filter(QueryBuilders.rangeQuery("@timestamp").gt(sdf.format(omnifit2Last)).timeZone("Asia/Seoul"));

        searchSourceBuilder.query(boolQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        List<Omnifit2> datas = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);


        for (int i = 0; i < searchHits.length; i++) {
            SearchHit hit = searchHits[i];
            String index = hit.getIndex();
            String id = hit.getId();
            float score = hit.getScore();
            String sourceAsString = hit.getSourceAsString();
            Omnifit2 care = objectMapper.readValue(sourceAsString, Omnifit2.class);
            care.set_id(id);
            care.set_index(hit.getIndex());
            care.set_type(hit.getType());


            if (omnifit2Last.getTime() < care.getTimestamp().getTime()) {
                datas.add(care);
            }
        }


        datas.forEach(it -> {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            log.debug(it.get_id() + "{} {}-> {}", format.format(it.getTimestamp()), it.getException_class(), it);
        });

//        datas = datas.stream().filter(it->!it.getStacktrace().contains("java.io.IOException: Broken pipe")).collect(Collectors.toList());
        datas = datas.stream()
                .filter(it -> !"/api/user".equals(it.getUrl_path()) && null!=it.getMsg() && !it.getMsg().contains("serialNo"))
                .filter(it -> null!=it.getMsg() && !it.getMsg().contains("Could not parse 'Accept' header"))
//                .filter(it-> !"org.springframework.security.access.AccessDeniedException".equals((it.getException_class())))
//                .filter(it -> !"M2007".equals(it.getCode()))
//                .filter(it -> !"M2006".equals(it.getCode()))
//                .filter(it -> !"c.k.o.o.s.s.CustomAuthenticationProvider".equals(it.getJava_class()))
                .collect(Collectors.toList());

        if (datas.size() > 0) {
            omnifit2Last = datas.get(0).getTimestamp();
        }
        log.info("==========>{}", datas);

        if (datas.size() > 0) {
            helper.setTo(new String[]{"serviceteam@omnicns.com"});
//            helper.setTo(new String[]{"khh@omnicns.com"});

            String title = String.format("%s %s", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()), "omnifit2 5초간격 ERROR 발생 (" + datas.size() + "건)");
            message.setSubject(title);


            StringBuffer content = new StringBuffer();
            String table = "border:1px solid black; boarder-spacing: 0px 0px; border-collapse: collapse;";
            String th = "border:1px solid black; boarder-spacing: 0px 0px; border-collapse: collapse; background-color:#c3c3c3; padding:15px;";
            String td = "border:1px solid black; boarder-spacing: 0px 0px; border-collapse: collapse; padding:15px;text-align: center;";
//            content.append(String.format("%s %s<br/>", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()), title));
            content.append(title + "<br/>");
            content.append("자세한 내용은 아래 상세 내역을 참고해주세요.<br/><br/><br/>");
            //content.append(String.format("* 참여기간: %s ~ %s <br/>", ptcpStDt.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")), ptcpEndDt.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))));
            content.append("* 상세 내역<br/>");


            content.append(String.format("<table style='%s'>", table));
            content.append("   <thead>");
            content.append("       <tr>");
            content.append(String.format("<th style='%s'>발생시간</th>", th));
            content.append(String.format("<th style='%s'>host</th>", th));
            content.append(String.format("<th style='%s'>시스템</th>", th));
            content.append(String.format("<th style='%s'>타입</th>", th));
            content.append(String.format("<th style='%s'>메시지</th>", th));
            content.append(String.format("<th style='%s'>코드</th>", th));
            content.append(String.format("<th style='%s'>url_path</th>", th));
            content.append(String.format("<th style='%s'>java_class</th>", th));
            content.append(String.format("<th style='%s'>exception_class</th>", th));
            content.append(String.format("<th style='%s'>full message</th>", th));
            content.append("       </tr>");
            content.append("   </thead>");
            content.append("   <tbody>");


            for (Omnifit2 data : datas) {
                content.append("       <tr>");
                // 발생시간
                content.append(String.format("<td style='%s'>", td) + data.getDate() + "</td>");

                content.append(String.format("<td style='%s'>", td) + data.getHost() + "</td>");
                // 시스템
                content.append(String.format("<td style='%s'>", td) + (null == data.getService_name() ? data.get_index() : data.getService_name()) + "</td>");
                // 타입
                content.append(String.format("<td style='%s'>", td) + (null == data.getService_type() ? data.get_type() : data.getService_type()) + "</td>");
                // 메시지
                content.append(String.format("<td style='%s'>", td) + data.getMsg() + "</td>");
                // code
                content.append(String.format("<td style='%s'>", td) + data.getCode() + "</td>");
                // url_path
                content.append(String.format("<td style='%s'>", td) + data.getUrl_path() + "</td>");
                // java_class
                content.append(String.format("<td style='%s'>", td) + data.getJava_class()  + "</td>");
                // exception_class
                content.append(String.format("<td style='%s'>", td) + data.getException_class() + "</td>");
                // stacktrace
                content.append(String.format("<td style='%s'><input type='text'  value='" + data.getMessage().replaceAll("\"", "'") + "'>", td));
                content.append("       </tr>");
            }
            content.append("   </tbody>");
            content.append("</table>");
            content.append("<br/>");
            content.append("<br/>");

            helper.setText(content.toString(), true);
            javaMailSender.send(message);
        }
    }


    Date mindcareLast = new Date();
    @Scheduled(cron = "*/5 * * * * *")
    public void mindcareMonitor() throws Throwable {
        log.debug("-==");


//        BufferedImage capturedImage = ghostDriver.getScreenshotAsBufferedImage("http://119.206.205.181:5601/app/kibana#/discover?_g=(filters:!(),refreshInterval:(pause:!t,value:0),time:(from:now-60m,to:now))&_a=(columns:!(_source),filters:!(),index:b73a3e60-a173-11ea-8461-dd4573115998,interval:auto,query:(language:kuery,query:''),sort:!())");
//        BufferedImage capturedImage = ghostDriver.getScreenshotAsBufferedImage("http://119.206.205.181:5601/app/kibana#/discover?_g%3D(filters%3A!()%2CrefreshInterval%3A(pause%3A!t%2Cvalue%3A0)%2Ctime%3A(from%3Anow-60m%2Cto%3Anow))%26_a%3D(columns%3A!(_source)%2Cfilters%3A!()%2Cindex%3Ab73a3e60-a173-11ea-8461-dd4573115998%2Cinterval%3Aauto%2Cquery%3A(language%3Akuery%2Cquery%3A'')%2Csort%3A!())");
//        BufferedImage capturedImage = ghostDriver.getScreenshotAsBufferedImage("http://google.com");
//        ImageIO.write(capturedImage, "png", new File("kakao.png"));
        //http://119.206.205.181:9200/mindcare_*care/_searÒch?q=log_level:ERROR&sort=@timestamp:desc&pretty=true

//        SearchRequest searchRequest = new SearchRequest("mindcare_*care");
        SearchRequest searchRequest = new SearchRequest("mindcare*");
//        searchRequest.indicesOptions(IndicesOptions.lenientExpandOpen());
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(100);
//        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        searchSourceBuilder.sort(new FieldSortBuilder("@timestamp").order(SortOrder.DESC));
//        searchSourceBuilder.query(QueryBuilders.termQuery("log_level", "ERROR"));


        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        BoolQueryBuilder shouldQueryBuilder = QueryBuilders.boolQuery().should(QueryBuilders.matchQuery("log_level", "ERROR"));
//        boolQueryBuilder.filter(shouldQueryBuilder).filter(QueryBuilders.rangeQuery("@timestamp").gte("now-6s").lt("now"));
//        boolQueryBuilder.filter(shouldQueryBuilder).filter(QueryBuilders.rangeQuery("@timestamp").gte("now-60m").lt("now").timeZone("Asia/Seoul"));
//        https://stackoverflow.com/questions/20238280/date-in-to-utc-format-java
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
//        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));   // This line converts the given date into UTC time zone
//        final java.util.Date dateObj = sdf.parse("2013-10-22T01:37:56");
//        boolQueryBuilder.filter(shouldQueryBuilder).filter(QueryBuilders.rangeQuery("@timestamp").gte(sdf.format(last)).timeZone("Asia/Seoul"));
        boolQueryBuilder.filter(shouldQueryBuilder).filter(QueryBuilders.rangeQuery("@timestamp").gt(sdf.format(mindcareLast)).timeZone("Asia/Seoul"));

        searchSourceBuilder.query(boolQueryBuilder);
//        QueryBuilders.boolQuery().filter(matchQueryBuilder)
//        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("log_level", "ERROR");
//        matchQueryBuilder.
//        searchSourceBuilder.query(matchQueryBuilder);
//        searchSourceBuilder.query(QueryBuilders.matchQuery("_id", "UOwFZHIBrXAsAi7m_V_T"));
//        searchSourceBuilder.query(QueryBuilders.rangeQuery("@timestamp").gte("now-6s").lt("now"));
//        searchSourceBuilder.query(QueryBuilders.rangeQuery("@timestamp").gte("now-145m").lt("now"));
//        searchSourceBuilder.query(QueryBuilders.rangeQuery("@timestamp").gte("now-60m").lt("now").timeZone("Asia/Seoul"));
//        searchSourceBuilder.query(QueryBuilders.rangeQuery("@timestamp").gte("now/d").lt("now").timeZone("Asia/Seoul"));
//        searchSourceBuilder.query(QueryBuilders.rangeQuery("@timestamp").gte("now/d").lt("now").timeZone("+09:00"));
//        searchSourceBuilder.query(QueryBuilders.rangeQuery("date").gte("2020-05-29T00:00:00").lt("now").timeZone("Asia/Seoul"));
//        searchSourceBuilder.query(QueryBuilders.rangeQuery("@timestamp").gte("2020-05-28T15:00:00.000Z").lt("2020-05-29T14:59:59.999Z"));//.timeZone("Asia/Seoul"));
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        List<MindCare> datas = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);


        for (int i = 0; i < searchHits.length; i++) {
            SearchHit hit = searchHits[i];
            String index = hit.getIndex();
            String id = hit.getId();
            float score = hit.getScore();
            String sourceAsString = hit.getSourceAsString();
//            log.info("-->"+sourceAsString);
            MindCare care = objectMapper.readValue(sourceAsString, MindCare.class);
            care.set_id(id);
            care.set_index(hit.getIndex());
            care.set_type(hit.getType());


            if (mindcareLast.getTime() < care.getTimestamp().getTime()) {
                datas.add(care);
            }
//            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
//            String documentTitle = (String) sourceAsMap.get("title");
//            List<Object> users = (List<Object>) sourceAsMap.get("user");
//            Map<String, Object> innerObject = (Map<String, Object>) sourceAsMap.get("innerObject");
        }


        datas.forEach(it -> {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            log.debug(it.get_id() + "{} {}-> {}", format.format(it.getTimestamp()), it.getException_class(), it);
        });

//        datas = datas.stream().filter(it->!it.getStacktrace().contains("java.io.IOException: Broken pipe")).collect(Collectors.toList());
        datas = datas.stream()
                .filter(it -> !"org.apache.catalina.connector.ClientAbortException".equals(it.getException_class()))
                .filter(it-> !"org.springframework.security.access.AccessDeniedException".equals((it.getException_class())))
                .filter(it-> null!=it.getMessage() && !it.getMessage().contains("java.lang.String cannot be cast to com.ko.omnicns.omnifit.login.vo.LoginVO"))
                .filter(it-> !"M1001".equals(it.getCode()) && !"/api/AI201".equals(it.getUrl_path()))
                .filter(it-> !"M1013".equals(it.getCode()) && !"/api/AI025".equals(it.getUrl_path())) //개인화엡 validation 오류

                //PW가 일치하지 않을 때
                //존재하지 않는 아이디
                .filter(it -> !"M2007".equals(it.getCode()))
                .filter(it -> !"M2006".equals(it.getCode()))
//                .filter(it -> !"M2001".equals(it.getCode()))
                .filter(it -> !"c.k.o.o.s.s.CustomAuthenticationProvider".equals(it.getJava_class()))
                .collect(Collectors.toList());

        if (datas.size() > 0) {
            mindcareLast = datas.get(0).getTimestamp();
        }
        log.info("==========>{}", datas);

        if (datas.size() > 0) {
//            SimpleMailMessage message = new SimpleMailMessage();
//            message.setTo("khh@omnicns.com");
//            message.setSubject("gg");
//            message.setText("czczczc" + datas.size());
            helper.setTo(new String[]{"serviceteam@omnicns.com"});
//            helper.setTo(new String[]{"khh@omnicns.com"});

            String title = String.format("%s %s", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()), "mindcare 5초간격 ERROR 발생 (" + datas.size() + "건)");
            message.setSubject(title);


            StringBuffer content = new StringBuffer();
            String table = "border:1px solid black; boarder-spacing: 0px 0px; border-collapse: collapse;";
            String th = "border:1px solid black; boarder-spacing: 0px 0px; border-collapse: collapse; background-color:#c3c3c3; padding:15px;";
            String td = "border:1px solid black; boarder-spacing: 0px 0px; border-collapse: collapse; padding:15px;text-align: center;";
//            content.append(String.format("%s %s<br/>", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()), title));
            content.append(title + "<br/>");
            content.append("자세한 내용은 아래 상세 내역을 참고해주세요.<br/><br/><br/>");
            //content.append(String.format("* 참여기간: %s ~ %s <br/>", ptcpStDt.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")), ptcpEndDt.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))));
            content.append("* 상세 내역<br/>");


            content.append(String.format("<table style='%s'>", table));
            content.append("   <thead>");
            content.append("       <tr>");
            content.append(String.format("<th style='%s'>발생시간</th>", th));
            content.append(String.format("<th style='%s'>host</th>", th));
            content.append(String.format("<th style='%s'>시스템</th>", th));
            content.append(String.format("<th style='%s'>타입</th>", th));
            content.append(String.format("<th style='%s'>메시지</th>", th));
            content.append(String.format("<th style='%s'>코드</th>", th));
            content.append(String.format("<th style='%s'>url_path</th>", th));
            content.append(String.format("<th style='%s'>java_class</th>", th));
            content.append(String.format("<th style='%s'>exception_class</th>", th));
            content.append(String.format("<th style='%s'>full message</th>", th));
            content.append("       </tr>");
            content.append("   </thead>");
            content.append("   <tbody>");


            for (MindCare data : datas) {
                content.append("       <tr>");
                // 발생시간
                content.append(String.format("<td style='%s'>", td) + data.getDate() + "</td>");

                content.append(String.format("<td style='%s'>", td) + data.getHost() + "</td>");
                // 시스템
                content.append(String.format("<td style='%s'>", td) + (null == data.getService_name() ? data.get_index() : data.getService_name()) + "</td>");
                // 타입
                content.append(String.format("<td style='%s'>", td) + (null == data.getService_type() ? data.get_type() : data.getService_type()) + "</td>");
                // 메시지
                content.append(String.format("<td style='%s'>", td) + data.getMsg() + "</td>");
                // code
                content.append(String.format("<td style='%s'>", td) + data.getCode() + "</td>");
                // url_path
                content.append(String.format("<td style='%s'>", td) + data.getUrl_path() + "</td>");
                // java_class
                content.append(String.format("<td style='%s'>", td) + data.getJava_class()  + "</td>");
                // exception_class
                content.append(String.format("<td style='%s'>", td) + data.getException_class() + "</td>");
                // stacktrace
                content.append(String.format("<td style='%s'><input type='text'  value='" + data.getMessage().replaceAll("\"", "'") + "'>", td));
                content.append("       </tr>");
            }
            content.append("   </tbody>");
            content.append("</table>");
            content.append("<br/>");
            content.append("<br/>");

            helper.setText(content.toString(), true);
//            helper.addA
            javaMailSender.send(message);
        }
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo(mailDto.getAddress());
//        message.setFrom(MailService.FROM_ADDRESS);
//        message.setSubject(mailDto.getTitle());
//        message.setText(mailDto.getMessage());
//
//        mailSender.send(message);
//        log.debug("--{}", datas);
////        List<MindCare> data = null;
//        List<MindCare> data = mindCareRepository.findByLog_level("ERROR");
//        log.debug("data:{}", data);


//        httpService.monitor();

        // 이 라인이 실행되도 아무런 동작을 하지 않는다.
//        WebClient webClient = builder.build();
//        Mono<Search> hiMono = webClient.get().uri("http://119.206.205.181:9200/mindcare_*care/_search?q=log_level:ERROR")
//                .retrieve()
//                .bodyToMono(Search.class);
//        // subscribe를 해줘야 스트리밍이 일어난다.
//        hiMono.subscribe(hiResult -> {
//            log.info("/hi API Result : " + hiResult);
//        });
    }

}
