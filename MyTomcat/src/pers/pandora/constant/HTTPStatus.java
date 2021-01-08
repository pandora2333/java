package pers.pandora.constant;


public class HTTPStatus {

    public static final String SLASH = "/";

    public static final String MUPART_NAME = "name=";

    public static final String MUPART_DESC_LINE = "--";//file start mark

    public static final int LINE_SPLITER = System.lineSeparator().length();//windows 2 byte:\r\n; linux 1 byte: \n

    public static final String FILEMARK = "multipart/form-data; boundary=";

    public static final String FILENAME = "; filename=";

    public static final char FILENAMETAIL = '\"';

    public static final char CRLF = '\n';

    public static final String JSP = ".jsp";

    public static final String GET = "GET";

    public static final String POST = "POST";

    public static final String PUT = "PUT";

    public static final String DELETE = "DELETE";

    public static final String OPTIONS = "OPTIONS";

    public static final char BLANK = ' ';

    public static final String HEAD_INFO_SPLITER = "\\:";

    public static final String COOKIE_SPLITER = "; ";

    public static final String PARAM_KV_SPLITER = "=";

    public static final String HTTP1_1 = "HTTP/1.1";

    public static final String HTTP1_0 = "HTTP/1.0";

    public static final String HTTP = "HTTP";

    public static final String COOKIE_MARK = "Cookie";

    public static final String SESSION_MARK = "sessionID";

    public static final String PLAIN = "text/plain";

    public static final String SERVER = "Server";

    public static final String DATE = "Date";

    public static final String CONTENTTYPE = "Content-Type";

    public static final String CONTENTLENGTH = "Content-Length";

    public static final String SET_COOKIE = "Set-Cookie";

    public static final char COLON = ':';

    public static final char GET_PARAMTER_MARK = '?';

    public static final String PARAMETER_SPLITER = "&";

    public static final char TRANSVERSE = '_';

    public static final char COMMA = ',';

    public static final String DEFAULTENCODING = "utf-8";

    public static final String HTML_MARK = ".html";

    public static final String HTM_MARK = ".htm";

    public static final String TEXT_HTML = "text/html";

    public static final String JPG = ".jpg";

    public static final String PNG = ".png";

    public static final String JPEG = ".jpeg";

    public static final String XML = ".xml";

    public static final String GIF = ".gif";

    public static final String JSON = ".json";

    public static final String PDF = ".pdf";

    public static final String JS = ".js";

    public static final String CSS = ".css";

    public static final String TXT = ".txt";

    public static final String SVG = ".svg";

    public static final String ICO = ".ico";

    public static final String BMP = ".bmp";

    public static final String WAV = ".wav";

    public static final String M3U = ".m3u";

    public static final String MP4 = ".mp4";

    public static final String MP3 = ".mp3";

    public static final String AU = ".au";

    public static final String ZIP = ".zip";

    public static final String CER = ".cer";

    public static final String CRT = ".crt";

    public static final String PPT = ".ppt";

    public static final String PPTX = ".pptx";

    public static final String XLS = ".xls";

    public static final String XLSX = ".xlsx";

    public static final String DOC = ".doc";

    public static final String DOCX = ".docx";

    public static final String SH = ".sh";

    public static final String TAR = ".tar";

    public static final String AVI = ".avi";

    public static final String WOFF2 = ".woff2";

    public static final String WOFF = ".woff";

    public static final String TTF = ".ttf";

    public static final String WOFF2_TYPE = "font/woff2";

    public static final String WOFF_TYPE = "font/woff";

    public static final String TTF_TYPE = "font/ttf";

    public static final String JSON_TYPE = "application/json";

    public static final String SH_TYPE = "application/x-sh";

    public static final String AVI_TYPE = "video/x-msvideo";

    public static final String TAR_TYPE = "application/x-tar";

    public static final String DOC_TYPE = "application/msword";

    public static final String XLS_TYPE = "application/vnd.ms-excel";

    public static final String PPT_TYPE = "application/vnd.ms-powerpoint";

    public static final String ZIP_TYPE = "application/zip";

