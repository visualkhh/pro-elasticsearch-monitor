//package com.khh.esm.service;
//
//import com.khh.esm.model.CaptureConfig;
//import org.openqa.selenium.*;
//import org.openqa.selenium.phantomjs.PhantomJSDriver;
//import org.openqa.selenium.remote.DesiredCapabilities;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.stereotype.Service;
//import javax.imageio.ImageIO;
//import java.awt.image.BufferedImage;
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//
////https://medium.com/chequer/springboot-phantomjs%EB%A5%BC-%EC%9D%B4%EC%9A%A9%ED%95%B4-%EB%B0%94%EC%BD%94%EB%93%9C-qr-%EC%BD%94%EB%93%9C%EA%B0%80-%ED%8F%AC%ED%95%A8%EB%90%9C-%EB%8F%99%EC%A0%81-%EC%9D%B4%EB%AF%B8%EC%A7%80-%EC%BF%A0%ED%8F%B0-%EB%A7%8C%EB%93%A4%EC%96%B4%EB%B3%B4%EA%B8%B0-17fc00dd83fb
//@Service
//public class GhostDriver {
//    private static DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
//
//    static {
//        desiredCapabilities.setJavascriptEnabled(true);
//        desiredCapabilities.setCapability("takesScreenshot", true);
//    }
//
//    public byte[] getScreenshotAsBytes(String url, CaptureConfig captureConfig) {
//        WebDriver webDriver = new PhantomJSDriver(desiredCapabilities);
//        webDriver.get(url);
//        if (captureConfig.getX() > 0 || captureConfig.getY() > 0) {
//            webDriver.manage().window().setPosition(new Point(captureConfig.getX(), captureConfig.getY()));
//        }
//        if (captureConfig.getWidth() > 0 && captureConfig.getHeight() > 0) {
//            webDriver.manage().window().setSize(new Dimension(captureConfig.getWidth(), captureConfig.getHeight()));
//        }
//        try {
////            String cssFontRule = IOUtils.readFully(new ClassPathResource("/phantomjs/cssFontRule.js").getInputStream());
////            String cssFontRule = "" +
////                    "var page = require('webpage').create();\n" +
////                    "page.open('http://119.206.205.181:5601/app/kibana#/discover?_g=(filters:!(),refreshInterval:(pause:!t,value:0),time:(from:now-60m,to:now))&_a=(columns:!(_source),filters:!(),index:b73a3e60-a173-11ea-8461-dd4573115998,interval:auto,query:(language:kuery,query:''),sort:!())', function() {\n" +
////                    "\t  window.setTimeout(function() {\n" +
////                    "\t\t  page.render('github.pdf');\n" +
////                    "\t\t  phantom.exit();\n" +
////                    "\t  }, 4000);\n" +
////                    "});" +
////                    "";
////            ((JavascriptExecutor) webDriver).executeScript(cssFontRule);
////            Thread.sleep(1000);
//        } catch (Exception e) {
//            // ignored
//        }
//        byte[] bytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
//        webDriver.quit();
//        return bytes;
//    }
//
//    public BufferedImage getScreenshotAsBufferedImage(String url, CaptureConfig captureConfig) {
//        byte[] bytes = getScreenshotAsBytes(url, captureConfig);
//        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
//        try {
//            return ImageIO.read(byteArrayInputStream);
//        } catch (IOException e) {
//            // ignored
//        }
//        return null;
//    }
//
//    public BufferedImage getScreenshotAsBufferedImage(String url) {
//        return getScreenshotAsBufferedImage(url, CaptureConfig.defaultConfig());
//    }
//}
////https://github.com/detro/ghostdriver 를 참고해서 간단하게 만들었습니다. 중간에 cssFontRule.js를 웹드라이버에 삽입하는데, 해당 js는 웹폰트를 사용할 경우 폰트 적용이 제대로 되지않는 이슈를 해결해줍니다. 즉 웹폰트를 PhantomJS가 잘 해석해서 렌더링하는데 도움을 주는 트릭 스크립트입니다.
////        테스트 코드를 통해 http://www.kakao.com 페이지를 이미지로 저장해보겠습니다.
