package de.contentreich.alfresco.repo.email;

import org.alfresco.model.ContentModel;
import org.alfresco.model.ImapModel;
import org.alfresco.repo.imap.AttachmentsExtractor;
import org.alfresco.repo.imap.ImapService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.util.config.RepositoryFolderConfigBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: deas
 * Date: 6/19/13
 * Time: 9:38 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExtendedAttachmentExtractor extends AttachmentsExtractor
{
    private Logger logger = LoggerFactory.getLogger(ExtendedAttachmentExtractor.class);
    private AttachmentsExtractorMode attachmentsExtractorMode;
    private NodeService nodeService;
    private FileFolderService fileFolderService;
    private NodeRef attachmentsFolderRef;
    private ServiceRegistry serviceRegistry;
    private RepositoryFolderConfigBean attachmentsFolder;
    private ImapService imapService;
    private String dispExtractRE;
    private boolean hideAttachmentFolders;

    public void setAttachmentsExtractorMode(String attachmentsExtractorMode)
    {
        super.setAttachmentsExtractorMode(attachmentsExtractorMode);
        this.attachmentsExtractorMode = AttachmentsExtractorMode.valueOf(attachmentsExtractorMode);
    }

    public void setHideAttachmentFolders(boolean hideAttachmentFolders) {
        this.hideAttachmentFolders = hideAttachmentFolders;
    }

    public void setDispExtractRE(String dispExtractRE) {
        this.dispExtractRE = dispExtractRE;
    }

    public void setImapService(ImapService imapService) {
        super.setImapService(imapService);
        this.imapService = imapService;
    }

    public void setNodeService(NodeService nodeService)
    {
        super.setNodeService(nodeService);
        this.nodeService = nodeService;
    }

    public void setFileFolderService(FileFolderService fileFolderService) {
        super.setFileFolderService(fileFolderService);
        this.fileFolderService = fileFolderService;
    }

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        super.setServiceRegistry(serviceRegistry);
        this.serviceRegistry = serviceRegistry;
    }

    public void setAttachmentsFolder(RepositoryFolderConfigBean attachmentsFolder)
    {
        super.setAttachmentsFolder(attachmentsFolder);
        this.attachmentsFolder = attachmentsFolder;
    }

    public void init()
    {

        logger.debug("init");
        attachmentsFolderRef = AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<NodeRef>() {
            public NodeRef doWork() throws Exception {
                NodeRef attFolderRef = attachmentsFolder.getOrCreateFolderPath(serviceRegistry.getNamespaceService(), nodeService, serviceRegistry.getSearchService(), fileFolderService);
                serviceRegistry.getPermissionService().setPermission(attFolderRef, PermissionService.ALL_AUTHORITIES, PermissionService.FULL_CONTROL, true);
                return attFolderRef;
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    public void extractAttachments(NodeRef messageRef, MimeMessage originalMessage) throws IOException, MessagingException
    {
        NodeRef attachmentsFolderRef = null;
        List<Part> collectAttachments = new ArrayList<>();
        Object content = originalMessage.getContent();
        if (content instanceof Multipart) {
            processMultiPart((Multipart) content, collectAttachments);
        }
        logger.debug("Extracted " + collectAttachments.size() + " attachments");

        if (collectAttachments.size() > 0) {
            switch (attachmentsExtractorMode)
            {
                case SAME:
                    attachmentsFolderRef = nodeService.getPrimaryParent(messageRef).getParentRef();
                    break;
                case COMMON:
                    attachmentsFolderRef = this.attachmentsFolderRef;
                    break;
                case SEPARATE:
                default:
                    NodeRef parentFolder = nodeService.getPrimaryParent(messageRef).getParentRef();
                    String messageName = (String) nodeService.getProperty(messageRef, ContentModel.PROP_NAME);
                    String attachmentsFolderName = messageName + "-attachments";
                    attachmentsFolderRef = fileFolderService.create(parentFolder, attachmentsFolderName, ContentModel.TYPE_FOLDER).getNodeRef();
                    if (hideAttachmentFolders) {
                        logger.debug("Adding hidden aspectd to attachment folder");
                        nodeService.addAspect(attachmentsFolderRef, ContentModel.ASPECT_HIDDEN, new HashMap());
                    }
                    break;
            }
            nodeService.createAssociation(messageRef, attachmentsFolderRef, ImapModel.ASSOC_IMAP_ATTACHMENTS_FOLDER);

            for (Part part: collectAttachments) {
                createAttachment(messageRef, attachmentsFolderRef, part);
            }
        }
    }

    /**
     * See de.contentreich.alfresco.repo.email.EMLTransformer
     *
     * https://labs.consol.de/blog/java/java-mail/removing-attachments-with-javamail/
     *
     */
    public void processMultiPart(Multipart multipart, List<Part> parts) throws MessagingException, IOException
    {
        logger.debug("Processing multipart of type {}", multipart.getContentType());
        for (int i = 0, n = multipart.getCount(); i < n; i++)
        {
            Part part = multipart.getBodyPart(i);
            logger.debug("Processing a {}", part.getClass().getName());
            if (part.getContent() instanceof Multipart) {
                processMultiPart((Multipart) part.getContent(), parts);
            } else {
                String dp = part.getDisposition() != null ? part.getDisposition() : "";
                Matcher matcher = Pattern.compile(dispExtractRE).matcher(dp);
                if (matcher.matches())
                {
                    logger.debug("Disposition " + dp + " matches " + dispExtractRE + ". Extracting ...");
                    parts.add(part);
                } else {
                    logger.debug("Disposition " + dp + " does not match" + dispExtractRE + ". Skipping");
                }
            }
        }
    }


    private void createAttachment(NodeRef messageFile, NodeRef attachmentsFolderRef, Part part) throws MessagingException, IOException
    {
        logger.info("Creating attachment in folder ");
        String fileName = part.getFileName();
        if (fileName == null || fileName.isEmpty())
        {
            fileName = "unnamed";
        }
        try
        {
            fileName = MimeUtility.decodeText(fileName);
        }
        catch (UnsupportedEncodingException e)
        {
            if (logger.isWarnEnabled())
            {
                logger.warn("Cannot decode file name '" + fileName + "'", e);
            }
        }

        ContentType contentType = new ContentType(part.getContentType());
        NodeRef attachmentFile = fileFolderService.searchSimple(attachmentsFolderRef, fileName);
        // The one possible behaviour
       /*
        if (result.size() > 0)
        {
            for (FileInfo fi : result)
            {
                fileFolderService.delete(fi.getNodeRef());
            }
        }
        */
        // And another one behaviour which will overwrite the content of the existing file. It is performance preferable.
        if (attachmentFile == null)
        {
            FileInfo createdFile = fileFolderService.create(attachmentsFolderRef, fileName, ContentModel.TYPE_CONTENT);
            nodeService.createAssociation(messageFile, createdFile.getNodeRef(), ImapModel.ASSOC_IMAP_ATTACHMENT);
            attachmentFile = createdFile.getNodeRef();
        }
        else
        {


            String newFileName = imapService.generateUniqueFilename(attachmentsFolderRef, fileName);

            FileInfo createdFile = fileFolderService.create(attachmentsFolderRef, newFileName, ContentModel.TYPE_CONTENT);
            nodeService.createAssociation(messageFile, createdFile.getNodeRef(), ImapModel.ASSOC_IMAP_ATTACHMENT);
            attachmentFile = createdFile.getNodeRef();

        }

        nodeService.setProperty(attachmentFile, ContentModel.PROP_DESCRIPTION, nodeService.getProperty(messageFile, ContentModel.PROP_NAME));

        ContentWriter writer = fileFolderService.getWriter(attachmentFile);
        writer.setMimetype(contentType.getBaseType());
        OutputStream os = writer.getContentOutputStream();
        FileCopyUtils.copy(part.getInputStream(), os);
    }

}
