package com.hfutonline.hc.crawler;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hfutonline.hc.model.Grade;
import com.hfutonline.hc.model.UserInfo;
import com.hfutonline.hc.utils.OCR;
import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hfutonline.hc.utils.OkHttpUtil.newGetRequest;
import static com.hfutonline.hc.utils.OkHttpUtil.newPostFormRequest;

/**
 * @author chenliangliang
 * @date 2019/3/18
 */
public class CrawlerExample {

    public static void main(String[] args) throws IOException {
        CrawlerExample crawlerExample = new CrawlerExample();

        Stopwatch stopwatch = Stopwatch.createStarted();

        boolean res = crawlerExample.crawler();
        long time = stopwatch.elapsed(TimeUnit.MILLISECONDS);

        System.err.println("爬取用时：" + time + " ms");

        System.err.println("爬取用时：" + (double) time / 1000 + " s");

        if (res) {
            stopwatch.reset();
            stopwatch.start();
            crawlerExample.parse();
            time = stopwatch.elapsed(TimeUnit.MILLISECONDS);

            System.err.println("解析用时：" + time + " ms");

            System.err.println("解析用时：" + (double) time / 1000 + " s");
        }
    }

    private static final String HFUT_HOST = "my.hfut.edu.cn";

    private static final String EDU_HOST = "jxglstu.hfut.edu.cn";

    private static final String CAPTCHA_URL = "http://my.hfut.edu.cn/captchaGenerate.portal";

    private static final String LOGIN_URL = "http://my.hfut.edu.cn/userPasswordValidate.portal";

    private static final String EDU_LOGIN_URL = "http://jxglstu.hfut.edu.cn/eams5-student/wiscom-sso/login";

    private static final String STUID_URL = "http://jxglstu.hfut.edu.cn/eams5-student/for-std/grade/sheet";


    private static Map<String, List<Cookie>> cookieMap = Maps.newHashMapWithExpectedSize(4);


    private CookieManager cookieManager = new CookieManager();


    private OkHttpClient client = new OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .cookieJar(cookieManager)
            .build();


    private OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .followRedirects(false)
            .followRedirects(false)
            .cookieJar(cookieManager)
            .build();


