package pers.pandora.utils;

import javassist.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pers.pandora.constant.HTTPStatus;
import pers.pandora.constant.JSP;
import pers.pandora.constant.LOG;
import pers.pandora.vo.Tuple;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jsp Parser
 */
public final class JspParser {

    private static final Logger logger = LogManager.getLogger(JspParser.class);
    //Caching generated servlet classes in non hot load mode,base on double check lock for lazy loading to save some memory
    private static volatile Map<String, String> jspCahce;

    //Hash encoding needed to generate jsp servlet class name
    private String hashEncode;

    private static final char PATH_SPLITER = '/';

    private static final String JSP_PACKAGE = "jsp";

    private static final char PACKAGE_SPLITER = '.';

    private static final String CLASSDIR = Objects.requireNonNull(JspParser.class.getClassLoader().getResource(String.valueOf(PACKAGE_SPLITER))).getPath();

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

    //It's lazy loading, no generation without access
    public Tuple<String, String, String> parse(String jspFile, boolean hotLoadJSP) {
        //JSP source file
        File file = new File(jspFile);
        if (!file.exists()) {
            return null;
        }
        String query = null;
        if(!hotLoadJSP){
            //DCL
            if (jspCahce == null) {
                synchronized (logger) {
                    if (jspCahce == null) {
                        jspCahce = new ConcurrentHashMap<>(16);
                    }
                }
            }
            query = jspCahce.get(jspFile);
        }
        Reader inFifle;
        StringBuilder buf;
        String jsp = null;
        try {
            if(!StringUtils.isNotEmpty(query)){
                inFifle = new FileReader(file);
                BufferedReader inputStream = new BufferedReader(inFifle);
                buf = new StringBuilder();
                String temp;
                while ((temp = inputStream.readLine()) != null) {
                    temp = temp.trim();
                    if(!temp.startsWith(JSP.DOUBLE_SLASH)){
                        buf.append(temp).append(JSP.CRLF);
                    }
                }
                inputStream.close();
                inFifle.close();
                jsp = buf.toString();
                //Using file hash, duplicate classes are not generated again
                query = CodeUtils.hashEncode(jsp, SALT, null, null);
                if(!hotLoadJSP){
                    //DCL
                    if (!jspCahce.containsKey(jspFile)) {
                        synchronized (logger) {
                            if (!jspCahce.containsKey(jspFile)) {
                                jspCahce.put(jspFile, query);
                            }
                        }
                    }
                }
            }
            int urlIndex = jspFile.lastIndexOf(PATH_SPLITER);
            String servletName = jspFile.substring(urlIndex + 1, jspFile.lastIndexOf(PACKAGE_SPLITER)).trim();
            String className = Character.toUpperCase(servletName.charAt(0)) + servletName.substring(1);
            className = className + LOG.CLASS_NAME_SPLITER + query;
            file = new File(CLASSDIR + JSP_PACKAGE + PATH_SPLITER + className + PACKAGE_SPLITER + CLASS_POS_MARK);
            className = JSP_PACKAGE + PACKAGE_SPLITER + className;
            if (file.exists()) {
                return new Tuple<>(className, null, null);
            }
            StringBuffer sb = new StringBuffer();
            if (jsp.contains(JSP.JSP_LANGUAGE_DESC)) {
                jsp = jsp.replace(JSP.JSP_LANGUAGE_DESC, LOG.NO_CHAR).trim();
            }
            sb.append(jsp.substring(jsp.indexOf(JSP.JSP_HEAD_DESC) + 6, jsp.indexOf(JSP.JSP_TAIL_DESC)).trim());
            //target class
            ClassPool pool = ClassPool.getDefault();
            CtClass ct = pool.makeClass(className);
            ct.setInterfaces(new CtClass[]{pool.get(SERVLET_CLASS)});
            /**
             * The first step is to parse the code of <% Java% >
             */
            Pattern pattern = Pattern.compile(JSP.JSP_CODE_PATTERN);
            StringBuffer sbuf = new StringBuffer();
            Matcher matcher = pattern.matcher(sb.toString());
            StringBuffer jspSrc = new StringBuffer();
            String javaCode, specToken;
            while (matcher.find()) {
                javaCode = sb.toString().substring(matcher.start() + 2, matcher.end() - 2);
                sbuf.append(JSP.JSP_CODE_PRE).append(javaCode).append(HTTPStatus.CRLF);
                matcher.appendReplacement(jspSrc, LOG.NO_CHAR);
            }
            matcher.appendTail(jspSrc);
            //Remove all "special symbols"
            if (jspSrc.toString().contains(String.valueOf(HTTPStatus.FILENAMETAIL))) {
                specToken = jspSrc.toString().replace(String.valueOf(HTTPStatus.FILENAMETAIL), JSP.SPECIAL_MARK);
                jspSrc.delete(0, jspSrc.length());
                jspSrc.append(specToken);
            }
            /**
             * The second step is to analyze the #{} expression
             */
            pattern = Pattern.compile(JSP.JSP_VAR_PATTERN);
            matcher = pattern.matcher(jspSrc);
            sb.delete(0, sb.length());
            //De duplicate class field
            Set<String> set = new HashSet<>();
            String el, tmp, field;
            String[] params;
            while (matcher.find()) {
                el = jspSrc.toString().substring(matcher.start() + 2, matcher.end() - 1);//#{user.submit}
                params = el.split(JSP.JSP_VAR_SPLITER_PATTERN);
                //Cascade assignment or variable name error is not supported
                if (params.length <= 2) {
                    tmp = params[0];
                    if (params.length == 2) {
                        tmp += LOG.CLASS_NAME_SPLITER + params[1];
                    }
                    if (!set.contains(tmp)) {
                        set.add(tmp);
                        field = JSP.JSP_JAVA_FIELD_PRE + tmp + JSP.JAVA_SPLITER;
                        ct.addField(CtField.make(field, ct));
                    }
                    //HTTPStatus.FILENAMETAIL + JSP.JAVA_ADD = 77 ?
                    //The expression type is inferred from left to right, resulting in the addition of the characters ASCII
                    matcher.appendReplacement(sb, String.valueOf(HTTPStatus.FILENAMETAIL) + JSP.JAVA_ADD + tmp +
                            JSP.JAVA_ADD + HTTPStatus.FILENAMETAIL);
                } else {
                    logger.warn(LOG.LOG_PRE + "parse for " + LOG.LOG_PRE + "that can't parse this format string", this, el);
                }
            }
            matcher.appendTail(sb);
            servletGenerator(ct, set, sbuf.toString(), sb.toString());
            return new Tuple<>(className, jspFile.substring(urlIndex), className);
        } catch (Exception e) {
            logger.error(LOG.LOG_PRE + "parse" + LOG.LOG_POS, this, LOG.EXCEPTION_DESC, e);
        }
        return null;
    }

