package pers.pandora.utils;

import javassist.*;
import pers.pandora.servlet.Request;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析jsp页面
 */
public final class JspParser {
    private List<Object> valueObject;
    private Map<String, MapContent> context;
    private Long jspNum;

    public JspParser(List<Object> valueObject, Map<String, MapContent> context) {//支持mvc使用,使用了#{}表达式
        this.valueObject = valueObject;
        this.context = context;
    }

    public void setContext(Map<String, MapContent> context) {
        this.context = context;
    }

    public void setValueObject(List<Object> valueObject) {
        this.valueObject = valueObject;
    }

    public Long getJspNum() {
        return jspNum;
    }

    public void setJspNum(Long jspNum) {
        this.jspNum = jspNum;
    }

    public String parse(String jspFile, Map<String, List<Object>> extra) {
        File file = new File(jspFile);
        if (!file.exists()) {
            return null;
        }
        Reader inFifle = null;
        try {
            inFifle = new FileReader(file);
            BufferedReader inputStream = new BufferedReader(inFifle);
            StringBuffer buf = new StringBuffer();
            String temp = null;
            while ((temp = inputStream.readLine()) != null) {
                buf.append(temp.trim());
            }
            inputStream.close();
            inFifle.close();
            String jsp = buf.toString();
            StringBuilder sb = new StringBuilder();
            if (jsp.contains("langage=\"java\"")) {
                jsp = jsp.replace("langage=\"java\"", "").trim();
            }
            sb.append(jsp.substring(jsp.indexOf("<jsp") + 6, jsp.indexOf("</jsp>")).trim());
            /**
             * 第一步对#{}表达式解析
             */
            Pattern pattern = Pattern.compile("#\\{.*?\\}");
            Matcher matcher = pattern.matcher(sb.toString());
            StringBuffer sbuf = new StringBuffer();
            while (matcher.find()) {
                String el = sb.toString().substring(matcher.start() + 2, matcher.end() - 1);//#{user.submit}
                String[] params = el.split("\\.");
                if (params.length == 2) {//不支持级联赋值或者变量名错误
                    if(params[0].equals(Request.REQUSTSCOPE)) {//请求域(只处理基本数据类型和String)
                        List<Object> param = extra.get(params[1]);
                        String replace = null;
                        if(param != null && param.size() == 1) {
                            replace = param.get(0).toString();
                        }else{
                            replace = Request.ERROR_PARAM;
                        }
                        matcher.appendReplacement(sbuf, replace);
                    } else {
                        if(valueObject != null) {
                            for (Object obj : valueObject) {
                                if (obj.getClass().getSimpleName().equalsIgnoreCase(params[0])) {
                                    matcher.appendReplacement(sbuf, String.valueOf(obj.getClass().getDeclaredMethod("get" + params[1].substring(0, 1)
                                            .toUpperCase() + params[1].substring(1)).invoke(obj)));
                                    break;
                                }
                            }
                        }else{
                            matcher.appendReplacement(sbuf, Request.ERROR_PARAM);
                        }
                    }
                } else {
                    throw new RuntimeException("不支持级联赋值或者变量名错误");
                }
            }
            matcher.appendTail(sbuf);
            /**
             * 第二步对<% java %>代码解析
             */
            if (sbuf.toString().equals("")) {
                sbuf.append(sb).trimToSize();
            }
            pattern = Pattern.compile("<%.*?%>");
            matcher = pattern.matcher(sbuf);
            sb.delete(0, sb.length());
            StringBuffer jspSrc = new StringBuffer();
            while (matcher.find()) {
                String javaCode = sbuf.toString().substring(matcher.start() + 2, matcher.end() - 2);
                //写入Servlet类
                sb.append("\n\t\t" + javaCode + "\n");
                matcher.appendReplacement(jspSrc, "");
            }
            matcher.appendTail(jspSrc);
            if (jspSrc.toString().contains("\"")) {
                String specToken = jspSrc.toString().replaceAll("\"", "");
                jspSrc.delete(0, jspSrc.length());
                jspSrc.append(specToken);
            }
            String servletName = jspFile.substring(jspFile.lastIndexOf("/") + 1, jspFile.lastIndexOf(".")).trim();
            String className = servletName.substring(0, 1).toUpperCase() + servletName.substring(1);
            MapContent servletJsp = new MapContent();
            servletJsp.setClassName("jsp." + className);
            List urls = new LinkedList();
            urls.add(jspFile.substring(jspFile.lastIndexOf("/")));
            servletJsp.setUrls(urls);
            context.put(className, servletJsp);//存入context中以便dispatcher获取servlet
            return servletGenerator(className, sb.toString(), jspSrc.toString());
//            System.out.println(jspSrc);
        } catch (Exception e) {

        }
        return null;
    }