    public static final String CER_TYPE = "application/x-x509-ca-cert";

    public static final String CRT_TYPE = CER_TYPE;

    public static final String AU_TYPE = "audio/basic";

    public static final String JPG_TYPE = "image/jpeg";

    public static final String PNG_TYPE = "image/png";

    public static final String XML_TYPE = "text/xml";

    public static final String GIF_TYPE = "image/gif";

    public static final String PDF_TYPE = "application/pdf";

    public static final String BINARY_TYPE = "application/octet-stream";

    public static final String JS_TYPE = "application/javascript";

    public static final String CSS_TYPE = "text/css";

    public static final String TXT_TYPE = PLAIN;

    public static final String SVG_TYPE = "image/svg+xml";

    public static final String ICO_TYPE = "image/x-icon";

    public static final String BMP_TYPE = "image/bmp";

    public static final String WAV_TYPE = "audio/x-wav";

    public static final String M3U_TYPE = "audio/x-mpegurl";

    public static final String MP4_TYPE = "video/mp4";

    public static final String MP3_TYPE = "audio/mpeg";

    public static final int CODE_200 = 200;

    public static final int CODE_302 = 302;

    public static final int CODE_400 = 400;

    public static final int CODE_404 = 404;

    public static final int CODE_405 = 405;

    public static final int CODE_500 = 500;

    public static final int CODE_304 = 304;

    public static final int CODE_206 = 206;

    public static final String CODE_206_DESC = "Partial Content";

    public static final String CODE_304_DESC = "Not Modified";

    public static final String CODE_200_DESC = "OK";

    public static final String CODE_404_DESC = "Not Found";

    public static final String CODE_500_DESC = "Server Error";

    public static final String CODE_302_DESC = "Found";

    public static final String ERROR_CODE = "Error Code";

    public static final String SERVER_DESC = "Pandora Server/1.0.1";

    public static final String CHARSET = "charset";

    public static final String CODE_500_OUTPUT_DESC = "Server Inner Some Errors";

    public static final String CODE_404_OUTPUT_DESC = "Page Not Found";

    public static final String CODE_405_METHOD_NOT_SUPPORTED = "method not allowed";

    public static final String CODE_400_BAD_REQUEST = "Bad Request";

    public static final String PATH_PARAM_SEPARATOE = "\\{.*?\\}";

    public static final String PATH_REGEX_MARK = ".*?/\\{.*?\\}.*";

    public static final String PATH_PARM_REPLACE = ".*?";

    public static final String VERSION = "Version";

    public static final String EXPIRES = "Expires";

    public static final String MAXAEG = "Max-Age";

    public static final String DOMAIN = "Domain";

    public static final String PATH = "Path";

    public static final String SECURE = "Secure";

    public static final String ALLOW = "Allow";

    public static final String REDIRECT = "@redirect:";

    public static final String LOCATION = "Location";

    public static final String CONNECTION = "Connection";

    public static final String KEEPALIVE = "keep-alive";

    public static final String CLOSE = "close";

    public static final String CACAHE_CONTROL = "Cache-Control";

    public static final String CACHAE_CONTROL_DESC = "public, max-age=";

    public static final String LASTMODIFIED = "Last-Modified";

    public static final String IF_MODIFIED_SINCE = "If-Modified-Since";

    public static final String IF_NONE_MATCH = "If-None-Match";

    public static final String ETAG = "ETag";

    public static final String NO_CACHE = "no-cache";

    public static final String PRAGMA = "Pragma";

    public static final String RANGE = "Range";

    public static final String BYTES = "bytes";

    public static final String RANGEREGEX = "\\-";

    public static final String ACCEPTRANGES = "Accept-Ranges";

    public static final String CONTENTRANGE = "Content-Range";

    public static final String IFRANGE = "If-Range";

    public static final String LISTTYPE = ".*?\\[[0-9]{1,}?\\]\\..*";

    public static final char LISTTYPESEPARATOR_PRE = '[';

    public static final char LISTTYPESEPARATOR_POS = ']';
}
