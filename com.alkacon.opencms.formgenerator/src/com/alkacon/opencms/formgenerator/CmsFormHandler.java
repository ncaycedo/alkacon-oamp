/*
 * File   : $Source: /alkacon/cvs/alkacon/com.alkacon.opencms.formgenerator/src/com/alkacon/opencms/formgenerator/CmsFormHandler.java,v $
 * Date   : $Date: 2008/01/15 17:31:54 $
 * Version: $Revision: 1.2 $
 *
 * This file is part of the Alkacon OpenCms Add-On Module Package
 *
 * Copyright (c) 2007 Alkacon Software GmbH (http://www.alkacon.com)
 *
 * The Alkacon OpenCms Add-On Module Package is free software: 
 * you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The Alkacon OpenCms Add-On Module Package is distributed 
 * in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with the Alkacon OpenCms Add-On Module Package.  
 * If not, see http://www.gnu.org/licenses/.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com.
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org.
 */

package com.alkacon.opencms.formgenerator;

import com.alkacon.opencms.formgenerator.database.CmsFormDataAccess;
import com.alkacon.opencms.formgenerator.database.I_CmsFormDataAccess;

import org.opencms.i18n.CmsEncoder;
import org.opencms.i18n.CmsMessages;
import org.opencms.jsp.CmsJspActionElement;
import org.opencms.mail.CmsHtmlMail;
import org.opencms.mail.CmsSimpleMail;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.module.CmsModule;
import org.opencms.util.CmsByteArrayDataSource;
import org.opencms.util.CmsMacroResolver;
import org.opencms.util.CmsRequestUtil;
import org.opencms.util.CmsStringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;

/**
 * The form handler controls the html or mail output of a configured email form.<p>
 * 
 * Provides methods to determine the action that takes place and methods to create different
 * output formats of a submitted form.<p>
 * 
 * @author Andreas Zahner 
 * @author Thomas Weckert
 * @author Jan Baudisch
 * 
 * @version $Revision: 1.2 $
 * 
 * @since 7.0.4 
 */
public class CmsFormHandler extends CmsJspActionElement {

    /** Request parameter value for the form action parameter: correct the input. */
    public static final String ACTION_CONFIRMED = "confirmed";

    /** Request parameter value for the form action parameter: correct the input. */
    public static final String ACTION_CORRECT_INPUT = "correct";

    /** Request parameter value for the form action parameter: Jump to the download page of the csv data (allow only for offline project!!!). */
    public static final String ACTION_DOWNLOAD_DATA_1 = "export1";

    /** Request parameter value for the form action parameter: Download the csv data.  */
    public static final String ACTION_DOWNLOAD_DATA_2 = "export2";

    /** Request parameter value for the form action parameter: form submitted. */
    public static final String ACTION_SUBMIT = "submit";

    /** Name of the file item session attribute. */
    public static final String ATTRIBUTE_FILEITEMS = "fileitems";

    /** Form error: mandatory field not filled out. */
    public static final String ERROR_MANDATORY = "mandatory";

