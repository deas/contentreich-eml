<#--
<@standalone>
   
   <@markup id="css" >
      <#include "include/web-preview-css-dependencies.lib.ftl" />
   </@>
   <@markup id="js" >
      <#include "include/web-preview-js-dependencies.lib.ftl" />
   </@>
-->
<#if isEmail>
   <@markup id="email-js" target="js" action="after" scope="global">
       <@script src="${url.context}/res/components/contentreich/email-document-metadata.js" group="${dependencyGroup}"/>
   </@>

    <@markup id="email-html" target="html" action="before" scope="global">
    <div>
        <div style="display:inline-block;" id="${args.htmlid?html}-header">
        </div>
        <div style="display:inline-block;float:right;" id="${args.htmlid?html}-attachment">
        </div>
    </div>
    </@markup>
</#if>
<#--
   <@markup id="widgets">
      <#if node??>
         <@createWidgets group="${dependencyGroup}"/>
      </#if>
   </@>

   <@markup id="html">
      <@uniqueIdDiv>
         <#if node??>
            <#assign el=args.htmlid?html>
         <div id="${el}-body" class="web-preview">
            <div id="${el}-previewer-div" class="previewer">
               <div class="message"></div>
            </div>
         </div>
         </#if>
      </@>
   </@>
</@standalone>
-->
