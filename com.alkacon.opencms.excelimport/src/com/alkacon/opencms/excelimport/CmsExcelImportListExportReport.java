/*
 * File   : $Source: /alkacon/cvs/alkacon/com.alkacon.opencms.excelimport/src/com/alkacon/opencms/excelimport/CmsExcelImportListExportReport.java,v $
 * Date   : $Date: 2009/04/30 10:52:07 $
 * Version: $Revision: 1.1 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) 2002 - 2008 Alkacon Software GmbH (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software GmbH, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.alkacon.opencms.excelimport;

import org.opencms.jsp.CmsJspActionElement;
import org.opencms.report.I_CmsReportThread;
import org.opencms.workplace.list.A_CmsListReport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

/**
 * Provides a report for user while importing excel files.<p> 
 *
 * @author  Mario Jaeger
 * 
 * @version $Revision: 1.1 $ 
 * 
 * @since 7.5.0 
 */
public class CmsExcelImportListExportReport extends A_CmsListReport {

    /** The CmsResourceExcelImport object. */
    private CmsResourceExcelImport m_cmsResourceExcelImport;

    /**
     * Public constructor with JSP action element.<p>
     * 
     * @param jsp an initialized JSP action element
     * @param cmsResourceExcelImport The current import object
     */
    public CmsExcelImportListExportReport(CmsJspActionElement jsp, CmsResourceExcelImport cmsResourceExcelImport) {

        super(jsp);
        m_cmsResourceExcelImport = cmsResourceExcelImport;
    }

    /**
     * Public constructor with JSP variables.<p>
     * 
     * @param context the JSP page context
     * @param req the JSP request
     * @param res the JSP response
     * @param cmsResourceExcelImport The current import object
     */
    public CmsExcelImportListExportReport(
        PageContext context,
        HttpServletRequest req,
        HttpServletResponse res,
        CmsResourceExcelImport cmsResourceExcelImport) {

        this(new CmsJspActionElement(context, req, res), cmsResourceExcelImport);
    }

    /** 
     * 
     * @see org.opencms.workplace.list.A_CmsListReport#initializeThread()
     */
    public I_CmsReportThread initializeThread() {

        I_CmsReportThread exportThread = new CmsExcelImport(getCms(), m_cmsResourceExcelImport);

        return exportThread;
    }
}