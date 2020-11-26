package pers.pandora.utils;

import javassist.*;
import pers.pandora.servlet.Dispatcher;
import pers.pandora.servlet.Request;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析jsp页面
 */
public final class JspParser {
    private String hashEncode;
    private static final String JSP_PACKAGE = "jsp";
    private static final String CLASSDIR = "bin";
    private static final char PATH_SPLITER = '/';
    private static final char PACKAGE_SPLITER = '.';
    public static final String CLASS_NAME_SPLITER = "_";
    private static final String SALT = "PANDORA_SERVER_SECRET_KEY";
    private static final String SERVLET_CLASS = "pers.pandora.servlet.Servlet";
    private static final String CLASS_POS_MARK = "class";

    public JspParser() {
    }

    public String getHashEncode() {
        return StringUtils.isNotEmpty(hashEncode) ? hashEncode : CodeUtils.DEFAULTENCODE;
    }

    public void setHashEncode(String hashEncode) {
        this.hashEncode = hashEncode;
    }

    //懒加载，不访问就不生成
    public String parse(String jspFile) {
        File file = new File(jspFile);
        if (!file.exists()) {
            return null;
        }
        Reader inFifle = null;
        try {
            inFifle = new FileReader(file);
            BufferedReader inputStream = new BufferedReader(inFifle);
            StringBuilder buf = new StringBuilder();
            String temp = null;
            while ((temp = inputStream.readLine()) != null) {
                buf.append(temp.trim());
            }
            inputStream.close();
            inFifle.close();
            String jsp = buf.toString();
            //重复类不再次生成
            String query = CodeUtils.hashEncode(jsp, SALT, null);
            int urlIndex = jspFile.lastIndexOf(PATH_SPLITER);
            String servletName = jspFile.substring(urlIndex + 1, jspFile.lastIndexOf(PACKAGE_SPLITER)).trim();
            String className = servletName.substring(0, 1).toUpperCase() + servletName.substring(1);
            className = className + CLASS_NAME_SPLITER + query;
            file = new File(CLASSDIR + PATH_SPLITER + JSP_PACKAGE + PATH_SPLITER + className + PACKAGE_SPLITER + CLASS_POS_MARK);
            className = JSP_PACKAGE + PACKAGE_SPLITER + className;
            if (file.exists()) {
                return className;
            }
            StringBuffer sb = new StringBuffer();
            if (jsp.contains("langage=\"java\"")) {
                jsp = jsp.replace("langage=\"java\"", "").trim();
            }
            sb.append(jsp.substring(jsp.indexOf("<jsp") + 6, jsp.indexOf("</jsp>")).trim());
            //target类生成
            ClassPool pool = ClassPool.getDefault();
            CtClass ct = pool.makeClass(className);
            ct.setInterfaces(new CtClass[]{pool.get(SERVLET_CLASS)});
            /**
             * 第一步对<% java %>代码解析
             */
            Pattern pattern = Pattern.compile("<%.*?%>");
            StringBuffer sbuf = new StringBuffer();
            Matcher matcher = pattern.matcher(sb.toString());
            StringBuffer jspSrc = new StringBuffer();
            while (matcher.find()) {
                String javaCode = sb.toString().substring(matcher.start() + 2, matcher.end() - 2);
                //写入Servlet类
                sbuf.append("\n\t\t" + javaCode + "\n");
                matcher.appendReplacement(jspSrc, "");
            }
            matcher.appendTail(jspSrc);
            //去除所有"特殊符号
            if (jspSrc.toString().contains("\"")) {
                String specToken = jspSrc.toString().replaceAll(String.valueOf(Request.FILENAMETAIL), "");
                jspSrc.delete(0, jspSrc.length());
                jspSrc.append(specToken);
            }
            /**
             * 第二步对#{}表达式解析
             */
            pattern = Pattern.compile("#\\{.*?\\}");
            matcher = pattern.matcher(jspSrc);
            sb.delete(0, sb.length());
            Set<String> set = new HashSet<>();//去重class field字段
            while (matcher.find()) {
                String el = jspSrc.toString().substring(matcher.start() + 2, matcher.end() - 1);//#{user.submit}
                String[] params = el.split("\\.");
                if (params.length <= 2) {//不支持级联赋值或者变量名错误
                    String tmp = params[0];
                    if (params.length == 2) {
                        tmp += CLASS_NAME_SPLITER + params[1];
                    }
                    if (!set.contains(tmp)) {
                        set.add(tmp);
                        String field = "private java.lang.Object " + tmp + ";";
                        CtField ctField = CtField.make(field, ct);
                        ct.addField(ctField);
                    }
                    matcher.appendReplacement(sb, Request.FILENAMETAIL + "+" + tmp + "+" + Request.FILENAMETAIL);
                } else {
                    throw new RuntimeException("不支持级联赋值或者变量名格式错误");
                }
            }
            matcher.appendTail(sb);
            Dispatcher.addUrlMapping(jspFile.substring(urlIndex), className);
            servletGenerator(ct, sbuf.toString(), sb.toString());
            return className;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void servletGenerator(CtClass ct, String javaCode, String jsp) throws Exception {
        CtMethod ctMethod = CtMethod.make("public void service(){" + javaCode + "}", ct);
        ct.addMethod(ctMethod);
        ctMethod = CtMethod.make("public String doGet(pers.pandora.servlet.Request request,pers.pandora.servlet.Response response) {StringBuffer sb = new StringBuffer(\"" + jsp + "\");service();return sb.toString();}", ct);
        ct.addMethod(ctMethod);
        ctMethod = CtMethod.make("public String doPost(pers.pandora.servlet.Request request,pers.pandora.servlet.Response response){return doGet(request,response);}", ct);
        ct.addMethod(ctMethod);
        ct.writeFile(CLASSDIR + PATH_SPLITER);
//        ct.defrost();
        System.out.println("编译结束：" + ct.getName());
    }

    /**
     * 动态编译Servlet类,jdk6引入
     */
//    private boolean dynamicClass(String className, String javaSrc) {
//        //当前编译器
//        JavaCompiler cmp = ToolProvider.getSystemJavaCompiler();
//        //Java标准文件管理器
//        StandardJavaFileManager fm = cmp.getStandardFileManager(null, null, null);
//        //Java文件对象
//        JavaFileObject jfo = new StringJavaObject(className, javaSrc);
//        //编译参数，类似于javac <options>中的options
//        List<String> optionsList = new ArrayList<String>();
//        //编译文件的存放地方，注意：此处是为Eclipse工具特设的
////        optionsList.addAll(Arrays.asList("-d","./bin"));
//        //要编译的单元
//        List<JavaFileObject> jfos = Arrays.asList(jfo);
//        //设置编译环境
//        JavaCompiler.CompilationTask task = cmp.getTask(null, fm, null, optionsList, null, jfos);
//        //编译成功
//        if (task.call()) {
//            try {
//                System.out.println(Class.forName("jsp." + className, true, Thread.currentThread().getContextClassLoader()));
//            } catch (ClassNotFoundException e) {
//                e.printStackTrace();
//            }
//            return true;
//        }
//        return false;
//    }
//
//    //提供动态编译的辅助类,实现三种方式的动态编译:字符串，文件（源文件，字节码文件），url资源
//    static class StringJavaObject extends SimpleJavaFileObject {
//        //源代码
//        private String content = "";
//
//        //遵循Java规范的类名及文件
//        public StringJavaObject(String _javaFileName, String _content) {
//            super(_createStringJavaObjectUri(_javaFileName), Kind.SOURCE);
//            content = _content;
//
//        }
//
//        //产生一个URL资源路径
//        private static URI _createStringJavaObjectUri(String name) {
//            //注意此处没有设置包名
//            return URI.create("String:///" + name + Kind.SOURCE.extension);
//        }
//
//        //文本文件代码
//        @Override
//        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
//            return content;
//        }
//    }
}
