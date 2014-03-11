package de.contentreich.alfresco.repo.email;

import org.alfresco.model.ImapModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by deas on 2/24/14.
 */
public class ImapContent implements NodeServicePolicies.OnMoveNodePolicy, NodeServicePolicies.OnDeleteNodePolicy, NodeServicePolicies.OnAddAspectPolicy {
    private static Logger logger = LoggerFactory.getLogger(ImapContent.class);
    private PolicyComponent policyComponent;
    private NodeService nodeService;

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setPolicyComponent(PolicyComponent policyComponent) {
        this.policyComponent = policyComponent;
    }

    @Override
    public void onMoveNode(ChildAssociationRef oldChildAssocRef, ChildAssociationRef newChildAssocRef) {
        NodeRef childNodeRef = oldChildAssocRef.getChildRef();
        logger.debug("On move node ");
        // List<ChildAssociationRef> childAssocs = nodeService.getChildAssocs(childNodeRef, attachmentAssocFolderTypes);
        List<AssociationRef> attachmentFolderCaRefs = nodeService.getTargetAssocs(childNodeRef, ImapModel.ASSOC_IMAP_ATTACHMENTS_FOLDER);
        logger.debug("Got {} attachment folders for {}", attachmentFolderCaRefs.size());
        if (attachmentFolderCaRefs.size() > 0) {
            NodeRef newParentNodeRef = newChildAssocRef.getParentRef();
            for (AssociationRef assoc : attachmentFolderCaRefs) {
                logger.debug("Moving attachment folder {} to {}", assoc.getTargetRef(), newParentNodeRef);
                ChildAssociationRef pca = nodeService.getPrimaryParent(assoc.getTargetRef());
                nodeService.moveNode(assoc.getTargetRef(), newParentNodeRef, pca.getTypeQName(), pca.getQName());
            }
        }
    }

    public void init() {
        logger.info("Init");
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnMoveNodePolicy.QNAME, ImapModel.ASPECT_IMAP_CONTENT,
                new JavaBehaviour(this, "onMoveNode", Behaviour.NotificationFrequency.TRANSACTION_COMMIT));
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnAddAspectPolicy.QNAME, ImapModel.ASPECT_IMAP_CONTENT,
                new JavaBehaviour(this, "onAddAspect", Behaviour.NotificationFrequency.TRANSACTION_COMMIT));
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnDeleteNodePolicy.QNAME, ImapModel.ASPECT_IMAP_CONTENT,
                new JavaBehaviour(this, "onDeleteNode", Behaviour.NotificationFrequency.TRANSACTION_COMMIT));

    }

    @Override
    public void onDeleteNode(ChildAssociationRef childAssocRef, boolean isNodeArchived) {
        logger.warn("Fix on delete node {}, archived {}", childAssocRef.getChildRef(), isNodeArchived);
    }

    @Override
    public void onAddAspect(NodeRef nodeRef, QName aspectTypeQName) {
        logger.warn("Fix on add aspect {} + {}", nodeRef, aspectTypeQName);
        // IMAP does create those attachment folders, so be careful here :/
    }
}