    private void servletGenerator(CtClass ct, Set<String> fields, String javaCode, String jsp) throws Exception {
        ct.addMethod(CtMethod.make(buildService(javaCode, fields), ct));
        ct.addMethod(CtMethod.make(buildDoGet(jsp), ct));
        ct.addMethod(CtMethod.make(buildDoPost(), ct));
        ct.writeFile(CLASSDIR);
        logger.info("Complie And Completed:" + LOG.LOG_PRE, ct.getName());
    }

    private String buildDoPost() {
        return JSP.JAVA_DOPOST;
    }

    private String buildService(String javaCode, Set<String> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append(JSP.JAVA_SERVICE_PRE);
        fields.forEach(field -> sb.append(JSP.IF_PRE).append(field).append(HTTPStatus.PARAM_KV_SPLITER)
                .append(HTTPStatus.PARAM_KV_SPLITER).append(JSP.NULL).append(JSP.IF_POS)
                .append(field).append(HTTPStatus.PARAM_KV_SPLITER).append(HTTPStatus.FILENAMETAIL)
                .append(HTTPStatus.FILENAMETAIL).append(HTTPStatus.COOKIE_SPLITER).append(HTTPStatus.CRLF));
        sb.append(javaCode).append(JSP.JAVA_RIGHT_BRACKET);
        return sb.toString();
    }

    private String buildDoGet(String jsp) {
        return JSP.JAVA_DOGET_PRE + jsp + JSP.JAVA_DOGET_POS;
    }

    /**
     * Dynamic compilation of servlet class, introduced by JDK6
     */
    @Deprecated
    private boolean dynamicClass(String className, String javaSrc) {
        //Current compiler
        JavaCompiler cmp = ToolProvider.getSystemJavaCompiler();
        //Java standard file manager
        StandardJavaFileManager fm = cmp.getStandardFileManager(null, null, null);
        //Java file object
        JavaFileObject jfo = new StringJavaObject(className, javaSrc);
        //Compile parameters, similar to options in javac <Options>
        List<String> optionsList = new ArrayList<String>();
        //Note: This is a special place for eclipse tools
        // optionsList.addAll(Arrays.asList("-d","./bin"));
        //Units to compile
        List<JavaFileObject> jfos = Arrays.asList(jfo);
        //Set up compilation environment
        JavaCompiler.CompilationTask task = cmp.getTask(null, fm, null, optionsList, null, jfos);
        //Compiled successfully
        if (task.call()) {
            try {
                logger.info("Complie And Completed:" + LOG.LOG_PRE,
                        Class.forName(JSP.JSP_PACKAGE + className, true, Thread.currentThread().getContextClassLoader()));
            } catch (ClassNotFoundException e) {
                logger.error(LOG.LOG_PRE + "dynamicClas" + LOG.LOG_POS, this.getClass().getName(), LOG.EXCEPTION_DESC, e);
            }
            return true;
        }
        return false;
    }

    //It provides the auxiliary class of dynamic compilation and realizes three ways of dynamic compilation: string, file (source file, bytecode file), URL resource
    static class StringJavaObject extends SimpleJavaFileObject {
        //source code
        private String content;

        //Class name and file following Java specification
        public StringJavaObject(String _javaFileName, String _content) {
            super(_createStringJavaObjectUri(_javaFileName), Kind.SOURCE);
            content = _content;

        }

        //Generate a URL resource path
        private static URI _createStringJavaObjectUri(String name) {
            //Note that the package name is not set here
            return URI.create(JSP.JSP_DYNAMIC_URI_HEAD + name + Kind.SOURCE.extension);
        }

        //Text file code
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return content;
        }
    }
}
