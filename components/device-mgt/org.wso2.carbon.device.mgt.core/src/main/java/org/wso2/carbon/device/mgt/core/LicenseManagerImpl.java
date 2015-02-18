/*
 *
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.wso2.carbon.device.mgt.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.device.mgt.common.DeviceManagementConstants;
import org.wso2.carbon.device.mgt.core.config.license.License;
import org.wso2.carbon.device.mgt.core.license.mgt.GenericArtifactManagerFactory;
import org.wso2.carbon.governance.api.exception.GovernanceException;
import org.wso2.carbon.governance.api.generic.GenericArtifactFilter;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.registry.api.RegistryException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LicenseManagerImpl implements LicenseManager {

    private static Log log = LogFactory.getLog(DeviceManagerImpl.class);
    private static final DateFormat format = new SimpleDateFormat("dd-mm-yyyy", Locale.ENGLISH);

    @Override
    public License getLicense(final String deviceType, final String languageCodes) throws LicenseManagementException {

        if (log.isDebugEnabled()) {
            log.debug("Retrieving the license configured upon Device Type: '" + deviceType + "' and Language Code: '" +
                    languageCodes + "'");
        }

        License license = null;
        GenericArtifact[] filteredArtifacts;
        try {
            GenericArtifactManager artifactManager =
                    GenericArtifactManagerFactory.getTenantAwareGovernanceArtifactManager();

            filteredArtifacts = artifactManager.findGenericArtifacts(
                    new GenericArtifactFilter() {
                        public boolean matches(GenericArtifact artifact) throws GovernanceException {
                            String attributeNameVal = artifact.getAttribute(
                                    DeviceManagementConstants.LicenseProperties.OVERVIEW_NAME);
                            String attributeLangVal = artifact.getAttribute(
                                    DeviceManagementConstants.LicenseProperties.OVERVIEW_LANGUAGE);
                            return (attributeNameVal != null && attributeLangVal != null && attributeNameVal.equals
                                    (deviceType) && attributeLangVal.equals(languageCodes));
                        }
                    });

            String validFrom;
            String validTo;
            Date fromDate;
            Date toDate;

            for (GenericArtifact artifact : filteredArtifacts) {
                if (log.isDebugEnabled()) {
                    log.debug("Overview name: " +
                            artifact.getAttribute(DeviceManagementConstants.LicenseProperties.OVERVIEW_NAME));
                    log.debug("Overview provider: " +
                            artifact.getAttribute(DeviceManagementConstants.LicenseProperties.OVERVIEW_PROVIDER));
                    log.debug("Overview language: " +
                            artifact.getAttribute(DeviceManagementConstants.LicenseProperties.OVERVIEW_LANGUAGE));
                    log.debug("Overview validity from: " +
                            artifact.getAttribute(DeviceManagementConstants.LicenseProperties.VALID_FROM));
                    log.debug("Overview validity to: " +
                            artifact.getAttribute(DeviceManagementConstants.LicenseProperties.VALID_TO));
                }
                validFrom = artifact.getAttribute(DeviceManagementConstants.LicenseProperties.VALID_FROM);
                validTo = artifact.getAttribute(DeviceManagementConstants.LicenseProperties.VALID_TO);

                fromDate = format.parse(validFrom);
                toDate = format.parse(validTo);

                license = new License();
                if (fromDate.getTime() <= new Date().getTime() && new Date().getTime() <= toDate.getTime()) {
                    license.setText(artifact.getAttribute(DeviceManagementConstants.LicenseProperties.LICENSE));
                }
            }
        } catch (RegistryException e) {
            String msg = "Error occurred while initializing generic artifact manager associated with retrieving " +
                    "license data stored in registry";
            throw new LicenseManagementException(msg, e);
        } catch (ParseException e) {
            String msg = "Error occurred while parsing the date string";
            log.error(msg, e);
            throw new LicenseManagementException(msg, e);
        }
        return license;
    }

}