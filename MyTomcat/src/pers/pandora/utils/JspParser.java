package pers.pandora.utils;
import org.junit.jupiter.api.Test;
import pers.pandora.bean.User;
import pers.pandora.servlet.Servlet;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析jsp页面
 */
public class JspParser {
    private  Stack<Object> valueStack;//引入Struts的值栈，维护一个对象栈
    private  Map<String,MapContent> context;
    private boolean isGenerator;//是否重复生成文件
    public  JspParser(Stack<Object> valueStack, Map<String,MapContent> context){//值栈支持mvc使用,使用了#{}表达式
        this.valueStack = valueStack;
        this.context = context;
        if(valueStack!=null){
            isGenerator = true;
        }
    }

    public  void parse(String jspFile){
        Reader inFifle = null;
        try {
            inFifle = new FileReader(new File(jspFile));
            BufferedReader inputStream = new BufferedReader(inFifle);
            StringBuffer buf = new StringBuffer();
            String temp = null;
            while ((temp = inputStream.readLine())!=null){
                buf.append(temp.trim());
            }
            inputStream.close();
            inFifle.close();
            String jsp = buf.toString();
            StringBuilder sb = new StringBuilder();
            if(jsp.contains("langage=\"java\"")){
                jsp = jsp.replace("langage=\"java\"","").trim();
            }
            sb.append(jsp.substring(jsp.indexOf("<jsp")+6,jsp.indexOf("</jsp>")).trim());
            /**
             * 第一步对#{}表达式解析
             */
            Pattern pattern = Pattern.compile("#\\{.*?\\}");
            Matcher matcher = pattern.matcher(sb.toString());
            StringBuffer sbuf = new StringBuffer();
            if(valueStack!=null) {
                Object obj = valueStack.pop();
                while (matcher.find()) {
                    String el = sb.toString().substring(matcher.start() + 2, matcher.end() - 1);//#{user.submit}
//                System.out.println(el);
                    String[] params = el.split("\\.");
                    while (obj != null) {
                        if (obj.getClass().getSimpleName().equalsIgnoreCase(params[0])) {
                            if (params.length >= 2) {//不支持级联赋值
                                matcher.appendReplacement(sbuf, String.valueOf(obj.getClass().getDeclaredMethod("get" + params[1].substring(0, 1)
                                        .toUpperCase() + params[1].substring(1)).invoke(obj)));
                                break;
                            }
                        } else {
                            obj = valueStack.pop();
                        }
                    }
                }
                matcher.appendTail(sbuf);
            }
            /**
             * 第二步对<% java %>代码解析
             */
            if(sbuf.toString().equals("")){
                sbuf.append(sb).trimToSize();
            }
            pattern = Pattern.compile("<%.*?%>");
            matcher = pattern.matcher(sbuf);
            sb.delete(0,sb.length());
            StringBuffer jspSrc = new StringBuffer();
            while (matcher.find()){
                String javaCode = sbuf.toString().substring(matcher.start()+2,matcher.end()-2);
                //写入Servlet类
                sb.append("\n\t\t"+javaCode+"\n");
                matcher.appendReplacement(jspSrc,"");
            }
            matcher.appendTail(jspSrc);
            if(jspSrc.toString().contains("\"")){
                String specToken = jspSrc.toString().replaceAll("\"","");
                jspSrc.delete(0,jspSrc.length());
                jspSrc.append(specToken);
            }
            String servletName = jspFile.substring(jspFile.lastIndexOf("/")+1,jspFile.lastIndexOf(".")).trim();
            String className = servletName.substring(0,1).toUpperCase()+servletName.substring(1);
            MapContent servletJsp = new MapContent();
            servletJsp.setClassName("jsp."+className);
            List urls = new LinkedList();
            urls.add(jspFile.substring(jspFile.lastIndexOf("/")));
            servletJsp.setUrls(urls);
            context.put(className,servletJsp);//存入context中以便dispatcher获取servlet
            servletGenerator(className,sb.toString(),jspSrc.toString());
//            System.out.println(jspSrc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析jsp生成Servlet
     */
    private void servletGenerator(String servletName,String javaCode,String jsp) throws Exception {
        File diretory = new File("src/jsp/");
        if(!diretory.exists()){
            diretory.mkdirs();
        }
        File javaSrc = new File("src/jsp/"+servletName+".java");
        //文件已经编译，无需重复生成的情况,减少额外的流操作，快速响应页面
        if(javaSrc.exists()&&!isGenerator){
            return;
        }
        OutputStream out = new FileOutputStream(javaSrc);
        BufferedOutputStream outServlet = new BufferedOutputStream(out);
        StringBuffer servlet = new StringBuffer();
        servlet.append("package jsp;\n");
        servlet.append("import pers.pandora.servlet.Servlet;\n");
        servlet.append("import java.util.Map;\n");
        servlet.append("public class "+servletName+" implements  Servlet{\n");
        servlet.append("\n\t@Override\n" +
                "\tpublic void service() {");
        servlet.append("\t\t"+javaCode);
        servlet.append("\t}\n");
        servlet.append("\t@Override\n" +
                "\tpublic String doGet(Map params) {\n");
        servlet.append("\t\tStringBuffer sb = new StringBuffer(\""+jsp+"\");\n");
        servlet.append("\t\tservice();\n");
        servlet.append("\t\treturn sb.toString();\n");
        servlet.append("\t}\n");
        servlet.append("\t@Override\n" +
                "\tpublic String doPost(Map params) {\n" +
                "\t\treturn doGet(params);\n" +
                "\t}\n");
        servlet.append("}");
        outServlet.write(servlet.toString().getBytes());
        outServlet.close();
        out.close();
        //使用动态编译
//        if(dynamicClass(servletName,servlet.toString())){
//            System.out.println("[DEBUG INFO] "+servletName+".jsp 编译成功!");
//        }else {
//            System.out.println("[DEBUG INFO] "+servletName+".jsp 编译失败!");
//        }

    }

    /**
     * 动态编译Servlet类,jdk6引入
     */
    private boolean dynamicClass(String className,String javaSrc){
        //当前编译器
        JavaCompiler cmp = ToolProvider.getSystemJavaCompiler();
        //Java标准文件管理器
        StandardJavaFileManager fm = cmp.getStandardFileManager(null,null,null);
        //Java文件对象
        JavaFileObject jfo = new StringJavaObject(className,javaSrc);
        //编译参数，类似于javac <options>中的options
        List<String> optionsList = new ArrayList<String>();
        //编译文件的存放地方，注意：此处是为Eclipse工具特设的
//        optionsList.addAll(Arrays.asList("-d","./bin"));
        //要编译的单元
        List<JavaFileObject> jfos = Arrays.asList(jfo);
        //设置编译环境
        JavaCompiler.CompilationTask task = cmp.getTask(null, fm, null, optionsList,null,jfos);
        //编译成功
        if(task.call()) {
            try {
                System.out.println(Class.forName(className));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return  true;
        }
        return false;
    }
    //提供动态编译的辅助类,实现三种方式的动态编译:字符串，文件（源文件，字节码文件），url资源
    static class StringJavaObject extends SimpleJavaFileObject {
      //源代码
      private String content = "";
      //遵循Java规范的类名及文件
      public StringJavaObject(String _javaFileName,String _content){
          super(_createStringJavaObjectUri(_javaFileName),Kind.SOURCE);
          content = _content;

      }
     //产生一个URL资源路径
     private static URI _createStringJavaObjectUri(String name){
         //注意此处没有设置包名
         return URI.create("String:///" + name + Kind.SOURCE.extension);
      }
    //文本文件代码
    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
          return content;
      }
    }
    @Test
    public void test(){
        valueStack = new Stack<>();
        User u = new User();
        u.setUsername("tom");
        u.setPassword("123");
        Thread th = new Thread();
        valueStack.push(u);
        valueStack.push(th);
        parse("WebRoot/test.jsp");
    }
}
