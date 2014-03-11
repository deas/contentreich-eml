package de.contentreich.share.web;

import org.alfresco.web.evaluator.BaseEvaluator;
import org.json.simple.JSONObject;

/**
 * Created by deas on 2/25/14.
 */
public class EmlAttachmentEvaluator extends BaseEvaluator {
    public static String[] path = new String[] {"node","properties","imap:messageFrom","hasAttachments"};
    @Override
    public boolean evaluate(JSONObject jsonObject) {
        Boolean is = Boolean.TRUE.equals(getProp(jsonObject, path));
        return is;
    }

    private Object getProp(JSONObject jo,String[] path) {
        Object val = null;
        JSONObject j = jo;
        for (int i=0;i<path.length;i++) {
            if (j != null) {
                val = j.get(path[i]);
                if (val instanceof JSONObject) {
                    j = (JSONObject) val;
                } else {
                    j = null;
                }
            } else {
                val = null;
                break;
            }
        }
        return val;
    }
}
