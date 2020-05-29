package com.khh.esm.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khh.esm.model.MindCare;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
    @Async
    @Scheduled(cron = "*/5 * * * * *")
    public void monitor() throws Throwable {
        log.debug("-==");

//http://119.206.205.181:9200/mindcare_*care/_search?q=log_level:ERROR&sort=@timestamp:desc&pretty=true

//        SearchRequest searchRequest = new SearchRequest("mindcare_*care");
        SearchRequest searchRequest = new SearchRequest("mindcare*");
//        searchRequest.indicesOptions(IndicesOptions.lenientExpandOpen());
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(500);
        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        searchSourceBuilder.sort(new FieldSortBuilder("@timestamp").order(SortOrder.DESC));
//        searchSourceBuilder.query(QueryBuilders.termQuery("log_level", "ERROR"));
        searchSourceBuilder.query(QueryBuilders.matchQuery("log_level", "ERROR"));
        searchSourceBuilder.query(QueryBuilders.rangeQuery("@timestamp").gte("now-5s").lt("now"));
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
        for (SearchHit hit : searchHits) {
            String index = hit.getIndex();
            String id = hit.getId();
            float score = hit.getScore();
            String sourceAsString = hit.getSourceAsString();
//            log.info("-->"+sourceAsString);
            MindCare care = objectMapper.readValue(sourceAsString, MindCare.class);
            care.set_id(id);
            care.set_index(hit.getIndex());
            care.set_type(hit.getType());

            datas.add(care);
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
        datas = datas.stream().filter(it -> null == it.getException_class() || !it.getException_class().contains("org.apache.catalina.connector.ClientAbortException")).collect(Collectors.toList());

        log.info("==========>{}", datas);

        if (datas.size() > 0) {
//            SimpleMailMessage message = new SimpleMailMessage();
//            message.setTo("khh@omnicns.com");
//            message.setSubject("gg");
//            message.setText("czczczc" + datas.size());
            helper.setTo(new String[]{"serviceteam@omnicns.com","hirakian@omnicns.com"});
//            helper.setTo(new String[]{"khh@omnicns.com"});

            String title = String.format("%s %s", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()), "mindcare 5초간격 ERROR 발생 (" + datas.size() + "건)");
            message.setSubject(title);


            StringBuffer content = new StringBuffer();
            String table = "border:1px solid black; boarder-spacing: 0px 0px; border-collapse: collapse;";
            String th = "border:1px solid black; boarder-spacing: 0px 0px; border-collapse: collapse; background-color:#c3c3c3; padding:15px;";
            String td = "border:1px solid black; boarder-spacing: 0px 0px; border-collapse: collapse; padding:15px;text-align: center;";
//            content.append(String.format("%s %s<br/>", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()), title));
            content.append(title+"<br/>");
            content.append("자세한 내용은 아래 상세 내역을 참고해주세요.<br/><br/><br/>");
            //content.append(String.format("* 참여기간: %s ~ %s <br/>", ptcpStDt.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")), ptcpEndDt.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))));
            content.append("* 상세 내역<br/>");


            content.append(String.format("<table style='%s'>", table));
            content.append("   <thead>");
            content.append("       <tr>");
            content.append(String.format("<th style='%s'>발생시간</th>", th));
            content.append(String.format("<th style='%s'>시스템</th>", th));
            content.append(String.format("<th style='%s'>타입</th>", th));
            content.append(String.format("<th style='%s'>메시지</th>", th));
            content.append(String.format("<th style='%s'>url_path</th>", th));
            content.append(String.format("<th style='%s'>exception_class</th>", th));
            content.append(String.format("<th style='%s'>full message</th>", th));
            content.append("       </tr>");
            content.append("   </thead>");
            content.append("   <tbody>");


            for (MindCare data : datas) {
                content.append("       <tr>");
                // 발생시간
                content.append(String.format("<td style='%s'>", td) + data.getDate() + "</td>");
                // 시스템
                content.append(String.format("<td style='%s'>", td) + (null==data.getService_name()?data.get_index():data.getService_name())  + "</td>");
                // 타입
                content.append(String.format("<td style='%s'>", td) + (null==data.getService_type()?data.get_type():data.getService_type()) + "</td>");
                // 메시지
                content.append(String.format("<td style='%s'>", td) + data.getMsg() + "</td>");
                // url_path
                content.append(String.format("<td style='%s'>", td) + data.getUrl_path() + "</td>");
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
