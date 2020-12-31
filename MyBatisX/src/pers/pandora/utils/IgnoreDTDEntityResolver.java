package pers.pandora.utils;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import pers.pandora.constant.LOG;
import pers.pandora.constant.XML;

public final class IgnoreDTDEntityResolver implements EntityResolver {

    @Override
    public InputSource resolveEntity(final String publicId, final String systemId) {
        return new InputSource(new ByteArrayInputStream(XML.HEAD_DTD.getBytes(Charset.forName(LOG.DEFAULTENCODING))));
    }
}
