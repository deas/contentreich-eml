<import resource="classpath:/alfresco/templates/org/alfresco/import/alfresco-util.js">
// model.widgets[0].options.mimeType - message/rfc822

function addEmailWidget() {
    logger.log("Adding email widget");
    AlfrescoUtil.param('nodeRef');
    AlfrescoUtil.param('site', null);
    AlfrescoUtil.param('formId', null);
    var documentDetails = AlfrescoUtil.getNodeDetails(model.nodeRef, model.site);
    if (documentDetails)
    {
        model.document = documentDetails;
        model.allowMetaDataUpdate = (!documentDetails.item.node.isLocked && documentDetails.item.node.permissions.user["Write"]) || false;
    }

    var emailMetadata = {
        /*
         id : "DocumentMetadata",
         name : "Alfresco.DocumentMetadata",
         */
        id : "EmailDocumentMetadata",
        name : "Contentreich.EmailDocumentMetadata",
        options : {
            nodeRef : model.nodeRef,
            siteId : model.site,
            formId : "email"//model.formId
        }
    };
    model.widgets.push(emailMetadata);
}

model.isEmail = false;

// ScriptWidgetUtils from webscripts !
// widgetUtils.deleteObjectFromArray(model.jsonModel, "id", "HEADER_MY_FILES");
// var sitesMenu = widgetUtils.findObject(model.jsonModel, "id", "HEADER_SITES_MENU");

// fixme use widgetutils

if (model.widgets[0].options.mimeType == "message/rfc822") {
    // var webPreview = widgetUtils.findObject(widgets/*model*/.jsonModel, "id", "WebPreview");
    for (var i=0; i<model.widgets.length; i++)
    {
        if (model.widgets[i].id == "WebPreview")
        {
            addEmailWidget();
            model.isEmail = true;
            break;
        }
    }
}