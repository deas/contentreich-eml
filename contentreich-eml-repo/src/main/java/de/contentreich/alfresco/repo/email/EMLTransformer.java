package de.contentreich.alfresco.repo.email;
/*
 * Copyright (C) 2005-2012 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.AbstractContentTransformer2;
import org.alfresco.repo.content.transform.TransformerConfig;
import org.alfresco.repo.management.subsystems.ChildApplicationContextFactory;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

// package org.alfresco.repo.content.transform;

/**
 * Uses javax.mail.MimeMessage to generate plain text versions of RFC822 email
 * messages. Searches for all text content parts, and returns them. Any
 * attachments are ignored. TIKA Note - could be replaced with the Tika email
 * parser. Would require a recursing parser to be specified, but not the full
 * Auto one (we don't want attachments), just one containing text and html
 * related parsers.
 * <p/>
 * web-preview.get.js ->
 * <p/>
 * http://localhost:8080/alfresco/service/api/node/workspace/SpacesStore/e3b3da93-fbe8-4322-b0e3-8192f7686b20/metadata
 * <p/>
 * ->
 * <p/>
 * {
 * "mimetype": "message\/rfc822",
 * }
 */
public class EMLTransformer extends AbstractContentTransformer2 implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    private boolean forceHtml;
    private Boolean html;

    // http://localhost:8080/alfresco/service/api/node/workspace/SpacesStore/e3b3da93-fbe8-4322-b0e3-8192f7686b20/metadata
    private static final Logger logger = LoggerFactory.getLogger(EMLTransformer.class);

    public void setForceHtml(boolean html) {
        this.forceHtml = html;
    }

    @Override
    public boolean isTransformableMimetype(String sourceMimetype, String targetMimetype, TransformationOptions options) {
        String target = html() ? MimetypeMap.MIMETYPE_HTML : MimetypeMap.MIMETYPE_TEXT_PLAIN;
        boolean is = false;
        if (MimetypeMap.MIMETYPE_RFC822.equals(sourceMimetype) && target.equals(targetMimetype)) {
            is = true;
        }
        logger.debug("{} transformable to {} : {}", new Object[]{sourceMimetype, targetMimetype, is});
        return is;
    }

    /*
    @Override
    public String getComments(boolean available)
    {
        return onlySupports(MimetypeMap.MIMETYPE_RFC822, MimetypeMap.MIMETYPE_TEXT_PLAIN, available);
    }
    */

    @Override
    protected void transformInternal(ContentReader reader, ContentWriter writer, TransformationOptions options)
            throws Exception {
        // TikaInputStream tikaInputStream = null;
        try (InputStream is = reader.getContentInputStream()) {
            // wrap the given stream to a TikaInputStream instance
            // tikaInputStream =  TikaInputStream.get(reader.getContentInputStream());
            // final Icu4jEncodingDetector encodingDetector = new Icu4jEncodingDetector();
            // final Charset charset = encodingDetector.detect(tikaInputStream, new Metadata());

            // MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()), tikaInputStream);
            MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()), is);
            /*
            if (charset != null)
            {
                mimeMessage.setHeader("Content-Type", "text/plain; charset=" + charset.name());
                mimeMessage.setHeader("Content-Transfer-Encoding", "quoted-printable");
            }
            */
            final StringBuilder sb = new StringBuilder();
            Object content = mimeMessage.getContent();
            if (content instanceof Multipart) {
                boolean html = html();
                Map<String, String> parts = new HashMap<String, String>();
                processPreviewMultiPart((Multipart) content, parts);
                String part = getPreview(parts, html);
                if (part != null) {
                    sb.append(part);
                }
                // sb.append(processPreviewMultiPart((Multipart) content));
            } else {
                sb.append(content.toString());
            }
            writer.putContent(sb.toString());
        }
        /* finally
        {
            if (tikaInputStream != null)
            {
                try
                {
                    // it closes any other resources associated with it
                    tikaInputStream.close();
                }
                catch (IOException e)
                {
                    logger.error(e.getMessage(), e);
                }
            }
        } */
    }

    private String getPreview(Map<String, String> parts, boolean html) {
        String part = null;
        String mimeType = null;
        if (html) { // <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
            if (parts.containsKey("text/html")) {
                mimeType = "text/html";
                part = parts.get(mimeType);
            }
        }

        if (part == null) {
            if (parts.containsKey("text/plain")) {
                mimeType = "text/plain";
                // part = parts.get("text/html");
            } else if (parts.containsKey("text/html")) {
                // part = parts.get("text/plain");
                mimeType = "text/html";
            } else if (parts.entrySet().size() > 0) {
                mimeType = parts.entrySet().iterator().next().getKey();
            }
            if (mimeType != null) {
                logger.debug("Using mime type {}", mimeType);
                part = parts.get(mimeType);
            } else {
                logger.warn("No part found !");
            }
        }
        logger.info("Using part with mime type " + mimeType);
        if (MimetypeMap.MIMETYPE_HTML.equals(mimeType)) {
            return part;
        } else {
            return "<pre>" + part + "</pre>";
        }
    }

    private void processPreviewMultiPart(Multipart multipart, Map<String, String> parts) throws MessagingException, IOException {
        logger.debug("Processing multipart of type {}", multipart.getContentType());
        // FIXME : Implement strict Depth or breadth first ?
        for (int i = 0, n = multipart.getCount(); i < n; i++) {
            Part part = multipart.getBodyPart(i);
            logger.debug("Processing part name {}, disposition = {}, type type = {}", new Object[]{part.getFileName(), part.getDisposition(), part.getContentType()});
            if (part.getContent() instanceof Multipart) {
                processPreviewMultiPart((Multipart) part.getContent(), parts);

            } else if (part.getContentType().contains("text")) {
                String key = part.getContentType().split(";")[0];
                String content = null;
                logger.debug("Add part with content type {} using key {}", part.getContentType(), key);

                if (key.endsWith("html")) {
                    // <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
                    // Breaks preview !
                    content = part.getContent().toString().replaceAll("(?i)(?s)<meta.*charset=[^>]*>","");
                } else {
                    content = part.getContent().toString();
                }
                appendPreviewContent(parts, key, content);

            }

        }
    }

    private void appendPreviewContent(Map<String, String> parts, String key, String part) {
        StringBuffer sb = new StringBuffer();
        if (parts.containsKey(key)) {
            logger.info("Appending to existing value for key {}", key);
            sb.append(parts.get(key));
            sb.append("\n");
        }
        sb.append(part);
        parts.put(key, sb.toString());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    boolean html() {
        boolean isHtml = false;
        if (forceHtml) {
            isHtml = forceHtml;
        } else {
            if (this.html == null) {
                String emlPipeLine = null;
                ChildApplicationContextFactory ca = (ChildApplicationContextFactory) applicationContext.getBean("Transformers");
                if (ca != null) {
                    TransformerConfig tc = (TransformerConfig) ca.getApplicationContext().getBean("transformerConfig");
                    emlPipeLine = tc.getProperty("content.transformer.complex.Rfc822ToSwf.pipeline");
                    if (emlPipeLine != null) {
                        this.html = emlPipeLine.toLowerCase().startsWith("rfc822|html");
                    }
                }
                if (this.html == null) {
                    this.html = Boolean.FALSE;
                }
                logger.debug("content.transformer.complex.Rfc822ToSwf.pipeline={} -> html = {}", emlPipeLine, html);
            }
            isHtml = this.html;
        }
        return isHtml;
    }
}