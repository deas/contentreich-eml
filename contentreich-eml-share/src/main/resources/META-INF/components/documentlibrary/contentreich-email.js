(function()
{
    var Dom = YAHOO.util.Dom;

    if (Alfresco.DocumentList)
    {
        var origFn = Alfresco.DocumentListViewRenderer.prototype.renderCellDescription;
        Alfresco.DocumentListViewRenderer.prototype.renderCellDescription = function(scope, elCell, oRecord, oColumn, oData) {
            if (oRecord.getData().node.mimetype == "message/rfc822") {
                var subject = oRecord.getData().node.properties['cm:subjectline'];
                if (subject) {
                    oRecord.getData().displayName = subject;
                    delete oRecord.getData().node.properties.title;
                }
            }
            origFn.call(this, scope, elCell, oRecord, oColumn, oData);
        };

        /*
        YAHOO.Bubbling.fire("registerRenderer",
            {
                propertyName: "emailNameRenderer",
                renderer: function(record, label)
                {
                    record.displayName = "displayname";
                    record.fileName = "fileName";
                    // record._filenameId
                    return ".. awesome ! ...";
                }
            });
        */
    }
})();