    /**
     * 爬取信息门户
     */
    public boolean crawler() throws IOException {

        String code = "";

        for (int i = 0; i < 3; i++) {
            //1.获取验证码

            Request captchaRequest = newGetRequest(getCaptchaUrl());

            Response captchaResponse = client.newCall(captchaRequest).execute();

            //2.识别验证码

            InputStream inputStream = captchaResponse.body().byteStream();

            code = OCR.getCode(inputStream);

            if (code.length() == 4 && !code.contains("?")) {
                break;
            }
        }

        System.out.println("获取验证码成功, code=" + code);


        //3.模拟提交表单

        Map<String, String> formMap = Maps.newHashMapWithExpectedSize(3);

        formMap.put("Login.Token1", UserInfo.USERNAME);
        formMap.put("Login.Token2", UserInfo.PASSWORD);
        formMap.put("captchaField", code);

        Request loginRequest = newPostFormRequest(LOGIN_URL, formMap);

        Response loginResponse = client.newCall(loginRequest).execute();

        if (!loginResponse.isSuccessful()) {
            return false;
        }

        System.out.println("模拟登录信息门户成功!");

        System.out.println("信息门户 cookie=" + cookieMap.get(HFUT_HOST));

        //4.获取cookie
        List<Cookie> cookies = cookieMap.get(HFUT_HOST);

        for (Cookie cookie : cookies) {
            if ("iPlanetDirectoryPro".equals(cookie.name())) {
                cookieMap.put(EDU_HOST, Lists.newArrayList(cookie));
                break;
            }
        }

        //5.登录新教务系统

        Request eduLoginRequest = newGetRequest(EDU_LOGIN_URL);

        Response eduLoginResponse = client.newCall(eduLoginRequest).execute();

        if (!eduLoginResponse.isSuccessful()) {
            return false;
        }

        System.out.println("新教务系统登录成功. cookie=" + cookieMap.get(EDU_HOST));

        //6.获取stuId(存储)

        Request stuIdRequest = newGetRequest(STUID_URL);

        Response stuIdResponse = okHttpClient.newCall(stuIdRequest).execute();

        if (!stuIdResponse.isRedirect()) {
            return false;
        }

        String location = stuIdResponse.header("Location");

        String stuId = location.substring(location.lastIndexOf("/") + 1);

        System.out.println("stuId=" + stuId);

        //7.查询成绩

        String gradeUrl = getGradeUrl(stuId);

        Request gradeRequest = newGetRequest(gradeUrl);

        Response gradeResponse = client.newCall(gradeRequest).execute();

        if (!gradeResponse.isSuccessful()) {
            return false;
        }

        String gradeHtml = gradeResponse.body().string();

        System.out.println("查询成绩成功! ");

        String filePath = "grade_" + UserInfo.USERNAME + ".html";

        FileUtils.write(new File(filePath), gradeHtml, StandardCharsets.UTF_8);

        //8.专业培养方案

//        String professionalTranProgramUrl = getProfessionalTranProgramUrl(stuId);
//
//        Request professionalTranProgramRequest = newGetRequest(professionalTranProgramUrl);
//
//        Response professionalTranProgramResponse = client.newCall(professionalTranProgramRequest).execute();
//
//        System.out.println(professionalTranProgramResponse.code());
//
//        String professionalTranProgramJsonString = professionalTranProgramResponse.body().string();

        //System.out.println(JSON.toJSONString(JSON.parseObject(professionalTranProgramJsonString), true));

        //9.基本信息

//        String basicStuInfo = getBasicStuInfo(stuId);
//
//        Request basicStuInfoRequest = newGetRequest(basicStuInfo);
//
//        Response basicStuInfoResponse = client.newCall(basicStuInfoRequest).execute();
//
//        System.out.println(basicStuInfoResponse.code());
//
//        String basicStuInfoHtml = basicStuInfoResponse.body().string();
//
//        String basicStuInfoHtmlPath="basicInfo_" + UserInfo.UESRNAME + ".html";
//
//        FileUtils.write(new File(basicStuInfoHtmlPath),basicStuInfoHtml,StandardCharsets.UTF_8);

        //10.课表信息

        return true;
    }


    public void parse() throws IOException {
        String filePath = "grade_" + UserInfo.USERNAME + ".html";

        String gradeHtml = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);

        Document document = Jsoup.parse(gradeHtml);

        Elements container = document.getElementsByClass("container-fluid").first().getElementsByClass("row");

        Map<String, List<Grade>> map = Maps.newLinkedHashMapWithExpectedSize(container.size());

        for (Element element : container) {

            Elements gradeElements = element.getElementsByClass("col-sm-12");

            Element gradeHeader = gradeElements.get(0);

            String header = gradeHeader.text();

            System.out.println(header);

            List<Grade> list = Lists.newLinkedList();
            map.put(header, list);

            Element gradeTable = gradeElements.get(1).getElementsByClass("student-grade-table").first();

            Element tbody = gradeTable.getElementsByTag("tbody").first();

            Elements trs = tbody.getElementsByTag("tr");

            for (Element tr : trs) {
                Elements tds = tr.getElementsByTag("td");

                Grade grade = new Grade();

                grade.setCourseName(tds.get(0).text());
                grade.setCourseCode(tds.get(1).text());
                grade.setClassCode(tds.get(2).text());
                grade.setCredit(tds.get(3).text());
                grade.setGradePoint(tds.get(4).text());
                grade.setScore(tds.get(5).text());

                String detail = tds.get(6).text()/*.replace(" ", "\n")*/;

                grade.setDetail(detail);

                System.out.println(grade);
                list.add(grade);
            }

            System.err.println("==================================================================================================================================");
        }