    /** Form error: validation error of input. */
    public static final String ERROR_VALIDATION = "validation";

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsFormHandler.class);

    private static final String MODULE = "com.alkacon.opencms.formgenerator";

    /** Request parameter name for the hidden form action parameter to determine the action. */
    public static final String PARAM_FORMACTION = "formaction";

    /** Contains eventual validation errors. */
    private Map m_errors;

    /** Needed to implant form fields into the mail text. */
    private transient CmsMacroResolver m_macroResolver;

    /** The form configuration object. */
    private CmsForm m_formConfiguration;

    /** Temporarily stores the fields as hidden fields in the String. */
    private String m_hiddenFields;

    /** Flag indicating if the form is displayed for the first time. */
    private boolean m_initial;

    /** Boolean indicating if the form is validated correctly. */
    private Boolean m_isValidatedCorrect;

    /** The localized messages for the form handler. */
    private CmsMessages m_messages;

    /** The multipart file items. */
    private List m_mulipartFileItems;

    /** The map of request parameters. */
    private Map m_parameterMap;

    /**
     * Constructor, creates the necessary form configuration objects.<p>
     * 
     * @param context the JSP page context object
     * @param req the JSP request 
     * @param res the JSP response 
     * @throws Exception if creating the form configuration objects fails
     */
    public CmsFormHandler(PageContext context, HttpServletRequest req, HttpServletResponse res)
    throws Exception {

        super(context, req, res);
        init(req, null);
    }

    /**
     * Constructor, creates the necessary form configuration objects using a given configuration file URI.<p>
     * 
     * @param context the JSP page context object
     * @param req the JSP request 
     * @param res the JSP response 
     * @param formConfigUri URI of the form configuration file, if not provided, current URI is used for configuration
     * @throws Exception if creating the form configuration objects fails
     */
    public CmsFormHandler(PageContext context, HttpServletRequest req, HttpServletResponse res, String formConfigUri)
    throws Exception {

        super(context, req, res);
        init(req, formConfigUri);
    }

    /**
     * Replaces line breaks with html &lt;br&gt;.<p>
     * 
     * @param value the value to substitute
     * @return the substituted value
     */
    public String convertToHtmlValue(String value) {

        return convertValue(value, "html");
    }

    /**
     * Replaces html &lt;br&gt; with line breaks.<p>
     * 
     * @param value the value to substitute
     * @return the substituted value
     */
    public String convertToPlainValue(String value) {

        return convertValue(value, "");
    }

    /**
     * Converts a given String value to the desired output format.<p>
     * 
     * The following output formats are possible:
     * <ul>
     * <li>"html" meaning that &lt;br&gt; tags are added</li>
     * <li>"plain"  or any other String value meaning that &lt;br&gt; tags are removed</li>
     * </ul>
     *  
     * @param value the String value to convert
     * @param outputType the type of the resulting output
     * @return the converted String in the desired output format
     */
    public String convertValue(String value, String outputType) {

        if ("html".equalsIgnoreCase(outputType)) {
            // output should be html, add line break tags and characters
            value = CmsStringUtil.escapeHtml(value);
        } else {
            // output should be plain, remove html line break tags and characters
            value = CmsStringUtil.substitute(value, "<br>", "\n");
        }
        return value;
    }

    /**
     * Returns the configured form field values as hidden input fields.<p>
     * 
     * @return the configured form field values as hidden input fields
     */
    public String createHiddenFields() {

        if (CmsStringUtil.isEmpty(m_hiddenFields)) {
            List fields = getFormConfiguration().getFields();
            StringBuffer result = new StringBuffer(fields.size() * 8);
            // iterate the form fields
            for (int i = 0, n = fields.size(); i < n; i++) {

                I_CmsField currentField = (I_CmsField)fields.get(i);

                if (currentField == null) {
                    continue;
                } else if (CmsCheckboxField.class.isAssignableFrom(currentField.getClass())) {
                    // special case: checkbox, can have more than one value
                    Iterator k = currentField.getItems().iterator();
                    while (k.hasNext()) {
                        CmsFieldItem item = (CmsFieldItem)k.next();
                        if (item.isSelected()) {
                            result.append("<input type=\"hidden\" name=\"");
                            result.append(currentField.getName());
                            result.append("\" value=\"");
                            result.append(CmsEncoder.escapeXml(item.getValue()));
                            result.append("\">\n");
                        }
                    }
                } else if (CmsStringUtil.isNotEmpty(currentField.getValue())) {
                    // all other fields are converted to a simple hidden field
                    result.append("<input type=\"hidden\" name=\"");
                    result.append(currentField.getName());
                    result.append("\" value=\"");
                    result.append(CmsEncoder.escapeXml(currentField.getValue()));
                    result.append("\">\n");
                }

            }
            // store the generated input fields for further usage to avoid unnecessary rebuilding
            m_hiddenFields = result.toString();
        }
        // return generated result list
        return m_hiddenFields;
    }

    /**
     * Creates a list of internet addresses (email) from a semicolon separated String.<p>
     * 
     * @param mailAddresses a semicolon separated String with email addresses
     * @return list of internet addresses (email)
     * @throws AddressException if an email address is not correct
     */
    protected List createInternetAddresses(String mailAddresses) throws AddressException {

        if (CmsStringUtil.isNotEmpty(mailAddresses)) {
            // at least one email address is present, generate list
            StringTokenizer T = new StringTokenizer(mailAddresses, ";");
            List addresses = new ArrayList(T.countTokens());
            while (T.hasMoreTokens()) {
                InternetAddress address = new InternetAddress(T.nextToken());
                addresses.add(address);
            }
            return addresses;
        } else {
            // no address given, return empty list
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Creates the output String of the submitted fields for email creation.<p>
     * 
     * @param isHtmlMail if true, the output is formatted as HTML, otherwise as plain text
     * @param isConfirmationMail if true, the text for the confirmation mail is created, otherwise the text for mail receiver
     * @return the output String of the submitted fields for email creation
     */
    public String createMailTextFromFields(boolean isHtmlMail, boolean isConfirmationMail) {

        List fieldValues = getFormConfiguration().getFields();
        StringBuffer result = new StringBuffer(fieldValues.size() * 8);
        if (isHtmlMail) {
            // create html head with style definitions and body
            result.append("<html><head>\n");
            result.append("<style type=\"text/css\"><!--\n");
            String style = getMessages().key("form.email.style.body");
            if (CmsStringUtil.isNotEmpty(style)) {
                result.append("body,h1,p,td { ");
                result.append(style);
                result.append(" }\n");
            }
            style = getMessages().key("form.email.style.h1");
            if (CmsStringUtil.isNotEmpty(style)) {
                result.append("h1 { ");
                result.append(style);
                result.append(" }\n");
            }
            style = getMessages().key("form.email.style.p");
            if (CmsStringUtil.isNotEmpty(style)) {
                result.append("p { ");
                result.append(style);
                result.append(" }\n");
            }
            style = getMessages().key("form.email.style.fields");
            if (CmsStringUtil.isNotEmpty(style)) {
                result.append("table.fields { ");
                result.append(style);
                result.append(" }\n");
            }
            style = getMessages().key("form.email.style.fieldlabel");
            if (CmsStringUtil.isNotEmpty(style)) {
                result.append("td.fieldlabel { ");
                result.append(style);
                result.append(" }\n");
            }
            style = getMessages().key("form.email.style.fieldvalue");
            if (CmsStringUtil.isNotEmpty(style)) {
                result.append("td.fieldvalue { ");
                result.append(style);
                result.append(" }\n");
            }
            style = getMessages().key("form.email.style.misc");
            if (CmsStringUtil.isNotEmpty(style)) {
                result.append(getMessages().key("form.email.style.misc"));
            }
            result.append("//--></style>\n");
            result.append("</head><body>\n");
            if (isConfirmationMail) {
                // append the confirmation mail text
                result.append(m_macroResolver.resolveMacros(getFormConfiguration().getConfirmationMailText()));
            } else {
                // append the email text
                result.append(m_macroResolver.resolveMacros(getFormConfiguration().getMailText()));
            }
            result.append("<table border=\"0\" class=\"fields\">\n");
        } else {
            // generate simple text mail
            if (isConfirmationMail) {
                // append the confirmation mail text
                result.append(m_macroResolver.resolveMacros(getFormConfiguration().getConfirmationMailTextPlain()));
            } else {
                // append the email text
                result.append(m_macroResolver.resolveMacros(getFormConfiguration().getMailTextPlain()));
            }
            result.append("\n\n");
        }
        // generate output for submitted form fields
        Iterator i = fieldValues.iterator();
        while (i.hasNext()) {

            I_CmsField current = (I_CmsField)i.next();
            // dont show the letter of agreement (CmsPrivacyField)
            if (isHtmlMail && !(current instanceof CmsPrivacyField)) {
                // format output as HTML
                result.append("<tr><td class=\"fieldlabel\">");
                result.append(current.getLabel());
                result.append("</td><td class=\"fieldvalue\">");
                result.append(convertToHtmlValue(current.toString()));
                result.append("</td></tr>\n");
            } else if (!(current instanceof CmsPrivacyField)) {
                // format output as plain text
                result.append(current.getLabel());
                result.append("\t");
                result.append(current.toString());
                result.append("\n");
            }
        }
        if (isHtmlMail) {
            // create html table closing tag
            result.append("</table>\n");
            if (!isConfirmationMail && getFormConfiguration().hasConfigurationErrors()) {
                // write form configuration errors to html mail
                result.append("<h1>");
                result.append(getMessages().key("form.configuration.error.headline"));
                result.append("</h1>\n<p>");
                for (int k = 0; k < getFormConfiguration().getConfigurationErrors().size(); k++) {
                    result.append(getFormConfiguration().getConfigurationErrors().get(k));
                    result.append("<br>");
                }
                result.append("</p>\n");
            }
            // create body and html closing tags
            result.append("</body></html>");
        } else if (!isConfirmationMail && getFormConfiguration().hasConfigurationErrors()) {
            // write form configuration errors to text mail
            result.append("\n");
            result.append(getMessages().key("form.configuration.error.headline"));
            result.append("\n");
            for (int k = 0; k < getFormConfiguration().getConfigurationErrors().size(); k++) {
                result.append(getFormConfiguration().getConfigurationErrors().get(k));
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * Returns if the data download should be initiated.<p>
     * 
     * @return true if the data download should be initiated, otherwise false
     */
    public boolean downloadData() {

        boolean result = false;

        String paramAction = getParameter(CmsFormHandler.PARAM_FORMACTION);
        if (CmsFormHandler.ACTION_DOWNLOAD_DATA_2.equals(paramAction)) {
            result = true;
        }
        return result;
    }

    /**
     * Returns the errors found when validating the form.<p>
     * 
     * @return the errors found when validating the form
     */
    public Map getErrors() {

        return m_errors;
    }

    /**
     * Returns the form configuration.<p>
     * 
     * @return the form configuration
     */
    public CmsForm getFormConfiguration() {

        return m_formConfiguration;
    }

    /**
     * Returns the localized messages.<p>
     *
     * @return the localized messages
     */
    public CmsMessages getMessages() {

        return m_messages;
    }

    /**
     * Returns the request parameter with the specified name.<p>
     * 
     * @param parameter the parameter to return
     * 
     * @return the parameter value
     */
    private String getParameter(String parameter) {

        try {
            return ((String[])m_parameterMap.get(parameter))[0];
        } catch (NullPointerException e) {
            return "";
        }
    }

    /** 
     * Returns the map of request parameters.<p>
     * 
     * @return the map of request parameters
     */
    public Map getParameterMap() {

        return m_parameterMap;
    }

    /**
     * Returns if the submitted values contain validation errors.<p>
     * 
     * @return true if the submitted values contain validation errors, otherwise false
     */
    public boolean hasValidationErrors() {

        return (!isInitial() && (getErrors().size() > 0));
    }

    /**
     * Initializes the form handler and creates the necessary configuration objects.<p>
     * 
     * @param req the JSP request 
     * @param formConfigUri URI of the form configuration file, if not provided, current URI is used for configuration
     * @throws Exception if creating the form configuration objects fails
     */
    public void init(HttpServletRequest req, String formConfigUri) throws Exception {

        m_mulipartFileItems = CmsRequestUtil.readMultipartFileItems(req);
        m_macroResolver = CmsMacroResolver.newInstance();
        m_macroResolver.setKeepEmptyMacros(true);

        if (m_mulipartFileItems != null) {
            m_parameterMap = CmsRequestUtil.readParameterMapFromMultiPart(
                getRequestContext().getEncoding(),
                m_mulipartFileItems);
        } else {
            m_parameterMap = new HashMap();
            m_parameterMap.putAll(getRequest().getParameterMap());
        }

        if (m_mulipartFileItems != null) {
            Map fileUploads = (Map)req.getSession().getAttribute(ATTRIBUTE_FILEITEMS);
            if (fileUploads == null) {
                fileUploads = new HashMap();
            }
            // check, if there are any attachments
            Iterator i = m_mulipartFileItems.iterator();
            while (i.hasNext()) {
                FileItem fileItem = (FileItem)i.next();
                if (CmsStringUtil.isNotEmpty(fileItem.getName())) {
                    // append file upload to the map of file items
                    fileUploads.put(fileItem.getFieldName(), fileItem);
                    m_parameterMap.put(fileItem.getFieldName(), new String[] {fileItem.getName()});
                }
            }
            req.getSession().setAttribute(ATTRIBUTE_FILEITEMS, fileUploads);
        } else {
            req.getSession().removeAttribute(ATTRIBUTE_FILEITEMS);
        }
        String formAction = getParameter(PARAM_FORMACTION);
        // security check: some form actions are not allowed for everyone:
        if (this.getCmsObject().getRequestContext().currentProject().isOnlineProject()) {
            if (CmsFormHandler.ACTION_DOWNLOAD_DATA_1.equals(formAction)) {
                LOG.error("Received an illegal request for download data of form "
                    + formConfigUri
                    + " (online request)");
                formAction = ACTION_SUBMIT;
            }
        }
        setErrors(new HashMap());
        m_isValidatedCorrect = null;
        setInitial(CmsStringUtil.isEmpty(formAction));
        // get the localized messages
        CmsModule module = OpenCms.getModuleManager().getModule(MODULE);
        String para = module.getParameter("message", "/com/alkacon/opencms/formgenerator/workplace");

        setMessages(new CmsMessages(para, getRequestContext().getLocale()));
        // get the form configuration
        setFormConfiguration(new CmsForm(this, getMessages(), isInitial(), formConfigUri, formAction));
    }

    /**
     * Returns if the form is displayed for the first time.<p>
     * 
     * @return true if the form is displayed for the first time, otherwise false
     */
    public boolean isInitial() {

        return m_initial;
    }

    /**
     * Sends the confirmation mail with the form data to the specified email address.<p>
     * 
     * @throws Exception if sending the confirmation mail fails
     */
    public void sendConfirmationMail() throws Exception {

        // get the field which contains the confirmation email address
        I_CmsField mailField = (I_CmsField)getFormConfiguration().getFields().get(
            getFormConfiguration().getConfirmationMailField());
        String mailTo = mailField.getValue();

        // create the new confirmation mail message depending on the configured email type
        if (getFormConfiguration().getMailType().equals(CmsForm.MAILTYPE_HTML)) {
            // create a HTML email
            CmsHtmlMail theMail = new CmsHtmlMail();
            theMail.setCharset(getCmsObject().getRequestContext().getEncoding());
            if (CmsStringUtil.isNotEmpty(getFormConfiguration().getMailFrom())) {
                theMail.setFrom(getFormConfiguration().getMailFrom());
            }
            theMail.setTo(createInternetAddresses(mailTo));
            theMail.setSubject(m_macroResolver.resolveMacros(getFormConfiguration().getMailSubjectPrefix()
                + getFormConfiguration().getConfirmationMailSubject()));
            theMail.setHtmlMsg(createMailTextFromFields(true, true));
            theMail.setTextMsg(createMailTextFromFields(false, true));
            // send the mail
            theMail.send();
        } else {
            // create a plain text email
            CmsSimpleMail theMail = new CmsSimpleMail();
            theMail.setCharset(getCmsObject().getRequestContext().getEncoding());
            if (CmsStringUtil.isNotEmpty(getFormConfiguration().getMailFrom())) {
                theMail.setFrom(getFormConfiguration().getMailFrom());
            }
            theMail.setTo(createInternetAddresses(mailTo));
            theMail.setSubject(m_macroResolver.resolveMacros(getFormConfiguration().getMailSubjectPrefix()
                + getFormConfiguration().getConfirmationMailSubject()));
            theMail.setMsg(createMailTextFromFields(false, true));
            // send the mail
            theMail.send();
        }
    }

    /**
       * Sends the collected data due to the configuration of the form 
       * (email, database or both).<p>
       * 
       * @return true if successful 
       */
    public boolean sendData() {

        boolean result = true;
        try {
            CmsForm data = this.getFormConfiguration();
            // fill the macro resolver for resolving in subject and content: 
            List fields = this.getFormConfiguration().getFields();
            I_CmsField field;
            Iterator itFields = fields.iterator();
            while (itFields.hasNext()) {
                field = (I_CmsField)itFields.next();
                m_macroResolver.addMacro(field.getLabel(), field.getValue());
            }
            // send optional confirmation mail
            if (data.isConfirmationMailEnabled()) {
                if (!data.isConfirmationMailOptional()
                    || Boolean.valueOf(getParameter(CmsForm.PARAM_SENDCONFIRMATION)).booleanValue()) {
                    sendConfirmationMail();
                }
            }
            if (data.isTransportDatabase()) {
                result &= sendDatabase();
            }
            if (data.isTransportEmail()) {
                result &= sendMail();
            }

        } catch (Exception e) {
            // an error occurred during mail creation
            if (LOG.isErrorEnabled()) {
                LOG.error("An unexpected error occured.", e);
            }
            m_errors.put("sendmail", e.getMessage());
            result = false;
        }
        return result;
    }

    /**
    * Stores the given form data in the database.<p>
    * 
    * @return true if successful  
    * 
    * @throws Exception if sth goes wrong
    */
    private boolean sendDatabase() throws Exception {

        boolean result = false;
        I_CmsFormDataAccess access = new CmsFormDataAccess();
        result = access.writeFormData(this);
        return result;
    }

    /**
     * Return <code>null</code> or the file item for the given field in 
     * case it is of type <code>{@link CmsFileUploadField}</code>.<p>
     * 
     * @param isFileUploadField the field to the the file item of 
     * 
     * @return <code>null</code> or the file item for the given field in 
     *      case it is of type <code>{@link CmsFileUploadField}</code>
     */
    public FileItem getUploadFile(I_CmsField isFileUploadField) {

        FileItem result = null;
        FileItem current;
        String fieldName = isFileUploadField.getName();
        String uploadFileFieldName;
        Map fileUploads = (Map)getRequest().getSession().getAttribute(ATTRIBUTE_FILEITEMS);

        if (fileUploads != null) {
            Iterator i = fileUploads.values().iterator();
            while (i.hasNext()) {
                current = (FileItem)i.next();
                uploadFileFieldName = current.getFieldName();
                if (fieldName.equals(uploadFileFieldName)) {
                    result = current;
                }
            }
        }
        return result;
    }

    /**
     * Sends the mail with the form data to the specified recipients.<p>
     * 
     * If configured, sends also a confirmation mail to the form submitter.<p>
     * 
     * @return true if the mail has been successfully sent, otherwise false
     */
    private boolean sendMail() {

        try {
            // send optional confirmation mail
            if (getFormConfiguration().isConfirmationMailEnabled()) {
                if (!getFormConfiguration().isConfirmationMailOptional()
                    || Boolean.valueOf(getParameter(CmsForm.PARAM_SENDCONFIRMATION)).booleanValue()) {
                    sendConfirmationMail();
                }
            }
            // create the new mail message depending on the configured email type
            if (getFormConfiguration().getMailType().equals(CmsForm.MAILTYPE_HTML)) {
                // create a HTML email
                CmsHtmlMail theMail = new CmsHtmlMail();
                theMail.setCharset(getCmsObject().getRequestContext().getEncoding());
                if (CmsStringUtil.isNotEmpty(getFormConfiguration().getMailFrom())) {
                    theMail.setFrom(getFormConfiguration().getMailFrom());
                }
                theMail.setTo(createInternetAddresses(getFormConfiguration().getMailTo()));
                theMail.setCc(createInternetAddresses(getFormConfiguration().getMailCC()));
                theMail.setBcc(createInternetAddresses(getFormConfiguration().getMailBCC()));
                theMail.setSubject(m_macroResolver.resolveMacros(getFormConfiguration().getMailSubjectPrefix()
                    + getFormConfiguration().getMailSubject()));
                theMail.setHtmlMsg(createMailTextFromFields(true, false));
                theMail.setTextMsg(createMailTextFromFields(false, false));

                // attach file uploads
                Map fileUploads = (Map)getRequest().getSession().getAttribute(ATTRIBUTE_FILEITEMS);
                if (fileUploads != null) {
                    Iterator i = fileUploads.values().iterator();
                    while (i.hasNext()) {
                        FileItem attachment = (FileItem)i.next();
                        if (attachment != null) {
                            String filename = attachment.getName().substring(
                                attachment.getName().lastIndexOf(File.separator) + 1);
                            theMail.attach(
                                new CmsByteArrayDataSource(
                                    filename,
                                    attachment.get(),
                                    OpenCms.getResourceManager().getMimeType(filename, null, "application/octet-stream")),
                                filename,
                                filename);
                        }
                    }
                }
                // send the mail
                theMail.send();
            } else {
                // create a plain text email
                CmsSimpleMail theMail = new CmsSimpleMail();
                theMail.setCharset(getCmsObject().getRequestContext().getEncoding());
                if (CmsStringUtil.isNotEmpty(getFormConfiguration().getMailFrom())) {
                    theMail.setFrom(getFormConfiguration().getMailFrom());
                }
                theMail.setTo(createInternetAddresses(getFormConfiguration().getMailTo()));
                theMail.setCc(createInternetAddresses(getFormConfiguration().getMailCC()));
                theMail.setBcc(createInternetAddresses(getFormConfiguration().getMailBCC()));
                theMail.setSubject(getFormConfiguration().getMailSubjectPrefix()
                    + getFormConfiguration().getMailSubject());
                theMail.setMsg(createMailTextFromFields(false, false));
                // send the mail
                theMail.send();
            }
        } catch (Exception e) {
            // an error occured during mail creation
            if (LOG.isErrorEnabled()) {
                LOG.error(e.getLocalizedMessage(), e);
            }
            m_errors.put("sendmail", e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Sets the errors found when validating the form.<p>
     * 
     * @param errors the errors found when validating the form
     */
    protected void setErrors(Map errors) {

        m_errors = errors;
    }

    /**
     * Sets the form configuration.<p>
     * 
     * @param configuration the form configuration
     */
    protected void setFormConfiguration(CmsForm configuration) {

        m_formConfiguration = configuration;
    }

    /**
     * Sets if the form is displayed for the first time.<p>
     * @param initial true if the form is displayed for the first time, otherwise false
     */
    protected void setInitial(boolean initial) {

        m_initial = initial;
    }

    /**
     * Sets the localized messages.<p>
     *
     * @param messages the localized messages
     */
    protected void setMessages(CmsMessages messages) {

        m_messages = messages;
    }

    /**
     * Returns if the optional check page should be displayed.<p>
     * 
     * @return true if the optional check page should be displayed, otherwise false
     */
    public boolean showCheck() {

        boolean result = false;

        if (getFormConfiguration().getShowCheck() && ACTION_SUBMIT.equals(getParameter(PARAM_FORMACTION))) {
            result = true;
        } else if (getFormConfiguration().captchaFieldIsOnCheckPage()
            && ACTION_CONFIRMED.equals(getParameter(PARAM_FORMACTION))
            && !validate()) {
            result = true;
        }

        return result;
    }

    /**
        * Returns if the data download page should be displayed.<p>
        * 
        * @return true if the data download page should be displayed, otherwise false
        */
    public boolean showDownloadData() {

        boolean result = false;

        String paramAction = getParameter(CmsFormHandler.PARAM_FORMACTION);
        if (CmsFormHandler.ACTION_DOWNLOAD_DATA_1.equals(paramAction)) {
            result = true;
        } else if (CmsFormHandler.ACTION_DOWNLOAD_DATA_2.equals(paramAction)) {
            result = true;
        }
        return result;
    }

    /**
     * Returns if the input form should be displayed.<p>
     * 
     * @return true if the input form should be displayed, otherwise false
     */
    public boolean showForm() {

        boolean result = false;
        String formAction = getParameter(PARAM_FORMACTION);

        if (isInitial()) {
            // initial call
            result = true;
        } else if (ACTION_CORRECT_INPUT.equalsIgnoreCase(formAction)) {
            // user decided to modify his inputs
            result = true;
        } else if (ACTION_SUBMIT.equalsIgnoreCase(formAction) && !validate()) {
            // input field validation failed
            result = true;

            if (getFormConfiguration().hasCaptchaField() && getFormConfiguration().captchaFieldIsOnCheckPage()) {
                // if there is a captcha field and a check page configured, we do have to remove the already
                // initialized captcha field from the form again. the captcha field gets initialized together with
                // the form, in this moment it is not clear yet whether we have validation errors or and need to
                // to go back to the input form...
                getFormConfiguration().removeCaptchaField();
            }
        } else if (ACTION_CONFIRMED.equalsIgnoreCase(formAction)
            && getFormConfiguration().captchaFieldIsOnCheckPage()
            && !validate()) {
            // captcha field validation on check page failed- redisplay the check page, not the input page!
            result = false;
        } else if (ACTION_DOWNLOAD_DATA_1.equalsIgnoreCase(formAction)) {
            result = false;
        } else if (ACTION_DOWNLOAD_DATA_2.equalsIgnoreCase(formAction)) {
            result = false;
        }

        return result;
    }

    /**
        * Returns if template head or other HTML should be inclulded (the page does not serve as a download page).<p>
        * 
        * @return true if template head or other HTML should be inclulded (the page does not serve as a download page)
        */
    public boolean showTemplate() {

        boolean result = true;

        String paramAction = getParameter(CmsFormHandler.PARAM_FORMACTION);
        if (CmsFormHandler.ACTION_DOWNLOAD_DATA_2.equals(paramAction)) {
            result = false;
        }
        return result;
    }

    /**
     * Validation method that checks the given input fields.<p>
     * 
     * All errors are stored in the member m_errors Map, with the input field name as key
     * and the error message String as value.<p>
     * 
     * @return true if all neccessary fields can be validated, otherwise false
     */
    public boolean validate() {

        if (m_isValidatedCorrect != null) {
            return m_isValidatedCorrect.booleanValue();
        }

        boolean allOk = true;
        // iterate the form fields
        List fields = getFormConfiguration().getFields();

        // validate each form field
        for (int i = 0, n = fields.size(); i < n; i++) {

            I_CmsField currentField = (I_CmsField)fields.get(i);

            if (currentField == null) {
                continue;
            }

            if (CmsCaptchaField.class.isAssignableFrom(currentField.getClass())) {
                // the captcha field doesn't get validated here...
                continue;
            }

            String validationError = currentField.validate(this);
            if (CmsStringUtil.isNotEmpty(validationError)) {
                getErrors().put(currentField.getName(), validationError);
                allOk = false;
            }
        }

        CmsCaptchaField captchaField = m_formConfiguration.getCaptchaField();
        if (captchaField != null) {

            boolean captchaFieldIsOnInputPage = getFormConfiguration().captchaFieldIsOnInputPage()
                && getFormConfiguration().isInputFormSubmitted();
            boolean captchaFieldIsOnCheckPage = getFormConfiguration().captchaFieldIsOnCheckPage()
                && getFormConfiguration().isCheckPageSubmitted();

            if (captchaFieldIsOnInputPage || captchaFieldIsOnCheckPage) {
                if (!captchaField.validateCaptchaPhrase(this, captchaField.getValue())) {
                    getErrors().put(captchaField.getName(), ERROR_VALIDATION);
                    allOk = false;
                }
            }
        }

        m_isValidatedCorrect = Boolean.valueOf(allOk);
        return allOk;
    }

}