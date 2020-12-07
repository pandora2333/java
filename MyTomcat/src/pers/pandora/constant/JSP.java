package pers.pandora.constant;

public class JSP {

    public static final String JSP_LANGUAGE_DESC = "langage=\"java\"";

    public static final String JSP_HEAD_DESC = "<jsp";

    public static final String JSP_TAIL_DESC = "</jsp>";

    public static final String JSP_CODE_PATTERN = "<%.*?%>";

    public static final String JSP_CODE_PRE = "\n\t\t";

    public static final String JSP_VAR_PATTERN = "#\\{.*?\\}";

    public static final String JSP_VAR_SPLITER_PATTERN = "\\.";

    public static final String JSP_JAVA_FIELD_PRE = "private java.lang.Object ";

    public static final char JAVA_SPLITER = ';';

    public static final char JAVA_ADD = '+';

    public static final String JAVA_DOPOST = "public String doPost(pers.pandora.core.Request request,pers.pandora.core.Response response){return doGet(request,response);}";

    public static  final String JAVA_SERVICE_PRE = "public void service(){";

    public static final char JAVA_RIGHT_BRACKET = '}';

    public static final String JAVA_DOGET_PRE = "public String doGet(pers.pandora.core.Request request,pers.pandora.core.Response response) {StringBuffer sb = new StringBuffer(\"";

    public static final String JAVA_DOGET_POS = "\");service();return sb.toString();}";

    public static final String JSP_PACKAGE = "jsp.";

    public static final String JSP_DYNAMIC_URI_HEAD = "String:///";

}
