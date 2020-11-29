package pers.pandora.constant;

public class HTTPStatus {

    public static final char SLASH = '/';

    public static final String MUPART_NAME = "name=";

    public static final String MUPART_DESC_LINE = "--";//文件开始

    public static final int LINE_SPLITER = System.lineSeparator().length();//windows 2 byte:\r\n; linux 1 byte: \n

    public static final String FILEMARK = "Content-Type: multipart/form-data; boundary=";

    public static final String FILENAME = "; filename=";

    public static final char FILENAMETAIL = '\"';

    public static final char CRLF = '\n';

    public static final String JSP = ".jsp";

    public static final String GET = "GET";

    public static final String POST = "POST";

    public static final char BLANK = ' ';

    public static final String HEAD_INFO_SPLITER = "\\:";

    public static final String COOKIE_SPLITER = "; ";

    public static final String COOKIE_KV_SPLITE = "=";

    public static final String HTTP1_1 = "HTTP/1.1";

    public static final String HTTP = "HTTP";

    public static final String TEXT_PLAIN = "text/plain";

    public static final String COOKIE_MARK = "cookie";

    public static final String SESSION_MARK = "sessionID";

    public static final String PLAIN = "text/plain";

    public static final String SERVER = "Server";

    public static final String DATE = "Date";

    public static final String CONTENTTYPE = "Content-type";

    public static final String CONTENTLENGTH = "Content-Length";

    public static final String SET_COOKIE = "Set-Cookie";

    public static final char COLON = ':';

    public static final char GET_PARAMTER_MARK = '?';

    public static final char PARAMETER_SPLITER = '&';

    public static final String DEFAULTENCODING = "utf-8";

    public static final String HTML_MARK = ".html";

    public static final String HTM_MARK = ".htm";

    public static final String TEXT_HTML = "text/html";

    public static final String JPG = ".jpg";

    public static final String PNG = ".png";

    public static final String JPEG = ".jpeg";

    public static final String IMAGE_TYPE = "image/jpeg";

    public static final String CODE_200 = "OK";

    public static final String CODE_404 = "Not Found";

    public static final String CODE_500 = "Server Error";

    public static final String CODE_302 = "Found";

    public static final String ERROR_CODE = "Error Code";

    public static final String SERVER_DESC = "Pandora Server/1.0.1";

    public static final String CHARSET = "charset";

    public static final String CODE_500_DESC = "Serer Inner Some Errors";

    public static final String CODE_404_DESC = "Page Not Found";

    //cookie
    public static final String VERSION = "Version";

    public static final String EXPIRES = "Expires";

    public static final String MAXAEG = "Max-Age";

    public static final String DOMAIN = "Domain";

    public static final String PATH = "Path";

    public static final String SECURE = "secure";
}