    /**
     * 解析jsp生成Servlet
     */
//    private void servletGenerator(String servletName,String javaCode,String jsp) throws Exception {
//        File diretory = new File("src/jsp/");
//        if(!diretory.exists()){
//            diretory.mkdirs();
//        }
//        File javaSrc = new File("src/jsp/"+servletName+".java");
//        //文件已经编译，无需重复生成的情况,减少额外的流操作，快速响应页面
//        if(javaSrc.exists()&&!isGenerator){
//            return;
//        }
//        OutputStream out = new FileOutputStream(javaSrc);
//        BufferedOutputStream outServlet = new BufferedOutputStream(out);
//        StringBuffer servlet = new StringBuffer();
//        servlet.append("package jsp;\n");
//        servlet.append("import pers.pandora.servlet.Servlet;\n");
//        servlet.append("import java.util.Map;\n");
//        servlet.append("public class "+servletName+" implements  Servlet{\n");
//        servlet.append("\n\t@Override\n" +
//                "\tpublic void service() {");
//        servlet.append("\t\t"+javaCode);
//        servlet.append("\t}\n");
//        servlet.append("\t@Override\n" +
//                "\tpublic String doGet(Map params) {\n");
//        servlet.append("\t\tStringBuffer sb = new StringBuffer(\""+jsp+"\");\n");
//        servlet.append("\t\tservice();\n");
//        servlet.append("\t\treturn sb.toString();\n");
//        servlet.append("\t}\n");
//        servlet.append("\t@Override\n" +
//                "\tpublic String doPost(Map params) {\n" +
//                "\t\treturn doGet(params);\n" +
//                "\t}\n");
//        servlet.append("}");
//        outServlet.write(servlet.toString().getBytes());
//        outServlet.close();
//        out.close();
//        //使用动态编译
//        if(dynamicClass(servletName,servlet.toString())){
//            System.out.println("[DEBUG INFO] "+servletName+".jsp 编译成功!");
//        }else {
//            System.out.println("[DEBUG INFO] "+servletName+".jsp 编译失败!");
//        }
//
//    }
    private String servletGenerator(String servletName, String javaCode, String jsp) throws Exception {
        jspNum = System.currentTimeMillis();
        ClassPool pool = ClassPool.getDefault();
        String className = "jsp." + servletName + "_" + jspNum;
        CtClass ct = pool.makeClass(className);
        ct.setInterfaces(new CtClass[]{pool.get("pers.pandora.servlet.Servlet")});
//        CtField ctField = CtField.make(,ct);
//        ct.addField(ctField);
        CtMethod ctMethod = CtMethod.make("public void service(){" + javaCode + "}", ct);
        ct.addMethod(ctMethod);
        ctMethod = CtMethod.make("public String doGet(pers.pandora.servlet.Request request,pers.pandora.servlet.Response response) {StringBuffer sb = new StringBuffer(\"" + jsp + "\");service();return sb.toString();}", ct);
        ct.addMethod(ctMethod);
        ctMethod = CtMethod.make("public String doPost(pers.pandora.servlet.Request request,pers.pandora.servlet.Response response){return doGet(request,response);}", ct);
        ct.addMethod(ctMethod);
//        CtConstructor ctConstructor = new CtConstructor(new CtClass[]{},ct);
//        ctConstructor.setBody();
//        ct.addConstructor(ctConstructor);
        ct.writeFile("bin/");
//        ct.defrost();
        System.out.println("编译结束：" + Class.forName("jsp." + servletName + "_" + jspNum));
        return className;
    }

    /**
     * 动态编译Servlet类,jdk6引入
     */
    private boolean dynamicClass(String className, String javaSrc) {
        //当前编译器
        JavaCompiler cmp = ToolProvider.getSystemJavaCompiler();
        //Java标准文件管理器
        StandardJavaFileManager fm = cmp.getStandardFileManager(null, null, null);
        //Java文件对象
        JavaFileObject jfo = new StringJavaObject(className, javaSrc);
        //编译参数，类似于javac <options>中的options
        List<String> optionsList = new ArrayList<String>();
        //编译文件的存放地方，注意：此处是为Eclipse工具特设的
//        optionsList.addAll(Arrays.asList("-d","./bin"));
        //要编译的单元
        List<JavaFileObject> jfos = Arrays.asList(jfo);
        //设置编译环境
        JavaCompiler.CompilationTask task = cmp.getTask(null, fm, null, optionsList, null, jfos);
        //编译成功
        if (task.call()) {
            try {
                System.out.println(Class.forName("jsp." + className, true, Thread.currentThread().getContextClassLoader()));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    //提供动态编译的辅助类,实现三种方式的动态编译:字符串，文件（源文件，字节码文件），url资源
    static class StringJavaObject extends SimpleJavaFileObject {
        //源代码
        private String content = "";

        //遵循Java规范的类名及文件
        public StringJavaObject(String _javaFileName, String _content) {
            super(_createStringJavaObjectUri(_javaFileName), Kind.SOURCE);
            content = _content;

        }

        //产生一个URL资源路径
        private static URI _createStringJavaObjectUri(String name) {
            //注意此处没有设置包名
            return URI.create("String:///" + name + Kind.SOURCE.extension);
        }

        //文本文件代码
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return content;
        }
    }
}
