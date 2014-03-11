package de.contentreich.alfresco.repo.email;

import org.alfresco.model.ImapModel;
import org.alfresco.repo.jscript.app.BasePropertyDecorator;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

/**
 * Actually a hack
 */
public class EmlAttachmentPropertyDecorator extends BasePropertyDecorator{
    private static final Logger logger = LoggerFactory.getLogger(EmlAttachmentPropertyDecorator.class);

    @Override
    public JSONAware decorate(QName propertyName, NodeRef nodeRef, Serializable value) {
        // List<ChildAssociationRef> attachmentFolders = nodeService.getChildAssocs(nodeRef, attachmentAssocTypes);
        List<AssociationRef> attachmentCaRefs = nodeService.getTargetAssocs(nodeRef, ImapModel.ASSOC_IMAP_ATTACHMENT);
        JSONObject map = new JSONObject();
        boolean has = attachmentCaRefs.size() > 0;
        map.put("hasAttachments",has);
        logger.debug("{} has attachments :{}", nodeRef, has);
        return map;
    }
    /**
     * "imap:messageFrom": {
     *  "hasAttachments": false
     * }
     */
}
