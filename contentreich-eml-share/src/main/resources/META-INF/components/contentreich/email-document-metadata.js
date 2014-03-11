if (typeof Contentreich == "undefined" || !Contentreich)
{
   var Contentreich = { };
}

/**
 * EmailDocumentMetadata component.
 * 
 * @namespace Contentreich
 * @class Contentreich.EmailDocumentMetadata
 */
(function()
{
    var Dom = YAHOO.util.Dom,
        Event = YAHOO.util.Event,
        formatDate = Alfresco.util.formatDate,
        fromISO8601 = Alfresco.util.fromISO8601;
    /**
     * EmailDocumentMetadata constructor.
     *
     * @param {String} htmlId The HTML id of the parent element
     * @return {Contentreich.EmailDocumentMetadata} The new DocumentMetadata instance
     * @constructor
     */
    Contentreich.EmailDocumentMetadata = function EmailDocumentMetadata_constructor(htmlId)
    {
        Contentreich.EmailDocumentMetadata.superclass.constructor.call(this, "Contentreich.EmailDocumentMetadata", htmlId);
        return this;
    };

    YAHOO.extend(Contentreich.EmailDocumentMetadata, Alfresco.component.Base,
        {

            options:
            {
                nodeRef: null,
                site: null,
                formId: null
            },
            onReady: function EmailDocumentMetadata_onReady()
            {
                // alert("onready");
                // Load the form
                Alfresco.util.Ajax.request(
                    {
                        url: Alfresco.constants.URL_SERVICECONTEXT + "components/form",
                        dataObj:
                        {
                            htmlid: this.id + "-formContainer",
                            itemKind: "node",
                            itemId: this.options.nodeRef,
                            formId: this.options.formId,
                            mode: "view"
                        },
                        successCallback:
                        {
                            fn: this.onFormLoaded,
                            scope: this
                        },
                        failureMessage: this.msg("message.failure"),
                        scope: this,
                        execScripts: true
                    });
            },

            onFormLoaded: function EmailDocumentMetadata_onFormLoaded(response)
            {
                var h1El = Dom.getElementBy(function(e) { return true; } , "h1", document);
                var formEl = Dom.get(this.id + "-header"),
                    me = this;
                formEl.innerHTML = response.serverResponse.responseText;
                Dom.getElementsByClassName("viewmode-value-date", "span", formEl, function()
                {

                    var showTime = Dom.getAttribute(this, "data-show-time")
                    var dateFormat = (showTime=='false') ? me.msg("date-format.defaultDateOnly") : me.msg("date-format.default")
                    this.innerHTML = formatDate(fromISO8601(Dom.getAttribute(this, "data-date-iso8601")), dateFormat )
                });
                var atts = Dom.getElementsByClassName("document-attachment");
                if (atts && atts.length == 1) {
                    var origAttEl = atts[0];
                    var attEl = Dom.get(this.id + "-attachment");
                    attEl.innerHTML = origAttEl.innerHTML;
                    origAttEl.parentElement.parentElement.innerHTML = "";

                }

                var formValues = Dom.getElementsByClassName("viewmode-value", "span", formEl);
                if (formValues.length > 0) {
                    var ni = Dom.getElementsByClassName("node-info", "div", document);
                    if (ni.length == 1) {
                        var svEl = formValues[formValues.length - 1];
                        var h1El = Dom.getElementBy(function(e) { return true; } , "h1", ni[0]);
                        if (h1El) {
                            Dom.setStyle(svEl.parentNode, 'display', 'none');
                            var subject = svEl.innerText || svEl.textContent;// textContent is FF :(
                            h1El.firstChild.nodeValue = subject;
                        }
                    }
                }
            }
        });
})();