        //String outFilePath = "grade_" + UserInfo.USERNAME + ".xlsx";
        //toExcel(outFilePath, map);

    }


    private void toExcel(String outFilePath, Map<String, List<Grade>> map) throws IOException {

        XSSFWorkbook workbook = new XSSFWorkbook();

        XSSFCellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFillBackgroundColor(IndexedColors.RED.getIndex());

        XSSFFont font = workbook.createFont();
        font.setColor(IndexedColors.BLACK.getIndex());
        font.setBold(true);

        cellStyle.setFont(font);

        XSSFSheet sheet = workbook.createSheet("成绩单");


        int rowIndex = 0;

        for (Map.Entry<String, List<Grade>> entry : map.entrySet()) {

            String key = entry.getKey();
            List<Grade> grades = entry.getValue();

            XSSFRow row = sheet.createRow(rowIndex++);

            XSSFCell cell = row.createCell(0, CellType.STRING);
            cell.setCellStyle(cellStyle);
            cell.setCellValue(key);

            XSSFRow headerRow = sheet.createRow(rowIndex++);

            XSSFCell courseNameCell = headerRow.createCell(0, CellType.STRING);
            courseNameCell.setCellValue("课程名称");

            XSSFCell courseCodeCell = headerRow.createCell(1, CellType.STRING);
            courseCodeCell.setCellValue("课程代码");

            XSSFCell classCodeCell = headerRow.createCell(2, CellType.STRING);
            classCodeCell.setCellValue("教学班代码");

            XSSFCell creditCell = headerRow.createCell(3, CellType.STRING);
            creditCell.setCellValue("学分");

            XSSFCell gradePointCell = headerRow.createCell(4, CellType.STRING);
            gradePointCell.setCellValue("绩点");

            XSSFCell scoreCell = headerRow.createCell(5, CellType.STRING);
            scoreCell.setCellValue("成绩");

            XSSFCell detailCell = headerRow.createCell(6, CellType.STRING);
            detailCell.setCellValue("成绩明细");

            for (Grade grade : grades) {

                XSSFRow dataRow = sheet.createRow(rowIndex++);


                courseNameCell = dataRow.createCell(0, CellType.STRING);
                courseNameCell.setCellValue(grade.getCourseName());

                courseCodeCell = dataRow.createCell(1, CellType.STRING);
                courseCodeCell.setCellValue(grade.getCourseCode());

                classCodeCell = dataRow.createCell(2, CellType.STRING);
                classCodeCell.setCellValue(grade.getClassCode());

                creditCell = dataRow.createCell(3, CellType.STRING);
                creditCell.setCellValue(grade.getCredit());

                gradePointCell = dataRow.createCell(4, CellType.STRING);
                gradePointCell.setCellValue(grade.getGradePoint());

                scoreCell = dataRow.createCell(5, CellType.STRING);
                scoreCell.setCellValue(grade.getScore());

                detailCell = dataRow.createCell(6, CellType.STRING);
                detailCell.setCellValue(grade.getDetail());


            }

            XSSFRow emptyRow = sheet.createRow(rowIndex++);

        }


        workbook.write(new FileOutputStream(outFilePath));

    }


    private String getCaptchaUrl() {
        return CAPTCHA_URL + "?s=" + Math.random();
    }

    private String getProfessionalTranProgramUrl(String stuId) {
        return "http://jxglstu.hfut.edu.cn/eams5-student/for-std/program/root-module-json/" + stuId;
    }

    private String getBasicStuInfo(String stuId) {
        return "http://jxglstu.hfut.edu.cn/eams5-student/for-std/student-info/info/" + stuId;
    }

    private String getGradeUrl(String stuId) {
        return "http://jxglstu.hfut.edu.cn/eams5-student/for-std/grade/sheet/info/" + stuId + "?semester=";
    }


    private static class CookieManager implements CookieJar {
        @Override
        public void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
            String host = httpUrl.host();
//            Set<Cookie> cookies = cookieMap.get(host);
//            if (cookies == null) {
//                cookieMap.put(host,  Sets.newHashSet(list));
//            } else {
//                cookies.addAll(list);
//            }
            cookieMap.put(host, list);
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl httpUrl) {
            String host = httpUrl.host();
            List<Cookie> cookies = cookieMap.get(host);
            if (cookies == null) {
                return Collections.emptyList();
            }
            return cookies;
        }
    }

}
